package com.shakal.camera.core

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

/**
 * Data class representing a single "shakal" quality preset.
 * All parameters are loaded from presets.json — nothing hardcoded in shaders.
 */
@Serializable
data class Preset(
    val id: String,
    val level: Int,
    val width: Int,
    val height: Int,
    val fpsMax: Int,
    val fpsLowLight: Int,
    val jpegQuality: Int,
    val jpegPasses: Int,
    val noise: Float,
    val shadowNoise: Float,
    val denoiseSmear: Float,
    val wbShift: List<Float>,   // [r, g, b]
    val saturation: Float,
    val chromaFringe: Float,
    val blockSize: Int,
    val vignette: Float,
    val hotspot: Float,
    val bloom: Float,
    val posterize: Int,
    val paletteSize: Int,
    val dither: Int,
    val freezeChance: Float,
    val rollingShutter: Float,
    val rowGlitch: Float
)

/**
 * Loads and caches presets from assets/presets.json.
 */
object PresetLoader {
    private var cachedPresets: List<Preset>? = null
    private val json = Json { ignoreUnknownKeys = true }

    fun load(context: Context): List<Preset> {
        cachedPresets?.let { return it }
        val jsonStr = context.assets.open("presets.json").bufferedReader().use { it.readText() }
        val presets = json.decodeFromString<List<Preset>>(jsonStr)
        cachedPresets = presets
        return presets
    }

    fun getByLevel(level: Int): Preset? {
        return cachedPresets?.find { it.level == level }
    }

    fun getLevels(): List<Int> {
        return cachedPresets?.map { it.level } ?: listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 12)
    }
}
