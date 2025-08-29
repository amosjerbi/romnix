# 🎮 ROM Downloader Web

A modern web-based ROM downloader with direct device transfer capabilities for retro gaming handhelds like Rocknix, muOS, and other SSH-enabled devices.

![ROM Downloader Preview](https://via.placeholder.com/800x400/6B46C1/FFFFFF?text=ROM+Downloader+Web)

## 🚀 Quick Start Options

### Option 1: GitHub Pages (Demo Mode) 
**Best for**: Trying out the interface and exploring features

🌐 **[Live Demo](https://rocknix.ajerbi.com)**

- ✅ Full web interface
- ✅ ROM browsing and search
- ✅ Platform selection
- ❌ No actual ROM downloads (demo data only)
- ❌ No device transfers

### Option 2: GitHub Codespaces (Full Functionality) ⭐ **Recommended**
**Best for**: Full ROM downloading and device transfer capabilities

[![Open in GitHub Codespaces](https://github.com/codespaces/badge.svg)](https://github.com/amosjerbi/romnix_web/codespaces/new)

- ✅ Complete functionality
- ✅ File management capabilities
- ✅ SSH device transfers  
- ✅ Network device discovery
- ✅ Runs entirely in your browser
- ✅ No local setup required

### Option 3: Local Development
**Best for**: Development and customization

```bash
git clone https://github.com/amosjerbi/romnix_web.git
cd romnix_web
python start_services.py
```

## 🎯 Features

### 🎮 Gaming Platform Support
- **14+ Gaming Platforms**: NES, SNES, Genesis, Game Boy, GBA, GBC, N64, PlayStation, Dreamcast, Saturn, Game Gear, Neo Geo Pocket, Sega Master System, TurboGrafx-16
- **Cross-Platform**: Works on desktop, tablet, and mobile devices
- **Configurable Sources**: Users can configure their own ROM sources

### 🔍 Advanced ROM Management
- **Smart Search**: Search across all platforms or filter by specific console
- **Quick Filters**: Fast access to popular platforms
- **Bulk Operations**: Download multiple ROMs with progress tracking
- **Platform Organization**: Easy console selection with visual grid

### 📱 Handheld Device Integration
- **Rocknix Support**: Pre-configured for Rocknix-based handhelds
- **muOS Support**: Optimized for muOS devices
- **Custom SSH**: Manual configuration for any SSH-enabled device
- **Template System**: Save and reuse connection settings

### 🌐 Network Features
- **Device Discovery**: Scan local network for SSH-enabled devices  
- **Connection Testing**: Verify SSH connections before transfers
- **Direct Streaming**: Stream ROMs directly to devices without local storage
- **Batch Transfers**: Transfer multiple ROMs with progress tracking

## 🛠️ Using GitHub Codespaces

GitHub Codespaces provides a complete cloud-based development environment that runs the full ROM Downloader application.

### Getting Started with Codespaces

1. **Click the Codespaces button** above or go to your repository
2. **Create a new Codespace** - it takes about 2-3 minutes to set up
3. **Wait for services to start** - all three servers start automatically:
   - Frontend Server (port 8000) - Main web interface
   - CORS Proxy (port 8001) - Handles cross-origin requests
   - Transfer Service (port 8002) - Manages device transfers

4. **Access the application** - Codespaces will automatically open the web interface

### Codespaces Features

#### ✅ **File Management**
- Configurable file sources (users provide their own)
- Individual and batch file operations  
- Progress tracking for all operations

#### ✅ **Device Transfer Capabilities**
- Connect to your local network devices through Codespaces port forwarding
- Transfer ROMs directly to Rocknix, muOS, or any SSH-enabled device
- Bulk transfer operations

#### ✅ **Network Discovery**
- Scan for SSH-enabled devices on your network
- Auto-configure popular handheld systems
- Test connections before transferring

#### ✅ **No Local Setup Required**
- All dependencies pre-installed (`sshpass`, Python libraries)
- Automatic service orchestration
- Persistent storage for your Codespace

### Connecting to Local Devices

Yes! Users can connect their Codespace to their local handheld devices, but it requires network setup since Codespaces run in the cloud.

**📖 See [NETWORK_SETUP_GUIDE.md](NETWORK_SETUP_GUIDE.md) for complete instructions**

**Quick Setup (Port Forwarding)**:
1. **Router setup**: Forward external port 2222 → device IP port 22
2. **Find public IP**: Visit whatismyipaddress.com  
3. **ROM Downloader**: Use public IP with port 2222
4. **Security**: Disable port forwarding when not in use

**Alternative options**: VPN tunneling (ngrok, Tailscale) for more secure access

## 🔧 Configuration

### Supported Handheld Systems

#### Rocknix Devices
- **Default SSH**: `root:rocknix` on port 22
- **ROM Path**: `/storage/roms/`
- **Popular Devices**: Anbernic RG35XX, RG353P, etc.

#### muOS Devices  
- **Default SSH**: `root:muos` on port 22
- **ROM Path**: `/mnt/mmc/ROMS/`
- **Popular Devices**: Miyoo Mini Plus, etc.

#### Custom Devices
- Configure any SSH-enabled retro gaming device
- Customizable paths, credentials, and settings
- Template system for easy reuse

### Platform Directory Mapping

ROMs are automatically organized by platform:
```
/storage/roms/          (Rocknix)
├── nes/               # Nintendo Entertainment System
├── snes/              # Super Nintendo
├── genesis/           # Sega Genesis
├── gba/               # Game Boy Advance  
├── ps1/               # PlayStation 1
└── ...
```

## 🚀 GitHub Pages Setup (For Repository Owners)

To deploy your own copy to GitHub Pages:

1. **Fork or clone this repository**
2. **Enable GitHub Pages**:
   - Go to repository Settings
   - Navigate to Pages section
   - Set Source to "Deploy from a branch"
   - Select "main" branch and "/ (root)" folder
   - Save settings

3. **Your site will be available at**:
   `https://yourusername.github.io/romnix_web`

## 🔒 Security & Legal

### Security Features
- **SSH Key Support**: Secure device authentication
- **No Cloud Storage**: ROMs are not stored on external servers
- **Local Processing**: All transfers happen directly between you and your device

### Legal Notice
⚠️ **Important**: This tool is for educational and backup purposes only. Only download ROMs for games you legally own. Respect copyright laws in your jurisdiction.

## 🛠️ Development

### Local Development Setup

```bash
# Clone the repository
git clone https://github.com/amosjerbi/romnix_web.git
cd romnix_web

# Install system dependencies (macOS)
brew install hudochenkov/sshpass/sshpass

# Install Python dependencies  
pip install requests

# Start all services
python start_services.py
```

### Project Structure
```
rom-downloader-web/
├── index.html              # Main web application
├── app.js                  # Frontend JavaScript logic
├── styles.css              # Material Design 3 styling
├── platforms.js            # ROM platform definitions
├── proxy_server.py         # CORS proxy server
├── transfer_service.py     # SSH transfer service
├── start_services.py       # Service orchestrator
├── .devcontainer/          # GitHub Codespaces configuration
│   └── devcontainer.json
└── README.md              # This file
```

### Technologies Used
- **Frontend**: Vanilla JavaScript, HTML5, CSS3
- **Styling**: Material Design 3 principles
- **Backend**: Python 3.11+ with standard library
- **SSH**: sshpass and OpenSSH client
- **Deployment**: GitHub Pages + GitHub Codespaces

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📋 Troubleshooting

### Common Issues

**Q: ROMs not downloading in GitHub Pages demo**
A: GitHub Pages runs in demo mode. Use GitHub Codespaces for full functionality.

**Q: Can't connect to my device in Codespaces**
A: Ensure your device has SSH enabled and is accessible. Check network settings and firewall rules.

**Q: Transfer failed with authentication error**
A: Verify SSH credentials. Default passwords: Rocknix=`rocknix`, muOS=`muos`. Some devices use `root` or empty password.

**Q: Codespace won't start**
A: Try refreshing the page or creating a new Codespace. Check GitHub Codespaces status.

### Getting Help

- 🐛 [Report Issues](https://github.com/amosjerbi/romnix_web/issues)
- 💬 [Discussions](https://github.com/amosjerbi/romnix_web/discussions)  
- 📖 [Wiki](https://github.com/amosjerbi/romnix_web/wiki)

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

**Made with ❤️ for the retro gaming community**

[![GitHub stars](https://img.shields.io/github/stars/amosjerbi/romnix_web?style=social)](https://github.com/amosjerbi/romnix_web)
[![GitHub forks](https://img.shields.io/github/forks/amosjerbi/romnix_web?style=social)](https://github.com/amosjerbi/romnix_web/network/members)
[![GitHub issues](https://img.shields.io/github/issues/amosjerbi/romnix_web)](https://github.com/amosjerbi/romnix_web/issues)
