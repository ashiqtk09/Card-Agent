package com.example.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BusinessCardParserTest {

    @Test
    fun `parses full business card preserving country code and all 7 fields`() {
        val lines = listOf(
            RecognizedLine("Maya Patel", heightPx = 64, topPx = 100, confidence = 0.96f),
            RecognizedLine("Strategy Director", heightPx = 38, topPx = 180, confidence = 0.94f),
            RecognizedLine("Meridian Advisory", heightPx = 42, topPx = 250, confidence = 0.95f),
            RecognizedLine("M: +968 9000 1234", heightPx = 32, topPx = 320, confidence = 0.93f),
            RecognizedLine("F: +968 2400 9999", heightPx = 32, topPx = 360, confidence = 0.92f),
            RecognizedLine("maya@meridian.com", heightPx = 32, topPx = 400, confidence = 0.97f),
            RecognizedLine("www.meridian.com", heightPx = 32, topPx = 440, confidence = 0.95f),
            RecognizedLine("Suite 402, Al Khuwair Tower", heightPx = 30, topPx = 480, confidence = 0.91f),
            RecognizedLine("Muscat 112, Oman", heightPx = 30, topPx = 520, confidence = 0.90f),
        )

        val result = BusinessCardParser.parse(lines)

        assertFalse(result.ocrFailed)
        assertEquals("Maya Patel", result.name)
        assertEquals("Strategy Director", result.designation)
        assertEquals("Meridian Advisory", result.companyName)
        assertEquals("+968 9000 1234", result.mobileNumber)
        assertEquals("maya@meridian.com", result.email)
        assertEquals("www.meridian.com", result.companyWebsite)
        assertEquals("Suite 402, Al Khuwair Tower, Muscat 112, Oman", result.address)
        assertTrue(result.uncertainFields.isEmpty())
    }

    @Test
    fun `leaves missing fields blank and flags low confidence fields for review`() {
        val lines = listOf(
            RecognizedLine("Rohan Al Nuaimi", heightPx = 58, topPx = 90, confidence = 0.92f),
            RecognizedLine("Partner", heightPx = 36, topPx = 160, confidence = 0.62f),
            RecognizedLine("Horizon Capital", heightPx = 40, topPx = 220, confidence = 0.95f),
        )

        val result = BusinessCardParser.parse(lines)

        assertFalse(result.ocrFailed)
        assertEquals("Rohan Al Nuaimi", result.name)
        assertEquals("Partner", result.designation)
        assertEquals("Horizon Capital", result.companyName)
        assertEquals("", result.mobileNumber)
        assertEquals("", result.email)
        assertEquals("", result.companyWebsite)
        assertEquals("", result.address)
        assertTrue(CardField.DESIGNATION in result.uncertainFields)
    }

    @Test
    fun `returns ocrFailed when no lines are recognized`() {
        val result = BusinessCardParser.parse(emptyList())
        assertTrue(result.ocrFailed)
    }

    @Test
    fun `validates phone email and website formats`() {
        assertTrue(ContactValidator.isValidPhone("+968 9000 1234"))
        assertTrue(ContactValidator.isValidPhone(""))
        assertFalse(ContactValidator.isValidPhone("123"))
        assertFalse(ContactValidator.isValidPhone("abc-1234567"))

        assertTrue(ContactValidator.isValidEmail("maya@meridian.com"))
        assertTrue(ContactValidator.isValidEmail(""))
        assertFalse(ContactValidator.isValidEmail("not-an-email"))

        assertTrue(ContactValidator.isValidWebsite("https://meridian.com"))
        assertTrue(ContactValidator.isValidWebsite("meridian.com"))
        assertTrue(ContactValidator.isValidWebsite(""))
        assertFalse(ContactValidator.isValidWebsite("maya@meridian.com"))
        assertFalse(ContactValidator.isValidWebsite("justword"))
    }
}
