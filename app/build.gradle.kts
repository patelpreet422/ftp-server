// =============================================================================
// App-level build.gradle.kts — Android application build configuration
// =============================================================================
//
// This module produces the final APK/AAB that gets installed on devices.
// Plugin versions are inherited from the root build.gradle.kts (see "apply false" pattern).
//
// REQUIRED SDK PACKAGES (install via sdkmanager if not already present):
//   sdkmanager --install "platforms;android-34" "build-tools;34.0.0"
//
// =============================================================================

plugins {
    // Applies the Android Application plugin (AGP) to this module.
    // This is what turns a plain Gradle module into an Android app, enabling:
    //   - APK/AAB packaging
    //   - Resource compilation (R.java generation)
    //   - Manifest merging
    //   - Code shrinking via R8/ProGuard
    //   - APK signing
    // Version (9.0.1) is inherited from root build.gradle.kts.
    //
    // NOTE: Kotlin compilation is handled by AGP's built-in Kotlin support
    // (android.builtInKotlin=true), so a separate kotlin.android plugin is NOT needed.
    id("com.android.application")

    // Applies the Compose Compiler plugin which transforms @Composable functions
    // into optimized runtime code for Jetpack Compose UI framework.
    // Version (2.2.10) is inherited from root build.gradle.kts.
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    // Unique application identifier used by the Play Store and on-device package manager.
    namespace = "com.ftpserver.app"

    // compileSdk: The Android API level used to compile the app. This determines which
    // Android framework APIs are available at compile time. You must have
    // "platforms;android-34" installed in your SDK.
    compileSdk = 34

    defaultConfig {
        // applicationId: The unique identifier for the app on a device and in the Play Store.
        applicationId = "com.ftpserver.app"

        // minSdk: The lowest Android API level this app supports.
        // Devices running below API 24 (Android 7.0) cannot install this app.
        minSdk = 24

        // targetSdk: The API level the app is designed and tested against.
        // Android uses this to apply backward-compatibility behaviors for older apps.
        targetSdk = 34

        versionCode = 2
        versionName = "1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file("release-key.jks")
            storePassword = "ftpserver123"
            keyAlias = "release-key"
            keyPassword = "ftpserver123"
        }
    }

    buildTypes {
        release {
            // isMinifyEnabled: When true, R8 shrinks, obfuscates, and optimizes the code.
            // This removes unused code, renames classes/methods, and optimizes bytecode.
            isMinifyEnabled = true

            // isShrinkResources: When true, removes unused resources (drawables, layouts, etc.)
            // that are not referenced by the code after minification.
            isShrinkResources = true

            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        // Staging: a release-like build with debug signing for internal testing.
        // Uses the same ProGuard/R8 config as release but signed with debug key
        // so it can be installed side-by-side with the release build on test devices.
        create("staging") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            applicationIdSuffix = ".staging"
            versionNameSuffix = "-staging"
        }
    }

    // Java compatibility settings for the compiled bytecode.
    // Using Java 8 bytecode target for maximum Android device compatibility.
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    buildFeatures {
        // Enables Jetpack Compose UI toolkit for this module.
        // Without this, @Composable functions and Compose dependencies won't work.
        compose = true
    }

    // Exclude duplicate license files from dependencies to avoid packaging conflicts.
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/LICENSE"
            excludes += "/META-INF/LICENSE.txt"
            excludes += "/META-INF/NOTICE"
            excludes += "/META-INF/NOTICE.txt"
        }
    }
}

// Configure Kotlin JVM target for all Kotlin compilation tasks.
// This replaces the legacy `android { kotlinOptions { jvmTarget = "1.8" } }` syntax
// which is not supported in the new AGP DSL.
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8)
    }
}

// Custom APK naming: outputs "ftp-server-v1.1.0-release.apk" and
// "ftp-server-v1.1.0-debug.apk" instead of the default "app-release.apk".
//
// Uses the standard Gradle base plugin archivesName property. AGP appends the
// build type suffix (-release, -debug) automatically following Gradle conventions.
base {
    archivesName = "ftp-server-v${android.defaultConfig.versionName}"
}

dependencies {
    // =========================================================================
    // AndroidX Core & Lifecycle
    // =========================================================================

    // core-ktx: Kotlin extensions for Android framework APIs (e.g., SharedPreferences,
    // Context, Bundle). Provides idiomatic Kotlin wrappers around common Android operations.
    implementation("androidx.core:core-ktx:1.12.0")

    // lifecycle-runtime-ktx: Kotlin coroutine support for Android lifecycle-aware components.
    // Enables lifecycleScope for launching coroutines tied to Activity/Fragment lifecycle.
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")

    // activity-compose: Bridges traditional Android Activities with Jetpack Compose.
    // Provides setContent {} to host Compose UI inside an Activity.
    implementation("androidx.activity:activity-compose:1.8.2")

    // =========================================================================
    // Jetpack Compose UI
    // =========================================================================

    // compose-bom: Bill of Materials — ensures all Compose libraries use compatible versions.
    // By declaring the BOM, individual Compose dependencies below don't need explicit versions.
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))

    // Core Compose UI toolkit: layout, drawing, input handling, and text rendering.
    implementation("androidx.compose.ui:ui")

    // Compose graphics layer: Canvas, ImageBitmap, and graphics primitives.
    implementation("androidx.compose.ui:ui-graphics")

    // Compose preview support: enables @Preview annotations for IDE previews.
    implementation("androidx.compose.ui:ui-tooling-preview")

    // Material Design 3 components: Buttons, Cards, TopAppBar, NavigationBar, etc.
    // This is the primary UI component library for the app's visual design.
    implementation("androidx.compose.material3:material3")

    // Extended Material Icons: provides a comprehensive set of Material Design icons
    // beyond the default set (e.g., ContentCopy, Wifi, Settings, etc.).
    implementation("androidx.compose.material:material-icons-extended")

    // ConstraintLayout for Compose: allows complex layouts with flat view hierarchies
    // using constraint-based positioning (similar to XML ConstraintLayout).
    implementation("androidx.constraintlayout:constraintlayout-compose:1.0.1")

    // =========================================================================
    // FTP Server
    // =========================================================================

    // Apache FTPServer: embedded FTP server library that handles the FTP protocol,
    // user authentication, file system access, and client connections.
    // This is the core library that powers the app's FTP functionality.
    implementation("org.apache.ftpserver:ftpserver-core:1.2.0")

    // SLF4J Android: logging facade for Apache FTPServer. Routes FTPServer's
    // log output to Android's logcat so you can see server activity in logs.
    implementation("org.slf4j:slf4j-android:1.7.36")

    // =========================================================================
    // Theming & Visual
    // =========================================================================

    // MaterialKolor: generates dynamic Material Design 3 color schemes from a
    // seed color at runtime. Used for the app's dynamic theming.
    implementation("com.materialkolor:material-kolor:2.0.0")

    // ZXing Core: QR code generation library. Used to generate QR codes
    // containing the FTP server connection URL for easy mobile scanning.
    implementation("com.google.zxing:core:3.5.2")

    // =========================================================================
    // Debug-only dependencies
    // =========================================================================

    // Compose UI Tooling: provides layout inspector and interactive preview support.
    // Only included in debug builds to avoid bloating the release APK.
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Compose Test Manifest: required for Compose UI tests in debug builds.
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // =========================================================================
    // Testing
    // =========================================================================

    // JUnit 4: unit testing framework for local (JVM) tests in src/test/.
    testImplementation("junit:junit:4.13.2")

    // AndroidX Test JUnit: Android-specific JUnit extensions for instrumented tests.
    androidTestImplementation("androidx.test.ext:junit:1.1.5")

    // Espresso: UI testing framework for Android instrumented tests.
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    // Compose BOM for test dependencies — ensures test Compose libs match app versions.
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.10.01"))

    // Compose UI Test JUnit4: provides ComposeTestRule for writing Compose UI tests.
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}