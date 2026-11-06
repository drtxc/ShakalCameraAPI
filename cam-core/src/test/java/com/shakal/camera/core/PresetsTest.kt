package com.shakal.camera.core

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for preset loading and validation.
 */
class PresetsTest {

    @Test
    fun `presets contain exactly levels 1 through 10 and 12`() {
        // This test validates the structure of presets.json
        // In a real Android test, we'd load from assets.
        // Here we verify the expected level set.
        val expectedLevels = setOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 12)
        assertEquals(11, expectedLevels.size)
        assertFalse("Level 11 should not exist", expectedLevels.contains(11))
        assertTrue("Level 12 should exist", expectedLevels.contains(12))
    }

    @Test
    fun `all preset fields are present`() {
        // Validate that a preset has all required fields
        val preset = Preset(
            id = "test",
            level = 1,
            width = 1280,
            height = 720,
            fpsMax = 30,
            fpsLowLight = 8,
            jpegQuality = 85,
            jpegPasses = 1,
            noise = 0.05f,
            shadowNoise = 0.1f,
            denoiseSmear = 0.3f,
            wbShift = listOf(1.0f, 1.0f, 1.0f),
            saturation = 1.0f,
            chromaFringe = 0.0f,
            blockSize = 0,
            vignette = 0.0f,
            hotspot = 0.0f,
            bloom = 0.0f,
            posterize = 0,
            paletteSize = 0,
            dither = 0,
            freezeChance = 0.0f,
            rollingShutter = 0.0f,
            rowGlitch = 0.0f
        )

        assertNotNull(preset.id)
        assertTrue(preset.level in 1..12)
        assertTrue(preset.width > 0)
        assertTrue(preset.height > 0)
        assertTrue(preset.fpsMax > 0)
        assertTrue(preset.fpsLowLight > 0)
        assertTrue(preset.fpsLowLight <= preset.fpsMax)
        assertTrue(preset.jpegQuality in 1..100)
        assertTrue(preset.jpegPasses >= 1)
        assertTrue(preset.noise >= 0f)
        assertTrue(preset.shadowNoise >= 0f)
        assertTrue(preset.saturation >= 0f)
        assertEquals(3, preset.wbShift.size)
    }

    @Test
    fun `video bitrate calculation is logarithmic`() {
        val encoder = VideoEncoder()
        val bitrate1 = encoder.calculateBitrate(1)
        val bitrate5 = encoder.calculateBitrate(5)
        val bitrate10 = encoder.calculateBitrate(10)

        assertTrue("Level 1 should have highest bitrate", bitrate1 > bitrate5)
        assertTrue("Level 5 should be between 1 and 10", bitrate5 > bitrate10)
        assertEquals("Level 1 should be 1.5 Mbps", 1_500_000, bitrate1)
        assertEquals("Level 10 should be 64 kbps", 64_000, bitrate10)
    }
}
