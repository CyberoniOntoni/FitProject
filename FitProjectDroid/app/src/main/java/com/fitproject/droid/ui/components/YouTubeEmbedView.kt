package com.fitproject.droid.ui.components

import android.annotation.SuppressLint
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebView
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
import coil.compose.AsyncImage
import com.fitproject.droid.data.FPWorkoutExercise
import com.fitproject.droid.ui.theme.BWSColors

private const val YOUTUBE_REFERER = "https://com.fitproject.droid"

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
        return """
            <!DOCTYPE html>
            <html>
            <head>
            <meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <meta name="referrer" content="strict-origin-when-cross-origin">
            <style>
            html,body{margin:0;padding:0;width:100%;height:100%;background:#000;overflow:hidden}
            iframe{position:absolute;inset:0;width:100%;height:100%;border:0}
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

object YouTubeEmbedUrls {
    fun embedUrl(youtubeId: String, autoplay: Boolean = true): String {
        val autoplayParam = if (autoplay) "1" else "0"
        val origin = Uri.encode(YOUTUBE_REFERER)
        return "https://www.youtube-nocookie.com/embed/$youtubeId" +
            "?autoplay=$autoplayParam&rel=0&modestbranding=1&playsinline=1&enablejsapi=1&origin=$origin"
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubeEmbedView(
    youtubeId: String,
    modifier: Modifier = Modifier
) {
    val normalizedId = remember(youtubeId) { YouTubeIds.normalize(youtubeId) ?: youtubeId }
    val embedUrl = remember(normalizedId) { YouTubeEmbedUrls.embedUrl(normalizedId) }
    var loadedId by remember { mutableStateOf<String?>(null) }

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
            }
        },
        update = { webView ->
            if (loadedId != normalizedId) {
                loadedId = normalizedId
                webView.loadUrl(
                    embedUrl,
                    mapOf("Referer" to YOUTUBE_REFERER)
                )
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

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(BWSColors.CardRadius.dp))
            .background(BWSColors.SurfaceHighlight)
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