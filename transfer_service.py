#!/usr/bin/env python3
"""
ROM Transfer Service for muOS/Rocknix devices
Handles SSH/SCP transfers from the web browser to retro gaming devices
"""

import os
import sys
import json
import subprocess
import tempfile
from http.server import HTTPServer, SimpleHTTPRequestHandler
from urllib.parse import urlparse, parse_qs
import urllib.request

class TransferHandler(SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
    
    def do_OPTIONS(self):
        """Handle CORS preflight requests"""
        self.send_response(200)
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Access-Control-Allow-Methods', 'GET, POST, OPTIONS')
        self.send_header('Access-Control-Allow-Headers', 'Content-Type')
        self.end_headers()
    
    def do_POST(self):
        """Handle ROM transfer requests"""
        try:
            # Parse request
            content_length = int(self.headers['Content-Length'])
            request_body = self.rfile.read(content_length).decode('utf-8')
            data = json.loads(request_body)
            
            # Handle different endpoints
            if self.path == '/check-file':
                self.handle_check_file(data)
            elif self.path == '/transfer':
                self.handle_transfer_local(data)
            else:
                # Default: download + transfer
                self.handle_download_and_transfer(data)
                
        except Exception as e:
            print(f"❌ Request error: {e}")
            self.send_error_response(500, str(e))
    
    def handle_check_file(self, data):
        """Check if a file exists in Downloads folder"""
        file_name = data.get('fileName')
        if not file_name:
            self.send_error_response(400, "Missing fileName")
            return
            
        # Check common download locations
        downloads_path = os.path.expanduser("~/Downloads")
        file_path = os.path.join(downloads_path, file_name)
        
        if os.path.exists(file_path):
            self.send_success_response({"exists": True, "filePath": file_path})
        else:
            self.send_success_response({"exists": False, "filePath": None})
    
    def handle_transfer_local(self, data):
        """Transfer a local file to device"""
        local_file_path = data.get('localFilePath')
        rom_name = data.get('romName')
        platform = data.get('platform', '').lower()
        host_config = data.get('hostConfig', {})
        
        if not local_file_path or not rom_name or not host_config:
            self.send_error_response(400, "Missing required parameters")
            return
            
        if not os.path.exists(local_file_path):
            self.send_error_response(404, f"Local file not found: {local_file_path}")
            return
        
        # Transfer to device
        print(f"🚀 Transferring local file to {host_config.get('hostIp')}...")
        success = self.transfer_to_device(local_file_path, rom_name, platform, host_config)
        
        if success:
            self.send_success_response(f"Successfully transferred {rom_name}")
        else:
            self.send_error_response(500, "Transfer failed")
    
    def handle_download_and_transfer(self, data):
        """Download ROM and transfer to device (combo action)"""
        rom_url = data.get('romUrl')
        rom_name = data.get('romName')
        platform = data.get('platform', '').lower()
        host_config = data.get('hostConfig', {})
        
        if not rom_url or not rom_name or not host_config:
            self.send_error_response(400, "Missing required parameters")
            return
        
        # Download ROM to temp file
        print(f"📦 Downloading {rom_name}...")
        temp_file = self.download_rom(rom_url, rom_name)
        
        if not temp_file:
            self.send_error_response(500, "Failed to download ROM")
            return
        
        # Transfer to device
        print(f"🚀 Transferring to {host_config.get('hostIp')}...")
        success = self.transfer_to_device(temp_file, rom_name, platform, host_config)
        
        # Cleanup
        try:
            os.unlink(temp_file)
        except:
            pass
        
        if success:
            self.send_success_response(f"Successfully transferred {rom_name}")
        else:
            self.send_error_response(500, "Transfer failed")
    
    def download_rom(self, rom_url, rom_name):
        """Download ROM file to temporary location"""
        try:
            # Create temp file with proper extension
            _, ext = os.path.splitext(rom_name)
            temp_fd, temp_file = tempfile.mkstemp(suffix=ext, prefix="rom_")
            
            # Download with user agent
            req = urllib.request.Request(rom_url)
            req.add_header('User-Agent', 'Mozilla/5.0 (ROM Downloader)')
            
            with urllib.request.urlopen(req) as response:
                with os.fdopen(temp_fd, 'wb') as f:
                    f.write(response.read())
            
            print(f"✅ Downloaded to {temp_file}")
            return temp_file
            
        except Exception as e:
            print(f"❌ Download failed: {e}")
            return None
    
    def transfer_to_device(self, local_file, rom_name, platform, host_config):
        """Transfer ROM file to retro gaming device via SSH/SCP"""
        try:
            host_ip = host_config.get('hostIp')
            username = host_config.get('username', 'root')
            password = host_config.get('password', 'muos')
            base_path = host_config.get('remoteBasePath', '/mnt/mmc/ROMS')
            
            print(f"🔧 Transfer config - IP: {host_ip}, User: {username}, Platform: {platform}")
            print(f"🔧 Host config received: {host_config}")
            
            # Map platform to directory name
            platform_dirs = {
                'nes': 'nes',
                'snes': 'snes', 
                'gb': 'gb',
                'game boy': 'gb',  # Handle "Game Boy" platform name
                'gbc': 'gbc',
                'gba': 'gba',
                'genesis': 'genesis',
                'gamegear': 'gamegear',
                'sms': 'sms',
                'segacd': 'segacd',
                'sega32x': 'sega32x',
                'saturn': 'saturn',
                'ngp': 'ngp'
            }
            
            platform_dir = platform_dirs.get(platform, platform)
            remote_path = f"{base_path}/{platform_dir}"
            
            # Create remote directory if needed
            mkdir_cmd = [
                'sshpass', '-p', password,
                'ssh', '-o', 'StrictHostKeyChecking=no',
                '-o', 'UserKnownHostsFile=/dev/null',
                '-o', 'ConnectTimeout=10',
                f"{username}@{host_ip}",
                f"mkdir -p '{remote_path}'"
            ]
            
            print(f"📁 Creating directory: {remote_path}")
            subprocess.run(mkdir_cmd, check=False, capture_output=True)
            
            # Transfer file
            scp_cmd = [
                'sshpass', '-p', password,
                'scp', '-o', 'StrictHostKeyChecking=no',
                '-o', 'UserKnownHostsFile=/dev/null',
                '-o', 'ConnectTimeout=30',
                local_file,
                f"{username}@{host_ip}:{remote_path}/{rom_name}"
            ]
            
            print(f"📤 Transferring {rom_name} to {host_ip}:{remote_path}/")
            result = subprocess.run(scp_cmd, capture_output=True, text=True)
            
            if result.returncode == 0:
                print(f"✅ Transfer successful!")
                return True
            else:
                print(f"❌ Transfer failed with password '{password}': {result.stderr}")
                
                # Try alternative muOS passwords
                alt_passwords = ['root', '', 'admin']
                for alt_password in alt_passwords:
                    if alt_password == password:
                        continue
                        
                    print(f"🔄 Trying alternative password: '{alt_password}'")
                    
                    # Try mkdir with alternative password
                    alt_mkdir_cmd = [
                        'sshpass', '-p', alt_password,
                        'ssh', '-o', 'StrictHostKeyChecking=no',
                        '-o', 'UserKnownHostsFile=/dev/null',
                        '-o', 'ConnectTimeout=10',
                        f"{username}@{host_ip}",
                        f"mkdir -p '{remote_path}'"
                    ]
                    subprocess.run(alt_mkdir_cmd, check=False, capture_output=True)
                    
                    # Try SCP with alternative password
                    alt_scp_cmd = [
                        'sshpass', '-p', alt_password,
                        'scp', '-o', 'StrictHostKeyChecking=no',
                        '-o', 'UserKnownHostsFile=/dev/null',
                        '-o', 'ConnectTimeout=30',
                        local_file,
                        f"{username}@{host_ip}:{remote_path}/{rom_name}"
                    ]
                    
                    alt_result = subprocess.run(alt_scp_cmd, capture_output=True, text=True)
                    if alt_result.returncode == 0:
                        print(f"✅ Transfer successful with password '{alt_password}'!")
                        return True
                    else:
                        print(f"❌ Transfer failed with password '{alt_password}': {alt_result.stderr}")
                
                return False
                
        except Exception as e:
            print(f"❌ Transfer error: {e}")
            return False
    
    def send_success_response(self, message_or_data):
        """Send successful response"""
        if isinstance(message_or_data, dict):
            response = {'success': True, **message_or_data}
        else:
            response = {'success': True, 'message': message_or_data}
        self.send_json_response(200, response)
    
    def send_error_response(self, code, message):
        """Send error response"""
        response = {'success': False, 'error': message}
        self.send_json_response(code, response)
    
    def send_json_response(self, code, data):
        """Send JSON response with CORS headers"""
        json_data = json.dumps(data).encode('utf-8')
        
        self.send_response(code)
        self.send_header('Content-Type', 'application/json')
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Content-Length', str(len(json_data)))
        self.end_headers()
        self.wfile.write(json_data)

def main():
    port = 8002
    
    print("🚀 Starting ROM Transfer Service...")
    print(f"📡 Listening on http://localhost:{port}")
    print("📝 This service handles ROM transfers to muOS/Rocknix devices")
    print("⚠️  Make sure 'sshpass' is installed: brew install hudochenkov/sshpass/sshpass")
    print("🔄 Keep this running alongside your main server and proxy")
    print("=" * 60)
    
    # Check if sshpass is available
    try:
        subprocess.run(['sshpass', '-V'], capture_output=True, check=True)
        print("✅ sshpass is available")
    except (subprocess.CalledProcessError, FileNotFoundError):
        print("❌ sshpass not found! Install with: brew install hudochenkov/sshpass/sshpass")
        sys.exit(1)
    
    try:
        httpd = HTTPServer(('localhost', port), TransferHandler)
        httpd.serve_forever()
    except KeyboardInterrupt:
        print("\n🛑 Transfer service stopped")
    except OSError as e:
        if "Address already in use" in str(e):
            print(f"❌ Port {port} is already in use. Try a different port or stop the existing service.")
        else:
            print(f"❌ Error starting server: {e}")

if __name__ == '__main__':
    main()
