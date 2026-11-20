package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material.icons.rounded.PersonAddAlt1
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.ContactEntity
import com.example.ocr.ContactValidator
import com.example.ui.components.CardScanBrandHeader
import com.example.ui.components.ContactMonogramAvatar
import com.example.ui.components.FullScreenCardImageDialog
import com.example.ui.components.ScannedCardPreviewCard
import java.io.File

@Composable
fun ContactDetailScreen(
    contact: ContactEntity?,
    onNavigateBack: () -> Unit,
    onEditClick: (ContactEntity) -> Unit,
    onDeleteConfirm: (Long) -> Unit,
    onAddToPhoneContacts: (ContactEntity) -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        CardScanBrandHeader()

        if (contact == null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back),
                    )
                }
                Text(
                    text = stringResource(R.string.title_contact_details),
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Contact not found.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            ContactDetailContent(
                contact = contact,
                showBackButton = true,
                onNavigateBack = onNavigateBack,
                onEditClick = { onEditClick(contact) },
                onDeleteConfirm = { onDeleteConfirm(contact.id) },
                onAddToPhoneContacts = { onAddToPhoneContacts(contact) },
                onShowMessage = onShowMessage,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
fun ContactDetailContent(
    contact: ContactEntity,
    showBackButton: Boolean,
    onNavigateBack: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteConfirm: () -> Unit,
    onAddToPhoneContacts: () -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showFullImage by remember { mutableStateOf(false) }
    val copiedMessage = stringResource(R.string.msg_copied)

    fun copyToClipboard(label: String, value: String) {
        val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        manager?.setPrimaryClip(ClipData.newPlainText(label, value))
        onShowMessage(copiedMessage)
    }

    val hasCardImage = remember(contact.cardImagePath) {
        contact.cardImagePath?.let(::File)?.exists() == true
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // Header row with title + Edit / Delete icons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showBackButton) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("detail_back_button"),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back),
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(8.dp))
            }

            Text(
                text = stringResource(R.string.title_contact_details),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )

            IconButton(
                onClick = onEditClick,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("detail_edit_button"),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = stringResource(R.string.cd_edit_contact),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }

            IconButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier
                    .size(48.dp)
                    .testTag("detail_delete_button"),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = stringResource(R.string.cd_delete_contact),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }

        // Scrollable detail body
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
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Identity summary card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        ContactMonogramAvatar(
                            initials = contact.initials,
                            seed = contact.displayTitle,
                            size = 58.dp,
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Text(
                                text = contact.displayTitle,
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            if (contact.designation.isNotBlank()) {
                                Text(
                                    text = contact.designation,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Medium,
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (contact.companyName.isNotBlank()) {
                                Text(
                                    text = contact.companyName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }

                // Scanned card image if available
                if (hasCardImage) {
                    ScannedCardPreviewCard(
                        imagePath = contact.cardImagePath,
                        name = contact.name,
                        designation = contact.designation,
                        companyName = contact.companyName,
                        onImageClick = { showFullImage = true },
                    )
                }

                // Structured Details Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        val rows = buildList<@Composable () -> Unit> {
                            if (contact.mobileNumber.isNotBlank()) {
                                add {
                                    DetailInfoRow(
                                        icon = Icons.Outlined.Phone,
                                        label = stringResource(R.string.field_mobile),
                                        value = contact.mobileNumber,
                                        actionContentDescription = stringResource(R.string.cd_call_contact),
                                        onRowClick = {
                                            val intent = Intent(
                                                Intent.ACTION_DIAL,
                                                Uri.parse("tel:${Uri.encode(contact.mobileNumber)}"),
                                            )
                                            runCatching { context.startActivity(intent) }
                                        },
                                        onCopyClick = { copyToClipboard("Mobile", contact.mobileNumber) },
                                        rowTag = "detail_mobile_row",
                                    )
                                }
                            }
                            if (contact.email.isNotBlank()) {
                                add {
                                    DetailInfoRow(
                                        icon = Icons.Outlined.Email,
                                        label = stringResource(R.string.field_email),
                                        value = contact.email,
                                        actionContentDescription = stringResource(R.string.cd_email_contact),
                                        onRowClick = {
                                            val intent = Intent(
                                                Intent.ACTION_SENDTO,
                                                Uri.parse("mailto:${Uri.encode(contact.email)}"),
                                            )
                                            runCatching { context.startActivity(intent) }
                                        },
                                        onCopyClick = { copyToClipboard("Email", contact.email) },
                                        rowTag = "detail_email_row",
                                    )
                                }
                            }
                            if (contact.companyWebsite.isNotBlank()) {
                                add {
                                    DetailInfoRow(
                                        icon = Icons.Outlined.Language,
                                        label = stringResource(R.string.field_website),
                                        value = contact.companyWebsite,
                                        actionContentDescription = stringResource(R.string.cd_open_website),
                                        onRowClick = {
                                            val formatted = ContactValidator.formatWebsiteForBrowser(
                                                contact.companyWebsite,
                                            )
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(formatted))
                                            runCatching { context.startActivity(intent) }
                                        },
                                        onCopyClick = { copyToClipboard("Website", contact.companyWebsite) },
                                        rowTag = "detail_website_row",
                                    )
                                }
                            }
                            if (contact.designation.isNotBlank()) {
                                add {
                                    DetailInfoRow(
                                        icon = Icons.Outlined.WorkOutline,
                                        label = stringResource(R.string.field_designation),
                                        value = contact.designation,
                                        actionContentDescription = null,
                                        onRowClick = null,
                                        onCopyClick = { copyToClipboard("Designation", contact.designation) },
                                        rowTag = "detail_designation_row",
                                    )
                                }
                            }
                            if (contact.companyName.isNotBlank()) {
                                add {
                                    DetailInfoRow(
                                        icon = Icons.Outlined.Business,
                                        label = stringResource(R.string.field_company),
                                        value = contact.companyName,
                                        actionContentDescription = null,
                                        onRowClick = null,
                                        onCopyClick = { copyToClipboard("Company", contact.companyName) },
                                        rowTag = "detail_company_row",
                                    )
                                }
                            }
                            if (contact.address.isNotBlank()) {
                                add {
                                    DetailInfoRow(
                                        icon = Icons.Outlined.LocationOn,
                                        label = stringResource(R.string.field_address),
                                        value = contact.address,
                                        actionContentDescription = stringResource(R.string.cd_open_address),
                                        onRowClick = {
                                            val geoUri = Uri.parse(
                                                "geo:0,0?q=${Uri.encode(contact.address)}",
                                            )
                                            val intent = Intent(Intent.ACTION_VIEW, geoUri)
                                            runCatching { context.startActivity(intent) }
                                        },
                                        onCopyClick = { copyToClipboard("Address", contact.address) },
                                        rowTag = "detail_address_row",
                                    )
                                }
                            }
                        }

                        if (rows.isEmpty()) {
                            Text(
                                text = "Tap Edit to add mobile number, email, website, or address.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(18.dp),
                            )
                        } else {
                            rows.forEachIndexed { index, row ->
                                row()
                                if (index < rows.lastIndex) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant,
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Bottom primary export button
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 600.dp)
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                ) {
                    Button(
                        onClick = onAddToPhoneContacts,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("detail_add_phone_contacts_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PersonAddAlt1,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.action_add_phone_contacts),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                            ),
                        )
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = stringResource(R.string.delete_dialog_title),
                    style = MaterialTheme.typography.titleLarge,
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.delete_dialog_body, contact.displayTitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteConfirm()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("confirm_delete_contact_button"),
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false },
                    modifier = Modifier.testTag("cancel_delete_contact_button"),
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    val imagePath = contact.cardImagePath
    if (showFullImage && !imagePath.isNullOrBlank()) {
        FullScreenCardImageDialog(
            imagePath = imagePath,
            onDismiss = { showFullImage = false },
        )
    }
}

@Composable
private fun DetailInfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    actionContentDescription: String?,
    onRowClick: (() -> Unit)?,
    onCopyClick: () -> Unit,
    rowTag: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onRowClick != null) {
                    Modifier.clickable(
                        onClickLabel = actionContentDescription,
                        onClick = onRowClick,
                    )
                } else {
                    Modifier
                },
            )
            .padding(start = 16.dp, end = 6.dp, top = 12.dp, bottom = 12.dp)
            .testTag(rowTag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f),
            modifier = Modifier.size(38.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = if (onRowClick != null) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
        }

        IconButton(
            onClick = onCopyClick,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.ContentCopy,
                contentDescription = stringResource(R.string.cd_copy_value),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
