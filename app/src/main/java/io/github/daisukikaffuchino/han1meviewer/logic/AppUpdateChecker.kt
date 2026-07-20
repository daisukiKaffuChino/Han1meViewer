package io.github.daisukikaffuchino.han1meviewer.logic

import android.util.Base64
import android.util.Log
import androidx.core.content.edit
import io.github.daisukikaffuchino.han1meviewer.Preferences
import io.github.daisukikaffuchino.utils.decodeFromStringByBase64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

@Serializable
data class AppUpdateInfo(
    val versionName: String,
    val versionCode: Int,
    val downloadUrl: String,
    val updateDescription: String,
    val forceUpdate: Boolean,
)

sealed interface AppUpdateState {
    data object Checking : AppUpdateState
    data object NoUpdate : AppUpdateState
    data class Available(val info: AppUpdateInfo) : AppUpdateState
}

@OptIn(ExperimentalSerializationApi::class)
object AppUpdateChecker {
    private const val TAG = "AppUpdateChecker"
    private const val UPDATE_URL =
        "https://hnm-1258664276.cos.ap-shanghai.myqcloud.com/update.json"
    private const val UPDATE_REFERER = "hnmviewerup.com"
    private const val ENCODED_CURRENT_VERSION_CODE = "MjYwNzE5"
    private const val LAST_CHECK_TIME_KEY = "app_update_last_check_time"
    private const val CACHED_JSON_KEY = "app_update_cached_json"
    private const val IGNORED_VERSION_CODE_KEY = "app_update_ignored_version_code"
    private const val CHECK_INTERVAL_MILLIS = 2L * 60L * 60L * 1_000L

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
        allowTrailingComma = true
    }

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    suspend fun checkForUpdate(): AppUpdateInfo? = withContext(Dispatchers.IO) {
        val preferences = Preferences.preferenceSp
        val now = System.currentTimeMillis()
        val lastCheckTime = preferences.getLong(LAST_CHECK_TIME_KEY, 0L)
        val cachedJson = preferences.getString(CACHED_JSON_KEY, null)
        val isCacheFresh = lastCheckTime in 1..now &&
            now - lastCheckTime < CHECK_INTERVAL_MILLIS

        if (isCacheFresh) {
            cachedJson?.let { Log.d(TAG, "Cached update JSON: $it") }
            return@withContext cachedJson.toAvailableUpdateOrNull()
        }

        val responseJson = runCatching { requestUpdateJson() }
            .onFailure { Log.e(TAG, "Failed to check for updates", it) }
            .getOrNull()

        preferences.edit {
            putLong(LAST_CHECK_TIME_KEY, now)
            if (responseJson != null) {
                putString(CACHED_JSON_KEY, responseJson)
            }
        }

        val jsonToUse = responseJson ?: cachedJson
        if (responseJson == null) {
            jsonToUse?.let { Log.d(TAG, "Using stale update JSON: $it") }
        }
        jsonToUse.toAvailableUpdateOrNull()
    }

    fun ignoreUpdate(versionCode: Int) {
        Preferences.preferenceSp.edit {
            putInt(IGNORED_VERSION_CODE_KEY, versionCode)
        }
    }

    private fun requestUpdateJson(): String {
        val request = Request.Builder()
            .url(UPDATE_URL)
            .header("Referer", UPDATE_REFERER)
            .get()
            .build()
        return client.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "Update check failed with HTTP ${response.code}" }
            response.body.string().also { json ->
                Log.d(TAG, "Update response JSON: $json")
            }
        }
    }

    private fun String?.toAvailableUpdateOrNull(): AppUpdateInfo? {
        if (this.isNullOrBlank()) return null
        return runCatching {
            val info = jsonParser.decodeFromString<AppUpdateInfo>(this)
            require(info.versionName.isNotBlank()) { "versionName is blank" }
            require(info.versionCode > 0) { "versionCode must be positive" }
            require(info.downloadUrl.toHttpUrlOrNull() != null) { "downloadUrl is invalid" }

            val currentVersionCode = ENCODED_CURRENT_VERSION_CODE
                .decodeFromStringByBase64(Base64.NO_WRAP)
                .toInt()
            val ignoredVersionCode = Preferences.preferenceSp.getInt(
                IGNORED_VERSION_CODE_KEY,
                -1,
            )
            info.takeIf {
                it.versionCode > currentVersionCode &&
                    (it.forceUpdate || it.versionCode != ignoredVersionCode)
            }
        }.onFailure {
            Log.e(TAG, "Invalid update JSON", it)
        }.getOrNull()
    }
}
