# EXIF Cleaner + Vault Technical Specification

## Core Architecture
- **Language**: Kotlin only.
- **UI**: Jetpack Compose only (Material 3).
- **Minimum SDK**: API 29 (Android 10). Target SDK: API 36.
- **Pure Kotlin EXIF Core**: `com.example.exifclean.core.exif` contains zero `android.*` imports for standard JVM unit testing.

## Package Architecture
- `com.example.exifclean.core.exif`: JpegSegmentStripper, PngChunkStripper, WebpChunkStripper, MetadataReport, RiskScorer, TrailingDataDetector, ScanDataHasher.
- `com.example.exifclean.core.vault`: VaultKeyManager, StreamingAesGcm, VaultRepository, VaultIndex.
- `com.example.exifclean.core.shred`: BestEffortOverwriter, OriginalDeleter, FreeSpaceFiller, ResultStatus.
- `com.example.exifclean.data`: IntakeCopier, MetadataReader, SanitizerEngine, ParanoidReencoder, OutputWriter, BatchProcessor, VideoMetadataStripper.
- `com.example.exifclean.work`: ShredWorker, TtlCleanerWorker, VaultForegroundService, PanicTileService.
- `com.example.exifclean.ui`: IntakeScreen, MetadataDetailScreen, SanitizationDiffScreen, VaultScreen, SettingsScreen, OnboardingScreen, CleanCameraActivity, AppNavigation.

## Hardening Rules
1. `FLAG_SECURE` set on `MainActivity` and `CleanCameraActivity`.
2. `allowBackup="false"` and `data_extraction_rules.xml` exclude all domains.
3. `android.permission.INTERNET` omitted from `AndroidManifest.xml`.
4. Original files never written in place.
5. Stream-based chunk processing with zero full-image memory loads during lossless operations.
