package com.xiaojiaoyin.jiaming.ui

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.core.content.FileProvider
import java.io.File

/** 证据面板/纪念卡 → PNG 分享（Compose GraphicsLayer 截图 + FileProvider） */
object ShareUtil {

    suspend fun exportPng(context: Context, layer: GraphicsLayer, fileName: String): File {
        val bitmap = layer.toImageBitmap().asAndroidBitmap()
        val dir = File(context.cacheDir, "share").apply { mkdirs() }
        val file = File(dir, fileName)
        file.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        return file
    }

    fun sharePng(context: Context, file: File, title: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, title))
    }
}

/** Composable 内使用的记忆化 GraphicsLayer 工厂（避免调用方直接依赖 compose graphics 细节） */
@androidx.compose.runtime.Composable
fun rememberShareLayer(): GraphicsLayer = rememberGraphicsLayer()
