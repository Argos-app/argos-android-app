import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
    id("com.google.gms.google-services")
}

val localProperties = Properties()
rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use {
    localProperties.load(it)
}
fun escapeBuildConfigValue(value: String) = value
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")

val cloudinaryCloudName = escapeBuildConfigValue(localProperties.getProperty("CLOUDINARY_CLOUD_NAME", ""))
val cloudinaryUploadPreset = escapeBuildConfigValue(localProperties.getProperty("CLOUDINARY_UPLOAD_PRESET", ""))
val googleMapsApiKey = escapeBuildConfigValue(localProperties.getProperty("MAPS_API_KEY", ""))

android {
    namespace = "com.android.argosapp"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.android.argosapp"
        minSdk = 30
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        manifestPlaceholders["MAPS_API_KEY"] = googleMapsApiKey

        buildConfigField("String", "MAPS_API_KEY", "\"$googleMapsApiKey\"")
        buildConfigField("String", "CLOUDINARY_CLOUD_NAME", "\"$cloudinaryCloudName\"")
        buildConfigField("String", "CLOUDINARY_UPLOAD_PRESET", "\"$cloudinaryUploadPreset\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
}

dependencies {
    implementation(platform("com.google.firebase:firebase-bom:34.17.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.github.bumptech.glide:glide:5.0.9")
    implementation("com.cloudinary:cloudinary-android:3.1.3")
    implementation(libs.androidx.room3.common)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation(libs.material)
    implementation(libs.play.services.maps)
    implementation(libs.play.services.location)
    implementation(libs.places)
    implementation(libs.room3.runtime)
    implementation(libs.sqlite.framework)
    ksp(libs.room3.compiler)
    implementation(libs.androidx.core.ktx)
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("androidx.activity:activity-ktx:1.13.0")
    implementation("androidx.fragment:fragment-ktx:1.8.9")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.4")
    implementation(libs.timber)
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.firebase:firebase-messaging")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
