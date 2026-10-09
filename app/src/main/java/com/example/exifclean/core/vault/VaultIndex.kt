package com.example.exifclean.core.vault

data class VaultItem(
    val id: String,
    val keyAlias: String,
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val createdAt: Long,
    val encryptedFileName: String
)

data class VaultIndex(
    val items: List<VaultItem> = emptyList()
)
