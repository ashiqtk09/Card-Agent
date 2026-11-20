package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CropFree
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.R
import com.example.ui.theme.AvatarColorPair
import com.example.ui.theme.AvatarPalette
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.CobaltPrimaryLight
import java.io.File
import kotlin.math.abs

@Composable
fun CardScanBrandHeader(
    modifier: Modifier = Modifier,
) {
    val logoDescription = stringResource(R.string.cd_brand_logo)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 10.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CardScanLogoMark(
            modifier = Modifier
                .size(width = 28.dp, height = 22.dp)
                .semantics { contentDescription = logoDescription },
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                letterSpacing = (-0.3).sp,
            ),
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
fun CardScanLogoMark(
    modifier: Modifier = Modifier,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
) {
    Canvas(modifier = modifier) {
        val strokeWidth = size.minDimension * 0.11f
        val corner = CornerRadius(size.minDimension * 0.16f)

        // Back card
        drawRoundRect(
            color = primaryColor.copy(alpha = 0.45f),
            topLeft = Offset(size.width * 0.22f, size.height * 0.06f),
            size = Size(size.width * 0.72f, size.height * 0.68f),
            cornerRadius = corner,
            style = Stroke(width = strokeWidth),
        )

        // Front card fill mask + outline
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(size.width * 0.06f, size.height * 0.24f),
            size = Size(size.width * 0.72f, size.height * 0.68f),
            cornerRadius = corner,
        )
        drawRoundRect(
            color = primaryColor,
            topLeft = Offset(size.width * 0.06f, size.height * 0.24f),
            size = Size(size.width * 0.72f, size.height * 0.68f),
            cornerRadius = corner,
            style = Stroke(width = strokeWidth),
        )

        // Accent bar inside front card
        drawRoundRect(
            color = primaryColor,
            topLeft = Offset(size.width * 0.18f, size.height * 0.46f),
            size = Size(size.width * 0.34f, size.height * 0.13f),
            cornerRadius = CornerRadius(size.minDimension * 0.06f),
        )
    }
}

fun avatarColorFor(seed: String): AvatarColorPair {
    val index = abs(seed.hashCode()) % AvatarPalette.size
    return AvatarPalette[index]
}

@Composable
fun ContactMonogramAvatar(
    initials: String,
    seed: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
) {
    val colors = remember(seed) { avatarColorFor(seed) }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(colors.background),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials,
            style = if (size >= 52.dp) {
                MaterialTheme.typography.titleLarge
            } else {
                MaterialTheme.typography.titleSmall
            },
            fontWeight = FontWeight.Bold,
            color = colors.foreground,
            maxLines = 1,
        )
    }
}

@Composable
fun ScannedCardPreviewCard(
    imagePath: String?,
    name: String,
    designation: String,
    companyName: String,
    modifier: Modifier = Modifier,
    onImageClick: (() -> Unit)? = null,
) {
    val imageFile = remember(imagePath) {
        imagePath?.takeIf { it.isNotBlank() }?.let(::File)?.takeIf { it.exists() }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(14.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (imageFile != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.68f)
                    .shadow(
                        elevation = 6.dp,
                        shape = RoundedCornerShape(14.dp),
                        ambientColor = Color.Black.copy(alpha = 0.08f),
                        spotColor = Color.Black.copy(alpha = 0.12f),
                    )
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .then(
                        if (onImageClick != null) {
                            Modifier
                                .testTag("scanned_card_preview_button")
                                .clickable(onClick = onImageClick)
                        } else {
                            Modifier
                        },
                    ),
            ) {
                AsyncImage(
                    model = imageFile,
                    contentDescription = stringResource(R.string.cd_scanned_card_preview),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )

                if (onImageClick != null) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(10.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = Color.Black.copy(alpha = 0.58f),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ZoomIn,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp),
                            )
                            Text(
                                text = "Zoom",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                            )
                        }
                    }
                }
            }
        } else {
            // Live miniature business card preview for manual entry
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.75f)
                    .shadow(
                        elevation = 6.dp,
                        shape = RoundedCornerShape(14.dp),
                        ambientColor = Color.Black.copy(alpha = 0.06f),
                        spotColor = Color.Black.copy(alpha = 0.10f),
                    ),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                ) {
                    // Subtle geometric mark in top-right echoing mockup card layout
                    Canvas(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(width = 34.dp, height = 24.dp),
                    ) {
                        val path = Path().apply {
                            moveTo(0f, size.height)
                            lineTo(size.width * 0.28f, 0f)
                            lineTo(size.width * 0.50f, size.height * 0.58f)
                            lineTo(size.width * 0.72f, 0f)
                            lineTo(size.width, size.height)
                            lineTo(size.width * 0.78f, size.height)
                            lineTo(size.width * 0.67f, size.height * 0.44f)
                            lineTo(size.width * 0.50f, size.height * 0.88f)
                            lineTo(size.width * 0.33f, size.height * 0.44f)
                            lineTo(size.width * 0.22f, size.height)
                            close()
                        }
                        drawPath(path = path, color = CobaltPrimary)
                    }

                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .fillMaxWidth(0.76f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = name.ifBlank { "Contact Name" },
                            style = MaterialTheme.typography.titleLarge,
                            color = if (name.isBlank()) {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = designation.ifBlank { "Designation" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (designation.isBlank()) {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .width(22.dp)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(CobaltPrimaryLight),
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = companyName.ifBlank { "Company Name" },
                            style = MaterialTheme.typography.titleSmall,
                            color = if (companyName.isBlank()) {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CardScanBottomBar(
    isContactsSelected: Boolean,
    onContactsClick: () -> Unit,
    onScanClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 6.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BottomNavPillItem(
                    label = stringResource(R.string.nav_contacts),
                    selected = isContactsSelected,
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.Group,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                        )
                    },
                    onClick = onContactsClick,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("nav_contacts_tab"),
                )
                BottomNavPillItem(
                    label = stringResource(R.string.nav_scan),
                    selected = !isContactsSelected,
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.CropFree,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                        )
                    },
                    onClick = onScanClick,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("nav_scan_tab"),
                )
            }
        }
    }
}

@Composable
private fun BottomNavPillItem(
    label: String,
    selected: Boolean,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        Color.Transparent
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = modifier
            .height(54.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        contentColor = contentColor,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            icon()
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                ),
                color = contentColor,
            )
        }
    }
}

@Composable
fun CardScanNavigationRail(
    isContactsSelected: Boolean,
    onContactsClick: () -> Unit,
    onScanClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationRail(
        modifier = modifier.statusBarsPadding(),
        containerColor = MaterialTheme.colorScheme.surface,
        header = {
            Box(modifier = Modifier.padding(top = 12.dp, bottom = 16.dp)) {
                CardScanLogoMark(modifier = Modifier.size(width = 30.dp, height = 24.dp))
            }
        },
    ) {
        NavigationRailItem(
            selected = isContactsSelected,
            onClick = onContactsClick,
            icon = { Icon(Icons.Rounded.Group, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_contacts)) },
            modifier = Modifier.testTag("rail_contacts_tab"),
            colors = NavigationRailItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
            ),
        )
        Spacer(modifier = Modifier.height(8.dp))
        NavigationRailItem(
            selected = !isContactsSelected,
            onClick = onScanClick,
            icon = { Icon(Icons.Rounded.CropFree, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_scan)) },
            modifier = Modifier.testTag("rail_scan_tab"),
            colors = NavigationRailItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
            ),
        )
    }
}

@Composable
fun FullScreenCardImageDialog(
    imagePath: String,
    onDismiss: () -> Unit,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.94f))
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val newScale = (scale * zoom).coerceIn(1f, 4f)
                        scale = newScale
                        offset = if (newScale > 1f) offset + pan else Offset.Zero
                    }
                },
        ) {
            AsyncImage(
                model = File(imagePath),
                contentDescription = stringResource(R.string.cd_scanned_card_preview),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y,
                    ),
            )

            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(16.dp),
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.6f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("close_fullscreen_preview_button"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.cd_close_preview),
                        tint = Color.White,
                    )
                }
            }
        }
    }
}
