package io.github.daisukikaffuchino.han1meviewer.ui.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import io.github.daisukikaffuchino.han1meviewer.ui.screen.login.ManualInputCookiesScreen

class ManualInputCookiesActivity : BaseActivity() {
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        setHanimeContent {
            ManualInputCookiesScreen(
                onBack = { finish() },
                onCookieScanned = { scannedCookie ->
                    val resultIntent = Intent().apply {
                        putExtra("cookie", scannedCookie)
                        Log.i("LoginActivity", scannedCookie)
                    }
                    setResult(RESULT_OK, resultIntent)
                    finish()
                },
            )
        }
    }

}
