package com.example.natureaura.connectivity

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import androidx.core.content.ContextCompat

/**
 * Integrates the phone's hardware fingerprint sensor honestly via Android BiometricPrompt.
 * Because Android TEE isolates raw fingerprint imagery/pulse data, this acts as a tactile
 * biometric touch-hold ritual that verifies sensor presence and arms real Phone Sensor / Camera PPG scanning.
 */
object FingerprintTriggerHelper {

    fun isFingerprintAvailable(context: Context): Boolean {
        val pm = context.packageManager
        if (!pm.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)) return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bm = context.getSystemService(BiometricManager::class.java) ?: return false
            bm.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.BIOMETRIC_WEAK,
            ) == BiometricManager.BIOMETRIC_SUCCESS
        } else {
            true
        }
    }

    fun triggerFingerprintScan(
        context: Context,
        onVerified: () -> Unit,
        onFallbackDirectScan: (String) -> Unit,
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            onFallbackDirectScan("Android ${Build.VERSION.SDK_INT} lacks BiometricPrompt — starting direct Phone Sensor scan.")
            return
        }

        val pm = context.packageManager
        if (!pm.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)) {
            onFallbackDirectScan("No fingerprint scanner detected — starting direct Phone Sensor scan.")
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bm = context.getSystemService(BiometricManager::class.java)
            val status = bm?.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.BIOMETRIC_WEAK,
            )
            if (status != BiometricManager.BIOMETRIC_SUCCESS) {
                onFallbackDirectScan("No fingerprint enrolled in Settings — starting direct Phone Sensor scan.")
                return
            }
        }

        runCatching {
            val executor = ContextCompat.getMainExecutor(context)
            val cancellationSignal = CancellationSignal()
            var negativeClicked = false
            val prompt = BiometricPrompt.Builder(context)
                .setTitle("Touch Fingerprint Scanner")
                .setSubtitle("Biometric touch arms Camera rPPG & Phone Sensors")
                .setDescription(
                    "Android isolates raw fingerprint images in TEE security, so touching verifies presence and launches live Camera/Sensor capture for BPM & warmth.",
                )
                .setNegativeButton("Cancel", executor) { _, _ ->
                    negativeClicked = true
                }
                .build()

            prompt.authenticate(
                cancellationSignal,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                        onVerified()
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                        if (!negativeClicked &&
                            errorCode != BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED &&
                            errorCode != BiometricPrompt.BIOMETRIC_ERROR_CANCELED
                        ) {
                            onFallbackDirectScan("Fingerprint unavailable ($errString) — starting Phone Sensor scan.")
                        }
                    }
                },
            )
        }.onFailure {
            onFallbackDirectScan("Fingerprint prompt unavailable — starting Phone Sensor scan.")
        }
    }
}
