package com.android.argosapp.util.io

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File

/** Prepares camera capture requests that save the image to an app-owned file. */
class CameraManager(private val context: Context) {
    fun createSelfieCapture(): CameraCapture {
        val photoDirectory = File(context.cacheDir, PROFILE_PHOTOS_DIRECTORY)
        if (!photoDirectory.exists() && !photoDirectory.mkdirs()) {
            throw IllegalStateException("Could not create the profile photo directory.")
        }

        val photoFile = File.createTempFile("profile_selfie_", ".jpg", photoDirectory)
        return try {
            val photoUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.profilephotos",
                photoFile,
            )
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                putExtra(EXTRA_CAMERA_FACING, FRONT_CAMERA)
                putExtra(EXTRA_LENS_FACING_FRONT, FRONT_CAMERA)
                putExtra(EXTRA_USE_FRONT_CAMERA, true)
                clipData = ClipData.newRawUri("profile selfie", photoUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
            CameraCapture(intent, photoFile)
        } catch (error: Exception) {
            photoFile.delete()
            throw error
        }
    }

    data class CameraCapture(val intent: Intent, val photoFile: File)

    private companion object {
        const val PROFILE_PHOTOS_DIRECTORY = "profile_photos"
        const val EXTRA_CAMERA_FACING = "android.intent.extras.CAMERA_FACING"
        const val EXTRA_LENS_FACING_FRONT = "android.intent.extras.LENS_FACING_FRONT"
        const val EXTRA_USE_FRONT_CAMERA = "android.intent.extra.USE_FRONT_CAMERA"
        const val FRONT_CAMERA = 1
    }
}