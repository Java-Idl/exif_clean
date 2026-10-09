package com.example.exifclean.core.vault

import android.content.Context
import com.example.exifclean.core.shred.ResultStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID

class VaultRepository(
    private val context: Context,
    private val keyManager: VaultKeyManager = VaultKeyManager()
) {

    private val vaultDir: File
        get() = File(context.filesDir, "vault").apply { if (!exists()) mkdirs() }

    private val indexFile: File
        get() = File(vaultDir, "vault_index.enc")

    private val indexKeyAlias = "vault_master_index_key"

    suspend fun importFileToVault(
        inputFile: File,
        displayName: String,
        mimeType: String,
        requireUserAuth: Boolean = false
    ): VaultItem = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        val keyAlias = "vault_key_$id"
        val encFileName = "$id.vault"
        val targetEncFile = File(vaultDir, encFileName)

        val key = keyManager.generateVaultKey(keyAlias, requireUserAuth)

        FileInputStream(inputFile).use { inStream ->
            FileOutputStream(targetEncFile).use { outStream ->
                StreamingAesGcm.encryptStream(key, inStream, outStream)
            }
        }

        val vaultItem = VaultItem(
            id = id,
            keyAlias = keyAlias,
            displayName = displayName,
            mimeType = mimeType,
            sizeBytes = inputFile.length(),
            createdAt = System.currentTimeMillis(),
            encryptedFileName = encFileName
        )

        val currentItems = getVaultItems().toMutableList()
        currentItems.add(vaultItem)
        saveVaultIndex(VaultIndex(currentItems))

        vaultItem
    }

    suspend fun exportFileFromVault(
        item: VaultItem,
        targetOutputStream: java.io.OutputStream
    ): Boolean = withContext(Dispatchers.IO) {
        val encFile = File(vaultDir, item.encryptedFileName)
        if (!encFile.exists()) return@withContext false

        val key = keyManager.getSecretKey(item.keyAlias) ?: return@withContext false

        try {
            FileInputStream(encFile).use { inStream ->
                StreamingAesGcm.decryptStream(key, inStream, targetOutputStream)
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun shredVaultItem(item: VaultItem): ResultStatus = withContext(Dispatchers.IO) {
        // 1. Destroy Keystore key
        keyManager.deleteKey(item.keyAlias)

        // 2. Delete ciphertext file
        val encFile = File(vaultDir, item.encryptedFileName)
        if (encFile.exists()) {
            encFile.delete()
        }

        // 3. Update index
        val currentItems = getVaultItems().filterNot { it.id == item.id }
        saveVaultIndex(VaultIndex(currentItems))

        ResultStatus.CRYPTO_SHREDDED
    }

    suspend fun shredEntireVault(): ResultStatus = withContext(Dispatchers.IO) {
        val items = getVaultItems()
        for (item in items) {
            keyManager.deleteKey(item.keyAlias)
            val encFile = File(vaultDir, item.encryptedFileName)
            if (encFile.exists()) encFile.delete()
        }

        keyManager.deleteKey(indexKeyAlias)
        if (indexFile.exists()) indexFile.delete()

        ResultStatus.CRYPTO_SHREDDED
    }

    suspend fun getVaultItems(): List<VaultItem> = withContext(Dispatchers.IO) {
        if (!indexFile.exists()) return@withContext emptyList()
        val indexKey = keyManager.getSecretKey(indexKeyAlias) ?: return@withContext emptyList()

        val bos = ByteArrayOutputStream()
        try {
            FileInputStream(indexFile).use { inStream ->
                StreamingAesGcm.decryptStream(indexKey, inStream, bos)
            }
            parseIndexJson(bos.toString(Charsets.UTF_8.name()))
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveVaultIndex(index: VaultIndex) {
        val indexKey = keyManager.getSecretKey(indexKeyAlias)
            ?: keyManager.generateVaultKey(indexKeyAlias, requireUserAuth = false)

        val json = serializeIndexJson(index)
        val bis = ByteArrayInputStream(json.toByteArray(Charsets.UTF_8))

        FileOutputStream(indexFile).use { outStream ->
            StreamingAesGcm.encryptStream(indexKey, bis, outStream)
        }
    }

    private fun serializeIndexJson(index: VaultIndex): String {
        val sb = StringBuilder()
        sb.append("[\n")
        index.items.forEachIndexed { i, item ->
            sb.append("  {")
            sb.append("\"id\":\"${item.id}\",")
            sb.append("\"keyAlias\":\"${item.keyAlias}\",")
            sb.append("\"displayName\":\"${escapeJson(item.displayName)}\",")
            sb.append("\"mimeType\":\"${item.mimeType}\",")
            sb.append("\"sizeBytes\":${item.sizeBytes},")
            sb.append("\"createdAt\":${item.createdAt},")
            sb.append("\"encryptedFileName\":\"${item.encryptedFileName}\"")
            sb.append("}")
            if (i < index.items.size - 1) sb.append(",")
            sb.append("\n")
        }
        sb.append("]")
        return sb.toString()
    }

    private fun parseIndexJson(json: String): List<VaultItem> {
        val list = mutableListOf<VaultItem>()
        if (json.trim().isEmpty() || json.trim() == "[]") return list

        val entries = json.split("},")
        for (entry in entries) {
            val id = extractJsonField(entry, "id") ?: continue
            val keyAlias = extractJsonField(entry, "keyAlias") ?: ""
            val displayName = extractJsonField(entry, "displayName") ?: ""
            val mimeType = extractJsonField(entry, "mimeType") ?: ""
            val sizeBytes = extractJsonField(entry, "sizeBytes")?.toLongOrNull() ?: 0L
            val createdAt = extractJsonField(entry, "createdAt")?.toLongOrNull() ?: 0L
            val encryptedFileName = extractJsonField(entry, "encryptedFileName") ?: ""

            list.add(
                VaultItem(
                    id = id,
                    keyAlias = keyAlias,
                    displayName = displayName,
                    mimeType = mimeType,
                    sizeBytes = sizeBytes,
                    createdAt = createdAt,
                    encryptedFileName = encryptedFileName
                )
            )
        }
        return list
    }

    private fun extractJsonField(json: String, field: String): String? {
        val pattern = "\"$field\"\\s*:\\s*\"?([^\",\\}\n]+)\"?"
        val regex = Regex(pattern)
        val match = regex.find(json)
        return match?.groupValues?.get(1)
    }

    private fun escapeJson(value: String): String {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
    }
}
