plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.velasmart.deckramobile"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.velasmart.deckramobile"
        minSdk = 26
        targetSdk = 35
        versionCode = providers.gradleProperty("deckraVersionCode").orElse("1").get().toInt()
        versionName = providers.gradleProperty("deckraVersionName").orElse("0.1.0").get()
    }
    signingConfigs {
        val releaseKeyPath = System.getenv("DECKRA_KEYSTORE_PATH")
        if (!releaseKeyPath.isNullOrBlank()) {
            create("release") {
                storeFile = file(releaseKeyPath)
                storePassword = System.getenv("DECKRA_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("DECKRA_KEY_ALIAS")
                keyPassword = System.getenv("DECKRA_KEY_PASSWORD")
            }
        }
    }
    buildTypes {
        getByName("release") {
            val hasSigning = !System.getenv("DECKRA_KEYSTORE_PATH").isNullOrBlank()
            if (hasSigning) signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
kotlin {
    jvmToolchain(17)
}
dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-ktx:1.10.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}
