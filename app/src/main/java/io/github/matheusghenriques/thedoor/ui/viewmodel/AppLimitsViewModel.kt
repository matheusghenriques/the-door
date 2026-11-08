package io.github.matheusghenriques.thedoor.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.matheusghenriques.thedoor.data.AppConfig
import io.github.matheusghenriques.thedoor.data.AppInfo
import io.github.matheusghenriques.thedoor.data.AppLimitRepository
import io.github.matheusghenriques.thedoor.data.TimeSchedule
import io.github.matheusghenriques.thedoor.data.UsageTracker
import io.github.matheusghenriques.thedoor.data.getLauncherPackages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppLimitsViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = AppLimitRepository(app)
    private val usageTracker = UsageTracker(app)
    private val pm = app.packageManager

    private val launcherPackages: Set<String> by lazy { getLauncherPackages(pm) }

    var apps by mutableStateOf<List<AppInfo>>(emptyList())
        private set

    var configs = mutableStateMapOf<String, AppConfig>()
        private set

    var usagePermissionGranted by mutableStateOf(false)
        private set

    var searchQuery by mutableStateOf("")

    var loadingComplete by mutableStateOf(false)
        private set

    val filteredApps: List<AppInfo>
        get() {
            if (searchQuery.isBlank()) return apps
            val q = searchQuery.lowercase()
            return apps.filter {
                it.appName.lowercase().contains(q) || it.packageName.lowercase().contains(q)
            }
        }

    init {
        checkUsagePermission()
        viewModelScope.launch {
            val selfPackage = getApplication<Application>().packageName
            val candidates = withContext(Dispatchers.IO) {
                val installed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION") pm.getInstalledApplications(PackageManager.GET_META_DATA)
                }
                installed.filter {
                    it.packageName != selfPackage && it.packageName in launcherPackages
                }.sortedBy { it.loadLabel(pm).toString().lowercase() }
            }
            val result = mutableListOf<AppInfo>()
            for (ai in candidates) {
                val info = withContext(Dispatchers.IO) {
                    AppInfo(ai.packageName, ai.loadLabel(pm).toString(), ai.loadIcon(pm))
                }
                result.add(info)
                apps = result.toList()
            }
            loadingComplete = true
        }
        viewModelScope.launch {
            repo.allConfigs.first().let { configs.putAll(it) }
        }
    }

    private fun checkUsagePermission() {
        val app = getApplication<Application>()
        val appOps = app.getSystemService(android.app.AppOpsManager::class.java)
        val mode = appOps.checkOpNoThrow(
            android.app.AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), app.packageName
        )
        usagePermissionGranted = mode == android.app.AppOpsManager.MODE_ALLOWED
    }

    fun getUsageAccessIntent(): Intent {
        return Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS)
    }

    fun refreshPermission() {
        checkUsagePermission()
    }

    fun getConfig(packageName: String): AppConfig = configs[packageName] ?: AppConfig()

    fun getWeeklyAverageMinutes(packageName: String): Int? =
        usageTracker.getWeeklyAverageMinutes(packageName)

    fun saveAll(
        packageName: String,
        blocked: Boolean,
        schedules: List<TimeSchedule>,
        perDayLimits: Map<Int, Int>,
        perDayTargets: Map<Int, Int>,
        maxOpensPerDay: Int?
    ) {
        val config = AppConfig(
            blocked = blocked,
            schedules = schedules,
            perDayLimits = perDayLimits,
            perDayTargets = perDayTargets,
            targetMinutes = perDayTargets.values.maxOrNull(),
            lastReductionWeekStart = SystemClock.elapsedRealtime(),
            maxOpensPerDay = maxOpensPerDay
        )
        configs[packageName] = config
        viewModelScope.launch { repo.saveConfig(packageName, config) }
    }
}
