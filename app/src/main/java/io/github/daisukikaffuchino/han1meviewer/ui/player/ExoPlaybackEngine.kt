package io.github.daisukikaffuchino.han1meviewer.ui.player

import android.content.Context
import android.view.Surface
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import io.github.daisukikaffuchino.han1meviewer.USER_AGENT
import io.github.daisukikaffuchino.utils.LogUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@OptIn(UnstableApi::class)
class ExoPlaybackEngine(
    context: Context,
) : PlaybackEngine, Player.Listener {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val player = ExoPlayer.Builder(context.applicationContext).build().apply {
        addListener(this@ExoPlaybackEngine)
    }
    private val appContext = context.applicationContext
    private val mutableState = MutableStateFlow(PlaybackEngineState())
    private var progressJob: Job? = null
    private var released = false

    override val state: StateFlow<PlaybackEngineState> = mutableState.asStateFlow()

    override fun load(request: PlaybackRequest) {
        check(!released) { "Playback engine has already been released" }
        mutableState.value = mutableState.value.copy(
            phase = PlaybackPhase.Preparing,
            isBuffering = true,
            errorMessage = null,
            videoWidth = 0,
            videoHeight = 0,
            hasRenderedFirstFrame = false,
        )
        player.repeatMode = if (request.looping) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        player.setMediaSource(createMediaSource(request))
        player.prepare()
        if (request.startPositionMs > 0L) {
            player.seekTo(request.startPositionMs)
        }
        player.playWhenReady = request.playWhenReady
        startProgressUpdates()
        publishState()
    }

    override fun play() {
        player.play()
        publishState()
    }

    override fun pause() {
        player.pause()
        publishState()
    }

    override fun setLooping(looping: Boolean) {
        player.repeatMode = if (looping) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
    }

    override fun seekTo(positionMs: Long) {
        val duration = player.duration.takeIf { it != C.TIME_UNSET && it > 0L }
        player.seekTo(positionMs.coerceIn(0L, duration ?: Long.MAX_VALUE))
        publishState()
    }

    override fun setPlaybackSpeed(speed: Float) {
        val safeSpeed = speed.coerceIn(0.25f, 5f)
        player.playbackParameters = PlaybackParameters(safeSpeed)
        publishState()
    }

    override fun setVolume(volume: Float) {
        player.volume = volume.coerceIn(0f, 1f)
    }

    override fun attachSurface(surface: Surface) {
        if (released) return
        player.setVideoSurface(surface)
    }

    override fun detachSurface(surface: Surface) {
        if (released) return
        player.clearVideoSurface(surface)
    }

    override fun release() {
        if (released) return
        released = true
        progressJob?.cancel()
        player.removeListener(this)
        player.clearVideoSurface()
        player.release()
        scope.cancel()
        mutableState.value = PlaybackEngineState()
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        publishState()
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        publishState()
    }

    override fun onIsLoadingChanged(isLoading: Boolean) {
        publishState()
    }

    override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
        publishState()
    }

    override fun onVideoSizeChanged(videoSize: VideoSize) {
        publishState(videoSize)
    }

    override fun onRenderedFirstFrame() {
        mutableState.value = mutableState.value.copy(hasRenderedFirstFrame = true)
    }

    override fun onPlayerError(error: PlaybackException) {
        progressJob?.cancel()
        LogUtil.e(TAG, "Playback failed", error)
        mutableState.value = mutableState.value.copy(
            phase = PlaybackPhase.Error,
            isPlaying = false,
            isBuffering = false,
            errorMessage = error.localizedMessage,
        )
    }

    private fun startProgressUpdates() {
        if (progressJob?.isActive == true) return
        progressJob = scope.launch {
            while (isActive) {
                publishState()
                delay(PROGRESS_UPDATE_INTERVAL_MS.milliseconds)
            }
        }
    }

    private fun publishState(videoSize: VideoSize = player.videoSize) {
        if (released) return
        val duration = player.duration.takeUnless { it == C.TIME_UNSET }?.coerceAtLeast(0L) ?: 0L
        mutableState.value = mutableState.value.copy(
            phase = when (player.playbackState) {
                Player.STATE_BUFFERING -> PlaybackPhase.Preparing
                Player.STATE_READY -> PlaybackPhase.Ready
                Player.STATE_ENDED -> PlaybackPhase.Ended
                else -> PlaybackPhase.Idle
            },
            isPlaying = player.isPlaying,
            isBuffering = player.isLoading || player.playbackState == Player.STATE_BUFFERING,
            positionMs = player.currentPosition.coerceAtLeast(0L),
            durationMs = duration,
            bufferedPositionMs = player.bufferedPosition.coerceAtLeast(0L),
            playbackSpeed = player.playbackParameters.speed,
            videoWidth = (videoSize.width * videoSize.pixelWidthHeightRatio).toInt(),
            videoHeight = videoSize.height,
            errorMessage = null,
        )
    }

    private fun createMediaSource(request: PlaybackRequest): MediaSource {
        val httpFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(USER_AGENT)
            .setDefaultRequestProperties(request.headers)
        val dataSourceFactory = DefaultDataSource.Factory(appContext, httpFactory)
        val item = MediaItem.fromUri(request.uri.toUri())
        return if (request.uri.substringBefore('?').endsWith(".m3u8", ignoreCase = true)) {
            HlsMediaSource.Factory(dataSourceFactory).createMediaSource(item)
        } else {
            ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(item)
        }
    }

    private companion object {
        const val TAG = "ExoPlaybackEngine"
        const val PROGRESS_UPDATE_INTERVAL_MS = 250L
    }
}
