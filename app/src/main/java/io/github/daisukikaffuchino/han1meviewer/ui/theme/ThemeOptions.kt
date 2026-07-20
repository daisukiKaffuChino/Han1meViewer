package io.github.daisukikaffuchino.han1meviewer.ui.theme

import androidx.compose.ui.graphics.Color

enum class ThemeAccentColor(
    val id: Int,
    val label: String,
    val colors: List<Color>,
) {
    Pink(0, "Momoi", listOf(Color(0xFFF596AA), Color(0xFFFFB1BF), Color(0xFFE46988), Color(0xFFEDBE92))),
    Green(1, "Midori", listOf(Color(0xFF8BC34A), Color(0xFF9FD75C), Color(0xFF6A9F2B), Color(0xFF9FD0CC))),
    Yellow(2, "Yuzu", listOf(Color(0xFFFFF59D), Color(0xFFD7CA2C), Color(0xFF9E9401), Color(0xFFA6D0BA))),
    Blue(3, "Arisu", listOf(Color(0xFF03A9F4), Color(0xFF8ECDFF), Color(0xFF0099DD), Color(0xFFCFC0E7)));

    companion object {
        fun fromId(id: Int): ThemeAccentColor = entries.find { it.id == id } ?: Pink
    }
}

enum class AppPaletteStyle(
    val id: Int,
    val label: String,
) {
    TonalSpot(1, "Tonal Spot"),
    Neutral(2, "Neutral"),
    Vibrant(3, "Vibrant"),
    Expressive(4, "Expressive"),
    Rainbow(5, "Rainbow"),
    FruitSalad(6, "Fruit Salad"),
    Fidelity(7, "Fidelity"),
    Content(8, "Content");

    companion object {
        fun fromId(id: Int): AppPaletteStyle = entries.find { it.id == id } ?: TonalSpot
    }
}
