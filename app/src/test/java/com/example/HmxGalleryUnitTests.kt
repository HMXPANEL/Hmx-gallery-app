package com.example

import com.example.data.model.MediaType
import com.example.data.remote.SupabaseConfig
import com.example.utils.FileValidator
import com.example.utils.SignedUrlCache
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HmxGalleryUnitTests {

    @Test
    fun `test file name sanitization matches web implementation`() {
        // Space replacement: \s+ -> '_'
        val sanitizedSpaces = FileValidator.sanitizeFileName("my summer photo 2026.jpg")
        assertEquals("my_summer_photo_2026.jpg", sanitizedSpaces)

        // Special characters replaced: [^a-zA-Z0-9\s\-_] -> '_'
        val sanitizedSpecial = FileValidator.sanitizeFileName("test#file@name!$.png")
        assertEquals("test_file_name__.png", sanitizedSpecial)

        // Truncate base name to 100 characters + extension
        val longBase = "a".repeat(120) + ".mp4"
        val sanitizedLong = FileValidator.sanitizeFileName(longBase)
        assertEquals(104, sanitizedLong.length) // 100 'a's + ".mp4"
        assertTrue(sanitizedLong.endsWith(".mp4"))
    }

    @Test
    fun `test file validation enforces 10MB limit and extensions`() {
        val valid = FileValidator.validateFile("valid.jpg", 5 * 1024 * 1024L)
        assertTrue(valid is FileValidator.ValidationResult.Valid)

        val oversized = FileValidator.validateFile("large.mp4", 11 * 1024 * 1024L)
        assertTrue(oversized is FileValidator.ValidationResult.Error)
        assertEquals("File size limit exceeded.", (oversized as FileValidator.ValidationResult.Error).title)

        val noExtension = FileValidator.validateFile("unknownfile", 1024L)
        assertTrue(noExtension is FileValidator.ValidationResult.Error)
        assertEquals("Unsupported file type.", (noExtension as FileValidator.ValidationResult.Error).title)
    }

    @Test
    fun `test media type identification`() {
        assertEquals(MediaType.IMAGE, MediaType.fromFilename("pic.jpg"))
        assertEquals(MediaType.IMAGE, MediaType.fromFilename("photo.PNG"))
        assertEquals(MediaType.IMAGE, MediaType.fromFilename("anim.gif"))
        assertEquals(MediaType.IMAGE, MediaType.fromFilename("vector.svg"))

        assertEquals(MediaType.VIDEO, MediaType.fromFilename("clip.mp4"))
        assertEquals(MediaType.VIDEO, MediaType.fromFilename("movie.MOV"))
        assertEquals(MediaType.VIDEO, MediaType.fromFilename("stream.webm"))

        assertEquals(MediaType.DOCUMENT, MediaType.fromFilename("manual.pdf"))
        assertEquals(MediaType.OTHER, MediaType.fromFilename("archive.zip"))
    }

    @Test
    fun `test signed url cache behavior`() {
        val cache = SignedUrlCache()
        val path = "test_gallery/pic.jpg"
        val url = "https://supabase.co/storage/v1/object/sign/images/test_gallery/pic.jpg?token=xyz"

        assertNull(cache.get(path))

        cache.put(path, url, expiresInSeconds = 3600)
        assertEquals(url, cache.get(path))

        cache.remove(path)
        assertNull(cache.get(path))

        cache.put(path, url, expiresInSeconds = 3600)
        cache.clear()
        assertNull(cache.get(path))
    }

    @Test
    fun `test supabase config bucket name`() {
        assertEquals("images", SupabaseConfig.BUCKET_NAME)
        assertEquals("images", SupabaseConfig.bucketName)
    }
}
