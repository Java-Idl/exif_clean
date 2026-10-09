# EXIF Cleaner + Vault

An offline-first Android privacy tool designed to inspect, strip metadata from images (JPEG, PNG, WebP, HEIC), and securely store sensitive files in an encrypted vault featuring crypto-shredding and best-effort deletion.

## Features

- **Metadata Inspection & Sanitization**: Detects EXIF, XMP, IPTC, MakerNotes, ICC profiles, and Motion Photo MP4 trailing data. Supports both **Lossless** stream segment stripping and **Paranoid** pixel re-encoding.
- **Mandatory Output Verification**: Hashes raw scan data before and after lossless stripping to guarantee 100% pixel fidelity while confirming 0 remaining metadata tags.
- **Encrypted Vault & Crypto-Shredding**: Encrypts files using AES-256-GCM keys generated in the Android Keystore (StrongBox with TEE fallback) via 64KB chunked authenticated streaming. Shredding deletes the Keystore key alias, rendering ciphertext permanently unrecoverable.
- **Hardened Security**: Zero network permissions (`android.permission.INTERNET` omitted), `FLAG_SECURE` enforced across screens, `allowBackup=false`, and strict data extraction exclusion rules.
- **Panic Action**: Instant single-tap "Shred Entire Vault" action accessible via App Shortcuts and Quick Settings tile.

## Operation Result Statuses

- `LOSSLESS_CLEANED`: Metadata segments removed directly from streams without decoding pixels.
- `REENCODED_CLEANED`: Image decoded and re-encoded in Paranoid mode.
- `CRYPTO_SHREDDED`: Android Keystore encryption key destroyed and ciphertext unlinked.
- `OVERWRITTEN_BEST_EFFORT`: Single-pass throwaway-key keystream overwrite for app-private storage.
- `UNLINKED_ONLY`: Original file deleted via MediaStore or SAF request.
- `FAILED(reason)`: Verification or processing failure; output deleted immediately.

## Building and Testing

### Build Debug APK
```bash
./gradlew assembleDebug
```

### Run Unit Tests
```bash
./gradlew testDebugUnitTest
```

### Run Instrumented Tests
```bash
./gradlew connectedCheck
```
