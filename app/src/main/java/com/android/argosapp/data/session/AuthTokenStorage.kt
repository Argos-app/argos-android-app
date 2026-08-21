package com.android.argosapp.data.session

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Armazena o ID token criptografado quando o usuário opta por permanecer conectado. */
interface AuthTokenStorage {
    fun read(): String?
    fun save(token: String)
    fun clear()
}

class SharedPreferencesAuthTokenStorage(
    context: Context,
) : AuthTokenStorage {

    private companion object {
        const val ANDROID_KEY_STORE = "AndroidKeyStore"
        const val KEY_ALIAS = "argos_auth_token_key"
        const val PREFERENCES_NAME = "argos_auth_session"
        const val TOKEN_KEY = "firebase_id_token"
        const val GCM_TAG_LENGTH = 128
    }

    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply {
        load(null)
    }

    override fun read(): String? {
        val encryptedToken = preferences.getString(TOKEN_KEY, null) ?: return null

        return runCatching {
            decrypt(encryptedToken)
        }.getOrElse {
            clear()
            null
        }
    }

    override fun save(token: String) {
        preferences.edit().putString(TOKEN_KEY, encrypt(token)).apply()
    }

    override fun clear() {
        preferences.edit().remove(TOKEN_KEY).apply()
    }

    private fun encrypt(token: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())

        val iv = cipher.iv
        val encryptedToken = cipher.doFinal(token.toByteArray(StandardCharsets.UTF_8))
        val payload = ByteBuffer
            .allocate(Int.SIZE_BYTES + iv.size + encryptedToken.size)
            .putInt(iv.size)
            .put(iv)
            .put(encryptedToken)
            .array()

        return Base64.encodeToString(payload, Base64.NO_WRAP)
    }

    private fun decrypt(encryptedValue: String): String {
        val payload = ByteBuffer.wrap(Base64.decode(encryptedValue, Base64.NO_WRAP))
        val ivSize = payload.int
        require(ivSize > 0 && ivSize <= payload.remaining())

        val iv = ByteArray(ivSize)
        payload.get(iv)

        val encryptedToken = ByteArray(payload.remaining())
        payload.get(encryptedToken)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateKey(),
            GCMParameterSpec(GCM_TAG_LENGTH, iv),
        )

        return String(cipher.doFinal(encryptedToken), StandardCharsets.UTF_8)
    }

    private fun getOrCreateKey(): SecretKey {
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEY_STORE,
        )
        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build(),
        )
        return keyGenerator.generateKey()
    }
}
