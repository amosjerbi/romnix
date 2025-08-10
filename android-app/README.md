# RomDownloader (Android)

Minimal Android app that mirrors key `rom.sh` functionality:
- Select platform or search across all
- Fetch ROM listings from ROM archives
- Download chosen ROMs via Android DownloadManager into app-specific Downloads (external files)

## Build
Open `android-app` in Android Studio (Giraffe or newer) and click Run.

## Notes
- SMB/SSH detection and transfers from `rom.sh` are not yet implemented in the Android app.
- Downloads are saved under: Android/data/com.example.romdownloader/files/Download/roms/<platform>.
