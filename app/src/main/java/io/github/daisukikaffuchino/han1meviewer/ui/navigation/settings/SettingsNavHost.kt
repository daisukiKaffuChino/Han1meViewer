package io.github.daisukikaffuchino.han1meviewer.ui.navigation.settings

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.daisukikaffuchino.han1meviewer.ui.component.appbar.HanimeTopAppBar
import io.github.daisukikaffuchino.han1meviewer.ui.component.appbar.HanimeScaffold
import io.github.daisukikaffuchino.han1meviewer.ui.navigation.main.HanimeScreen
import io.github.daisukikaffuchino.han1meviewer.ui.navigation.main.TopLevelBackStack

@Composable
fun SettingsScaffold(
    backStack: TopLevelBackStack<HanimeScreen>,
    destination: SettingsDestinationSpec,
    fallbackDestination: HanimeScreen,
    onNavigateBack: (() -> Boolean)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    fun navigateBack() {
        if (onNavigateBack?.invoke() == true) return
        if (!backStack.removeLast()) {
            backStack.add(fallbackDestination, launchSingleTop = true)
        }
    }

    HanimeScaffold(
        topBar = {
            if (destination.showToolbar) {
                HanimeTopAppBar(
                    title = stringResource(destination.titleRes),
                    onBack = ::navigateBack,
                    actions = actions,
                )
            }
        },
        floatingActionButton = floatingActionButton,
    ) {
        content()
    }
}
