package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.datastore.SecurityPreferences
import com.example.data.db.VaultDatabase
import com.example.data.repository.DecryptedVaultEntry
import com.example.data.repository.VaultRepository
import com.example.security.CryptoHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class VaultViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: VaultRepository
    private val securityPreferences: SecurityPreferences

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    private val _hasMasterPin = MutableStateFlow(false)
    val hasMasterPin: StateFlow<Boolean> = _hasMasterPin.asStateFlow()

    private val _biometricsEnabled = MutableStateFlow(true)
    val biometricsEnabled: StateFlow<Boolean> = _biometricsEnabled.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    val filteredEntries: StateFlow<List<DecryptedVaultEntry>>

    init {
        val database = VaultDatabase.getDatabase(application)
        repository = VaultRepository(database.vaultDao())
        securityPreferences = SecurityPreferences(application)

        viewModelScope.launch {
            securityPreferences.masterPinHash.collect { pinHash ->
                _hasMasterPin.value = !pinHash.isNullOrEmpty()
                // Auto-unlock if no PIN has been set up yet
                if (pinHash.isNullOrEmpty()) {
                    _isUnlocked.value = true
                }
            }
        }

        viewModelScope.launch {
            securityPreferences.biometricsEnabled.collect { enabled ->
                _biometricsEnabled.value = enabled
            }
        }

        filteredEntries = combine(
            repository.allEntries,
            _searchQuery,
            _selectedCategory
        ) { entries, query, category ->
            entries.filter { entry ->
                val matchesQuery = query.isBlank() ||
                        entry.serviceName.contains(query, ignoreCase = true) ||
                        entry.username.contains(query, ignoreCase = true) ||
                        entry.notesPlain.contains(query, ignoreCase = true)

                val matchesCategory = category == "All" || entry.category.equals(category, ignoreCase = true)

                matchesQuery && matchesCategory
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(category: String) {
        _selectedCategory.value = category
    }

    fun unlockWithBiometrics() {
        _isUnlocked.value = true
    }

    fun lockVault() {
        if (_hasMasterPin.value) {
            _isUnlocked.value = false
        }
    }

    fun verifyPin(pin: String): Boolean {
        var isValid = false
        viewModelScope.launch {
            val storedHash = securityPreferences.masterPinHash.first()
            if (storedHash == null || storedHash == CryptoHelper.hashPin(pin)) {
                _isUnlocked.value = true
                isValid = true
            }
        }
        return isValid
    }

    fun setupMasterPin(pin: String) {
        if (pin.length in 4..6) {
            viewModelScope.launch {
                securityPreferences.saveMasterPinHash(CryptoHelper.hashPin(pin))
                _hasMasterPin.value = true
                _isUnlocked.value = true
                showToast("Master Security PIN created!")
            }
        }
    }

    fun setBiometricsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            securityPreferences.setBiometricsEnabled(enabled)
            _biometricsEnabled.value = enabled
            showToast(if (enabled) "Biometric unlock enabled" else "Biometric unlock disabled")
        }
    }

    fun saveVaultEntry(
        id: Long = 0,
        serviceName: String,
        username: String,
        passwordPlain: String,
        notesPlain: String,
        category: String = "Logins"
    ) {
        if (serviceName.isBlank() || passwordPlain.isBlank()) {
            showToast("Service name and password cannot be empty")
            return
        }

        viewModelScope.launch {
            repository.saveEntry(
                id = id,
                serviceName = serviceName,
                username = username,
                passwordPlain = passwordPlain,
                notesPlain = notesPlain,
                category = category
            )
            showToast(if (id == 0L) "Entry saved to vault" else "Vault entry updated")
        }
    }

    fun toggleFavorite(entry: DecryptedVaultEntry) {
        viewModelScope.launch {
            repository.toggleFavorite(entry)
        }
    }

    fun deleteEntry(id: Long) {
        viewModelScope.launch {
            repository.deleteEntryById(id)
            showToast("Entry removed from vault")
        }
    }

    fun showToast(message: String) {
        _toastMessage.value = message
    }

    fun clearToast() {
        _toastMessage.value = null
    }
}
