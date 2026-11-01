package com.android.argosapp.data.config

import com.android.argosapp.BuildConfig

data class CloudinaryConfig(
    val cloudName: String,
    val uploadPreset: String,
) {
    companion object {
        fun fromBuildConfig() = CloudinaryConfig(
            cloudName = BuildConfig.CLOUDINARY_CLOUD_NAME,
            uploadPreset = BuildConfig.CLOUDINARY_UPLOAD_PRESET,
        )
    }
}
