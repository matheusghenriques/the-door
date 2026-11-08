package io.github.matheusghenriques.thedoor.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_preferences")

class AppPreferences(private val context: Context) {

    companion object {
        private const val TAG = "AppPreferences"
        private val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        private val PROTECTION_CONFIG_JSON = stringPreferencesKey("protection_config_json")
        private val PIN_SETUP_COMPLETE = booleanPreferencesKey("pin_setup_complete")
        private val ZERO_TRUST_ENABLED = booleanPreferencesKey("zero_trust_enabled")
        private val ONBOARDING_PAGE = intPreferencesKey("onboarding_page")
        private val LANGUAGE_CODE = stringPreferencesKey("language_code")
    }

    private val encPrefs: SharedPreferences by lazy {
        context.getSharedPreferences("pin_secure_prefs", Context.MODE_PRIVATE)
    }

    val onboardingComplete: Flow<Boolean> = context.dataStore.data.catch { e ->
        if (e is CorruptionException || e is IOException) emit(emptyPreferences()) else throw e
    }.map { prefs -> prefs[ONBOARDING_COMPLETE] ?: false }

    val protectionConfig: Flow<ProtectionConfig> = context.dataStore.data.catch { e ->
        if (e is CorruptionException || e is IOException) emit(emptyPreferences()) else throw e
    }.map { prefs -> parseProtectionConfig(prefs[PROTECTION_CONFIG_JSON] ?: "") }

    val pinSetupComplete: Flow<Boolean> = context.dataStore.data.catch { e ->
        if (e is CorruptionException || e is IOException) emit(emptyPreferences()) else throw e
    }.map { prefs -> prefs[PIN_SETUP_COMPLETE] ?: false }

    val zeroTrustEnabled: Flow<Boolean> = context.dataStore.data.catch { e ->
        if (e is CorruptionException || e is IOException) emit(emptyPreferences()) else throw e
    }.map { prefs -> prefs[ZERO_TRUST_ENABLED] ?: false }

    val onboardingPage: Flow<Int> = context.dataStore.data.catch { e ->
        if (e is CorruptionException || e is IOException) emit(emptyPreferences()) else throw e
    }.map { prefs -> prefs[ONBOARDING_PAGE] ?: 0 }

    val languageCode: Flow<String> = context.dataStore.data.catch { e ->
        if (e is CorruptionException || e is IOException) emit(emptyPreferences()) else throw e
    }.map { prefs -> prefs[LANGUAGE_CODE] ?: "system" }

    val blockAdultContent: Flow<Boolean> = context.dataStore.data.catch { e ->
        if (e is CorruptionException || e is IOException) emit(emptyPreferences()) else throw e
    }.map { prefs -> prefs[booleanPreferencesKey("block_adult")] ?: false }

    suspend fun setLanguageCode(code: String) {
        context.dataStore.edit { prefs ->
            prefs[LANGUAGE_CODE] = code
        }
    }

    suspend fun saveOnboardingPage(page: Int) {
        context.dataStore.edit { prefs ->
            prefs[ONBOARDING_PAGE] = page
        }
    }

    suspend fun setPin(pin: String) {
        val hash = PinManager.hashPin(pin)
        withContext(NonCancellable + Dispatchers.IO) {
            encPrefs.edit().putString("pin_hash", hash).commit()
        }
        context.dataStore.edit { prefs ->
            prefs[PIN_SETUP_COMPLETE] = true
            prefs[ZERO_TRUST_ENABLED] = false
        }
    }

    fun verifyPin(pin: String): Boolean {
        val hash = try {
            encPrefs.getString("pin_hash", null)
        } catch (e: Exception) {
            Log.e(TAG, "verifyPin: erro ao ler encPrefs", e)
            null
        }
        if (hash == null) {
            Log.w(TAG, "verifyPin: pin_hash não encontrado")
            return false
        }
        return PinManager.verifyPin(pin, hash).also { ok ->
            if (!ok) Log.w(TAG, "verifyPin: hash inválido para o PIN fornecido")
        }
    }

    suspend fun setZeroTrust() {
        context.dataStore.edit { prefs ->
            prefs[ZERO_TRUST_ENABLED] = true
            prefs[PIN_SETUP_COMPLETE] = false
        }
    }

    suspend fun saveProtectionConfigOnly(config: ProtectionConfig) {
        context.dataStore.edit { prefs ->
            prefs[PROTECTION_CONFIG_JSON] = protectionConfigToJson(config)
        }
    }

    suspend fun finishOnboarding(
        blockAdultContent: Boolean,
        blockSocial: Boolean,
        blockGambling: Boolean,
        protectionConfig: ProtectionConfig
    ) {
        context.dataStore.edit { prefs ->
            prefs[ONBOARDING_COMPLETE] = true
            prefs[booleanPreferencesKey("block_adult")] = blockAdultContent
            prefs[booleanPreferencesKey("block_social")] = blockSocial
            prefs[booleanPreferencesKey("block_gambling")] = blockGambling
            prefs[PROTECTION_CONFIG_JSON] = protectionConfigToJson(protectionConfig)
        }
    }

    private fun protectionConfigToJson(config: ProtectionConfig): String =
        protectionConfigJson(config)

    private fun parseProtectionConfig(json: String): ProtectionConfig =
        parseProtectionConfigJson(json)
}

internal fun protectionConfigJson(config: ProtectionConfig): String {
    val obj = JSONObject().apply {
        put("blockAccessibilitySettings", config.blockAccessibilitySettings)
        put("blockSettingsPopups", config.blockSettingsPopups)
        put("blockDeveloperOptions", config.blockDeveloperOptions)
        put("blockUninstallTheDoor", config.blockUninstallTheDoor)
        put("blockAppInfo", config.blockAppInfo)
        put("blockLanguageChanges", config.blockLanguageChanges)
        put("blockPowerMenu", config.blockPowerMenu)
        put("blockVpn", config.blockVpn)
        put("blockPrivateDns", config.blockPrivateDns)
        put("blockUninstallFirefox", config.blockUninstallFirefox)
        put("blockUninstallRethink", config.blockUninstallRethink)
        put("blockFirefoxSettings", config.blockFirefoxSettings)
        put("blockFirefoxUblockOrigin", config.blockFirefoxUblockOrigin)
        put("blockFirefoxBlockNSFW", config.blockFirefoxBlockNSFW)
        put("blockSecureFolderAddApps", config.blockSecureFolderAddApps)
        put("blockInstallUnknownApps", config.blockInstallUnknownApps)
        put("blockTileEditing", config.blockTileEditing)
        put("blockAutoBlocker", config.blockAutoBlocker)
    }
    return obj.toString()
}

internal fun parseProtectionConfigJson(json: String): ProtectionConfig {
    if (json.isBlank()) return ProtectionConfig()
    val obj = JSONObject(json)
    return ProtectionConfig(
        blockAccessibilitySettings = obj.optBoolean("blockAccessibilitySettings", false),
        blockSettingsPopups = obj.optBoolean("blockSettingsPopups", false),
        blockDeveloperOptions = obj.optBoolean("blockDeveloperOptions", false),
        blockUninstallTheDoor = obj.optBoolean("blockUninstallTheDoor", false),
        blockAppInfo = obj.optBoolean("blockAppInfo", false),
        blockLanguageChanges = obj.optBoolean("blockLanguageChanges", false),
        blockPowerMenu = obj.optBoolean("blockPowerMenu", false),
        blockVpn = obj.optBoolean("blockVpn", false),
        blockPrivateDns = obj.optBoolean("blockPrivateDns", false),
        blockUninstallFirefox = obj.optBoolean("blockUninstallFirefox", false),
        blockUninstallRethink = obj.optBoolean("blockUninstallRethink", false),
        blockFirefoxSettings = obj.optBoolean("blockFirefoxSettings", false),
        blockFirefoxUblockOrigin = obj.optBoolean("blockFirefoxUblockOrigin", false),
        blockFirefoxBlockNSFW = obj.optBoolean("blockFirefoxBlockNSFW", false),
        blockSecureFolderAddApps = obj.optBoolean("blockSecureFolderAddApps", false),
        blockInstallUnknownApps = obj.optBoolean("blockInstallUnknownApps", false),
        blockTileEditing = obj.optBoolean("blockTileEditing", false),
        blockAutoBlocker = obj.optBoolean("blockAutoBlocker", false),
    )
}
