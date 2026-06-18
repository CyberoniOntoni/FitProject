package com.fitproject.droid.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader
import coil.compose.AsyncImage
import com.fitproject.droid.data.FPWorkoutExercise
import com.fitproject.droid.data.FirebaseConfig
import com.fitproject.droid.ui.theme.BWSColors
import java.io.File
import java.net.URLDecoder

private const val YOUTUBE_HOST = FirebaseConfig.PackageName
private const val YOUTUBE_REFERER = "https://$YOUTUBE_HOST/"
private const val PLAYER_FILE = "player.html"
private const val WEBVIEW_DIR = "youtube-webview"

object YouTubeIds {
    private val urlPattern = Regex(
        """(?:youtube\.com/(?:watch\?v=|embed/|shorts/)|youtu\.be/|youtube-nocookie\.com/embed/)([A-Za-z0-9_-]{11})"""
    )

    fun normalize(raw: String?): String? {
        val value = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        urlPattern.find(value)?.groupValues?.getOrNull(1)?.let { return it }
        return value
    }
}

object YouTubeEmbedHtml {
    fun build(youtubeId: String, autoplay: Boolean = true): String {
        val autoplayParam = if (autoplay) "1" else "0"
        val origin = Uri.encode("https://$YOUTUBE_HOST")
        return """
            <!DOCTYPE html>
            <html>
            <head>
            <meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <meta name="referrer" content="strict-origin-when-cross-origin">
            <style>
            html,body{margin:0;padding:0;width:100%;height:100%;background:#000;overflow:hidden}
            iframe{position:absolute;inset:0;width:100%;height:100%;border:0;background:#000}
            </style>
            </head>
            <body>
            <iframe
              src="https://www.youtube-nocookie.com/embed/$youtubeId?autoplay=$autoplayParam&rel=0&modestbranding=1&playsinline=1&enablejsapi=1&origin=$origin"
              title="Exercise video"
              allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
              referrerpolicy="strict-origin"
              allowfullscreen>
            </iframe>
            </body>
            </html>
        """.trimIndent()
    }
}

private class DirectoryPathHandler(
    private val directory: File
) : WebViewAssetLoader.PathHandler {
    override fun handle(path: String): WebResourceResponse? {
        val safePath = URLDecoder.decode(path, Charsets.UTF_8.name()).removePrefix("/")
        val target = File(directory, safePath)
        if (!target.canonicalPath.startsWith(directory.canonicalPath)) return null
        if (!target.exists() || !target.isFile) return null
        val mime = when (target.extension.lowercase()) {
            "html" -> "text/html"
            "js" -> "application/javascript"
            "css" -> "text/css"
            else -> "application/octet-stream"
        }
        return WebResourceResponse(mime, "UTF-8", target.inputStream())
    }
}

private object YouTubeWebViewHost {
    fun webFolder(context: Context): File =
        File(context.filesDir, WEBVIEW_DIR).apply { mkdirs() }

    fun writePlayerHtml(context: Context, youtubeId: String) {
        val file = File(webFolder(context), PLAYER_FILE)
        file.writeText(YouTubeEmbedHtml.build(youtubeId))
    }

    fun playerUrl(): String = "https://$YOUTUBE_HOST/$PLAYER_FILE"

    fun createAssetLoader(context: Context): WebViewAssetLoader =
        WebViewAssetLoader.Builder()
            .setDomain(YOUTUBE_HOST)
            .addPathHandler("/", DirectoryPathHandler(webFolder(context)))
            .build()
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubeEmbedView(
    youtubeId: String,
    modifier: Modifier = Modifier
) {
    val normalizedId = remember(youtubeId) { YouTubeIds.normalize(youtubeId) ?: youtubeId }
    var loadedId by remember { mutableStateOf<String?>(null) }

    AndroidView(
        factory = { context ->
            val assetLoader = YouTubeWebViewHost.createAssetLoader(context)
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                webChromeClient = WebChromeClient()
                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView,
                        request: WebResourceRequest
                    ): WebResourceResponse? =
                        assetLoader.shouldInterceptRequest(request.url)

                    override fun onPageFinished(view: WebView?, url: String?) {
                        view?.setBackgroundColor(android.graphics.Color.BLACK)
                    }
                }
                setBackgroundColor(android.graphics.Color.BLACK)
                overScrollMode = View.OVER_SCROLL_NEVER
                isHorizontalScrollBarEnabled = false
                isVerticalScrollBarEnabled = false
            }
        },
        update = { webView ->
            if (loadedId != normalizedId) {
                loadedId = normalizedId
                YouTubeWebViewHost.writePlayerHtml(webView.context, normalizedId)
                webView.setBackgroundColor(android.graphics.Color.BLACK)
                webView.loadUrl(YouTubeWebViewHost.playerUrl())
            }
        },
        modifier = modifier.background(Color.Black)
    )
}

@Composable
fun ExerciseVideoPreview(
    exercise: FPWorkoutExercise,
    modifier: Modifier = Modifier
) {
    val youtubeId = remember(exercise.id, exercise.youtubeId) {
        YouTubeIds.normalize(exercise.youtubeId)
    }
    val thumbnailUrl = exercise.videoThumbnailUrl
    if (youtubeId == null && thumbnailUrl == null) return

    var isPlaying by remember(exercise.id) { mutableStateOf(false) }
    var isLoading by remember(exercise.id) { mutableStateOf(false) }
    val shape = RoundedCornerShape(BWSColors.CardRadius.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .then(if (isPlaying) Modifier else Modifier.clip(shape))
            .background(BWSColors.SurfaceHighlight, shape)
    ) {
        when {
            isPlaying && youtubeId != null -> {
                YouTubeEmbedView(
                    youtubeId = youtubeId,
                    modifier = Modifier.fillMaxSize()
                )
            }
            thumbnailUrl != null -> {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = "Exercise video preview",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f))
                        .clickable(enabled = youtubeId != null) {
                            if (youtubeId != null) {
                                isLoading = true
                                isPlaying = true
                                isLoading = false
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = BWSColors.Accent)
                    } else if (youtubeId != null) {
                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = "Play video",
                            tint = Color.White.copy(alpha = 0.92f),
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }
            }
        }
    }
}