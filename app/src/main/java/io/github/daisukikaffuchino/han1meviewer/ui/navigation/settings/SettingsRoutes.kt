package io.github.daisukikaffuchino.han1meviewer.ui.navigation.settings

import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import io.github.daisukikaffuchino.han1meviewer.R
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable
object HomeSettingsRoute

@Serializable
object VideoPlaybackSettingsRoute

@Serializable
object NetworkDownloadSettingsRoute

@Serializable
object AppearanceSettingsRoute

@Serializable
object PrivacySettingsRoute

@Serializable
object DataSettingsRoute

@Serializable
object AboutSettingsRoute

@Serializable
object OpenSourceLicensesRoute

@Serializable
object PlayerSettingsRoute

@Serializable
object NetworkSettingsRoute

@Serializable
object DownloadSettingsRoute

@Serializable
object MpvPlayerSettingsRoute

@Serializable
object HKeyframesRoute

@Serializable
object SharedHKeyframesRoute

@Serializable
object HKeyframeSettingsRoute

enum class SettingsDestinationSpec(
    val routeKey: String,
    val titleRes: Int,
    val screenClassName: String,
    val routeClass: KClass<*>,
    val showToolbar: Boolean = true,
) {
    Home(
        routeKey = "home",
        titleRes = R.string.settings,
        screenClassName = "HomeSettingsScreen",
        routeClass = HomeSettingsRoute::class,
    ),
    VideoPlayback(
        routeKey = "video_playback",
        titleRes = R.string.settings_video_playback,
        screenClassName = "HomeSettingsScreen.VideoPlayback",
        routeClass = VideoPlaybackSettingsRoute::class,
    ),
    NetworkDownload(
        routeKey = "network_download",
        titleRes = R.string.settings_network_download,
        screenClassName = "HomeSettingsScreen.NetworkDownload",
        routeClass = NetworkDownloadSettingsRoute::class,
    ),
    Appearance(
        routeKey = "appearance",
        titleRes = R.string.settings_appearance,
        screenClassName = "HomeSettingsScreen.Appearance",
        routeClass = AppearanceSettingsRoute::class,
    ),
    Privacy(
        routeKey = "privacy",
        titleRes = R.string.privacy,
        screenClassName = "HomeSettingsScreen.Privacy",
        routeClass = PrivacySettingsRoute::class,
    ),
    Data(
        routeKey = "data",
        titleRes = R.string.settings_data,
        screenClassName = "HomeSettingsScreen.Data",
        routeClass = DataSettingsRoute::class,
    ),
    About(
        routeKey = "about",
        titleRes = R.string.about,
        screenClassName = "HomeSettingsScreen.About",
        routeClass = AboutSettingsRoute::class,
    ),
    OpenSourceLicenses(
        routeKey = "open_source_licenses",
        titleRes = R.string.open_source_license,
        screenClassName = "OpenSourceLicensesScreen",
        routeClass = OpenSourceLicensesRoute::class,
    ),
    Player(
        routeKey = "player",
        titleRes = R.string.player_settings,
        screenClassName = "PlayerSettingsScreen",
        routeClass = PlayerSettingsRoute::class,
    ),
    Network(
        routeKey = "network",
        titleRes = R.string.network_settings,
        screenClassName = "NetworkSettingsScreen",
        routeClass = NetworkSettingsRoute::class,
    ),
    Download(
        routeKey = "download",
        titleRes = R.string.download_settings,
        screenClassName = "DownloadSettingsScreen",
        routeClass = DownloadSettingsRoute::class,
    ),
    Mpv(
        routeKey = "mpv",
        titleRes = R.string.mpv_advanced_settings,
        screenClassName = "MpvPlayerSettingsScreen",
        routeClass = MpvPlayerSettingsRoute::class,
    ),
    HKeyframes(
        routeKey = "h_keyframes",
        titleRes = R.string.h_keyframe_manage,
        screenClassName = "HKeyframesScreen",
        routeClass = HKeyframesRoute::class,
    ),
    SharedHKeyframes(
        routeKey = "shared_h_keyframes",
        titleRes = R.string.shared_h_keyframe_manage,
        screenClassName = "SharedHKeyframesScreen",
        routeClass = SharedHKeyframesRoute::class,
    ),
    HKeyframeSettings(
        routeKey = "h_keyframe_settings",
        titleRes = R.string.h_keyframe_settings,
        screenClassName = "HKeyframeSettingsScreen",
        routeClass = HKeyframeSettingsRoute::class,
    );

    val route: Any
        get() = when (this) {
            Home -> HomeSettingsRoute
            VideoPlayback -> VideoPlaybackSettingsRoute
            NetworkDownload -> NetworkDownloadSettingsRoute
            Appearance -> AppearanceSettingsRoute
            Privacy -> PrivacySettingsRoute
            Data -> DataSettingsRoute
            About -> AboutSettingsRoute
            OpenSourceLicenses -> OpenSourceLicensesRoute
            Player -> PlayerSettingsRoute
            Network -> NetworkSettingsRoute
            Download -> DownloadSettingsRoute
            Mpv -> MpvPlayerSettingsRoute
            HKeyframes -> HKeyframesRoute
            SharedHKeyframes -> SharedHKeyframesRoute
            HKeyframeSettings -> HKeyframeSettingsRoute
        }

    companion object {
        fun fromRouteKey(routeKey: String?): SettingsDestinationSpec? =
            entries.firstOrNull { it.routeKey == routeKey }

        fun fromDestination(destination: NavDestination?): SettingsDestinationSpec? {
            if (destination == null) return null
            return entries.firstOrNull { destination.hasRoute(it.routeClass) }
        }
    }
}
