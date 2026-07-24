package io.github.daisukikaffuchino.han1meviewer.ui.activity

import android.content.Intent
import android.os.Bundle
import io.github.daisukikaffuchino.utils.LogUtil
import io.github.daisukikaffuchino.han1meviewer.ui.screen.login.ManualInputCookiesScreen

class ManualInputCookiesActivity : BaseActivity() {
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        setHanimeContent {
            ManualInputCookiesScreen(
                onBack = { finish() },
                onCookieScanned = { scannedCookie ->
                    val resultIntent = Intent().apply {
                        putExtra("cookie", scannedCookie)
                        LogUtil.i("LoginActivity", scannedCookie)
                    }
                    setResult(RESULT_OK, resultIntent)
                    finish()
                },
            )
        }
    }

}
