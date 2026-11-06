package com.shakal.camera.core

import android.util.Size

/**
 * Brightness analyzer using CameraX ImageAnalysis.
 * Measures average luminance from the Y plane of the image.
 * Used to determine if we should drop to fpsLowLight.
 */
object BrightnessAnalyzer {

    private var lastMeasurementTime: Long = 0
    private var currentBrightness: Float = 0.5f
    private const val MEASUREMENT_INTERVAL_MS = 500L

    /**
     * Calculate average brightness from Y plane pixels.
     * Call this from ImageAnalysis.Analyzer on a background thread.
     * 
     * @param yBuffer Y plane ByteBuffer from ImageProxy
     * @param width Image width
     * @param height Image height
     * @param rowStride Row stride of the Y plane
     * @return Average brightness 0.0 (black) to 1.0 (white)
     */
    fun analyze(yBuffer: java.nio.ByteBuffer, width: Int, height: Int, rowStride: Int): Float {
        val now = System.currentTimeMillis()
        if (now - lastMeasurementTime < MEASUREMENT_INTERVAL_MS) {
            return currentBrightness
        }
        lastMeasurementTime = now

        // Sample every 8th pixel for performance
        var sum = 0L
        var count = 0
        val step = 8

        for (y in 0 until height step step) {
            for (x in 0 until width step step) {
                val index = y * rowStride + x
                if (index < yBuffer.capacity()) {
                    sum += (yBuffer.get(index).toInt() and 0xFF)
                    count++
                }
            }
        }

        currentBrightness = if (count > 0) {
            (sum.toFloat() / count) / 255f
        } else {
            0.5f
        }

        return currentBrightness
    }

    fun getBrightness(): Float = currentBrightness

    /**
     * Determine if current brightness is "low light" (below threshold).
     */
    fun isLowLight(threshold: Float = 0.25f): Boolean {
        return currentBrightness < threshold
    }

    /**
     * Get the target FPS based on brightness and preset.
     */
    fun getTargetFps(preset: Preset): Int {
        return if (isLowLight()) preset.fpsLowLight else preset.fpsMax
    }
}
