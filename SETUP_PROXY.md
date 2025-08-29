# ROM Downloader - Proxy Setup Guide

Since all public CORS proxy services are failing, here are your options to get ROM searching working:

## Option 1: Local Proxy Server (Recommended)

**Step 1:** Open a new terminal window and run:
```bash
cd "/Users/amosjerbi/Desktop/rom android_Illegal/android-app/web-version"
python3 proxy_server.py
```

**Step 2:** Keep both servers running:
- Terminal 1: `python3 -m http.server 8000` (main app)
- Terminal 2: `python3 proxy_server.py` (proxy on port 8001)

**Step 3:** Refresh your browser and try searching for ROMs

## Option 2: Browser CORS Extension

**For Chrome:**
1. Install "CORS Unblock" extension from Chrome Web Store
2. Enable the extension 
3. Refresh the ROM Downloader page
4. Try searching again

**For Firefox:**
1. Install "CORS Everywhere" extension
2. Enable it and refresh the page

## Option 3: Chrome with Disabled Security (Development Only)

**⚠️ Warning:** Only use this for development, not regular browsing!

1. Close all Chrome windows
2. Run Chrome with flags:
   ```bash
   # macOS
   /Applications/Google\ Chrome.app/Contents/MacOS/Google\ Chrome --disable-web-security --user-data-dir=/tmp/chrome_dev_test --disable-features=VizDisplayCompositor
   
   # Windows
   chrome.exe --disable-web-security --user-data-dir=c:/temp/chrome_dev_test
   
   # Linux
   google-chrome --disable-web-security --user-data-dir=/tmp/chrome_dev_test
   ```
3. Navigate to http://localhost:8000

## Troubleshooting

### Local Proxy Issues
- **Port 8001 in use:** Change the PORT variable in `proxy_server.py` to 8002, then update `platforms.js` line 126
- **Connection refused:** Make sure the proxy server is running in a separate terminal

### Still No Results
- Check browser console (F12) for detailed error messages
- Ensure both servers are running simultaneously
- Try a different browser or incognito mode

### Testing
Open browser console and run:
```javascript
window.romApp.romRepository.testProxy()
```

This should show "Proxy test result: Success" if everything is working.

## How It Works

The local proxy server (`proxy_server.py`):
1. Receives requests from your web app
2. Fetches ROM directory pages from myrient.erista.me
3. Returns the HTML content with proper CORS headers
4. Allows the web app to parse the directory listings and find ROM files

This bypasses browser CORS restrictions while keeping everything local and secure.
