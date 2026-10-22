package com.android.argosapp.data.repository

import android.content.Context
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

        return upload(file)
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
