package io.github.daisukikaffuchino.han1meviewer.util

import android.content.Context
import io.github.daisukikaffuchino.utils.LogUtil
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object AnimeShaders {
    const val SHADERS_DIRECTORY = "shaders"

    val mpvSuperResolutionArray = arrayOf(
        "Anime4K_Clamp_Highlights.glsl",
        "Anime4K_Restore_CNN_VL.glsl",
        "Anime4K_Upscale_CNN_x2_VL.glsl",
        "Anime4K_AutoDownscalePre_x2.glsl",
        "Anime4K_AutoDownscalePre_x4.glsl",
        "Anime4K_Upscale_CNN_x2_M.glsl",
    )

    val mpvSuperResolutionLiteArray = arrayOf(
        "Anime4K_Clamp_Highlights.glsl",
        "Anime4K_Restore_CNN_M.glsl",
        "Anime4K_Restore_CNN_S.glsl",
        "Anime4K_Upscale_CNN_x2_M.glsl",
        "Anime4K_AutoDownscalePre_x2.glsl",
        "Anime4K_AutoDownscalePre_x4.glsl",
        "Anime4K_Upscale_CNN_x2_S.glsl",
    )

    fun copyShaderAssets(context: Context): Int {
        return try {
            val targetDir = File(context.filesDir, SHADERS_DIRECTORY)
            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }

            val assetManager = context.assets
            val assetFiles = assetManager.list(SHADERS_DIRECTORY) ?: return 0

            var copiedCount = 0
            for (filename in assetFiles) {
                val targetFile = File(targetDir, filename)
                assetManager.open("$SHADERS_DIRECTORY/$filename").use { inputStream ->
                    FileOutputStream(targetFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                        copiedCount++
                    }
                }
            }
            copiedCount
        } catch (e: IOException) {
            LogUtil.e("AnimeShaders", "复制 shader 资源失败", e)
            -1
        }
    }

    fun getShader(context: Context, type: Int): String {
        val shadersDir = File(context.filesDir, SHADERS_DIRECTORY)
        if (!shadersDir.exists()) {
            throw IllegalStateException("Shader folder not found: $shadersDir")
        }

        val shaderFiles = when (type) {
            0 -> emptyArray()
            1 -> mpvSuperResolutionLiteArray
            2 -> mpvSuperResolutionArray
            else -> throw IllegalArgumentException("Unknown shader type: $type")
        }

        return shaderFiles.joinToString(separator = ":") { shaderFile ->
            File(shadersDir, shaderFile).absolutePath
        }
    }

    fun copyCertAssets(context: Context): Int {
        return try {
            val assetName = "cacert.pem"
            val outputFile = File(context.filesDir, assetName)
            if (!outputFile.exists()) {
                val assetManager = context.assets
                assetManager.open(assetName).use { inputStream ->
                    FileOutputStream(outputFile).use { outputStream ->
                        val buffer = ByteArray(1024)
                        var read: Int
                        while (inputStream.read(buffer).also { read = it } != -1) {
                            outputStream.write(buffer, 0, read)
                        }
                        outputStream.flush()
                    }
                }
            }
            1
        } catch (e: IOException) {
            LogUtil.e("AnimeShaders", "复制证书资源失败", e)
            -1
        }
    }

    fun getCert(context: Context): String {
        val certFile = File(context.filesDir, "cacert.pem")
        if (!certFile.exists()) {
            throw IllegalStateException("Root certificate file not found: $certFile")
        }
        return certFile.absolutePath
    }
}
