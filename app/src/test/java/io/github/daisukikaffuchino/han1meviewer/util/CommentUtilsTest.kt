package io.github.daisukikaffuchino.han1meviewer.util

import kotlin.test.Test
import kotlin.test.assertEquals

class CommentUtilsTest {

    @Test
    fun `parses relative time strings to minutes`() {
        assertEquals(45, parseTimeStrToMinutes("45分鐘前"))
        assertEquals(120, parseTimeStrToMinutes("2小時前"))
        assertEquals(60 * 24 * 5, parseTimeStrToMinutes("5天前"))
        assertEquals(60 * 24 * 7, parseTimeStrToMinutes("1週前"))
        assertEquals(60 * 24 * 30 * 2, parseTimeStrToMinutes("2個月前"))
        assertEquals(60 * 24 * 365 * 3, parseTimeStrToMinutes("3年前"))
    }

    @Test
    fun `unknown time string falls back to zero`() {
        assertEquals(0, parseTimeStrToMinutes("剛剛"))
        assertEquals(0, parseTimeStrToMinutes(""))
    }

    @Test
    fun `safeSortedBy sorts ascending and descending`() {
        val unsorted = listOf(3, 1, 2)
        val ascending = unsorted.safeSortedBy(selector = { value: Int -> value })
        val descending = unsorted.safeSortedBy(selector = { value: Int -> value }, descending = true)
        assertEquals(listOf(1, 2, 3), ascending)
        assertEquals(listOf(3, 2, 1), descending)
    }
}
