package io.github.daisukikaffuchino.han1meviewer.ui.screen.home.homepage

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.daisukikaffuchino.han1meviewer.logic.AppUpdateInfo
import io.github.daisukikaffuchino.han1meviewer.ui.component.lazy.LazyColumn
import io.github.daisukikaffuchino.han1meviewer.ui.preview.ComponentPreview
import io.github.daisukikaffuchino.han1meviewer.ui.preview.fakeAnnouncements
import io.github.daisukikaffuchino.han1meviewer.ui.preview.fakeHomePage
import io.github.daisukikaffuchino.han1meviewer.ui.screen.home.homepage.component.AnnouncementCard
import io.github.daisukikaffuchino.han1meviewer.ui.screen.home.homepage.component.AppUpdateCard
import io.github.daisukikaffuchino.han1meviewer.ui.screen.home.homepage.component.BannerCarousel
import io.github.daisukikaffuchino.han1meviewer.ui.screen.home.homepage.component.CategoryRow

/**
 * 渲染首页可滚动内容区域。
 *
 * @param data 主页数据
 * @param onEvent 主页事件回调
 * @param onCloseAnnouncement 关闭公告时调用。
 * @param modifier 应用于列表根布局的修饰符。
 */
@Composable
fun HomePageContent(
    data: HomeData,
    updateInfo: AppUpdateInfo?,
    onEvent: (HomeUiEvent) -> Unit,
    onCloseAnnouncement: () -> Unit,
    modifier: Modifier = Modifier
) {
    val banners = remember(data.page.banner) {
        listOfNotNull(data.page.banner)
    }
    val announcements = remember(data.announcements) {
        data.announcements.filter { it.isActive }
    }

    val categories = remember(data.page) {
        buildCategoryList(data.page)
    }
    LazyColumn(modifier = modifier.fillMaxSize()) {
        if (updateInfo != null) {
            item(key = "app_update_${updateInfo.versionCode}") {
                AppUpdateCard(
                    updateInfo = updateInfo,
                    onUpdateClick = {
                        onEvent(HomeUiEvent.OpenUpdatePage(updateInfo.downloadUrl))
                    },
                    onIgnoreClick = {
                        onEvent(HomeUiEvent.IgnoreUpdate(updateInfo.versionCode))
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
        item(key = "banner") {
            BannerCarousel(
                banners = banners,
                onBannerClick = { videoCode ->
                    videoCode?.let {
                        onEvent(HomeUiEvent.OpenVideo(it))
                    }
                },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
        if (announcements.isNotEmpty()) {
            item(key = "announcement") {
                AnnouncementCard(
                    announcements = announcements,
                    onAnnouncementClick = { announcement ->
                        onEvent(HomeUiEvent.ShowAnnouncementDialog(announcement))
                    },
                    onClose = onCloseAnnouncement,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }
        categories.forEach { category ->
            item(key = "category_${category.titleRes}") {
                CategoryRow(
                    title = stringResource(category.titleRes),
                    videos = category.videos,
                    onMoreClick = {
                        val params = category.toAdvancedSearchParams()
                        if (params.isNotEmpty()) {
                            onEvent(HomeUiEvent.NavigateToSearchAdvanced(params))
                        }
                    },
                    onVideoClick = { code ->
                        onEvent(HomeUiEvent.OpenVideo(code))
                    },
                    onVideoLongClick = { _, _ ->
                       // onEvent(HomeUiEvent.LongPressVideoCopy(code, title))
                    },
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "首页主内容")
@Composable
private fun HomePageContentPreview() {
    ComponentPreview {
        Surface(color = MaterialTheme.colorScheme.background) {
            HomePageContent(
                data = HomeData(
                    page = fakeHomePage,
                    announcements = fakeAnnouncements,
                ),
                updateInfo = AppUpdateInfo(
                    versionName = "26.1.0",
                    versionCode = 260720,
                    downloadUrl = "https://example.com",
                    updateDescription = "A new version is ready.",
                    forceUpdate = false,
                ),
                onEvent = {},
                onCloseAnnouncement = {},
            )
        }
    }
}
