# Technical Deviations and Platform Adaptations

1. **Gradle AGP 9.2 Built-in Kotlin Integration**:
   - Android Gradle Plugin 9.2 integrates Kotlin directly into `com.android.application`. Applying `org.jetbrains.kotlin.android` alongside AGP 9.2 causes a duplicate extension registration error in Gradle. Therefore, `com.android.application` with `org.jetbrains.kotlin.plugin.compose` is applied directly.

2. **HEIC/HEIF Sanitization Fallback**:
   - `ExifInterface` on Android does not support writing metadata back to HEIF files. As specified in Phase 3, all HEIC/HEIF files are automatically routed to **Paranoid** mode for pixel re-encoding.

3. **Android 14 Quick Settings Tile API**:
   - Android 14+ (`API 34+`) deprecates `startActivityAndCollapse(Intent)` in favor of `startActivityAndCollapse(PendingIntent)`. `PanicTileService` uses a version check to call `startActivityAndCollapse(PendingIntent)` on Android 14+.
