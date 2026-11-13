plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

android {
    namespace = "br.com.morfy3d.app"
    compileSdk = 36
    defaultConfig {
        applicationId = "br.com.morfy3d.app"
        minSdk = 23
        targetSdk = 36
        versionCode = 2
        versionName = "1.1.0"
    }
    signingConfigs {
        val uploadStore = System.getenv("MORFY_KEYSTORE_PATH")
        if (!uploadStore.isNullOrBlank()) {
            create("release") {
                storeFile = file(uploadStore)
                storePassword = System.getenv("MORFY_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("MORFY_KEY_ALIAS")
                keyPassword = System.getenv("MORFY_KEY_PASSWORD")
            }
        }
    }
    buildTypes {
        getByName("release") {
            val hasUploadKey = !System.getenv("MORFY_KEYSTORE_PATH").isNullOrBlank()
            if (hasUploadKey) signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(platform("com.google.firebase:firebase-bom:34.5.0"))
    implementation("com.google.firebase:firebase-messaging")
    implementation("androidx.core:core-ktx:1.17.0")
}
