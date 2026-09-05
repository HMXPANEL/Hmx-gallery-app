# HMX iOS Gallery — Android

A faithful native Android Jetpack Compose reproduction of the **HMX iOS Gallery** application, preserving the exact visual language, interaction model, and Supabase Storage structure.

---

## Features
- **iOS-Inspired Dark Glassmorphism UI**: Beautiful gradients (`#667eea` to `#764ba2`), frosted glass overlays, iOS status bar, and smooth touch interactions.
- **Gallery-Name Model**: Zero accounts, zero login, and zero custom database. The gallery name corresponds to a directory in Supabase Storage (`images/<galleryName>/`).
- **DataStore Persistence**: Automatically remembers the entered gallery name across app launches.
- **Efficient Media Grid**: 3-column lazy grid with pagination (30 items per page), infinite scroll guard, and pull-to-refresh.
- **Image & Video Thumbnails**: Uses Coil for image caching and `MediaMetadataRetriever` for on-device video thumbnail generation.
- **In-Memory Signed URL Cache**: Caches 1-hour signed URLs to minimize Supabase API calls.
- **Full-Screen Media Viewer**: Immersive viewer with horizontal swipe navigation between items, hardware-accelerated video playback with controls, and action sheet.
- **Selection & Bulk Actions**: Long-press or tap "Select" to enter multi-select mode. Delete multiple items or share links in bulk.
- **File Uploads**: Supports single and multiple image/video uploads with a 10MB per-file validation, filename sanitization, and progress feedback.
- **Sharing**: Native Android share sheet (`Intent.ACTION_SEND` and `ACTION_SEND_MULTIPLE`) and clipboard link copying.

---

## Supabase Storage Configuration

### Bucket Setup
1. In your [Supabase Dashboard](https://supabase.com), navigate to **Storage**.
2. Create a bucket named `images` (or ensure it exists).
3. Under Bucket Settings, configure permissions / policies:
   - For an open gallery with the anon key, add storage policies allowing `SELECT`, `INSERT`, and `DELETE` on the `images` bucket for `public` / `anon` role.

### Configuring Credentials in Android
The app reads the Supabase configuration from `BuildConfig` (or fallback defaults in `SupabaseConfig.kt`).

To configure your credentials securely:
1. Open the `.env` file (or configure via the **Secrets panel** in AI Studio):
   ```properties
   SUPABASE_URL=https://your-project-id.supabase.co
   SUPABASE_ANON_KEY=your-anon-public-key
   ```
2. Build the project. The Secrets Gradle Plugin injects these variables into `BuildConfig.SUPABASE_URL` and `BuildConfig.SUPABASE_ANON_KEY`.

---

## Building and Running
1. Open the project in Android Studio or compile with Gradle:
   ```bash
   gradle :app:assembleDebug
   ```
2. Run unit and UI tests:
   ```bash
   gradle :app:testDebugUnitTest
   ```

---

## Known Limitations & Considerations
- **Signed URL Expiration**: Supabase signed URLs expire after 1 hour (3600s). Shared links are temporary.
- **File Size Limit**: Files exceeding 10MB are rejected prior to upload to conform with the prototype specification.
