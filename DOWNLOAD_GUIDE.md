# Enhanced ROM Downloader - User Guide

## 🚀 New Features

This enhanced ROM downloader now includes **direct downloading capabilities** inspired by the powerful zuz_illegal_archive.sh script, combining the best of both web interface ease-of-use and command-line efficiency.

## ✨ What's New

### 1. **Direct ROM Downloads**
- ROMs are now **actually downloaded** to your computer (not just browsed)
- Downloads use multiple proxy servers for reliability
- Files are saved with proper names and organized by platform

### 2. **Batch Download Capabilities**
- **Download All**: Download all found ROMs without requiring device connection
- **Download All & Transfer**: Download + transfer to connected retro gaming devices
- Real-time progress tracking with detailed results

### 3. **Enhanced Search & Browse**
- Improved directory parsing that skips navigation links
- Better error handling and timeout management
- Support for archive.org ROM collections

### 4. **Smart Progress Tracking**
- Visual progress bars for batch operations
- Detailed success/failure reporting
- Individual ROM download status indicators

## 🎮 How to Use

### Quick Start
1. **Start the servers**:
   ```bash
   # Terminal 1: Web server
   cd web-version && python3 -m http.server 8000
   
   # Terminal 2: CORS proxy (optional but recommended)
   cd web-version && python3 proxy_server.py
   ```

2. **Open**: `http://localhost:8000`

3. **Select a platform** (NES, SNES, Genesis, etc.)

4. **Search or browse** for ROMs

### Download Options

#### Individual ROM Downloads
- Click the **download button** (⬇️) on any ROM card
- ROM will be downloaded directly to your Downloads folder
- Button shows progress: ⏳ → ✅ → ⬇️

#### Batch Downloads
- **Download All**: Downloads all search results locally
- **Download All & Transfer**: Downloads + transfers to connected device
- Progress bar shows current file and overall completion

### Search Tips
- **Empty search**: Shows all available ROMs for the platform
- **Specific search**: Enter game names, series, or keywords
- **Case insensitive**: Search works regardless of capitalization

## 🔧 Technical Details

### Proxy System
- Uses multiple CORS proxies for reliability
- Local proxy server (`localhost:8001`) for best performance
- Automatic fallback to public proxies if local fails

### File Organization
- Downloads are organized by platform
- Original filenames preserved with proper extensions
- Support for `.zip`, `.7z`, `.nes`, `.smc`, `.md`, etc.

### Error Handling
- Automatic retry with different proxies
- Timeout protection (8 seconds per attempt)
- Clear error reporting and recovery

## 🎯 Based on zuz_illegal_archive.sh

This enhanced system incorporates key features from the comprehensive ROM management script:

- **Direct archive.org URLs** for reliable ROM sources
- **Batch processing** with progress tracking
- **Multiple platform support** with proper file extensions
- **Organized download structure** for easy management
- **Error resilience** with multiple fallback options

## 🚨 Usage Notes

- **Legal**: Only download ROMs you legally own
- **Performance**: Local proxy provides fastest downloads
- **Storage**: Ensure sufficient disk space for batch downloads
- **Network**: Requires internet connection for ROM fetching

## 🛠️ Troubleshooting

### Downloads Not Working
1. Check if proxy server is running (`localhost:8001`)
2. Try hard refresh (`Cmd+Shift+R` or `Ctrl+Shift+R`)
3. Check browser console for error details

### Slow Downloads
1. Ensure local proxy server is running
2. Check network connection
3. Try smaller batch sizes

### Search Issues
1. Verify archive URLs are accessible
2. Check console for CORS errors
3. Try different search terms

---

**Enjoy your enhanced ROM downloading experience!** 🎮✨
