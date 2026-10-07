package io.github.daisukikaffuchino.han1meviewer.logic.model

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * DataStore 迁移护栏：旧版 slide_sensitivity（1..9）必须稳定映射到新语义。
 *
 * 这是纯函数，迁移一旦写错会导致老用户手势灵敏度错乱，因此单独锁死。
 */
class SlideSensitivityMigrationTest {

    private val defaultValue = AppSettings().slideSensitivity

    @Test
    fun `legacy values are remapped`() {
        assertEquals(6, normalizeLegacySlideSensitivity(1))
        assertEquals(6, normalizeLegacySlideSensitivity(2))
        assertEquals(5, normalizeLegacySlideSensitivity(3))
        assertEquals(5, normalizeLegacySlideSensitivity(4))
        assertEquals(4, normalizeLegacySlideSensitivity(5))
        assertEquals(3, normalizeLegacySlideSensitivity(6))
        assertEquals(2, normalizeLegacySlideSensitivity(7))
        assertEquals(1, normalizeLegacySlideSensitivity(8))
        assertEquals(1, normalizeLegacySlideSensitivity(9))
    }

    @Test
    fun `missing or out-of-range values fall back to default`() {
        assertEquals(defaultValue, normalizeLegacySlideSensitivity(null))
        assertEquals(defaultValue, normalizeLegacySlideSensitivity(0))
        assertEquals(defaultValue, normalizeLegacySlideSensitivity(10))
    }
}
