package com.example.utils

import java.util.concurrent.ConcurrentHashMap

data class CachedSignedUrl(
    val url: String,
    val expiresAtMillis: Long
) {
    val isValid: Boolean
        // Consider invalid if less than 60 seconds remaining to prevent mid-operation expiry
        get() = System.currentTimeMillis() < (expiresAtMillis - 60_000)
}

class SignedUrlCache {
    private val cache = ConcurrentHashMap<String, CachedSignedUrl>()

    fun get(filePath: String): String? {
        val entry = cache[filePath] ?: return null
        return if (entry.isValid) {
            entry.url
        } else {
            cache.remove(filePath)
            null
        }
    }

    fun put(filePath: String, url: String, expiresInSeconds: Int = 3600) {
        val expiresAt = System.currentTimeMillis() + (expiresInSeconds * 1000L)
        cache[filePath] = CachedSignedUrl(url, expiresAt)
    }

    fun remove(filePath: String) {
        cache.remove(filePath)
    }

    fun removeAll(filePaths: Collection<String>) {
        filePaths.forEach { cache.remove(it) }
    }

    fun clear() {
        cache.clear()
    }
}
