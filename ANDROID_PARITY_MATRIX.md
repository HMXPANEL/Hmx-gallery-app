# HMX iOS Gallery — Android Parity Matrix

This document tracks feature parity between the original HMX iOS Gallery web application (`index.html`, `script.js`, `style.css`) and the native Android Jetpack Compose application.

| # | Web Feature | Source Implementation | Android Native Equivalent | Implementation Status | Notes |
|---|---|---|---|---|---|
| 1 | **First-Launch / Setup Screen** | `setupScreen` with Cloud icon, Title, Subtitle, Folder input, "Start Uploading" button | `SetupScreen.kt` with glowing floating cloud icon, M3 styled text field, gradient action button | Complete | Validates length >= 2, verifies Supabase Storage access |
| 2 | **Gallery Persistence** | `localStorage.getItem('hmxGallery')` / `sessionStorage` | Android `DataStore Preferences` (`GalleryPreferences.kt`) | Complete | Restores active gallery on cold launch without re-prompting |
| 3 | **Supabase Storage Integration** | `@supabase/supabase-js` storage API (`images` bucket) | OkHttp + Moshi Supabase Storage Client (`SupabaseStorageClient.kt`) | Complete | Native HTTP client supporting list, signedUrl, upload (upsert), and batch delete |
| 4 | **Gallery Top Header** | Status bar time + Gallery Title + Refresh + Select + Upload (+) buttons | `GalleryTopBar.kt` with iOS-inspired glass styling, dynamic time/battery indicators, action icons | Complete | Dark translucent header with blur and icon buttons |
| 5 | **Responsive Media Grid** | CSS grid 3-column (4/6 cols on tablets) with 1:1 aspect ratio tiles | `LazyVerticalGrid` with 3 columns (adaptive for wider screens), 1:1 aspect ratio tiles | Complete | High performance, memory-efficient recycling |
| 6 | **Pagination & Infinite Scroll** | `ITEMS_PER_PAGE = 30`, limit 31 to check `hasMoreItems`, triggers near bottom | `LazyGridState` scroll detection with `isLoadingMore` guard and offset calculation | Complete | Batches 30 items per page, prevents duplicate requests |
| 7 | **Sort Order** | `sortBy: { column: 'updated_at', order: 'desc' }` | Supabase list request includes `sortBy: { column: 'updated_at', order: 'desc' }` | Complete | Exact parity with web sorting |
| 8 | **Lazy Hydration & URL Prefetch** | IntersectionObserver + `prefetchSignedUrls` | Viewport-driven lazy signed-URL resolve (gated, deduped) + Coil async thumbnail loader | Complete | Resolves URLs only for composed grid items; no eager page prefetch |
| 9 | **Signed URL In-Memory Cache** | `signedUrlCache = new Map()` with 1-hour expiration | `SignedUrlCache.kt` thread-safe in-memory cache with expiry checks | Complete | Reuses valid URLs; invalidates on delete/upload |
| 10 | **Image Thumbnails** | `<img>` tag with lazy loading and fallback | Coil `AsyncImage` with placeholder shimmer & crossfade | Complete | Efficient bitmap caching |
| 11 | **Video Thumbnails & Badges** | HTML5 `<video>` seek to 0.1s + Canvas drawImage + play badge | `MediaMetadataRetriever` / Coil Video frame decoder + video badge overlay | Complete | Generates video frame thumbnail without full playback |
| 12 | **Non-Media File Badges** | File icon + uppercase extension badge (e.g. PDF) | Generic document card with file extension badge | Complete | Graceful rendering of arbitrary storage items |
| 13 | **Pull-to-Refresh** | Touch gesture dragging `.pull-to-refresh` indicator | Jetpack Compose `PullToRefreshBox` / custom pull indicator | Complete | Reloads gallery, clears pagination, invalidates stale cache |
| 14 | **Selection Mode** | `isSelectMode`, `.select-checkbox`, `.photo-item.selected` | Long-press or "Select" header button enters selection mode | Complete | Circular checkmark in `#667eea` with border highlight |
| 15 | **Long Press Gesture** | 500ms touch timer entering selection mode | `combinedClickable(onLongClick = ..., onClick = ...)` | Complete | Reliable touch disambiguation |
| 16 | **Floating Selection Actions Bar** | Floating pill showing "X Selected", Trash, Share, Close buttons | Floating capsule action bar at bottom with animation | Complete | iOS-styled blurred dark pill with `#667eea` action icons |
| 17 | **Single & Bulk Deletion** | Supabase `.remove([path])` with confirmation dialog | `DeleteConfirmationDialog` + `repository.deleteFiles()` | Complete | Confirmation modal before permanent deletion |
| 18 | **Single & Bulk Sharing** | Clipboard copy + Web Share API (`navigator.share`) | Android `Intent.ACTION_SEND` / `Intent.ACTION_SEND_MULTIPLE` + clipboard copy | Complete | Shares signed links to other apps or clipboard |
| 19 | **File Upload (Single & Multi)** | File picker dialog (All, Images, Videos), max 10MB | Android Photo Picker / `ActivityResultContracts.GetMultipleContents` | Complete | Modern zero-permission document & media picker |
| 20 | **File Validation & Sanitization** | Max 10MB limit + regex sanitization (`[^a-zA-Z0-9\s\-_]` -> `_`) | `FileValidator.kt` checking 10MB limit and exact regex sanitization | Complete | Rejects oversized files with friendly error toast |
| 21 | **Upload Overlay & Progress** | Full-screen modal with drag zone and progress bar | `UploadOverlay.kt` modal with dashed border zone and animated progress | Complete | Shows preparing, percentage progress, and completion states |
| 22 | **Full-Screen Media Viewer** | Fullscreen black viewer with close, more, and share buttons | `MediaViewerScreen.kt` with gesture-enabled horizontal pager | Complete | Full-screen immersive view with swipe navigation |
| 23 | **Horizontal Swipe Navigation** | Touchstart/touchend X-axis diff > 50px | Compose `HorizontalPager` with smooth page animation | Complete | Swipe left/right between media items seamlessly |
| 24 | **Video Player in Viewer** | HTML5 `<video controls>` in full-screen slide | AndroidView with `ExoPlayer` / `VideoView` with play/pause/seek controls | Complete | Native hardware-accelerated video playback |
| 25 | **Action Sheet & Options** | iOS-style bottom sheet with Delete, Share, Cancel | Animated bottom sheet dialog matching iOS style | Complete | Delete (red destructive), Share, and Cancel actions |
| 26 | **Toast Notifications** | Slide-in toast with check/error icons | Custom Compose toast overlay matching iOS dark toast | Complete | 3-second auto-dismiss with success/error accents |
| 27 | **Bottom Tab Bar** | 2 tabs: "Gallery" and "Upload" with active glow | `BottomTabBar.kt` with iOS glass effect and icons | Complete | Seamless switching between gallery and upload sheet |
| 28 | **Back Navigation Handling** | N/A (browser history) | `BackHandler` in Compose (Viewer -> Gallery, Selection -> Normal, Gallery -> Exit) | Complete | Standard Android back button integration |
| 29 | **Error Recovery** | Friendly error toast + retry actions | Recoverable error banners, retry buttons, no crashes | Complete | Graceful offline and network interruption handling |
| 30 | **Design Language & Theme** | Dark glassmorphism, purple/blue gradients (`#667eea`, `#764ba2`) | Jetpack Compose custom M3 color scheme and glass modifiers | Complete | High visual fidelity to original CSS design |
