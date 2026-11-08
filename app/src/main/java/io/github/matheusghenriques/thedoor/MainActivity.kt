package io.github.matheusghenriques.thedoor

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import io.github.matheusghenriques.thedoor.data.AppLimitRepository
import io.github.matheusghenriques.thedoor.data.AppPreferences
import io.github.matheusghenriques.thedoor.data.CurrentUsageProvider
import io.github.matheusghenriques.thedoor.data.OpenCountProvider
import io.github.matheusghenriques.thedoor.data.ProtectionConfig
import io.github.matheusghenriques.thedoor.ui.screen.AppLimitsScreen
import io.github.matheusghenriques.thedoor.ui.screen.ConfigChecklistScreen
import io.github.matheusghenriques.thedoor.ui.screen.ConfigLockChoiceScreen
import io.github.matheusghenriques.thedoor.ui.screen.DashboardCard
import io.github.matheusghenriques.thedoor.ui.screen.MainScreen
import io.github.matheusghenriques.thedoor.ui.screen.OnboardingScreen
import io.github.matheusghenriques.thedoor.ui.screen.PinSetupScreen
import io.github.matheusghenriques.thedoor.ui.screen.ProtectionConfigScreen
import io.github.matheusghenriques.thedoor.ui.screen.RedirectConfigScreen
import io.github.matheusghenriques.thedoor.ui.screen.TimeSavedMetrics
import io.github.matheusghenriques.thedoor.ui.screen.ZeroTrustConfirmScreen
import io.github.matheusghenriques.thedoor.ui.screen.formatMinutes
import io.github.matheusghenriques.thedoor.ui.screen.formatScheduleSummary
import io.github.matheusghenriques.thedoor.ui.theme.TheDoorTheme
import io.github.matheusghenriques.thedoor.ui.viewmodel.OnboardingViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.Locale

enum class ConfigStep { AppLimits, RedirectConfig, ProtectionConfig, Checklist, LockChoice, PinSetup, ZeroTrust }

private val ProtectionConfigSaver = listSaver<ProtectionConfig?, Boolean>(
    save = { config ->
        if (config != null) listOf(
            config.blockAccessibilitySettings,
            config.blockDeveloperOptions,
            config.blockUninstallTheDoor,
            config.blockAppInfo,
            config.blockLanguageChanges,
            config.blockPowerMenu,
            config.blockVpn,
            config.blockPrivateDns,
            config.blockUninstallFirefox,
            config.blockUninstallRethink,
            config.blockFirefoxSettings,
            config.blockFirefoxUblockOrigin,
            config.blockFirefoxBlockNSFW,
            config.blockSecureFolderAddApps,
            config.blockInstallUnknownApps,
            config.blockAutoBlocker,
            config.blockSettingsPopups,
            config.blockTileEditing
        ) else emptyList()
    },
    restore = { list ->
        if (list.isEmpty()) null
        else ProtectionConfig(
            blockAccessibilitySettings = list[0], blockDeveloperOptions = list[1],
            blockUninstallTheDoor = list[2], blockAppInfo = list[3],
            blockLanguageChanges = list[4], blockPowerMenu = list[5],
            blockVpn = list[6], blockPrivateDns = list[7],
            blockUninstallFirefox = list[8], blockUninstallRethink = list[9],
            blockFirefoxSettings = list[10], blockFirefoxUblockOrigin = list[11],
            blockFirefoxBlockNSFW = list[12],
            blockSecureFolderAddApps = list[13],
            blockInstallUnknownApps = list[14],
            blockAutoBlocker = list[15],
            blockSettingsPopups = list[16],
            blockTileEditing = list[17]
        )
    }
)

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_REDIRECT_PHRASE = "redirect_phrase"
    }

    private val redirectPhrase = mutableStateOf("")

    override fun attachBaseContext(newBase: Context) {
        val code = try {
            runBlocking { AppPreferences(newBase).languageCode.first() }
        } catch (_: Exception) {
            "system"
        }
        val locale = when (code) {
            "pt" -> Locale.forLanguageTag("pt-BR")
            "en" -> Locale.forLanguageTag("en-US")
            else -> Locale.getDefault()
        }
        Locale.setDefault(locale)
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        redirectPhrase.value = intent.getStringExtra(EXTRA_REDIRECT_PHRASE) ?: ""
        enableEdgeToEdge()
        setContent {
            val prefs = remember { AppPreferences(applicationContext) }
            val onboardingComplete by prefs.onboardingComplete.collectAsState(initial = false)
            val pinSetupComplete by prefs.pinSetupComplete.collectAsState(initial = false)
            val zeroTrustEnabled by prefs.zeroTrustEnabled.collectAsState(initial = false)

            val protectionConfigDone = rememberSaveable { mutableStateOf(false) }
            val savedProtectionConfig =
                rememberSaveable(stateSaver = ProtectionConfigSaver) { mutableStateOf(null) }
            val configStep = rememberSaveable { mutableStateOf<ConfigStep?>(null) }
            val animDirection = remember { mutableStateOf(true) }

            val settingsMode = rememberSaveable { mutableStateOf(false) }
            val showPinDialog = rememberSaveable { mutableStateOf(false) }
            val settingsProtectionConfig = remember { mutableStateOf<ProtectionConfig?>(null) }
            val settingsBlockAdultContent = remember { mutableStateOf(true) }

            val viewModel = remember { OnboardingViewModel(prefs) }
            val scope = rememberCoroutineScope()

            val onToggleLanguage: () -> Unit = {
                scope.launch {
                    val current = prefs.languageCode.first()
                    val next = if (current == "pt") "en" else "pt"
                    prefs.setLanguageCode(next)
                    prefs.saveOnboardingPage(viewModel.currentPage)
                    recreate()
                }
            }

            val limitsRepo = remember { AppLimitRepository(applicationContext) }
            val configs by limitsRepo.allConfigs.collectAsState(initial = emptyMap())
            val usageToday by limitsRepo.usageTodayFlow.collectAsState(initial = emptyMap())
            val appNameCache = remember { mutableMapOf<String, String>() }
            val openCounts by OpenCountProvider.openCounts.collectAsState(initial = emptyMap())
            val currentUsage by CurrentUsageProvider.currentUsage.collectAsState()

            val todayDow = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK)

            val dashboardCards by remember(configs, usageToday, openCounts, currentUsage) {
                derivedStateOf {
                    val opens = mutableListOf<DashboardCard.Opens>()
                    val limits = mutableListOf<DashboardCard.Limit>()
                    val schedules = mutableListOf<DashboardCard.Schedule>()
                    val blockeds = mutableListOf<DashboardCard.Blocked>()

                    configs.forEach { (pkg, config) ->
                        val name = appNameCache.getOrPut(pkg) {
                            try {
                                val info = packageManager.getApplicationInfo(pkg, 0)
                                packageManager.getApplicationLabel(info).toString()
                            } catch (_: Exception) {
                                pkg
                            }
                        }
                        if (config.maxOpensPerDay != null) {
                            opens.add(
                                DashboardCard.Opens(
                                    name,
                                    openCounts[pkg] ?: 0,
                                    config.maxOpensPerDay
                                )
                            )
                        }
                        if (config.perDayLimits.any { it.value > 0 }) {
                            val limitMin = config.perDayLimits[todayDow]
                                ?: config.perDayLimits.values.maxOrNull()
                            if (limitMin != null) {
                                limits.add(
                                    DashboardCard.Limit(
                                        name,
                                        ((currentUsage[pkg] ?: usageToday[pkg]
                                        ?: 0L) / 60000).toInt(),
                                        limitMin
                                    )
                                )
                            }
                        }
                        if (config.schedules.isNotEmpty()) {
                            schedules.add(
                                DashboardCard.Schedule(
                                    name,
                                    formatScheduleSummary(config.schedules)
                                )
                            )
                        }
                        if (config.blocked) {
                            blockeds.add(DashboardCard.Blocked(name))
                        }
                    }

                    opens + limits + schedules + blockeds
                }
            }

            val savedTimes by limitsRepo.savedTimesFlow.collectAsState(
                initial = io.github.matheusghenriques.thedoor.data.SavedTimeData()
            )

            val savedMetrics by remember(savedTimes) {
                derivedStateOf {
                    TimeSavedMetrics(
                        perDayFormatted = formatMinutes(savedTimes.dailyMinutes),
                        weekFormatted = formatMinutes(savedTimes.weekDays.sum()),
                        totalFormatted = formatMinutes(savedTimes.totalMinutes.toInt())
                    )
                }
            }

            LaunchedEffect(Unit) {
                limitsRepo.updateSavedTimes()
            }

            LaunchedEffect(settingsMode.value) {
                if (settingsMode.value) {
                    configStep.value = ConfigStep.AppLimits
                    animDirection.value = true
                    settingsProtectionConfig.value = prefs.protectionConfig.first()
                    settingsBlockAdultContent.value = prefs.blockAdultContent.first()
                } else {
                    limitsRepo.refreshUsage()
                }
            }

            val activity = LocalActivity.current as? ComponentActivity
            DisposableEffect(activity) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        limitsRepo.refreshUsage()
                    }
                }
                activity?.lifecycle?.addObserver(observer)
                onDispose { activity?.lifecycle?.removeObserver(observer) }
            }

            BackHandler(
                enabled = (!onboardingComplete && configStep.value != null) || (onboardingComplete && settingsMode.value)
            ) {
                val current = configStep.value ?: return@BackHandler
                when {
                    !onboardingComplete -> {
                        val prev = when (current) {
                            ConfigStep.PinSetup, ConfigStep.ZeroTrust -> ConfigStep.LockChoice
                            ConfigStep.LockChoice -> ConfigStep.Checklist
                            ConfigStep.Checklist -> ConfigStep.ProtectionConfig
                            ConfigStep.ProtectionConfig -> ConfigStep.RedirectConfig
                            ConfigStep.RedirectConfig -> ConfigStep.AppLimits
                            ConfigStep.AppLimits -> null
                        }
                        animDirection.value = false
                        configStep.value = prev
                    }

                    settingsMode.value -> {
                        val prev = when (current) {
                            ConfigStep.ZeroTrust, ConfigStep.PinSetup -> ConfigStep.LockChoice
                            ConfigStep.LockChoice -> ConfigStep.Checklist
                            ConfigStep.Checklist -> ConfigStep.ProtectionConfig
                            ConfigStep.ProtectionConfig -> ConfigStep.RedirectConfig
                            ConfigStep.RedirectConfig -> ConfigStep.AppLimits
                            ConfigStep.AppLimits -> null
                        }
                        if (prev != null) {
                            animDirection.value = false
                            configStep.value = prev
                        } else {
                            configStep.value = null
                            settingsMode.value = false
                        }
                    }
                }
            }

            TheDoorTheme {
                if (onboardingComplete) {
                    if (zeroTrustEnabled) {
                        MainScreen(
                            redirectPhrase = redirectPhrase.value,
                            zeroTrust = true,
                            onToggleLanguage = onToggleLanguage,
                            dashboardCards = dashboardCards,
                            savedMetrics = savedMetrics
                        )
                    } else if (settingsMode.value) {
                        val step = configStep.value ?: ConfigStep.AppLimits
                        AnimatedContent(
                            targetState = step, transitionSpec = {
                                (if (animDirection.value) {
                                    slideInHorizontally(tween(300)) { width -> width } togetherWith slideOutHorizontally(
                                        tween(300)
                                    ) { width -> -width }
                                } else {
                                    slideInHorizontally(tween(300)) { width -> -width } togetherWith slideOutHorizontally(
                                        tween(300)
                                    ) { width -> width }
                                }).using(SizeTransform(clip = false))
                            }) { s ->
                            when (s) {
                                ConfigStep.AppLimits -> AppLimitsScreen(onBack = {
                                    configStep.value = null; settingsMode.value = false
                                }, onComplete = {
                                    animDirection.value = true; configStep.value =
                                    ConfigStep.RedirectConfig
                                })

                                ConfigStep.RedirectConfig -> RedirectConfigScreen(onBack = {
                                    animDirection.value = false; configStep.value =
                                    ConfigStep.AppLimits
                                }, onComplete = {
                                    animDirection.value = true; configStep.value =
                                    ConfigStep.ProtectionConfig
                                })

                                ConfigStep.ProtectionConfig -> ProtectionConfigScreen(
                                    initialConfig = settingsProtectionConfig.value
                                        ?: ProtectionConfig(),
                                    showFirefoxTiles = settingsBlockAdultContent.value, onBack = {
                                        animDirection.value = false; configStep.value =
                                        ConfigStep.RedirectConfig
                                    }, onComplete = { config ->
                                        savedProtectionConfig.value = config
                                        protectionConfigDone.value = true
                                        animDirection.value = true
                                        configStep.value = ConfigStep.Checklist
                                    })

                                ConfigStep.Checklist -> {
                                    ConfigChecklistScreen(
                                        blockAdultContent = viewModel.blockAdultContent,
                                        blockSocial = viewModel.blockSocial,
                                        onBack = {
                                            animDirection.value = false; configStep.value =
                                            ConfigStep.ProtectionConfig
                                        },
                                        onComplete = {
                                            animDirection.value = true; configStep.value =
                                            ConfigStep.LockChoice
                                        })
                                }

                                ConfigStep.LockChoice -> ConfigLockChoiceScreen(onBack = {
                                    animDirection.value = false; configStep.value =
                                    ConfigStep.Checklist
                                }, onPinChosen = {
                                    animDirection.value = true; configStep.value =
                                    ConfigStep.PinSetup
                                }, onZeroTrustChosen = {
                                    animDirection.value = true; configStep.value =
                                    ConfigStep.ZeroTrust
                                })

                                ConfigStep.PinSetup -> PinSetupScreen(onBack = {
                                    animDirection.value = false; configStep.value =
                                    ConfigStep.LockChoice
                                }, onComplete = { pin ->
                                    scope.launch {
                                        prefs.setPin(pin)
                                        savedProtectionConfig.value?.let {
                                            prefs.saveProtectionConfigOnly(
                                                it
                                            )
                                        }
                                        settingsMode.value = false
                                        configStep.value = null
                                        settingsProtectionConfig.value = null
                                    }
                                }, onKeepPin = {
                                    scope.launch {
                                        savedProtectionConfig.value?.let {
                                            prefs.saveProtectionConfigOnly(
                                                it
                                            )
                                        }
                                        settingsMode.value = false
                                        configStep.value = null
                                        settingsProtectionConfig.value = null
                                    }
                                })

                                ConfigStep.ZeroTrust -> ZeroTrustConfirmScreen(onBack = {
                                    animDirection.value = false; configStep.value =
                                    ConfigStep.LockChoice
                                }, onComplete = {
                                    scope.launch {
                                        prefs.setZeroTrust()
                                        savedProtectionConfig.value?.let {
                                            prefs.saveProtectionConfigOnly(
                                                it
                                            )
                                        }
                                        settingsMode.value = false
                                        configStep.value = null
                                        settingsProtectionConfig.value = null
                                    }
                                })

                            }
                        }
                    } else {
                        MainScreen(
                            redirectPhrase = redirectPhrase.value,
                            showSettingsButton = pinSetupComplete,
                            onSettingsClick = { showPinDialog.value = true },
                            onToggleLanguage = onToggleLanguage,
                            dashboardCards = dashboardCards,
                            savedMetrics = savedMetrics
                        )

                        if (showPinDialog.value) {
                            PinDialog(onVerify = { pin -> prefs.verifyPin(pin) }, onCorrect = {
                                showPinDialog.value = false
                                settingsMode.value = true
                                configStep.value = ConfigStep.AppLimits
                            }, onDismiss = { showPinDialog.value = false })
                        }
                    }
                } else {
                    val step = configStep.value
                    if (step != null) {
                        AnimatedContent(
                            targetState = step, transitionSpec = {
                                (if (animDirection.value) {
                                    slideInHorizontally(tween(300)) { width -> width } togetherWith slideOutHorizontally(
                                        tween(300)
                                    ) { width -> -width }
                                } else {
                                    slideInHorizontally(tween(300)) { width -> -width } togetherWith slideOutHorizontally(
                                        tween(300)
                                    ) { width -> width }
                                }).using(SizeTransform(clip = false))
                            }) { s ->
                            when (s) {
                                ConfigStep.AppLimits -> AppLimitsScreen(onBack = {
                                    animDirection.value = false; configStep.value = null
                                }, onComplete = {
                                    animDirection.value = true; configStep.value =
                                    ConfigStep.RedirectConfig
                                })

                                ConfigStep.RedirectConfig -> RedirectConfigScreen(onBack = {
                                    animDirection.value = false; configStep.value =
                                    ConfigStep.AppLimits
                                }, onComplete = {
                                    animDirection.value = true; configStep.value =
                                    ConfigStep.ProtectionConfig
                                })

                                ConfigStep.ProtectionConfig -> ProtectionConfigScreen(
                                    showFirefoxTiles = viewModel.blockAdultContent, onBack = {
                                    animDirection.value = false; configStep.value =
                                    ConfigStep.RedirectConfig
                                }, onComplete = { config ->
                                    savedProtectionConfig.value = config
                                    protectionConfigDone.value = true
                                    animDirection.value = true
                                    configStep.value = ConfigStep.Checklist
                                })

                                ConfigStep.Checklist -> {
                                    if (protectionConfigDone.value) {
                                        ConfigChecklistScreen(
                                            blockAdultContent = viewModel.blockAdultContent,
                                            blockSocial = viewModel.blockSocial,
                                            onBack = {
                                                animDirection.value = false; configStep.value =
                                                ConfigStep.ProtectionConfig
                                            },
                                            onComplete = {
                                                animDirection.value = true; configStep.value =
                                                ConfigStep.LockChoice
                                            })
                                    }
                                }

                                ConfigStep.LockChoice -> ConfigLockChoiceScreen(onBack = {
                                    animDirection.value = false; configStep.value =
                                    ConfigStep.Checklist
                                }, onPinChosen = {
                                    animDirection.value = true; configStep.value =
                                    ConfigStep.PinSetup
                                }, onZeroTrustChosen = {
                                    animDirection.value = true; configStep.value =
                                    ConfigStep.ZeroTrust
                                })

                                ConfigStep.PinSetup -> PinSetupScreen(onBack = {
                                    animDirection.value = false; configStep.value =
                                    ConfigStep.LockChoice
                                }, onComplete = { pin ->
                                    scope.launch {
                                        prefs.setPin(pin)
                                        savedProtectionConfig.value?.let { viewModel.finish(it) { } }
                                    }
                                })

                                ConfigStep.ZeroTrust -> ZeroTrustConfirmScreen(onBack = {
                                    animDirection.value = false; configStep.value =
                                    ConfigStep.LockChoice
                                }, onComplete = {
                                    scope.launch {
                                        prefs.setZeroTrust()
                                        savedProtectionConfig.value?.let { viewModel.finish(it) { } }
                                    }
                                })
                            }
                        }
                    } else {
                        OnboardingScreen(
                            viewModel = viewModel,
                            onToggleLanguage = onToggleLanguage,
                            onStartAppLimits = {
                                animDirection.value = true; configStep.value = ConfigStep.AppLimits
                            })
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        redirectPhrase.value = intent.getStringExtra(EXTRA_REDIRECT_PHRASE) ?: ""
    }
}

@Composable
private fun PinDialog(
    onVerify: (String) -> Boolean, onCorrect: () -> Unit, onDismiss: () -> Unit
) {
    val pin = remember { mutableStateOf("") }
    val error = remember { mutableStateOf(false) }

    AlertDialog(onDismissRequest = onDismiss, title = {
        Text(
            text = stringResource(R.string.pin_dialog_title),
            style = MaterialTheme.typography.titleLarge
        )
    }, text = {
        Column {
            Text(
                text = stringResource(R.string.pin_dialog_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = pin.value,
                onValueChange = {
                    if (it.length <= 6 && it.all { c -> c.isDigit() }) {
                        pin.value = it
                        error.value = false
                    }
                },
                label = { Text(stringResource(R.string.pin_dialog_label)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth(),
                isError = error.value
            )
            if (error.value) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.pin_dialog_error),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }, confirmButton = {
        TextButton(
            onClick = {
                if (pin.value.length == 6 && onVerify(pin.value)) {
                    onCorrect()
                } else {
                    error.value = true
                }
            }) {
            Text(stringResource(R.string.pin_dialog_btn_unlock))
        }
    }, dismissButton = {
        TextButton(onClick = onDismiss) {
            Text(stringResource(R.string.pin_dialog_btn_cancel))
        }
    })
}
