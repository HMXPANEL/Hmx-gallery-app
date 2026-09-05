package com.example.ui.viewer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.data.model.GalleryItem
import com.example.data.model.MediaType
import com.example.ui.theme.SophisticatedBg
import com.example.ui.theme.SophisticatedPrimary
import com.example.ui.theme.SophisticatedSurface
import com.example.ui.theme.SophisticatedSurfaceVariant
import com.example.ui.theme.SophisticatedTextPrimary
import com.example.ui.theme.SophisticatedTextSecondary

@Composable
fun MediaViewerScreen(
    items: List<GalleryItem>,
    initialIndex: Int,
    onIndexChanged: (Int) -> Unit,
    onClose: () -> Unit,
    onOptionsClick: () -> Unit,
    onResolveSignedUrl: suspend (String) -> String?,
    onShowToast: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty() || initialIndex !in items.indices) return

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val pagerState = rememberPagerState(initialPage = initialIndex) { items.size }

    var areControlsVisible by remember { mutableStateOf(true) }

    BackHandler {
        onClose()
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            onIndexChanged(page)
        }
    }

    val currentItem = items.getOrNull(pagerState.currentPage)

    fun shareCurrentItem() {
        val item = currentItem ?: return
        val url = item.signedUrl
        if (url.isNullOrBlank()) {
            onShowToast("Loading media link...")
            return
        }

        clipboardManager.setText(AnnotatedString(url))
        onShowToast("Link copied to clipboard!")

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, item.name)
            putExtra(Intent.EXTRA_TEXT, url)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share ${item.name}"))
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SophisticatedBg)
            .testTag("media_viewer")
    ) {
        // Pager for horizontal swipe navigation
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            areControlsVisible = !areControlsVisible
                        }
                    )
                }
        ) { pageIndex ->
            val pageItem = items.getOrNull(pageIndex)
            if (pageItem != null) {
                MediaViewerItem(
                    item = pageItem,
                    isActive = pagerState.currentPage == pageIndex,
                    onResolveSignedUrl = onResolveSignedUrl,
                    onToggleControls = { areControlsVisible = !areControlsVisible }
                )
            }
        }

        // Top Header Controls
        AnimatedVisibility(
            visible = areControlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SophisticatedSurface.copy(alpha = 0.88f))
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Close button (down chevron)
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(SophisticatedSurfaceVariant)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true),
                            onClick = onClose
                        )
                        .testTag("viewer_close_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Close Viewer",
                        tint = SophisticatedTextPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Actions (Options & Share)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Options button
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(SophisticatedSurfaceVariant)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true),
                                onClick = onOptionsClick
                            )
                            .testTag("viewer_options_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreHoriz,
                            contentDescription = "Options",
                            tint = SophisticatedTextPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Share button
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(SophisticatedSurfaceVariant)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true),
                                onClick = { shareCurrentItem() }
                            )
                            .testTag("viewer_share_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = SophisticatedTextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaViewerItem(
    item: GalleryItem,
    isActive: Boolean,
    onResolveSignedUrl: suspend (String) -> String?,
    onToggleControls: () -> Unit
) {
    val context = LocalContext.current
    var signedUrl by remember(item.path) { mutableStateOf(item.signedUrl) }
    var isLoadingUrl by remember(item.path) { mutableStateOf(false) }

    LaunchedEffect(item.path) {
        if (signedUrl == null) {
            isLoadingUrl = true
            signedUrl = onResolveSignedUrl(item.path)
            isLoadingUrl = false
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (isLoadingUrl) {
            CircularProgressIndicator(
                color = SophisticatedPrimary,
                modifier = Modifier.size(36.dp)
            )
        } else if (signedUrl != null) {
            when (item.mediaType) {
                MediaType.IMAGE -> {
                    var scale by remember { mutableFloatStateOf(1f) }
                    var offsetX by remember { mutableFloatStateOf(0f) }
                    var offsetY by remember { mutableFloatStateOf(0f) }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    scale = (scale * zoom).coerceIn(1f, 4f)
                                    if (scale > 1f) {
                                        offsetX += pan.x
                                        offsetY += pan.y
                                    } else {
                                        offsetX = 0f
                                        offsetY = 0f
                                    }
                                }
                            }
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationX = offsetX
                                translationY = offsetY
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        SubcomposeAsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(signedUrl)
                                .memoryCacheKey(item.path)
                                .diskCacheKey(item.path)
                                .crossfade(true)
                                .build(),
                            contentDescription = item.name,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize(),
                            loading = {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = SophisticatedPrimary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }
                        )
                    }
                }

                MediaType.VIDEO -> {
                    if (isActive) {
                        VideoPlayer(
                            url = signedUrl!!,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                MediaType.DOCUMENT, MediaType.OTHER -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = item.name,
                            tint = SophisticatedTextSecondary,
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = item.name,
                            color = SophisticatedTextPrimary,
                            fontSize = 17.sp,
                            modifier = Modifier.padding(top = 84.dp)
                        )
                    }
                }
            }
        } else {
            Text(
                text = "Unable to load media",
                color = SophisticatedTextSecondary,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
private fun VideoPlayer(
    url: String,
    modifier: Modifier = Modifier
) {
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }
    var playFailed by remember(url) { mutableStateOf(false) }

    DisposableEffect(url) {
        onDispose {
            videoViewRef?.stopPlayback()
        }
    }

    if (playFailed) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Unable to play video",
                color = SophisticatedTextSecondary,
                fontSize = 15.sp
            )
        }
        return
    }

    AndroidView(
        factory = { ctx ->
            FrameLayout(ctx).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                val videoView = VideoView(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        android.view.Gravity.CENTER
                    )

                    val controller = MediaController(ctx)
                    controller.setAnchorView(this)
                    setMediaController(controller)
                    setVideoURI(Uri.parse(url))
                    setOnPreparedListener { mp ->
                        mp.isLooping = true
                        start()
                    }
                    setOnErrorListener { _, _, _ ->
                        playFailed = true
                        true
                    }
                }
                videoViewRef = videoView
                addView(videoView)
            }
        },
        update = { _ ->
            // No-op: factory already sets URI + starts onPrepared.
            // Re-setting here would restart playback on every recomposition.
        },
        modifier = modifier
    )
}

