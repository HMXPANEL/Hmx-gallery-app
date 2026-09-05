package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.preferences.GalleryPreferences
import com.example.viewmodel.AppScreen
import com.example.viewmodel.GalleryViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Cloud Gallery is always the app entry point: a fresh process must open
 * Setup even when a previous session persisted a gallery name. Persisted
 * data itself must be left untouched.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CloudGalleryEntryPointTest {

    @Test
    fun `fresh launch starts at Cloud Gallery despite saved gallery`() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        // Simulate a previous session that persisted a gallery name.
        GalleryPreferences(app).saveGalleryName("previous_gallery")

        // Fresh ViewModel = fresh app process.
        val viewModel = GalleryViewModel(app)

        assertTrue(viewModel.uiState.value.screen is AppScreen.Setup)
    }

    @Test
    fun `entry point does not delete persisted gallery data`() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        GalleryPreferences(app).saveGalleryName("previous_gallery")

        GalleryViewModel(app) // fresh launch; must not clear storage

        assertEquals("previous_gallery", GalleryPreferences(app).savedGalleryName.first())
    }
}
