package com.example.generator

import java.security.SecureRandom
import kotlin.math.log2
import kotlin.math.pow

enum class PasswordStrength(val label: String, val score: Float) {
    WEAK("Weak", 0.25f),
    FAIR("Fair", 0.50f),
    STRONG("Strong", 0.75f),
    ULTRA("Ultra Secure", 1.0f)
}

data class StrengthAnalysis(
    val strength: PasswordStrength,
    val score: Float,
    val entropyBits: Double,
    val estimatedCrackTime: String,
    val suggestion: String
)

object PasswordGenerator {

    private const val UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private const val LOWERCASE = "abcdefghijklmnopqrstuvwxyz"
    private const val NUMBERS = "0123456789"
    private const val SYMBOLS = "@#$%&*!_+-=?"
    private const val SIMILAR = "iI1lLo0O"

    private val secureRandom = SecureRandom()

    fun generate(
        length: Int = 16,
        includeUppercase: Boolean = true,
        includeLowercase: Boolean = true,
        includeNumbers: Boolean = true,
        includeSymbols: Boolean = true,
        excludeSimilar: Boolean = false
    ): String {
        var upperPool = UPPERCASE
        var lowerPool = LOWERCASE
        var numberPool = NUMBERS
        var symbolPool = SYMBOLS

        if (excludeSimilar) {
            upperPool = upperPool.filterNot { SIMILAR.contains(it) }
            lowerPool = lowerPool.filterNot { SIMILAR.contains(it) }
            numberPool = numberPool.filterNot { SIMILAR.contains(it) }
        }

        val poolList = mutableListOf<String>()
        if (includeUppercase && upperPool.isNotEmpty()) poolList.add(upperPool)
        if (includeLowercase && lowerPool.isNotEmpty()) poolList.add(lowerPool)
        if (includeNumbers && numberPool.isNotEmpty()) poolList.add(numberPool)
        if (includeSymbols && symbolPool.isNotEmpty()) poolList.add(symbolPool)

        if (poolList.isEmpty()) {
            return "P@ssw0rd123!" // Fallback safety
        }

        val fullPool = poolList.joinToString("")
        val passwordChars = mutableListOf<Char>()

        // Guarantee at least one character from each selected category
        for (pool in poolList) {
            if (passwordChars.size < length) {
                passwordChars.add(pool[secureRandom.nextInt(pool.length)])
            }
        }

        // Fill remaining length
        while (passwordChars.size < length) {
            passwordChars.add(fullPool[secureRandom.nextInt(fullPool.length)])
        }

        // Shuffle securely
        return passwordChars.shuffled(secureRandom).joinToString("")
    }

    fun analyzeStrength(password: String): StrengthAnalysis {
        if (password.isEmpty()) {
            return StrengthAnalysis(PasswordStrength.WEAK, 0f, 0.0, "Instant", "Enter or generate a password")
        }

        var charsetSize = 0
        if (password.any { UPPERCASE.contains(it) }) charsetSize += 26
        if (password.any { LOWERCASE.contains(it) }) charsetSize += 26
        if (password.any { NUMBERS.contains(it) }) charsetSize += 10
        if (password.any { SYMBOLS.contains(it) }) charsetSize += SYMBOLS.length

        if (charsetSize == 0) charsetSize = 26

        val entropyBits = password.length * log2(charsetSize.toDouble())

        val (strength, score) = when {
            entropyBits < 40 -> PasswordStrength.WEAK to (entropyBits / 40.0 * 0.3).toFloat()
            entropyBits < 60 -> PasswordStrength.FAIR to (0.3 + (entropyBits - 40) / 20.0 * 0.3).toFloat()
            entropyBits < 80 -> PasswordStrength.STRONG to (0.6 + (entropyBits - 60) / 20.0 * 0.25).toFloat()
            else -> PasswordStrength.ULTRA to 1.0f
        }

        val crackTime = when {
            entropyBits < 32 -> "Instant"
            entropyBits < 48 -> "A few minutes"
            entropyBits < 64 -> "Several days"
            entropyBits < 80 -> "Many years"
            else -> "Trillions of centuries"
        }

        val suggestion = when {
            password.length < 12 -> "Increase password length to 12+ characters."
            !password.any { SYMBOLS.contains(it) } -> "Add special symbols (@#$%) for extra entropy."
            !password.any { NUMBERS.contains(it) } -> "Include numbers to strengthen security."
            strength == PasswordStrength.ULTRA -> "Excellent! Highly secure offline password."
            else -> "Good password structure."
        }

        return StrengthAnalysis(strength, score.coerceIn(0.1f, 1.0f), entropyBits, crackTime, suggestion)
    }
}
