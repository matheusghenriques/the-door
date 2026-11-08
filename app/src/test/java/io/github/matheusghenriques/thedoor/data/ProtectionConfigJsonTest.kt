package io.github.matheusghenriques.thedoor.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtectionConfigJsonTest {

    @Test
    fun protectionConfigJson_parseProtectionConfigJson_roundTrip() {
        val config = ProtectionConfig(
            blockAccessibilitySettings = true,
            blockSettingsPopups = true,
            blockDeveloperOptions = false,
            blockUninstallTheDoor = true,
            blockAppInfo = true,
            blockLanguageChanges = false,
            blockPowerMenu = true,
            blockVpn = true,
            blockPrivateDns = false,
            blockUninstallFirefox = true,
            blockUninstallRethink = true,
            blockFirefoxSettings = false,
            blockFirefoxUblockOrigin = true,
            blockFirefoxBlockNSFW = false,
            blockSecureFolderAddApps = true,
            blockInstallUnknownApps = false,
            blockTileEditing = true,
            blockAutoBlocker = false
        )
        assertEquals(config, parseProtectionConfigJson(protectionConfigJson(config)))
    }

    @Test
    fun parseProtectionConfigJson_emptyJson_returnsDefaults() {
        assertEquals(ProtectionConfig(), parseProtectionConfigJson("{}"))
        assertEquals(ProtectionConfig(), parseProtectionConfigJson(""))
    }

    @Test
    fun parseProtectionConfigJson_missingNewFields_defaultsToFalse() {
        // Installed configs written before blockSettingsPopups/blockTileEditing
        // existed must upgrade cleanly with both toggles off.
        val legacy = """{"blockAccessibilitySettings":true,"blockVpn":true,"blockPowerMenu":true}"""
        val parsed = parseProtectionConfigJson(legacy)
        assertEquals(true, parsed.blockAccessibilitySettings)
        assertEquals(true, parsed.blockVpn)
        assertEquals(false, parsed.blockSettingsPopups)
        assertEquals(false, parsed.blockTileEditing)
    }

    @Test
    fun protectionConfigJson_containsAllFields() {
        val json = protectionConfigJson(ProtectionConfig(blockSettingsPopups = true))
        assertTrue(json.contains("blockSettingsPopups"))
        assertTrue(json.contains("blockTileEditing"))
        assertTrue(json.contains("blockAccessibilitySettings"))
    }
}
