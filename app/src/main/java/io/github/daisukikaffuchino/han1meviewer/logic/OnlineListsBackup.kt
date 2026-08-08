package io.github.daisukikaffuchino.han1meviewer.logic

import io.github.daisukikaffuchino.han1meviewer.EMPTY_STRING
import io.github.daisukikaffuchino.han1meviewer.logic.model.HanimeInfo
import io.github.daisukikaffuchino.han1meviewer.logic.model.ListItemExport
import io.github.daisukikaffuchino.han1meviewer.logic.model.ListsExport
import io.github.daisukikaffuchino.han1meviewer.logic.model.MyListType
import io.github.daisukikaffuchino.han1meviewer.logic.model.PlaylistExport
import io.github.daisukikaffuchino.han1meviewer.logic.model.Playlists
import io.github.daisukikaffuchino.han1meviewer.logic.state.PageLoadingState
import io.github.daisukikaffuchino.han1meviewer.logic.state.VideoLoadingState
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
        val probeVideoCode = data.watchLater.firstOrNull()?.videoCode
            ?: data.favorites.firstOrNull()?.videoCode
            ?: data.playlists.firstOrNull()?.items?.firstOrNull()?.videoCode
        val csrfToken = probeVideoCode?.let { fetchVideoCsrfToken(it) }
            ?: fetchCsrfToken(userId)

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
        val knownCodes = existingPlaylists.values.toMutableSet()
        val createdCodes = mutableMapOf<String, String>()
        data.playlists.forEach { playlist ->
            var listCode = createdCodes[playlist.title]
                ?: existingPlaylists[playlist.title]
            if (listCode == null) {
                awaitWebsiteSuccess(
                    NetworkRepo.createPlaylist(
                        EMPTY_STRING,
                        playlist.title,
                        playlist.desc,
                        csrfToken,
                    )
                )
                val fresh = fetchAllPlaylists(userId)
                listCode = fresh.firstOrNull { it.listCode !in knownCodes }?.listCode
                    ?: fresh.firstOrNull { it.title == playlist.title }?.listCode
                    ?: playlist.items.firstOrNull()?.videoCode?.let { probeCode ->
                        fetchPlaylistCodeFromVideoPage(probeCode, playlist.title)
                    }
                    ?: error("Failed to find created playlist: ${playlist.title}")
                knownCodes += listCode
                createdCodes[playlist.title] = listCode
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

    private suspend fun fetchVideoCsrfToken(videoCode: String): String? {
        val state = NetworkRepo.getHanimeVideo(videoCode)
            .first { it !is VideoLoadingState.Loading }
        return when (state) {
            is VideoLoadingState.Success -> state.info.csrfToken
            // 失败时返回 null，让调用处的 elvis 兜底 fetchCsrfToken(userId) 生效
            is VideoLoadingState.Error -> null
            is VideoLoadingState.Loading -> null
            is VideoLoadingState.NoContent -> null
        }
    }

    private suspend fun fetchPlaylistCodeFromVideoPage(
        videoCode: String,
        playlistTitle: String,
    ): String? {
        val state = NetworkRepo.getHanimeVideo(videoCode)
            .first { it !is VideoLoadingState.Loading }
        return when (state) {
            is VideoLoadingState.Success -> state.info.myList?.myListInfo
                ?.firstOrNull { it.title == playlistTitle }
                ?.code

            // 失败时返回 null，让调用处的 elvis 链继续走到明确的 error(...) 提示
            is VideoLoadingState.Error -> null
            is VideoLoadingState.Loading -> null
            is VideoLoadingState.NoContent -> null
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
            addedAt = 0,
        )
}
