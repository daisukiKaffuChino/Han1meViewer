package io.github.daisukikaffuchino.han1meviewer

import android.app.Activity
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import io.github.daisukikaffuchino.han1meviewer.logic.SettingsRepository
import io.github.daisukikaffuchino.han1meviewer.logic.datastore.DataStoreManager
import io.github.daisukikaffuchino.han1meviewer.logic.network.HDns
import io.github.daisukikaffuchino.han1meviewer.logic.network.HProxySelector
import io.github.daisukikaffuchino.han1meviewer.logic.network.ech.ConscryptEch
import io.github.daisukikaffuchino.han1meviewer.logic.network.ech.HyEchH3
import io.github.daisukikaffuchino.han1meviewer.logic.network.ech.echTransport
import io.github.daisukikaffuchino.han1meviewer.ui.crash.CrashHandler
import io.github.daisukikaffuchino.han1meviewer.util.AnimeShaders
import io.github.daisukikaffuchino.han1meviewer.util.AppLanguageManager
import io.github.daisukikaffuchino.utils.ActivityManager
import io.github.daisukikaffuchino.utils.LogUtil
import io.github.daisukikaffuchino.utils.applicationContext as globalApplicationContext
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import `is`.xyz.mpv.MPVLib
import okhttp3.OkHttpClient
import java.lang.ref.WeakReference
import java.net.ProxySelector
import java.util.concurrent.TimeUnit

/**
 * @project Hanime1
 * @author Yenaly Liew
 * @time 2022/06/08 008 17:32
 */
class HanimeApplication : Application(), Application.ActivityLifecycleCallbacks,
    SingletonImageLoader.Factory {

    companion object {
        const val TAG = "HanimeApplication"
    }

    private val imageClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .echTransport(HDns())
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    override fun newImageLoader(context: Context): ImageLoader = ImageLoader.Builder(context)
        .components {
            add(OkHttpNetworkFetcherFactory(callFactory = { imageClient }))
        }
        .build()

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        globalApplicationContext = this
    }

    override fun onCreate() {
        super.onCreate()
        Thread.setDefaultUncaughtExceptionHandler(CrashHandler(applicationContext))
        DataStoreManager.initialize(this)
        SettingsRepository.install(DataStoreManager)
        ConscryptEch.install()
        HyEchH3.attach(this)
        AppLanguageManager.applyStoredLanguage(this)
        registerActivityLifecycleCallbacks(this)
        ProxySelector.setDefault(HProxySelector())
        HProxySelector.rebuildNetwork()
        initNotificationChannel()
        MPVLib.create(applicationContext)
        MPVLib.init()

        if (AnimeShaders.copyShaderAssets(applicationContext) <= 0) {
            LogUtil.w(TAG, "Shader 复制失败")
        }
        if (AnimeShaders.copyCertAssets(applicationContext) <= 0) {
            LogUtil.w(TAG, "cert 复制失败")
        }
        val selected = SettingsRepository.fakeLauncherIcon
        switchLauncher(selected)
    }

    private fun initNotificationChannel() {
        val nm = NotificationManagerCompat.from(this)

        val hanimeDownloadChannel = NotificationChannelCompat.Builder(
            DOWNLOAD_NOTIFICATION_CHANNEL,
            NotificationManagerCompat.IMPORTANCE_HIGH
        ).setName("Hanime Download").build()
        nm.createNotificationChannel(hanimeDownloadChannel)

        val appUpdateChannel = NotificationChannelCompat.Builder(
            UPDATE_NOTIFICATION_CHANNEL,
            NotificationManagerCompat.IMPORTANCE_HIGH
        ).setName("App Update").build()
        nm.createNotificationChannel(appUpdateChannel)
    }

    fun switchLauncher(alias: String) {
        val pm = packageManager

        val allAliases = listOf(
            "io.github.daisukikaffuchino.han1meviewer.LauncherAliasDefault",
            "io.github.daisukikaffuchino.han1meviewer.LauncherFakeCalc",
            "io.github.daisukikaffuchino.han1meviewer.LauncherFakeCornhub",
            "io.github.daisukikaffuchino.han1meviewer.LauncherFakeXxt"
        )

        allAliases.forEach { a ->
            val state = if (a == alias)
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            else
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED

            pm.setComponentEnabledSetting(
                ComponentName(this, a),
                state,
                PackageManager.DONT_KILL_APP
            )
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

    override fun onActivityStarted(activity: Activity) = Unit

    override fun onActivityResumed(activity: Activity) {
        ActivityManager.currentActivity = WeakReference(activity)
    }

    override fun onActivityPaused(activity: Activity) = Unit

    override fun onActivityStopped(activity: Activity) = Unit

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

    override fun onActivityDestroyed(activity: Activity) = Unit
}
