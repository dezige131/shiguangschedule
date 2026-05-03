package com.xingheyuzhuan.shiguangschedule.data.security

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

class BiometricAuthHelper(private val activity: FragmentActivity) {

    /**
     * 检查设备是否支持生物识别或是否已设置锁屏密码
     */
    fun canAuthenticate(): Int {
        val biometricManager = BiometricManager.from(activity)
        return biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
    }

    /**
     * 发起生物识别或图案/密码认证
     */
    fun authenticate(
        cryptoObject: BiometricPrompt.CryptoObject? = null,
        title: String = "验证身份",
        subtitle: String = "请验证指纹、PIN或锁屏图案",
        negativeButtonText: String = "取消",
        onResult: (AuthResult) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val biometricPrompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onResult(AuthResult.Error(errorCode, errString.toString()))
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                // 如果 result.cryptoObject 为空，说明认证虽然成功，但未解锁加密密钥
                onResult(AuthResult.Success(result.cryptoObject))
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onResult(AuthResult.Failed)
            }
        })

        val promptBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)

        if (cryptoObject != null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                // Android 11+ 支持 CryptoObject + 强生物识别 + 设备凭据（图案/PIN）
                promptBuilder.setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
                )
            } else {
                // API 29 及以下：使用 CryptoObject 时严禁启用 DEVICE_CREDENTIAL，必须设置取消按钮
                promptBuilder.setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                promptBuilder.setNegativeButtonText(negativeButtonText)
            }
        } else {
            // 不涉及密钥解锁时，允许所有方式
            promptBuilder.setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
        }

        val promptInfo = promptBuilder.build()
        if (cryptoObject != null) {
            biometricPrompt.authenticate(promptInfo, cryptoObject)
        } else {
            biometricPrompt.authenticate(promptInfo)
        }
    }

    sealed class AuthResult {
        data class Success(val cryptoObject: BiometricPrompt.CryptoObject?) : AuthResult()
        data class Error(val code: Int, val message: String) : AuthResult()
        object Failed : AuthResult()
    }
}
