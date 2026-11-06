package com.shakal.camera.core

import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES30
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * OpenGL ES 3.0 renderer that applies "shakal" effects to camera frames.
 * 
 * Pipeline:
 * 1. Camera OES texture → FBO at preset resolution (nearest downscale)
 * 2. Apply all shader effects (color, noise, blocks, fringe, vignette, etc.)
 * 3. Upscale to screen (nearest)
 * 
 * Frame rate limiting is done externally by skipping draw calls.
 */
class ShakalGlRenderer {

    private var programId: Int = 0
    private var fboId: Int = 0
    private var fboTextureId: Int = 0
    private var cameraTextureId: Int = 0
    private var quadVbo: Int = 0
    private var quadVao: Int = 0

    private var presetWidth: Int = 640
    private var presetHeight: Int = 480
    private var screenWidth: Int = 0
    private var screenHeight: Int = 0

    // Uniform locations
    private var uTextureLoc: Int = -1
    private var uResolutionLoc: Int = -1
    private var uTexelSizeLoc: Int = -1
    private var uTimeLoc: Int = -1
    private var uBrightnessLoc: Int = -1
    private var uWbShiftLoc: Int = -1
    private var uSaturationLoc: Int = -1
    private var uNoiseLoc: Int = -1
    private var uShadowNoiseLoc: Int = -1
    private var uDenoiseSmearLoc: Int = -1
    private var uChromaFringeLoc: Int = -1
    private var uBlockSizeLoc: Int = -1
    private var uVignetteLoc: Int = -1
    private var uHotspotLoc: Int = -1
    private var uBloomLoc: Int = -1
    private var uPosterizeLoc: Int = -1
    private var uPaletteSizeLoc: Int = -1
    private var uDitherLoc: Int = -1
    private var uRollingShutterLoc: Int = -1
    private var uRowGlitchLoc: Int = -1

    private var startTime: Long = System.nanoTime()
    private var currentPreset: Preset? = null

    // Frame buffer for freeze effect
    private var freezeBufferId: Int = 0
    private var isFrozen: Boolean = false
    private var freezeTextureId: Int = 0

    fun init() {
        // Create camera OES texture
        cameraTextureId = createOesTexture()

        // Compile and link shader program
        val vertexShader = compileShader(GLES30.GL_VERTEX_SHADER, ShaderSources.VERTEX_SHADER)
        val fragmentShader = compileShader(GLES30.GL_FRAGMENT_SHADER, ShaderSources.FRAGMENT_SHADER)
        programId = linkProgram(vertexShader, fragmentShader)

        // Get uniform locations
        uTextureLoc = GLES30.glGetUniformLocation(programId, "uTexture")
        uResolutionLoc = GLES30.glGetUniformLocation(programId, "uResolution")
        uTexelSizeLoc = GLES30.glGetUniformLocation(programId, "uTexelSize")
        uTimeLoc = GLES30.glGetUniformLocation(programId, "uTime")
        uBrightnessLoc = GLES30.glGetUniformLocation(programId, "uBrightness")
        uWbShiftLoc = GLES30.glGetUniformLocation(programId, "uWbShift")
        uSaturationLoc = GLES30.glGetUniformLocation(programId, "uSaturation")
        uNoiseLoc = GLES30.glGetUniformLocation(programId, "uNoise")
        uShadowNoiseLoc = GLES30.glGetUniformLocation(programId, "uShadowNoise")
        uDenoiseSmearLoc = GLES30.glGetUniformLocation(programId, "uDenoiseSmear")
        uChromaFringeLoc = GLES30.glGetUniformLocation(programId, "uChromaFringe")
        uBlockSizeLoc = GLES30.glGetUniformLocation(programId, "uBlockSize")
        uVignetteLoc = GLES30.glGetUniformLocation(programId, "uVignette")
        uHotspotLoc = GLES30.glGetUniformLocation(programId, "uHotspot")
        uBloomLoc = GLES30.glGetUniformLocation(programId, "uBloom")
        uPosterizeLoc = GLES30.glGetUniformLocation(programId, "uPosterize")
        uPaletteSizeLoc = GLES30.glGetUniformLocation(programId, "uPaletteSize")
        uDitherLoc = GLES30.glGetUniformLocation(programId, "uDither")
        uRollingShutterLoc = GLES30.glGetUniformLocation(programId, "uRollingShutter")
        uRowGlitchLoc = GLES30.glGetUniformLocation(programId, "uRowGlitch")

        // Create fullscreen quad
        createQuad()

        // Create FBO for preset-resolution rendering
        createFbo(640, 480)
    }

    fun getCameraTextureId(): Int = cameraTextureId

    fun createSurfaceTexture(): SurfaceTexture {
        return SurfaceTexture(cameraTextureId).apply {
            setOnFrameAvailableListener { }
        }
    }

    fun setPreset(preset: Preset) {
        currentPreset = preset
        if (preset.width != presetWidth || preset.height != presetHeight) {
            presetWidth = preset.width
            presetHeight = preset.height
            createFbo(presetWidth, presetHeight)
        }
    }

    fun setScreenSize(width: Int, height: Int) {
        screenWidth = width
        screenHeight = height
    }

    fun drawFrame(surfaceTexture: SurfaceTexture) {
        surfaceTexture.updateTexImage()

        val preset = currentPreset ?: return

        val time = (System.nanoTime() - startTime) / 1_000_000_000f

        // Freeze effect: skip update if frozen
        if (preset.freezeChance > 0f) {
            if (!isFrozen && Math.random() < preset.freezeChance * 0.016) {
                isFrozen = true
                // Will remain frozen for a random duration
            }
        }

        // Render to FBO at preset resolution
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fboId)
        GLES30.glViewport(0, 0, presetWidth, presetHeight)

        GLES30.glUseProgram(programId)

        // Bind camera texture
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, cameraTextureId)
        GLES30.glUniform1i(uTextureLoc, 0)

        // Set uniforms
        GLES30.glUniform2f(uResolutionLoc, presetWidth.toFloat(), presetHeight.toFloat())
        GLES30.glUniform2f(uTexelSizeLoc, 1f / presetWidth, 1f / presetHeight)
        GLES30.glUniform1f(uTimeLoc, time)
        GLES30.glUniform1f(uBrightnessLoc, 0.5f) // Updated externally

        // Preset parameters
        val wb = preset.wbShift
        GLES30.glUniform3f(uWbShiftLoc, wb[0], wb[1], wb[2])
        GLES30.glUniform1f(uSaturationLoc, preset.saturation)
        GLES30.glUniform1f(uNoiseLoc, preset.noise)
        GLES30.glUniform1f(uShadowNoiseLoc, preset.shadowNoise)
        GLES30.glUniform1f(uDenoiseSmearLoc, preset.denoiseSmear)
        GLES30.glUniform1f(uChromaFringeLoc, preset.chromaFringe)
        GLES30.glUniform1i(uBlockSizeLoc, preset.blockSize)
        GLES30.glUniform1f(uVignetteLoc, preset.vignette)
        GLES30.glUniform1f(uHotspotLoc, preset.hotspot)
        GLES30.glUniform1f(uBloomLoc, preset.bloom)
        GLES30.glUniform1i(uPosterizeLoc, preset.posterize)
        GLES30.glUniform1i(uPaletteSizeLoc, preset.paletteSize)
        GLES30.glUniform1i(uDitherLoc, preset.dither)
        GLES30.glUniform1f(uRollingShutterLoc, preset.rollingShutter)
        GLES30.glUniform1f(uRowGlitchLoc, preset.rowGlitch)

        // Draw fullscreen quad
        GLES30.glBindVertexArray(quadVao)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
        GLES30.glBindVertexArray(0)

        // Blit FBO to screen with nearest upscale
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        GLES30.glViewport(0, 0, screenWidth, screenHeight)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, fboTextureId)
        // Use passthrough or just blit
        GLES30.glBindFramebuffer(GLES30.GL_READ_FRAMEBUFFER, fboId)
        GLES30.glBindFramebuffer(GLES30.GL_DRAW_FRAMEBUFFER, 0)
        GLES30.glBlitFramebuffer(
            0, 0, presetWidth, presetHeight,
            0, 0, screenWidth, screenHeight,
            GLES30.GL_COLOR_BUFFER_BIT, GLES30.GL_NEAREST
        )
        GLES30.glBindFramebuffer(GLES30.GL_READ_FRAMEBUFFER, 0)
    }

    /**
     * Read the current FBO content as pixel data for photo capture.
     * Returns IntArray of ARGB pixels at preset resolution.
     */
    fun readPixels(): IntArray {
        val buffer = ByteBuffer.allocate(presetWidth * presetHeight * 4)
        GLES30.glReadPixels(0, 0, presetWidth, presetHeight, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, buffer)
        val pixels = IntArray(presetWidth * presetHeight)
        buffer.asIntBuffer().get(pixels)
        // Flip vertically (OpenGL origin is bottom-left)
        val flipped = IntArray(pixels.size)
        for (y in 0 until presetHeight) {
            System.arraycopy(pixels, y * presetWidth, flipped, (presetHeight - 1 - y) * presetWidth, presetWidth)
        }
        return flipped
    }

    fun getPresetWidth(): Int = presetWidth
    fun getPresetHeight(): Int = presetHeight

    fun cleanup() {
        if (programId != 0) GLES30.glDeleteProgram(programId)
        if (fboId != 0) GLES30.glDeleteFramebuffers(1, intArrayOf(fboId))
        if (fboTextureId != 0) GLES30.glDeleteTextures(1, intArrayOf(fboTextureId))
        if (cameraTextureId != 0) GLES30.glDeleteTextures(1, intArrayOf(cameraTextureId))
        if (quadVbo != 0) GLES30.glDeleteBuffers(1, intArrayOf(quadVbo))
        if (quadVao != 0) GLES30.glDeleteVertexArrays(1, intArrayOf(quadVao))
    }

    private fun createOesTexture(): Int {
        val textures = IntArray(1)
        GLES30.glGenTextures(1, textures, 0)
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textures[0])
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
        return textures[0]
    }

    private fun createFbo(width: Int, height: Int) {
        // Delete old FBO if exists
        if (fboId != 0) {
            GLES30.glDeleteFramebuffers(1, intArrayOf(fboId))
            GLES30.glDeleteTextures(1, intArrayOf(fboTextureId))
        }

        // Create texture
        val textures = IntArray(1)
        GLES30.glGenTextures(1, textures, 0)
        fboTextureId = textures[0]
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, fboTextureId)
        GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA, width, height, 0,
            GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, null)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_NEAREST)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)

        // Create FBO
        val fbos = IntArray(1)
        GLES30.glGenFramebuffers(1, fbos, 0)
        fboId = fbos[0]
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fboId)
        GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0,
            GLES30.GL_TEXTURE_2D, fboTextureId, 0)
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
    }

    private fun createQuad() {
        // Fullscreen quad: position (xy) + texcoord (uv)
        val vertices = floatArrayOf(
            -1f, -1f,  0f, 0f,
             1f, -1f,  1f, 0f,
            -1f,  1f,  0f, 1f,
             1f,  1f,  1f, 1f,
        )

        val buffer = ByteBuffer.allocateDirect(vertices.size * 4)
        buffer.order(ByteOrder.nativeOrder())
        val floatBuffer = buffer.asFloatBuffer()
        floatBuffer.put(vertices)
        floatBuffer.position(0)

        val buffers = IntArray(1)
        GLES30.glGenBuffers(1, buffers, 0)
        quadVbo = buffers[0]
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, quadVbo)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vertices.size * 4, floatBuffer, GLES30.GL_STATIC_DRAW)

        val vaos = IntArray(1)
        GLES30.glGenVertexArrays(1, vaos, 0)
        quadVao = vaos[0]
        GLES30.glBindVertexArray(quadVao)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, quadVbo)
        // Position attribute (location 0)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 16, 0)
        // TexCoord attribute (location 1)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, 16, 8)
        GLES30.glBindVertexArray(0)
    }

    private fun compileShader(type: Int, source: String): Int {
        val shader = GLES30.glCreateShader(type)
        GLES30.glShaderSource(shader, source)
        GLES30.glCompileShader(shader)
        val status = IntArray(1)
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, status, 0)
        if (status[0] == 0) {
            val log = GLES30.glGetShaderInfoLog(shader)
            GLES30.glDeleteShader(shader)
            throw RuntimeException("Shader compile error: $log")
        }
        return shader
    }

    private fun linkProgram(vertexShader: Int, fragmentShader: Int): Int {
        val program = GLES30.glCreateProgram()
        GLES30.glAttachShader(program, vertexShader)
        GLES30.glAttachShader(program, fragmentShader)
        GLES30.glLinkProgram(program)
        val status = IntArray(1)
        GLES30.glGetProgramiv(program, GLES30.GL_LINK_STATUS, status, 0)
        if (status[0] == 0) {
            val log = GLES30.glGetProgramInfoLog(program)
            GLES30.glDeleteProgram(program)
            throw RuntimeException("Program link error: $log")
        }
        GLES30.glDeleteShader(vertexShader)
        GLES30.glDeleteShader(fragmentShader)
        return program
    }
}
