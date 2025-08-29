#!/usr/bin/env python3
"""
Service Orchestrator for ROM Downloader Web
Starts all necessary services for the web application
"""

import os
import sys
import time
import signal
import threading
import subprocess
import http.server
import socketserver
from contextlib import suppress

class ServiceOrchestrator:
    def __init__(self):
        self.processes = []
        self.frontend_server = None
        
    def start_frontend_server(self):
        """Start the frontend HTTP server"""
        print("🌐 Starting Frontend Server on port 8000...")
        os.chdir('/workspaces' if os.path.exists('/workspaces') else '.')
        
        class CustomHTTPRequestHandler(http.server.SimpleHTTPRequestHandler):
            def end_headers(self):
                # Add CORS headers for development
                self.send_header('Cross-Origin-Embedder-Policy', 'require-corp')
                self.send_header('Cross-Origin-Opener-Policy', 'same-origin')
                super().end_headers()
                
            def log_message(self, format, *args):
                # Suppress verbose logging
                pass
        
        try:
            self.frontend_server = socketserver.TCPServer(("", 8000), CustomHTTPRequestHandler)
            self.frontend_server.serve_forever()
        except Exception as e:
            print(f"❌ Frontend server error: {e}")
    
    def start_proxy_server(self):
        """Start the CORS proxy server"""
        print("🔧 Starting CORS Proxy Server on port 8001...")
        try:
            process = subprocess.Popen([
                sys.executable, 'proxy_server.py'
            ], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
            self.processes.append(process)
            return process
        except Exception as e:
            print(f"❌ Proxy server error: {e}")
            return None
    
    def start_transfer_service(self):
        """Start the ROM transfer service"""
        print("📡 Starting ROM Transfer Service on port 8002...")
        try:
            process = subprocess.Popen([
                sys.executable, 'transfer_service.py'
            ], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
            self.processes.append(process)
            return process
        except Exception as e:
            print(f"❌ Transfer service error: {e}")
            return None
    
    def check_dependencies(self):
        """Check if required dependencies are available"""
        print("🔍 Checking dependencies...")
        
        # Check if sshpass is available
        try:
            subprocess.run(['sshpass', '-V'], capture_output=True, check=True)
            print("✅ sshpass is available")
        except (subprocess.CalledProcessError, FileNotFoundError):
            print("⚠️  sshpass not found - SSH transfers may not work")
            print("   In Codespaces, this should be installed automatically")
        
        # Check Python version
        if sys.version_info >= (3, 8):
            print(f"✅ Python {sys.version.split()[0]} is available")
        else:
            print(f"⚠️  Python version {sys.version.split()[0]} may not be fully supported")
    
    def setup_signal_handlers(self):
        """Setup signal handlers for graceful shutdown"""
        def signal_handler(signum, frame):
            print(f"\n🛑 Received signal {signum}, shutting down gracefully...")
            self.shutdown()
            sys.exit(0)
        
        signal.signal(signal.SIGINT, signal_handler)
        signal.signal(signal.SIGTERM, signal_handler)
    
    def shutdown(self):
        """Gracefully shutdown all services"""
        print("🔄 Shutting down services...")
        
        # Stop frontend server
        if self.frontend_server:
            self.frontend_server.shutdown()
            self.frontend_server.server_close()
        
        # Stop other processes
        for process in self.processes:
            with suppress(Exception):
                process.terminate()
                process.wait(timeout=5)
    
    def start_all_services(self):
        """Start all services"""
        print("🚀 ROM Downloader Web - Service Orchestrator")
        print("=" * 60)
        
        # Check dependencies
        self.check_dependencies()
        
        # Setup signal handlers
        self.setup_signal_handlers()
        
        # Start background services
        proxy_process = self.start_proxy_server()
        transfer_process = self.start_transfer_service()
        
        # Wait a moment for services to start
        time.sleep(2)
        
        # Check if services started successfully
        services_status = []
        if proxy_process and proxy_process.poll() is None:
            services_status.append("✅ CORS Proxy (port 8001)")
        else:
            services_status.append("❌ CORS Proxy (port 8001)")
            
        if transfer_process and transfer_process.poll() is None:
            services_status.append("✅ Transfer Service (port 8002)")
        else:
            services_status.append("❌ Transfer Service (port 8002)")
        
        print("\n📊 Service Status:")
        for status in services_status:
            print(f"   {status}")
        
        print(f"\n🌐 Frontend will be available at:")
        print(f"   • Local: http://localhost:8000")
        print(f"   • Codespaces: https://<codespace-name>-8000.github.dev")
        print(f"\n💡 In GitHub Codespaces:")
        print(f"   • The frontend will auto-open in your browser")
        print(f"   • All services run in the cloud environment")
        print(f"   • You can connect to devices on your local network")
        print(f"   • Use port forwarding for local device access")
        print("\n" + "=" * 60)
        
        # Start frontend server (blocking)
        try:
            self.start_frontend_server()
        except KeyboardInterrupt:
            self.shutdown()

def main():
    """Main entry point"""
    orchestrator = ServiceOrchestrator()
    orchestrator.start_all_services()

if __name__ == '__main__':
    main()
