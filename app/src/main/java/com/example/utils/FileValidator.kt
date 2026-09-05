package com.example.utils

object FileValidator {
    const val MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024L // 10MB limit

    /**
     * Sanitizes filename strictly conforming to original HMX iOS Gallery web application:
     * baseName replaced: [^a-zA-Z0-9\s\-_] -> '_'
     * spaces replaced: \s+ -> '_'
     * substring: max 100 chars
     * + extension
     */
    fun sanitizeFileName(name: String): String {
        val lastDotIndex = name.lastIndexOf('.')
        val baseName = if (lastDotIndex != -1) name.substring(0, lastDotIndex) else name
        val extension = if (lastDotIndex != -1) name.substring(lastDotIndex) else ""

        val cleanedBase = baseName
            .replace(Regex("[^a-zA-Z0-9\\s\\-_]"), "_")
            .replace(Regex("\\s+"), "_")
            .take(100)

        return "$cleanedBase$extension"
    }

    sealed class ValidationResult {
        object Valid : ValidationResult()
        data class Error(val title: String, val detail: String) : ValidationResult()
    }

    fun validateFile(name: String, sizeBytes: Long): ValidationResult {
        if (sizeBytes > MAX_FILE_SIZE_BYTES) {
            return ValidationResult.Error(
                title = "File size limit exceeded.",
                detail = "$name is larger than 10 MB. Please choose a smaller file."
            )
        }

        if (!name.contains('.')) {
            return ValidationResult.Error(
                title = "Unsupported file type.",
                detail = "$name doesn't have a recognized file extension."
            )
        }

        return ValidationResult.Valid
    }
}
