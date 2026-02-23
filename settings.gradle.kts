// =============================================================================
// settings.gradle.kts — Project structure and repository configuration
// =============================================================================

// pluginManagement: Configures where Gradle looks for build plugins.
// These repositories are searched (in order) when resolving plugin dependencies
// declared in build.gradle.kts files.
pluginManagement {
    repositories {
        google()            // Google's Maven repo — hosts AGP, AndroidX, etc.
        mavenCentral()      // Central Maven repo — hosts most open-source libraries.
        gradlePluginPortal() // Gradle Plugin Portal — hosts Gradle-specific plugins.
    }
}

plugins {
    // Foojay Toolchain Resolver: automatically downloads the exact JDK version
    // required by this project (Adoptium 21) if it's not already installed.
    // The target JDK version and download URLs are defined in:
    //   gradle/gradle-daemon-jvm.properties
    //
    // This means developers do NOT need to install Java manually.
    // On first build, Gradle will download the correct JDK for their OS/architecture.
    // Docs: https://github.com/gradle/foojay-toolchains
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

// dependencyResolutionManagement: Configures where Gradle looks for library dependencies.
// FAIL_ON_PROJECT_REPOS: Ensures that repositories are ONLY defined here (centrally),
// not inside individual module build.gradle.kts files. This enforces consistency.
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()       // AndroidX, Material, Play Services, etc.
        mavenCentral() // Apache FTPServer, ZXing, SLF4J, MaterialKolor, etc.
    }
}

// The display name of the root project.
rootProject.name = "FTP Server"

// Includes the :app module (the only module in this project).
// This tells Gradle to look for app/build.gradle.kts and treat it as a sub-project.
include(":app")