package com.android.argosapp.data.repository

import com.android.argosapp.data.config.CloudinaryConfig
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import java.io.File
import java.io.IOException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class CloudinaryPhotoRepository(
    private val config: CloudinaryConfig = CloudinaryConfig.fromBuildConfig(),
) {
    suspend fun uploadProfilePhoto(file: File): String {
        if (!file.exists() || file.length() == 0L) {
            throw IOException("The captured profile photo is empty.")
        }

        return uploadWithCloudinarySdk(file)
    }

    private suspend fun uploadWithCloudinarySdk(file: File): String = suspendCancellableCoroutine { continuation ->
        var requestId: String? = null

        val callback = object: UploadCallback {
            override fun onStart(requestId: String) = Unit

            override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) = Unit

            override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                val secureUrl = resultData["secure_url"] as? String
                if (continuation.isActive) {
                    if (secureUrl.isNullOrBlank()) {
                        continuation.resumeWithException(IOException("Cloudinary did not return a secure image URL."))
                    } else {
                        continuation.resume(secureUrl)
                    }
                }
            }

            override fun onError(requestId: String, error: ErrorInfo) {
                if (continuation.isActive) {
                    continuation.resumeWithException(IOException(error.description ?: "Cloudinary upload failed."))
                }
            }

            override fun onReschedule(requestId: String, error: ErrorInfo) = Unit
        }

        requestId = MediaManager.get()
            .upload(file.absolutePath)
            .unsigned(config.uploadPreset)
            .callback(callback)
            .dispatch()

        continuation.invokeOnCancellation {
            requestId?.let(MediaManager.get()::cancelRequest)
        }
    }

}
