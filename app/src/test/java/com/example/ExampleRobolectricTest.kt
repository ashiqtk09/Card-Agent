package com.example

import android.content.Context
import android.provider.ContactsContract
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.contacts.ContactExportData
import com.example.contacts.PhoneContactsExporter
import com.example.data.CardScanDatabase
import com.example.data.ContactEntity
import com.example.data.ContactRepository
import com.example.data.DuplicateMatchReason
import com.example.data.SearchFilter
import com.example.data.SortOrder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var database: CardScanDatabase
    private lateinit var repository: ContactRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, CardScanDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = ContactRepository(database.contactDao(), context)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `app name matches CardScan`() {
        assertEquals("CardScan", context.getString(R.string.app_name))
    }

    @Test
    fun `insert search filter and detect duplicates in Room`() = runTest {
        val id = repository.insertContact(
            ContactEntity(
                name = "Maya Patel",
                mobileNumber = "+968 9000 1234",
                normalizedMobile = "",
                designation = "Strategy Director",
                companyName = "Meridian Advisory",
                companyWebsite = "https://meridian.com",
                address = "Suite 400, Muscat Business Park",
                email = "maya@example.com",
            ),
        )
        assertTrue(id > 0L)

        // Case-insensitive partial search across all fields
        val byWebsite = repository.observeContacts("MERIDIAN.COM", SearchFilter.ALL, SortOrder.RECENT).first()
        assertEquals(1, byWebsite.size)
        assertEquals("Maya Patel", byWebsite.first().name)

        // Filter by Designation only
        val byDesignation = repository.observeContacts("strategy", SearchFilter.DESIGNATION, SortOrder.RECENT).first()
        assertEquals(1, byDesignation.size)

        val mismatchCompanyFilter = repository.observeContacts("strategy", SearchFilter.COMPANY, SortOrder.RECENT).first()
        assertTrue(mismatchCompanyFilter.isEmpty())

        // Duplicate detection by reformatted mobile number
        val phoneDup = repository.findDuplicate(mobileNumber = "+968-9000-1234", email = "")
        assertNotNull(phoneDup)
        assertEquals(DuplicateMatchReason.MOBILE, phoneDup?.reason)

        // Duplicate detection by case-insensitive email
        val emailDup = repository.findDuplicate(mobileNumber = "", email = "MAYA@EXAMPLE.COM")
        assertNotNull(emailDup)
        assertEquals(DuplicateMatchReason.EMAIL, emailDup?.reason)

        // Excluding self during edit should not flag duplicate
        val selfCheck = repository.findDuplicate(
            mobileNumber = "+968 9000 1234",
            email = "maya@example.com",
            excludeId = id,
        )
        assertNull(selfCheck)

        // Delete removes record cleanly
        repository.deleteContact(id)
        val afterDelete = repository.observeContacts("", SearchFilter.ALL, SortOrder.RECENT).first()
        assertTrue(afterDelete.isEmpty())
    }

    @Test
    fun `phone contacts export intent prefills all fields`() {
        val intent = PhoneContactsExporter.buildInsertContactIntent(
            ContactExportData(
                name = "Maya Patel",
                mobileNumber = "+968 9000 1234",
                designation = "Strategy Director",
                companyName = "Meridian Advisory",
                companyWebsite = "meridian.com",
                address = "120 Market St, San Francisco",
                email = "maya@example.com",
            ),
        )
        assertEquals(ContactsContract.Intents.Insert.ACTION, intent.action)
        assertEquals("Maya Patel", intent.getStringExtra(ContactsContract.Intents.Insert.NAME))
        assertEquals("+968 9000 1234", intent.getStringExtra(ContactsContract.Intents.Insert.PHONE))
        assertEquals("Strategy Director", intent.getStringExtra(ContactsContract.Intents.Insert.JOB_TITLE))
        assertEquals("Meridian Advisory", intent.getStringExtra(ContactsContract.Intents.Insert.COMPANY))
        assertEquals("maya@example.com", intent.getStringExtra(ContactsContract.Intents.Insert.EMAIL))
        assertEquals("120 Market St, San Francisco", intent.getStringExtra(ContactsContract.Intents.Insert.POSTAL))
    }
}
