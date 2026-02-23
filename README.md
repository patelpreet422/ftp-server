# FTP Server for Android

A beautiful and simple FTP server application for Android with a modern UI built using Jetpack Compose.

## Features

- ✨ Beautiful modern UI with dark theme
- 🎯 Large circular button to start/stop the server
- 🌊 Animated wavy effect around the button when server is running
- 📋 Copyable connection details (IP, Port, Username, Password)
- 🔒 Secure foreground service for reliable operation
- 📱 Material Design 3 components
- 🎨 Gradient backgrounds and smooth animations

## Screenshots

The app features:
- A central circular button that toggles between START and STOP states
- When running, animated wavy circles pulse around the button
- Connection details card showing IP address, port, username, and password
- Each detail can be copied individually by tapping
- A "Copy Full URL" button to copy the complete FTP connection string

## Technical Details

### Server Configuration
- **Default Port**: 2221
- **Default Username**: android
- **Default Password**: android123
- **Root Directory**: App's external files directory (`/ftp_root`)

### Technologies Used
- **Language**: Kotlin 2.2.10
- **UI Framework**: Jetpack Compose with Material 3 (Compose Compiler Plugin: `org.jetbrains.kotlin.plugin.compose`)
- **FTP Library**: Apache FTPServer 1.2.0
- **Android Gradle Plugin**: 9.0.1
- **Minimum SDK**: 24 (Android 7.0)
- **Target SDK**: 34 (Android 14)
- **Compile SDK**: 34

### Build Runtime Versions

| Component | Version | Managed By |
|---|---|---|
| **Java (JDK)** | Adoptium (Eclipse Temurin) 21 | Auto-downloaded by Gradle via [Foojay Toolchain Resolver](https://github.com/gradle/foojay-toolchains) (`gradle/gradle-daemon-jvm.properties`) |
| **Gradle** | 9.2.1 | Auto-downloaded by the [Gradle Wrapper](https://docs.gradle.org/current/userguide/gradle_wrapper.html) (`gradle/wrapper/gradle-wrapper.properties`) |
| **Android SDK** | API 34 (Build Tools 34.x) | Must be installed manually (see setup below) |
| **Kotlin** | 2.2.10 | Managed by Gradle plugin (`build.gradle.kts`) |
| **AGP** | 9.0.1 | Managed by Gradle plugin (`build.gradle.kts`) |

> **Note:** You do **not** need to install Java or Gradle manually. The project is configured to automatically download exact pinned versions of both. Only the Android SDK requires manual setup.

---

## 🛠 Development Setup (CLI / VS Code)

This guide gets you from a bare machine to a running build using only the command line. No Android Studio required.

### Step 1: Install Android SDK Command Line Tools

1.  **Download** the "Command line tools only" package for your OS from the official Android developer site:
    [https://developer.android.com/studio#command-line-tools-only](https://developer.android.com/studio#command-line-tools-only)

2.  **Create a directory** for the Android SDK and extract the tools into it:

    ```bash
    # macOS / Linux
    mkdir -p ~/android-sdk/cmdline-tools
    unzip commandlinetools-*.zip -d ~/android-sdk/cmdline-tools
    mv ~/android-sdk/cmdline-tools/cmdline-tools ~/android-sdk/cmdline-tools/latest
    ```

    ```powershell
    # Windows (PowerShell)
    mkdir $env:USERPROFILE\android-sdk\cmdline-tools
    Expand-Archive commandlinetools-*.zip -DestinationPath $env:USERPROFILE\android-sdk\cmdline-tools
    Rename-Item $env:USERPROFILE\android-sdk\cmdline-tools\cmdline-tools latest
    ```

3.  **Set the `ANDROID_HOME` environment variable** and add tools to your `PATH`:

    ```bash
    # macOS / Linux — add to ~/.zshrc, ~/.bashrc, or ~/.profile
    export ANDROID_HOME="$HOME/android-sdk"
    export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"
    ```

    ```powershell
    # Windows (PowerShell) — run once, or add to your profile
    [System.Environment]::SetEnvironmentVariable("ANDROID_HOME", "$env:USERPROFILE\android-sdk", "User")
    $env:ANDROID_HOME = "$env:USERPROFILE\android-sdk"
    $env:PATH = "$env:ANDROID_HOME\cmdline-tools\latest\bin;$env:ANDROID_HOME\platform-tools;$env:PATH"
    ```

4.  **Reload your shell** (or open a new terminal) and verify:
    ```bash
    sdkmanager --version
    ```

### Step 2: Install Required SDK Packages

Accept the licenses and install the platform and build tools needed by this project:

```bash
sdkmanager --install "platform-tools" "platforms;android-34" "build-tools;34.0.0"
sdkmanager --licenses   # Accept all licenses when prompted
```

### Step 3: Clone the Repository

```bash
git clone https://github.com/patelpreet422/ftp-server.git
cd ftp-server
```

### Step 4: Build the Project

```bash
./gradlew assembleDebug
```

**What happens automatically on first run:**
1.  The **Gradle Wrapper** (`gradlew`) downloads **Gradle 9.2.1** (defined in `gradle/wrapper/gradle-wrapper.properties`).
2.  Gradle reads `gradle/gradle-daemon-jvm.properties` and the **Foojay Toolchain Resolver** automatically downloads **Adoptium JDK 21** for your OS/architecture.
3.  Gradle syncs the project and compiles the app.

You do **not** need to install Java or Gradle yourself. Everything is pinned and reproducible.

### Step 5: Install & Run on a Device

1.  **Connect** an Android device via USB (with USB Debugging enabled) or start an emulator.

2.  **Verify** the device is detected:
    ```bash
    adb devices
    ```

3.  **Install** the debug APK:
    ```bash
    ./gradlew installDebug
    ```

4.  **Launch** the app from the device, or via adb:
    ```bash
    adb shell am start -n com.ftpserver.app/.MainActivity
    ```

### Build Outputs

| Variant | Path |
|---|---|
| Debug | `app/build/outputs/apk/debug/ftp-server-v1.1.0-debug.apk` |
| Release | `app/build/outputs/apk/release/ftp-server-v1.1.0.apk` |

### Optional: VS Code Setup

For a comfortable editing experience without Android Studio:
- Install the [Kotlin](https://marketplace.visualstudio.com/items?itemName=mathiasfrohlich.Kotlin) language extension.
- Install [Gradle for Java](https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-gradle) for task running and dependency management.

---

<details>
<summary><strong>Alternative: Android Studio Setup</strong></summary>

If you prefer using Android Studio:

1. Open Android Studio.
2. Select **"Open an Existing Project"**.
3. Navigate to the cloned `ftp-server` directory and click **"OK"**.
4. Wait for Gradle sync to complete (Java and Gradle are downloaded automatically).
5. Click the **Run** button or press **Shift+F10**.

</details>

---

## How to Use

1. **Launch the app** on your Android device
2. **Tap the START button** to start the FTP server
3. **Note the connection details** displayed on the screen:
   - IP Address (your device's local network IP)
   - Port (default: 2221)
   - Username (default: android)
   - Password (default: android123)
4. **Connect from any FTP client**:
   - Windows: File Explorer (ftp://192.168.x.x:2221)
   - macOS: Finder → Go → Connect to Server
   - FileZilla, WinSCP, or any FTP client
5. **Tap individual fields** to copy them to clipboard
6. **Tap "Copy Full URL"** to copy the complete connection string
7. **Tap STOP** when finished to stop the server

## Permissions Required

- `INTERNET` - To allow network communication
- `ACCESS_NETWORK_STATE` - To check network connectivity
- `ACCESS_WIFI_STATE` - To retrieve the device's local IP address
- `FOREGROUND_SERVICE` - To keep the server running reliably
- `POST_NOTIFICATIONS` - To show server status notifications

## Project Structure

```
ftp-server/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/ftpserver/app/
│   │       │   ├── MainActivity.kt          # Main UI with Compose
│   │       │   ├── FTPServerService.kt      # FTP server service
│   │       │   └── ui/screens/              # Compose screens
│   │       ├── res/
│   │       │   ├── values/
│   │       │   │   ├── strings.xml
│   │       │   │   ├── themes.xml
│   │       │   │   └── colors.xml
│   │       │   └── drawable/
│   │       └── AndroidManifest.xml
│   └── build.gradle.kts                     # App-level build config
├── build.gradle.kts                         # Root build config (AGP + Kotlin versions)
├── settings.gradle.kts                      # Foojay plugin & repository config
├── gradle.properties                        # Android build flags
├── gradle/
│   ├── gradle-daemon-jvm.properties         # Pinned JDK version (Adoptium 21)
│   └── wrapper/
│       └── gradle-wrapper.properties        # Pinned Gradle version (9.2.1)
└── README.md
```

## Customization

### Change Server Port
Edit `FTPServerService.kt`:
```kotlin
companion object {
    const val PORT = 2221  // Change this value
    // ...
}
```

### Change Credentials
Edit `FTPServerService.kt`:
```kotlin
companion object {
    // ...
    const val USERNAME = "your_username"
    const val PASSWORD = "your_password"
}
```

### Modify UI Colors
Edit the theme files in `app/src/main/java/com/ftpserver/app/ui/theme/` to customize colors.

## Troubleshooting

### Server won't start
- Check that no other app is using port 2221
- Ensure the app has necessary permissions
- Verify network connectivity

### Can't connect from client
- Ensure both devices are on the same network
- Check firewall settings on the client device
- Verify the IP address is correct (it may change on network reconnect)
- Try using the device's hostname instead of IP

### Files not accessible
- The FTP root is located in the app's external storage
- Files are stored in `/Android/data/com.ftpserver.app/files/ftp_root/`
- The user has full read/write permissions by default

### Build fails on first run
- Ensure `ANDROID_HOME` is set correctly: `echo $ANDROID_HOME`
- Verify SDK packages are installed: `sdkmanager --list_installed`
- Check your internet connection (Gradle and JDK are downloaded on first build)

## Security Notes

⚠️ **Important**: This is a basic FTP server intended for local network use only. For production use, consider:
- Using FTPS (FTP over SSL/TLS)
- Implementing stronger authentication
- Adding access controls and logging
- Using secure file transfer protocols

## License

This project is open source and available under the Apache License 2.0.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## Support

For issues, questions, or suggestions, please open an issue on the GitHub repository.