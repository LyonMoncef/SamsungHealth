---
type: code-source
language: kotlin
file_path: android-app/app/build.gradle.kts
git_blob: 17ccea6ae24e77beca222ee83bae180677f10de1
last_synced: '2026-05-20T16:30:46Z'
loc: 135
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/build.gradle.kts

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/build.gradle.kts`](../../../android-app/app/build.gradle.kts).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
    id("app.cash.paparazzi")
}

configurations.all {
    resolutionStrategy {
        force("org.hamcrest:hamcrest:2.2")
        force("org.hamcrest:hamcrest-core:2.2")
        force("org.hamcrest:hamcrest-library:2.2")
    }
}

android {
    namespace = "fr.datasaillance.nightfall"
    compileSdk = 35

    defaultConfig {
        applicationId = "fr.datasaillance.nightfall"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "4.0.0"
        buildConfigField("String", "DEFAULT_BACKEND_URL", "\"https://sh-dev.datasaillance.fr\"")
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
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
        buildConfig = true
    }

    flavorDimensions += "rendering"
    productFlavors {
        create("webview") {
            dimension = "rendering"
        }
        create("native") {
            dimension = "rendering"
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.5")

    // Security / Crypto
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Local DB — Room + SQLCipher (Phase A local-first migration)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    implementation("net.zetetic:android-database-sqlcipher:4.5.4")
    implementation("androidx.sqlite:sqlite-ktx:2.4.0")

    // Retrofit + kotlinx-serialization
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")

    // Logging
    implementation("com.jakewharton.timber:timber:5.0.1")

    // Browser (Custom Tabs for OAuth)
    implementation("androidx.browser:browser:1.8.0")

    // Core
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")

    // OSMDroid — carte OpenStreetMap embedded (Phase C_gps, raster tiles, no API key)
    implementation("org.osmdroid:osmdroid-android:6.1.20")

    // Test dependencies
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.hamcrest:hamcrest:2.2")
    testImplementation("app.cash.paparazzi:paparazzi:1.3.4")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.3.1")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("org.robolectric:robolectric:4.12.2")
    testImplementation("androidx.navigation:navigation-testing:2.8.5")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("androidx.test:core:1.6.1")
    testImplementation("androidx.test.ext:junit:1.2.1")
    testImplementation("androidx.room:room-testing:2.6.1")

    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
```

---

## Appendix — symbols & navigation *(auto)*
