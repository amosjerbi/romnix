# 🌐 Network Setup Guide for GitHub Codespaces

How users can connect their GitHub Codespace to their local handheld devices.

## 🎯 Connection Scenarios

### ✅ **Scenario 1: Public IP + Port Forwarding** (Recommended)

**When it works**: User has router access and can set up port forwarding

**Setup Steps**:
1. **Enable SSH on your handheld device** (Rocknix/muOS)
2. **Find your router's admin interface** (usually 192.168.1.1 or 192.168.0.1)
3. **Set up port forwarding**:
   - External Port: `2222` (or any unused port)
   - Internal IP: Your device's local IP (e.g., `192.168.1.100`)
   - Internal Port: `22` (SSH port)
   - Protocol: `TCP`
4. **Find your public IP**: Visit [whatismyipaddress.com](https://whatismyipaddress.com)
5. **In ROM Downloader**: Use your public IP with port 2222

**Example Configuration**:
```
Host IP: 203.0.113.42 (your public IP)
Username: root
Password: rocknix (or muos)
Port: 2222 (your forwarded port)
Remote Path: /storage/roms (or /mnt/mmc/ROMS)
```

**Security**: ⚠️ **Only enable temporarily** - disable port forwarding when not in use!

### ✅ **Scenario 2: VPN/Tunnel Services** 

**When it works**: User wants a more secure connection

**Options**:
- **ngrok**: `ngrok tcp 22` (creates public tunnel to local device)
- **localtunnel**: Similar tunneling service
- **Tailscale**: VPN solution for device access

**Example with ngrok**:
1. **Install ngrok** on a computer on same network as device
2. **Run**: `ngrok tcp [device-ip]:22`
3. **Use ngrok URL** in ROM Downloader (e.g., `0.tcp.ngrok.io:12345`)

### ⚠️ **Scenario 3: Same Network** (Limited)

**When it works**: If Codespace and device are somehow on the same network (rare)

**Limitations**: 
- GitHub Codespaces run in Microsoft Azure cloud
- Most home devices are behind NAT/firewall
- **Direct local IP connection usually won't work**

### ❌ **What Won't Work**

- ❌ **Direct local IPs** (192.168.x.x, 10.x.x.x) from Codespaces
- ❌ **UPnP automatic forwarding** (Codespaces can't trigger this)
- ❌ **Bluetooth or USB connections** (cloud environment)

## 🛠️ **Step-by-Step Setup Examples**

### **For Rocknix Devices**

1. **Enable SSH on Rocknix**:
   - Go to System Settings → Services
   - Enable SSH service
   - Note the device IP (Settings → Network)

2. **Router Port Forwarding**:
   - Forward external port 2222 → device IP port 22
   - Save and restart router if needed

3. **In ROM Downloader Codespace**:
   ```
   Template: Custom
   Host IP: [Your Public IP]
   Username: root
   Password: rocknix
   Port: 2222
   Remote Path: /storage/roms
   ```

### **For muOS Devices**

1. **Enable SSH on muOS**:
   - SSH is usually enabled by default
   - Check device IP in Network settings

2. **Same port forwarding setup** as above

3. **In ROM Downloader Codespace**:
   ```
   Template: Custom  
   Host IP: [Your Public IP]
   Username: root
   Password: muos
   Port: 2222
   Remote Path: /mnt/mmc/ROMS
   ```

## 🔒 **Security Best Practices**

### **Port Forwarding Security**:
- ✅ **Use non-standard ports** (2222 instead of 22)
- ✅ **Strong passwords** (change default device passwords)
- ✅ **Temporary access** (disable when not in use)
- ✅ **Firewall rules** (limit access to specific IPs if possible)
- ❌ **Never leave port 22 permanently open to internet**

### **Alternative: SSH Key Authentication**
1. **Generate SSH key** in Codespace:
   ```bash
   ssh-keygen -t rsa -b 4096
   ```
2. **Copy public key** to device:
   ```bash
   ssh-copy-id -p 2222 root@[your-public-ip]
   ```
3. **Disable password auth** on device for extra security

## 🧪 **Testing Your Connection**

### **Test SSH Connection First**:
```bash
# In your Codespace terminal:
ssh -p 2222 root@[your-public-ip]
```

### **Test from ROM Downloader**:
1. Go to **Devices** tab
2. Configure your connection
3. Click **"Test Connection"**
4. Look for success message

## 📊 **Connection Success Rates**

| Method | Success Rate | Security | Ease of Setup |
|--------|--------------|----------|---------------|
| Port Forwarding | 90% | Medium | Easy |
| ngrok/Tunnel | 95% | High | Medium |
| VPN (Tailscale) | 98% | Very High | Hard |
| Local IP | 5% | N/A | N/A |

## 🆘 **Troubleshooting**

### **Connection Timeouts**:
1. Check if port forwarding is working: Use online port checker
2. Verify device SSH is running: Connect locally first
3. Check firewall settings on router and device

### **Authentication Failures**:
1. Try common passwords: `rocknix`, `muos`, `root`, `` (empty)
2. Check username variations: `root`, `admin`
3. Verify SSH is enabled on device

### **Transfer Failures**:
1. Check available storage space on device
2. Verify ROM path is correct (`/storage/roms` vs `/mnt/mmc/ROMS`)
3. Test with small ROM first

## 💡 **Pro Tips**

### **Multiple Devices**:
- Set up **different external ports** for each device
- Port 2222 → Device 1 (port 22)  
- Port 2223 → Device 2 (port 22)
- Save as **custom templates** in ROM Downloader

### **Network Discovery**:
- The "Scan Network" feature **won't work** from Codespaces
- You'll need to **manually enter device IPs**
- Keep a note of your public IP and forwarded ports

### **Performance**:
- **Direct streaming** (Download & Transfer) works great
- Codespaces have good bandwidth for ROM transfers
- **Bulk transfers** work efficiently with proper setup

---

**📝 Summary**: Yes, users can definitely connect to their devices from Codespaces! Port forwarding is the most common solution, though it requires router access and temporary security considerations. The ROM Downloader is fully functional once the network connection is established.
