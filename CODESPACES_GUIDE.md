# 🚀 GitHub Codespaces Quick Start Guide

Welcome to ROM Downloader Web in GitHub Codespaces! This guide will help you get started quickly.

## 🎯 What is GitHub Codespaces?

GitHub Codespaces provides a complete development environment in the cloud. For ROM Downloader Web, this means:

- ✅ **No local setup required** - Everything runs in your browser
- ✅ **Full functionality** - Real ROM downloads and device transfers
- ✅ **Pre-configured environment** - All dependencies already installed
- ✅ **Persistent storage** - Your settings and templates are saved
- ✅ **SSH capabilities** - Direct connection to your handheld devices

## 🚀 Getting Started

### Step 1: Launch Codespace
Click the "Open in GitHub Codespaces" button in the main README or:
1. Go to the repository on GitHub
2. Click the green "Code" button
3. Select "Codespaces" tab
4. Click "Create codespace on main"

### Step 2: Wait for Setup (2-3 minutes)
The Codespace will automatically:
- Install Python and dependencies
- Install SSH tools (`sshpass`, `openssh-client`)
- Start all three services:
  - Frontend Server (port 8000)
  - CORS Proxy Server (port 8001) 
  - ROM Transfer Service (port 8002)

### Step 3: Access the Application
Once setup is complete:
- The web interface will automatically open in a new tab
- URL format: `https://[codespace-name]-8000.github.dev`
- All functionality will be available immediately

## 🎮 Using the Application

### Device Connection

1. **Go to the Devices tab** (bottom navigation)
2. **Choose a template**:
   - **Rocknix**: For Rocknix-based handhelds
   - **muOS**: For muOS devices
   - **Custom**: Manual configuration
3. **Click Connect** to apply template settings
4. **Scan Network** to find your device, or enter IP manually

### ROM Management

1. **Go to the Consoles tab** to browse platforms
2. **Select a console** (NES, SNES, Genesis, etc.)
3. **Search for games** using the search bar
4. **Download options**:
   - **Download**: Save ROM locally
   - **Upload**: Transfer previously downloaded ROM
   - **Download & Transfer**: Stream directly to device

### Bulk Operations

When you have search results:
- **Download All**: Download all found ROMs
- **Download All & Transfer**: Stream all ROMs directly to your device

## 🔧 Device Configuration

### Rocknix Devices
```
Username: root
Password: rocknix
Port: 22
ROM Path: /storage/roms/
```

### muOS Devices
```
Username: root  
Password: muos
Port: 22
ROM Path: /mnt/mmc/ROMS/
```

### Custom Devices
- Configure any SSH-enabled device
- Test connection before transferring
- Save settings as custom templates

## 🌐 Network Setup

### For Local Network Devices

If your handheld device is on your local network:

1. **Enable SSH** on your handheld device
2. **Find the device IP** (check device settings or router admin)
3. **Add device** using the "Scan Network" or manual IP entry
4. **Test connection** before transferring ROMs

### Port Forwarding (Advanced)

For devices behind firewalls, you may need to set up port forwarding on your router to expose SSH (port 22) to the internet. This allows Codespaces to connect directly.

⚠️ **Security Note**: Only do this temporarily and use strong passwords.

## 🔍 Troubleshooting

### Services Not Starting
If services don't start automatically:
```bash
python start_services.py
```

### Can't Connect to Device
1. Verify device has SSH enabled
2. Check IP address is correct  
3. Try different passwords (muOS sometimes uses 'root' or empty password)
4. **📖 See [NETWORK_SETUP_GUIDE.md](NETWORK_SETUP_GUIDE.md) for complete connection setup**
5. Most users need **port forwarding** on their router to connect from Codespaces

### ROM Downloads Failing
1. Check the CORS proxy is running (port 8001)
2. Try refreshing the page
3. Check browser console for errors

### Transfer Failures
1. Test SSH connection first
2. Verify credentials are correct
3. Check device has enough storage space
4. Try alternative passwords for your device

## 💡 Tips & Tricks

### Persistent Storage
- Your Codespace persists for 30 days of inactivity
- Templates and settings are saved automatically
- Downloaded ROMs are stored in the Codespace

### Multiple Devices
- Save different devices as custom templates
- Switch between devices easily using the templates
- Test connections before bulk transfers

### Performance
- Codespaces provides good performance for ROM transfers
- Direct streaming (Download & Transfer) is fastest
- Bulk operations show progress bars

### Security
- All transfers use SSH encryption
- No ROMs are stored on GitHub servers
- Codespace storage is private to your account

## 🆘 Getting Help

### Check Service Status
In the Codespace terminal:
```bash
# Check if services are running
ps aux | grep python

# Restart services if needed
python start_services.py
```

### View Logs
```bash
# View transfer service logs
tail -f /tmp/transfer_service.log

# View proxy logs  
tail -f /tmp/proxy_server.log
```

### Report Issues
- [GitHub Issues](https://github.com/yourusername/rom-downloader-web/issues)
- [Discussions](https://github.com/yourusername/rom-downloader-web/discussions)

## 🎉 Enjoy!

You now have a full-featured ROM downloader running in the cloud with the ability to transfer games directly to your handheld devices. Happy gaming! 🎮
