package io.github.daisukikaffuchino.utils

import android.app.Application
import androidx.lifecycle.AndroidViewModel

open class ApplicationViewModel(
    @JvmField protected val application: Application,
) : AndroidViewModel(application) {

    inline fun <reified VM : ApplicationViewModel> sub() = sub(VM::class.java)

    fun <VM : ApplicationViewModel> sub(clazz: Class<VM>): Lazy<VM> = unsafeLazy {
        clazz.getConstructor(Application::class.java).newInstance(application)
    }
}
