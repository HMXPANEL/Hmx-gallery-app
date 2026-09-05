package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.galleryDataStore: DataStore<Preferences> by preferencesDataStore(name = "hmx_gallery_prefs")

class GalleryPreferences(private val context: Context) {
    companion object {
        private val KEY_GALLERY_NAME = stringPreferencesKey("hmxGallery")
    }

    val savedGalleryName: Flow<String?> = context.galleryDataStore.data.map { preferences ->
        preferences[KEY_GALLERY_NAME]
    }

    suspend fun saveGalleryName(name: String) {
        context.galleryDataStore.edit { preferences ->
            preferences[KEY_GALLERY_NAME] = name
        }
    }

    suspend fun clearGalleryName() {
        context.galleryDataStore.edit { preferences ->
            preferences.remove(KEY_GALLERY_NAME)
        }
    }
}
