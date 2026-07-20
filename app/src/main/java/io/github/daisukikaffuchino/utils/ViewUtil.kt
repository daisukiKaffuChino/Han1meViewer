package io.github.daisukikaffuchino.utils

import android.annotation.SuppressLint
import android.content.res.Resources
import android.view.View
import android.view.ViewGroup
import androidx.core.util.TypedValueCompat

private val Number.dpFloat: Float
    get() = TypedValueCompat.dpToPx(
        toFloat(),
        applicationContext.resources.displayMetrics,
    )

val Number.dp: Int
    get() {
        val px = dpFloat
        return (if (px >= 0) px + 0.5f else px - 0.5f).toInt()
    }

val appScreenWidth: Int
    get() = applicationContext.resources.displayMetrics.widthPixels

val statusBarHeight: Int
    @SuppressLint("DiscouragedApi", "InternalInsetResource")
    get() {
        val resources: Resources = applicationContext.resources
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return resources.getDimensionPixelSize(resourceId)
    }

val navBarHeight: Int
    @SuppressLint("DiscouragedApi", "InternalInsetResource")
    get() {
        val resources: Resources = applicationContext.resources
        val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        return resources.getDimensionPixelSize(resourceId)
    }

fun View.removeItself() {
    (parent as? ViewGroup)?.removeView(this)
}
