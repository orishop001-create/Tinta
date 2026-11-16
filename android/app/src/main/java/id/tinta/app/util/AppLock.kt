package id.tinta.app.util

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/** Verifikasi sidik jari atau PIN perangkat untuk membuka aplikasi. */
object AppLock {

    fun isAvailable(context: Context): Boolean {
        val manager = BiometricManager.from(context)
        val strong = BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        return manager.canAuthenticate(strong) == BiometricManager.BIOMETRIC_SUCCESS ||
            manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }

    fun prompt(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onError(errString.toString())
                }
            },
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Buka Tinta")
            .setSubtitle("Gunakan sidik jari atau PIN perangkat")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL,
            )
            .build()
        prompt.authenticate(info)
    }
}
