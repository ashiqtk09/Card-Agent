package com.example.contacts

import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.provider.ContactsContract
import com.example.ocr.ContactValidator

data class ContactExportData(
    val name: String,
    val mobileNumber: String,
    val designation: String,
    val companyName: String,
    val companyWebsite: String,
    val address: String,
    val email: String,
)

object PhoneContactsExporter {

    /**
     * Launches Android's native Contacts editor prefilled with every available business card field.
     * Uses the standard zero-permission ContactsContract INSERT Intent so the user reviews and
     * confirms before saving to their phone's native Contacts app.
     *
     * @return true if the native Contacts editor was launched, false if no Contacts app exists.
     */
    fun openNativeContactEditor(context: Context, data: ContactExportData): Boolean {
        val intent = buildInsertContactIntent(data).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }

    fun buildInsertContactIntent(data: ContactExportData): Intent {
        val intent = Intent(ContactsContract.Intents.Insert.ACTION).apply {
            type = ContactsContract.RawContacts.CONTENT_TYPE
        }

        if (data.name.isNotBlank()) {
            intent.putExtra(ContactsContract.Intents.Insert.NAME, data.name.trim())
        }
        if (data.mobileNumber.isNotBlank()) {
            intent.putExtra(ContactsContract.Intents.Insert.PHONE, data.mobileNumber.trim())
            intent.putExtra(
                ContactsContract.Intents.Insert.PHONE_TYPE,
                ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE,
            )
        }
        if (data.email.isNotBlank()) {
            intent.putExtra(ContactsContract.Intents.Insert.EMAIL, data.email.trim())
            intent.putExtra(
                ContactsContract.Intents.Insert.EMAIL_TYPE,
                ContactsContract.CommonDataKinds.Email.TYPE_WORK,
            )
        }
        if (data.companyName.isNotBlank()) {
            intent.putExtra(ContactsContract.Intents.Insert.COMPANY, data.companyName.trim())
        }
        if (data.designation.isNotBlank()) {
            intent.putExtra(ContactsContract.Intents.Insert.JOB_TITLE, data.designation.trim())
        }
        if (data.address.isNotBlank()) {
            intent.putExtra(ContactsContract.Intents.Insert.POSTAL, data.address.trim())
            intent.putExtra(
                ContactsContract.Intents.Insert.POSTAL_TYPE,
                ContactsContract.CommonDataKinds.StructuredPostal.TYPE_WORK,
            )
        }

        val extraData = ArrayList<ContentValues>()
        if (data.companyWebsite.isNotBlank()) {
            extraData.add(
                ContentValues().apply {
                    put(
                        ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE,
                    )
                    put(
                        ContactsContract.CommonDataKinds.Website.URL,
                        ContactValidator.formatWebsiteForBrowser(data.companyWebsite),
                    )
                    put(
                        ContactsContract.CommonDataKinds.Website.TYPE,
                        ContactsContract.CommonDataKinds.Website.TYPE_WORK,
                    )
                },
            )
        }
        if (extraData.isNotEmpty()) {
            intent.putParcelableArrayListExtra(ContactsContract.Intents.Insert.DATA, extraData)
        }

        return intent
    }
}
