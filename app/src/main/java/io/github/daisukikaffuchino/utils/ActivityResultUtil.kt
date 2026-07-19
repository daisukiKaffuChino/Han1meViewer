package io.github.daisukikaffuchino.utils

import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private val activityResultRequestCode = AtomicInteger()

suspend fun <I, O> Context.awaitActivityResult(
    contract: ActivityResultContract<I, O>,
    input: I,
): O {
    val key = "activity_rq#${activityResultRequestCode.getAndIncrement()}"
    val activity = requireComponentActivity()
    val lifecycle = activity.lifecycle
    var launcher: ActivityResultLauncher<I>? = null
    val observer = object : LifecycleEventObserver {
        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            if (event == Lifecycle.Event.ON_DESTROY) {
                launcher?.unregister()
                lifecycle.removeObserver(this)
            }
        }
    }

    return withContext(Dispatchers.Main) {
        lifecycle.addObserver(observer)
        suspendCoroutine(object : Function1<Continuation<O>, Unit> {
            private var resumed = false

            override fun invoke(continuation: Continuation<O>) {
                launcher = activity.activityResultRegistry.register(key, contract) {
                    if (!resumed) {
                        resumed = true
                        launcher?.unregister()
                        lifecycle.removeObserver(observer)
                        continuation.resume(it)
                    }
                }.apply { launch(input) }
            }
        })
    }
}

suspend fun Context.requestPermission(permission: String): Boolean {
    if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
        return true
    }
    return awaitActivityResult(ActivityResultContracts.RequestPermission(), permission)
}
