package io.github.daisukikaffuchino.utils

import android.view.View
import android.view.ViewGroup

fun View.removeItself() {
    (parent as? ViewGroup)?.removeView(this)
}
