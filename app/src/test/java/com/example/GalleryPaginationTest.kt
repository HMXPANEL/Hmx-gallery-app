package com.example

import com.example.data.model.GalleryItem
import com.example.data.repository.GalleryRepository
import com.example.data.repository.mergeGalleryPages
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GalleryPaginationTest {

    private fun item(name: String) = GalleryItem(name = name, path = "g/$name")

    @Test
    fun `page size fits large galleries without tiny pages`() {
        assertTrue(GalleryRepository.PAGE_SIZE in 50..100)
    }

    @Test
    fun `append adds only unseen paths preserving order`() {
        val existing = listOf(item("a.jpg"), item("b.jpg"))
        val incoming = listOf(item("b.jpg"), item("c.jpg"))
        val merged = mergeGalleryPages(existing, incoming)
        assertEquals(listOf("g/a.jpg", "g/b.jpg", "g/c.jpg"), merged.map { it.path })
    }

    @Test
    fun `fully overlapping page adds nothing`() {
        val existing = listOf(item("a.jpg"))
        val merged = mergeGalleryPages(existing, listOf(item("a.jpg")))
        assertEquals(1, merged.size)
        assertEquals("g/a.jpg", merged[0].path)
    }

    @Test
    fun `empty sides are handled`() {
        val incoming = listOf(item("a.jpg"))
        assertEquals(incoming, mergeGalleryPages(emptyList(), incoming))
        assertEquals(incoming, mergeGalleryPages(incoming, emptyList()))
    }

    @Test
    fun `simulated 1000-item scroll accumulates no duplicates`() {
        // 17 pages x 60 with a 5-item overlap between consecutive pages.
        var accumulated = emptyList<GalleryItem>()
        var page = (1..60).map { item("f$it.jpg") }
        accumulated = mergeGalleryPages(accumulated, page)
        repeat(16) { round ->
            val overlap = page.takeLast(5)
            page = overlap + ((1..55).map { item("g${round}_$it.jpg") })
            accumulated = mergeGalleryPages(accumulated, page)
        }
        val paths = accumulated.map { it.path }
        assertEquals(paths.size, paths.toSet().size)
        assertEquals(60 + 16 * 55, paths.size)
    }
}
