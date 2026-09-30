package io.github.daisukikaffuchino.han1meviewer.ui.player

import android.view.Surface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlaybackQuality(
    val label: String,
    val uri: String,
    val headers: Map<String, String> = emptyMap(),
    val mimeType: String? = null,
)

data class ComposePlaybackState(
    val title: String = "",
    val artworkUri: String? = null,
    val qualities: List<PlaybackQuality> = emptyList(),
    val selectedQualityIndex: Int = -1,
    val isLooping: Boolean = false,
    val engine: PlaybackEngineState = PlaybackEngineState(),
)

/**
 * UI-facing coordinator. It owns source switching and preserves the current position while
 * delegating actual decoding and rendering to a selected [PlaybackEngine].
 */
class ComposePlaybackController(
    private val playbackEngine: PlaybackEngine,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
) {
    private val mutableState = MutableStateFlow(ComposePlaybackState())
    private var engineCollectionJob: Job = scope.launch {
        playbackEngine.state.collect { engineState ->
            mutableState.update { it.copy(engine = engineState) }
        }
    }
    private var requestedPlaybackSpeed = PlayerDefaults.DEFAULT_SPEED
    private var requestedLooping = false

    val state: StateFlow<ComposePlaybackState> = mutableState.asStateFlow()

    fun load(
        title: String,
        qualities: List<PlaybackQuality>,
        preferredQuality: String? = null,
        artworkUri: String? = null,
        startPositionMs: Long = 0L,
        playWhenReady: Boolean = true,
        looping: Boolean = false,
    ) {
        requestedLooping = looping
        val selectedIndex = qualities.indexOfFirst { it.label == preferredQuality }
            .takeIf { it >= 0 }
            ?: qualities.lastIndex
        mutableState.value = ComposePlaybackState(
            title = title,
            artworkUri = artworkUri,
            qualities = qualities,
            selectedQualityIndex = selectedIndex,
            isLooping = looping,
        )
        if (selectedIndex >= 0) {
            loadQuality(selectedIndex, startPositionMs, playWhenReady, artworkUri)
        }
    }

    fun selectQuality(index: Int) {
        if (index !in mutableState.value.qualities.indices || index == mutableState.value.selectedQualityIndex) {
            return
        }
        val engineState = mutableState.value.engine
        loadQuality(index, engineState.positionMs, engineState.isPlaying)
    }

    fun play() = playbackEngine.play()

    fun pause() = playbackEngine.pause()

    fun setLooping(looping: Boolean) {
        requestedLooping = looping
        mutableState.update { it.copy(isLooping = looping) }
        playbackEngine.setLooping(looping)
    }

    fun togglePlayPause() {
        if (mutableState.value.engine.isPlaying) pause() else play()
    }

    fun replay() {
        val selectedIndex = mutableState.value.selectedQualityIndex
        if (selectedIndex in mutableState.value.qualities.indices) {
            loadQuality(selectedIndex, positionMs = 0L, playWhenReady = true)
        }
    }

    fun seekTo(positionMs: Long) = playbackEngine.seekTo(positionMs)

    fun seekBy(deltaMs: Long) {
        val state = mutableState.value.engine
        seekTo((state.positionMs + deltaMs).coerceAtLeast(0L))
    }

    fun setPlaybackSpeed(speed: Float) {
        requestedPlaybackSpeed = speed.coerceIn(0.25f, 5f)
        playbackEngine.setPlaybackSpeed(requestedPlaybackSpeed)
    }

    fun setVolume(volume: Float) = playbackEngine.setVolume(volume)

    fun release() {
        engineCollectionJob.cancel()
        playbackEngine.release()
        scope.coroutineContext.cancel()
    }

    private fun loadQuality(
        index: Int,
        positionMs: Long,
        playWhenReady: Boolean,
        artworkUri: String? = mutableState.value.artworkUri,
    ) {
        val quality = mutableState.value.qualities[index]
        mutableState.update { it.copy(selectedQualityIndex = index) }
        playbackEngine.load(
            PlaybackRequest(
                uri = quality.uri,
                headers = quality.headers,
                title = mutableState.value.title,
                artworkUri = artworkUri,
                mimeType = quality.mimeType,
                startPositionMs = positionMs,
                playWhenReady = playWhenReady,
                looping = requestedLooping,
            )
        )
        playbackEngine.setPlaybackSpeed(requestedPlaybackSpeed)
    }
}
