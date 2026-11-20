package com.example.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
data object ContactsRoute

@Serializable
data object ScanRoute

@Serializable
data object ReviewRoute

@Serializable
data class ContactDetailRoute(val contactId: Long)
