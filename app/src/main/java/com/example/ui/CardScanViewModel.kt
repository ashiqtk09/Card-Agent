package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.contacts.ContactExportData
import com.example.data.CardScanDatabase
import com.example.data.ContactEntity
import com.example.data.ContactRepository
import com.example.data.DuplicateMatch
import com.example.data.SearchFilter
import com.example.data.SortOrder
import com.example.ocr.CardField
import com.example.ocr.CardOcrEngine
import com.example.ocr.ContactValidator
import com.example.ocr.ExtractedCardData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class ContactsUiState(
    val contacts: List<ContactEntity> = emptyList(),
    val totalCount: Int = 0,
    val searchQuery: String = "",
    val activeFilter: SearchFilter = SearchFilter.ALL,
    val sortOrder: SortOrder = SortOrder.RECENT,
    val recentlyAddedContactId: Long? = null,
    val selectedContactId: Long? = null,
)

data class ScanUiState(
    val isProcessingOcr: Boolean = false,
    val flashEnabled: Boolean = false,
    val errorMessage: String? = null,
)

data class ReviewUiState(
    val editingContactId: Long? = null,
    val name: String = "",
    val mobileNumber: String = "",
    val designation: String = "",
    val companyName: String = "",
    val companyWebsite: String = "",
    val address: String = "",
    val email: String = "",
    val cardImagePath: String? = null,
    val uncertainFields: Set<CardField> = emptySet(),
    val ocrFailed: Boolean = false,
    val isManualEntry: Boolean = false,
    val fieldErrors: Map<CardField, String> = emptyMap(),
    val formError: String? = null,
    val duplicateMatch: DuplicateMatch? = null,
    val isSaving: Boolean = false,
) {
    val isEditMode: Boolean
        get() = editingContactId != null

    fun toExportData(): ContactExportData = ContactExportData(
        name = name.trim(),
        mobileNumber = mobileNumber.trim(),
        designation = designation.trim(),
        companyName = companyName.trim(),
        companyWebsite = companyWebsite.trim(),
        address = address.trim(),
        email = email.trim(),
    )
}

sealed interface CardScanEvent {
    data object NavigateToReview : CardScanEvent
    data class ContactSaved(val message: String, val savedId: Long) : CardScanEvent
    data class ContactUpdated(val message: String, val updatedId: Long) : CardScanEvent
    data class ContactDeleted(val message: String) : CardScanEvent
    data class ShowMessage(val message: String) : CardScanEvent
}

private data class QueryParams(
    val query: String,
    val filter: SearchFilter,
    val sortOrder: SortOrder,
)

class CardScanViewModel(application: Application) : AndroidViewModel(application) {

    private val database = CardScanDatabase.getInstance(application)
    private val repository = ContactRepository(database.contactDao(), application)
    private val ocrEngine = CardOcrEngine(application)

    private val searchQuery = MutableStateFlow("")
    private val activeFilter = MutableStateFlow(SearchFilter.ALL)
    private val sortOrder = MutableStateFlow(SortOrder.RECENT)
    private val recentlyAddedContactId = MutableStateFlow<Long?>(null)
    private val selectedContactId = MutableStateFlow<Long?>(null)

    private val _scanUiState = MutableStateFlow(ScanUiState())
    val scanUiState: StateFlow<ScanUiState> = _scanUiState.asStateFlow()

    private val _reviewUiState = MutableStateFlow(ReviewUiState())
    val reviewUiState: StateFlow<ReviewUiState> = _reviewUiState.asStateFlow()

    private val _events = MutableSharedFlow<CardScanEvent>()
    val events: SharedFlow<CardScanEvent> = _events.asSharedFlow()

    private val queryParams = combine(searchQuery, activeFilter, sortOrder) { q, f, s ->
        QueryParams(q, f, s)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val filteredContacts = queryParams.flatMapLatest { params ->
        repository.observeContacts(params.query, params.filter, params.sortOrder)
    }

    val contactsUiState: StateFlow<ContactsUiState> = combine(
        filteredContacts,
        repository.totalCount,
        queryParams,
        recentlyAddedContactId,
        selectedContactId,
    ) { contacts, total, params, recentId, selectedId ->
        val validSelection = selectedId?.takeIf { id -> contacts.any { it.id == id } }
            ?: contacts.firstOrNull()?.id
        ContactsUiState(
            contacts = contacts,
            totalCount = total,
            searchQuery = params.query,
            activeFilter = params.filter,
            sortOrder = params.sortOrder,
            recentlyAddedContactId = recentId,
            selectedContactId = validSelection,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ContactsUiState(),
    )

    fun observeContact(id: Long): Flow<ContactEntity?> = repository.observeContactById(id)

    fun updateSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun updateSearchFilter(filter: SearchFilter) {
        activeFilter.value = filter
    }

    fun updateSortOrder(order: SortOrder) {
        sortOrder.value = order
    }

    fun clearSearch() {
        searchQuery.value = ""
        activeFilter.value = SearchFilter.ALL
    }

    fun selectContactForPane(id: Long) {
        selectedContactId.value = id
    }

    fun toggleFlash() {
        _scanUiState.update { it.copy(flashEnabled = !it.flashEnabled) }
    }

    fun setFlashEnabled(enabled: Boolean) {
        _scanUiState.update { it.copy(flashEnabled = enabled) }
    }

    fun clearScanError() {
        _scanUiState.update { it.copy(errorMessage = null) }
    }

    fun onScanCaptureError(message: String) {
        _scanUiState.update { it.copy(isProcessingOcr = false, errorMessage = message) }
    }

    fun processCapturedCardFile(file: File) {
        if (_scanUiState.value.isProcessingOcr) return
        viewModelScope.launch {
            _scanUiState.update { it.copy(isProcessingOcr = true, errorMessage = null) }
            val extracted = ocrEngine.recognizeFromFile(file, cropToCardFrame = true)
            _scanUiState.update { it.copy(isProcessingOcr = false) }
            applyExtractedData(extracted)
            _events.emit(CardScanEvent.NavigateToReview)
        }
    }

    fun processGalleryImageUri(uri: Uri) {
        if (_scanUiState.value.isProcessingOcr) return
        viewModelScope.launch {
            _scanUiState.update { it.copy(isProcessingOcr = true, errorMessage = null) }
            val extracted = ocrEngine.recognizeFromUri(uri)
            _scanUiState.update { it.copy(isProcessingOcr = false) }
            applyExtractedData(extracted)
            _events.emit(CardScanEvent.NavigateToReview)
        }
    }

    fun startManualContactEntry() {
        _reviewUiState.value = ReviewUiState(isManualEntry = true)
        viewModelScope.launch {
            _events.emit(CardScanEvent.NavigateToReview)
        }
    }

    fun startEditContact(contact: ContactEntity) {
        _reviewUiState.value = ReviewUiState(
            editingContactId = contact.id,
            name = contact.name,
            mobileNumber = contact.mobileNumber,
            designation = contact.designation,
            companyName = contact.companyName,
            companyWebsite = contact.companyWebsite,
            address = contact.address,
            email = contact.email,
            cardImagePath = contact.cardImagePath,
        )
        viewModelScope.launch {
            _events.emit(CardScanEvent.NavigateToReview)
        }
    }

    private fun applyExtractedData(data: ExtractedCardData) {
        _reviewUiState.value = ReviewUiState(
            editingContactId = null,
            name = data.name,
            mobileNumber = data.mobileNumber,
            designation = data.designation,
            companyName = data.companyName,
            companyWebsite = data.companyWebsite,
            address = data.address,
            email = data.email,
            cardImagePath = data.imagePath,
            uncertainFields = data.uncertainFields,
            ocrFailed = data.ocrFailed,
            isManualEntry = false,
        )
    }

    fun updateReviewField(field: CardField, value: String) {
        _reviewUiState.update { state ->
            val updatedErrors = state.fieldErrors - field
            val updatedUncertain = state.uncertainFields - field
            val base = state.copy(
                fieldErrors = updatedErrors,
                uncertainFields = updatedUncertain,
                formError = null,
            )
            when (field) {
                CardField.NAME -> base.copy(name = value)
                CardField.MOBILE -> base.copy(mobileNumber = value)
                CardField.DESIGNATION -> base.copy(designation = value)
                CardField.COMPANY -> base.copy(companyName = value)
                CardField.WEBSITE -> base.copy(companyWebsite = value)
                CardField.ADDRESS -> base.copy(address = value)
                CardField.EMAIL -> base.copy(email = value)
            }
        }
    }

    fun dismissDuplicateDialog() {
        _reviewUiState.update { it.copy(duplicateMatch = null) }
    }

    fun attemptSaveContact() {
        val state = _reviewUiState.value
        if (state.isSaving) return
        if (!validateReviewForm(state)) return

        viewModelScope.launch {
            _reviewUiState.update { it.copy(isSaving = true) }
            val duplicate = repository.findDuplicate(
                mobileNumber = state.mobileNumber,
                email = state.email,
                excludeId = state.editingContactId ?: -1L,
            )

            if (duplicate != null) {
                _reviewUiState.update { it.copy(isSaving = false, duplicateMatch = duplicate) }
                return@launch
            }

            if (state.editingContactId != null) {
                val updatedEntity = buildEntityFromReview(state, id = state.editingContactId)
                repository.updateContact(updatedEntity)
                _reviewUiState.update { it.copy(isSaving = false) }
                _events.emit(
                    CardScanEvent.ContactUpdated(
                        message = getApplication<Application>().getString(R.string.msg_contact_updated),
                        updatedId = state.editingContactId,
                    ),
                )
            } else {
                val insertedId = repository.insertContact(buildEntityFromReview(state, id = 0L))
                recentlyAddedContactId.value = insertedId
                selectedContactId.value = insertedId
                _reviewUiState.update { it.copy(isSaving = false) }
                _events.emit(
                    CardScanEvent.ContactSaved(
                        message = getApplication<Application>().getString(R.string.msg_contact_saved),
                        savedId = insertedId,
                    ),
                )
            }
        }
    }

    fun confirmUpdateExistingDuplicate() {
        val state = _reviewUiState.value
        val match = state.duplicateMatch ?: return
        if (state.isSaving) return

        viewModelScope.launch {
            _reviewUiState.update { it.copy(isSaving = true, duplicateMatch = null) }
            val merged = ContactEntity(
                id = match.existingContact.id,
                name = state.name.trim().ifBlank { match.existingContact.name },
                mobileNumber = state.mobileNumber.trim().ifBlank { match.existingContact.mobileNumber },
                normalizedMobile = "",
                designation = state.designation.trim().ifBlank { match.existingContact.designation },
                companyName = state.companyName.trim().ifBlank { match.existingContact.companyName },
                companyWebsite = state.companyWebsite.trim().ifBlank { match.existingContact.companyWebsite },
                address = state.address.trim().ifBlank { match.existingContact.address },
                email = state.email.trim().ifBlank { match.existingContact.email },
                cardImagePath = state.cardImagePath ?: match.existingContact.cardImagePath,
                createdAt = match.existingContact.createdAt,
            )
            repository.updateContact(merged)
            // If user was editing another record and chose to merge into existing duplicate, leave cleanly
            recentlyAddedContactId.value = merged.id
            selectedContactId.value = merged.id
            _reviewUiState.update { it.copy(isSaving = false) }
            _events.emit(
                CardScanEvent.ContactSaved(
                    message = getApplication<Application>().getString(R.string.msg_contact_updated),
                    savedId = merged.id,
                ),
            )
        }
    }

    fun confirmSaveSeparateFromDuplicate() {
        val state = _reviewUiState.value
        if (state.isSaving) return

        viewModelScope.launch {
            _reviewUiState.update { it.copy(isSaving = true, duplicateMatch = null) }
            if (state.editingContactId != null) {
                repository.updateContact(buildEntityFromReview(state, id = state.editingContactId))
                _reviewUiState.update { it.copy(isSaving = false) }
                _events.emit(
                    CardScanEvent.ContactUpdated(
                        message = getApplication<Application>().getString(R.string.msg_contact_updated),
                        updatedId = state.editingContactId,
                    ),
                )
            } else {
                val newId = repository.insertContact(buildEntityFromReview(state, id = 0L))
                recentlyAddedContactId.value = newId
                selectedContactId.value = newId
                _reviewUiState.update { it.copy(isSaving = false) }
                _events.emit(
                    CardScanEvent.ContactSaved(
                        message = getApplication<Application>().getString(R.string.msg_contact_saved),
                        savedId = newId,
                    ),
                )
            }
        }
    }

    fun deleteContact(id: Long) {
        viewModelScope.launch {
            repository.deleteContact(id)
            if (recentlyAddedContactId.value == id) {
                recentlyAddedContactId.value = null
            }
            if (selectedContactId.value == id) {
                selectedContactId.value = null
            }
            _events.emit(
                CardScanEvent.ContactDeleted(
                    getApplication<Application>().getString(R.string.msg_contact_deleted),
                ),
            )
        }
    }

    fun validateForPhoneExport(): Boolean = validateReviewForm(_reviewUiState.value)

    private fun validateReviewForm(state: ReviewUiState): Boolean {
        val app = getApplication<Application>()
        val errors = mutableMapOf<CardField, String>()

        val hasMinimumIdentity = state.name.isNotBlank() ||
            state.companyName.isNotBlank() ||
            state.mobileNumber.isNotBlank() ||
            state.email.isNotBlank()

        if (!ContactValidator.isValidPhone(state.mobileNumber)) {
            errors[CardField.MOBILE] = app.getString(R.string.error_invalid_phone)
        }
        if (!ContactValidator.isValidEmail(state.email)) {
            errors[CardField.EMAIL] = app.getString(R.string.error_invalid_email)
        }
        if (!ContactValidator.isValidWebsite(state.companyWebsite)) {
            errors[CardField.WEBSITE] = app.getString(R.string.error_invalid_website)
        }

        val formError = if (!hasMinimumIdentity) {
            app.getString(R.string.error_contact_empty)
        } else {
            null
        }

        _reviewUiState.update {
            it.copy(fieldErrors = errors, formError = formError)
        }
        return errors.isEmpty() && formError == null
    }

    private fun buildEntityFromReview(state: ReviewUiState, id: Long): ContactEntity {
        return ContactEntity(
            id = id,
            name = state.name.trim(),
            mobileNumber = state.mobileNumber.trim(),
            normalizedMobile = "",
            designation = state.designation.trim(),
            companyName = state.companyName.trim(),
            companyWebsite = state.companyWebsite.trim(),
            address = state.address.trim(),
            email = state.email.trim(),
            cardImagePath = state.cardImagePath,
        )
    }

    override fun onCleared() {
        super.onCleared()
        ocrEngine.close()
    }
}
