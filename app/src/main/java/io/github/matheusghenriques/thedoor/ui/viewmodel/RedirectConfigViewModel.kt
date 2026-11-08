package io.github.matheusghenriques.thedoor.ui.viewmodel

import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.matheusghenriques.thedoor.data.AppInfo
import io.github.matheusghenriques.thedoor.data.AppLimitRepository
import io.github.matheusghenriques.thedoor.data.RedirectConfig
import io.github.matheusghenriques.thedoor.data.RedirectType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class RedirectConfigViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = AppLimitRepository(app)
    private val pm = app.packageManager

    private val launcherPackages: Set<String> by lazy {
        io.github.matheusghenriques.thedoor.data.getLauncherPackages(
            pm
        )
    }

    var redirectType by mutableStateOf(RedirectType.THE_DOOR)

    var selectedAppPackage by mutableStateOf("")

    var theDoorPhrase by mutableStateOf("")

    var toastEnabled by mutableStateOf(false)

    var toastMessage by mutableStateOf("")

    var availableApps by mutableStateOf<List<AppInfo>>(emptyList())

    val selectedApp: AppInfo?
        get() = availableApps.find { it.packageName == selectedAppPackage }

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val savedConfig = repo.redirectConfig.first()
            redirectType = savedConfig.type
            selectedAppPackage = savedConfig.appPackage
            theDoorPhrase = savedConfig.theDoorPhrase
            toastEnabled = savedConfig.toastEnabled
            toastMessage = savedConfig.toastMessage

            val configs = repo.allConfigs.first()
            val blockedOrLimited =
                configs.filter { it.value.blocked || it.value.perDayLimits.any { e -> e.value > 0 } }.keys

            val selfPackage = getApplication<Application>().packageName

            @Suppress("DEPRECATION") val installed =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
                } else {
                    pm.getInstalledApplications(PackageManager.GET_META_DATA)
                }

            availableApps = installed.filter {
                it.packageName != selfPackage && it.packageName in launcherPackages && it.packageName !in blockedOrLimited
            }.mapNotNull {
                try {
                    AppInfo(
                        packageName = it.packageName,
                        appName = it.loadLabel(pm).toString(),
                        icon = it.loadIcon(pm)
                    )
                } catch (_: Exception) {
                    null
                }
            }.sortedBy { it.appName.lowercase() }
        }
    }

    fun selectApp(packageName: String) {
        selectedAppPackage = if (selectedAppPackage == packageName) "" else packageName
    }

    fun save(onSaved: () -> Unit) {
        viewModelScope.launch {
            repo.saveRedirectConfig(
                RedirectConfig(
                    type = redirectType,
                    appPackage = selectedAppPackage,
                    theDoorPhrase = theDoorPhrase,
                    toastEnabled = toastEnabled,
                    toastMessage = toastMessage
                )
            )
            onSaved()
        }
    }
}
