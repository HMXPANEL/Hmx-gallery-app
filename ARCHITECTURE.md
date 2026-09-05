# Architecture Documentation — HMX iOS Gallery Android

## Overview
HMX iOS Gallery Android is a native Kotlin Jetpack Compose application designed to provide exact functional and visual parity with the HMX iOS Gallery web application. It communicates directly with Supabase Storage without an unnecessary backend or user database.

```
┌─────────────────────────────────────────────────────────────┐
│                       Jetpack Compose UI                    │
│  SetupScreen | GalleryScreen | MediaViewer | UploadOverlay  │
└──────────────────────────────┬──────────────────────────────┘
                               │ StateFlow / Events
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                    GalleryViewModel                         │
│  - App State (Setup, Gallery, Viewer, Selection, Upload)    │
│  - Pagination & Infinite Scroll                             │
│  - Multi-selection State & Bulk Actions                     │
│  - Signed URL In-Memory Cache Manager                       │
└──────────────────────────────┬──────────────────────────────┘
                               │ Coroutines / Result
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                   GalleryRepository                         │
│  - listFiles(gallery, limit, offset, sort)                  │
│  - createSignedUrl(filePath, expiresIn)                     │
│  - uploadFile(filePath, stream, mimeType)                   │
│  - deleteFiles(filePaths)                                   │
└──────────────┬──────────────────────────────┬───────────────┘
               │                              │
               ▼                              ▼
┌─────────────────────────────┐┌──────────────────────────────┐
│  SupabaseStorageClient      ││  GalleryPreferences          │
│  (OkHttp / REST API)        ││  (DataStore Preferences)     │
│  - Bucket: "images"         ││  - Key: "hmxGallery"         │
└─────────────────────────────┘└──────────────────────────────┘
```

## Directory Structure
```
app/src/main/java/com/example/
├── MainActivity.kt
├── data/
│   ├── model/
│   │   ├── GalleryItem.kt
│   │   ├── SupabaseFile.kt
│   │   └── UploadProgress.kt
│   ├── preferences/
│   │   └── GalleryPreferences.kt
│   ├── remote/
│   │   ├── SupabaseConfig.kt
│   │   └── SupabaseStorageClient.kt
│   └── repository/
│       └── GalleryRepository.kt
├── ui/
│   ├── components/
│   │   ├── ActionSheet.kt
│   │   ├── BottomTabBar.kt
│   │   ├── DeleteConfirmationDialog.kt
│   │   ├── GalleryTopBar.kt
│   │   ├── HmxToast.kt
│   │   ├── MediaGridItem.kt
│   │   └── SelectionActionBar.kt
│   ├── gallery/
│   │   └── GalleryScreen.kt
│   ├── setup/
│   │   └── SetupScreen.kt
│   ├── theme/
│   │   ├── Color.kt
│   │   ├── Theme.kt
│   │   └── Type.kt
│   ├── upload/
│   │   └── UploadOverlay.kt
│   └── viewer/
│       ├── MediaViewerScreen.kt
│       └── VideoPlayerView.kt
├── utils/
│   ├── FileValidator.kt
│   ├── SignedUrlCache.kt
│   └── VideoThumbnailHelper.kt
└── viewmodel/
    ├── GalleryState.kt
    └── GalleryViewModel.kt
```

## Key Architectural Principles

1. **Lightweight & Modular MVVM**:
   - Single source of truth for gallery files: Supabase Storage.
   - UI Composables are purely reactive, observing state exposed via Kotlin `StateFlow`.
   - Actions (e.g. upload, select, delete, swipe) are routed through `GalleryViewModel`.

2. **Supabase Storage REST Client**:
   - Direct HTTP communication using OkHttp and Moshi.
   - Headers: `apikey: <anonKey>`, `Authorization: Bearer <anonKey>`.
   - List objects: `POST /storage/v1/object/list/images`
   - Signed URLs: `POST /storage/v1/object/sign/images/{filePath}` with `{ "expiresIn": 3600 }`
   - Upload: `POST /storage/v1/object/images/{filePath}` with `x-upsert: true`
   - Delete: `DELETE /storage/v1/object/images` with `{ "prefixes": [...] }`

3. **In-Memory Signed URL Cache**:
   - Holds map of `filePath -> CachedSignedUrl(url, expiresAt)`.
   - Valid for 1 hour (3600s).
   - Prevents redundant network round-trips when rendering grid thumbnails and opening viewer.

4. **Resource & Memory Safety**:
   - Coil provides asynchronous image decoding with disk/memory caching.
   - Video thumbnails generated on background threads with `MediaMetadataRetriever`.
   - Video player released immediately when navigating away from the viewer.
   - Upload concurrency throttled to avoid memory exhaustion.
