package io.github.daisukikaffuchino.han1meviewer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * 分享链接 / deep link 解析护栏。
 *
 * [toVideoCode] 用正则从任意文本里提取 v 参数，是分享、剪贴板、deep link 的公共入口，
 * 正则一旦改动影响面很大。
 */
class VideoCodeTest {

    @Test
    fun `extracts code from known mirror hosts`() {
        assertEquals("123456", "https://hanime1.me/watch?v=123456".toVideoCode())
        assertEquals("987", "https://hanime1.com/watch?v=987".toVideoCode())
        assertEquals("1", "https://hanimeone.me/watch?v=1".toVideoCode())
        assertEquals("55", "https://javchu.com/watch?v=55".toVideoCode())
    }

    @Test
    fun `extracts code from extra query params and scheme-less urls`() {
        assertEquals("42", "https://hanime1.me/watch?foo=bar&v=42".toVideoCode())
        assertEquals("42", "//hanime1.me/watch?v=42".toVideoCode())
        assertEquals("42", "hanime1.me/watch?v=42".toVideoCode())
    }

    @Test
    fun `returns null when no video code present`() {
        assertNull("".toVideoCode())
        assertNull("https://hanime1.me/".toVideoCode())
        assertNull("https://hanime1.me/watch?v=abc".toVideoCode())
    }
}
