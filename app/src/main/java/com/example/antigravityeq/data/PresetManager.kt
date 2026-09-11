package com.example.antigravityeq.data

object PresetManager {
    data class StudioPreset(
        val id: String,
        val name: String,
        val subtitle: String,
        val badge: String,
        val description: String,
        val apply: (EqualizerSettings) -> EqualizerSettings
    )

    val PRESETS = listOf(
        StudioPreset(
            id = "audiophile_ref",
            name = "Audiophile Reference 180°",
            subtitle = "Harman Studio Stage",
            badge = "REF",
            description = "Dead-center vocal lock with expansive 180° instrument wings, smooth neutral EQ, and high-frequency air.",
            apply = { s ->
                s.copy(
                    isEqEnabled = true,
                    eqPreset = 1, // Acoustic
                    bandLevels = listOf(2, 2, 1, 0, 0, 1, 2, 3, 4, 3),
                    isSpatialAudioEnabled = true,
                    spatialAudioMode = 0, // 360° Sphere
                    spatialAudioAngle = 180,
                    spatialDirection = 1, // Front-Center 180°
                    instrumentSeparation = 85,
                    isClarityEnabled = true,
                    clarityMode = 0, // Natural
                    clarity = 400,
                    isBassEnabled = false
                )
            }
        ),
        StudioPreset(
            id = "sub_earthquake",
            name = "Subwoofer Earthquake",
            subtitle = "Pure Sub-Bass & Tube Warmth",
            badge = "SUB",
            description = "Deep physical sub-bass rumble (<60Hz) coupled with 12AX7 tube harmonics and 0dB vocal protection.",
            apply = { s ->
                s.copy(
                    isBassEnabled = true,
                    viperBassMode = 2, // Subwoofer
                    bassFrequency = 45,
                    bassBoost = 850,
                    isTubeEnabled = true,
                    tubeWarmth = 600,
                    isDynamicSystemEnabled = true,
                    dynamicBassStrength = 22,
                    isEqEnabled = true,
                    bandLevels = listOf(8, 7, 4, 1, 0, 0, 1, 2, 3, 2),
                    isClarityEnabled = false
                )
            }
        ),
        StudioPreset(
            id = "imax_3d",
            name = "IMAX 3D Cinema Theater",
            subtitle = "Dolby Atmos Spatial Stage",
            badge = "3D",
            description = "Massive front soundstage projection, dialogue intelligibility boost, overhead height channels, and cavernous hall reflections.",
            apply = { s ->
                s.copy(
                    isSpatialAudioEnabled = true,
                    spatialAudioMode = 1, // Dolby Atmos Cinema Stage
                    spatialAudioAngle = 180,
                    spatialDirection = 1, // Front-Center
                    instrumentSeparation = 90,
                    isReverbEnabled = true,
                    reverbRoomSize = 350,
                    reverbWetRatio = 25,
                    reverbSoundField = 25,
                    isFieldSurroundEnabled = true,
                    fieldSurroundStrength = 75,
                    midImageSize = 65,
                    isTransientShaperEnabled = true,
                    transientAttack = 40
                )
            }
        ),
        StudioPreset(
            id = "vocal_crystal",
            name = "Crystalline Vocal & Air",
            subtitle = "XHiFi Pro & 16kHz Shimmer",
            badge = "AIR",
            description = "Pristine vocal intelligibility, sibilance softening, and cubic harmonic high-frequency restoration.",
            apply = { s ->
                s.copy(
                    isClarityEnabled = true,
                    clarityMode = 2, // XHiFi Pro
                    clarity = 750,
                    isSpectrumExtensionEnabled = true,
                    spectrumExtensionStrength = 6,
                    isAuditoryProtectionEnabled = true,
                    isEqEnabled = true,
                    bandLevels = listOf(0, 0, 0, 1, 2, 3, 4, 4, 5, 5),
                    isTransientShaperEnabled = true,
                    transientAttack = 25,
                    transientSpeed = 0 // Fast
                )
            }
        )
    )
}
