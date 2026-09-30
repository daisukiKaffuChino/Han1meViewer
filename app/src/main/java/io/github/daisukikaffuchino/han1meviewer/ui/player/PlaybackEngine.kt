package io.github.daisukikaffuchino.han1meviewer.ui.player

import android.view.Surface
import kotlinx.coroutines.flow.StateFlow

typealias PlayerKernel = io.github.daisukikaffuchino.han1meviewer.logic.model.PlayerKernel

object PlayerDefaults {
    const val DEFAULT_SPEED = 1f
    const val DEFAULT_SPEED_INDEX = 2
    const val DEFAULT_PROGRESS_SLIDE_SENSITIVITY = 4
    const val DEFAULT_LONG_PRESS_SPEED_MULTIPLIER = 2.5f
    const val DEFAULT_COUNTDOWN_SECONDS = 10

    val speeds = floatArrayOf(
        0.5f,
        0.75f,
        DEFAULT_SPEED,
        1.25f,
        1.5f,
        1.75f,
        2f,
        2.25f,
        2.5f,
        2.75f,
        3f,
    )

    val speedLabels: Array<String>
        get() = Array(speeds.size) { "${speeds[it]}x" }
}

enum class PlaybackPhase {
    Idle,
    Preparing,
    Ready,
    Ended,
    Error,
}

data class PlaybackEngineState(
    val phase: PlaybackPhase = PlaybackPhase.Idle,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
    val playbackSpeed: Float = 1f,
    val videoWidth: Int = 0,
    val videoHeight: Int = 0,
    val hasRenderedFirstFrame: Boolean = false,
    val errorMessage: String? = null,
    val isCastSupported: Boolean = false,
    val isCasting: Boolean = false,
    val castDeviceName: String? = null,
)

data class PlaybackRequest(
    val uri: String,
    val headers: Map<String, String> = emptyMap(),
    val title: String = "",
    val artworkUri: String? = null,
    val mimeType: String? = null,
    val startPositionMs: Long = 0L,
    val playWhenReady: Boolean = true,
    val looping: Boolean = false,
)

interface PlaybackEngine {
    val state: StateFlow<PlaybackEngineState>

    fun load(request: PlaybackRequest)
    fun play()
    fun pause()
    fun setLooping(looping: Boolean)
    fun seekTo(positionMs: Long)
    fun setPlaybackSpeed(speed: Float)
    fun setVolume(volume: Float)
    fun attachSurface(surface: Surface)
    fun detachSurface(surface: Surface)
    fun release()
}
