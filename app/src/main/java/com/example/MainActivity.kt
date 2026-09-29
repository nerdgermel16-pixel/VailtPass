package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import com.example.security.BiometricAuthManager
import com.example.ui.VaultApp
import com.example.ui.theme.VaultPassTheme
import com.example.viewmodel.GeneratorViewModel
import com.example.viewmodel.VaultViewModel

class MainActivity : FragmentActivity() {

    private val vaultViewModel: VaultViewModel by viewModels()
    private val generatorViewModel: GeneratorViewModel by viewModels()
    private lateinit var biometricAuthManager: BiometricAuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        biometricAuthManager = BiometricAuthManager(this)

        setContent {
            VaultPassTheme {
                VaultApp(
                    vaultViewModel = vaultViewModel,
                    generatorViewModel = generatorViewModel,
                    onTriggerBiometricAuth = { triggerBiometrics() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Auto-prompt biometrics if locked and biometrics is enabled
        if (!vaultViewModel.isUnlocked.value && vaultViewModel.hasMasterPin.value && vaultViewModel.biometricsEnabled.value) {
            triggerBiometrics()
        }
    }

    private fun triggerBiometrics() {
        if (!biometricAuthManager.isBiometricAvailable()) {
            Toast.makeText(this, "Biometrics not available on this device. Use PIN.", Toast.LENGTH_SHORT).show()
            return
        }

        biometricAuthManager.authenticate(
            title = "Unlock VaultPass",
            subtitle = "Verify identity to access your encrypted offline vault",
            onSuccess = {
                vaultViewModel.unlockWithBiometrics()
            },
            onError = { err ->
                Toast.makeText(this, err, Toast.LENGTH_SHORT).show()
            },
            onUsePin = {
                // User chose PIN fallback
            }
        )
    }
}
