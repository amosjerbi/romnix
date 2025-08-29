# 🔧 Rocknix Connection Troubleshooting Guide

Having trouble connecting to your Rocknix device from GitHub Codespaces? Let's debug this step by step!

## 🚨 **Step 1: Verify Rocknix SSH Settings**

### **Enable SSH on Rocknix Device**
1. **Go to**: Main Menu → System Settings → Services
2. **Find**: "SSH" or "SSH Server"
3. **Enable**: Turn ON SSH service
4. **Note**: Some Rocknix versions have SSH in "Advanced Settings"

### **Get Device Network Info**
1. **Go to**: System Settings → Network → Network Settings
2. **Find your IP**: Usually `192.168.x.x` or `10.x.x.x`
3. **Write it down**: e.g., `192.168.1.100`

### **Test SSH Locally First**
From a computer on the same network as your Rocknix:
```bash
ssh root@192.168.1.100  # Replace with your device IP
# Try password: rocknix
# Or try: root
# Or try: (empty - just press Enter)
```

**If local SSH fails, fix Rocknix settings before continuing!**

## 🌐 **Step 2: Router Port Forwarding Setup**

### **Access Your Router**
1. **Find router IP**: Usually `192.168.1.1` or `192.168.0.1`
2. **Open browser**: Go to your router IP
3. **Login**: Use admin credentials (often on router sticker)

### **Set Up Port Forwarding**
1. **Find**: "Port Forwarding" or "Virtual Servers" section
2. **Add new rule**:
   - **Service Name**: Rocknix SSH
   - **External Port**: `2222` (or any unused port)
   - **Internal IP**: `192.168.1.100` (your Rocknix IP)
   - **Internal Port**: `22`
   - **Protocol**: `TCP`
3. **Save and Apply** settings
4. **Restart router** if required

### **Find Your Public IP**
- Visit: https://whatismyipaddress.com
- **Write down your public IP**: e.g., `203.0.113.42`

## 🧪 **Step 3: Test Port Forwarding**

### **Use Online Port Checker**
1. **Visit**: https://www.portchecktool.com/
2. **Enter**: Your public IP and external port (e.g., `203.0.113.42:2222`)
3. **Check**: Should show "Port is open"

**If port shows closed, check router settings and firewall!**

## 🔍 **Step 4: Test from Codespace Terminal**

In your Codespace terminal, test the connection:

```bash
# Test basic connectivity
ping -c 3 [YOUR-PUBLIC-IP]

# Test SSH connection manually
ssh -p 2222 root@[YOUR-PUBLIC-IP]
# Try passwords: rocknix, root, or empty

# Test with verbose output for debugging
ssh -v -p 2222 root@[YOUR-PUBLIC-IP]
```

## ⚙️ **Step 5: ROM Downloader Configuration**

### **Correct Settings in ROM Downloader**
```
Template: Custom (don't use pre-filled Rocknix template)
Host IP: [YOUR-PUBLIC-IP]  # e.g., 203.0.113.42
Username: root
Password: rocknix  # (or try 'root' or empty)
Port: 2222         # (your external port)
Remote Path: /storage/roms
```

### **Common Mistakes to Avoid**
❌ **Don't use local IP** (192.168.x.x) - Codespace can't reach it
❌ **Don't use port 22** - use your external port (2222)
❌ **Don't use default template** - configure custom settings

## 🛠️ **Step 6: Advanced Debugging**

### **Check Rocknix SSH Logs**
On your Rocknix device via local terminal:
```bash
# Check if SSH daemon is running
ps aux | grep ssh

# Check SSH logs
journalctl -u ssh
# or
tail -f /var/log/auth.log
```

### **Try Alternative SSH Settings**
Sometimes Rocknix has different configurations:

**Try different usernames**:
- `root` (most common)
- `admin`
- `anbernic` (on some devices)

**Try different passwords**:
- `rocknix` (default)
- `root`
- `` (empty password)
- `anbernic`

### **SSH Key Authentication (Advanced)**
If password auth fails, try SSH keys:
```bash
# In Codespace, generate SSH key
ssh-keygen -t rsa -b 4096 -f ~/.ssh/rocknix_key

# Copy public key to Rocknix (replace IPs)
ssh-copy-id -i ~/.ssh/rocknix_key.pub -p 2222 root@[YOUR-PUBLIC-IP]
```

## 🔒 **Step 7: Firewall Considerations**

### **Check Router Firewall**
- Some routers have built-in firewalls
- Look for "Firewall" or "Security" settings
- Temporarily disable to test (re-enable after!)

### **Check ISP Restrictions**
- Some ISPs block incoming connections on residential plans
- Try different external ports (2222, 2223, 3333, etc.)
- Contact ISP if issues persist

## 🆘 **Common Error Messages & Solutions**

### **"Connection timeout"**
- Port forwarding not working
- Router firewall blocking
- Wrong public IP

### **"Connection refused"**
- SSH not enabled on Rocknix
- Wrong port number
- Device not responding

### **"Authentication failed"**
- Wrong username/password
- Try different credentials
- SSH keys might be required

### **"Host unreachable"**
- Internet connectivity issue
- Wrong public IP address
- Router/ISP blocking

## ✅ **Quick Checklist**

Before asking for help, verify:

- [ ] SSH enabled on Rocknix device
- [ ] Port forwarding configured (external → internal)
- [ ] Router settings saved and applied
- [ ] Public IP correct (check whatismyipaddress.com)
- [ ] External port accessible (use port checker tool)
- [ ] Tested SSH manually from Codespace terminal
- [ ] Tried different passwords (rocknix, root, empty)
- [ ] Used custom configuration (not default template)

## 📞 **Still Need Help?**

If you've tried all steps above, please provide:

1. **Error message** from ROM Downloader
2. **SSH test results** from Codespace terminal
3. **Port checker results** from online tool
4. **Rocknix version** and device model
5. **Router model** and firmware version

---

**🎯 Most connection issues are solved by proper port forwarding setup and SSH configuration!**
