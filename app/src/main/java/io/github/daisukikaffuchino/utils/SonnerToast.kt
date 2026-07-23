package io.github.daisukikaffuchino.utils

import android.os.Handler
import android.os.Looper
import androidx.annotation.StringRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.luminance
import com.dokar.sonner.ToastType
import com.dokar.sonner.Toaster
import com.dokar.sonner.ToasterDefaults
import com.dokar.sonner.ToasterState
import com.dokar.sonner.rememberToasterState
import java.util.ArrayDeque
import kotlin.time.Duration

/**
 * 应用内唯一的 Sonner Toast 入口。
 *
 * 调用方可以位于 Compose、Activity、ViewModel、Worker 或传统 View 中；请求会切换到主线程，
 * 并在下一个可用的 Activity 宿主显示。不要直接使用 Android [android.widget.Toast]。
 */
object SonnerToast {
    private data class Request(
        val message: String,
        val type: ToastType,
        val duration: Duration,
    )

    private const val MAX_PENDING_REQUESTS = 10

    private val mainHandler = Handler(Looper.getMainLooper())
    private val pendingRequests = ArrayDeque<Request>()
    private var toasterState: ToasterState? = null

    /** Adds the shared toast host to an Activity's root Compose content. */
    @Composable
    fun Host() {
        val state = rememberToasterState()
        val darkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f

        DisposableEffect(state) {
            attach(state)
            onDispose { detach(state) }
        }

        Toaster(
            state = state,
            maxVisibleToasts = 3,
            richColors = true,
            darkTheme = darkTheme,
            showCloseButton = true,
        )
    }

    fun show(
        message: String?,
        type: ToastType = ToastType.Normal,
        duration: Duration = ToasterDefaults.DurationDefault,
    ) {
        val text = message?.trim().orEmpty()
        if (text.isEmpty()) return
        mainHandler.post { deliver(Request(text, type, duration)) }
    }

    fun show(
        @StringRes resId: Int,
        vararg formatArgs: Any,
        type: ToastType = ToastType.Normal,
        duration: Duration = ToasterDefaults.DurationDefault,
    ) {
        show(applicationContext.getString(resId, *formatArgs), type, duration)
    }

    fun success(message: String?) = show(message, ToastType.Success)

    fun success(@StringRes resId: Int, vararg formatArgs: Any) =
        show(resId, *formatArgs, type = ToastType.Success)

    fun info(message: String?) = show(message, ToastType.Info)

    fun info(@StringRes resId: Int, vararg formatArgs: Any) =
        show(resId, *formatArgs, type = ToastType.Info)

    fun warning(message: String?) = show(message, ToastType.Warning)

    fun warning(@StringRes resId: Int, vararg formatArgs: Any) =
        show(resId, *formatArgs, type = ToastType.Warning)

    fun error(message: String?) = show(message, ToastType.Error, ToasterDefaults.DurationLong)

    fun error(@StringRes resId: Int, vararg formatArgs: Any) =
        show(resId, *formatArgs, type = ToastType.Error, duration = ToasterDefaults.DurationLong)

    @Suppress("UNUSED_PARAMETER")
    fun dismissAll() {
        mainHandler.post { toasterState?.dismissAll() }
    }

    private fun attach(state: ToasterState) {
        toasterState = state
        while (pendingRequests.isNotEmpty()) {
            val request = pendingRequests.removeFirst()
            state.show(request.message, type = request.type, duration = request.duration)
        }
    }

    private fun detach(state: ToasterState) {
        if (toasterState === state) toasterState = null
    }

    private fun deliver(request: Request) {
        val state = toasterState
        if (state == null) {
            if (pendingRequests.size == MAX_PENDING_REQUESTS) pendingRequests.removeFirst()
            pendingRequests.addLast(request)
            return
        }
        state.show(request.message, type = request.type, duration = request.duration)
    }
}
