package com.example

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import com.example.viewmodel.GalleryViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("HMX Gallery", appName)
    }

    @Test
    fun `viewModel can be created by AndroidViewModelFactory`() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val factory = ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        val viewModel = factory.create(GalleryViewModel::class.java)
        assertNotNull(viewModel)
    }

    @Test
    fun `video thumbnail helper handles invalid inputs gracefully`() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        com.example.utils.VideoThumbnailHelper.clear()
        val blankResult = com.example.utils.VideoThumbnailHelper.getThumbnail(context, "")
        org.junit.Assert.assertNull(blankResult)

        val invalidResult = com.example.utils.VideoThumbnailHelper.getThumbnail(context, "not_a_valid_url")
        org.junit.Assert.assertNull(invalidResult)
    }
}
