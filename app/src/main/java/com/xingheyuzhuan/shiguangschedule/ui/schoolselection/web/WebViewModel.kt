package com.xingheyuzhuan.shiguangschedule.ui.schoolselection.web

import androidx.lifecycle.ViewModel
import com.xingheyuzhuan.shiguangschedule.data.repository.AppSettingsRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.CourseConversionRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.TimeSlotRepository
import com.xingheyuzhuan.shiguangschedule.data.security.SecureCredentialManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class WebViewModel @Inject constructor(
    val courseConversionRepository: CourseConversionRepository,
    val timeSlotRepository: TimeSlotRepository,
    val appSettingsRepository: AppSettingsRepository
) : ViewModel() {
    val secureCredentialManager = SecureCredentialManager()
    val appSettings = appSettingsRepository.getAppSettings()

    fun getCipherForEncryption(): javax.crypto.Cipher {
        return secureCredentialManager.getInitializedCipherForEncryption()
    }

    fun getCipherForDecryption(iv: String): javax.crypto.Cipher {
        val ivBytes = android.util.Base64.decode(iv, android.util.Base64.NO_WRAP)
        return secureCredentialManager.getInitializedCipherForDecryption(ivBytes)
    }

    suspend fun saveCredentials(username: String, password: String, cipher: javax.crypto.Cipher) {
        val encrypted = secureCredentialManager.encrypt(cipher, username, password)
        appSettingsRepository.saveCredentials(encrypted.data, encrypted.iv)
    }

    fun decryptCredentials(cipher: javax.crypto.Cipher, encryptedData: String): Pair<String, String>? {
        return secureCredentialManager.decrypt(cipher, encryptedData)
    }

    suspend fun clearCredentials() {
        appSettingsRepository.clearCredentials()
    }
}
