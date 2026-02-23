# =============================================================================
# ProGuard / R8 rules for FTP Server app
# =============================================================================
#
# For more details, see:
#   https://developer.android.com/build/shrink-code
#   https://www.guardsquare.com/manual/configuration/usage

# -----------------------------------------------------------------------------
# Debugging: preserve source file names and line numbers in stack traces
# -----------------------------------------------------------------------------
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# -----------------------------------------------------------------------------
# Apache FTPServer & MINA
# -----------------------------------------------------------------------------
# FTPServer uses reflection for user management and command factories.
# MINA (the underlying NIO framework) uses reflection for session management.
-keep class org.apache.ftpserver.** { *; }
-keep class org.apache.mina.** { *; }
-dontwarn org.apache.ftpserver.**
-dontwarn org.apache.mina.**

# -----------------------------------------------------------------------------
# SLF4J logging
# -----------------------------------------------------------------------------
-dontwarn org.slf4j.**
-keep class org.slf4j.** { *; }

# -----------------------------------------------------------------------------
# Kotlin
# -----------------------------------------------------------------------------
# Keep Kotlin metadata for reflection (used by some libraries)
-keepattributes *Annotation*
-keepattributes RuntimeVisibleAnnotations
-keep class kotlin.Metadata { *; }

# -----------------------------------------------------------------------------
# Jetpack Compose
# -----------------------------------------------------------------------------
# Compose uses reflection for state management and recomposition.
# R8 full mode is generally Compose-aware, but these rules ensure safety.
-dontwarn androidx.compose.**

# -----------------------------------------------------------------------------
# ZXing (QR code generation)
# -----------------------------------------------------------------------------
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**

# -----------------------------------------------------------------------------
# MaterialKolor
# -----------------------------------------------------------------------------
-dontwarn com.materialkolor.**