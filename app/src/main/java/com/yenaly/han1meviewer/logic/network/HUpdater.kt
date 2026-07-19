package com.yenaly.han1meviewer.logic.network

import android.util.Log
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.logic.model.github.Latest
import com.yenaly.han1meviewer.util.checkNeedUpdate
import com.yenaly.han1meviewer.util.copyTo
import okio.use
import java.io.File
import java.util.zip.ZipInputStream

/**
 * @project Han1meViewer
 * @author Yenaly Liew
 * @time 2024/03/21 021 08:28
 */
object HUpdater {

    const val TAG = "HUpdater"

    suspend fun checkForUpdate(forceCheck: Boolean = false): Latest? {
        if (forceCheck || Preferences.isUpdateDialogVisible) {
            val ver = HanimeNetwork.githubService.getLatestVersion()
            if (checkNeedUpdate(ver.tagName)) {
                return Latest(
                    ver.tagName,
                    ver.body,
                    ver.assets.first().browserDownloadURL,
                    ver.assets.first().nodeID
                )
            }
        }
        return null
    }

    suspend fun File.injectUpdate(
        url: String,
        progress: (suspend (Int, Long, Long) -> Unit)? = null,
    ) {
        val res = HanimeNetwork.githubService.request(url)
        if (url.endsWith("zip")) {
            Log.d(TAG, "Injecting update from zip ($url)")
            res.body()?.use { body ->
                body.byteStream().use { stream ->
                    ZipInputStream(stream).use { zip ->
                        zip.nextEntry
                        this.outputStream().use {
                            Log.i(TAG, "content length: ${body.contentLength()}")
                            zip.copyTo(
                                it,
                                (body.contentLength() * 1.79).toLong(),
                                progress = progress,
                            )
                        }
                    }
                }
            }
        } else {
            Log.d(TAG, "Injecting update from release ($url)")
            this.outputStream().use {
                res.body()?.use { body ->
                    Log.i(TAG, "content length: ${body.contentLength()}")
                    body.byteStream().copyTo(it, body.contentLength(), progress = progress)
                }
            }
        }
    }
}
