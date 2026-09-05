package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.data.model.GalleryItem
import com.example.data.model.MediaType
import com.example.ui.theme.SophisticatedContainer
import com.example.ui.theme.SophisticatedOnPrimary
import com.example.ui.theme.SophisticatedPrimary
import com.example.ui.theme.SophisticatedSurfaceVariant
import com.example.ui.theme.SophisticatedTextPrimary
import com.example.ui.theme.SophisticatedTextSecondary
import com.example.utils.VideoThumbnailHelper

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaGridItem(
    item: GalleryItem,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onResolveSignedUrl: suspend (String) -> String?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var resolvedUrl by remember(item.path) { mutableStateOf(item.signedUrl) }
    var videoThumbnail by remember(item.path) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(item.path) {
        if (resolvedUrl == null) {
            resolvedUrl = onResolveSignedUrl(item.path)
        }
        if (item.mediaType == MediaType.VIDEO && resolvedUrl != null) {
            videoThumbnail = VideoThumbnailHelper.getThumbnail(context, resolvedUrl!!)
        }
    }

    // Design: rounded-sm corner radius
    val itemShape = RoundedCornerShape(3.dp)

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(itemShape)
            .background(if (item.mediaType == MediaType.VIDEO) SophisticatedContainer else SophisticatedSurfaceVariant)
            .then(
                if (isSelected) {
                    Modifier.border(2.dp, SophisticatedPrimary, itemShape)
                } else {
                    Modifier
                }
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("media_item_${item.name}"),
        contentAlignment = Alignment.Center
    ) {
        when (item.mediaType) {
            MediaType.IMAGE -> {
                if (resolvedUrl != null) {
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(resolvedUrl)
                            // Stable keys: signed URLs rotate hourly, path does not.
                            // Reuses bitmap across URL refreshes, saving bandwidth/memory.
                            .memoryCacheKey(item.path)
                            .diskCacheKey(item.path)
                            .crossfade(true)
                            .build(),
                        contentDescription = item.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        loading = {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = SophisticatedPrimary,
                                    strokeWidth = 2.dp
                                )
                            }
                        },
                        error = {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = null,
                                    tint = SophisticatedTextSecondary.copy(alpha = 0.5f),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = SophisticatedTextSecondary.copy(alpha = 0.4f),
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Subtle bottom gradient for image item
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .aspectRatio(3f)
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0x40000000))
                            )
                        )
                )
            }

            MediaType.VIDEO -> {
                if (videoThumbnail != null) {
                    Image(
                        bitmap = videoThumbnail!!.asImageBitmap(),
                        contentDescription = item.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (resolvedUrl != null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = null,
                            tint = SophisticatedTextSecondary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = null,
                        tint = SophisticatedTextSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Video center play circle
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0x66000000)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Video",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Video badge bottom left
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0x99000000))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "VIDEO",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
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
                        contentDescription = null,
                        tint = SophisticatedTextSecondary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Extension badge top right
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0x99000000))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = item.fileExtension.take(4).uppercase(),
                        color = SophisticatedPrimary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Selection overlay tint: bg-[#A8C7FF]/20
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SophisticatedPrimary.copy(alpha = 0.20f))
            )
        }

        // Selection checkbox
        if (isSelectionMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(5.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) SophisticatedPrimary else Color(0x66000000))
                    .then(
                        if (!isSelected) {
                            Modifier.border(1.5.dp, SophisticatedTextSecondary.copy(alpha = 0.8f), CircleShape)
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = SophisticatedOnPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
