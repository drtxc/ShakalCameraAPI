package com.shakal.camera.core

/**
 * GLSL shader sources for the ShakalCamera GPU pipeline.
 * 
 * Pipeline stages:
 * 1. Downscale to preset resolution (nearest)
 * 2. Color: WB shift → saturation → posterize → palette+dither
 * 3. Noise: grain (stronger in shadows, time-dependent)
 * 4. Blocks 8×8: luma/chroma quantization (MJPEG imitation)
 * 5. Chromatic fringe
 * 6. Vignette / hotspot / bloom
 * 7. Upscale to screen (nearest)
 */
object ShaderSources {

    /**
     * Vertex shader — simple fullscreen quad.
     */
    val VERTEX_SHADER = """
        #version 300 es
        layout(location = 0) in vec2 aPosition;
        layout(location = 1) in vec2 aTexCoord;
        out vec2 vTexCoord;
        void main() {
            vTexCoord = aTexCoord;
            gl_Position = vec4(aPosition, 0.0, 1.0);
        }
    """.trimIndent()

    /**
     * Main fragment shader implementing all "shakal" effects.
     * Parameters are passed as uniforms from the Preset.
     */
    val FRAGMENT_SHADER = """
        #version 300 es
        precision highp float;
        precision highp sampler2D;
        
        in vec2 vTexCoord;
        out vec4 fragColor;
        
        uniform sampler2D uTexture;        // Camera frame (OES external or 2D)
        uniform vec2 uResolution;          // Output resolution
        uniform vec2 uTexelSize;           // 1.0 / preset resolution
        uniform float uTime;               // Time in seconds for noise animation
        uniform float uBrightness;         // Average brightness for adaptive effects
        
        // Preset parameters
        uniform vec3 uWbShift;             // White balance shift [r, g, b]
        uniform float uSaturation;
        uniform float uNoise;
        uniform float uShadowNoise;
        uniform float uDenoiseSmear;
        uniform float uChromaFringe;
        uniform int uBlockSize;
        uniform float uVignette;
        uniform float uHotspot;
        uniform float uBloom;
        uniform int uPosterize;
        uniform int uPaletteSize;
        uniform int uDither;
        uniform float uRollingShutter;
        uniform float uRowGlitch;
        
        // Hash function for noise
        float hash(vec2 p) {
            vec3 p3 = fract(vec3(p.xyx) * 0.1031);
            p3 += dot(p3, p3.yzx + 33.33);
            return fract((p3.x + p3.y) * p3.z);
        }
        
        // Value noise
        float noise2d(vec2 p) {
            vec2 i = floor(p);
            vec2 f = fract(p);
            f = f * f * (3.0 - 2.0 * f);
            float a = hash(i);
            float b = hash(i + vec2(1.0, 0.0));
            float c = hash(i + vec2(0.0, 1.0));
            float d = hash(i + vec2(1.0, 1.0));
            return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
        }
        
        // Bayer 4x4 dither matrix
        float bayerDither4x4(vec2 pos) {
            int x = int(mod(pos.x, 4.0));
            int y = int(mod(pos.y, 4.0));
            int idx = x + y * 4;
            float bayer[16] = float[16](
                0.0/16.0,  8.0/16.0,  2.0/16.0, 10.0/16.0,
                12.0/16.0, 4.0/16.0, 14.0/16.0,  6.0/16.0,
                3.0/16.0, 11.0/16.0,  1.0/16.0,  9.0/16.0,
                15.0/16.0, 7.0/16.0, 13.0/16.0,  5.0/16.0
            );
            return bayer[idx] - 0.5;
        }
        
        void main() {
            vec2 uv = vTexCoord;
            
            // Rolling shutter effect — shift rows based on time
            if (uRollingShutter > 0.0) {
                float shift = sin(uv.y * 20.0 + uTime * 5.0) * uRollingShutter * 0.02;
                uv.x += shift;
            }
            
            // Row glitch effect
            if (uRowGlitch > 0.0) {
                float rowNoise = hash(vec2(floor(uv.y * uResolution.y), floor(uTime * 10.0)));
                if (rowNoise > (1.0 - uRowGlitch * 0.1)) {
                    uv.x += (rowNoise - 0.5) * 0.1;
                }
            }
            
            // Sample with block quantization (MJPEG block effect)
            vec2 blockUv = uv;
            if (uBlockSize > 0) {
                float bs = float(uBlockSize);
                vec2 blockPos = floor(uv * uResolution / bs) * bs / uResolution;
                blockUv = blockPos;
            }
            
            // Chromatic fringe — offset R and B channels
            vec3 color;
            if (uChromaFringe > 0.0) {
                float offset = uChromaFringe * 0.005;
                color.r = texture(uTexture, blockUv + vec2(offset, 0.0)).r;
                color.g = texture(uTexture, blockUv).g;
                color.b = texture(uTexture, blockUv - vec2(offset, 0.0)).b;
            } else {
                color = texture(uTexture, blockUv).rgb;
            }
            
            // White balance shift
            color *= uWbShift;
            
            // Saturation
            float luma = dot(color, vec3(0.299, 0.587, 0.114));
            color = mix(vec3(luma), color, uSaturation);
            
            // Posterize
            if (uPosterize > 0) {
                float levels = float(uPosterize);
                color = floor(color * levels + 0.5) / levels;
            }
            
            // Palette reduction (for four_tone and similar)
            if (uPaletteSize > 0) {
                float levels = float(uPaletteSize);
                // Convert to luma with slight olive tint
                float y = dot(color, vec3(0.299, 0.587, 0.114));
                // Ordered dithering
                if (uDither > 0) {
                    y += bayerDither4x4(vTexCoord * uResolution) / levels;
                }
                y = floor(y * levels) / (levels - 1.0);
                y = clamp(y, 0.0, 1.0);
                // Olive tint: slight green-yellow
                color = vec3(y * 0.95, y * 1.02, y * 0.85);
            }
            
            // Noise — grain stronger in shadows
            float shadowFactor = 1.0 - luma;
            float noiseAmount = uNoise + uShadowNoise * shadowFactor;
            float n = (noise2d(vTexCoord * uResolution * 0.5 + uTime * 3.0) - 0.5) * 2.0;
            color += n * noiseAmount;
            
            // Denoise smear (softens the image, makes it "plasticky")
            if (uDenoiseSmear > 0.0) {
                vec2 texel = uTexelSize * uDenoiseSmear;
                vec3 sum = color;
                sum += texture(uTexture, blockUv + vec2(texel.x, 0.0)).rgb * uWbShift;
                sum += texture(uTexture, blockUv - vec2(texel.x, 0.0)).rgb * uWbShift;
                sum += texture(uTexture, blockUv + vec2(0.0, texel.y)).rgb * uWbShift;
                sum += texture(uTexture, blockUv - vec2(0.0, texel.y)).rgb * uWbShift;
                color = mix(color, sum / 5.0, uDenoiseSmear * 0.5);
            }
            
            // Vignette
            if (uVignette > 0.0) {
                vec2 vigUv = vTexCoord * 2.0 - 1.0;
                float vig = 1.0 - dot(vigUv * 0.5, vigUv * 0.5);
                vig = clamp(pow(vig, 1.0 + uVignette * 3.0), 0.0, 1.0);
                color *= vig;
            }
            
            // Hotspot (center overexposure)
            if (uHotspot > 0.0) {
                vec2 center = vTexCoord - 0.5;
                float dist = length(center);
                float hot = exp(-dist * dist * 8.0) * uHotspot;
                color += vec3(hot * 0.8, hot * 0.7, hot * 0.5);
            }
            
            // Bloom (bright areas glow)
            if (uBloom > 0.0) {
                float bright = max(max(color.r, color.g), color.b);
                float bloomAmount = smoothstep(0.6, 1.0, bright) * uBloom;
                color += vec3(bloomAmount * 0.3, bloomAmount * 0.25, bloomAmount * 0.2);
            }
            
            // Block quantization of luma/chroma within blocks
            if (uBlockSize > 0) {
                float qLevels = 8.0;
                color = floor(color * qLevels + 0.5) / qLevels;
            }
            
            color = clamp(color, 0.0, 1.0);
            fragColor = vec4(color, 1.0);
        }
    """.trimIndent()

    /**
     * Simple pass-through shader for FBO blit operations.
     */
    val PASSTHROUGH_FRAGMENT = """
        #version 300 es
        precision highp float;
        precision highp sampler2D;
        in vec2 vTexCoord;
        out vec4 fragColor;
        uniform sampler2D uTexture;
        void main() {
            fragColor = texture(uTexture, vTexCoord);
        }
    """.trimIndent()
}
