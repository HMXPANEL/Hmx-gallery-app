package com.example.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

object VideoThumbnailHelper {
    // 20MB cache for video thumbnail bitmaps
    private val memoryCache = object : LruCache<String, Bitmap>(20 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.byteCount
        }
    }

    private val mutex = Mutex()

    suspend fun getThumbnail(videoUrl: String): Bitmap? = getThumbnail(null, videoUrl)

    suspend fun getThumbnail(context: Context?, videoUrl: String): Bitmap? = withContext(Dispatchers.IO) {
        if (videoUrl.isBlank()) return@withContext null

        // 1. Check memory cache
        memoryCache.get(videoUrl)?.let { return@withContext it }

        // 2. Check persistent disk cache if context is provided
        val diskCacheDir = context?.let { File(it.cacheDir, "video_thumbs").apply { mkdirs() } }
        val diskCacheKey = md5(videoUrl)
        val diskCacheFile = diskCacheDir?.let { File(it, "$diskCacheKey.jpg") }

        if (diskCacheFile != null && diskCacheFile.exists() && diskCacheFile.length() > 0) {
            try {
                val cachedBitmap = BitmapFactory.decodeFile(diskCacheFile.absolutePath)
                if (cachedBitmap != null) {
                    memoryCache.put(videoUrl, cachedBitmap)
                    return@withContext cachedBitmap
                }
            } catch (ignored: Throwable) {}
        }

        // 3. Serialize retrieval to protect native MediaMetadataRetriever from concurrency collisions
        mutex.withLock {
            // Check memory cache again in case another coroutine just resolved it
            memoryCache.get(videoUrl)?.let { return@withLock it }

            var retriever: MediaMetadataRetriever? = null

            try {
                val isRemote = videoUrl.startsWith("http://", ignoreCase = true) ||
                        videoUrl.startsWith("https://", ignoreCase = true)

                retriever = MediaMetadataRetriever()
                try {
                    if (isRemote) {
                        // Stream frame directly — avoids downloading the full file.
                        retriever.setDataSource(videoUrl, HashMap<String, String>())
                    } else {
                        retriever.setDataSource(videoUrl)
                    }
                } catch (e: Throwable) {
                    return@withLock null
                }

                // Verify video track exists before calling getFrameAtTime to prevent native JNI NULL pointer error
                val hasVideo = try {
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO)
                } catch (e: Throwable) {
                    null
                }

                if (hasVideo != "yes") {
                    return@withLock null
                }

                val durationMs = try {
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                } catch (e: Throwable) {
                    0L
                }

                val rawBitmap = try {
                    // Always try representative frame first, which is guaranteed to find the initial keyframe
                    retriever.frameAtTime ?: if (durationMs > 200L) {
                        retriever.getFrameAtTime(100_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    } else {
                        null
                    }
                } catch (e: Throwable) {
                    null
                }

                if (rawBitmap != null) {
                    // Scale to thumbnail size (max 300x300) to keep memory lightweight
                    val scaled = if (rawBitmap.width > 300 || rawBitmap.height > 300) {
                        val ratio = rawBitmap.width.toFloat() / rawBitmap.height.toFloat()
                        val targetW = if (ratio >= 1f) 300 else (300 * ratio).toInt()
                        val targetH = if (ratio >= 1f) (300 / ratio).toInt() else 300
                        Bitmap.createScaledBitmap(rawBitmap, targetW.coerceAtLeast(1), targetH.coerceAtLeast(1), true)
                    } else {
                        rawBitmap
                    }

                    memoryCache.put(videoUrl, scaled)

                    // Persist to disk cache
                    diskCacheFile?.let { file ->
                        try {
                            FileOutputStream(file).use { out ->
                                scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
                            }
                        } catch (ignored: Throwable) {}
                    }

                    return@withLock scaled
                }
            } catch (e: Throwable) {
                // Silently return null on network/codec error, allowing fallback icon
            } finally {
                try {
                    retriever?.release()
                } catch (ignored: Throwable) {}
            }
            null
        }
    }

    private fun md5(input: String): String {
        return try {
            val md = MessageDigest.getInstance("MD5")
            val digest = md.digest(input.toByteArray())
            digest.joinToString("") { "%02x".format(it) }
        } catch (e: Throwable) {
            input.hashCode().toString()
        }
    }

    fun clear() {
        memoryCache.evictAll()
    }
}
