package io.github.daisukikaffuchino.han1meviewer.logic.state

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 下载状态机护栏。
 *
 * [DownloadState] 的 mask 是持久化到 Room 的整数值，任何改动都必须保持向后兼容。
 */
class DownloadStateTest {

    @Test
    fun `mask values are stable`() {
        assertEquals(0, DownloadState.Unknown.mask)
        assertEquals(1, DownloadState.Queued.mask)
        assertEquals(2, DownloadState.Downloading.mask)
        assertEquals(3, DownloadState.Paused.mask)
        assertEquals(4, DownloadState.Finished.mask)
        assertEquals(5, DownloadState.Failed.mask)
    }

    @Test
    fun `from maps every mask back to its enum`() {
        DownloadState.entries.forEach { state ->
            assertEquals(state, DownloadState.from(state.mask))
        }
    }

    @Test
    fun `unknown mask falls back to Unknown`() {
        assertEquals(DownloadState.Unknown, DownloadState.from(999))
        assertEquals(DownloadState.Unknown, DownloadState.from(-1))
    }
}
