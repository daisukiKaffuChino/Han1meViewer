package io.github.daisukikaffuchino.han1meviewer.ui.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import io.github.daisukikaffuchino.han1meviewer.ui.theme.HanimeTheme

abstract class BaseActivity : AppCompatActivity() {

    protected open fun beforeSuperOnCreate(savedInstanceState: Bundle?) = Unit

    protected open fun onActivityCreated(savedInstanceState: Bundle?) = Unit

    final override fun onCreate(savedInstanceState: Bundle?) {
        beforeSuperOnCreate(savedInstanceState)
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        onActivityCreated(savedInstanceState)
    }

    protected fun setHanimeContent(content: @Composable () -> Unit) {
        setContent {
            HanimeTheme(content = content)
        }
    }
}
