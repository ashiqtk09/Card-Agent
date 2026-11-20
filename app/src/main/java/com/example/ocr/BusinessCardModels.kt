package com.example.ocr

enum class CardField {
    NAME,
    MOBILE,
    DESIGNATION,
    COMPANY,
    WEBSITE,
    ADDRESS,
    EMAIL,
}

data class RecognizedLine(
    val text: String,
    val heightPx: Int = 0,
    val topPx: Int = 0,
    val leftPx: Int = 0,
    val confidence: Float? = null,
)

data class ExtractedCardData(
    val name: String = "",
    val mobileNumber: String = "",
    val designation: String = "",
    val companyName: String = "",
    val companyWebsite: String = "",
    val address: String = "",
    val email: String = "",
    val uncertainFields: Set<CardField> = emptySet(),
    val rawText: String = "",
    val ocrFailed: Boolean = false,
    val failureReason: String? = null,
    val imagePath: String? = null,
) {
    val hasAnyExtractedField: Boolean
        get() = listOf(
            name,
            mobileNumber,
            designation,
            companyName,
            companyWebsite,
            address,
            email,
        ).any { it.isNotBlank() }
}
