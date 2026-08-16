package com.example.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

data class LauncherIconVariant(
  val id: String,
  val title: String,
  val subtitle: String,
  val aliasClass: String,
  val category: String = "General",
  val primaryColorHex: Long = 0xFF141414,
  val accentColorHex: Long = 0xFF38BDF8
)

class IconChangerEngine(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager
    private val basePackageToken = "com.example.MainActivity"

    // COMPACT MATRIX MAPPING: Condenses all 50 entries into clean, un-duplicated short string suffixes
    private val compressedThemeTags = linkedSetOf(
        "Default", "MidnightPrism", "GothMatrix", "CeramicMatte", "BlurpleTwilight", "SunsetGlow",
        "CrimsonGlow", "LightFrost", "AeroClassic", "PureOLED", "NeonSynthwave", "SolarFlare",
        "DesertSage", "Cyberpunk2077", "RadObsidian", "RetroCRT", "GlitchOverdrive", "TokyoDrift",
        "CarbonFiber", "NordicBlizzard", "MonochromeMinimal", "UbuntuOrange", "DraculaCore", "GruvboxHard",
        "SteelFoundry", "HydraCyan", "RustOxide", "SolarizedAbyss", "SunsetGlowAlt", "GoldLeaf",
        "RoyalAmethyst", "EmeraldVault", "CopperCircuit", "PlatinumSilk", "CrimsonShadow", "FrozenTundra",
        "GunmetalHeavy", "NvidiaShield", "PlayStationClassic", "XboxCommand", "GameBoyPocket", "NukaQuantum",
        "DeepSpace", "VolcanicAsh", "GhostProtocol", "SubzeroFrost", "RedlineRacing", "VintageParchment",
        "BioHazard", "NeonMirage", "ChromaEclipse", "JackOverlord"
    )

    /**
     * Programmatically activates chosen launcher themes using a compressed single-string builder loop.
     * Prevents browser simulator memory choke bugs while offering full 50-theme deployment.
     */
    fun safelyDeployTargetTheme(themeSuffix: String) {
        val cleanSuffix = themeSuffix.removePrefix(basePackageToken).trim()
        if (!compressedThemeTags.contains(cleanSuffix)) return

        compressedThemeTags.forEach { currentSuffix ->
            // Dynamically construct full activity alias strings programmatically
            val fullComponentNamePath = "$basePackageToken$currentSuffix"
            val componentName = ComponentName(context, fullComponentNamePath)
            
            val configurationStateSetting = if (currentSuffix == cleanSuffix) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }

            try {
                packageManager.setComponentEnabledSetting(
                    componentName,
                    configurationStateSetting,
                    PackageManager.DONT_KILL_APP
                )
            } catch (_: Exception) {
                // Safeguard against missing alias declaration in manifest
            }
        }
    }

    /**
     * Backward-compatible alias delegate
     */
    fun safelySwitchLauncherIcon(targetAliasName: String) {
        val suffix = targetAliasName.removePrefix(basePackageToken)
        safelyDeployTargetTheme(suffix)
    }

    companion object {
        val ICON_VARIANTS: List<LauncherIconVariant> = listOf(
            // 1–10: Core System Baselines
            LauncherIconVariant("default", "Default System", "Classic dark emerald folder with document tabs", "com.example.MainActivityDefault", "Core Baselines", 0xFF006738, 0xFF4ECB98),
            LauncherIconVariant("midnight_prism", "Midnight Prism", "Deep obsidian backdrop with electric cyan prism glow", "com.example.MainActivityMidnightPrism", "Core Baselines", 0xFF0F172A, 0xFF38BDF8),
            LauncherIconVariant("goth_matrix", "Goth Matrix", "Pitch black backdrop with cyber terminal neon green", "com.example.MainActivityGothMatrix", "Core Baselines", 0xFF050505, 0xFF00FF66),
            LauncherIconVariant("ceramic_matte", "Ceramic Matte", "Minimalist matte charcoal with stark titanium white", "com.example.MainActivityCeramicMatte", "Core Baselines", 0xFF18181B, 0xFFF4F4F5),
            LauncherIconVariant("blurple_twilight", "Blurple Twilight", "Deep indigo nightshade with vivid royal violet", "com.example.MainActivityBlurpleTwilight", "Core Baselines", 0xFF1E1B4B, 0xFF818CF8),
            LauncherIconVariant("sunset_glow", "Sunset Glow", "Warm crimson dusk with radiant amber horizon", "com.example.MainActivitySunsetGlow", "Core Baselines", 0xFF450A0A, 0xFFF97316),
            LauncherIconVariant("crimson_glow", "Crimson Glow", "Pure obsidian black with intense crimson red aura", "com.example.MainActivityCrimsonGlow", "Core Baselines", 0xFF2A0808, 0xFFEF4444),
            LauncherIconVariant("light_frost", "Light Frost", "Crisp glacial white layout with horizon cobalt tint", "com.example.MainActivityLightFrost", "Core Baselines", 0xFFF0F9FF, 0xFF0284C7),
            LauncherIconVariant("aero_classic", "Aero Classic", "Refined deep navy blue with sapphire aero gloss", "com.example.MainActivityAeroClassic", "Core Baselines", 0xFF1E3A8A, 0xFF60A5FA),
            LauncherIconVariant("pure_oled", "Pure OLED", "Pure #000000 black canvas for peak battery endurance", "com.example.MainActivityPureOLED", "Core Baselines", 0xFF000000, 0xFFFFFFFF),

            // 11–18: Cyberpunk & Retro Terminals
            LauncherIconVariant("neon_synthwave", "Neon Synthwave", "Retro violet nightscape with hot magenta pink accents", "com.example.MainActivityNeonSynthwave", "Cyberpunk & Retro", 0xFF1E1035, 0xFFF43F5E),
            LauncherIconVariant("solar_flare", "Solar Flare", "Volcanic dark magma with intense solar corona flare", "com.example.MainActivitySolarFlare", "Cyberpunk & Retro", 0xFF2B1100, 0xFFFB923C),
            LauncherIconVariant("desert_sage", "Desert Sage", "Earthy dark sage camouflage with pale olive cream", "com.example.MainActivityDesertSage", "Cyberpunk & Retro", 0xFF1E2822, 0xFF84CC16),
            LauncherIconVariant("cyberpunk_2077", "Cyberpunk 2077", "Stark asphalt graphite with electric neon yellow", "com.example.MainActivityCyberpunk2077", "Cyberpunk & Retro", 0xFF0D0D11, 0xFFFCEE0A),
            LauncherIconVariant("rad_obsidian", "Rad Obsidian", "Obsidian velvet with intense ultraviolet radiant crest", "com.example.MainActivityRadObsidian", "Cyberpunk & Retro", 0xFF111115, 0xFFA855F7),
            LauncherIconVariant("retro_crt", "Retro CRT", "Vintage green phosphor monitor scanline emulator", "com.example.MainActivityRetroCRT", "Cyberpunk & Retro", 0xFF0A1A0A, 0xFF39FF14),
            LauncherIconVariant("glitch_overdrive", "Glitch Overdrive", "Dark cyber glitch matrix with electric cyan highlights", "com.example.MainActivityGlitchOverdrive", "Cyberpunk & Retro", 0xFF120024, 0xFF00FFFF),
            LauncherIconVariant("tokyo_drift", "Tokyo Drift", "Midnight neon magenta with high-octane racing pink", "com.example.MainActivityTokyoDrift", "Cyberpunk & Retro", 0xFF1A001A, 0xFFFF007F),

            // 19–28: Industrial & Developer Palettes
            LauncherIconVariant("carbon_fiber", "Carbon Fiber", "Woven composite charcoal weave with titanium slate", "com.example.MainActivityCarbonFiber", "Industrial & Dev", 0xFF1F1F1F, 0xFF9CA3AF),
            LauncherIconVariant("nordic_blizzard", "Nordic Blizzard", "Deep Arctic ocean slate with glacial frost cyan", "com.example.MainActivityNordicBlizzard", "Industrial & Dev", 0xFF2E3440, 0xFF88C0D0),
            LauncherIconVariant("monochrome_minimal", "Monochrome Minimal", "Clean balanced grayscale with stark contrast edge", "com.example.MainActivityMonochromeMinimal", "Industrial & Dev", 0xFF121212, 0xFFE0E0E0),
            LauncherIconVariant("ubuntu_orange", "Ubuntu Orange", "Canonical aubergine backing with iconic warm orange", "com.example.MainActivityUbuntuOrange", "Industrial & Dev", 0xFF300A24, 0xFFE95420),
            LauncherIconVariant("dracula_core", "Dracula Core", "Dark gothic slate with soft pastel purple & pink", "com.example.MainActivityDraculaCore", "Industrial & Dev", 0xFF282A36, 0xFFFF79C6),
            LauncherIconVariant("gruvbox_hard", "Gruvbox Hard", "Retro warm dark earth with golden yellow accents", "com.example.MainActivityGruvboxHard", "Industrial & Dev", 0xFF1D2021, 0xFFFABD2F),
            LauncherIconVariant("steel_foundry", "Steel Foundry", "Heavy industrial steel plate with brushed graphite", "com.example.MainActivitySteelFoundry", "Industrial & Dev", 0xFF27272A, 0xFF71717A),
            LauncherIconVariant("hydra_cyan", "Hydra Cyan", "Abyssal trench navy with bioluminescent aqua cyan", "com.example.MainActivityHydraCyan", "Industrial & Dev", 0xFF082F49, 0xFF06B6D4),
            LauncherIconVariant("rust_oxide", "Rust Oxide", "Deep weathered copper oxide with burnt amber rust", "com.example.MainActivityRustOxide", "Industrial & Dev", 0xFF3B180A, 0xFFD97706),
            LauncherIconVariant("solarized_abyss", "Solarized Abyss", "Precision solarized cyan-slate for terminal clarity", "com.example.MainActivitySolarizedAbyss", "Industrial & Dev", 0xFF002B36, 0xFF268BD2),

            // 29–37: Premium Materials & Elements
            LauncherIconVariant("sunset_glow_alt", "Sunset Glow Alt", "Radiant sunset horizon with vivid ruby ember tint", "com.example.MainActivitySunsetGlowAlt", "Premium Materials", 0xFF4A0E17, 0xFFFF6B6B),
            LauncherIconVariant("gold_leaf", "Gold Leaf", "Gilded 24K imperial gold sheen over onyx stone", "com.example.MainActivityGoldLeaf", "Premium Materials", 0xFF1C1917, 0xFFEAB308),
            LauncherIconVariant("royal_amethyst", "Royal Amethyst", "Imperial monarch purple with radiant crystal gem glow", "com.example.MainActivityRoyalAmethyst", "Premium Materials", 0xFF2E0854, 0xFFC084FC),
            LauncherIconVariant("emerald_vault", "Emerald Vault", "Deep vault green with brilliant cut emerald facets", "com.example.MainActivityEmeraldVault", "Premium Materials", 0xFF022C22, 0xFF10B981),
            LauncherIconVariant("copper_circuit", "Copper Circuit", "Raw conductive copper traces with warm metallic bronze", "com.example.MainActivityCopperCircuit", "Premium Materials", 0xFF2B1810, 0xFFB45309),
            LauncherIconVariant("platinum_silk", "Platinum Silk", "Brushed lustrous platinum silver with cool chrome edge", "com.example.MainActivityPlatinumSilk", "Premium Materials", 0xFF1E293B, 0xFFCBD5E1),
            LauncherIconVariant("crimson_shadow", "Crimson Shadow", "Vampiric dark bloodwood with vibrant scarlet borders", "com.example.MainActivityCrimsonShadow", "Premium Materials", 0xFF1A0A0E, 0xFFDC2626),
            LauncherIconVariant("frozen_tundra", "Frozen Tundra", "Sub-zero permafrost frost with crystalline ice hue", "com.example.MainActivityFrozenTundra", "Premium Materials", 0xFF0C2333, 0xFF7DD3FC),
            LauncherIconVariant("gunmetal_heavy", "Gunmetal Heavy", "Tactical matte gunmetal alloy with stealth edges", "com.example.MainActivityGunmetalHeavy", "Premium Materials", 0xFF18181B, 0xFF52525B),

            // 38–52: Pop-Culture & Specialized Concepts
            LauncherIconVariant("nvidia_shield", "NVIDIA Shield", "Team green tactical gaming emblem with lime pulse", "com.example.MainActivityNvidiaShield", "Pop-Culture & Special", 0xFF0A1F0A, 0xFF76B900),
            LauncherIconVariant("playstation_classic", "PlayStation Classic", "Heritage PlayStation royal blue console aesthetic", "com.example.MainActivityPlayStationClassic", "Pop-Culture & Special", 0xFF001E50, 0xFF003791),
            LauncherIconVariant("xbox_command", "Xbox Command", "Dark gaming command deck with Xbox emerald green", "com.example.MainActivityXboxCommand", "Pop-Culture & Special", 0xFF0D2810, 0xFF107C10),
            LauncherIconVariant("gameboy_pocket", "GameBoy Pocket", "Nostalgic 90s monochrome LCD matrix olive-gray", "com.example.MainActivityGameBoyPocket", "Pop-Culture & Special", 0xFF263238, 0xFF8BC34A),
            LauncherIconVariant("nuka_quantum", "Nuka Quantum", "Post-apocalyptic glowing strontium cyan beverage vibe", "com.example.MainActivityNukaQuantum", "Pop-Culture & Special", 0xFF05233B, 0xFF00E5FF),
            LauncherIconVariant("deep_space", "Deep Space", "Interstellar dark void with cosmic nebula indigo", "com.example.MainActivityDeepSpace", "Pop-Culture & Special", 0xFF050510, 0xFF6366F1),
            LauncherIconVariant("volcanic_ash", "Volcanic Ash", "Molten magma rock fissures with incandescent ember", "com.example.MainActivityVolcanicAsh", "Pop-Culture & Special", 0xFF1C1311, 0xFFFF5722),
            LauncherIconVariant("ghost_protocol", "Ghost Protocol", "Stealth black-ops reconnaissance slate with icy HUD", "com.example.MainActivityGhostProtocol", "Pop-Culture & Special", 0xFF0A0E17, 0xFF64748B),
            LauncherIconVariant("subzero_frost", "Subzero Frost", "Cryogenic deep freeze atmosphere with ice flare", "com.example.MainActivitySubzeroFrost", "Pop-Culture & Special", 0xFF051B2C, 0xFF38BDF8),
            LauncherIconVariant("redline_racing", "Redline Racing", "High-rev track champion scarlet with apex checkered edge", "com.example.MainActivityRedlineRacing", "Pop-Culture & Special", 0xFF2B0000, 0xFFFF0033),
            LauncherIconVariant("vintage_parchment", "Vintage Parchment", "Ancient archival scroll parchment with sepia ink", "com.example.MainActivityVintageParchment", "Pop-Culture & Special", 0xFF2B2117, 0xFFD4A373),
            LauncherIconVariant("biohazard", "BioHazard", "Containment hazard yellow with cautionary radioactive trim", "com.example.MainActivityBioHazard", "Pop-Culture & Special", 0xFF1A1800, 0xFFE2E600),
            LauncherIconVariant("neon_mirage", "Neon Mirage", "Synthwave cyber dusk with electric hot magenta flare", "com.example.MainActivityNeonMirage", "Pop-Culture & Special", 0xFF1F002B, 0xFFFF00AA),
            LauncherIconVariant("chroma_eclipse", "Chroma Eclipse", "Total solar eclipse corona with ultraviolet spectrum", "com.example.MainActivityChromaEclipse", "Pop-Culture & Special", 0xFF08080C, 0xFF8B5CF6),
            LauncherIconVariant("jack_overlord", "Jack Overlord", "Master Crimson & Jet Obsidian executive command profile", "com.example.MainActivityJackOverlord", "Pop-Culture & Special", 0xFF1A0000, 0xFFD32F2F)
        )

        private const val PREFS_NAME = "launcher_icon_prefs"
        private const val KEY_ACTIVE_ICON = "active_launcher_icon_id"

        fun getActiveIconId(context: Context): String {
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_ACTIVE_ICON, "default") ?: "default"
        }

        fun setLauncherIcon(context: Context, variantId: String): Boolean {
            val target = ICON_VARIANTS.find { it.id == variantId } ?: return false
            val engine = IconChangerEngine(context)
            engine.safelyDeployTargetTheme(target.aliasClass)

            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_ACTIVE_ICON, variantId)
                .apply()

            return true
        }
    }
}
