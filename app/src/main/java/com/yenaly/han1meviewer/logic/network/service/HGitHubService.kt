package com.yenaly.han1meviewer.logic.network.service

import com.yenaly.han1meviewer.logic.model.github.Release
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Streaming
import retrofit2.http.Url

/**
 * @project Han1meViewer
 * @author Yenaly Liew
 * @time 2022/09/09 009 20:04
 */
interface HGitHubService {
    @GET("releases/latest")
    suspend fun getLatestVersion(): Release

    @GET
    @Streaming
    suspend fun request(
        @Url url: String,
    ): Response<ResponseBody>
}
