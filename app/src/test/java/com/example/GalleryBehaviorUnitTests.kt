package com.example

import com.example.data.model.GalleryItem
import com.example.data.model.MediaType
import com.example.utils.FileValidator
import com.example.utils.SignedUrlCache
import com.example.viewmodel.GalleryUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GalleryBehaviorUnitTests {

    @Test
    fun `signed url cache reserves 60s safety buffer`() {
        val cache = SignedUrlCache()
        val path = "g/vid.mp4"
        // 30s TTL is inside the 60s safety window -> treated as expired.
        cache.put(path, "http://x/1", expiresInSeconds = 30)
        assertNull(cache.get(path))
        // 3600s TTL stays valid.
        cache.put(path, "http://x/2", expiresInSeconds = 3600)
        assertEquals("http://x/2", cache.get(path))
    }

    @Test
    fun `viewer derived state follows index bounds`() {
        val items = listOf(
            GalleryItem(name = "a.jpg", path = "g/a.jpg"),
            GalleryItem(name = "b.mp4", path = "g/b.mp4")
        )
        assertFalse(GalleryUiState(galleryItems = items, viewerIndex = null).isViewerOpen)
        assertTrue(GalleryUiState(galleryItems = items, viewerIndex = 1).isViewerOpen)
        assertFalse(GalleryUiState(galleryItems = items, viewerIndex = 5).isViewerOpen)
        assertEquals("b.mp4", GalleryUiState(galleryItems = items, viewerIndex = 1).currentViewerItem?.name)
    }

    @Test
    fun `empty state is true only when not loading and no items`() {
        assertTrue(GalleryUiState(isLoading = false, galleryItems = emptyList()).isEmpty)
        assertFalse(GalleryUiState(isLoading = true, galleryItems = emptyList()).isEmpty)
        assertFalse(
            GalleryUiState(
                isLoading = false,
                galleryItems = listOf(GalleryItem(name = "a.jpg", path = "g/a.jpg"))
            ).isEmpty
        )
    }

    @Test
    fun `filtered viewer index maps back to full list`() {
        val items = listOf(
            GalleryItem(name = "a.jpg", path = "g/a.jpg"),
            GalleryItem(name = "b.mp4", path = "g/b.mp4"),
            GalleryItem(name = "c.jpg", path = "g/c.jpg")
        )
        val imagesOnly = items.filter { it.mediaType == MediaType.IMAGE }
        // Second visible image (c.jpg) is index 2 in the full list, not 1.
        val tapped = imagesOnly[1]
        val realIndex = items.indexOfFirst { it.path == tapped.path }
        assertEquals(2, realIndex)
    }

    @Test
    fun `sanitize keeps extension with multiple dots`() {
        // Parens/spaces collapse per web parity: [^a-zA-Z0-9\s\-_] -> '_' then \s+ -> '_'.
        assertEquals("my_photo__final_.jpg", FileValidator.sanitizeFileName("my photo (final).jpg"))
        assertEquals("archive_tar.gz", FileValidator.sanitizeFileName("archive#tar.gz"))
    }

    @Test
    fun `mime types cover grid badges`() {
        assertEquals("image/jpeg", MediaType.mimeTypeFromFilename("a.JPG"))
        assertEquals("video/mp4", MediaType.mimeTypeFromFilename("b.mov"))
        assertEquals("application/pdf", MediaType.mimeTypeFromFilename("c.pdf"))
        assertEquals(MediaType.OTHER, MediaType.fromFilename("d.zip"))
    }

    @Test
    fun `upload size boundary enforces 10MB`() {
        assertTrue(FileValidator.validateFile("a.jpg", 10 * 1024 * 1024L) is FileValidator.ValidationResult.Valid)
        val over = FileValidator.validateFile("a.jpg", 10 * 1024 * 1024L + 1)
        assertTrue(over is FileValidator.ValidationResult.Error)
    }
}
