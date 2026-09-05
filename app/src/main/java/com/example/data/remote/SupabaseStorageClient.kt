package com.example.data.remote

import android.net.Uri
import com.example.data.model.SignedUrlResponse
import com.example.data.model.SupabaseFile
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class SupabaseStorageClient(
    private val config: SupabaseConfig = SupabaseConfig,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
) {
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val fileListType = Types.newParameterizedType(List::class.java, SupabaseFile::class.java)
    private val fileListAdapter = moshi.adapter<List<SupabaseFile>>(fileListType)
    private val signedUrlAdapter = moshi.adapter(SignedUrlResponse::class.java)

    /**
     * Lists files from the specified gallery folder in Supabase Storage.
     */
    suspend fun listFiles(
        galleryName: String,
        limit: Int = 31,
        offset: Int = 0
    ): Result<List<SupabaseFile>> = withContext(Dispatchers.IO) {
        try {
            val listUrl = "${config.url}/storage/v1/object/list/${config.bucketName}"
            val requestJson = """
                {
                    "prefix": "$galleryName",
                    "limit": $limit,
                    "offset": $offset,
                    "sortBy": {
                        "column": "updated_at",
                        "order": "desc"
                    }
                }
            """.trimIndent()

            val request = Request.Builder()
                .url(listUrl)
                .addHeader("apikey", config.anonKey)
                .addHeader("Authorization", "Bearer ${config.anonKey}")
                .post(requestJson.toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    if (response.code == 404 || body.contains("Not Found", ignoreCase = true)) {
                        return@withContext Result.success(emptyList())
                    }
                    return@withContext Result.failure(
                        IOException("HTTP ${response.code}: $body")
                    )
                }

                val files = fileListAdapter.fromJson(body) ?: emptyList()
                Result.success(files)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Generates a signed URL for a file in Supabase Storage valid for [expiresIn] seconds (default 3600 = 1 hour).
     */
    suspend fun createSignedUrl(
        filePath: String,
        expiresIn: Int = 3600
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Encode each path segment
            val encodedSegments = filePath.split("/").joinToString("/") { Uri.encode(it) }
            val signUrl = "${config.url}/storage/v1/object/sign/${config.bucketName}/$encodedSegments"

            val requestJson = """
                {
                    "expiresIn": $expiresIn
                }
            """.trimIndent()

            val request = Request.Builder()
                .url(signUrl)
                .addHeader("apikey", config.anonKey)
                .addHeader("Authorization", "Bearer ${config.anonKey}")
                .post(requestJson.toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        IOException("HTTP ${response.code}: $body")
                    )
                }

                val result = signedUrlAdapter.fromJson(body)
                val rawUrl = result?.url
                if (rawUrl.isNullOrBlank()) {
                    return@withContext Result.failure(
                        IOException("Empty signed URL in response: $body")
                    )
                }

                val fullUrl = when {
                    rawUrl.startsWith("http://") || rawUrl.startsWith("https://") -> rawUrl
                    rawUrl.startsWith("/storage/v1") -> "${config.url}$rawUrl"
                    rawUrl.startsWith("/object/sign") -> "${config.url}/storage/v1$rawUrl"
                    else -> "${config.url}/storage/v1/${rawUrl.trimStart('/')}"
                }
                Result.success(fullUrl)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Uploads binary file data to Supabase Storage with upsert enabled.
     */
    suspend fun uploadFile(
        filePath: String,
        data: ByteArray,
        mimeType: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val encodedSegments = filePath.split("/").joinToString("/") { Uri.encode(it) }
            val uploadUrl = "${config.url}/storage/v1/object/${config.bucketName}/$encodedSegments"

            val body = data.toRequestBody(mimeType.toMediaTypeOrNull())

            val request = Request.Builder()
                .url(uploadUrl)
                .addHeader("apikey", config.anonKey)
                .addHeader("Authorization", "Bearer ${config.anonKey}")
                .addHeader("x-upsert", "true")
                .addHeader("cache-control", "3600")
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    if (response.code == 413 || responseBody.contains("Payload too large", ignoreCase = true)) {
                        return@withContext Result.failure(
                            IOException("Payload too large: File exceeds storage limit")
                        )
                    }
                    return@withContext Result.failure(
                        IOException("HTTP ${response.code}: $responseBody")
                    )
                }
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Deletes multiple file paths from Supabase Storage in batch.
     */
    suspend fun deleteFiles(
        filePaths: List<String>
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val deleteUrl = "${config.url}/storage/v1/object/${config.bucketName}"

            // Format prefixes JSON array
            val prefixesJson = filePaths.joinToString(
                separator = ",",
                prefix = "[",
                postfix = "]"
            ) { "\"${it.replace("\"", "\\\"")}\"" }

            val requestJson = """
                {
                    "prefixes": $prefixesJson
                }
            """.trimIndent()

            val request = Request.Builder()
                .url(deleteUrl)
                .addHeader("apikey", config.anonKey)
                .addHeader("Authorization", "Bearer ${config.anonKey}")
                .delete(requestJson.toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        IOException("HTTP ${response.code}: $body")
                    )
                }
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
