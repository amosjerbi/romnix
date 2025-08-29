#!/usr/bin/env python3
"""
Simple CORS proxy server for ROM Downloader
Run this alongside the main HTTP server to bypass CORS restrictions.
"""

import http.server
import socketserver
import urllib.request
import urllib.parse
import json
from urllib.error import URLError, HTTPError

class CORSProxyHandler(http.server.BaseHTTPRequestHandler):
    def do_GET(self):
        # Extract the target URL from the query parameter
        parsed_path = urllib.parse.urlparse(self.path)
        query_params = urllib.parse.parse_qs(parsed_path.query)
        
        if 'url' not in query_params:
            self.send_error(400, "Missing 'url' parameter")
            return
            
        target_url = query_params['url'][0]
        
        # Ensure the URL is properly encoded
        try:
            # Parse and rebuild the URL to handle special characters
            from urllib.parse import quote, unquote
            
            # The URL might already be encoded, so decode it first, then re-encode properly
            decoded_url = unquote(target_url)
            
            # Split URL into parts and encode only the path part
            if '://' in decoded_url:
                protocol_and_domain, path = decoded_url.split('://', 1)
                if '/' in path:
                    domain, file_path = path.split('/', 1)
                    # Encode the file path properly
                    encoded_path = quote(file_path, safe='/()-')
                    target_url = f"{protocol_and_domain}://{domain}/{encoded_path}"
                else:
                    target_url = decoded_url
            
            print(f"📡 Fetching: {target_url}")
            
            # Create request with proper headers
            req = urllib.request.Request(
                target_url,
                headers={
                    'User-Agent': 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0 Safari/537.36',
                    'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
                    'Accept-Language': 'en-US,en;q=0.9'
                }
            )
            
            # Fetch the content
            with urllib.request.urlopen(req, timeout=30) as response:
                content = response.read()
                content_type = response.headers.get('Content-Type', 'text/html')
                
                # Send successful response with CORS headers
                self.send_response(200)
                self.send_header('Access-Control-Allow-Origin', '*')
                self.send_header('Access-Control-Allow-Methods', 'GET, POST, OPTIONS')
                self.send_header('Access-Control-Allow-Headers', 'Content-Type')
                self.send_header('Content-Type', content_type)
                self.send_header('Content-Length', str(len(content)))
                self.end_headers()
                
                self.wfile.write(content)
                
                print(f"✓ Successfully proxied: {target_url}")
                
        except HTTPError as e:
            print(f"✗ HTTP Error {e.code} for: {target_url}")
            self.send_error(e.code, f"HTTP Error: {e.reason}")
        except URLError as e:
            print(f"✗ URL Error for: {target_url} - {e.reason}")
            self.send_error(500, f"URL Error: {e.reason}")
        except Exception as e:
            print(f"✗ General Error for: {target_url} - {str(e)}")
            self.send_error(500, f"Proxy Error: {str(e)}")
    
    def do_OPTIONS(self):
        # Handle preflight requests
        self.send_response(200)
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Access-Control-Allow-Methods', 'GET, POST, OPTIONS')
        self.send_header('Access-Control-Allow-Headers', 'Content-Type')
        self.end_headers()
    
    def log_message(self, format, *args):
        # Suppress default logging, we handle it above
        pass

def main():
    PORT = 8001
    
    print("🚀 Starting CORS Proxy Server...")
    print(f"📡 Listening on http://localhost:{PORT}")
    print("📝 Usage: http://localhost:8001/?url=<encoded_target_url>")
    print("🔄 Use this proxy in your web app by updating the corsProxies array")
    print("⚠️  Keep this running alongside your main HTTP server (port 8000)")
    print("\n" + "="*60 + "\n")
    
    try:
        with socketserver.TCPServer(("", PORT), CORSProxyHandler) as httpd:
            httpd.serve_forever()
    except KeyboardInterrupt:
        print("\n\n🛑 Proxy server stopped")
    except OSError as e:
        if e.errno == 48:  # Address already in use
            print(f"❌ Port {PORT} is already in use. Try a different port or stop the existing service.")
        else:
            print(f"❌ Error starting server: {e}")

if __name__ == "__main__":
    main()
