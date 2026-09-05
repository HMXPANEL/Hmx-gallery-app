package com.example.viewmodel

import com.example.data.model.GalleryItem

sealed class AppScreen {
    object InitialLoading : AppScreen()
    object Setup : AppScreen()
    object Gallery : AppScreen()
}

data class ToastEvent(
    val message: String,
    val isError: Boolean = false,
    val id: Long = System.currentTimeMillis()
)

data class UploadProgressState(
    val isUploading: Boolean = false,
    val progressPercent: Float = 0f,
    val statusText: String = "",
    val totalFiles: Int = 0,
    val completedFiles: Int = 0,
    val failedFiles: Int = 0
)

data class DeleteConfirmationState(
    val isVisible: Boolean = false,
    val title: String = "",
    val message: String = "",
    val filesToDelete: List<String> = emptyList(),
    val isFromViewer: Boolean = false
)

data class GalleryUiState(
    val screen: AppScreen = AppScreen.InitialLoading,
    val currentGallery: String = "",
    val galleryItems: List<GalleryItem> = emptyList(),
    val selectedItemPaths: Set<String> = emptySet(),
    val isSelectionMode: Boolean = false,
    val currentPage: Int = 0,
    val hasMoreItems: Boolean = false,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isUploadOverlayOpen: Boolean = false,
    val uploadState: UploadProgressState = UploadProgressState(),
    val viewerIndex: Int? = null,
    val isActionSheetVisible: Boolean = false,
    val deleteConfirmation: DeleteConfirmationState = DeleteConfirmationState(),
    val activeToast: ToastEvent? = null,
    val selectedTab: Int = 0 // 0: Gallery, 1: Upload
) {
    val isViewerOpen: Boolean
        get() = viewerIndex != null && viewerIndex in galleryItems.indices

    val currentViewerItem: GalleryItem?
        get() = viewerIndex?.let { galleryItems.getOrNull(it) }

    val isEmpty: Boolean
        get() = !isLoading && galleryItems.isEmpty()
}
