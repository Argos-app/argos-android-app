package com.android.argosapp.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import com.android.argosapp.data.config.CloudinaryConfig
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.cloudinary.android.preprocess.BitmapDecoder
import com.cloudinary.android.preprocess.BitmapEncoder
import com.cloudinary.android.preprocess.DimensionsValidator
import com.cloudinary.android.preprocess.ImagePreprocessChain
import com.cloudinary.android.preprocess.Limit
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class CloudinaryPhotoRepository(
    context: Context,
    private val config: CloudinaryConfig = CloudinaryConfig.fromBuildConfig(),
) {
    private val appContext = context.applicationContext

    suspend fun uploadProfilePhoto(file: File): String {
        if (!file.exists() || file.length() == 0L) {
            throw IOException("The captured profile photo is empty.")
        }

        val uprightFile = normalizeOrientation(file)
        return try {
            upload(uprightFile)
        } finally {
            if (uprightFile != file) uprightFile.delete()
        }
    }

    private fun normalizeOrientation(file: File): File {
        val orientation = ExifInterface(file.absolutePath).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL,
        )
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                matrix.setRotate(180f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.setRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.setRotate(-90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(-90f)
            else -> return file
        }

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        val largestDimension = maxOf(bounds.outWidth, bounds.outHeight)
        if (largestDimension <= 0) throw IOException("Could not decode the captured profile photo.")
        var sampleSize = 1
        while (largestDimension / sampleSize > MAX_SIZE) sampleSize *= 2
        val bitmap = BitmapFactory.decodeFile(
            file.absolutePath,
            BitmapFactory.Options().apply { inSampleSize = sampleSize },
        ) ?: throw IOException("Could not decode the captured profile photo.")
        val uprightBitmap = try {
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (error: Exception) {
            bitmap.recycle()
            throw error
        }
        val uprightFile = try {
            File.createTempFile("profile_photo_upright_", ".jpg", appContext.cacheDir)
        } catch (error: Exception) {
            if (uprightBitmap !== bitmap) uprightBitmap.recycle()
            bitmap.recycle()
            throw error
        }
        try {
            FileOutputStream(uprightFile).use { output ->
                if (!uprightBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)) {
                    throw IOException("Could not normalize the captured profile photo orientation.")
                }
            }
            return uprightFile
        } catch (error: Exception) {
            uprightFile.delete()
            throw error
        } finally {
            if (uprightBitmap !== bitmap) uprightBitmap.recycle()
            bitmap.recycle()
        }
    }

    private suspend fun upload(file: File): String {

        return suspendCancellableCoroutine { continuation ->

            var requestId: String? = null
            val callback = object : UploadCallback {
                override fun onStart(requestId: String) = Unit
                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) = Unit

                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                    val secureUrl = resultData["secure_url"] as? String
                    val width = (resultData["width"] as? Number)?.toInt()
                    val height = (resultData["height"] as? Number)?.toInt()
                    if (!continuation.isActive) return

                    if (
                        secureUrl.isNullOrBlank() ||
                        width == null || height == null ||
                        width !in MIN_SIZE..MAX_SIZE || height !in MIN_SIZE..MAX_SIZE
                    ) {
                        continuation.resumeWithException(
                            IOException("Cloudinary did not store the profile photo within 1024x1024."),
                        )
                    } else {
                        continuation.resume(secureUrl)
                    }
                }

                override fun onError(requestId: String, error: ErrorInfo) {
                    if (continuation.isActive) {
                        continuation.resumeWithException(
                            IOException(error.description ?: "Cloudinary upload failed."),
                        )
                    }
                }

                override fun onReschedule(requestId: String, error: ErrorInfo) = Unit
            }

            requestId = MediaManager.get()
                .upload(file.absolutePath)
                .unsigned(config.uploadPreset)
                .preprocess(
                    ImagePreprocessChain()
                        .loadWith(BitmapDecoder(MAX_SIZE, MAX_SIZE))
                        .addStep(Limit(MAX_SIZE, MAX_SIZE))
                        .addStep(DimensionsValidator(MIN_SIZE, MIN_SIZE, MAX_SIZE, MAX_SIZE))
                        .saveWith(BitmapEncoder(BitmapEncoder.Format.JPEG, JPEG_QUALITY)),
                )
                .callback(callback)
                .dispatch(appContext)

            continuation.invokeOnCancellation {
                requestId?.let(MediaManager.get()::cancelRequest)
            }
        }
    }

    private companion object {
        const val MIN_SIZE = 10
        const val MAX_SIZE = 1024
        const val JPEG_QUALITY = 90
    }
}
