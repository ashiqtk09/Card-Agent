package com.example

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.contacts.ContactExportData
import com.example.contacts.PhoneContactsExporter
import com.example.data.ContactEntity
import com.example.ui.CardScanEvent
import com.example.ui.CardScanViewModel
import com.example.ui.navigation.ContactDetailRoute
import com.example.ui.navigation.ContactsRoute
import com.example.ui.navigation.ReviewRoute
import com.example.ui.navigation.ScanRoute
import com.example.ui.screens.ContactDetailScreen
import com.example.ui.screens.ContactsScreen
import com.example.ui.screens.ReviewContactScreen
import com.example.ui.screens.ScanScreen
import com.example.ui.theme.CardScanTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        setContent {
            CardScanTheme(darkTheme = false) {
                CardScanApp()
            }
        }
    }
}

@Composable
fun CardScanApp(
    viewModel: CardScanViewModel = viewModel(),
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val contactsState by viewModel.contactsUiState.collectAsStateWithLifecycle()
    val scanState by viewModel.scanUiState.collectAsStateWithLifecycle()
    val reviewState by viewModel.reviewUiState.collectAsStateWithLifecycle()

    fun showSnackbar(message: String) {
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short,
            )
        }
    }

    fun exportToPhoneContacts(exportData: ContactExportData) {
        val opened = PhoneContactsExporter.openNativeContactEditor(context, exportData)
        if (opened) {
            showSnackbar(context.getString(R.string.msg_opening_phone_contacts))
        } else {
            showSnackbar(context.getString(R.string.error_contacts_app_missing))
        }
    }

    fun ContactEntity.toExportData() = ContactExportData(
        name = name,
        mobileNumber = mobileNumber,
        designation = designation,
        companyName = companyName,
        companyWebsite = companyWebsite,
        address = address,
        email = email,
    )

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                CardScanEvent.NavigateToReview -> {
                    navController.navigate(ReviewRoute) {
                        launchSingleTop = true
                    }
                }

                is CardScanEvent.ContactSaved -> {
                    navController.popBackStack(route = ContactsRoute, inclusive = false)
                    showSnackbar(event.message)
                }

                is CardScanEvent.ContactUpdated -> {
                    navController.popBackStack()
                    showSnackbar(event.message)
                }

                is CardScanEvent.ContactDeleted -> {
                    if (navController.currentDestination?.route?.contains("ContactDetailRoute") == true) {
                        navController.popBackStack()
                    }
                    showSnackbar(event.message)
                }

                is CardScanEvent.ShowMessage -> {
                    showSnackbar(event.message)
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = ContactsRoute,
            modifier = Modifier.fillMaxSize(),
        ) {
            composable<ContactsRoute> {
                ContactsScreen(
                    uiState = contactsState,
                    onSearchQueryChange = viewModel::updateSearchQuery,
                    onFilterChange = viewModel::updateSearchFilter,
                    onSortOrderChange = viewModel::updateSortOrder,
                    onClearSearch = viewModel::clearSearch,
                    onOpenScan = {
                        viewModel.clearScanError()
                        navController.navigate(ScanRoute) {
                            launchSingleTop = true
                        }
                    },
                    onAddManualContact = viewModel::startManualContactEntry,
                    onContactClick = { contact ->
                        navController.navigate(ContactDetailRoute(contact.id)) {
                            launchSingleTop = true
                        }
                    },
                    onSelectContactInPane = viewModel::selectContactForPane,
                    onEditContact = viewModel::startEditContact,
                    onDeleteContact = viewModel::deleteContact,
                    onAddToPhoneContacts = { contact ->
                        exportToPhoneContacts(contact.toExportData())
                    },
                    onShowMessage = ::showSnackbar,
                )
            }

            composable<ScanRoute> {
                ScanScreen(
                    uiState = scanState,
                    onNavigateBack = { navController.popBackStack() },
                    onToggleFlash = viewModel::toggleFlash,
                    onResetFlash = { viewModel.setFlashEnabled(false) },
                    onPhotoCaptured = viewModel::processCapturedCardFile,
                    onCaptureError = viewModel::onScanCaptureError,
                    onGalleryImageSelected = viewModel::processGalleryImageUri,
                    onEnterManually = viewModel::startManualContactEntry,
                )
            }

            composable<ReviewRoute> {
                ReviewContactScreen(
                    uiState = reviewState,
                    onNavigateBack = { navController.popBackStack() },
                    onRetakeScan = {
                        val poppedToScan = navController.popBackStack(
                            route = ScanRoute,
                            inclusive = false,
                        )
                        if (!poppedToScan) {
                            navController.navigate(ScanRoute) {
                                launchSingleTop = true
                            }
                        }
                    },
                    onFieldChange = viewModel::updateReviewField,
                    onSaveClick = viewModel::attemptSaveContact,
                    onAddToPhoneContactsClick = {
                        if (viewModel.validateForPhoneExport()) {
                            exportToPhoneContacts(reviewState.toExportData())
                        }
                    },
                    onDismissDuplicateDialog = viewModel::dismissDuplicateDialog,
                    onConfirmUpdateExisting = viewModel::confirmUpdateExistingDuplicate,
                    onConfirmSaveSeparate = viewModel::confirmSaveSeparateFromDuplicate,
                )
            }

            composable<ContactDetailRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<ContactDetailRoute>()
                val contact by remember(route.contactId) {
                    viewModel.observeContact(route.contactId)
                }.collectAsStateWithLifecycle(initialValue = null)

                ContactDetailScreen(
                    contact = contact,
                    onNavigateBack = { navController.popBackStack() },
                    onEditClick = viewModel::startEditContact,
                    onDeleteConfirm = viewModel::deleteContact,
                    onAddToPhoneContacts = { item ->
                        exportToPhoneContacts(item.toExportData())
                    },
                    onShowMessage = ::showSnackbar,
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 82.dp),
        ) { data ->
            Snackbar(snackbarData = data)
        }
    }
}
