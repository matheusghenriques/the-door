package io.github.matheusghenriques.thedoor.data

data class ProtectionConfig(
    val blockAccessibilitySettings: Boolean = false,
    val blockSettingsPopups: Boolean = false,
    val blockDeveloperOptions: Boolean = false,
    val blockUninstallTheDoor: Boolean = false,
    val blockAppInfo: Boolean = false,
    val blockLanguageChanges: Boolean = false,
    val blockPowerMenu: Boolean = false,
    val blockVpn: Boolean = false,
    val blockPrivateDns: Boolean = false,
    val blockUninstallFirefox: Boolean = false,
    val blockUninstallRethink: Boolean = false,
    val blockFirefoxSettings: Boolean = false,
    val blockFirefoxUblockOrigin: Boolean = false,
    val blockFirefoxBlockNSFW: Boolean = false,
    val blockSecureFolderAddApps: Boolean = false,
    val blockInstallUnknownApps: Boolean = false,
    val blockTileEditing: Boolean = false,
    val blockAutoBlocker: Boolean = false,
)
