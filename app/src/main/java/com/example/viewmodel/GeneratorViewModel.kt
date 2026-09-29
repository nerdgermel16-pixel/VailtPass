package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.datastore.SecurityPreferences
import com.example.generator.PasswordGenerator
import com.example.generator.StrengthAnalysis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class GeneratorViewModel(application: Application) : AndroidViewModel(application) {

    private val securityPreferences = SecurityPreferences(application)

    private val _length = MutableStateFlow(16)
    val length: StateFlow<Int> = _length.asStateFlow()

    private val _includeUppercase = MutableStateFlow(true)
    val includeUppercase: StateFlow<Boolean> = _includeUppercase.asStateFlow()

    private val _includeLowercase = MutableStateFlow(true)
    val includeLowercase: StateFlow<Boolean> = _includeLowercase.asStateFlow()

    private val _includeNumbers = MutableStateFlow(true)
    val includeNumbers: StateFlow<Boolean> = _includeNumbers.asStateFlow()

    private val _includeSymbols = MutableStateFlow(true)
    val includeSymbols: StateFlow<Boolean> = _includeSymbols.asStateFlow()

    private val _excludeSimilar = MutableStateFlow(false)
    val excludeSimilar: StateFlow<Boolean> = _excludeSimilar.asStateFlow()

    private val _generatedPassword = MutableStateFlow("")
    val generatedPassword: StateFlow<String> = _generatedPassword.asStateFlow()

    private val _strengthAnalysis = MutableStateFlow(PasswordGenerator.analyzeStrength(""))
    val strengthAnalysis: StateFlow<StrengthAnalysis> = _strengthAnalysis.asStateFlow()

    private val _saveDialogVisible = MutableStateFlow(false)
    val saveDialogVisible: StateFlow<Boolean> = _saveDialogVisible.asStateFlow()

    init {
        viewModelScope.launch {
            _length.value = securityPreferences.generatorLength.first()
            _includeUppercase.value = securityPreferences.generatorUppercase.first()
            _includeLowercase.value = securityPreferences.generatorLowercase.first()
            _includeNumbers.value = securityPreferences.generatorNumbers.first()
            _includeSymbols.value = securityPreferences.generatorSymbols.first()
            _excludeSimilar.value = securityPreferences.generatorExcludeSimilar.first()
            generateNewPassword()
        }
    }

    fun setLength(newLength: Int) {
        _length.value = newLength.coerceIn(6, 32)
        generateNewPassword()
        savePreferences()
    }

    fun setIncludeUppercase(value: Boolean) {
        if (!value && !_includeLowercase.value && !_includeNumbers.value && !_includeSymbols.value) return
        _includeUppercase.value = value
        generateNewPassword()
        savePreferences()
    }

    fun setIncludeLowercase(value: Boolean) {
        if (!value && !_includeUppercase.value && !_includeNumbers.value && !_includeSymbols.value) return
        _includeLowercase.value = value
        generateNewPassword()
        savePreferences()
    }

    fun setIncludeNumbers(value: Boolean) {
        if (!value && !_includeUppercase.value && !_includeLowercase.value && !_includeSymbols.value) return
        _includeNumbers.value = value
        generateNewPassword()
        savePreferences()
    }

    fun setIncludeSymbols(value: Boolean) {
        if (!value && !_includeUppercase.value && !_includeLowercase.value && !_includeNumbers.value) return
        _includeSymbols.value = value
        generateNewPassword()
        savePreferences()
    }

    fun setExcludeSimilar(value: Boolean) {
        _excludeSimilar.value = value
        generateNewPassword()
        savePreferences()
    }

    fun generateNewPassword() {
        val pwd = PasswordGenerator.generate(
            length = _length.value,
            includeUppercase = _includeUppercase.value,
            includeLowercase = _includeLowercase.value,
            includeNumbers = _includeNumbers.value,
            includeSymbols = _includeSymbols.value,
            excludeSimilar = _excludeSimilar.value
        )
        _generatedPassword.value = pwd
        _strengthAnalysis.value = PasswordGenerator.analyzeStrength(pwd)
    }

    fun openSaveDialog() {
        _saveDialogVisible.value = true
    }

    fun closeSaveDialog() {
        _saveDialogVisible.value = false
    }

    private fun savePreferences() {
        viewModelScope.launch {
            securityPreferences.saveGeneratorOptions(
                length = _length.value,
                uppercase = _includeUppercase.value,
                lowercase = _includeLowercase.value,
                numbers = _includeNumbers.value,
                symbols = _includeSymbols.value,
                excludeSimilar = _excludeSimilar.value
            )
        }
    }
}
