package com.example.data.repository

import com.example.data.model.GalleryItem
import com.example.data.model.MediaType
import com.example.data.remote.SupabaseStorageClient
import com.example.utils.FileValidator
import com.example.utils.SignedUrlCache
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit

/**
 * Appends a fetched page without duplicates. Page overlaps (retries, shifting
 * server offsets) must never duplicate paths: duplicates would break LazyGrid
 * key stability and trigger redundant sign-URL/thumbnail work per duplicate.
 */
internal fun mergeGalleryPages(
    existing: List<GalleryItem>,
    incoming: List<GalleryItem>
): List<GalleryItem> {
    if (incoming.isEmpty()) return existing
    if (existing.isEmpty()) return incoming
    val known = HashSet<String>(existing.size + incoming.size)
    existing.forEach { known.add(it.path) }
    val merged = ArrayList<GalleryItem>(existing.size + incoming.size)
    merged.addAll(existing)
    incoming.forEach { if (known.add(it.path)) merged.add(it) }
    return merged
}

class GalleryRepository(
    private val storageClient: SupabaseStorageClient = SupabaseStorageClient(),
    private val signedUrlCache: SignedUrlCache = SignedUrlCache()
) {
    companion object {
        // Items per page: large enough for ~2 viewports per round-trip (fewer
        // emissions for 1,000+ galleries), small enough to stay light. The grid
        // resolves signed URLs lazily for composed items only.
        const val PAGE_SIZE = 60
    }

    // Coalesces concurrent signed-URL fetches for the same path (grid race).
    private val inFlightMutex = Mutex()
    private val inFlight = mutableMapOf<String, CompletableDeferred<Result<String>>>()
    // Bounds concurrent sign-URL network calls across grid/viewer/share callers.
    // Fast scrolling composes many items at once; without this gate each composes
    // its own blocking OkHttp call (thread/GC pressure -> dropped frames).
    // Queued waiters suspend in cancellable withPermit, so items that leave the
    // viewport abandon the queue instead of firing wasted requests.
    private val signSemaphore = Semaphore(8)
    /**
     * Lists gallery files with pagination, sorting by updated_at desc.
     * Matches web: requests limit = itemsPerPage + 1 to determine if more items exist.
     */
    suspend fun listGalleryFiles(
        galleryName: String,
        page: Int = 0,
        itemsPerPage: Int = PAGE_SIZE
    ): Result<Pair<List<GalleryItem>, Boolean>> {
        val offset = page * itemsPerPage
        val limit = itemsPerPage + 1

        val result = storageClient.listFiles(
            galleryName = galleryName,
            limit = limit,
            offset = offset
        )

        return result.map { rawFiles ->
            val nonHidden = rawFiles.filter { !it.name.startsWith(".") }
            val hasMore = nonHidden.size > itemsPerPage
            val paged = nonHidden.take(itemsPerPage).map { file ->
                val filePath = "$galleryName/${file.name}"
                val cachedUrl = signedUrlCache.get(filePath)
                val size = (file.metadata?.get("size") as? Number)?.toLong() ?: 0L

                GalleryItem(
                    name = file.name,
                    path = filePath,
                    size = size,
                    updatedAt = file.updatedAt.orEmpty(),
                    mediaType = MediaType.fromFilename(file.name),
                    signedUrl = cachedUrl
                )
            }
            Pair(paged, hasMore)
        }
    }

    /**
     * Checks whether the gallery can be accessed.
     */
    suspend fun testGalleryAccess(galleryName: String): Result<Unit> {
        return storageClient.listFiles(galleryName = galleryName, limit = 1).map { Unit }
    }

    /**
     * Retrieves or generates a signed URL for a file path, checking the cache first.
     * Concurrent callers for the same path share one network request.
     */
    suspend fun getSignedUrl(filePath: String, expiresIn: Int = 3600): Result<String> {
        signedUrlCache.get(filePath)?.let { return Result.success(it) }

        val waiter: CompletableDeferred<Result<String>>
        val isOwner: Boolean
        inFlightMutex.withLock {
            signedUrlCache.get(filePath)?.let { return Result.success(it) }
            val existing = inFlight[filePath]
            if (existing != null) {
                waiter = existing
                isOwner = false
            } else {
                waiter = CompletableDeferred()
                inFlight[filePath] = waiter
                isOwner = true
            }
        }
        if (!isOwner) return waiter.await()

        try {
            // Gated: bounds total concurrent sign requests during scroll bursts.
            val result = signSemaphore.withPermit {
                storageClient.createSignedUrl(filePath, expiresIn)
            }
            result.onSuccess { url -> signedUrlCache.put(filePath, url, expiresIn) }
            waiter.complete(result)
            return result
        } catch (e: Throwable) {
            val failure = Result.failure<String>(e)
            waiter.complete(failure)
            return failure
        } finally {
            inFlightMutex.withLock { inFlight.remove(filePath) }
        }
    }

    /**
     * Uploads a single file to Supabase Storage with upsert enabled.
     */
    suspend fun uploadFile(
        galleryName: String,
        originalFileName: String,
        data: ByteArray,
        mimeType: String
    ): Result<String> {
        val sanitizedName = FileValidator.sanitizeFileName(originalFileName)
        val filePath = "$galleryName/$sanitizedName"

        val uploadResult = storageClient.uploadFile(
            filePath = filePath,
            data = data,
            mimeType = mimeType
        )

        return uploadResult.map {
            // Invalidate cache for replaced/uploaded file
            signedUrlCache.remove(filePath)
            filePath
        }
    }

    /**
     * Deletes one or more files from Supabase Storage.
     */
    suspend fun deleteFiles(
        galleryName: String,
        fileNames: List<String>
    ): Result<Unit> {
        val paths = fileNames.map { "$galleryName/$it" }
        val result = storageClient.deleteFiles(paths)
        result.onSuccess {
            signedUrlCache.removeAll(paths)
        }
        return result
    }

    fun invalidateCache(filePath: String) {
        signedUrlCache.remove(filePath)
    }

    fun clearCache() {
        signedUrlCache.clear()
    }
}
