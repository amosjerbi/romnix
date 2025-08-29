// Platform definitions from the Android app
const platforms = {
    NES: {
        id: "nes",
        label: "NES",
        archiveUrl: "",
        extensions: ["7z", "zip", "nes"],
        icon: "sports_esports"
    },
    SNES: {
        id: "snes",
        label: "SNES",
        archiveUrl: "",
        extensions: ["7z", "zip", "smc", "sfc"],
        icon: "sports_esports"
    },
    GENESIS: {
        id: "genesis",
        label: "Genesis",
        archiveUrl: "",
        extensions: ["7z", "zip", "md", "gen"],
        icon: "sports_esports"
    },
    GB: {
        id: "gb",
        label: "Game Boy",
        archiveUrl: "",
        extensions: ["7z", "zip", "gb"],
        icon: "gamepad"
    },
    GBA: {
        id: "gba",
        label: "GBA",
        archiveUrl: "",
        extensions: ["7z", "zip", "gba"],
        icon: "gamepad"
    },
    GBC: {
        id: "gbc",
        label: "GBC",
        archiveUrl: "",
        extensions: ["7z", "zip", "gbc"],
        icon: "gamepad"
    },
    GAMEGEAR: {
        id: "gamegear",
        label: "Game Gear",
        archiveUrl: "",
        extensions: ["7z", "zip", "gg"],
        icon: "gamepad"
    },
    NGP: {
        id: "ngp",
        label: "Neo Geo Pocket",
        archiveUrl: "",
        extensions: ["7z", "zip", "ngp", "ngc"],
        icon: "gamepad"
    },
    SMS: {
        id: "sms",
        label: "Sega Master System",
        archiveUrl: "",
        extensions: ["7z", "zip", "sms"],
        icon: "sports_esports"
    },
    SEGACD: {
        id: "segacd",
        label: "Sega CD",
        archiveUrl: "",
        extensions: ["7z", "zip", "chd", "cue", "bin", "iso"],
        icon: "album"
    },
    SEGA32X: {
        id: "sega32x",
        label: "Sega 32X",
        archiveUrl: "",
        extensions: ["7z", "zip", "32x"],
        icon: "sports_esports"
    },
    SATURN: {
        id: "saturn",
        label: "Sega Saturn",
        archiveUrl: "",
        extensions: ["7z", "zip", "chd", "cue", "bin", "iso"],
        icon: "videogame_asset"
    },
    TG16: {
        id: "tg16",
        label: "TurboGrafx-16",
        archiveUrl: "",
        extensions: ["7z", "zip", "pce"],
        icon: "sports_esports"
    },
    PS1: {
        id: "ps1",
        label: "PlayStation",
        archiveUrl: "",
        extensions: ["7z", "zip", "chd", "cue", "bin", "iso"],
        icon: "videogame_asset"
    },
    N64: {
        id: "n64",
        label: "Nintendo 64",
        archiveUrl: "",
        extensions: ["7z", "zip", "z64", "n64", "v64"],
        icon: "videogame_asset"
    },
    DREAMCAST: {
        id: "dreamcast",
        label: "Dreamcast",
        archiveUrl: "",
        extensions: ["7z", "zip", "chd", "cdi", "gdi"],
        icon: "videogame_asset"
    }
};

// ROM repository class for fetching ROMs
class RomRepository {
    constructor() {
        this.linkRegex = /<a\s+[^>]*href="([^"]+)"/gi;
        
        // CORS proxy configuration for development
        // Using multiple proxies as fallbacks
        this.corsProxies = [
            // Local proxy server (run proxy_server.py)
            'http://localhost:8001/?url=',
            // Traditional CORS proxies that return raw HTML
            'https://api.allorigins.win/raw?url=',
            'https://corsproxy.io/?',
            'https://api.codetabs.com/v1/proxy?quest=',
            'https://cors-anywhere.herokuapp.com/',
            'https://thingproxy.freeboard.io/fetch/'
        ];
        this.currentProxyIndex = 0;
        
        // Demo mode disabled: always use live directory parsing via proxies
        this.forceDemoMode = false;

        // Helper to build proxy URL for different proxy formats
        this.buildProxyUrl = (proxyBase, targetUrl) => {
            try {
                // Query-style proxies with encoded URL parameter
                if (proxyBase.includes('?url=') || proxyBase.includes('?quest=')) {
                    return proxyBase + encodeURIComponent(targetUrl);
                }
                // Simple query-style proxy
                if (proxyBase.endsWith('?')) {
                    return proxyBase + encodeURIComponent(targetUrl);
                }
                // Path-style proxies that append the full URL
                if (proxyBase.endsWith('/')) {
                    return proxyBase + targetUrl;
                }
                return proxyBase + targetUrl;
            } catch (_) {
                return proxyBase + targetUrl;
            }
        };
    }

    async fetchHtml(url) {
        console.log(`Fetching HTML for: ${url}`);
        
        // Try multiple CORS proxies
        for (let i = 0; i < this.corsProxies.length; i++) {
            try {
                const proxyUrl = this.buildProxyUrl(this.corsProxies[i], url);
                console.log(`Trying proxy ${i + 1}/${this.corsProxies.length}: ${this.corsProxies[i]}`);
                console.log(`Full proxy URL: ${proxyUrl}`);
                
                // Special handling for localhost proxy
                if (this.corsProxies[i].includes('localhost:8001')) {
                    console.log('🏠 Using local proxy server...');
                }
                
                const controller = new AbortController();
                const timeoutId = setTimeout(() => {
                    console.warn(`Proxy ${i + 1} timeout after 8 seconds`);
                    controller.abort();
                }, 8000); // Reduced timeout to 8 seconds
                
                console.log(`Starting fetch for proxy ${i + 1}...`);
                const response = await fetch(proxyUrl, { method: 'GET', credentials: 'omit', signal: controller.signal });
                clearTimeout(timeoutId);
                console.log(`Fetch completed for proxy ${i + 1}`);

                console.log(`Proxy ${i + 1} response status: ${response.status}`);
                
                if (response.ok) {
                    const html = await response.text();
                    console.log(`Proxy ${i + 1} returned ${html.length} characters`);
                    
                    const htmlLower = html.toLowerCase();
                    
                    // Check if this is actual HTML with anchor tags
                    const hasAnchors = htmlLower.includes('<a ') && htmlLower.includes('href=');
                    const isMarkdown = htmlLower.includes('markdown content:') || htmlLower.includes('______');
                    
                    // Detect common anti-bot or error pages; try next proxy if detected
                    const looksBlocked =
                        htmlLower.includes('error 403') ||
                        (htmlLower.includes('cloudflare') && htmlLower.includes('attention required')) ||
                        htmlLower.includes('access denied') ||
                        htmlLower.includes('captcha') ||
                        isMarkdown;
                    
                    if (!looksBlocked && hasAnchors) {
                        console.log(`Successfully fetched HTML using proxy ${i + 1}`);
                        return html;
                    }
                    
                    if (isMarkdown) {
                        console.warn(`Proxy ${i + 1} returned markdown content instead of HTML; trying next`);
                    } else if (!hasAnchors) {
                        console.warn(`Proxy ${i + 1} returned content without anchor tags; trying next`);
                    } else {
                        console.warn(`Proxy ${i + 1} returned blocked/error content; trying next`);
                    }
                }
                
                console.warn(`Proxy ${i + 1} failed with status: ${response.status}`);
            } catch (error) {
                console.warn(`Proxy ${i + 1} failed with error:`, error.message);
            }
        }
        
        console.error('All proxies failed, unable to fetch HTML for:', url);
        console.log('Suggestion: Try one of these alternatives:');
        console.log('1. Install a CORS browser extension (like "CORS Unblock")');
        console.log('2. Use the browser flag: --disable-web-security --user-data-dir=/tmp/chrome_dev_test');
        console.log('3. Set up a local proxy server');
        
        // Show helpful error message to user
        if (window.romApp) {
            window.romApp.showNotification('All proxy services failed. Check console for alternatives.', 'error');
        }
        
        return '';
    }

    buildAbsoluteUrl(base, href) {
        const normalizedBase = base.replace(/\/$/, '') + '/';
        if (href.startsWith('http://') || href.startsWith('https://')) {
            return href;
        }
        return normalizedBase + href.replace(/^\//, '');
    }

    isDirectoryLink(href) {
        if (!href || href.trim() === '') return false;
        if (href.startsWith('?')) return false;
        if (href === '/') return false;
        if (href.startsWith('../')) return false;
        if (!href.endsWith('/')) return false;
        
        // Don't follow external links - only relative paths
        if (href.startsWith('http://') || href.startsWith('https://')) return false;
        
        // First check: Don't treat ROM files as directories, even if they have trailing slashes
        const cleanHref = href.replace(/\/$/, ''); // Remove trailing slash
        const romExtensions = ['zip', '7z', 'nes', 'smc', 'md', 'gb', 'gbc', 'gba', 'gen', 'sfc', 'rar'];
        const hasRomExtension = romExtensions.some(ext => 
            cleanHref.toLowerCase().endsWith('.' + ext)
        );
        if (hasRomExtension) {
            return false; // This is a ROM file, not a directory
        }
        
        // Exclude common navigation/system directories that won't contain ROMs
        const navigationPaths = [
            'about', 'donate', 'help', 'contact', 'terms', 'privacy', 
            'blog', 'news', 'support', 'login', 'register', 'account',
            'api', 'admin', 'assets', 'css', 'js', 'images', 'img',
            'static', 'public', 'common', 'shared', 'lib', 'libraries'
        ];
        
        const pathLower = href.toLowerCase().replace(/\//g, '');
        return !navigationPaths.includes(pathLower);
    }

    async parseDirectoryRecursively(baseUrl, extensions, depth = 2, visitedUrls = new Set()) {
        const results = [];
        
        console.log(`parseDirectoryRecursively called with: ${baseUrl}, depth: ${depth}`);
        
        // Prevent infinite loops by tracking visited URLs
        const normalizedUrl = baseUrl.replace(/\/+$/, '');
        if (visitedUrls.has(normalizedUrl)) {
            console.log(`Already visited ${normalizedUrl}, skipping to prevent loops`);
            return results;
        }
        visitedUrls.add(normalizedUrl);
        
        if (depth <= 0) {
            console.log('Max depth reached, returning empty results');
            return results;
        }

        try {
            console.log(`Fetching HTML for: ${baseUrl}`);
            const html = await this.fetchHtml(baseUrl);
            if (!html) {
                console.log('No HTML returned, returning empty results');
                return results;
            }

            console.log(`Received HTML of length: ${html.length}`);
            console.log('HTML sample:', html.substring(0, 500) + '...');

            const links = [];
            let match;
            
            // Reset regex lastIndex
            this.linkRegex.lastIndex = 0;
            
            while ((match = this.linkRegex.exec(html)) !== null) {
                links.push(match[1]);
            }
            
            console.log(`Extracted ${links.length} total links from HTML`);
            console.log('First 10 links (unfiltered):', links.slice(0, 10));
            
            // Filter out obvious navigation/system links to focus on ROM files
            const filteredLinks = links.filter(href => {
                // Skip obvious navigation links
                if (href.startsWith('#') || href.startsWith('mailto:') || href.includes('change.org')) return false;
                if (href.startsWith('/') && !href.includes('.zip') && !href.includes('.7z') && !href.endsWith('/')) return false;
                if (href.startsWith('https://') && !href.includes('archive.org/download')) return false;
                if (href.includes('/details/') || href.includes('/search/') || href.includes('/account/')) return false;
                
                // Keep potential ROM files and directories (handle .zip/ URLs)
                if (href.match(/\.(zip|7z|nes|smc|md|gb|gbc|gba|gen|sfc)\/?$/i)) return true;
                if (href.endsWith('/') && !href.includes('donate') && !href.includes('help')) return true;
                
                return false;
            });
            
            console.log(`Filtered to ${filteredLinks.length} potential ROM links`);
            console.log('First 10 ROM-related links:', filteredLinks.slice(0, 10));
            console.log(`Processing ${filteredLinks.length} links, looking for extensions: [${extensions.join(', ')}]`);
            
            // Use filtered links but still limit to prevent overload
            const maxLinks = Math.min(filteredLinks.length, 3000); // Much higher limit for comprehensive searching
            console.log(`Processing first ${maxLinks} links out of ${filteredLinks.length} total`);
            
            // Use filteredLinks instead of links
            const linksToProcess = filteredLinks;
            
            for (let i = 0; i < maxLinks; i++) {
                const href = linksToProcess[i];
                if (!href) continue; // Skip undefined links
                
                const decoded = decodeURIComponent(href);
                const lower = decoded.toLowerCase();

                if (this.isDirectoryLink(href) && depth > 1) {
                    console.log(`Found directory: ${decoded}, recursing...`);
                    // Recursively search subdirectories
                    const nextUrl = this.buildAbsoluteUrl(baseUrl, href);
                    console.log(`🔍 Base URL: ${baseUrl}`);
                    console.log(`🔍 Directory href: ${href}`);
                    console.log(`🔍 Built URL: ${nextUrl}`);
                    const subResults = await this.parseDirectoryRecursively(nextUrl, extensions, depth - 1, visitedUrls);
                    results.push(...subResults);
                    continue;
                }

                // Check if file has one of the target extensions (handle trailing slashes)
                const cleanHref = href.replace(/\/$/, ''); // Remove trailing slash
                const hasTargetExtension = extensions.some(ext => 
                    cleanHref.toLowerCase().endsWith('.' + ext.toLowerCase())
                );

                if (hasTargetExtension) {
                    // Handle trailing slashes in URL paths
                    const pathParts = decoded.split('/').filter(part => part.length > 0);
                    const nameOnly = pathParts[pathParts.length - 1];
                    const fullUrl = this.buildAbsoluteUrl(baseUrl, cleanHref); // Use cleanHref without trailing slash
                    
                    console.log(`✓ Found ROM: ${nameOnly}`);
                    results.push({
                        displayName: nameOnly,
                        downloadUrl: fullUrl,
                        size: null // Could be extracted from HTML if needed
                    });
                }
                
                // Small delay to prevent overwhelming the server
                if (i < maxLinks - 1 && this.isDirectoryLink(href)) {
                    await new Promise(resolve => setTimeout(resolve, 100));
                }
            }
            
            console.log(`Found ${results.length} ROM files in this directory`);
            console.log('ROM results:', results.map(r => r.displayName));
            
            // If no results and few links, show what we're looking for
            if (results.length === 0 && linksToProcess.length < 20) {
                console.log('No ROMs found. All filtered links:', linksToProcess);
                console.log('Looking for extensions:', extensions);
            }
        } catch (error) {
            console.error('Error parsing directory:', baseUrl, error);
        }

        console.log(`parseDirectoryRecursively returning ${results.length} results for ${baseUrl}`);
        return results;
    }

    async search(platform, searchTerm = '') {
        try {
            console.log(`Searching ${platform.label} for "${searchTerm}"...`);
            console.log(`🔗 Platform archive URL: ${platform.archiveUrl}`);
            
            // Check if platform has a configured archive URL
            if (!platform.archiveUrl || platform.archiveUrl.trim() === '') {
                console.log(`❌ No archive URL configured for ${platform.label}`);
                
                // Show helpful notification to user
                if (window.romApp) {
                    window.romApp.showNotification(
                        `No archive URL configured for ${platform.label}. Click Settings (⚙️) to configure.`, 
                        'warning'
                    );
                }
                return [];
            }
            
            // Enhanced search logic with direct download capabilities
            const startUrl = platform.archiveUrl.replace(/\/$/, '') + '/';
            console.log(`🎯 Final search URL: ${startUrl}`);
            const extensions = platform.extensions.map(ext => ext.toLowerCase());
            
            const rawItems = await this.parseDirectoryRecursively(startUrl, extensions, 2, new Set());
            console.log(`🎯 parseDirectoryRecursively returned ${rawItems.length} raw items:`, rawItems);
            
            // Add platform info to each ROM
            const items = rawItems.map(item => ({
                ...item,
                platform: platform
            }));
            console.log(`🎮 Final items with platform info:`, items);

            // Filter by search term if provided
            if (searchTerm && searchTerm.trim() !== '') {
                const filtered = items.filter(item => 
                    item.displayName.toLowerCase().includes(searchTerm.toLowerCase())
                );
                console.log(`Found ${filtered.length} ROMs matching "${searchTerm}" for ${platform.label}`);
                return filtered;
            }

            console.log(`Found ${items.length} ROMs for ${platform.label}`);
            return items;
        } catch (error) {
            console.error('Error searching platform:', platform.label, error);
            return [];
        }
    }

    async searchAll(searchTerm = '') {
        const allPlatforms = Object.values(platforms);
        const results = [];

        console.log(`Searching all platforms for "${searchTerm}"...`);

        // Search all platforms in parallel for better performance
        const searchPromises = allPlatforms.map(platform => 
            this.search(platform, searchTerm)
        );

        try {
            const platformResults = await Promise.all(searchPromises);
            
            for (const platformRoms of platformResults) {
                results.push(...platformRoms);
            }

            console.log(`Found total ${results.length} ROMs across all platforms`);
            return results;
        } catch (error) {
            console.error('Error searching all platforms:', error);
            return results; // Return partial results
        }
    }

    // Toggle demo mode on/off
    setDemoMode(enabled) {
        this.forceDemoMode = enabled;
        console.log(`Demo mode ${enabled ? 'enabled' : 'disabled'}`);
    }

    // Check if in demo mode
    isDemoMode() {
        return this.forceDemoMode;
    }
    
    // Enhanced download functionality
    async downloadRom(rom, onProgress = null) {
        try {
            console.log(`🚀 Starting download: ${rom.displayName}`);
            
            // Create download directory structure
            const platformDir = `${rom.platform.label}_ROMs`;
            
            // Use the same proxy to download the file
            const response = await this.fetchRomFile(rom.downloadUrl);
            
            if (!response) {
                throw new Error('Failed to fetch ROM file');
            }
            
            // Get the file as a blob
            const blob = await response.blob();
            
            // Create download link
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.style.display = 'none';
            a.href = url;
            a.download = rom.displayName;
            
            // Trigger download
            document.body.appendChild(a);
            a.click();
            
            // Cleanup
            window.URL.revokeObjectURL(url);
            document.body.removeChild(a);
            
            console.log(`✅ Download completed: ${rom.displayName}`);
            return true;
            
        } catch (error) {
            console.error(`❌ Download failed for ${rom.displayName}:`, error);
            return false;
        }
    }
    
    async fetchRomFile(url) {
        console.log(`Fetching ROM file: ${url}`);
        
        // Try multiple CORS proxies for file download
        for (let i = 0; i < this.corsProxies.length; i++) {
            try {
                const proxyUrl = this.buildProxyUrl(this.corsProxies[i], url);
                console.log(`Trying download proxy ${i + 1}/${this.corsProxies.length}: ${this.corsProxies[i]}`);
                
                const controller = new AbortController();
                const timeoutId = setTimeout(() => {
                    console.warn(`Download proxy ${i + 1} timeout after 30 seconds`);
                    controller.abort();
                }, 30000); // Longer timeout for downloads
                
                const response = await fetch(proxyUrl, { 
                    method: 'GET', 
                    credentials: 'omit', 
                    signal: controller.signal 
                });
                clearTimeout(timeoutId);
                
                if (response.ok) {
                    console.log(`✅ Download successful using proxy ${i + 1}`);
                    return response;
                }
                
            } catch (error) {
                console.log(`Download proxy ${i + 1} failed:`, error.message);
                continue;
            }
        }
        
        console.error('All download proxies failed');
        return null;
    }
    
    async downloadMultipleRoms(roms, onProgress = null) {
        console.log(`🚀 Starting batch download of ${roms.length} ROMs`);
        
        const results = {
            successful: [],
            failed: [],
            total: roms.length
        };
        
        for (let i = 0; i < roms.length; i++) {
            const rom = roms[i];
            
            if (onProgress) {
                onProgress({
                    current: i + 1,
                    total: roms.length,
                    currentRom: rom.displayName,
                    completed: results.successful.length,
                    failed: results.failed.length
                });
            }
            
            console.log(`[${i + 1}/${roms.length}] Downloading: ${rom.displayName}`);
            
            const success = await this.downloadRom(rom);
            
            if (success) {
                results.successful.push(rom);
            } else {
                results.failed.push(rom);
            }
            
            // Brief pause between downloads to avoid overwhelming the server
            if (i < roms.length - 1) {
                await new Promise(resolve => setTimeout(resolve, 1000));
            }
        }
        
        console.log(`✅ Batch download completed: ${results.successful.length} successful, ${results.failed.length} failed`);
        return results;
    }
    
    // Test method to verify proxy functionality
    async testProxy() {
        console.log('Testing proxy functionality...');
        const testUrl = 'https://myrient.erista.me/files/No-Intro/Nintendo%20-%20Nintendo%20Entertainment%20System%20%28Headered%29/';
        
        try {
            const html = await this.fetchHtml(testUrl);
            console.log('Proxy test result:', html ? `Success (${html.length} chars)` : 'Failed');
            return html.length > 0;
        } catch (error) {
            console.error('Proxy test failed:', error);
            return false;
        }
    }
}

// Downloader class for handling file downloads
class WebDownloader {
    download(romItem) {
        try {
            // Create a temporary anchor element to trigger download
            const link = document.createElement('a');
            link.href = romItem.downloadUrl;
            link.download = romItem.displayName;
            link.target = '_blank';
            
            // Some browsers require the link to be in the DOM
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
            
            // Show success notification
            this.showNotification(`Downloading ${romItem.displayName}`, 'success');
            
        } catch (error) {
            console.error('Download error:', error);
            this.showNotification(`Failed to download ${romItem.displayName}`, 'error');
        }
    }

    downloadMultiple(romItems, onProgress) {
        let completed = 0;
        const total = romItems.length;

        romItems.forEach((item, index) => {
            setTimeout(() => {
                this.download(item);
                completed++;
                
                if (onProgress) {
                    onProgress(completed, total, `Downloaded ${item.displayName}`);
                }
                
                if (completed === total) {
                    this.showNotification(`Downloaded ${total} ROMs successfully`, 'success');
                }
            }, index * 500); // Stagger downloads by 500ms
        });
    }

    showNotification(message, type = 'info') {
        // Create and show a temporary notification
        const notification = document.createElement('div');
        notification.className = `notification notification-${type}`;
        notification.style.cssText = `
            position: fixed;
            top: 20px;
            right: 20px;
            background: ${type === 'error' ? '#BA1A1A' : type === 'success' ? '#006D40' : '#797ED2'};
            color: white;
            padding: 12px 20px;
            border-radius: 12px;
            box-shadow: 0px 2px 6px 2px rgba(0, 0, 0, 0.15);
            z-index: 10000;
            max-width: 300px;
            font-weight: 500;
            transform: translateX(100%);
            transition: transform 0.3s ease;
        `;
        notification.textContent = message;
        
        document.body.appendChild(notification);
        
        // Trigger animation
        setTimeout(() => {
            notification.style.transform = 'translateX(0)';
        }, 10);
        
        // Remove after delay
        setTimeout(() => {
            notification.style.transform = 'translateX(100%)';
            setTimeout(() => {
                if (notification.parentNode) {
                    notification.parentNode.removeChild(notification);
                }
            }, 300);
        }, 3000);
    }
}

// Export for use in main script
window.platforms = platforms;
window.RomRepository = RomRepository;
window.WebDownloader = WebDownloader;
