package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.view.MotionEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.ui.ScanUiState
import com.example.ui.components.CardScanBrandHeader
import com.example.ui.theme.CobaltPrimary
import java.io.File

@Composable
fun ScanScreen(
    uiState: ScanUiState,
    onNavigateBack: () -> Unit,
    onToggleFlash: () -> Unit,
    onResetFlash: () -> Unit,
    onPhotoCaptured: (File) -> Unit,
    onCaptureError: (String) -> Unit,
    onGalleryImageSelected: (Uri) -> Unit,
    onEnterManually: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var permissionRequestedOnce by remember { mutableStateOf(false) }
    var cameraUnavailable by remember { mutableStateOf(false) }
    var hasFlashUnit by remember { mutableStateOf(false) }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasCameraPermission = granted
        permissionRequestedOnce = true
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            onGalleryImageSelected(uri)
        }
    }

    // Request camera permission only when the user actually navigates to the Scan screen
    LaunchedEffect(Unit) {
        if (!hasCameraPermission && !permissionRequestedOnce) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Sync torch state with CameraX
    LaunchedEffect(boundCamera, uiState.flashEnabled, hasFlashUnit) {
        if (hasFlashUnit) {
            runCatching {
                boundCamera?.cameraControl?.enableTorch(uiState.flashEnabled)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { boundCamera?.cameraControl?.enableTorch(false) }
            onResetFlash()
        }
    }

    val captureErrorText = stringResource(R.string.error_capture_failed)

    fun launchGalleryPicker() {
        photoPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
        )
    }

    fun triggerCapture() {
        val captureUseCase = imageCapture
        if (captureUseCase == null) {
            onCaptureError(captureErrorText)
            return
        }
        val outputDir = File(context.cacheDir, "captured_cards").apply { mkdirs() }
        val outputFile = File(outputDir, "scan_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()

        captureUseCase.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    onPhotoCaptured(outputFile)
                }

                override fun onError(exception: ImageCaptureException) {
                    onCaptureError(captureErrorText)
                }
            },
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // Brand mark + Scan card title header
        CardScanBrandHeader()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("scan_back_button"),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back),
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }

            Text(
                text = stringResource(R.string.title_scan_card),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )

            TextButton(
                onClick = onEnterManually,
                enabled = !uiState.isProcessingOcr,
                modifier = Modifier.testTag("scan_enter_manually_button"),
            ) {
                Text(
                    text = stringResource(R.string.scan_manual_entry),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        // Camera viewfinder / fallback region
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF111520)),
        ) {
            when {
                hasCameraPermission && !cameraUnavailable -> {
                    CameraPreviewSurface(
                        lifecycleOwner = lifecycleOwner,
                        onCameraBound = { camera, capture ->
                            boundCamera = camera
                            imageCapture = capture
                            hasFlashUnit = camera.cameraInfo.hasFlashUnit()
                        },
                        onCameraFailed = {
                            cameraUnavailable = true
                        },
                        modifier = Modifier.fillMaxSize(),
                    )

                    BusinessCardFramingOverlay(
                        modifier = Modifier.fillMaxSize(),
                    )

                    // Bottom-centered translucent hint pill matching design mockup
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 24.dp, vertical = 22.dp),
                        shape = RoundedCornerShape(24.dp),
                        color = Color(0xFF1C2029).copy(alpha = 0.72f),
                    ) {
                        Text(
                            text = stringResource(R.string.scan_align_hint),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium,
                            ),
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        )
                    }
                }

                !hasCameraPermission -> {
                    CameraFallbackCard(
                        title = stringResource(R.string.scan_camera_permission_title),
                        body = stringResource(R.string.scan_camera_permission_body),
                        primaryButtonText = if (permissionRequestedOnce) {
                            stringResource(R.string.scan_open_settings)
                        } else {
                            stringResource(R.string.scan_enable_camera)
                        },
                        onPrimaryClick = {
                            if (permissionRequestedOnce) {
                                openAppSettings(context)
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        onImportGallery = ::launchGalleryPicker,
                        onEnterManually = onEnterManually,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }

                else -> {
                    CameraFallbackCard(
                        title = stringResource(R.string.title_scan_card),
                        body = stringResource(R.string.error_camera_unavailable),
                        primaryButtonText = stringResource(R.string.scan_import_photo),
                        onPrimaryClick = ::launchGalleryPicker,
                        onImportGallery = null,
                        onEnterManually = onEnterManually,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
            }

            // Inline capture error banner
            androidx.compose.animation.AnimatedVisibility(
                visible = uiState.errorMessage != null && !uiState.isProcessingOcr,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f)),
                ) {
                    Text(
                        text = uiState.errorMessage.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    )
                }
            }

            // OCR Processing overlay
            androidx.compose.animation.AnimatedVisibility(
                visible = uiState.isProcessingOcr,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize(),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.62f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 8.dp,
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 28.dp, vertical = 22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(34.dp),
                            )
                            Text(
                                text = stringResource(R.string.scan_processing),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        }

        // Bottom white shutter control tray matching mockup
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 36.dp, vertical = 22.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Gallery Import button
                Surface(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .testTag("import_gallery_button")
                        .clickable(enabled = !uiState.isProcessingOcr) { launchGalleryPicker() },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Image,
                            contentDescription = stringResource(R.string.cd_import_gallery),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }

                // Cobalt Shutter button
                CobaltShutterButton(
                    enabled = hasCameraPermission && !cameraUnavailable && !uiState.isProcessingOcr,
                    onClick = ::triggerCapture,
                    modifier = Modifier.testTag("capture_card_button"),
                )

                // Flash Toggle button
                val flashAvailable = hasCameraPermission && !cameraUnavailable && hasFlashUnit
                val flashContainerColor = if (uiState.flashEnabled && flashAvailable) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainer
                }
                val flashIconTint = when {
                    !flashAvailable -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                    uiState.flashEnabled -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface
                }

                Surface(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .testTag("toggle_flash_button")
                        .clickable(enabled = flashAvailable && !uiState.isProcessingOcr) {
                            onToggleFlash()
                        },
                    shape = CircleShape,
                    color = flashContainerColor,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (flashAvailable) Icons.Rounded.Bolt else Icons.Rounded.FlashOff,
                            contentDescription = stringResource(R.string.cd_toggle_flash),
                            tint = flashIconTint,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CobaltShutterButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.92f else 1f,
        label = "shutterScale",
    )

    val ringColor = if (enabled) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline
    }

    Box(
        modifier = modifier
            .size(76.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(Color.Transparent)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        // Outer cobalt ring
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = CircleShape,
            color = Color.Transparent,
            border = BorderStroke(3.dp, ringColor),
        ) {}

        // Inner cobalt shutter circle with clean white breathing gap
        Surface(
            modifier = Modifier.size(62.dp),
            shape = CircleShape,
            color = ringColor,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.CameraAlt,
                    contentDescription = stringResource(R.string.cd_capture_card),
                    tint = Color.White.copy(alpha = 0.0f), // Keeps accessibility description while keeping pure cobalt circle from mockup
                )
            }
        }
    }
}

@Composable
private fun CameraPreviewSurface(
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    onCameraBound: (Camera, ImageCapture) -> Unit,
    onCameraFailed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var activeCamera by remember { mutableStateOf<Camera?>(null) }

    val previewView = remember(context) {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    DisposableEffect(lifecycleOwner, previewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val executor = ContextCompat.getMainExecutor(context)
        var providerRef: ProcessCameraProvider? = null

        val listener = Runnable {
            try {
                val cameraProvider = cameraProviderFuture.get()
                providerRef = cameraProvider

                val selector = when {
                    cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) -> {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    }
                    cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) -> {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    }
                    else -> {
                        onCameraFailed()
                        return@Runnable
                    }
                }

                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val capture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    selector,
                    preview,
                    capture,
                )
                activeCamera = camera
                onCameraBound(camera, capture)
            } catch (_: Exception) {
                onCameraFailed()
            }
        }

        cameraProviderFuture.addListener(listener, executor)

        onDispose {
            runCatching { providerRef?.unbindAll() }
        }
    }

    AndroidView(
        factory = {
            previewView.apply {
                setOnTouchListener { view, event ->
                    if (event.action == MotionEvent.ACTION_UP) {
                        val point = meteringPointFactory.createPoint(event.x, event.y)
                        val action = FocusMeteringAction.Builder(point).build()
                        runCatching { activeCamera?.cameraControl?.startFocusAndMetering(action) }
                        view.performClick()
                    }
                    true
                }
            }
        },
        modifier = modifier,
    )
}

@Composable
private fun BusinessCardFramingOverlay(
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier.graphicsLayer {
            compositingStrategy = CompositingStrategy.Offscreen
        },
    ) {
        val horizontalMargin = size.width * 0.08f
        val frameWidth = (size.width - horizontalMargin * 2f).coerceAtLeast(1f)
        val idealHeight = frameWidth / 1.586f
        val maxHeight = size.height * 0.56f
        val frameHeight = idealHeight.coerceAtMost(maxHeight)
        val adjustedWidth = if (idealHeight > maxHeight) frameHeight * 1.586f else frameWidth

        val left = (size.width - adjustedWidth) / 2f
        val top = (size.height - frameHeight) / 2f - size.height * 0.03f
        val frameRect = Rect(left, top, left + adjustedWidth, top + frameHeight)
        val frameCorner = 14.dp.toPx()

        // Dim area outside the card frame
        drawRect(color = Color.Black.copy(alpha = 0.30f))
        drawRoundRect(
            color = Color.Transparent,
            topLeft = frameRect.topLeft,
            size = frameRect.size,
            cornerRadius = CornerRadius(frameCorner, frameCorner),
            blendMode = BlendMode.Clear,
        )

        // Subtle crisp white border inside framing window
        drawRoundRect(
            color = Color.White.copy(alpha = 0.35f),
            topLeft = frameRect.topLeft,
            size = frameRect.size,
            cornerRadius = CornerRadius(frameCorner, frameCorner),
            style = Stroke(width = 1.dp.toPx()),
        )

        // Cobalt-blue rounded corner brackets matching the mockup
        val bracketLength = 26.dp.toPx()
        val bracketRadius = 10.dp.toPx()
        val outset = 6.dp.toPx()
        val stroke = Stroke(
            width = 4.dp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )

        val l = frameRect.left - outset
        val t = frameRect.top - outset
        val r = frameRect.right + outset
        val b = frameRect.bottom + outset

        // Top-left bracket
        drawPath(
            path = Path().apply {
                moveTo(l, t + bracketLength)
                lineTo(l, t + bracketRadius)
                quadraticTo(l, t, l + bracketRadius, t)
                lineTo(l + bracketLength, t)
            },
            color = CobaltPrimary,
            style = stroke,
        )

        // Top-right bracket
        drawPath(
            path = Path().apply {
                moveTo(r - bracketLength, t)
                lineTo(r - bracketRadius, t)
                quadraticTo(r, t, r, t + bracketRadius)
                lineTo(r, t + bracketLength)
            },
            color = CobaltPrimary,
            style = stroke,
        )

        // Bottom-left bracket
        drawPath(
            path = Path().apply {
                moveTo(l, b - bracketLength)
                lineTo(l, b - bracketRadius)
                quadraticTo(l, b, l + bracketRadius, b)
                lineTo(l + bracketLength, b)
            },
            color = CobaltPrimary,
            style = stroke,
        )

        // Bottom-right bracket
        drawPath(
            path = Path().apply {
                moveTo(r - bracketLength, b)
                lineTo(r - bracketRadius, b)
                quadraticTo(r, b, r, b - bracketRadius)
                lineTo(r, b - bracketLength)
            },
            color = CobaltPrimary,
            style = stroke,
        )
    }
}

@Composable
private fun CameraFallbackCard(
    title: String,
    body: String,
    primaryButtonText: String,
    onPrimaryClick: () -> Unit,
    onImportGallery: (() -> Unit)?,
    onEnterManually: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .padding(24.dp)
            .widthIn(max = 420.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp,
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(56.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.CameraAlt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onPrimaryClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("camera_fallback_primary_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text(text = primaryButtonText, style = MaterialTheme.typography.labelLarge)
            }
            if (onImportGallery != null) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onImportGallery,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("camera_fallback_gallery_button"),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PhotoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = stringResource(R.string.scan_import_photo),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            TextButton(
                onClick = onEnterManually,
                modifier = Modifier.testTag("camera_fallback_manual_button"),
            ) {
                Text(
                    text = stringResource(R.string.scan_manual_entry),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

private fun openAppSettings(context: Context) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null),
    ).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(intent) }
}
