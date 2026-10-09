package com.example.exifclean.core.vault

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class VaultKeyManager {

    private val keyStore: KeyStore = KeyStore.getInstance("AndroidKeyStore").apply {
        load(null)
    }

    fun generateVaultKey(
        alias: String,
        requireUserAuth: Boolean = false,
        authValidityDurationSeconds: Int = -1
    ): SecretKey {
        return try {
            generateKeyInternal(alias, requireUserAuth, authValidityDurationSeconds, useStrongBox = true)
        } catch (_: StrongBoxUnavailableException) {
            generateKeyInternal(alias, requireUserAuth, authValidityDurationSeconds, useStrongBox = false)
        } catch (_: Exception) {
            generateKeyInternal(alias, requireUserAuth, authValidityDurationSeconds, useStrongBox = false)
        }
    }

    fun getSecretKey(alias: String): SecretKey? {
        return try {
            if (!keyStore.containsAlias(alias)) return null
            (keyStore.getEntry(alias, null) as? KeyStore.SecretKeyEntry)?.secretKey
        } catch (_: Exception) {
            null
        }
    }

    fun deleteKey(alias: String): Boolean {
        return try {
            if (keyStore.containsAlias(alias)) {
                keyStore.deleteEntry(alias)
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    fun hasKey(alias: String): Boolean {
        return try {
            keyStore.containsAlias(alias)
        } catch (_: Exception) {
            false
        }
    }

    private fun generateKeyInternal(
        alias: String,
        requireUserAuth: Boolean,
        authValidityDurationSeconds: Int,
        useStrongBox: Boolean
    ): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore"
        )

        val builder = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)

        if (useStrongBox) {
            try {
                builder.setIsStrongBoxBacked(true)
            } catch (_: Exception) {}
        }

        if (requireUserAuth) {
            builder.setUserAuthenticationRequired(true)
            if (authValidityDurationSeconds > 0) {
                @Suppress("DEPRECATION")
                builder.setUserAuthenticationValidityDurationSeconds(authValidityDurationSeconds)
            }
        }

        keyGenerator.init(builder.build())
        return keyGenerator.generateKey()
    }
}
