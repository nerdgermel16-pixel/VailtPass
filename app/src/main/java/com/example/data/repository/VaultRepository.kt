package com.example.data.repository

import com.example.data.db.VaultDao
import com.example.data.db.VaultEntry
import com.example.security.CryptoHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Model exposed to the UI with decrypted plain-text passwords and notes.
 */
data class DecryptedVaultEntry(
    val id: Long = 0,
    val serviceName: String,
    val username: String,
    val passwordPlain: String,
    val notesPlain: String,
    val category: String,
    val isFavorite: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

class VaultRepository(private val vaultDao: VaultDao) {

    val allEntries: Flow<List<DecryptedVaultEntry>> = vaultDao.getAllEntries().map { entries ->
        entries.map { it.toDecrypted() }
    }

    suspend fun saveEntry(
        id: Long = 0,
        serviceName: String,
        username: String,
        passwordPlain: String,
        notesPlain: String,
        category: String = "Logins",
        isFavorite: Boolean = false
    ): Long {
        val encryptedPassword = CryptoHelper.encrypt(passwordPlain)
        val encryptedNotes = CryptoHelper.encrypt(notesPlain)

        val entry = VaultEntry(
            id = id,
            serviceName = serviceName.trim(),
            username = username.trim(),
            encryptedPassword = encryptedPassword,
            encryptedNotes = encryptedNotes,
            category = category,
            isFavorite = isFavorite,
            updatedAt = System.currentTimeMillis()
        )

        return if (id == 0L) {
            vaultDao.insertEntry(entry)
        } else {
            vaultDao.updateEntry(entry)
            id
        }
    }

    suspend fun toggleFavorite(entry: DecryptedVaultEntry) {
        val updated = VaultEntry(
            id = entry.id,
            serviceName = entry.serviceName,
            username = entry.username,
            encryptedPassword = CryptoHelper.encrypt(entry.passwordPlain),
            encryptedNotes = CryptoHelper.encrypt(entry.notesPlain),
            category = entry.category,
            isFavorite = !entry.isFavorite,
            createdAt = entry.createdAt,
            updatedAt = System.currentTimeMillis()
        )
        vaultDao.updateEntry(updated)
    }

    suspend fun deleteEntryById(id: Long) {
        vaultDao.deleteEntryById(id)
    }

    suspend fun deleteAll() {
        vaultDao.deleteAll()
    }

    private fun VaultEntry.toDecrypted(): DecryptedVaultEntry {
        return DecryptedVaultEntry(
            id = id,
            serviceName = serviceName,
            username = username,
            passwordPlain = CryptoHelper.decrypt(encryptedPassword),
            notesPlain = CryptoHelper.decrypt(encryptedNotes),
            category = category,
            isFavorite = isFavorite,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}
