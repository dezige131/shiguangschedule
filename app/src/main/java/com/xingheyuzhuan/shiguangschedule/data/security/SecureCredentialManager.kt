package com.xingheyuzhuan.shiguangschedule.data.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SecureCredentialManager {

    companion object {
        private const val KEY_ALIAS = "EducationalSystemCredentialsKey"
        private const val ANDROID_KEY_STORE = "AndroidKeyStore"
        private const val AES_MODE = "AES/GCM/NoPadding"
        private const val DELIMITER = "|SHIGUANG|"
    }

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply {
        load(null)
    }

    /**
     * 生成需要身份验证的 AES 密钥（支持生物识别和图案/PIN/密码）
     */
    private fun generateSecretKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
        val builder = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            // Android 11+：显式允许强生物识别或设备凭据（图案、PIN、密码）
            builder.setUserAuthenticationParameters(
                0, // 0 表示每次使用密钥都必须认证
                KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL
            )
            // 为了更好的兼容图案/密码切换，建议在 Android 11+ 上关闭此项，或根据需求开启
            builder.setInvalidatedByBiometricEnrollment(false)
        } else {
            // Android 10 及以下：旧 API，通常只支持生物识别解锁 CryptoObject
            @Suppress("DEPRECATION")
            builder.setUserAuthenticationRequired(true)
            builder.setInvalidatedByBiometricEnrollment(true)
        }

        keyGenerator.init(builder.build())
        return keyGenerator.generateKey()
    }

    private fun getSecretKey(): SecretKey {
        val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        return entry?.secretKey ?: generateSecretKey()
    }

    /**
     * 初始化用于加密的 Cipher
     */
    fun getInitializedCipherForEncryption(): Cipher {
        val cipher = Cipher.getInstance(AES_MODE)
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
        return cipher
    }

    /**
     * 初始化用于解密的 Cipher
     */
    fun getInitializedCipherForDecryption(iv: ByteArray): Cipher {
        val cipher = Cipher.getInstance(AES_MODE)
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)
        return cipher
    }

    /**
     * 加密用户名和密码
     */
    fun encrypt(cipher: Cipher, username: String, password: String): EncryptedData {
        val combined = "$username$DELIMITER$password"
        val encryptedBytes = cipher.doFinal(combined.toByteArray(Charsets.UTF_8))
        return EncryptedData(
            iv = Base64.encodeToString(cipher.iv, Base64.NO_WRAP),
            data = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
        )
    }

    /**
     * 解密并返回用户名和密码
     */
    fun decrypt(cipher: Cipher, encryptedDataBase64: String): Pair<String, String>? {
        return try {
            val encryptedBytes = Base64.decode(encryptedDataBase64, Base64.NO_WRAP)
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            val combined = String(decryptedBytes, Charsets.UTF_8)
            val parts = combined.split(DELIMITER)
            if (parts.size == 2) {
                parts[0] to parts[1]
            } else null
        } catch (e: Exception) {
            null
        }
    }

    fun isKeyInvalidated(e: Exception): Boolean {
        return e is KeyPermanentlyInvalidatedException
    }

    fun deleteKey() {
        keyStore.deleteEntry(KEY_ALIAS)
    }

    data class EncryptedData(val iv: String, val data: String)
}
