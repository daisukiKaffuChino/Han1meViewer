package io.github.daisukikaffuchino.han1meviewer.ui.navigation.settings

import io.github.daisukikaffuchino.han1meviewer.R
import io.github.daisukikaffuchino.han1meviewer.ui.navigation.main.HanimeScreen
import kotlinx.serialization.Serializable

@Serializable
object HomeSettingsRoute : HanimeScreen

@Serializable
object VideoPlaybackSettingsRoute : HanimeScreen

@Serializable
object NetworkDownloadSettingsRoute : HanimeScreen

@Serializable
object AppearanceSettingsRoute : HanimeScreen

@Serializable
object InterfaceInteractionSettingsRoute : HanimeScreen

@Serializable
object DataPrivacySettingsRoute : HanimeScreen

@Serializable
object DeveloperOptionsSettingsRoute : HanimeScreen

@Serializable
object AboutSettingsRoute : HanimeScreen

@Serializable
object OpenSourceLicensesRoute : HanimeScreen

@Serializable
object PlayerSettingsRoute : HanimeScreen

@Serializable
object NetworkSettingsRoute : HanimeScreen

@Serializable
object DownloadSettingsRoute : HanimeScreen

@Serializable
object EchTestSettingsRoute : HanimeScreen

@Serializable
object MpvPlayerSettingsRoute : HanimeScreen

@Serializable
object HKeyframesRoute : HanimeScreen

@Serializable
object SharedHKeyframesRoute : HanimeScreen

@Serializable
object HKeyframeSettingsRoute : HanimeScreen

enum class SettingsDestinationSpec(
    val titleRes: Int,
    val showToolbar: Boolean = true,
) {
    Home(
        titleRes = R.string.settings,
    ),
    VideoPlayback(
        titleRes = R.string.settings_video_playback,
    ),
    NetworkDownload(
        titleRes = R.string.settings_network_download,
    ),
    Appearance(
        titleRes = R.string.settings_appearance,
    ),
    InterfaceInteraction(
        titleRes = R.string.settings_interface_interaction,
    ),
    DataPrivacy(
        titleRes = R.string.settings_data_privacy,
    ),
    DeveloperOptions(
        titleRes = R.string.developer_options,
    ),
    About(
        titleRes = R.string.about,
    ),
    OpenSourceLicenses(
        titleRes = R.string.open_source_license,
    ),
    Player(
        titleRes = R.string.player_settings,
    ),
    Network(
        titleRes = R.string.network_settings,
    ),
    Download(
        titleRes = R.string.download_settings,
    ),
    EchTest(
        titleRes = R.string.ech_test,
    ),
    Mpv(
        titleRes = R.string.mpv_advanced_settings,
    ),
    HKeyframes(
        titleRes = R.string.h_keyframe_manage,
    ),
    SharedHKeyframes(
        titleRes = R.string.shared_h_keyframe_manage,
    ),
    HKeyframeSettings(
        titleRes = R.string.h_keyframe_settings,
    );

    val route: HanimeScreen
        get() = when (this) {
            Home -> HomeSettingsRoute
            VideoPlayback -> VideoPlaybackSettingsRoute
            NetworkDownload -> NetworkDownloadSettingsRoute
            Appearance -> AppearanceSettingsRoute
            InterfaceInteraction -> InterfaceInteractionSettingsRoute
            DataPrivacy -> DataPrivacySettingsRoute
            DeveloperOptions -> DeveloperOptionsSettingsRoute
            About -> AboutSettingsRoute
            OpenSourceLicenses -> OpenSourceLicensesRoute
            Player -> PlayerSettingsRoute
            Network -> NetworkSettingsRoute
            Download -> DownloadSettingsRoute
            EchTest -> EchTestSettingsRoute
            Mpv -> MpvPlayerSettingsRoute
            HKeyframes -> HKeyframesRoute
            SharedHKeyframes -> SharedHKeyframesRoute
            HKeyframeSettings -> HKeyframeSettingsRoute
        }
}
