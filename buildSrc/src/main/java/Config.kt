@file:Suppress("UnstableApiUsage")

import org.gradle.api.Project
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * @author Yenaly Liew
 * @time 2023/11/25 025 17:55
 */
object Config {

    val Project.isRelease: Boolean
        get() = gradle.startParameter.taskNames.any { it.contains("Release") }

    object Version {

        const val DEBUG = "debug"
        const val RELEASE = "release"

        fun Project.createVersion(
            major: Int, minor: Int, patch: Int
        ): Pair<Int, String> {
            val source = this.source
            val versionCode: Int
            val versionName: String
            when (source) {
                DEBUG -> {
                    versionCode = 1
                    versionName = "$DEBUG+$versionCode"
                }

                else -> {
                    versionCode = LocalDateTime.now(Clock.systemUTC()).format(
                        DateTimeFormatter.ofPattern("yyMMddHH")
                    ).toInt()
                    versionName = "${major}.${minor}.${patch}-$source+$versionCode"
                }
            }
            return versionCode to versionName
        }

        val Project.source: String
            get() = if (isRelease) RELEASE else DEBUG
    }

    val thisYear: Int
        get() = LocalDateTime.now(Clock.system(ZoneId.of("UTC+8"))).year
}
