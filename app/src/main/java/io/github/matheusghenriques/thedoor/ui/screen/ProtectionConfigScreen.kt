package io.github.matheusghenriques.thedoor.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.matheusghenriques.thedoor.R
import io.github.matheusghenriques.thedoor.data.ProtectionConfig

@Composable
fun ProtectionConfigScreen(
    initialConfig: ProtectionConfig = ProtectionConfig(),
    showFirefoxTiles: Boolean = true,
    onBack: () -> Unit,
    onComplete: (ProtectionConfig) -> Unit
) {
    var config by remember { mutableStateOf(initialConfig) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 24.dp, vertical = 24.dp)
    ) {
        Text(
            text = stringResource(R.string.protection_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.protection_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )

        Spacer(Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            SectionHeader(stringResource(R.string.protection_section_recommended))

            ProtectionItem(
                checked = config.blockAccessibilitySettings,
                onCheckedChange = { config = config.copy(blockAccessibilitySettings = it) },
                title = stringResource(R.string.protection_item_accessibility),
                subtitle = stringResource(R.string.protection_item_accessibility_desc)
            )
            Spacer(Modifier.height(8.dp))
            ProtectionItem(
                checked = config.blockSettingsPopups,
                onCheckedChange = { config = config.copy(blockSettingsPopups = it) },
                title = stringResource(R.string.protection_item_settings_popups),
                subtitle = stringResource(R.string.protection_item_settings_popups_desc)
            )
            Spacer(Modifier.height(8.dp))
            ProtectionItem(
                checked = config.blockDeveloperOptions,
                onCheckedChange = { config = config.copy(blockDeveloperOptions = it) },
                title = stringResource(R.string.protection_item_developer_options),
                subtitle = stringResource(R.string.protection_item_developer_options_desc)
            )
            Spacer(Modifier.height(8.dp))
            ProtectionItem(
                checked = config.blockUninstallTheDoor,
                onCheckedChange = { config = config.copy(blockUninstallTheDoor = it) },
                title = stringResource(R.string.protection_item_uninstall_door),
                subtitle = stringResource(R.string.protection_item_uninstall_door_desc)
            )
            Spacer(Modifier.height(8.dp))
            ProtectionItem(
                checked = config.blockAppInfo,
                onCheckedChange = { config = config.copy(blockAppInfo = it) },
                title = stringResource(R.string.protection_item_app_info),
                subtitle = stringResource(R.string.protection_item_app_info_desc)
            )
            Spacer(Modifier.height(8.dp))
            ProtectionItem(
                checked = config.blockLanguageChanges,
                onCheckedChange = { config = config.copy(blockLanguageChanges = it) },
                title = stringResource(R.string.protection_item_language),
                subtitle = stringResource(R.string.protection_item_language_desc)
            )
                Spacer(Modifier.height(8.dp))
                ProtectionItem(
                    checked = config.blockSecureFolderAddApps,
                    onCheckedChange = { config = config.copy(blockSecureFolderAddApps = it) },
                    title = stringResource(R.string.protection_item_secure_folder),
                    subtitle = stringResource(R.string.protection_item_secure_folder_desc)
                )

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(16.dp))

            SectionHeader(stringResource(R.string.protection_section_additional))

            ProtectionItem(
                checked = config.blockPowerMenu,
                onCheckedChange = { config = config.copy(blockPowerMenu = it) },
                title = stringResource(R.string.protection_item_power_menu),
                subtitle = stringResource(R.string.protection_item_power_menu_desc)
            )
            Spacer(Modifier.height(8.dp))
            ProtectionItem(
                checked = config.blockVpn,
                onCheckedChange = { config = config.copy(blockVpn = it) },
                title = stringResource(R.string.protection_item_vpn),
                subtitle = stringResource(R.string.protection_item_vpn_desc)
            )
            Spacer(Modifier.height(8.dp))
            ProtectionItem(
                checked = config.blockTileEditing,
                onCheckedChange = { config = config.copy(blockTileEditing = it) },
                title = stringResource(R.string.protection_item_tile_editing),
                subtitle = stringResource(R.string.protection_item_tile_editing_desc)
            )
            Spacer(Modifier.height(8.dp))
            ProtectionItem(
                checked = config.blockPrivateDns,
                onCheckedChange = { config = config.copy(blockPrivateDns = it) },
                title = stringResource(R.string.protection_item_private_dns),
                subtitle = stringResource(R.string.protection_item_private_dns_desc)
            )
            Spacer(Modifier.height(8.dp))
            ProtectionItem(
                checked = config.blockInstallUnknownApps,
                onCheckedChange = { config = config.copy(blockInstallUnknownApps = it) },
                title = stringResource(R.string.protection_item_unknown_install),
                subtitle = stringResource(R.string.protection_item_unknown_install_desc)
            )
            Spacer(Modifier.height(8.dp))
            ProtectionItem(
                checked = config.blockAutoBlocker,
                onCheckedChange = { config = config.copy(blockAutoBlocker = it) },
                title = stringResource(R.string.protection_item_auto_blocker),
                subtitle = stringResource(R.string.protection_item_auto_blocker_desc)
            )
            Spacer(Modifier.height(8.dp))
            if (showFirefoxTiles) {
                ProtectionItem(
                    checked = config.blockUninstallFirefox,
                    onCheckedChange = { config = config.copy(blockUninstallFirefox = it) },
                    title = stringResource(R.string.protection_item_uninstall_firefox),
                    subtitle = stringResource(R.string.protection_item_uninstall_firefox_desc)
                )
                Spacer(Modifier.height(8.dp))
                ProtectionItem(
                    checked = config.blockFirefoxSettings,
                    onCheckedChange = { config = config.copy(blockFirefoxSettings = it) },
                    title = stringResource(R.string.protection_item_firefox_settings),
                    subtitle = stringResource(R.string.protection_item_firefox_settings_desc)
                )
                Spacer(Modifier.height(8.dp))
                ProtectionItem(
                    checked = config.blockFirefoxUblockOrigin,
                    onCheckedChange = { config = config.copy(blockFirefoxUblockOrigin = it) },
                    title = stringResource(R.string.protection_item_firefox_ublock),
                    subtitle = stringResource(R.string.protection_item_firefox_ublock_desc)
                )
                Spacer(Modifier.height(8.dp))
                ProtectionItem(
                    checked = config.blockFirefoxBlockNSFW,
                    onCheckedChange = { config = config.copy(blockFirefoxBlockNSFW = it) },
                    title = stringResource(R.string.protection_item_firefox_blocknsfw),
                    subtitle = stringResource(R.string.protection_item_firefox_blocknsfw_desc)
                )
                Spacer(Modifier.height(8.dp))
            }
            ProtectionItem(
                checked = config.blockUninstallRethink,
                onCheckedChange = { config = config.copy(blockUninstallRethink = it) },
                title = stringResource(R.string.protection_item_uninstall_rethink),
                subtitle = stringResource(R.string.protection_item_uninstall_rethink_desc)
            )
            Spacer(Modifier.height(24.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(
                onClick = onBack, modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.btn_voltar))
            }
            Spacer(Modifier.width(12.dp))
            TimedButton(
                text = stringResource(R.string.btn_proximo),
                onClick = { onComplete(config) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}


@Composable
private fun ProtectionItem(
    checked: Boolean, onCheckedChange: (Boolean) -> Unit, title: String, subtitle: String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically
        ) {
            if (checked) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .border(
                            2.dp,
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            CircleShape
                        )
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}
