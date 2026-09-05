package com.example.ui.gallery

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MediaType
import com.example.ui.components.ActionSheet
import com.example.ui.components.BottomTabBar
import com.example.ui.components.DeleteConfirmationDialog
import com.example.ui.components.GalleryTopBar
import com.example.ui.components.HmxToast
import com.example.ui.components.MediaGridItem
import com.example.ui.components.SelectionActionBar
import com.example.ui.theme.SophisticatedBg
import com.example.ui.theme.SophisticatedBorder
import com.example.ui.theme.SophisticatedContainer
import com.example.ui.theme.SophisticatedOnPrimary
import com.example.ui.theme.SophisticatedOnSecondary
import com.example.ui.theme.SophisticatedPrimary
import com.example.ui.theme.SophisticatedSecondary
import com.example.ui.theme.SophisticatedSurfaceVariant
import com.example.ui.theme.SophisticatedTextPrimary
import com.example.ui.theme.SophisticatedTextSecondary
import com.example.ui.upload.UploadOverlay
import com.example.ui.viewer.MediaViewerScreen
import com.example.viewmodel.GalleryUiState
import com.example.viewmodel.GalleryViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    viewModel: GalleryViewModel,
    uiState: GalleryUiState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()
    val pullRefreshState = rememberPullToRefreshState()

    var selectedFilter by rememberSaveable { mutableStateOf("All Media") }
    val filterCategories = listOf("All Media", "Images", "Videos")

    val displayedItems = remember(uiState.galleryItems, selectedFilter) {
        when (selectedFilter) {
            "Images" -> uiState.galleryItems.filter { it.mediaType == MediaType.IMAGE }
            "Videos" -> uiState.galleryItems.filter { it.mediaType == MediaType.VIDEO }
            else -> uiState.galleryItems
        }
    }

    // Handle back button: if viewer open -> close viewer; if selection mode -> cancel selection
    BackHandler(enabled = uiState.isViewerOpen || uiState.isSelectionMode || uiState.isUploadOverlayOpen) {
        when {
            uiState.isViewerOpen -> viewModel.closeViewer()
            uiState.isUploadOverlayOpen -> viewModel.closeUploadOverlay()
            uiState.isSelectionMode -> viewModel.cancelSelectionMode()
        }
    }

    // Infinite scroll detection
    LaunchedEffect(gridState, uiState.galleryItems.size, uiState.hasMoreItems) {
        snapshotFlow {
            val totalItems = gridState.layoutInfo.totalItemsCount
            val lastVisibleItem = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleItem >= totalItems - 6
        }
            .distinctUntilChanged()
            .collect { shouldLoadMore ->
                if (shouldLoadMore && uiState.hasMoreItems && !uiState.isLoadingMore && !uiState.isLoading) {
                    viewModel.loadNextPage()
                }
            }
    }

    Scaffold(
        topBar = {
            GalleryTopBar(
                title = uiState.currentGallery,
                isSelectionMode = uiState.isSelectionMode,
                onRefreshClick = { viewModel.refreshGallery() },
                onSelectToggle = { viewModel.toggleSelectionMode() },
                onUploadClick = { viewModel.openUploadOverlay() }
            )
        },
        bottomBar = {
            BottomTabBar(
                selectedTab = uiState.selectedTab,
                onTabSelected = { tabIndex ->
                    viewModel.selectTab(tabIndex)
                }
            )
        },
        containerColor = SophisticatedBg,
        modifier = modifier
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.refreshGallery() },
            state = pullRefreshState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Filter Chips - Sophisticated Dark Layout
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        filterCategories.forEach { category ->
                            val isChipSelected = selectedFilter == category
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isChipSelected) SophisticatedPrimary
                                        else SophisticatedSurfaceVariant
                                    )
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = ripple(bounded = true),
                                        onClick = { selectedFilter = category }
                                    )
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = category,
                                    color = if (isChipSelected) SophisticatedOnPrimary else SophisticatedTextSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    if (uiState.isLoading && uiState.galleryItems.isEmpty()) {
                        // Initial Loading spinner
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = SophisticatedPrimary,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    } else if (uiState.isEmpty || displayedItems.isEmpty()) {
                        // Empty State
                        EmptyStateView(
                            onUploadClick = { viewModel.openUploadOverlay() },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        // Adaptive grid: 3 columns on phones (~120dp cells), more on tablets/foldables.
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 120.dp),
                            state = gridState,
                            contentPadding = PaddingValues(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("gallery_media_grid")
                        ) {
                            itemsIndexed(
                                items = displayedItems,
                                key = { _, item -> item.path }
                            ) { index, item ->
                                val isSelected = uiState.selectedItemPaths.contains(item.path)

                                MediaGridItem(
                                    item = item,
                                    isSelected = isSelected,
                                    isSelectionMode = uiState.isSelectionMode,
                                    onResolveSignedUrl = { path ->
                                        viewModel.resolveSignedUrl(path)
                                    },
                                    onClick = {
                                        if (uiState.isSelectionMode) {
                                            viewModel.toggleItemSelection(item.path)
                                        } else {
                                            // displayedItems may be filtered; map back to full list index for viewer.
                                            val realIndex = uiState.galleryItems.indexOfFirst { it.path == item.path }
                                            if (realIndex >= 0) viewModel.openViewer(realIndex)
                                        }
                                    },
                                    onLongClick = {
                                        if (!uiState.isSelectionMode) {
                                            viewModel.enterSelectionModeWithItem(item.path)
                                        }
                                    }
                                )
                            }

                            // Bottom loading indicator when fetching next page
                            if (uiState.isLoadingMore) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            color = SophisticatedPrimary,
                                            modifier = Modifier.size(28.dp),
                                            strokeWidth = 2.5.dp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Sophisticated Floating Action Button (FAB)
                if (!uiState.isSelectionMode && !uiState.isUploadOverlayOpen && !uiState.isViewerOpen) {
                    FloatingActionButton(
                        onClick = { viewModel.openUploadOverlay() },
                        containerColor = SophisticatedSecondary,
                        contentColor = SophisticatedOnSecondary,
                        shape = RoundedCornerShape(16.dp),
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 16.dp, bottom = 16.dp)
                            .size(56.dp)
                            .testTag("gallery_fab_upload")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Upload Media",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // Selection Actions Bar (Capsule floating above bottom navigation)
                SelectionActionBar(
                    selectedCount = uiState.selectedItemPaths.size,
                    isVisible = uiState.isSelectionMode,
                    onDeleteClick = { viewModel.requestDeleteSelected() },
                    onShareClick = {
                        scope.launch {
                            val shareItems = viewModel.resolveSignedUrlsForSelected()
                            if (shareItems.isEmpty()) {
                                viewModel.showToast("Could not generate share links", isError = true)
                                return@launch
                            }

                            val shareText = shareItems.joinToString("\n\n") { (name, url) ->
                                "$name: $url"
                            }

                            clipboardManager.setText(AnnotatedString(shareText))
                            viewModel.showToast("${shareItems.size} link(s) copied to clipboard!")

                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Shared Media from HMX Gallery")
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(
                                Intent.createChooser(shareIntent, "Share ${shareItems.size} files")
                            )
                        }
                    },
                    onCancelClick = { viewModel.cancelSelectionMode() },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp)
                )
            }
        }
    }

    // Upload Overlay
    UploadOverlay(
        isOpen = uiState.isUploadOverlayOpen,
        uploadState = uiState.uploadState,
        onClose = { viewModel.closeUploadOverlay() },
        onFilesSelected = { uris ->
            viewModel.uploadFilesFromUris(context, uris)
        }
    )

    // Fullscreen Media Viewer
    if (uiState.isViewerOpen && uiState.viewerIndex != null) {
        MediaViewerScreen(
            items = uiState.galleryItems,
            initialIndex = uiState.viewerIndex,
            onIndexChanged = { index -> viewModel.setViewerIndex(index) },
            onClose = { viewModel.closeViewer() },
            onOptionsClick = { viewModel.openActionSheet() },
            onResolveSignedUrl = { path -> viewModel.resolveSignedUrl(path) },
            onShowToast = { msg -> viewModel.showToast(msg) }
        )
    }

    // Viewer Action Sheet
    ActionSheet(
        isVisible = uiState.isActionSheetVisible,
        onDeleteClick = { viewModel.requestDeleteCurrentViewerItem() },
        onShareClick = {
            viewModel.closeActionSheet()
            scope.launch {
                val current = uiState.currentViewerItem ?: return@launch
                val url = viewModel.resolveSignedUrl(current.path)
                if (url != null) {
                    clipboardManager.setText(AnnotatedString(url))
                    viewModel.showToast("Link copied to clipboard!")
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, current.name)
                        putExtra(Intent.EXTRA_TEXT, url)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share ${current.name}"))
                }
            }
        },
        onDismiss = { viewModel.closeActionSheet() }
    )

    // Delete Confirmation Dialog
    DeleteConfirmationDialog(
        isVisible = uiState.deleteConfirmation.isVisible,
        title = uiState.deleteConfirmation.title,
        message = uiState.deleteConfirmation.message,
        onConfirm = { viewModel.confirmDelete() },
        onDismiss = { viewModel.dismissDeleteConfirmation() }
    )

    // Toast Notifications
    HmxToast(toast = uiState.activeToast)
}

@Composable
private fun EmptyStateView(
    onUploadClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(32.dp)
            .testTag("gallery_empty_state"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(SophisticatedSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CloudUpload,
                    contentDescription = null,
                    tint = SophisticatedPrimary,
                    modifier = Modifier.size(42.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "No Photos Yet",
                color = SophisticatedTextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Upload your first photos and videos to get started",
                color = SophisticatedTextSecondary,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .height(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(SophisticatedPrimary)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true),
                        onClick = onUploadClick
                    )
                    .padding(horizontal = 24.dp)
                    .testTag("empty_upload_button"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Upload,
                        contentDescription = null,
                        tint = SophisticatedOnPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Upload Files",
                        color = SophisticatedOnPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

