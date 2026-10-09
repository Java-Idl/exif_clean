# Testing Strategy and Manual Verification Checklist

## Automated Testing Overview

### JVM Core Unit Tests (`app/src/test/java/...`)
1. **RiskScorerTest**: Validates per-tag risk scoring (GPS/Serials = HIGH, Timestamps/Make/Model = MEDIUM, Exposure/ISO = LOW).
2. **TrailingDataDetectorTest**: Validates JPEG SOI/SOS/EOI parsing, EXIF detection, and trailing Motion Photo MP4 payload detection.
3. **JpegSegmentStripperTest**: Validates APP1 EXIF/XMP stripping, orientation tag injection, and trailing data removal.
4. **PngChunkStripperTest**: Validates PNG chunk CRC recalculation and removal of `tEXt`/`eXIf` metadata chunks.
5. **WebpChunkStripperTest**: Validates RIFF parsing, removal of `EXIF`/`XMP ` chunks, and VP8X flag bit clearing.
6. **StreamingAesGcmTest**: Validates 64KB chunked AES-GCM encryption/decryption round-tripping, bit-flip tamper detection, and chunk reordering protection.
7. **BestEffortOverwriterTest**: Validates single-pass keystream overwrite, forced channel sync, truncation, and deletion.

## Manual Test Checklist

- [ ] **Android 10 (API 29) - Android 14+ GPS Permission**: Test tapping "Read Location" requests `ACCESS_MEDIA_LOCATION` and `MediaStore.setRequireOriginal` displays GPS coordinates.
- [ ] **Share Sheet Integration**: Share 1..N photos from System Gallery via `ACTION_SEND` and `ACTION_SEND_MULTIPLE`.
- [ ] **Motion Photo Handling**: Process a Google Camera Motion Photo JPEG. Confirm Motion Photo MP4 trailing bytes detected and stripped.
- [ ] **HEIC File Sanitization**: Verify HEIC files route to Paranoid mode and verify metadata stripped.
- [ ] **ExifTool Comparison**: Compare output JPEG/PNG against ExifTool output (`exiftool -all=`).
- [ ] **Vault Crypto-Shredding**: Add photo to Vault, shred item or entire vault, verify Keystore key alias is deleted and ciphertext cannot be decrypted.
- [ ] **Panic Shortcut & Quick Settings Tile**: Activate Quick Settings tile or App Shortcut to trigger immediate vault shredding.
