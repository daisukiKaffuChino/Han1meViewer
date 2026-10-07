package io.github.daisukikaffuchino.han1meviewer

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format.char
import io.github.daisukikaffuchino.han1meviewer.logic.SettingsRepository

/**
 * 我觉得空字符串写出来太逆天了，所以搞了个常量
 */
const val EMPTY_STRING = ""

const val APP_NAME = "Han1meViewer"

// 标准时间格式

/* yyyy-MM-dd */
@JvmField
val LOCAL_DATE_FORMAT = LocalDate.Formats.ISO

/* yyyy-MM-dd HH:mm */
@JvmField
val LOCAL_DATE_TIME_FORMAT = LocalDateTime.Format {
    date(LocalDate.Formats.ISO); char(' ')
    hour(); char(':'); minute()
}

// 网络基本设置

const val USER_AGENT =
    "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Mobile Safari/537.36"
const val DESKTOP_USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36"

// 設置發佈日期年份，在搜索的tag裏

/**
 * 發佈日期年份開始於
 */
const val SEARCH_YEAR_RANGE_START = 1990

/**
 * 發佈日期年份結束於
 */
const val SEARCH_YEAR_RANGE_END = BuildConfig.SEARCH_YEAR_RANGE_END

const val VIDEO_COMMENT_PREFIX = "video"

const val PREVIEW_COMMENT_PREFIX = "preview"

// base url

val HANIME_BASE_URL: String
    get() = SettingsRepository.baseUrl

/**
 * 如果添加备选网址别忘了确认[String.toVideoCode]的videoUrlRegex
 */
object HanimeConstants {
    val HANIME_HOSTNAME = arrayOf("hanime1.me","hanime1.com","hanimeone.me","javchu.com")
    val HANIME_URL = arrayOf("https://hanime1.me/","https://hanime1.com/","https://hanimeone.me/","https://javchu.com/")
    val ANIME_URL = arrayOf("https://hanime1.me/","https://hanime1.com/","https://hanimeone.me/")
}

val HANIME_LOGIN_URL: String
    get() = HANIME_BASE_URL + "login"

// 媒体 CDN

const val HANIME_MEDIA_CDN_HOST = "vdownload.hembed.com"
const val HANIME_BACKUP_MEDIA_CDN_HOST = "1497203185.rsc.cdn77.org"

fun String.replaceBackupMediaCdnHost(): String {
    if (!SettingsRepository.useBackupMediaCdn) return this
    return replace(
        HANIME_MEDIA_CDN_HOST,
        HANIME_BACKUP_MEDIA_CDN_HOST,
        ignoreCase = true,
    )
}

// github url

const val HA1_GITHUB_URL = "https://github.com/daisukiKaffuChino/Han1meViewer"

const val HA1_GITHUB_ISSUE_URL = "$HA1_GITHUB_URL/issues"

const val HA1_GITHUB_FORUM_URL = "$HA1_GITHUB_URL/discussions"

// Notification

const val DOWNLOAD_NOTIFICATION_CHANNEL = "download_channel"

const val UPDATE_NOTIFICATION_CHANNEL = "update_channel"

// File

const val FILE_PROVIDER_AUTHORITY = "${BuildConfig.APPLICATION_ID}.fileProvider"
const val GETCHU_BASE_URL = "https://www.getchu.com/"
