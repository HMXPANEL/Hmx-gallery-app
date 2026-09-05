package com.example.data.remote

import com.example.BuildConfig

object SupabaseConfig {
    /**
     * Bucket name matching the original HMX iOS Gallery web application.
     */
    const val BUCKET_NAME = "images"
    val bucketName: String get() = BUCKET_NAME

    /**
     * Retrieves the Supabase URL, defaulting to BuildConfig injected via .env / Secrets panel.
     */
    val url: String
        get() {
            val key = try {
                BuildConfig.SUPABASE_URL
            } catch (e: Throwable) {
                ""
            }
            return if (key.isNotBlank()) key.trimEnd('/') else "https://superbase url"
        }

    /**
     * Retrieves the Supabase anon/public key.
     */
    val anonKey: String
        get() {
            return try {
                BuildConfig.SUPABASE_ANON_KEY.trim()
            } catch (e: Throwable) {
                "superbase api"
            }
        }

    /**
     * Checks if actual Supabase credentials are configured or using dummy placeholders.
     */
    val isConfigured: Boolean
        get() = url.startsWith("https://") && !url.contains("superbase url") && anonKey != "superbase api"

    /**
     * User-safe diagnostic for missing credentials.
     * Names the expected build variables but never contains secret values.
     */
    fun missingConfigMessage(): String =
        "Gallery service is not configured. Rebuild the app with SUPABASE_URL and SUPABASE_ANON_KEY."
}
