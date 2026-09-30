package io.github.daisukikaffuchino.han1meviewer.ui.player

import android.content.Context
import android.view.Surface
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.cast.CastPlayer
import androidx.media3.cast.SessionAvailabilityListener
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.google.android.gms.cast.framework.CastContext
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
class CastPlaybackEngine private constructor(
    context: Context,
    private val localEngine: PlaybackEngine,
) : PlaybackEngine, Player.Listener, SessionAvailabilityListener {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val castContext = CastContext.getSharedInstance(context.applicationContext)
    private val castPlayer = CastPlayer(castContext)
    private var isCasting = castPlayer.isCastSessionAvailable()
    private val mutableState = MutableStateFlow(localStateWithCastStatus())
    private var localStateJob: Job? = null
    private var progressJob: Job? = null
    private var latestRequest: PlaybackRequest? = null
    private var attachedSurface: Surface? = null
    private var requestedPlaybackSpeed = PlayerDefaults.DEFAULT_SPEED
    private var lastCastPositionMs = 0L
    private var lastCastPlayWhenReady = false
    private var released = false

    override val state: StateFlow<PlaybackEngineState> = mutableState.asStateFlow()

    init {
        castPlayer.addListener(this)
        castPlayer.setSessionAvailabilityListener(this)
        localStateJob = scope.launch {
            localEngine.state.collect { localState ->
                if (!isCasting) mutableState.value = localState.withCastStatus()
            }
        }
        progressJob = scope.launch {
            while (isActive) {
                if (isCasting) publishCastState()
                delay(PROGRESS_UPDATE_INTERVAL_MS.milliseconds)
            }
        }
        if (isCasting) publishCastState()
    }

    override fun load(request: PlaybackRequest) {
        check(!released) { "Playback engine has already been released" }
        latestRequest = request
        if (isCasting) loadOnCast(request) else localEngine.load(request)
    }

    override fun play() {
        if (isCasting) castPlayer.play() else localEngine.play()
    }

    override fun pause() {
        if (isCasting) castPlayer.pause() else localEngine.pause()
    }

    override fun setLooping(looping: Boolean) {
        val repeatMode = if (looping) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        latestRequest = latestRequest?.copy(looping = looping)
        if (isCasting) {
            castPlayer.repeatMode = repeatMode
        } else {
            localEngine.setLooping(looping)
        }
    }

    override fun seekTo(positionMs: Long) {
        if (isCasting) castPlayer.seekTo(positionMs.coerceAtLeast(0L)) else localEngine.seekTo(positionMs)
    }

    override fun setPlaybackSpeed(speed: Float) {
        requestedPlaybackSpeed = speed.coerceIn(0.25f, 5f)
        if (isCasting) {
            if (castPlayer.isCommandAvailable(Player.COMMAND_SET_SPEED_AND_PITCH)) {
                castPlayer.playbackParameters = PlaybackParameters(requestedPlaybackSpeed)
            }
        } else {
            localEngine.setPlaybackSpeed(requestedPlaybackSpeed)
        }
    }

    override fun setVolume(volume: Float) {
        val safeVolume = volume.coerceIn(0f, 1f)
        if (isCasting) {
            try {
                castContext.sessionManager.currentCastSession?.setVolume(safeVolume.toDouble())
            } catch (error: Exception) {
                LogUtil.w(TAG, "Unable to change Google Cast volume: ${error.localizedMessage}")
            }
        } else {
            localEngine.setVolume(safeVolume)
        }
    }

    override fun attachSurface(surface: Surface) {
        attachedSurface = surface
        if (!isCasting) localEngine.attachSurface(surface)
    }

    override fun detachSurface(surface: Surface) {
        if (!isCasting) localEngine.detachSurface(surface)
        if (attachedSurface == surface) attachedSurface = null
    }

    override fun release() {
        if (released) return
        released = true
        localStateJob?.cancel()
        progressJob?.cancel()
        castPlayer.removeListener(this)
        castPlayer.setSessionAvailabilityListener(null)
        castPlayer.release()
        localEngine.release()
        scope.cancel()
    }

    override fun onCastSessionAvailable() {
        if (released || isCasting) return
        val localState = localEngine.state.value
        val shouldPlay = localState.isPlaying ||
                (localState.phase == PlaybackPhase.Preparing && latestRequest?.playWhenReady == true)
        lastCastPositionMs = localState.positionMs
        lastCastPlayWhenReady = shouldPlay
        isCasting = true
        attachedSurface?.let(localEngine::detachSurface)
        localEngine.pause()
        latestRequest?.let { request ->
            loadOnCast(
                request.copy(
                    startPositionMs = localState.positionMs,
                    playWhenReady = shouldPlay,
                )
            )
        } ?: publishCastState()
    }

    override fun onCastSessionUnavailable() {
        if (released || !isCasting) return
        rememberCastPosition()
        isCasting = false
        attachedSurface?.let(localEngine::attachSurface)
        latestRequest?.let { request ->
            localEngine.load(
                request.copy(
                    startPositionMs = lastCastPositionMs,
                    playWhenReady = lastCastPlayWhenReady,
                )
            )
            localEngine.setPlaybackSpeed(requestedPlaybackSpeed)
        }
        mutableState.value = localStateWithCastStatus()
    }

    override fun onEvents(player: Player, events: Player.Events) {
        if (isCasting) publishCastState()
    }

    override fun onPlayerError(error: PlaybackException) {
        LogUtil.e(TAG, "Google Cast playback failed", error)
        if (isCasting) publishCastState()
    }

    private fun loadOnCast(request: PlaybackRequest) {
        lastCastPositionMs = request.startPositionMs
        lastCastPlayWhenReady = request.playWhenReady
        mutableState.value = mutableState.value.copy(
            phase = PlaybackPhase.Preparing,
            isPlaying = false,
            isBuffering = true,
            positionMs = request.startPositionMs,
            errorMessage = null,
            isCastSupported = true,
            isCasting = true,
            castDeviceName = castDeviceName(),
        )
        castPlayer.repeatMode = if (request.looping) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        castPlayer.setMediaItem(request.toCastMediaItem(), request.startPositionMs)
        castPlayer.prepare()
        castPlayer.playWhenReady = request.playWhenReady
        if (castPlayer.isCommandAvailable(Player.COMMAND_SET_SPEED_AND_PITCH)) {
            castPlayer.playbackParameters = PlaybackParameters(requestedPlaybackSpeed)
        }
        publishCastState()
    }

    private fun publishCastState() {
        if (released || !isCasting) return
        rememberCastPosition()
        val duration = castPlayer.duration.takeUnless { it == C.TIME_UNSET }?.coerceAtLeast(0L) ?: 0L
        val localState = localEngine.state.value
        val error = castPlayer.playerError
        mutableState.value = PlaybackEngineState(
            phase = when {
                error != null -> PlaybackPhase.Error
                castPlayer.playbackState == Player.STATE_BUFFERING -> PlaybackPhase.Preparing
                castPlayer.playbackState == Player.STATE_READY -> PlaybackPhase.Ready
                castPlayer.playbackState == Player.STATE_ENDED -> PlaybackPhase.Ended
                else -> PlaybackPhase.Idle
            },
            isPlaying = castPlayer.isPlaying,
            isBuffering = castPlayer.playbackState == Player.STATE_BUFFERING,
            positionMs = lastCastPositionMs,
            durationMs = duration,
            bufferedPositionMs = castPlayer.bufferedPosition.coerceAtLeast(0L),
            playbackSpeed = castPlayer.playbackParameters.speed,
            videoWidth = localState.videoWidth,
            videoHeight = localState.videoHeight,
            hasRenderedFirstFrame = true,
            errorMessage = error?.localizedMessage,
            isCastSupported = true,
            isCasting = true,
            castDeviceName = castDeviceName(),
        )
    }

    private fun rememberCastPosition() {
        if (castPlayer.currentMediaItem == null) return
        castPlayer.currentPosition.takeIf { it != C.TIME_UNSET && it >= 0L }?.let {
            lastCastPositionMs = it
        }
        lastCastPlayWhenReady = castPlayer.playWhenReady &&
                castPlayer.playbackState != Player.STATE_ENDED
    }

    private fun localStateWithCastStatus(): PlaybackEngineState =
        localEngine.state.value.withCastStatus()

    private fun PlaybackEngineState.withCastStatus(): PlaybackEngineState = copy(
        isCastSupported = true,
        isCasting = false,
        castDeviceName = null,
    )

    private fun castDeviceName(): String? =
        castContext.sessionManager.currentCastSession?.castDevice?.friendlyName

    private fun PlaybackRequest.toCastMediaItem(): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .apply { artworkUri?.takeIf(String::isNotBlank)?.let { setArtworkUri(it.toUri()) } }
            .build()
        return MediaItem.Builder()
            .setUri(uri)
            .setMimeType(mimeType ?: inferMimeType(uri))
            .setMediaMetadata(metadata)
            .build()
    }

    private fun inferMimeType(uri: String): String = when {
        uri.substringBefore('?').endsWith(".m3u8", ignoreCase = true) -> HLS_MIME_TYPE
        uri.substringBefore('?').endsWith(".webm", ignoreCase = true) -> WEBM_MIME_TYPE
        else -> MP4_MIME_TYPE
    }

    companion object {
        private const val TAG = "CastPlaybackEngine"
        private const val PROGRESS_UPDATE_INTERVAL_MS = 250L
        private const val MP4_MIME_TYPE = "video/mp4"
        private const val WEBM_MIME_TYPE = "video/webm"
        private const val HLS_MIME_TYPE = "application/x-mpegURL"

        fun createOrLocal(context: Context, localEngine: PlaybackEngine): PlaybackEngine =
            try {
                CastPlaybackEngine(context, localEngine)
            } catch (error: Exception) {
                LogUtil.w(TAG, "Google Cast is unavailable: ${error.localizedMessage}")
                localEngine
            }
    }
}
