# 🚀 Easy Port Forwarding Setup Guide

## 🎯 **Option 1: Router Web Interface (Most Common)**

### **Step 1: Find Your Router**
```bash
# On your local computer, find your router IP:
# Windows:
ipconfig | findstr "Default Gateway"

# Mac/Linux:
route -n get default | grep gateway
# or
netstat -rn | grep default
```
Usually: `192.168.1.1` or `192.168.0.1`

### **Step 2: Access Router Admin**
1. **Open browser** → Go to your router IP (e.g., `http://192.168.1.1`)
2. **Login** with admin credentials:
   - Common defaults: `admin/admin`, `admin/password`, `admin/(blank)`
   - Check sticker on router for actual credentials

### **Step 3: Find Port Forwarding**
Look for these menu names:
- ✅ **"Port Forwarding"**
- ✅ **"Virtual Servers"**
- ✅ **"Port Mapping"**
- ✅ **"Applications & Gaming"**
- ✅ **"NAT Forwarding"**

### **Step 4: Add Rule**
Create new rule with these **exact settings**:
```
Service Name: Rocknix SSH
External Port: 2222
Internal IP: 192.168.0.132
Internal Port: 22
Protocol: TCP
Status: Enabled
```

### **Step 5: Save & Restart**
- **Save/Apply** settings
- **Restart router** if prompted
- **Test** the connection

---

## 🎯 **Option 2: UPnP (Super Easy!)**

### **Check if UPnP is Enabled**
1. **Router Settings** → Look for **"UPnP"**
2. **Enable UPnP** if disabled
3. **Save settings**

### **Use UPnP Tool**
Download a UPnP tool to automatically create the port mapping:

**Windows:**
- Download "Simple Port Forwarding" tool
- Or use Windows built-in: `netsh interface portproxy`

**Mac/Linux:**
```bash
# Install miniupnpc
brew install miniupnpc  # Mac
sudo apt-get install miniupnpc  # Linux

# Add port forwarding
upnpc -a 192.168.0.132 22 2222 TCP

# Check if it worked
upnpc -l
```

---

## 🎯 **Option 3: Router-Specific Quick Guides**

### **Netgear Routers**
1. **Visit**: http://192.168.1.1
2. **Advanced** → **Dynamic DNS/Port Forwarding**
3. **Add** → Custom Service
4. **Fill**: External Port `2222`, Internal Port `22`, Server IP `192.168.0.132`

### **Linksys Routers**
1. **Visit**: http://192.168.1.1
2. **Smart Wi-Fi Tools** → **Port Forwarding**
3. **Manual** → Add new rule

### **ASUS Routers**
1. **Visit**: http://192.168.1.1
2. **WAN** → **Virtual Server/Port Forwarding**
3. **Enable Port Forwarding** → **Add**

### **TP-Link Routers**
1. **Visit**: http://192.168.1.1
2. **Advanced** → **NAT Forwarding** → **Port Forwarding**
3. **Add** rule

---

## 🎯 **Option 4: Easy Alternatives (No Router Setup!)**

### **A. Use ngrok (Simplest!)**
1. **Download ngrok**: https://ngrok.com/download
2. **Run on your local network**:
   ```bash
   # Install ngrok
   # Then expose your Rocknix
   ngrok tcp 192.168.0.132:22
   ```
3. **Get public URL**: `tcp://0.tcp.ngrok.io:12345`
4. **Use in Codespace**: `ssh -p 12345 root@0.tcp.ngrok.io`

### **B. Use Tailscale VPN**
1. **Install Tailscale**: https://tailscale.com/download
2. **Install on both**:
   - Your local computer
   - Enable in your Codespace
3. **Direct connection** without port forwarding!

---

## 🧪 **Test Your Setup**

### **From Your Local Computer**
```bash
# Test external access to your own router
ssh -p 2222 root@[YOUR-PUBLIC-IP]
```

### **From Your Codespace**
```bash
# Get your public IP first
curl -s https://whatismyipaddress.com

# Test the connection (replace with your actual public IP)
ssh -p 2222 root@[YOUR-PUBLIC-IP]

# If successful, test ROM Downloader transfer!
```

---

## 🆘 **Still Having Trouble?**

### **Quick Diagnostic Commands**
```bash
# Check if port is open externally
# Visit: https://www.portchecktool.com/
# Enter: [YOUR-PUBLIC-IP]:2222

# Or use nmap from another network
nmap -p 2222 [YOUR-PUBLIC-IP]
```

### **Common Router Default IPs**
- **192.168.1.1** (Linksys, Netgear, D-Link)
- **192.168.0.1** (Belkin, SMC, US Robotics)
- **10.0.1.1** (Apple AirPort)
- **192.168.2.1** (Belkin, 2Wire)

### **Common Login Credentials**
- `admin` / `admin`
- `admin` / `password`  
- `admin` / `` (blank)
- `admin` / `1234`
- Check router sticker!

---

## ✅ **Recommended: Start with ngrok!**

**If router setup seems complicated, try ngrok first:**

1. **Download ngrok** (free account needed)
2. **Run**: `ngrok tcp 192.168.0.132:22`  
3. **Use the ngrok URL** in your Codespace
4. **No router configuration needed!**

This gets you testing immediately while you figure out the permanent router solution! 🚀
