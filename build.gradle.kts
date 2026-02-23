// =============================================================================
// Root build.gradle.kts — Central plugin version management
// =============================================================================
//
// ENVIRONMENT SETUP (before first build):
//   1. Install Android SDK Command Line Tools:
//      https://developer.android.com/studio#command-line-tools-only
//
//   2. Set ANDROID_HOME and update PATH:
//      export ANDROID_HOME="$HOME/android-sdk"
//      export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"
//
//   3. Install required SDK packages:
//      sdkmanager --install "platform-tools" "platforms;android-34" "build-tools;34.0.0"
//      sdkmanager --licenses
//
//   Java and Gradle are managed automatically:
//     - JDK 21 (Adoptium) → auto-downloaded via Foojay Toolchain Resolver
//       (see gradle/gradle-daemon-jvm.properties)
//     - Gradle 9.2.1 → auto-downloaded via the Gradle Wrapper
//       (see gradle/wrapper/gradle-wrapper.properties)
//
// =============================================================================
//
// WHY "apply false"?
//   Plugins are declared here with their versions so that every sub-module
//   (e.g., :app) uses the exact same version. "apply false" means:
//   "Download and register this plugin version, but don't activate it on the
//   root project itself — only sub-modules that explicitly apply it will use it."
//
//   Sub-modules then apply the plugin without specifying a version:
//     plugins { id("com.android.application") }  // version inherited from here
//
// =============================================================================

plugins {
    // Android Gradle Plugin (AGP) — the core plugin that knows how to compile
    // Android resources, merge manifests, run R8/ProGuard, sign APKs, and
    // package everything into an .apk or .aab file.
    // Docs: https://developer.android.com/build
    id("com.android.application") version "9.0.1" apply false

    // Kotlin Android Plugin — adds Kotlin language support to the Android build.
    // Compiles .kt files, configures Kotlin-specific options (jvmTarget, etc.),
    // and integrates with the Android build pipeline.
    // Docs: https://kotlinlang.org/docs/gradle-configure-project.html
    id("org.jetbrains.kotlin.android") version "2.2.10" apply false

    // Compose Compiler Plugin — transforms @Composable functions into the
    // underlying code that the Jetpack Compose runtime can execute.
    // Since Kotlin 2.0, this is a standalone plugin (no longer bundled in the
    // Kotlin compiler), allowing independent version management.
    // Docs: https://developer.android.com/develop/ui/compose/compiler
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10" apply false
}