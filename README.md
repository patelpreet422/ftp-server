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
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material 3
- **FTP Library**: Apache FTPServer 1.2.0
- **Minimum SDK**: 24 (Android 7.0)
- **Target SDK**: 34 (Android 14)

## Building the Project

### Prerequisites
- Android Studio (latest version recommended)
- JDK 8 or higher
- Android SDK with API level 34
- Gradle 8.2 or higher (included via wrapper)

### Build Steps

1. **Clone the repository**:
   ```bash
   git clone https://github.com/patelpreet422/ftp-server.git
   cd ftp-server
   ```

2. **Build using Gradle**:
   ```bash
   ./gradlew assembleDebug
   ```

3. **Install on device**:
   ```bash
   ./gradlew installDebug
   ```

4. **Or open in Android Studio**:
   - Open Android Studio
   - Select "Open an Existing Project"
   - Navigate to the project directory
   - Click "OK"
   - Wait for Gradle sync to complete
   - Click the Run button or press Shift+F10

### Build Outputs
The APK files will be generated in:
- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/apk/release/app-release.apk`

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
│   │       │   └── FTPServerService.kt      # FTP server service
│   │       ├── res/
│   │       │   ├── values/
│   │       │   │   ├── strings.xml
│   │       │   │   ├── themes.xml
│   │       │   │   └── colors.xml
│   │       │   └── drawable/
│   │       └── AndroidManifest.xml
│   └── build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
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
Edit `MainActivity.kt` in the `FTPServerTheme` composable to customize colors.

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