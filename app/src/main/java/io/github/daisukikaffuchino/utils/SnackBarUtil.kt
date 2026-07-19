package io.github.daisukikaffuchino.utils

import android.app.Activity
import android.view.View
import androidx.annotation.StringRes
import com.google.android.material.snackbar.Snackbar

inline fun Activity.showSnackBar(
    @StringRes message: Int,
    length: Int = Snackbar.LENGTH_SHORT,
    view: View = findViewById(android.R.id.content),
    action: Snackbar.() -> Unit = {},
) {
    Snackbar.make(view, message, length).apply(action).show()
}
