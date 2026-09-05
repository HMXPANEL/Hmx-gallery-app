package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SupabaseFile(
    @Json(name = "name") val name: String,
    @Json(name = "id") val id: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "last_accessed_at") val lastAccessedAt: String? = null,
    @Json(name = "metadata") val metadata: Map<String, Any?>? = null
)

@JsonClass(generateAdapter = true)
data class SignedUrlResponse(
    @Json(name = "signedURL") val signedUrlUpper: String? = null,
    @Json(name = "signedUrl") val signedUrlLower: String? = null,
    @Json(name = "error") val error: String? = null,
    @Json(name = "message") val message: String? = null
) {
    val url: String?
        get() = signedUrlUpper ?: signedUrlLower
}
