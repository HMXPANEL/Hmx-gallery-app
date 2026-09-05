package com.example.data.model

enum class MediaType {
    IMAGE,
    VIDEO,
    DOCUMENT,
    OTHER;

    companion object {
        private val IMAGE_EXTS = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "svg")
        private val VIDEO_EXTS = setOf("mp4", "webm", "ogg", "avi", "mov", "mkv", "flv", "wmv")

        fun fromFilename(fileName: String): MediaType {
            val extension = fileName.substringAfterLast('.', "").lowercase()
            return when {
                extension in IMAGE_EXTS -> IMAGE
                extension in VIDEO_EXTS -> VIDEO
                extension == "pdf" -> DOCUMENT
                else -> OTHER
            }
        }

        fun mimeTypeFromFilename(fileName: String): String {
            val extension = fileName.substringAfterLast('.', "").lowercase()
            return when (extension) {
                "jpg", "jpeg" -> "image/jpeg"
                "png" -> "image/png"
                "gif" -> "image/gif"
                "webp" -> "image/webp"
                "svg" -> "image/svg+xml"
                "bmp" -> "image/bmp"
                "mp4", "mov", "mkv", "avi" -> "video/mp4"
                "webm" -> "video/webm"
                "ogg" -> "video/ogg"
                "pdf" -> "application/pdf"
                else -> "application/octet-stream"
            }
        }
    }
}

data class GalleryItem(
    val name: String,
    val path: String,
    val size: Long = 0L,
    val updatedAt: String = "",
    val mediaType: MediaType = MediaType.fromFilename(name),
    val signedUrl: String? = null
) {
    val fileExtension: String
        get() = name.substringAfterLast('.', "").uppercase()
}
