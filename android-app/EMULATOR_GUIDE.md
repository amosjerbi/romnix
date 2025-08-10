# Android Studio Emulator Guide

## Quick Start
1. Open Android Studio (should already be open)
2. Wait for Gradle sync to complete
3. Select or create an emulator from Device Manager
4. Click Run (green play button)

## Creating an Emulator
1. **Device Manager** → **Create Device**
2. Choose: **Pixel 6** or **Pixel 7**
3. System Image: **API 33** or **API 34** (with Google Play)
4. Name it: "Pixel 6 API 33"
5. Click **Finish**

## Common Issues & Solutions

### Issue: "No Device Selected"
**Solution**: Click dropdown next to Run button → Select your emulator

### Issue: "Gradle Sync Failed"
**Solution**: 
- File → Sync Project with Gradle Files
- Check internet connection
- File → Invalidate Caches and Restart

### Issue: "Emulator Won't Start"
**Solution**:
- Ensure you have enough RAM (8GB+ recommended)
- Try Cold Boot: Device Manager → ▼ → Cold Boot Now
- Check virtualization is enabled in BIOS

### Issue: "App Crashes on Launch"
**Solution**:
- Build → Clean Project
- Build → Rebuild Project
- Check Logcat for errors (bottom panel)

## Testing the New UI

### Handheld Tab
- Tap "Connect" on Rocknix or muOS cards
- Click Settings icon to edit templates
- Try manual configuration section

### Consoles Tab
- Browse the console grid
- Use search to filter consoles
- Select a console to go to Browse

### Browse Tab
- Search for games
- Use platform filters
- Try quick filter chips
- Test download/upload buttons

### Custom Tab
- Scan network for devices
- Add manual IP addresses
- Configure SSH settings

## Keyboard Shortcuts
- **Run App**: Shift + F10
- **Debug App**: Shift + F9
- **Stop App**: Cmd + F2
- **Logcat**: Cmd + 6
- **Build**: Cmd + F9

## UI Features to Notice
- Purple color theme (#797ED2)
- Rounded corners on cards
- Smooth animations
- Material Design 3 components
- Responsive layout
