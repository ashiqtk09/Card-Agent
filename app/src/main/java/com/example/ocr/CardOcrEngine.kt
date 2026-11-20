package com.example.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class CardOcrEngine(private val appContext: Context) {

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    suspend fun recognizeFromFile(
        imageFile: File,
        cropToCardFrame: Boolean = false,
    ): ExtractedCardData = withContext(Dispatchers.IO) {
        try {
            val bitmap = decodeOrientedBitmapFromFile(imageFile)
                ?: return@withContext ExtractedCardData(
                    ocrFailed = true,
                    failureReason = "Unable to decode captured image.",
                    imagePath = imageFile.absolutePath,
                )

            val processedBitmap = if (cropToCardFrame) {
                cropCenterCardRegion(bitmap)
            } else {
                bitmap
            }

            val previewFile = savePreviewBitmap(processedBitmap)
            if (processedBitmap !== bitmap) {
                bitmap.recycle()
            }

            try {
                runMlKitRecognition(processedBitmap, previewFile.absolutePath)
            } finally {
                processedBitmap.recycle()
            }
        } catch (e: Exception) {
            ExtractedCardData(
                ocrFailed = true,
                failureReason = e.localizedMessage ?: "Text recognition failed.",
                imagePath = imageFile.takeIf { it.exists() }?.absolutePath,
            )
        }
    }

    suspend fun recognizeFromUri(uri: Uri): ExtractedCardData = withContext(Dispatchers.IO) {
        try {
            val bitmap = decodeOrientedBitmapFromUri(uri)
                ?: return@withContext ExtractedCardData(
                    ocrFailed = true,
                    failureReason = "Unable to open selected photo.",
                )

            val previewFile = savePreviewBitmap(bitmap)
            try {
                runMlKitRecognition(bitmap, previewFile.absolutePath)
            } finally {
                bitmap.recycle()
            }
        } catch (e: Exception) {
            ExtractedCardData(
                ocrFailed = true,
                failureReason = e.localizedMessage ?: "Text recognition failed.",
            )
        }
    }

    private suspend fun runMlKitRecognition(
        bitmap: Bitmap,
        previewPath: String,
    ): ExtractedCardData {
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        val visionText = recognizer.process(inputImage).awaitResult()
        val lines = extractSortedLines(visionText)
        return BusinessCardParser.parse(lines).copy(imagePath = previewPath)
    }

    private fun extractSortedLines(visionText: Text): List<RecognizedLine> {
        val recognized = mutableListOf<RecognizedLine>()
        for (block in visionText.textBlocks) {
            for (line in block.lines) {
                val box = line.boundingBox
                recognized.add(
                    RecognizedLine(
                        text = line.text,
                        heightPx = box?.height() ?: 0,
                        topPx = box?.top ?: 0,
                        leftPx = box?.left ?: 0,
                        confidence = line.confidence,
                    ),
                )
            }
        }
        return recognized.sortedWith(compareBy<RecognizedLine> { it.topPx }.thenBy { it.leftPx })
    }

    private fun decodeOrientedBitmapFromFile(file: File): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        val sampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, MAX_DIMENSION_PX)
        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val raw = BitmapFactory.decodeFile(file.absolutePath, decodeOptions) ?: return null
        val exif = runCatching { ExifInterface(file.absolutePath) }.getOrNull()
        return applyExifOrientation(raw, exif)
    }

    private fun decodeOrientedBitmapFromUri(uri: Uri): Bitmap? {
        val resolver = appContext.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
        } ?: return null

        val sampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, MAX_DIMENSION_PX)
        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val raw = resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions)
        } ?: return null

        val exif = resolver.openInputStream(uri)?.use { stream ->
            runCatching { ExifInterface(stream) }.getOrNull()
        }
        return applyExifOrientation(raw, exif)
    }

    private fun applyExifOrientation(bitmap: Bitmap, exif: ExifInterface?): Bitmap {
        val orientation = exif?.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL,
        ) ?: ExifInterface.ORIENTATION_NORMAL

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
            else -> return bitmap
        }

        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated !== bitmap) {
            bitmap.recycle()
        }
        return rotated
    }

    private fun cropCenterCardRegion(source: Bitmap): Bitmap {
        // Gently focus on the framed business card area while retaining generous padding so edges aren't cut
        val targetWidth = (source.width * 0.90f).toInt().coerceAtLeast(1)
        val desiredHeight = (targetWidth / 1.45f).toInt()
        val targetHeight = desiredHeight.coerceAtMost((source.height * 0.72f).toInt()).coerceAtLeast(1)
        val startX = ((source.width - targetWidth) / 2).coerceAtLeast(0)
        val startY = ((source.height - targetHeight) / 2).coerceAtLeast(0)
        return Bitmap.createBitmap(source, startX, startY, targetWidth, targetHeight)
    }

    private fun savePreviewBitmap(bitmap: Bitmap): File {
        val dir = File(appContext.cacheDir, "scanned_previews").apply { mkdirs() }
        // Keep cache tidy by pruning previews older than 1 hour
        val cutoff = System.currentTimeMillis() - 3_600_000L
        dir.listFiles()?.forEach { file ->
            if (file.lastModified() < cutoff) file.delete()
        }
        val output = File(dir, "preview_${System.currentTimeMillis()}.jpg")
        FileOutputStream(output).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 88, fos)
        }
        return output
    }

    private fun calculateInSampleSize(width: Int, height: Int, maxDim: Int): Int {
        var sample = 1
        val largest = maxOf(width, height)
        while (largest / sample > maxDim) {
            sample *= 2
        }
        return sample
    }

    fun close() {
        runCatching { recognizer.close() }
    }

    companion object {
        private const val MAX_DIMENSION_PX = 2048
    }
}

private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { result ->
        if (continuation.isActive) {
            continuation.resume(result)
        }
    }
    addOnFailureListener { exception ->
        if (continuation.isActive) {
            continuation.resumeWithException(exception)
        }
    }
    addOnCanceledListener {
        if (continuation.isActive) {
            continuation.cancel()
        }
    }
}
