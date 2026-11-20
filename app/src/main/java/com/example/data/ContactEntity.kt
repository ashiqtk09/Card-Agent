package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "contacts",
    indices = [
        Index(value = ["normalizedMobile"]),
        Index(value = ["email"]),
        Index(value = ["name"]),
    ],
)
data class ContactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val mobileNumber: String,
    val normalizedMobile: String,
    val designation: String,
    val companyName: String,
    val companyWebsite: String,
    val address: String,
    val email: String,
    val cardImagePath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
) {
    val displayTitle: String
        get() = when {
            name.isNotBlank() -> name
            companyName.isNotBlank() -> companyName
            mobileNumber.isNotBlank() -> mobileNumber
            email.isNotBlank() -> email
            else -> "Unnamed Contact"
        }

    val roleAndCompanySubtitle: String
        get() = listOf(designation, companyName)
            .filter { it.isNotBlank() }
            .joinToString(", ")

    val initials: String
        get() {
            val source = name.ifBlank { companyName }.trim()
            if (source.isBlank()) return "?"
            val parts = source.split(Regex("\\s+")).filter { it.any(Char::isLetterOrDigit) }
            return when {
                parts.size >= 2 -> "${parts.first().first().uppercaseChar()}${parts.last().first().uppercaseChar()}"
                parts.size == 1 -> parts.first().take(2).uppercase()
                else -> "?"
            }
        }
}
