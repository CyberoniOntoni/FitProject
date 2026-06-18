package com.fitproject.droid.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
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
import androidx.compose.runtime.DisposableEffect
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.fitproject.droid.data.FPWorkoutExercise
import com.fitproject.droid.data.FirebaseConfig
import com.fitproject.droid.ui.theme.BWSColors

private const val YOUTUBE_REFERER = "https://${FirebaseConfig.PackageName}"

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
        val origin = Uri.encode(YOUTUBE_REFERER)
        val widgetReferrer = Uri.encode(YOUTUBE_REFERER)
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
              src="https://www.youtube-nocookie.com/embed/$youtubeId?autoplay=$autoplayParam&rel=0&modestbranding=1&playsinline=1&enablejsapi=1&origin=$origin&widget_referrer=$widgetReferrer"
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

object YouTubeEmbedUrls {
    fun embedUrl(youtubeId: String, autoplay: Boolean = true): String {
        val autoplayParam = if (autoplay) "1" else "0"
        val origin = Uri.encode(YOUTUBE_REFERER)
        val widgetReferrer = Uri.encode(YOUTUBE_REFERER)
        return "https://www.youtube-nocookie.com/embed/$youtubeId" +
            "?autoplay=$autoplayParam&rel=0&modestbranding=1&playsinline=1&enablejsapi=1" +
            "&origin=$origin&widget_referrer=$widgetReferrer"
    }
}

private enum class YouTubeLoadMode {
    HtmlEmbed,
    DirectEmbed
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubeEmbedView(
    youtubeId: String,
    modifier: Modifier = Modifier
) {
    val normalizedId = remember(youtubeId) { YouTubeIds.normalize(youtubeId) ?: youtubeId }
    val html = remember(normalizedId) { YouTubeEmbedHtml.build(normalizedId) }
    val directUrl = remember(normalizedId) { YouTubeEmbedUrls.embedUrl(normalizedId) }
    var loadedId by remember { mutableStateOf<String?>(null) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    DisposableEffect(lifecycle, webViewRef) {
        val webView = webViewRef
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> webView?.onResume()
                Lifecycle.Event.ON_PAUSE -> webView?.onPause()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        webView?.onResume()
        onDispose {
            lifecycle.removeObserver(observer)
            webView?.onPause()
        }
    }

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                webChromeClient = WebChromeClient()
                setBackgroundColor(android.graphics.Color.BLACK)
                overScrollMode = View.OVER_SCROLL_NEVER
                isHorizontalScrollBarEnabled = false
                isVerticalScrollBarEnabled = false
            }.also { webViewRef = it }
        },
        update = { webView ->
            val fallbackUrl = directUrl
            webView.webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    view?.setBackgroundColor(android.graphics.Color.BLACK)
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    view?.setBackgroundColor(android.graphics.Color.BLACK)
                }

                override fun onReceivedError(
                    view: WebView,
                    request: WebResourceRequest,
                    error: WebResourceError
                ) {
                    if (!request.isForMainFrame) return
                    if (view.tag == YouTubeLoadMode.HtmlEmbed) return
                    view.tag = YouTubeLoadMode.HtmlEmbed
                    view.setLayerType(View.LAYER_TYPE_HARDWARE, null)
                    view.loadDataWithBaseURL(
                        YOUTUBE_REFERER,
                        html,
                        "text/html",
                        "UTF-8",
                        null
                    )
                }
            }

            if (loadedId != normalizedId) {
                loadedId = normalizedId
                webView.tag = YouTubeLoadMode.DirectEmbed
                webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                webView.setBackgroundColor(android.graphics.Color.BLACK)
                webView.loadUrl(
                    fallbackUrl,
                    mapOf("Referer" to YOUTUBE_REFERER)
                )
            }
        },
        onRelease = { webView ->
            webView.stopLoading()
            webView.onPause()
            webView.destroy()
            if (webViewRef === webView) {
                webViewRef = null
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