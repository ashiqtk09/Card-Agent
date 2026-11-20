package com.example.ocr

object ContactValidator {

    private val EMAIL_REGEX = Regex(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$",
    )

    private val WEBSITE_REGEX = Regex(
        "^(https?://)?([A-Za-z0-9-]+\\.)+[A-Za-z]{2,}(/[^\\s]*)?$",
        RegexOption.IGNORE_CASE,
    )

    private val ALLOWED_PHONE_CHARS = Regex("^[+]?[0-9\\s()./-]+$")

    fun normalizePhoneForLookup(phone: String): String {
        val trimmed = phone.trim()
        if (trimmed.isEmpty()) return ""
        val digitsOnly = trimmed.filter(Char::isDigit)
        // Normalize international 00 prefix (e.g. 00968 -> 968) to match +968
        return if (digitsOnly.startsWith("00") && digitsOnly.length > 9) {
            digitsOnly.drop(2)
        } else {
            digitsOnly
        }
    }

    fun isValidPhone(phone: String): Boolean {
        val trimmed = phone.trim()
        if (trimmed.isEmpty()) return true
        if (!ALLOWED_PHONE_CHARS.matches(trimmed)) return false
        val digitCount = trimmed.count(Char::isDigit)
        return digitCount in 7..15
    }

    fun isValidEmail(email: String): Boolean {
        val trimmed = email.trim()
        if (trimmed.isEmpty()) return true
        return EMAIL_REGEX.matches(trimmed)
    }

    fun isValidWebsite(url: String): Boolean {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return true
        if (trimmed.contains("@")) return false
        return WEBSITE_REGEX.matches(trimmed)
    }

    fun formatWebsiteForBrowser(url: String): String {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return ""
        return if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            trimmed
        } else {
            "https://$trimmed"
        }
    }
}
