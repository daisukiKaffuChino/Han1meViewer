package io.github.daisukikaffuchino.han1meviewer.ui.navigation.main

import androidx.compose.runtime.Composable
import io.github.daisukikaffuchino.han1meviewer.ui.activity.MainActivity
import io.github.daisukikaffuchino.han1meviewer.ui.screen.home.DailyCheckInScreen

@Composable
fun DailyCheckInRouteScreen(
    activity: MainActivity,
    onBack: () -> Unit,
) {
    DailyCheckInScreen(
        activity = activity,
        onBack = onBack,
    )
}
