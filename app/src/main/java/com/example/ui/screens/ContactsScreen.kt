package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CropFree
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.ContactEntity
import com.example.data.SearchFilter
import com.example.data.SortOrder
import com.example.ui.ContactsUiState
import com.example.ui.components.CardScanBottomBar
import com.example.ui.components.CardScanBrandHeader
import com.example.ui.components.CardScanLogoMark
import com.example.ui.components.CardScanNavigationRail
import com.example.ui.components.ContactMonogramAvatar
import com.example.ui.theme.MintBadgeBg
import com.example.ui.theme.MintBadgeText

@Composable
fun ContactsScreen(
    uiState: ContactsUiState,
    onSearchQueryChange: (String) -> Unit,
    onFilterChange: (SearchFilter) -> Unit,
    onSortOrderChange: (SortOrder) -> Unit,
    onClearSearch: () -> Unit,
    onOpenScan: () -> Unit,
    onAddManualContact: () -> Unit,
    onContactClick: (ContactEntity) -> Unit,
    onSelectContactInPane: (Long) -> Unit,
    onEditContact: (ContactEntity) -> Unit,
    onDeleteContact: (Long) -> Unit,
    onAddToPhoneContacts: (ContactEntity) -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        val isExpanded = maxWidth >= 840.dp

        if (isExpanded) {
            // Canonical Expanded Layout: NavigationRail + List-Detail two-pane directory
            Row(modifier = Modifier.fillMaxSize()) {
                CardScanNavigationRail(
                    isContactsSelected = true,
                    onContactsClick = {},
                    onScanClick = onOpenScan,
                )
                VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                ContactsListPane(
                    uiState = uiState,
                    isExpandedPane = true,
                    onSearchQueryChange = onSearchQueryChange,
                    onFilterChange = onFilterChange,
                    onSortOrderChange = onSortOrderChange,
                    onClearSearch = onClearSearch,
                    onOpenScan = onOpenScan,
                    onAddManualContact = onAddManualContact,
                    onContactClick = { contact -> onSelectContactInPane(contact.id) },
                    modifier = Modifier
                        .weight(0.46f)
                        .fillMaxHeight(),
                )

                VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                val selectedContact = uiState.contacts.firstOrNull {
                    it.id == uiState.selectedContactId
                }

                Box(
                    modifier = Modifier
                        .weight(0.54f)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.background),
                ) {
                    if (selectedContact != null) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            ContactDetailContent(
                                contact = selectedContact,
                                showBackButton = false,
                                onNavigateBack = {},
                                onEditClick = { onEditContact(selectedContact) },
                                onDeleteConfirm = { onDeleteContact(selectedContact.id) },
                                onAddToPhoneContacts = { onAddToPhoneContacts(selectedContact) },
                                onShowMessage = onShowMessage,
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "Select a contact to view details",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        } else {
            // Compact / Medium Phone Layout matching attached mockup
            Column(modifier = Modifier.fillMaxSize()) {
                ContactsListPane(
                    uiState = uiState,
                    isExpandedPane = false,
                    onSearchQueryChange = onSearchQueryChange,
                    onFilterChange = onFilterChange,
                    onSortOrderChange = onSortOrderChange,
                    onClearSearch = onClearSearch,
                    onOpenScan = onOpenScan,
                    onAddManualContact = onAddManualContact,
                    onContactClick = onContactClick,
                    modifier = Modifier.weight(1f),
                )

                CardScanBottomBar(
                    isContactsSelected = true,
                    onContactsClick = {},
                    onScanClick = onOpenScan,
                )
            }
        }
    }
}

@Composable
private fun ContactsListPane(
    uiState: ContactsUiState,
    isExpandedPane: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onFilterChange: (SearchFilter) -> Unit,
    onSortOrderChange: (SortOrder) -> Unit,
    onClearSearch: () -> Unit,
    onOpenScan: () -> Unit,
    onAddManualContact: () -> Unit,
    onContactClick: (ContactEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        CardScanBrandHeader()

        // Title + Overflow Menu matching mockup
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 12.dp, top = 6.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.title_contacts),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("contacts_more_menu_button"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MoreHoriz,
                        contentDescription = stringResource(R.string.cd_more_options),
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    containerColor = MaterialTheme.colorScheme.surface,
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_add_manually)) },
                        leadingIcon = {
                            Icon(Icons.Rounded.Add, contentDescription = null)
                        },
                        onClick = {
                            menuExpanded = false
                            onAddManualContact()
                        },
                        modifier = Modifier.testTag("menu_add_manual_contact"),
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    DropdownMenuItem(
                        text = { Text("Sort: Newest first") },
                        trailingIcon = {
                            if (uiState.sortOrder == SortOrder.RECENT) {
                                Icon(
                                    Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        },
                        onClick = {
                            menuExpanded = false
                            onSortOrderChange(SortOrder.RECENT)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Sort: Name (A–Z)") },
                        trailingIcon = {
                            if (uiState.sortOrder == SortOrder.NAME) {
                                Icon(
                                    Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        },
                        onClick = {
                            menuExpanded = false
                            onSortOrderChange(SortOrder.NAME)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Sort: Company (A–Z)") },
                        trailingIcon = {
                            if (uiState.sortOrder == SortOrder.COMPANY) {
                                Icon(
                                    Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        },
                        onClick = {
                            menuExpanded = false
                            onSortOrderChange(SortOrder.COMPANY)
                        },
                    )
                }
            }
        }

        // Rounded Search Bar matching mockup
        ContactsSearchBar(
            query = uiState.searchQuery,
            onQueryChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
        )

        // Optional Filter Chips (All / Name / Company / Designation)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val filters = listOf(
                SearchFilter.ALL to stringResource(R.string.filter_all),
                SearchFilter.NAME to stringResource(R.string.filter_name),
                SearchFilter.COMPANY to stringResource(R.string.filter_company),
                SearchFilter.DESIGNATION to stringResource(R.string.filter_designation),
            )
            filters.forEach { (filter, label) ->
                val selected = uiState.activeFilter == filter
                FilterChip(
                    selected = selected,
                    onClick = { onFilterChange(filter) },
                    label = {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                            ),
                        )
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.primary,
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        borderColor = MaterialTheme.colorScheme.outlineVariant,
                        selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                    ),
                    modifier = Modifier.testTag("filter_chip_${filter.name.lowercase()}"),
                )
            }
        }

        // Main directory body
        when {
            uiState.totalCount == 0 -> {
                EmptyDirectoryState(
                    onScanClick = onOpenScan,
                    onManualClick = onAddManualContact,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
            }

            uiState.contacts.isEmpty() -> {
                EmptySearchState(
                    query = uiState.searchQuery,
                    onClearSearch = onClearSearch,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
            }

            else -> {
                // Compact table column strip for clear directory scanning
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "NAME  •  DESIGNATION, COMPANY",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "${uiState.contacts.size} ${if (uiState.contacts.size == 1) "card" else "cards"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("contacts_list"),
                    contentPadding = PaddingValues(bottom = 16.dp),
                ) {
                    itemsIndexed(
                        items = uiState.contacts,
                        key = { _, contact -> contact.id },
                    ) { index, contact ->
                        ContactTableRow(
                            contact = contact,
                            isJustAdded = contact.id == uiState.recentlyAddedContactId,
                            isSelected = isExpandedPane && contact.id == uiState.selectedContactId,
                            onClick = { onContactClick(contact) },
                        )
                        if (index < uiState.contacts.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 76.dp, end = 20.dp),
                                color = MaterialTheme.colorScheme.outlineVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactsSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    Surface(
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (query.isEmpty()) {
                    Text(
                        text = stringResource(R.string.search_placeholder),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = { focusManager.clearFocus() },
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("contacts_search_input"),
                )
            }
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("clear_search_button"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.cd_clear_search),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ContactTableRow(
    contact: ContactEntity,
    isJustAdded: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val rowBackground = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
    } else {
        MaterialTheme.colorScheme.background
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBackground)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp)
            .testTag("contact_row_${contact.id}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ContactMonogramAvatar(
            initials = contact.initials,
            seed = contact.displayTitle,
            size = 44.dp,
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = contact.displayTitle,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )

                if (isJustAdded) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MintBadgeBg,
                    ) {
                        Text(
                            text = stringResource(R.string.badge_just_added),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                            ),
                            color = MintBadgeText,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                        )
                    }
                }
            }

            val subtitle = contact.roleAndCompanySubtitle.ifBlank {
                contact.mobileNumber.ifBlank { contact.email }
            }
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = stringResource(R.string.cd_open_contact),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun EmptyDirectoryState(
    onScanClick: () -> Unit,
    onManualClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.padding(horizontal = 28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 360.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(76.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    CardScanLogoMark(modifier = Modifier.size(width = 40.dp, height = 32.dp))
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.empty_contacts_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.empty_contacts_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onScanClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("empty_scan_card_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Icon(
                    imageVector = Icons.Rounded.CropFree,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.action_scan_first_card),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = onManualClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("empty_add_manual_button"),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                Text(
                    text = stringResource(R.string.action_add_manually),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun EmptySearchState(
    query: String,
    onClearSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.padding(horizontal = 28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 340.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.size(64.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.SearchOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(30.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.empty_search_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (query.isNotBlank()) {
                    "No contacts found matching \"$query\". ${stringResource(R.string.empty_search_subtitle)}"
                } else {
                    stringResource(R.string.empty_search_subtitle)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(18.dp))
            OutlinedButton(
                onClick = onClearSearch,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("clear_search_empty_button"),
            ) {
                Text(
                    text = stringResource(R.string.action_clear_filters),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}
