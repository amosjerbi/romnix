# Romnix - ROM Downloader

A cross-platform ROM downloading and management application with both Android and command-line interfaces.

## Overview

Romnix provides an easy way to browse, search, and download ROMs for various gaming platforms. The application supports multiple retro gaming consoles and includes features for transferring ROMs to handheld devices via SMB/SSH.

## Project Structure

```
Romnix/
├── android-app/          # Android application
└── README.md           # This file
```

## Features

- **Multi-platform ROM support**: NES, SNES, Genesis, Game Boy, GBA, PlayStation, and more
- **Search functionality**: Search across all platforms or filter by specific console
- **Android app**: Native Android interface with modern UI
- **Network transfer**: SMB/SSH support for transferring ROMs to handheld devices
- **Download management**: Organized ROM storage by platform

## Setup Instructions

### 1. Configure ROM Archive Links

Before compiling, you need to replace the placeholder links with actual ROM archive URLs:

#### Android App Configuration

Edit `android-app/app/src/main/java/com/example/romdownloader/MainActivity.kt`:

1. Find the `Platform` enum (around line 120)
2. Replace all instances of `"insert_your_link_here"` with your actual ROM archive URLs
3. Update the referer header (around line 189) with your archive domain

Example:
```kotlin
enum class Platform(val id: String, val label: String, val archiveUrl: String, val extensions: List<String>) {
    NES("nes", "NES", "https://your-rom-archive.com/nes/", listOf("7z", "zip")),
    SNES("snes", "SNES", "https://your-rom-archive.com/snes/", listOf("7z", "zip")),
    // ... continue for all platforms
}
```

And update the referer:
```kotlin
.header("Referer", "https://your-rom-archive.com/")
```

### 2. Android App Compilation

#### Prerequisites
- Android Studio (Giraffe or newer)
- Android SDK (API level 24 or higher)
- Kotlin support

#### Build Steps

1. **Open the project**:
   ```bash
   cd android-app
   # Open in Android Studio or use command line
   ```

2. **Install dependencies**:
   ```bash
   ./gradlew build
   ```

3. **Debug build**:
   ```bash
   ./gradlew assembleDebug
   ```

4. **Release build**:
   ```bash
   ./gradlew assembleRelease
   ```

5. **Install on device**:
   ```bash
   ./gradlew installDebug
   ```

#### Android Studio Method

1. Open Android Studio
2. Select "Open an existing project"
3. Navigate to the `android-app` folder
4. Click "Open"
5. Wait for Gradle sync to complete
6. Click the "Run" button or press Shift+F10

### 3. Testing Your Configuration

After updating the links and compiling:

1. **Test ROM listing**: Select a platform and verify ROMs are displayed
2. **Test downloads**: Try downloading a ROM to ensure URLs work correctly
3. **Check network features**: Test SMB/SSH connectivity if using handheld devices

## Platform Support

The application supports the following platforms:

| Platform | Supported Extensions | Notes |
|----------|---------------------|-------|
| NES | 7z, zip | Nintendo Entertainment System |
| SNES | 7z, zip | Super Nintendo Entertainment System |
| Genesis | 7z, zip | Sega Genesis/Mega Drive |
| Game Boy | 7z, zip | Original Game Boy |
| GBA | 7z, zip | Game Boy Advance |
| GBC | 7z, zip | Game Boy Color |
| Game Gear | 7z, zip | Sega Game Gear |
| Neo Geo Pocket | 7z, zip | SNK Neo Geo Pocket |
| Master System | 7z, zip | Sega Master System |
| Sega CD | 7z, zip, chd, cue, bin | Sega CD/Mega CD |
| Sega 32X | 7z, zip | Sega 32X |
| Saturn | 7z, zip | Sega Saturn |
| TurboGrafx-16 | 7z, zip | PC Engine/TurboGrafx-16 |
| PlayStation | 7z, zip, cue | Sony PlayStation |
| Nintendo 64 | 7z, zip, z64, n64, v64 | Nintendo 64 |
| Dreamcast | 7z, zip, chd | Sega Dreamcast |

## Network Features

### SMB/SSH Support

The application can transfer ROMs to handheld gaming devices via:

- **SMB shares**: For devices with Samba support
- **SSH transfers**: For devices with SSH access
- **Auto-discovery**: Automatically scan for compatible devices on your network

### Supported Devices

- RockNix devices
- Lakka systems
- Custom Linux handhelds with SSH/SMB

## Development

### Code Structure

- `MainActivity.kt`: Main Android activity with ROM browsing and download logic
- `Platform` enum: Defines supported gaming platforms and their archive URLs
- Network classes: Handle SMB/SSH connections and transfers
- UI components: Modern Material Design interface

### Adding New Platforms

1. Add new entry to the `Platform` enum
2. Configure appropriate file extensions
3. Test ROM listing and downloads

## Legal Notice

This application is a ROM management tool. Users are responsible for ensuring they own legal copies of any ROMs they download and that their use complies with local copyright laws.

## Troubleshooting

### Common Issues

1. **No ROMs displayed**: Verify your archive URLs are correct and accessible
2. **Download failures**: Check network connectivity and URL formatting
3. **Network transfer issues**: Ensure target devices are on the same network and have proper credentials

### Build Issues

1. **Gradle sync failed**: Check internet connection and Android Studio version
2. **Missing dependencies**: Run `./gradlew clean` and retry
3. **API level errors**: Update your Android SDK to the latest version

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly
5. Submit a pull request

## License

This project is provided as-is for educational and personal use. Please respect copyright laws and only download ROMs you legally own.
