package com.android.argosapp.util

import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Cifra valores antes de persistir em SharedPreferences usando uma chave do Android Keystore.
 */
class SharedPreferencesCipher(
    private val keyAlias: String,
) {

    private companion object {
        const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
        const val ENCRYPTED_VALUE_PREFIX = "v1:"
        const val GCM_TAG_LENGTH_BITS = 128
    }

    fun isEncrypted(value: String): Boolean {
        return value.startsWith(ENCRYPTED_VALUE_PREFIX)
    }

    fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        }
        val encryptedValue = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        return buildString {
            append(ENCRYPTED_VALUE_PREFIX)
            append(Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            append(":")
            append(Base64.encodeToString(encryptedValue, Base64.NO_WRAP))
        }
    }

    fun decrypt(encryptedValue: String): String? {
        val parts = encryptedValue.removePrefix(ENCRYPTED_VALUE_PREFIX).split(":", limit = 2)
        if (parts.size != 2) return null

        return runCatching {
            val initializationVector = Base64.decode(parts[0], Base64.NO_WRAP)
            val cipherText = Base64.decode(parts[1], Base64.NO_WRAP)
            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION).apply {
                init(
                    Cipher.DECRYPT_MODE,
                    getOrCreateKey(),
                    GCMParameterSpec(GCM_TAG_LENGTH_BITS, initializationVector),
                )
            }

            cipher.doFinal(cipherText).toString(Charsets.UTF_8)
        }.getOrNull()
    }

    /**
     * Lê, decifra e migra automaticamente valores persistidos antes da criptografia.
     */
    fun readSecureValue(
        preferences: SharedPreferences,
        key: String,
    ): String? {
        val persistedValue = preferences.getString(key, null) ?: return null

        if (isEncrypted(persistedValue)) {
            return decrypt(persistedValue)
        }

        return runCatching {
            preferences.edit { putString(key, encrypt(persistedValue)) }
            persistedValue
        }.getOrNull()
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        (keyStore.getKey(keyAlias, null) as? SecretKey)?.let { return it }

        return KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            KEYSTORE_PROVIDER,
        )
            .apply {
                init(
                    KeyGenParameterSpec.Builder(
                        keyAlias,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                    )
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .build(),
                )
            }
            .generateKey()
    }
}
