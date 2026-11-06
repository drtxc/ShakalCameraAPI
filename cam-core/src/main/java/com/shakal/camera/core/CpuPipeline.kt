package com.shakal.camera.core

import android.graphics.Bitmap
import android.graphics.Color
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.min

/**
 * CPU-side pipeline for processing captured photos.
 * 
 * Design choice: Uses Bitmap + IntArray pixel manipulation rather than RenderScript
 * (which is deprecated since API 31). The same transformations as the GL shader
 * are applied on CPU to ensure visual consistency between preview and saved photo.
 * 
 * For levels 1-6 this runs fast enough on modern devices.
 * Level 9 (forwarded_50) is the heaviest due to multiple JPEG passes.
 */
object CpuPipeline {

    /**
     * Process a captured bitmap through the full shakal pipeline.
     * 
     * @param sourceBitmap Full-resolution captured image
     * @param preset The current preset to apply
     * @return Processed bitmap at upscaled resolution (1280 on long side)
     */
    fun process(sourceBitmap: Bitmap, preset: Preset): Bitmap {
        // Step 1: Downscale to preset resolution (nearest neighbor)
        var current = nearestResize(sourceBitmap, preset.width, preset.height)

        // Step 2: Apply color transformations (same as GL shader)
        current = applyColorTransforms(current, preset)

        // Step 3: Apply noise
        current = applyNoise(current, preset)

        // Step 4: Apply block effect
        if (preset.blockSize > 0) {
            current = applyBlockEffect(current, preset)
        }

        // Step 5: Apply posterize
        if (preset.posterize > 0) {
            current = applyPosterize(current, preset.posterize)
        }

        // Step 6: Apply palette + dither (for four_tone)
        if (preset.paletteSize > 0) {
            current = applyPalette(current, preset.paletteSize, preset.dither > 0)
        }

        // Step 7: Apply vignette
        if (preset.vignette > 0f) {
            current = applyVignette(current, preset.vignette)
        }

        // Step 8: Apply hotspot
        if (preset.hotspot > 0f) {
            current = applyHotspot(current, preset.hotspot)
        }

        // Step 9: JPEG re-encoding passes (simulates quality degradation)
        current = jpegPasses(current, preset.jpegQuality, preset.jpegPasses)

        // Step 10: Upscale to 1280 on long side (nearest) for gallery visibility
        val longSide = max(current.width, current.height)
        val scale = 1280f / longSide
        val finalWidth = (current.width * scale).toInt()
        val finalHeight = (current.height * scale).toInt()
        current = nearestResize(current, finalWidth, finalHeight)

        return current
    }

    /**
     * Encode bitmap as JPEG with specified quality, stripping EXIF.
     */
    fun encodeJpeg(bitmap: Bitmap, quality: Int = 85): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        return stream.toByteArray()
    }

    private fun nearestResize(source: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        if (source.width == targetWidth && source.height == targetHeight) {
            return source.copy(source.config, false)
        }
        val result = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val srcPixels = IntArray(source.width * source.height)
        val dstPixels = IntArray(targetWidth * targetHeight)
        source.getPixels(srcPixels, 0, source.width, 0, 0, source.width, source.height)

        val xRatio = source.width.toFloat() / targetWidth
        val yRatio = source.height.toFloat() / targetHeight

        for (y in 0 until targetHeight) {
            val srcY = min((y * yRatio).toInt(), source.height - 1)
            for (x in 0 until targetWidth) {
                val srcX = min((x * xRatio).toInt(), source.width - 1)
                dstPixels[y * targetWidth + x] = srcPixels[srcY * source.width + srcX]
            }
        }

        result.setPixels(dstPixels, 0, targetWidth, 0, 0, targetWidth, targetHeight)
        return result
    }

    private fun applyColorTransforms(bitmap: Bitmap, preset: Preset): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val wbR = preset.wbShift[0]
        val wbG = preset.wbShift[1]
        val wbB = preset.wbShift[2]
        val sat = preset.saturation

        for (i in pixels.indices) {
            val pixel = pixels[i]
            var r = Color.red(pixel) / 255f
            var g = Color.green(pixel) / 255f
            var b = Color.blue(pixel) / 255f

            // White balance
            r *= wbR
            g *= wbG
            b *= wbB

            // Saturation
            val luma = 0.299f * r + 0.587f * g + 0.114f * b
            r = luma + (r - luma) * sat
            g = luma + (g - luma) * sat
            b = luma + (b - luma) * sat

            pixels[i] = Color.argb(
                Color.alpha(pixel),
                (clamp(r) * 255).toInt(),
                (clamp(g) * 255).toInt(),
                (clamp(b) * 255).toInt()
            )
        }

        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }

    private fun applyNoise(bitmap: Bitmap, preset: Preset): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val random = java.util.Random(42) // Deterministic for reproducibility

        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = Color.red(pixel) / 255f
            val g = Color.green(pixel) / 255f
            val b = Color.blue(pixel) / 255f
            val luma = 0.299f * r + 0.587f * g + 0.114f * b

            // Noise stronger in shadows
            val shadowFactor = 1f - luma
            val noiseAmount = preset.noise + preset.shadowNoise * shadowFactor

            val n = (random.nextFloat() - 0.5f) * 2f * noiseAmount

            pixels[i] = Color.argb(
                Color.alpha(pixel),
                (clamp(r + n) * 255).toInt(),
                (clamp(g + n) * 255).toInt(),
                (clamp(b + n) * 255).toInt()
            )
        }

        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }

    private fun applyBlockEffect(bitmap: Bitmap, preset: Preset): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val bs = preset.blockSize
        val qLevels = 8f

        for (blockY in 0 until height step bs) {
            for (blockX in 0 until width step bs) {
                // Average color in block
                var sumR = 0f; var sumG = 0f; var sumB = 0f; var count = 0
                for (y in blockY until min(blockY + bs, height)) {
                    for (x in blockX until min(blockX + bs, width)) {
                        val pixel = pixels[y * width + x]
                        sumR += Color.red(pixel)
                        sumG += Color.green(pixel)
                        sumB += Color.blue(pixel)
                        count++
                    }
                }
                val avgR = (sumR / count / 255f * qLevels).toInt() / qLevels
                val avgG = (sumG / count / 255f * qLevels).toInt() / qLevels
                val avgB = (sumB / count / 255f * qLevels).toInt() / qLevels

                // Fill block with quantized average
                val blockColor = Color.rgb(
                    (avgR * 255).toInt().coerceIn(0, 255),
                    (avgG * 255).toInt().coerceIn(0, 255),
                    (avgB * 255).toInt().coerceIn(0, 255)
                )
                for (y in blockY until min(blockY + bs, height)) {
                    for (x in blockX until min(blockX + bs, width)) {
                        pixels[y * width + x] = blockColor
                    }
                }
            }
        }

        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }

    private fun applyPosterize(bitmap: Bitmap, levels: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val l = levels.toFloat()
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = ((Color.red(pixel) / 255f * l + 0.5f).toInt() / l * 255).toInt().coerceIn(0, 255)
            val g = ((Color.green(pixel) / 255f * l + 0.5f).toInt() / l * 255).toInt().coerceIn(0, 255)
            val b = ((Color.blue(pixel) / 255f * l + 0.5f).toInt() / l * 255).toInt().coerceIn(0, 255)
            pixels[i] = Color.argb(Color.alpha(pixel), r, g, b)
        }

        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }

    private fun applyPalette(bitmap: Bitmap, paletteSize: Int, dither: Boolean): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val levels = paletteSize.toFloat()
        // Bayer 4x4 dither matrix
        val bayer = floatArrayOf(
            0f/16f, 8f/16f, 2f/16f, 10f/16f,
            12f/16f, 4f/16f, 14f/16f, 6f/16f,
            3f/16f, 11f/16f, 1f/16f, 9f/16f,
            15f/16f, 7f/16f, 13f/16f, 5f/16f
        )

        for (y in 0 until height) {
            for (x in 0 until width) {
                val idx = y * width + x
                val pixel = pixels[idx]
                val r = Color.red(pixel) / 255f
                val g = Color.green(pixel) / 255f
                val b = Color.blue(pixel) / 255f

                // Luma with olive tint
                var luma = 0.299f * r + 0.587f * g + 0.114f * b

                if (dither) {
                    val bayerIdx = (x % 4) + (y % 4) * 4
                    luma += (bayer[bayerIdx] - 0.5f) / levels
                }

                luma = (luma * levels).toInt().toFloat() / (levels - 1f)
                luma = clamp(luma)

                // Olive tint
                pixels[idx] = Color.argb(
                    Color.alpha(pixel),
                    (luma * 0.95f * 255).toInt().coerceIn(0, 255),
                    (luma * 1.02f * 255).toInt().coerceIn(0, 255),
                    (luma * 0.85f * 255).toInt().coerceIn(0, 255)
                )
            }
        }

        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }

    private fun applyVignette(bitmap: Bitmap, strength: Float): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val cx = width / 2f
        val cy = height / 2f
        val maxDist = kotlin.math.sqrt(cx * cx + cy * cy)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val dx = (x - cx) / cx * 0.5f
                val dy = (y - cy) / cy * 0.5f
                val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                var vig = 1f - dist * dist
                vig = Math.pow(vig.toDouble(), (1.0 + strength * 3.0)).toFloat()
                vig = clamp(vig)

                val idx = y * width + x
                val pixel = pixels[idx]
                pixels[idx] = Color.argb(
                    Color.alpha(pixel),
                    (Color.red(pixel) * vig).toInt().coerceIn(0, 255),
                    (Color.green(pixel) * vig).toInt().coerceIn(0, 255),
                    (Color.blue(pixel) * vig).toInt().coerceIn(0, 255)
                )
            }
        }

        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }

    private fun applyHotspot(bitmap: Bitmap, strength: Float): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val cx = width / 2f
        val cy = height / 2f

        for (y in 0 until height) {
            for (x in 0 until width) {
                val dx = (x - cx) / width
                val dy = (y - cy) / height
                val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                val hot = Math.exp((-dist * dist * 8.0).toDouble()).toFloat() * strength

                val idx = y * width + x
                val pixel = pixels[idx]
                pixels[idx] = Color.argb(
                    Color.alpha(pixel),
                    (Color.red(pixel) + hot * 0.8f * 255).toInt().coerceIn(0, 255),
                    (Color.green(pixel) + hot * 0.7f * 255).toInt().coerceIn(0, 255),
                    (Color.blue(pixel) + hot * 0.5f * 255).toInt().coerceIn(0, 255)
                )
            }
        }

        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }

    /**
     * Simulates quality degradation through repeated JPEG encode/decode cycles.
     */
    private fun jpegPasses(bitmap: Bitmap, quality: Int, passes: Int): Bitmap {
        var current = bitmap
        for (i in 0 until passes) {
            val stream = ByteArrayOutputStream()
            current.compress(Bitmap.CompressFormat.JPEG, quality, stream)
            val bytes = stream.toByteArray()
            current = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }
        return current
    }

    private fun clamp(value: Float): Float = max(0f, min(1f, value))
}
