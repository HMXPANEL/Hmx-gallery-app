package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.preferences.GalleryPreferences
import com.example.data.remote.SupabaseConfig
import com.example.data.repository.GalleryRepository
import com.example.data.repository.mergeGalleryPages
import com.example.utils.FileValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

class GalleryViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: GalleryRepository = GalleryRepository(),
    private val preferences: GalleryPreferences = GalleryPreferences(application)
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(GalleryUiState())
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

    private var toastJob: Job? = null

    init {
        startAtCloudGallery()
    }

    private fun startAtCloudGallery() {
        // Cloud Gallery is always the entry point: never auto-restore the previous
        // Gallery / Viewer / Upload screen. Persisted gallery data is left untouched
        // (see GalleryPreferences) — only the entry destination is fixed.
        _uiState.update { it.copy(screen = AppScreen.Setup) }
    }

    fun handleSetup(galleryName: String, onDone: (() -> Unit)? = null) {
        val trimmed = galleryName.trim()
        if (trimmed.length < 2) {
            showToast("Gallery name too short.\nPlease enter at least 2 characters.", isError = true)
            return
        }
        // Synchronously claim loading: repeated taps on Continue (button + IME
        // Done) must not fire duplicate gallery-access/creation requests.
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true) }
        if (!SupabaseConfig.isConfigured) {
            _uiState.update { it.copy(isLoading = false) }
            showToast(SupabaseConfig.missingConfigMessage(), isError = true)
            return
        }

        viewModelScope.launch {
            val testResult = repository.testGalleryAccess(trimmed)

            testResult.fold(
                onSuccess = {
                    preferences.saveGalleryName(trimmed)
                    _uiState.update {
                        it.copy(
                            screen = AppScreen.Gallery,
                            currentGallery = trimmed,
                            isLoading = false
                        )
                    }
                    showToast("Gallery \"$trimmed\" created successfully!")
                    loadGallery(clear = true)
                    onDone?.invoke()
                },
                onFailure = { error ->
                    // Even if 404 or folder doesn't exist yet, Supabase storage allows upload
                    val isNetworkError = error.message?.contains("Unable to resolve host", ignoreCase = true) == true
                    if (isNetworkError) {
                        _uiState.update { it.copy(isLoading = false) }
                        showToast("Cannot access storage.\nPlease check your connection.", isError = true)
                    } else {
                        // Allow proceeding with folder name
                        preferences.saveGalleryName(trimmed)
                        _uiState.update {
                            it.copy(
                                screen = AppScreen.Gallery,
                                currentGallery = trimmed,
                                isLoading = false
                            )
                        }
                        showToast("Gallery \"$trimmed\" created successfully!")
                        loadGallery(clear = true)
                        onDone?.invoke()
                    }
                }
            )
        }
    }

    fun loadGallery(clear: Boolean = true) {
        val gallery = _uiState.value.currentGallery
        if (gallery.isBlank()) return

        viewModelScope.launch {
            if (clear) {
                _uiState.update {
                    val refreshing = it.isRefreshing
                    it.copy(
                        // Don't show full-screen spinner when pull-to-refresh is active.
                        // Keep existing items so refresh doesn't flash empty or kill the viewer.
                        isLoading = !refreshing,
                        currentPage = 0,
                        hasMoreItems = true
                    )
                }
            }

            val pageToFetch = if (clear) 0 else _uiState.value.currentPage
            val result = repository.listGalleryFiles(
                gallery,
                page = pageToFetch,
                itemsPerPage = GalleryRepository.PAGE_SIZE
            )

            result.fold(
                onSuccess = { (newFiles, hasMore) ->
                    // Refresh replaces with page 0; pagination appends (deduped).
                    // Signed URLs resolve lazily for composed grid items only —
                    // never prefetched for off-screen pages.
                    val updatedList = if (clear) newFiles else mergeGalleryPages(_uiState.value.galleryItems, newFiles)
                    _uiState.update {
                        it.copy(
                            galleryItems = updatedList,
                            hasMoreItems = hasMore,
                            isLoading = false,
                            isRefreshing = false,
                            isLoadingMore = false
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            isLoadingMore = false
                        )
                    }
                    // Missing credentials are a configuration error, not a network failure.
                    // The message never contains secret values (see SupabaseConfig).
                    if (SupabaseConfig.isConfigured) {
                        showToast("Unable to load gallery.\nCheck your internet connection.", isError = true)
                    } else {
                        showToast(SupabaseConfig.missingConfigMessage(), isError = true)
                    }
                }
            )
        }
    }

    fun refreshGallery() {
        _uiState.update { it.copy(isRefreshing = true) }
        loadGallery(clear = true)
    }

    fun loadNextPage() {
        val state = _uiState.value
        if (state.isLoadingMore || !state.hasMoreItems || state.isLoading || state.isRefreshing) return

        _uiState.update { it.copy(isLoadingMore = true) }

        viewModelScope.launch {
            val nextPage = state.currentPage + 1
            val result = repository.listGalleryFiles(
                state.currentGallery,
                page = nextPage,
                itemsPerPage = GalleryRepository.PAGE_SIZE
            )

            result.fold(
                onSuccess = { (newFiles, hasMore) ->
                    _uiState.update {
                        it.copy(
                            galleryItems = mergeGalleryPages(it.galleryItems, newFiles),
                            currentPage = nextPage,
                            hasMoreItems = hasMore,
                            isLoadingMore = false
                        )
                    }
                },
                onFailure = {
                    _uiState.update { it.copy(isLoadingMore = false) }
                }
            )
        }
    }

    // --- Selection Mode ---

    fun toggleSelectionMode() {
        _uiState.update {
            val newMode = !it.isSelectionMode
            it.copy(
                isSelectionMode = newMode,
                selectedItemPaths = if (newMode) it.selectedItemPaths else emptySet()
            )
        }
    }

    fun toggleItemSelection(path: String) {
        _uiState.update {
            val selected = it.selectedItemPaths.toMutableSet()
            if (selected.contains(path)) {
                selected.remove(path)
            } else {
                selected.add(path)
            }
            it.copy(selectedItemPaths = selected)
        }
    }

    fun cancelSelectionMode() {
        _uiState.update {
            it.copy(
                isSelectionMode = false,
                selectedItemPaths = emptySet()
            )
        }
    }

    fun enterSelectionModeWithItem(path: String) {
        _uiState.update {
            it.copy(
                isSelectionMode = true,
                selectedItemPaths = setOf(path)
            )
        }
    }

    // --- Deletion Flow ---

    fun requestDeleteSelected() {
        val selectedPaths = _uiState.value.selectedItemPaths.toList()
        if (selectedPaths.isEmpty()) return

        val fileNames = selectedPaths.map { it.substringAfterLast('/') }
        val count = fileNames.size
        _uiState.update {
            it.copy(
                deleteConfirmation = DeleteConfirmationState(
                    isVisible = true,
                    title = "Delete Selected Items?",
                    message = "Are you sure you want to delete $count item(s)? This action cannot be undone.",
                    filesToDelete = fileNames,
                    isFromViewer = false
                )
            )
        }
    }

    fun requestDeleteCurrentViewerItem() {
        val currentItem = _uiState.value.currentViewerItem ?: return
        _uiState.update {
            it.copy(
                isActionSheetVisible = false,
                deleteConfirmation = DeleteConfirmationState(
                    isVisible = true,
                    title = "Delete This File?",
                    message = "Are you sure you want to delete \"${currentItem.name}\"? This action cannot be undone.",
                    filesToDelete = listOf(currentItem.name),
                    isFromViewer = true
                )
            )
        }
    }

    fun dismissDeleteConfirmation() {
        _uiState.update {
            it.copy(deleteConfirmation = DeleteConfirmationState(isVisible = false))
        }
    }

    fun confirmDelete() {
        val deleteState = _uiState.value.deleteConfirmation
        val gallery = _uiState.value.currentGallery
        val files = deleteState.filesToDelete
        val isFromViewer = deleteState.isFromViewer

        dismissDeleteConfirmation()

        if (files.isEmpty() || gallery.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = repository.deleteFiles(gallery, files)

            result.fold(
                onSuccess = {
                    if (isFromViewer) {
                        closeViewer()
                    }
                    cancelSelectionMode()
                    showToast("Successfully deleted ${files.size} item(s)")
                    loadGallery(clear = true)
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isLoading = false) }
                    showToast("Failed to delete file(s): ${error.localizedMessage}", isError = true)
                }
            )
        }
    }

    // --- Sharing Flow ---

    suspend fun resolveSignedUrlsForSelected(): List<Pair<String, String>> {
        val selectedPaths = _uiState.value.selectedItemPaths
        val gallery = _uiState.value.currentGallery
        return withContext(Dispatchers.IO) {
            selectedPaths.mapNotNull { path ->
                val fileName = path.substringAfterLast('/')
                val urlResult = repository.getSignedUrl(path)
                urlResult.getOrNull()?.let { url -> Pair(fileName, url) }
            }
        }
    }

    suspend fun resolveSignedUrl(filePath: String): String? {
        return repository.getSignedUrl(filePath).getOrNull()
    }

    // --- Viewer Flow ---

    fun openViewer(index: Int) {
        if (index in _uiState.value.galleryItems.indices) {
            _uiState.update { it.copy(viewerIndex = index, isActionSheetVisible = false) }
        }
    }

    fun setViewerIndex(index: Int) {
        if (index in _uiState.value.galleryItems.indices) {
            _uiState.update { it.copy(viewerIndex = index) }
        }
    }

    fun closeViewer() {
        _uiState.update { it.copy(viewerIndex = null, isActionSheetVisible = false) }
    }

    fun openActionSheet() {
        _uiState.update { it.copy(isActionSheetVisible = true) }
    }

    fun closeActionSheet() {
        _uiState.update { it.copy(isActionSheetVisible = false) }
    }

    // --- Upload Flow ---

    fun openUploadOverlay() {
        _uiState.update { it.copy(isUploadOverlayOpen = true) }
    }

    fun closeUploadOverlay() {
        _uiState.update { it.copy(isUploadOverlayOpen = false, selectedTab = 0) }
    }

    fun selectTab(index: Int) {
        _uiState.update {
            if (index == 1) {
                it.copy(selectedTab = 1, isUploadOverlayOpen = true)
            } else {
                it.copy(selectedTab = 0, isUploadOverlayOpen = false)
            }
        }
    }

    fun uploadFilesFromUris(context: Context, uris: List<Uri>) {
        if (uris.isEmpty()) return
        val gallery = _uiState.value.currentGallery
        if (gallery.isBlank()) {
            showToast("Please create a gallery first before uploading.", isError = true)
            return
        }

        viewModelScope.launch {
            closeUploadOverlay()

            // Resolve file names and sizes from Android content resolver
            data class PendingUpload(val name: String, val size: Long, val uri: Uri, val mime: String)

            val pendingList = mutableListOf<PendingUpload>()
            val contentResolver = context.applicationContext.contentResolver

            for (uri in uris) {
                var name = "file_${System.currentTimeMillis()}"
                var size = 0L
                val mime = contentResolver.getType(uri) ?: "application/octet-stream"

                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIdx != -1) name = cursor.getString(nameIdx)
                        if (sizeIdx != -1) size = cursor.getLong(sizeIdx)
                    }
                }

                // Do NOT trust InputStream.available() for size (often 0 for content URIs).
                // Unknown sizes (0) are enforced by the capped read below.
                val validation = FileValidator.validateFile(name, size)
                if (validation is FileValidator.ValidationResult.Error && size > 0L) {
                    showToast(validation.detail, isError = true)
                    continue
                }
                if (size == 0L && !name.contains('.')) {
                    showToast("$name doesn't have a recognized file extension.", isError = true)
                    continue
                }

                pendingList.add(PendingUpload(name, size, uri, mime))
            }

            if (pendingList.isEmpty()) return@launch

            // Initialize progress state
            _uiState.update {
                it.copy(
                    uploadState = UploadProgressState(
                        isUploading = true,
                        progressPercent = 10f,
                        statusText = "Preparing ${pendingList.size} files...",
                        totalFiles = pendingList.size,
                        completedFiles = 0,
                        failedFiles = 0
                    )
                )
            }

            val total = pendingList.size
            val completed = java.util.concurrent.atomic.AtomicInteger(0)
            val errors = java.util.concurrent.atomic.AtomicInteger(0)
            val semaphore = Semaphore(2) // 2 concurrent uploads to avoid overloading network/memory

            withContext(Dispatchers.IO) {
                pendingList.map { item ->
                    async {
                        semaphore.withPermit {
                            try {
                                // Bounded read: caps at 10MB+1 to enforce the limit
                                // without loading arbitrarily large files into memory.
                                val bytes = readCappedBytes(contentResolver, item.uri)
                                if (bytes == null) {
                                    errors.incrementAndGet()
                                    return@withPermit
                                }
                                if (bytes.size > FileValidator.MAX_FILE_SIZE_BYTES) {
                                    showToast(
                                        "${item.name} is larger than 10 MB. Please choose a smaller file.",
                                        isError = true
                                    )
                                    errors.incrementAndGet()
                                    return@withPermit
                                }
                                val result = repository.uploadFile(
                                    galleryName = gallery,
                                    originalFileName = item.name,
                                    data = bytes,
                                    mimeType = item.mime
                                )
                                if (result.isSuccess) {
                                    completed.incrementAndGet()
                                } else {
                                    errors.incrementAndGet()
                                }
                            } catch (e: Exception) {
                                errors.incrementAndGet()
                            } finally {
                                val doneCount = completed.get() + errors.get()
                                val progress = 10f + ((doneCount.toFloat() / total) * 85f)
                                _uiState.update {
                                    it.copy(
                                        uploadState = it.uploadState.copy(
                                            progressPercent = progress,
                                            statusText = "Uploading... $doneCount/$total",
                                            completedFiles = completed.get(),
                                            failedFiles = errors.get()
                                        )
                                    )
                                }
                            }
                        }
                    }
                }.awaitAll()
            }

            _uiState.update {
                it.copy(
                    uploadState = it.uploadState.copy(
                        progressPercent = 100f,
                        statusText = "Processing..."
                    )
                )
            }

            delay(400)

            _uiState.update {
                it.copy(uploadState = UploadProgressState(isUploading = false))
            }

            if (completed.get() > 0) {
                showToast("Successfully uploaded ${completed.get()} file(s)${if (errors.get() > 0) " (${errors.get()} failed)" else ""}")
                loadGallery(clear = true)
            } else if (errors.get() > 0) {
                showToast("All uploads failed. Check file size (max 10MB) and connection.", isError = true)
            }
        }
    }

    // --- Toast Notification ---
    private fun readCappedBytes(resolver: android.content.ContentResolver, uri: Uri): ByteArray? {
        return try {
            resolver.openInputStream(uri)?.use { input ->
                val out = java.io.ByteArrayOutputStream()
                val buf = ByteArray(64 * 1024)
                var total = 0L
                val cap = FileValidator.MAX_FILE_SIZE_BYTES + 1
                while (true) {
                    val n = input.read(buf)
                    if (n < 0) break
                    total += n
                    if (total > cap) {
                        out.write(buf, 0, n)
                        break
                    }
                    out.write(buf, 0, n)
                }
                out.toByteArray()
            }
        } catch (e: Exception) {
            null
        }
    }

    fun showToast(message: String, isError: Boolean = false) {
        toastJob?.cancel()
        _uiState.update {
            it.copy(activeToast = ToastEvent(message = message, isError = isError))
        }
        toastJob = viewModelScope.launch {
            delay(3000)
            _uiState.update { it.copy(activeToast = null) }
        }
    }

    fun dismissToast() {
        toastJob?.cancel()
        _uiState.update { it.copy(activeToast = null) }
    }

    fun exitToSetup() {
        viewModelScope.launch {
            preferences.clearGalleryName()
            repository.clearCache()
            _uiState.update {
                GalleryUiState(screen = AppScreen.Setup)
            }
        }
    }
}
