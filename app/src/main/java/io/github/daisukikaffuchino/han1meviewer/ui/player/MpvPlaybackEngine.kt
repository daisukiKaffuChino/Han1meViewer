package io.github.daisukikaffuchino.han1meviewer.ui.player

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.view.Surface
import androidx.core.net.toUri
import io.github.daisukikaffuchino.han1meviewer.BuildConfig
import io.github.daisukikaffuchino.han1meviewer.logic.SettingsRepository
import io.github.daisukikaffuchino.han1meviewer.USER_AGENT
import io.github.daisukikaffuchino.han1meviewer.logic.network.HProxySelector
import io.github.daisukikaffuchino.han1meviewer.util.AnimeShaders
import io.github.daisukikaffuchino.han1meviewer.util.AnimeShaders.getCert
import `is`.xyz.mpv.MPVLib
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class MpvPlaybackEngine(
    private val context: Context,
) : PlaybackEngine {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mutableState = MutableStateFlow(PlaybackEngineState())
    private var currentSurface: Surface? = null
    private var currentPfd: ParcelFileDescriptor? = null
    private var detachedFd: Int? = null
    private var pendingRequest: PlaybackRequest? = null
    private var initialized = false
    private var released = false
    private var surfaceWidth = 0
    private var surfaceHeight = 0
    private var lastVideoWidth = 0
    private var lastVideoHeight = 0
    private var hasRenderedFrame = false
    private var hasReachedEndOfFile = false
    private var isLooping = false
    private var lastKnownPositionMs = 0L
    private var lastKnownDurationMs = 0L
    private val observer = object : MPVLib.EventObserver {
        override fun eventProperty(property: String) = publishState()
        override fun eventProperty(property: String, value: Double) = publishState()
        override fun eventProperty(property: String, value: Long) = publishState()
        override fun eventProperty(property: String, value: Boolean) {
            if (property == "eof-reached") hasReachedEndOfFile = value
            publishState()
        }
        override fun eventProperty(property: String, value: String) = publishState()

        override fun event(eventId: Int) {
            when (eventId) {
                MPVLib.mpvEventId.MPV_EVENT_START_FILE -> {
                    hasReachedEndOfFile = false
                    lastKnownPositionMs = 0L
                    lastKnownDurationMs = 0L
                    mutableState.value = mutableState.value.copy(
                        phase = PlaybackPhase.Preparing,
                        isBuffering = true,
                        errorMessage = null,
                    )
                }

                MPVLib.mpvEventId.MPV_EVENT_FILE_LOADED -> {
                    pendingRequest?.let { request ->
                        MPVLib.setPropertyDouble("speed", requestSpeed.toDouble())
                        if (request.startPositionMs > 0L) {
                            seekTo(request.startPositionMs)
                        }
                        if (request.playWhenReady) startPlayback()
                    }
                    mutableState.value = mutableState.value.copy(
                        phase = PlaybackPhase.Ready,
                        isBuffering = false,
                    )
                }

                MPVLib.mpvEventId.MPV_EVENT_END_FILE -> {
                    val playbackState = mutableState.value
                    val reachedRecordedDuration = lastKnownDurationMs > 0L &&
                            lastKnownPositionMs >=
                            (lastKnownDurationMs - NORMAL_END_TOLERANCE_MS).coerceAtLeast(0L)
                    val endedNormally = hasReachedEndOfFile ||
                            MPVLib.getPropertyBoolean("eof-reached") == true ||
                            reachedRecordedDuration
                    if (isLooping && endedNormally) return@event
                    mutableState.value = playbackState.copy(
                        phase = if (endedNormally) PlaybackPhase.Ended else PlaybackPhase.Error,
                        isPlaying = false,
                        isBuffering = false,
                        errorMessage = if (endedNormally) null else "Playback failed before reaching end of file",
                    )
                }

                MPVLib.mpvEventId.MPV_EVENT_SHUTDOWN -> {
                    mutableState.value = PlaybackEngineState()
                }
            }
        }
    }
    private var requestSpeed = PlayerDefaults.DEFAULT_SPEED

    override val state: StateFlow<PlaybackEngineState> = mutableState.asStateFlow()

    override fun load(request: PlaybackRequest) {
        check(!released) { "Playback engine has already been released" }
        initializeIfNeeded()
        pendingRequest = request
        isLooping = request.looping
        lastVideoWidth = 0
        lastVideoHeight = 0
        hasRenderedFrame = false
        MPVLib.setPropertyBoolean("pause", true)
        MPVLib.command(arrayOf("loadfile", "", "replace"))
        val path = prepareUri(request.uri.toUri())
        if (path == null) {
            mutableState.value = mutableState.value.copy(
                phase = PlaybackPhase.Error,
                errorMessage = "Unable to open media URI",
            )
            return
        }
        MPVLib.setOptionString("force-window", "yes")
        MPVLib.command(arrayOf("loadfile", path, "replace"))
        currentSurface?.let {
            MPVLib.attachSurface(it)
            applySurfaceSize()
        }
        mutableState.value = mutableState.value.copy(
            phase = PlaybackPhase.Preparing,
            isBuffering = true,
            errorMessage = null,
            videoWidth = 0,
            videoHeight = 0,
            hasRenderedFirstFrame = false,
        )
    }

    override fun play() = startPlayback()

    override fun pause() {
        MPVLib.setPropertyBoolean("pause", true)
        publishState()
    }

    override fun setLooping(looping: Boolean) {
        isLooping = looping
        MPVLib.setPropertyString("loop-file", if (looping) "inf" else "no")
    }

    override fun seekTo(positionMs: Long) {
        MPVLib.command(arrayOf("seek", (positionMs.coerceAtLeast(0L) / 1000.0).toString(), "absolute", "exact"))
        publishState()
    }

    override fun setPlaybackSpeed(speed: Float) {
        requestSpeed = speed.coerceIn(0.25f, 5f)
        MPVLib.setPropertyDouble("speed", requestSpeed.toDouble())
        publishState()
    }

    override fun setVolume(volume: Float) {
        MPVLib.setPropertyDouble("volume", (volume.coerceIn(0f, 1f) * 100f).toDouble())
    }

    override fun attachSurface(surface: Surface) {
        if (released) return
        currentSurface = surface
        if (initialized) {
            MPVLib.attachSurface(surface)
            MPVLib.setOptionString("force-window", "yes")
            MPVLib.setPropertyString("vo", videoOutput)
            applySurfaceSize()
        }
    }

    override fun detachSurface(surface: Surface) {
        if (released) return
        if (currentSurface == surface) {
            currentSurface = null
            if (initialized) {
                MPVLib.setPropertyString("vo", "null")
                MPVLib.setOptionString("force-window", "no")
                MPVLib.detachSurface()
            }
        }
    }

    fun updateSurfaceSize(width: Int, height: Int) {
        if (released || width <= 0 || height <= 0) return
        surfaceWidth = width
        surfaceHeight = height
        applySurfaceSize()
    }

    fun setSuperResolution(index: Int) {
        val shader = AnimeShaders.getShader(context, index)
        MPVLib.command(arrayOf("change-list", "glsl-shaders", "set", shader))
    }

    override fun release() {
        if (released) return
        released = true
        if (initialized) {
            MPVLib.setPropertyBoolean("pause", true)
            MPVLib.command(arrayOf("loadfile", "", "replace"))
            MPVLib.setOptionString("force-window", "no")
            MPVLib.detachSurface()
            currentSurface = null
            MPVLib.removeObserver(observer)
        }
        closeCurrentFile()
        scope.cancel()
        mutableState.value = PlaybackEngineState()
    }

    private fun initializeIfNeeded() {
        if (initialized) return
        mpvOptions().forEach { (key, value) -> MPVLib.setOptionString(key, value) }
        parseCustomMpvParams().forEach { (key, value) -> MPVLib.setOptionString(key, value) }
        MPVLib.observeProperty("time-pos", MPVLib.mpvFormat.MPV_FORMAT_DOUBLE)
        MPVLib.observeProperty("duration", MPVLib.mpvFormat.MPV_FORMAT_DOUBLE)
        MPVLib.observeProperty("pause", MPVLib.mpvFormat.MPV_FORMAT_FLAG)
        MPVLib.observeProperty("eof-reached", MPVLib.mpvFormat.MPV_FORMAT_FLAG)
        MPVLib.observeProperty("video-params/w", MPVLib.mpvFormat.MPV_FORMAT_INT64)
        MPVLib.observeProperty("video-params/h", MPVLib.mpvFormat.MPV_FORMAT_INT64)
        MPVLib.observeProperty("demuxer-cache-duration", MPVLib.mpvFormat.MPV_FORMAT_DOUBLE)
        MPVLib.addObserver(observer)
        initialized = true
        scope.launch {
            while (isActive) {
                publishState()
                delay(250L.milliseconds)
            }
        }
    }

    private fun startPlayback() {
        MPVLib.setPropertyBoolean("pause", false)
        publishState()
    }

    private fun applySurfaceSize() {
        if (!initialized || released || currentSurface == null) return
        if (surfaceWidth <= 0 || surfaceHeight <= 0) return
        MPVLib.setPropertyString("android-surface-size", "${surfaceWidth}x${surfaceHeight}")
        if (MPVLib.getPropertyBoolean("pause") == true) {
            MPVLib.command(arrayOf("seek", "0", "relative", "exact"))
        }
    }

    private fun publishState() {
        if (!initialized || released) return
        MPVLib.getPropertyDouble("time-pos")?.let {
            lastKnownPositionMs = (it * 1000).toLong().coerceAtLeast(0L)
        }
        MPVLib.getPropertyDouble("duration")?.let {
            lastKnownDurationMs = (it * 1000).toLong().coerceAtLeast(0L)
        }
        val buffered = MPVLib.getPropertyDouble("demuxer-cache-duration") ?: 0.0
        val paused = MPVLib.getPropertyBoolean("pause") ?: true
        val width = MPVLib.getPropertyInt("video-params/w") ?: 0
        val height = MPVLib.getPropertyInt("video-params/h") ?: 0
        if (width > 0 && height > 0) {
            lastVideoWidth = width
            lastVideoHeight = height
            hasRenderedFrame = true
        }
        mutableState.value = mutableState.value.copy(
            isPlaying = !paused,
            isBuffering = !paused && lastKnownDurationMs > 0L && lastKnownPositionMs == 0L,
            positionMs = lastKnownPositionMs,
            durationMs = lastKnownDurationMs,
            bufferedPositionMs = (lastKnownPositionMs + buffered * 1000).toLong().coerceAtLeast(0L),
            playbackSpeed = requestSpeed,
            videoWidth = lastVideoWidth,
            videoHeight = lastVideoHeight,
            hasRenderedFirstFrame = hasRenderedFrame,
        )
    }

    private fun prepareUri(uri: Uri): String? {
        return when (uri.scheme) {
            "http", "https" -> uri.toString()
            "file", "content" -> {
                closeCurrentFile()
                currentPfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return null
                detachedFd = currentPfd?.detachFd()
                detachedFd?.let { "fd://$it" }
            }
            else -> null
        }
    }

    private fun closeCurrentFile() {
        currentPfd?.close()
        detachedFd?.let { runCatching { ParcelFileDescriptor.adoptFd(it).close() } }
        currentPfd = null
        detachedFd = null
    }

    private fun mpvOptions(): Map<String, String> = buildMap {
        put("vo", videoOutput)
        put("profile", SettingsRepository.mpvProfile.takeIf { it == "gpu-hq" || it == "fast" } ?: "default")
        put("hwdec", when (SettingsRepository.mpvHwdec) {
            "HW" -> "mediacodec-copy"
            "HW+" -> "mediacodec"
            "Vulkan" -> "vulkan-copy"
            "vulkan+" -> "vulkan"
            "SW" -> "no"
            else -> "auto"
        })
        put("msg-level", "all=" + if (BuildConfig.DEBUG) "debug" else "warn")
        put("cache", "yes")
        put("cache-secs", SettingsRepository.mpvCacheSecs.toString())
        put("vd-lavc-threads", Runtime.getRuntime().availableProcessors().toString())
        put("framedrop", if (SettingsRepository.mpvFramedrop) "vo" else "no")
        put("deband", if (SettingsRepository.mpvDeband) "yes" else "no")
        put("cache-pause", "no")
        put("network-timeout", SettingsRepository.mpvNetworkTimeout.toString())
        put("tls-ca-file", getCert(context))
        put("tls-verify", if (SettingsRepository.mpvTlsVerify) "no" else "yes")
        put("user-agent", USER_AGENT)
        SettingsRepository.proxyIp.takeIf { it.isNotBlank() && SettingsRepository.proxyPort != -1 }?.let { ip ->
            if (SettingsRepository.proxyType == HProxySelector.TYPE_HTTP) {
                put("http-proxy", "http://$ip:${SettingsRepository.proxyPort}")
            }
        }
        if (SettingsRepository.mpvInterpolation) {
            put("interpolation", "yes")
            put("tscale", "oversample")
            put("video-sync", "display-resample")
        }
    }

    private fun parseCustomMpvParams(): Map<String, String> = buildMap {
        SettingsRepository.customMpvParams.split(';').forEach { entry ->
            val parts = entry.trim().split(',', limit = 2)
            if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
                put(parts[0].trim(), parts[1].trim())
            }
        }
    }

    private val videoOutput: String
        get() = if (SettingsRepository.enableGPUNextRenderer) "gpu-next" else "gpu"

    private companion object {
        const val NORMAL_END_TOLERANCE_MS = 3_000L
    }
}
