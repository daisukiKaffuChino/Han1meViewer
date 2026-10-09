package io.github.daisukikaffuchino.han1meviewer.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import io.github.daisukikaffuchino.han1meviewer.util.toUiMessage

/**
 * 用户可见的错误文案（Compose 版本）。
 *
 * 具体规则见 [io.github.daisukikaffuchino.han1meviewer.util.toUiMessage]。
 */
@Composable
fun Throwable.toUiMessage(): String = toUiMessage(LocalContext.current)
