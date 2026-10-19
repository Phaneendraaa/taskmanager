plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.example.taskmanager"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.habitanvil.app"
        minSdk = 26
        targetSdk = 36
        // CI passes the run number, so every build is newer than the last and installs as an update.
        val run = (System.getenv("GITHUB_RUN_NUMBER") ?: "0").toIntOrNull() ?: 0
        versionCode = 100 + run
        versionName = "5.0.$run"
    }

    // Fixed debug key so every build is signed the same and installs as an update.
    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    // Upload key for Google Play. CI decrypts it and passes the path and password through the
    // environment; without them (local or debug builds) the release type falls back to the debug key.
    val uploadPath = System.getenv("UPLOAD_KEYSTORE_PATH")
    val uploadPassword = System.getenv("UPLOAD_KEY_PASSWORD")
    signingConfigs {
        if (uploadPath != null && uploadPassword != null) {
            create("release") {
                storeFile = file(uploadPath)
                storePassword = uploadPassword
                keyAlias = "upload"
                keyPassword = uploadPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }

    lint {
        abortOnError = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    debugImplementation("androidx.compose.ui:ui-tooling")

    // Screenshot previews rendered on the CI machine (see PreviewShots.kt).
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("androidx.test:core:1.6.1")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
