# Threat Model & Security Guarantees

## Assets Protected
1. **Photo Metadata**: GPS location coordinates, camera/body/lens serial numbers, document/instance IDs, timestamps, and camera technical settings.
2. **Vault Content**: User photos and sensitive documents stored in the encrypted vault.
3. **Encryption Keys**: AES-256-GCM keys managed inside Android Keystore (StrongBox/TEE).

## Adversaries & Scenarios
- **Physical Device Seizure / Loss**: Adversary attempting to read raw storage. Mitigated by Android File-Based Encryption (FBE) and AES-256-GCM vault encryption.
- **Malicious Apps on Device**: Malware inspecting external media or app cache. Mitigated by `FLAG_SECURE`, zero network permission, and immediate cache purging.
- **Data Exfiltration via Backup/Cloud**: Mitigated by disabling Android cloud backups (`allowBackup=false`) and data extraction rules.

## Hardware & System Limitations (Out of Scope)
- **Flash Memory FTL & Wear-Leveling**: Modern NAND flash controllers remap logical block addresses. Overwriting a block on flash memory writes to new physical cells, leaving unallocated wear-leveled blocks until garbage collected by the controller.
- **System Thumbnails & Cloud Copies**: Android's MediaStore thumbnail database or third-party gallery cloud sync copies created prior to cleaning cannot be erased retroactively by third-party apps without system root.
- **Forensic Secure Erase Disclaimer**: This app makes no claim of physical forensic secure erase on flash storage hardware.
