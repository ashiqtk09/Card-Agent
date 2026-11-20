package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.PersonAddAlt1
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.DuplicateMatch
import com.example.data.DuplicateMatchReason
import com.example.ocr.CardField
import com.example.ui.ReviewUiState
import com.example.ui.components.CardScanBrandHeader
import com.example.ui.components.ContactMonogramAvatar
import com.example.ui.components.FullScreenCardImageDialog
import com.example.ui.components.ScannedCardPreviewCard
import com.example.ui.theme.AmberBadgeBg
import com.example.ui.theme.AmberBadgeBorder
import com.example.ui.theme.AmberBadgeText

@Composable
fun ReviewContactScreen(
    uiState: ReviewUiState,
    onNavigateBack: () -> Unit,
    onRetakeScan: () -> Unit,
    onFieldChange: (CardField, String) -> Unit,
    onSaveClick: () -> Unit,
    onAddToPhoneContactsClick: () -> Unit,
    onDismissDuplicateDialog: () -> Unit,
    onConfirmUpdateExisting: () -> Unit,
    onConfirmSaveSeparate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    var showFullImagePreview by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding(),
    ) {
        // Brand Header + Title bar
        CardScanBrandHeader()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("review_back_button"),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back),
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }

            Text(
                text = stringResource(
                    if (uiState.isEditMode) R.string.title_edit_contact else R.string.title_review_contact,
                ),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )

            if (!uiState.isEditMode && !uiState.isManualEntry) {
                TextButton(
                    onClick = onRetakeScan,
                    modifier = Modifier.testTag("review_retake_button"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.scan_retake),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }

        // Scrollable card preview + editable form
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                ScannedCardPreviewCard(
                    imagePath = uiState.cardImagePath,
                    name = uiState.name,
                    designation = uiState.designation,
                    companyName = uiState.companyName,
                    onImageClick = uiState.cardImagePath?.let { { showFullImagePreview = true } },
                )

                // OCR failure or uncertain field alert banners
                AnimatedVisibility(visible = uiState.ocrFailed) {
                    OcrNoticeBanner(
                        message = stringResource(R.string.ocr_failed_banner),
                        isError = true,
                        actionLabel = stringResource(R.string.scan_retake),
                        onAction = onRetakeScan,
                    )
                }

                AnimatedVisibility(visible = !uiState.ocrFailed && uiState.uncertainFields.isNotEmpty()) {
                    OcrNoticeBanner(
                        message = stringResource(R.string.ocr_uncertain_banner),
                        isError = false,
                        actionLabel = null,
                        onAction = null,
                    )
                }

                AnimatedVisibility(visible = uiState.formError != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = uiState.formError.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    }
                }

                LabeledContactInput(
                    label = stringResource(R.string.field_name),
                    value = uiState.name,
                    placeholder = stringResource(R.string.placeholder_name),
                    onValueChange = { onFieldChange(CardField.NAME, it) },
                    isUncertain = CardField.NAME in uiState.uncertainFields,
                    errorText = uiState.fieldErrors[CardField.NAME],
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next,
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) },
                    ),
                    inputTag = "input_name",
                )

                LabeledContactInput(
                    label = stringResource(R.string.field_designation),
                    value = uiState.designation,
                    placeholder = stringResource(R.string.placeholder_designation),
                    onValueChange = { onFieldChange(CardField.DESIGNATION, it) },
                    isUncertain = CardField.DESIGNATION in uiState.uncertainFields,
                    errorText = uiState.fieldErrors[CardField.DESIGNATION],
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next,
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) },
                    ),
                    inputTag = "input_designation",
                )

                LabeledContactInput(
                    label = stringResource(R.string.field_company),
                    value = uiState.companyName,
                    placeholder = stringResource(R.string.placeholder_company),
                    onValueChange = { onFieldChange(CardField.COMPANY, it) },
                    isUncertain = CardField.COMPANY in uiState.uncertainFields,
                    errorText = uiState.fieldErrors[CardField.COMPANY],
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next,
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) },
                    ),
                    inputTag = "input_company",
                )

                LabeledContactInput(
                    label = stringResource(R.string.field_mobile),
                    value = uiState.mobileNumber,
                    placeholder = stringResource(R.string.placeholder_mobile),
                    onValueChange = { onFieldChange(CardField.MOBILE, it) },
                    isUncertain = CardField.MOBILE in uiState.uncertainFields,
                    errorText = uiState.fieldErrors[CardField.MOBILE],
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Next,
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) },
                    ),
                    inputTag = "input_mobile",
                )

                LabeledContactInput(
                    label = stringResource(R.string.field_email),
                    value = uiState.email,
                    placeholder = stringResource(R.string.placeholder_email),
                    onValueChange = { onFieldChange(CardField.EMAIL, it) },
                    isUncertain = CardField.EMAIL in uiState.uncertainFields,
                    errorText = uiState.fieldErrors[CardField.EMAIL],
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) },
                    ),
                    inputTag = "input_email",
                )

                LabeledContactInput(
                    label = stringResource(R.string.field_website),
                    value = uiState.companyWebsite,
                    placeholder = stringResource(R.string.placeholder_website),
                    onValueChange = { onFieldChange(CardField.WEBSITE, it) },
                    isUncertain = CardField.WEBSITE in uiState.uncertainFields,
                    errorText = uiState.fieldErrors[CardField.WEBSITE],
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Next,
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) },
                    ),
                    inputTag = "input_website",
                )

                LabeledContactInput(
                    label = stringResource(R.string.field_address),
                    value = uiState.address,
                    placeholder = stringResource(R.string.placeholder_address),
                    onValueChange = { onFieldChange(CardField.ADDRESS, it) },
                    isUncertain = CardField.ADDRESS in uiState.uncertainFields,
                    errorText = uiState.fieldErrors[CardField.ADDRESS],
                    singleLine = false,
                    minLines = 2,
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() },
                    ),
                    inputTag = "input_address",
                )

                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Sticky bottom actions matching the mockup's primary cobalt CTA + Add to Phone Contacts action
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 4.dp,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 600.dp)
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Button(
                        onClick = onSaveClick,
                        enabled = !uiState.isSaving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("save_contact_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp),
                            )
                        } else {
                            Text(
                                text = stringResource(
                                    if (uiState.isEditMode) {
                                        R.string.action_update_contact
                                    } else {
                                        R.string.action_save_contact
                                    },
                                ),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                ),
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onAddToPhoneContactsClick,
                        enabled = !uiState.isSaving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("review_add_phone_contacts_button"),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.25.dp, MaterialTheme.colorScheme.primary),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PersonAddAlt1,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.action_add_phone_contacts),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }
    }

    // Duplicate detection confirmation dialog
    val duplicate = uiState.duplicateMatch
    if (duplicate != null) {
        DuplicateContactDialog(
            duplicateMatch = duplicate,
            onDismiss = onDismissDuplicateDialog,
            onUpdateExisting = onConfirmUpdateExisting,
            onSaveSeparate = onConfirmSaveSeparate,
        )
    }

    // Full-screen card image zoom modal
    val fullImagePath = uiState.cardImagePath
    if (showFullImagePreview && !fullImagePath.isNullOrBlank()) {
        FullScreenCardImageDialog(
            imagePath = fullImagePath,
            onDismiss = { showFullImagePreview = false },
        )
    }
}

@Composable
private fun LabeledContactInput(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    isUncertain: Boolean,
    errorText: String?,
    inputTag: String,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (isUncertain) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = AmberBadgeBg,
                ) {
                    Text(
                        text = stringResource(R.string.badge_verify),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = AmberBadgeText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }
        }

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                )
            },
            isError = errorText != null,
            singleLine = singleLine,
            minLines = minLines,
            maxLines = maxLines,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
            ),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                errorContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = if (isUncertain) {
                    AmberBadgeBorder
                } else {
                    MaterialTheme.colorScheme.outline
                },
                errorBorderColor = MaterialTheme.colorScheme.error,
                cursorColor = MaterialTheme.colorScheme.primary,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(inputTag),
        )

        if (errorText != null) {
            Text(
                text = errorText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
}

@Composable
private fun OcrNoticeBanner(
    message: String,
    isError: Boolean,
    actionLabel: String?,
    onAction: (() -> Unit)?,
) {
    val bgColor = if (isError) MaterialTheme.colorScheme.errorContainer else AmberBadgeBg
    val borderColor = if (isError) {
        MaterialTheme.colorScheme.error.copy(alpha = 0.35f)
    } else {
        AmberBadgeBorder.copy(alpha = 0.5f)
    }
    val contentColor = if (isError) MaterialTheme.colorScheme.onErrorContainer else AmberBadgeText

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = if (isError) Icons.Rounded.ErrorOutline else Icons.Rounded.WarningAmber,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor,
                modifier = Modifier.weight(1f),
            )
            if (actionLabel != null && onAction != null) {
                TextButton(onClick = onAction) {
                    Text(
                        text = actionLabel,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = contentColor,
                    )
                }
            }
        }
    }
}

@Composable
private fun DuplicateContactDialog(
    duplicateMatch: DuplicateMatch,
    onDismiss: () -> Unit,
    onUpdateExisting: () -> Unit,
    onSaveSeparate: () -> Unit,
) {
    val existing = duplicateMatch.existingContact
    val matchedLabel = when (duplicateMatch.reason) {
        DuplicateMatchReason.MOBILE -> "mobile number"
        DuplicateMatchReason.EMAIL -> "email address"
        DuplicateMatchReason.MOBILE_AND_EMAIL -> "mobile number and email"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = stringResource(R.string.duplicate_dialog_title),
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = stringResource(R.string.duplicate_dialog_body, matchedLabel),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        ContactMonogramAvatar(
                            initials = existing.initials,
                            seed = existing.displayTitle,
                            size = 42.dp,
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = existing.displayTitle,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (existing.roleAndCompanySubtitle.isNotBlank()) {
                                Text(
                                    text = existing.roleAndCompanySubtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            val contactInfo = listOf(existing.mobileNumber, existing.email)
                                .filter { it.isNotBlank() }
                                .joinToString(" • ")
                            if (contactInfo.isNotBlank()) {
                                Text(
                                    text = contactInfo,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onUpdateExisting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("duplicate_update_existing_button"),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(stringResource(R.string.action_update_existing))
                }
                OutlinedButton(
                    onClick = onSaveSeparate,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("duplicate_save_separate_button"),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(stringResource(R.string.action_save_separate))
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .testTag("duplicate_cancel_button"),
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        },
    )
}
