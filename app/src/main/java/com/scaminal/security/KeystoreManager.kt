/**
 * @file KeystoreManager.kt
 * @description Chiffrement/déchiffrement des credentials SSH via Android Keystore AES-GCM.
 */
package com.scaminal.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import timber.log.Timber
import java.nio.ByteBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KeystoreManager @Inject constructor() {

    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "scaminal_ssh_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
    }

    private val keyStore: KeyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }

    /**
     * Chiffre un texte en clair avec AES-256-GCM.
     *
     * @param plaintext Le texte à chiffrer
     * @return Blob au format [4 bytes IV length][IV][ciphertext]
     */
    fun encrypt(plaintext: String): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())

        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

        val buffer = ByteBuffer.allocate(4 + iv.size + ciphertext.size)
        buffer.putInt(iv.size)
        buffer.put(iv)
        buffer.put(ciphertext)

        Timber.d("Credentials encrypted (IV=%d bytes, cipher=%d bytes)", iv.size, ciphertext.size)
        return buffer.array()
    }

    /**
     * Déchiffre un blob chiffré par [encrypt].
     *
     * @param blob Le blob chiffré au format [4 bytes IV length][IV][ciphertext]
     * @return Le texte en clair
     */
    fun decrypt(blob: ByteArray): String {
        val buffer = ByteBuffer.wrap(blob)
        val ivLength = buffer.int
        val iv = ByteArray(ivLength)
        buffer.get(iv)
        val ciphertext = ByteArray(buffer.remaining())
        buffer.get(ciphertext)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_LENGTH, iv))

        val plaintext = cipher.doFinal(ciphertext)
        Timber.d("Credentials decrypted successfully")
        return String(plaintext, Charsets.UTF_8)
    }

    private fun getOrCreateKey(): SecretKey {
        keyStore.getKey(KEY_ALIAS, null)?.let { return it as SecretKey }

        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        keyGenerator.init(spec)
        Timber.d("New Keystore key generated: %s", KEY_ALIAS)
        return keyGenerator.generateKey()
    }
}
