@file:Suppress("DEPRECATION")

package io.github.daisukikaffuchino.han1meviewer.ui.navigation.settings

import android.app.Activity
import android.app.AppOpsManager
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.annotation.IntRange
import androidx.annotation.RequiresApi
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.text.parseAsHtml
import io.github.daisukikaffuchino.han1meviewer.HanimeConstants.HANIME_HOSTNAME
import io.github.daisukikaffuchino.han1meviewer.HanimeConstants.HANIME_URL
import io.github.daisukikaffuchino.han1meviewer.Preferences
import io.github.daisukikaffuchino.han1meviewer.R
import io.github.daisukikaffuchino.han1meviewer.ui.view.video.HJzvdStd
import io.github.daisukikaffuchino.utils.formatBytesPerSecond
import io.github.daisukikaffuchino.utils.formatFileSizeV2
import io.github.daisukikaffuchino.utils.SonnerToast

internal fun saveBoolean(key: String, value: Boolean) {
    Preferences.preferenceSp.edit { putBoolean(key, value) }
}

internal fun saveString(key: String, value: String) {
    Preferences.preferenceSp.edit { putString(key, value) }
}

internal fun saveInt(key: String, value: Int) {
    Preferences.preferenceSp.edit { putInt(key, value) }
}

internal fun buildDomainOptions(context: Context): List<Pair<String, String>> = listOf(
    "${HANIME_HOSTNAME[0]} (${context.getString(R.string.default_)})" to HANIME_URL[0],
    "${HANIME_HOSTNAME[1]} (${context.getString(R.string.alternative)})" to HANIME_URL[1],
    "${HANIME_HOSTNAME[2]} (${context.getString(R.string.alternative)})" to HANIME_URL[2],
    "${HANIME_HOSTNAME[3]} (av)" to HANIME_URL[3],
)

internal fun generateClearCacheSummary(context: Context, size: Long): CharSequence {
    return context.getString(R.string.cache_usage_summary, size.formatFileSizeV2()).parseAsHtml()
}

internal fun toPrettySensitivityString(
    context: Context,
    @IntRange(from = 1, to = 9) value: Int
): String {
    val pretty = when (value) {
        1, 2 -> context.getString(R.string.high)
        3, 4 -> context.getString(R.string.moderately_high)
        5 -> context.getString(R.string.moderate)
        6 -> context.getString(R.string.slightly_low)
        7 -> context.getString(R.string.low)
        8 -> context.getString(R.string.very_low)
        9 -> context.getString(R.string.extremely_low)
        else -> error("Invalid sensitivity value: $value")
    }
    return context.getString(R.string.current_slide_sensitivity, pretty)
}

internal fun toPrettyCountdownRemindString(
    context: Context,
    @IntRange(from = 5, to = 30) value: Int
): String {
    return buildString {
        append(context.getString(R.string.will_remind_before_d_seconds, value))
        if (value == HJzvdStd.DEF_COUNTDOWN_SEC) append(" (${context.getString(R.string.default_)})")
    }
}

internal fun Long.toDownloadSpeedPrettyString(context: Context): String {
    return if (this == 0L) {
        context.getString(R.string.no_limit)
    } else {
        formatBytesPerSecond()
    }
}

internal fun toDownloadCountLimitPrettyString(context: Context, value: Int): String {
    return if (value == 0) context.getString(R.string.no_limit) else value.toString()
}

internal fun isDeviceSecureCompat(context: Context): Boolean {
    val km = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
    return km.isDeviceSecure
}

internal fun isPipPermissionGranted(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = appOps.unsafeCheckOpNoThrow(
        AppOpsManager.OPSTR_PICTURE_IN_PICTURE,
        Process.myUid(),
        context.packageName,
    )
    return mode == AppOpsManager.MODE_ALLOWED
}

internal fun openPipPermissionSettings(context: Context) {
    val intent = Intent(
        "android.settings.PICTURE_IN_PICTURE_SETTINGS",
        "package:${context.packageName}".toUri()
    )
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    context.startActivity(intent)
}

@RequiresApi(Build.VERSION_CODES.S)
internal fun openApplyDeepLinksSettings(context: Context, activity: Activity) {
    try {
        val intent = Intent().apply {
            action = Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS
            addCategory(Intent.CATEGORY_DEFAULT)
            data = "package:${context.packageName}".toUri()
            flags = Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
        }
        activity.startActivity(intent)
    } catch (e: Exception) {
        SonnerToast.warning(R.string.action_app_open_by_default_settings_not_support)
        e.printStackTrace()
    }
}
