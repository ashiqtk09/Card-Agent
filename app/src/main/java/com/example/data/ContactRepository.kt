package com.example.data

import android.content.Context
import com.example.ocr.ContactValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

enum class SearchFilter {
    ALL,
    NAME,
    COMPANY,
    DESIGNATION,
}

enum class SortOrder {
    RECENT,
    NAME,
    COMPANY,
}

enum class DuplicateMatchReason {
    MOBILE,
    EMAIL,
    MOBILE_AND_EMAIL,
}

data class DuplicateMatch(
    val existingContact: ContactEntity,
    val reason: DuplicateMatchReason,
)

class ContactRepository(
    private val contactDao: ContactDao,
    private val appContext: Context,
) {
    val totalCount: Flow<Int> = contactDao.observeTotalCount()

    fun observeContacts(
        query: String,
        filter: SearchFilter,
        sortOrder: SortOrder,
    ): Flow<List<ContactEntity>> {
        val trimmed = query.trim()
        val baseFlow = if (trimmed.isEmpty()) {
            when (sortOrder) {
                SortOrder.RECENT -> contactDao.observeAllByRecent()
                SortOrder.NAME -> contactDao.observeAllByName()
                SortOrder.COMPANY -> contactDao.observeAllByCompany()
            }
        } else {
            when (filter) {
                SearchFilter.ALL -> contactDao.searchAllFields(trimmed)
                SearchFilter.NAME -> contactDao.searchByName(trimmed)
                SearchFilter.COMPANY -> contactDao.searchByCompany(trimmed)
                SearchFilter.DESIGNATION -> contactDao.searchByDesignation(trimmed)
            }
        }

        return if (trimmed.isNotEmpty()) {
            baseFlow.map { list ->
                when (sortOrder) {
                    SortOrder.RECENT -> list.sortedByDescending { it.createdAt }
                    SortOrder.NAME -> list.sortedWith(
                        compareBy<ContactEntity> { it.name.lowercase() }
                            .thenBy { it.companyName.lowercase() },
                    )
                    SortOrder.COMPANY -> list.sortedWith(
                        compareBy<ContactEntity> { it.companyName.lowercase() }
                            .thenBy { it.name.lowercase() },
                    )
                }
            }
        } else {
            baseFlow
        }
    }

    fun observeContactById(id: Long): Flow<ContactEntity?> = contactDao.observeContactById(id)

    suspend fun getContactById(id: Long): ContactEntity? = contactDao.getContactById(id)

    suspend fun findDuplicate(
        mobileNumber: String,
        email: String,
        excludeId: Long = -1L,
    ): DuplicateMatch? {
        val normalizedPhone = ContactValidator.normalizePhoneForLookup(mobileNumber)
        val trimmedEmail = email.trim()

        val mobileMatch = if (normalizedPhone.length >= 7) {
            contactDao.findByNormalizedMobile(normalizedPhone, excludeId)
        } else {
            null
        }

        val emailMatch = if (trimmedEmail.isNotBlank()) {
            contactDao.findByEmail(trimmedEmail, excludeId)
        } else {
            null
        }

        return when {
            mobileMatch != null && emailMatch != null && mobileMatch.id == emailMatch.id -> {
                DuplicateMatch(mobileMatch, DuplicateMatchReason.MOBILE_AND_EMAIL)
            }
            mobileMatch != null -> DuplicateMatch(mobileMatch, DuplicateMatchReason.MOBILE)
            emailMatch != null -> DuplicateMatch(emailMatch, DuplicateMatchReason.EMAIL)
            else -> null
        }
    }

    suspend fun insertContact(contact: ContactEntity): Long {
        val persistedPath = persistCardImageIfTemporary(contact.cardImagePath)
        val now = System.currentTimeMillis()
        val toInsert = contact.copy(
            id = 0L,
            normalizedMobile = ContactValidator.normalizePhoneForLookup(contact.mobileNumber),
            cardImagePath = persistedPath,
            createdAt = now,
            updatedAt = now,
        )
        return contactDao.insert(toInsert)
    }

    suspend fun updateContact(contact: ContactEntity) {
        val existing = contactDao.getContactById(contact.id)
        val persistedPath = persistCardImageIfTemporary(contact.cardImagePath)
        val updated = contact.copy(
            normalizedMobile = ContactValidator.normalizePhoneForLookup(contact.mobileNumber),
            cardImagePath = persistedPath,
            createdAt = existing?.createdAt ?: contact.createdAt,
            updatedAt = System.currentTimeMillis(),
        )
        contactDao.update(updated)

        if (existing?.cardImagePath != null && existing.cardImagePath != persistedPath) {
            deletePersistedImage(existing.cardImagePath)
        }
    }

    suspend fun deleteContact(id: Long) {
        val existing = contactDao.getContactById(id)
        contactDao.deleteById(id)
        existing?.cardImagePath?.let { deletePersistedImage(it) }
    }

    private suspend fun persistCardImageIfTemporary(imagePath: String?): String? =
        withContext(Dispatchers.IO) {
            if (imagePath.isNullOrBlank()) return@withContext null
            val source = File(imagePath)
            if (!source.exists()) return@withContext null

            val permanentDir = File(appContext.filesDir, "saved_cards").apply { mkdirs() }
            if (source.parentFile?.canonicalPath == permanentDir.canonicalPath) {
                return@withContext source.absolutePath
            }

            val destination = File(permanentDir, "card_${System.currentTimeMillis()}.jpg")
            runCatching {
                source.copyTo(destination, overwrite = true)
                destination.absolutePath
            }.getOrNull()
        }

    private suspend fun deletePersistedImage(path: String) = withContext(Dispatchers.IO) {
        val permanentDir = File(appContext.filesDir, "saved_cards")
        val target = File(path)
        if (target.exists() && target.parentFile?.canonicalPath == permanentDir.canonicalPath) {
            target.delete()
        }
    }
}
