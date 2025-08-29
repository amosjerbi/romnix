# Connection Debug Commands

Run these commands to help debug your connection:

## Test 1: Where am I?
```bash
curl -s https://api.ipify.org
echo "My public IP is above"
hostname
echo "My hostname is above"
```

## Test 2: Can I reach your device?
```bash
# This should work if you're local, fail if you're in Codespace
ping -c 3 192.168.0.132
```

## Test 3: SSH test with verbose output
```bash
ssh -v -p 22 root@192.168.0.132
```

## Test 4: Check network route
```bash
traceroute 192.168.0.132
# or if traceroute not available:
ping -c 1 192.168.0.132 && echo "Direct route exists"
```

Please run these and share the output!
