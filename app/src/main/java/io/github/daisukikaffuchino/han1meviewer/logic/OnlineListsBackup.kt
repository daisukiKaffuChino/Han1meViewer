package io.github.daisukikaffuchino.han1meviewer.logic

import io.github.daisukikaffuchino.han1meviewer.EMPTY_STRING
import io.github.daisukikaffuchino.han1meviewer.logic.model.HanimeInfo
import io.github.daisukikaffuchino.han1meviewer.logic.model.ListItemExport
import io.github.daisukikaffuchino.han1meviewer.logic.model.ListsExport
import io.github.daisukikaffuchino.han1meviewer.logic.model.MyListType
import io.github.daisukikaffuchino.han1meviewer.logic.model.PlaylistExport
import io.github.daisukikaffuchino.han1meviewer.logic.model.Playlists
import io.github.daisukikaffuchino.han1meviewer.logic.state.PageLoadingState
import io.github.daisukikaffuchino.han1meviewer.logic.state.WebsiteState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

/**
 * 在线稍后再看 / 我喜欢的影片 / 播放清单的导入导出。
 * 所有操作都需要已登录状态。
 */
object OnlineListsBackup {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    suspend fun exportOnlineLists(): ListsExport {
        val userId = requireLoggedInUserId()
        val watchLater = fetchAllMyListItems(userId, MyListType.WATCH_LATER)
        val favorites = fetchAllMyListItems(userId, MyListType.FAV_VIDEO)
        val playlists = fetchAllPlaylists(userId).map { playlist ->
            val (items, desc) = fetchPlaylistData(playlist.listCode)
            PlaylistExport(
                title = playlist.title,
                desc = desc,
                items = items,
            )
        }
        return ListsExport(
            watchLater = watchLater,
            favorites = favorites,
            playlists = playlists,
        )
    }

    suspend fun exportOnlineListsJson(): String =
        json.encodeToString(exportOnlineLists())

    suspend fun importOnlineLists(data: ListsExport) {
        val userId = requireLoggedInUserId()
        val csrfToken = fetchCsrfToken(userId)

        data.watchLater.forEachIndexed { index, item ->
            awaitWebsiteSuccess(
                NetworkRepo.addToMyList(
                    listCode = "save",
                    videoCode = item.videoCode,
                    isChecked = true,
                    position = index,
                    csrfToken = csrfToken,
                )
            )
        }
        data.favorites.forEach { item ->
            awaitWebsiteSuccess(
                NetworkRepo.addToMyFavVideo(
                    videoCode = item.videoCode,
                    likeStatus = false,
                    currentUserId = userId,
                    token = csrfToken,
                )
            )
        }

        val existingPlaylists = fetchAllPlaylists(userId).associate { it.title to it.listCode }
        val createdCodes = mutableMapOf<String, String>()
        data.playlists.forEach { playlist ->
            val listCode = createdCodes[playlist.title]
                ?: existingPlaylists[playlist.title]
                ?: run {
                    awaitWebsiteSuccess(
                        NetworkRepo.createPlaylist(
                            EMPTY_STRING,
                            playlist.title,
                            playlist.desc,
                            csrfToken,
                        )
                    )
                    val fresh = fetchAllPlaylists(userId)
                    val code = fresh.firstOrNull { it.title == playlist.title }?.listCode
                        ?: error("Failed to find created playlist: ${playlist.title}")
                    createdCodes[playlist.title] = code
                    code
                }
            playlist.items.forEachIndexed { index, item ->
                awaitWebsiteSuccess(
                    NetworkRepo.addToMyList(
                        listCode = listCode,
                        videoCode = item.videoCode,
                        isChecked = true,
                        position = index,
                        csrfToken = csrfToken,
                    )
                )
            }
        }
    }

    suspend fun importOnlineListsJson(jsonText: String) =
        importOnlineLists(json.decodeFromString<ListsExport>(jsonText))

    private fun requireLoggedInUserId(): String {
        if (!SettingsRepository.isAlreadyLogin || SettingsRepository.savedUserId.isBlank()) {
            error("Not logged in")
        }
        return SettingsRepository.savedUserId
    }

    private suspend fun fetchAllMyListItems(
        userId: String,
        type: MyListType,
    ): List<ListItemExport> {
        val result = mutableListOf<ListItemExport>()
        var page = 1
        while (true) {
            val state = NetworkRepo.getMyListItems(userId, type, page)
                .first { it !is PageLoadingState.Loading }
            when (state) {
                is PageLoadingState.Success -> {
                    if (state.info.hanimeInfo.isEmpty()) return result
                    result += state.info.hanimeInfo.map { it.toListItemExport() }
                    page++
                }

                is PageLoadingState.Error -> throw state.throwable
                is PageLoadingState.NoMoreData -> return result
                is PageLoadingState.Loading -> return result
            }
        }
    }

    private suspend fun fetchAllPlaylists(userId: String): List<Playlists.Playlist> {
        val result = mutableListOf<Playlists.Playlist>()
        var page = 1
        while (true) {
            val state = NetworkRepo.getPlaylists(page, userId)
                .first { it !is WebsiteState.Loading }
            when (state) {
                is WebsiteState.Success -> {
                    if (state.info.playlists.isEmpty()) return result
                    result += state.info.playlists
                    page++
                }

                is WebsiteState.Error -> throw state.throwable
                is WebsiteState.Loading -> return result
            }
        }
    }

    private suspend fun fetchPlaylistData(
        listCode: String,
    ): Pair<List<ListItemExport>, String> {
        val result = mutableListOf<ListItemExport>()
        var desc = ""
        var page = 1
        while (true) {
            val state = NetworkRepo.getMyPlayListItems(page, listCode)
                .first { it !is PageLoadingState.Loading }
            when (state) {
                is PageLoadingState.Success -> {
                    if (page == 1) desc = state.info.desc.orEmpty()
                    if (state.info.hanimeInfo.isEmpty()) return result to desc
                    result += state.info.hanimeInfo.map { it.toListItemExport() }
                    page++
                }

                is PageLoadingState.Error -> throw state.throwable
                is PageLoadingState.NoMoreData -> return result to desc
                is PageLoadingState.Loading -> return result to desc
            }
        }
    }

    private suspend fun fetchCsrfToken(userId: String): String {
        val state = NetworkRepo.getPlaylists(1, userId)
            .first { it !is WebsiteState.Loading }
        return when (state) {
            is WebsiteState.Success -> state.info.csrfToken ?: error("Missing CSRF token")
            is WebsiteState.Error -> throw state.throwable
            is WebsiteState.Loading -> error("Missing CSRF token")
        }
    }

    private suspend fun <T> awaitWebsiteSuccess(flow: Flow<WebsiteState<T>>): T {
        val state = flow.first { it !is WebsiteState.Loading }
        return when (state) {
            is WebsiteState.Success -> state.info
            is WebsiteState.Error -> throw state.throwable
            is WebsiteState.Loading -> error("Unexpected loading state")
        }
    }

    private fun HanimeInfo.toListItemExport(): ListItemExport =
        ListItemExport(
            videoCode = videoCode,
            title = title,
            coverUrl = coverUrl,
            duration = duration,
            views = views,
            uploadTime = uploadTime,
            genre = genre,
            reviews = reviews,
            currentArtist = currentArtist,
            itemType = itemType,
            addedAt = 0,
        )
}
