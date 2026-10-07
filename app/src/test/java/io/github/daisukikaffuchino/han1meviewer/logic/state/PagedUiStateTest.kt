package io.github.daisukikaffuchino.han1meviewer.logic.state

import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PagedUiStateTest {

    @Test
    fun `dataOrNull exposes payload for success, no-more-data and cached error`() {
        assertEquals("a", PagedUiState.Success("a").dataOrNull)
        assertEquals("b", PagedUiState.NoMoreData("b").dataOrNull)
        assertEquals("c", PagedUiState.Error(IOException("x"), cached = "c").dataOrNull)
        assertNull(PagedUiState.Loading.dataOrNull)
        assertNull(PagedUiState.Empty.dataOrNull)
        assertNull(PagedUiState.NoMoreData<String>().dataOrNull)
        assertNull(PagedUiState.Error<String>(IOException("x")).dataOrNull)
    }

    @Test
    fun `first page helpers only fire for the first load`() {
        assertTrue(PagedUiState.Loading.isFirstPageLoading)
        assertFalse(PagedUiState.Success("a").isFirstPageLoading)

        assertTrue(PagedUiState.Empty.isFirstPageEmpty)
        assertFalse(PagedUiState.NoMoreData<String>().isFirstPageEmpty)

        assertTrue(PagedUiState.Error<String>(IOException("x")).isFirstPageError)
        assertFalse(PagedUiState.Error(IOException("x"), cached = "c").isFirstPageError)
        assertFalse(PagedUiState.Success("a").isFirstPageError)
    }

    @Test
    fun `error exposes structured kind`() {
        val state = PagedUiState.Error<String>(java.net.SocketTimeoutException("x"))
        assertEquals(HanimeErrorKind.Timeout, state.error.kind)
        assertTrue(state.error.isRetryable)
    }

    @Test
    fun `refreshing success keeps previous data`() {
        val state = PagedUiState.Success("a", isRefreshing = true)
        assertTrue(state.isRefreshing)
        assertEquals("a", state.dataOrNull)
    }
}
