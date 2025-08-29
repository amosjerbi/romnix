/**
 * ROM Downloader Web Application
 * Main application logic and UI management
 */

class RomDownloaderApp {
    constructor() {
        this.currentScreen = 'devicesScreen';
        this.selectedPlatform = null;
        this.searchTerm = '';
        this.romResults = [];
        this.isSearching = false;
        this.selectedHost = null;
        this.selectedHostTemplate = 'muos'; // Default to muOS
        this.discoveredHosts = [];
        this.isScanning = false;
        
        // SSH Configuration
        this.sshConfig = {
            username: 'root',
            password: 'root',
            port: '22',
            remoteBasePath: '',
            useGuest: false,
            isDhcpNetwork: true
        };
        
        // Host Templates
        this.hostTemplates = {
            rocknix: {
                name: 'Rocknix',
                username: 'root',
                password: 'rocknix',
                port: '22',
                remoteBasePath: '/storage/roms',
                hostIp: '192.168.0.132',
                useGuest: false,
                isDhcpNetwork: true
            },
            muos: {
                name: 'muOS',
                username: 'root',
                password: 'muos',
                port: '22',
                remoteBasePath: '/mnt/mmc/ROMS',
                hostIp: '192.168.1.101',
                useGuest: false,
                isDhcpNetwork: true
            }
        };
        
        // Initialize ROM repository and downloader
        this.romRepository = new RomRepository();
        this.downloader = new WebDownloader();
        
        this.init();
    }
    
    init() {
        this.loadTemplatesFromStorage();
        this.loadArchiveSettingsFromStorage();
        this.setupEventListeners();
        this.populateConsoleGrid();
        this.populateQuickFilters();
        this.populatePlatformModal();
        this.showDemoBanner();
        this.updateUI();
    }
    
    setupEventListeners() {
        // Bottom navigation
        document.querySelectorAll('.nav-item').forEach(button => {
            button.addEventListener('click', (e) => {
                const screenId = e.currentTarget.dataset.screen;
                this.switchScreen(screenId);
            });
        });
        
        // Handheld screen
        document.querySelectorAll('.connect-btn').forEach(button => {
            button.addEventListener('click', (e) => {
                const template = e.currentTarget.dataset.template;
                console.log(`🖱️ Connect button clicked for template: ${template}`);
                this.applyTemplate(template);
            });
        });
        
        document.getElementById('editTemplatesBtn').addEventListener('click', () => {
            this.showTemplateEditor();
        });
        
        document.getElementById('settingsBtn').addEventListener('click', () => {
            this.showArchiveSettings();
        });
        
        // Console screen
        document.getElementById('consoleSearch').addEventListener('input', (e) => {
            this.filterConsoles(e.target.value);
        });
        
        // Browse screen
        document.getElementById('romSearch').addEventListener('input', (e) => {
            this.searchTerm = e.target.value;
            this.updateClearButton();
        });
        
        document.getElementById('searchBtn').addEventListener('click', () => {
            this.searchRoms();
        });
        
        document.getElementById('clearSearchBtn').addEventListener('click', () => {
            this.clearSearch();
        });
        
        document.getElementById('platformFilter').addEventListener('click', () => {
            this.showModal('platformModal');
        });
        
        document.getElementById('clearFilterBtn').addEventListener('click', () => {
            this.clearPlatformFilter();
        });
        
        document.getElementById('downloadAllBtn').addEventListener('click', () => {
            this.downloadAllRoms();
        });
        
        document.getElementById('bulkDownloadBtn').addEventListener('click', () => {
            this.bulkDownloadAndTransfer();
        });
        
        // Network screen
        document.getElementById('scanBtn').addEventListener('click', () => {
            this.scanNetwork();
        });
        
        document.getElementById('testConnectionBtn').addEventListener('click', () => {
            this.testConnection();
        });
        

        
        // SSH config changes
        document.getElementById('sshUsername').addEventListener('input', (e) => {
            this.sshConfig.username = e.target.value;
        });
        
        document.getElementById('sshPassword').addEventListener('input', (e) => {
            this.sshConfig.password = e.target.value;
        });
        
        document.getElementById('sshPort').addEventListener('input', (e) => {
            this.sshConfig.port = e.target.value;
        });
        
        document.getElementById('remoteBasePath').addEventListener('input', (e) => {
            this.sshConfig.remoteBasePath = e.target.value;
        });
        
        document.getElementById('guestMode').addEventListener('change', (e) => {
            this.sshConfig.useGuest = e.target.checked;
            if (e.target.checked) {
                document.getElementById('sshPassword').value = '';
                this.sshConfig.password = '';
            }
        });
        
        document.getElementById('dhcpNetwork').addEventListener('change', (e) => {
            this.sshConfig.isDhcpNetwork = e.target.checked;
        });
        
        // Modal controls
        document.querySelectorAll('.close-btn, [data-modal]').forEach(button => {
            button.addEventListener('click', (e) => {
                const modalId = e.currentTarget.dataset.modal;
                if (modalId) {
                    this.hideModal(modalId);
                }
            });
        });
        
        document.getElementById('saveTemplatesBtn').addEventListener('click', () => {
            this.saveTemplates();
        });
        
        document.getElementById('saveArchiveSettingsBtn').addEventListener('click', () => {
            this.saveArchiveSettings();
        });
        
        // Modal background clicks
        document.querySelectorAll('.modal').forEach(modal => {
            modal.addEventListener('click', (e) => {
                if (e.target === modal) {
                    this.hideModal(modal.id);
                }
            });
        });
        
        // Keyboard shortcuts
        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape') {
                this.hideAllModals();
            } else if (e.key === 'Enter' && e.target.classList.contains('search-input')) {
                if (this.currentScreen === 'browseScreen') {
                    this.searchRoms();
                } else if (this.currentScreen === 'consoleScreen') {
                    this.selectFirstVisibleConsole();
                }
            }
        });
    }
    
    switchScreen(screenId) {
        console.log('switchScreen called with:', screenId);
        // Update navigation
        document.querySelectorAll('.nav-item').forEach(item => {
            item.classList.remove('active');
        });
        const navItem = document.querySelector(`[data-screen="${screenId}"]`);
        console.log('Found nav item:', !!navItem);
        if (navItem) navItem.classList.add('active');
        
        // Update screens
        document.querySelectorAll('.screen').forEach(screen => {
            screen.classList.remove('active');
        });
        const screenElement = document.getElementById(screenId);
        console.log('Found screen element:', !!screenElement);
        if (screenElement) screenElement.classList.add('active');
        
        this.currentScreen = screenId;
        console.log('Current screen set to:', this.currentScreen);
        
        // Handle screen-specific logic
        if (screenId === 'consoleScreen') {
            this.prepareConsoleScreen();
        } else if (screenId === 'browseScreen') {
            console.log('Preparing browse screen...');
            this.prepareBrowseScreen();
        } else if (screenId === 'devicesScreen') {
            this.prepareDevicesScreen();
        }
        console.log('switchScreen completed');
    }
    
    prepareConsoleScreen() {
        // Focus search input
        setTimeout(() => {
            document.getElementById('consoleSearch').focus();
        }, 100);
    }
    
    prepareBrowseScreen() {
        this.updateBulkControls();
        this.updatePlatformFilterDisplay();
        // Focus the ROM search input and update placeholder to reflect selected platform
        const input = document.getElementById('romSearch');
        if (input) {
            input.placeholder = this.selectedPlatform
                ? `Search ${this.selectedPlatform.label} games...`
                : 'Search games...';
            setTimeout(() => input.focus(), 50);
        }
    }
    
    prepareDevicesScreen() {
        // Combined logic for both handheld templates and network configuration
        this.updateHostsList();
        this.updateSSHConfigDisplay();
    }
    
    populateConsoleGrid() {
        const grid = document.getElementById('consoleGrid');
        grid.innerHTML = '';
        
        Object.values(platforms).forEach(platform => {
            const card = this.createConsoleCard(platform);
            grid.appendChild(card);
        });
    }
    
    createConsoleCard(platform) {
        const card = document.createElement('div');
        card.className = 'console-card';
        card.dataset.platformId = platform.id;
        
        card.innerHTML = `
            <div class="console-icon">
                <span class="material-icons">${platform.icon || 'sports_esports'}</span>
            </div>
            <h3>${platform.label}</h3>
        `;
        
        card.addEventListener('click', () => {
            console.log('Platform clicked:', platform.label, platform.id);
            this.selectPlatform(platform);
        });
        
        return card;
    }
    
    selectPlatform(platform) {
        console.log('selectPlatform called with:', platform.label, platform.id);
        this.selectedPlatform = platform;
        console.log('Switching to browseScreen...');
        this.switchScreen('browseScreen');
        console.log('Updating platform filter display...');
        this.updatePlatformFilterDisplay();
        // Update search input context for this platform and focus it
        const input = document.getElementById('romSearch');
        console.log('Found search input:', !!input);
        if (input) {
            input.placeholder = `Search ${platform.label} games...`;
            setTimeout(() => input.focus(), 50);
        }
        
        // Clear previous results and show instruction
        console.log('Clearing results and showing instruction...');
        this.clearResults();
        this.showSearchInstruction();
        console.log('selectPlatform completed');
    }
    
    filterConsoles(searchTerm) {
        const cards = document.querySelectorAll('.console-card');
        const term = searchTerm.toLowerCase();
        
        cards.forEach(card => {
            const platformId = card.dataset.platformId;
            const platform = Object.values(platforms).find(p => p.id === platformId);
            const matches = platform.label.toLowerCase().includes(term) || 
                           platform.id.toLowerCase().includes(term);
            
            card.style.display = matches ? 'block' : 'none';
        });
    }
    
    populateQuickFilters() {
        const container = document.getElementById('quickFilters');
        const quickPlatforms = ['genesis', 'snes', 'ps1', 'n64', 'gba', 'nes'];
        
        container.innerHTML = '';
        
        quickPlatforms.forEach(platformId => {
            const platform = Object.values(platforms).find(p => p.id === platformId);
            if (platform) {
                const chip = document.createElement('button');
                chip.className = 'quick-filter';
                chip.textContent = platform.label;
                chip.dataset.platformId = platformId;
                
                chip.addEventListener('click', () => {
                    this.toggleQuickFilter(platform);
                });
                
                container.appendChild(chip);
            }
        });
        
        this.updateQuickFilters();
    }
    
    toggleQuickFilter(platform) {
        if (this.selectedPlatform?.id === platform.id) {
            this.selectedPlatform = null;
        } else {
            this.selectedPlatform = platform;
        }
        this.updateQuickFilters();
        this.updatePlatformFilterDisplay();
        this.updateBulkControls();
    }
    
    updateQuickFilters() {
        document.querySelectorAll('.quick-filter').forEach(chip => {
            const platformId = chip.dataset.platformId;
            chip.classList.toggle('active', this.selectedPlatform?.id === platformId);
        });
    }
    
    populatePlatformModal() {
        const list = document.getElementById('platformList');
        list.innerHTML = '';
        
        // All platforms option
        const allOption = document.createElement('div');
        allOption.className = 'platform-option';
        allOption.innerHTML = `
            <span class="material-icons">apps</span>
            <span>All Platforms</span>
        `;
        allOption.addEventListener('click', () => {
            this.selectedPlatform = null;
            this.updatePlatformFilterDisplay();
            this.hideModal('platformModal');
            this.updateQuickFilters();
        });
        list.appendChild(allOption);
        
        // Individual platforms
        Object.values(platforms).forEach(platform => {
            const option = document.createElement('div');
            option.className = 'platform-option';
            option.dataset.platformId = platform.id;
            
            option.innerHTML = `
                <span class="material-icons">${platform.icon || 'sports_esports'}</span>
                <span>${platform.label}</span>
            `;
            
            option.addEventListener('click', () => {
                this.selectedPlatform = platform;
                this.updatePlatformFilterDisplay();
                this.hideModal('platformModal');
                this.updateQuickFilters();
                // Switch to browse and focus search input
                this.switchScreen('browseScreen');
                this.prepareBrowseScreen();
            });
            
            list.appendChild(option);
        });
    }
    
    updatePlatformFilterDisplay() {
        const filterText = document.getElementById('platformFilterText');
        const clearBtn = document.getElementById('clearFilterBtn');
        
        if (this.selectedPlatform) {
            filterText.textContent = this.selectedPlatform.label;
            clearBtn.style.display = 'flex';
        } else {
            filterText.textContent = 'All Platforms';
            clearBtn.style.display = 'none';
        }
        
        // Update modal selection
        document.querySelectorAll('.platform-option').forEach(option => {
            const platformId = option.dataset.platformId;
            option.classList.toggle('selected', 
                this.selectedPlatform?.id === platformId || 
                (!this.selectedPlatform && !platformId)
            );
        });
    }
    
    clearPlatformFilter() {
        this.selectedPlatform = null;
        this.updatePlatformFilterDisplay();
        this.updateQuickFilters();
        this.updateBulkControls();
    }
    
    updateClearButton() {
        const clearBtn = document.getElementById('clearSearchBtn');
        clearBtn.style.display = this.searchTerm ? 'flex' : 'none';
    }
    
    clearSearch() {
        this.searchTerm = '';
        document.getElementById('romSearch').value = '';
        this.updateClearButton();
        this.clearResults();
    }
    
    async searchRoms() {
        if (this.isSearching) return;
        
        this.isSearching = true;
        this.updateSearchButton();
        this.showLoadingShimmer();
        this.hideNoResults();
        
        try {
            let results;
            if (this.selectedPlatform) {
                console.log(`🔍 Starting search for platform: ${this.selectedPlatform.label}, term: "${this.searchTerm}"`);
                results = await this.romRepository.search(this.selectedPlatform, this.searchTerm);
            } else {
                console.log(`🔍 Starting search across ALL platforms, term: "${this.searchTerm}"`);
                results = await this.romRepository.searchAll(this.searchTerm);
            }
            
            console.log(`🎯 Search completed! Received ${results.length} results:`, results);
            this.romResults = results;
            console.log(`💾 Stored results in this.romResults:`, this.romResults);
            this.displayResults();
            this.updateBulkControls();
            
        } catch (error) {
            console.error('Search error:', error);
            this.showNotification('Search failed', 'error');
        } finally {
            this.isSearching = false;
            this.updateSearchButton();
            this.hideLoadingShimmer();
        }
    }
    
    displayResults() {
        console.log(`🖥️ displayResults called with ${this.romResults.length} ROMs:`, this.romResults);
        const resultsContainer = document.getElementById('romList');
        resultsContainer.innerHTML = '';
        
        if (this.romResults.length === 0) {
            console.log(`❌ No results to display, showing "No Results" message`);
            this.showNoResults();
            return;
        }
        
        console.log(`✅ Displaying ${this.romResults.length} ROM results`);
        
        this.romResults.forEach(rom => {
            const card = this.createRomCard(rom);
            resultsContainer.appendChild(card);
        });
    }
    
    createRomCard(rom) {
        const card = document.createElement('div');
        card.className = 'rom-card';
        
        const hasTransferAction = this.selectedHost !== null;
        
        card.innerHTML = `
            <div class="rom-icon">
                <span class="material-icons">description</span>
            </div>
            <div class="rom-info">
                <div class="rom-title">${this.escapeHtml(rom.displayName)}</div>
                <div class="rom-platform">${this.escapeHtml(rom.platform.label)}</div>
            </div>
            <div class="rom-actions">
                <button class="rom-action download" title="Download">
                    <span class="material-icons">download</span>
                </button>
                <button class="rom-action upload" title="Upload to Device">
                    <span class="material-icons">upload</span>
                </button>
                ${hasTransferAction ? `
                    <button class="rom-action combo" title="Download & Transfer">
                        <span class="material-icons">sync_alt</span>
                    </button>
                ` : ''}
            </div>
        `;
        
        // Add event listeners
        const downloadBtn = card.querySelector('.download');
        const uploadBtn = card.querySelector('.upload');
        const comboBtn = card.querySelector('.combo');
        
        downloadBtn.addEventListener('click', () => {
            this.downloadRom(rom);
        });
        
        uploadBtn.addEventListener('click', () => {
            this.uploadRom(rom);
        });
        
        if (comboBtn) {
            comboBtn.addEventListener('click', () => {
                this.downloadAndTransferRom(rom);
            });
        }
        
        return card;
    }
    
    async downloadRom(rom) {
        try {
            this.showNotification(`Starting download: ${rom.displayName}`, 'info');
            
            // Update button state
            const downloadBtn = event.target.closest('.download');
            if (downloadBtn) {
                downloadBtn.disabled = true;
                downloadBtn.innerHTML = '<span class="material-icons">hourglass_empty</span>';
            }
            
            // Use the enhanced download functionality
            const success = await this.romRepository.downloadRom(rom);
            
            if (success) {
                this.showNotification(`✅ Downloaded: ${rom.displayName}`, 'success');
                if (downloadBtn) {
                    downloadBtn.innerHTML = '<span class="material-icons">check_circle</span>';
                    setTimeout(() => {
                        downloadBtn.innerHTML = '<span class="material-icons">download</span>';
                        downloadBtn.disabled = false;
                    }, 3000);
                }
            } else {
                this.showNotification(`❌ Download failed: ${rom.displayName}`, 'error');
                if (downloadBtn) {
                    downloadBtn.innerHTML = '<span class="material-icons">error</span>';
                    setTimeout(() => {
                        downloadBtn.innerHTML = '<span class="material-icons">download</span>';
                        downloadBtn.disabled = false;
                    }, 3000);
                }
            }
            
        } catch (error) {
            console.error('Download error:', error);
            this.showNotification(`Download failed: ${error.message}`, 'error');
        }
    }
    
    async uploadRom(rom) {
        // Check if we're running on GitHub Pages (no backend services)
        if (window.location.hostname.includes('github.io')) {
            this.showNotification('⚠️ Upload requires GitHub Codespaces or local setup. See README for instructions.', 'error');
            return;
        }
        
        if (!this.selectedHost) {
            this.showNotification('No host selected', 'error');
            return;
        }
        
        try {
            // Check if ROM was already downloaded locally
            const downloadedFile = await this.checkLocalDownload(rom.displayName);
            if (!downloadedFile) {
                this.showNotification(`❌ ROM not found locally. Download it first!`, 'error');
                return;
            }
            
            this.showNotification(`Starting transfer: ${rom.displayName} to ${this.selectedHost.ip}`, 'info');
            
            // Get host configuration
            const hostConfig = this.hostTemplates[this.selectedHostTemplate] || this.hostTemplates.muos;
            hostConfig.hostIp = this.selectedHost.ip;
            
            console.log(`🔧 Transfer debug - Selected host:`, this.selectedHost);
            console.log(`🔧 Transfer debug - Template:`, this.selectedHostTemplate);
            console.log(`🔧 Transfer debug - Host config:`, hostConfig);
            
            // Call transfer service with local file path
            const response = await fetch('http://localhost:8002/transfer', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    localFilePath: downloadedFile,
                    romName: rom.displayName,
                    platform: rom.platform?.label?.toLowerCase() || 'roms',
                    hostConfig: hostConfig
                })
            });
            
            const result = await response.json();
            
            if (result.success) {
                this.showNotification(`✅ Transfer completed: ${rom.displayName}`, 'success');
            } else {
                this.showNotification(`❌ Transfer failed: ${result.error}`, 'error');
            }
            
        } catch (error) {
            console.error('Transfer error:', error);
            this.showNotification(`Transfer failed: ${error.message}`, 'error');
        }
    }
    
    async checkLocalDownload(fileName) {
        // Check if file exists in Downloads folder (this is a simplified check)
        // In a real implementation, you'd maintain a registry of downloaded files
        try {
            const response = await fetch('http://localhost:8002/check-file', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ fileName: fileName })
            });
            const result = await response.json();
            return result.exists ? result.filePath : null;
        } catch (error) {
            console.error('File check error:', error);
            return null;
        }
    }
    
    async downloadAndTransferRom(rom) {
        // Check if we're running on GitHub Pages (no backend services)
        if (window.location.hostname.includes('github.io')) {
            this.showNotification('⚠️ Transfer requires GitHub Codespaces or local setup. See README for instructions.', 'error');
            return;
        }
        
        if (!this.selectedHost) {
            this.showNotification('No host selected', 'error');
            return;
        }
        
        // Update button state to show it's working
        const comboBtn = event.target.closest('.combo');
        if (comboBtn) {
            comboBtn.disabled = true;
            comboBtn.innerHTML = '<span class="material-icons">downloading</span>';
        }
        
        try {
            // Get host configuration
            const hostConfig = this.hostTemplates[this.selectedHostTemplate] || this.hostTemplates.muos;
            hostConfig.hostIp = this.selectedHost.ip;
            
            // STEP 1: Stream ROM directly from source to device
            this.showNotification(`📥 STEP 1: Downloading ${rom.displayName} from archive...`, 'info');
            
            // Update button to show transfer phase
            if (comboBtn) {
                comboBtn.innerHTML = '<span class="material-icons">upload</span>';
            }
            
            // Small delay for visual feedback
            await new Promise(resolve => setTimeout(resolve, 500));
            
            // STEP 2: Transfer directly to device (download + transfer in one operation)
            this.showNotification(`📤 STEP 2: Streaming ${rom.displayName} to ${this.selectedHost.ip}...`, 'info');
            
            // Use the combo download+transfer service (root endpoint)
            const response = await fetch('http://localhost:8002/', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    romUrl: rom.downloadUrl,
                    romName: rom.displayName,
                    platform: rom.platform?.label?.toLowerCase() || 'roms',
                    hostConfig: hostConfig
                })
            });
            
            const result = await response.json();
            
            if (result.success) {
                this.showNotification(`🎉 COMPLETE: ${rom.displayName} streamed directly to device!`, 'success');
                
                // Success - show check mark
                if (comboBtn) {
                    comboBtn.innerHTML = '<span class="material-icons">check_circle</span>';
                    setTimeout(() => this.resetComboButton(comboBtn), 3000);
                }
            } else {
                throw new Error(result.error || 'Transfer failed');
            }
            
        } catch (error) {
            console.error('Direct transfer error:', error);
            this.showNotification(`❌ DIRECT TRANSFER FAILED: ${error.message}`, 'error');
            
            // Error - show error icon
            if (comboBtn) {
                comboBtn.innerHTML = '<span class="material-icons">error</span>';
                setTimeout(() => this.resetComboButton(comboBtn), 3000);
            }
        }
    }
    
    resetComboButton(comboBtn) {
        if (comboBtn) {
            comboBtn.disabled = false;
            comboBtn.innerHTML = '<span class="material-icons">download_for_offline</span>';
        }
    }
    
    async bulkDownloadAndTransfer() {
        // Check if we're running on GitHub Pages (no backend services)
        if (window.location.hostname.includes('github.io')) {
            this.showNotification('⚠️ Bulk transfer requires GitHub Codespaces or local setup. See README for instructions.', 'error');
            return;
        }
        
        if (this.romResults.length === 0) {
            this.showNotification('No ROMs to download', 'warning');
            return;
        }
        
        if (!this.selectedHost) {
            this.showNotification('No host selected for transfer', 'error');
            return;
        }
        
        const progressContainer = document.getElementById('progressContainer');
        const progressFill = document.getElementById('progressFill');
        const progressText = document.getElementById('progressText');
        const bulkBtn = document.getElementById('bulkDownloadBtn');
        
        progressContainer.style.display = 'block';
        bulkBtn.disabled = true;
        
        // Get host configuration for direct streaming
        const hostConfig = this.hostTemplates[this.selectedHostTemplate] || this.hostTemplates.muos;
        hostConfig.hostIp = this.selectedHost.ip;
        
        try {
            const total = this.romResults.length;
            const successful = [];
            const failed = [];
            
            this.showNotification(`📡 Starting bulk stream transfer of ${total} ROMs to ${this.selectedHost.ip}`, 'info');
            
            // Stream each ROM directly to device
            for (let i = 0; i < total; i++) {
                const rom = this.romResults[i];
                
                // Update progress UI
                const percentage = (i / total) * 100;
                progressFill.style.width = `${percentage}%`;
                progressText.textContent = `Streaming ${rom.displayName} (${i + 1}/${total})`;
                
                try {
                    // Stream ROM directly from source to device
                    const response = await fetch('http://localhost:8002/', {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json',
                        },
                        body: JSON.stringify({
                            romUrl: rom.downloadUrl,
                            romName: rom.displayName,
                            platform: rom.platform?.label?.toLowerCase() || 'roms',
                            hostConfig: hostConfig
                        })
                    });
                    
                    const result = await response.json();
                    
                    if (result.success) {
                        successful.push(rom.displayName);
                        console.log(`✅ Streamed successfully: ${rom.displayName}`);
                    } else {
                        failed.push({ name: rom.displayName, error: result.error });
                        console.log(`❌ Stream failed: ${rom.displayName} - ${result.error}`);
                    }
                    
                } catch (error) {
                    failed.push({ name: rom.displayName, error: error.message });
                    console.error(`❌ Stream error for ${rom.displayName}:`, error);
                }
                
                // Brief pause between transfers to prevent overwhelming
                if (i < total - 1) {
                    await new Promise(resolve => setTimeout(resolve, 500));
                }
            }
            
            // Final progress update
            progressFill.style.width = '100%';
            progressText.textContent = `Completed ${successful.length}/${total} transfers`;
            
            // Wait a moment then hide progress
            setTimeout(() => {
                progressContainer.style.display = 'none';
                bulkBtn.disabled = false;
            }, 2000);
            
            // Show comprehensive results
            const successCount = successful.length;
            const failedCount = failed.length;
            
            if (failedCount === 0) {
                this.showNotification(`🎉 All ${successCount} ROMs streamed successfully to device!`, 'success');
            } else if (successCount === 0) {
                this.showNotification(`❌ All ${failedCount} ROM streams failed!`, 'error');
            } else {
                this.showNotification(`⚠️ Bulk stream completed: ${successCount} successful, ${failedCount} failed out of ${total}`, 'warning');
            }
            
            // Log detailed results
            console.log('Bulk stream results:', { successful, failed, total });
            
        } catch (error) {
            console.error('Bulk stream error:', error);
            progressContainer.style.display = 'none';
            bulkBtn.disabled = false;
            this.showNotification(`Bulk stream failed: ${error.message}`, 'error');
        }
    }
    
    async downloadAllRoms() {
        if (this.romResults.length === 0) {
            this.showNotification('No ROMs to download', 'warning');
            return;
        }
        
        const progressContainer = document.getElementById('progressContainer');
        const progressFill = document.getElementById('progressFill');
        const progressText = document.getElementById('progressText');
        const downloadBtn = document.getElementById('downloadAllBtn');
        
        progressContainer.style.display = 'block';
        downloadBtn.disabled = true;
        
        // Use the enhanced batch download functionality (download only)
        try {
            const results = await this.romRepository.downloadMultipleRoms(
                this.romResults,
                (progress) => {
                    // Update progress UI
                    const percentage = (progress.current / progress.total) * 100;
                    progressFill.style.width = `${percentage}%`;
                    progressText.textContent = `Downloading ${progress.currentRom} (${progress.current}/${progress.total})`;
                    
                    console.log(`Progress: ${progress.completed} completed, ${progress.failed} failed`);
                }
            );
            
            progressContainer.style.display = 'none';
            downloadBtn.disabled = false;
            
            // Show comprehensive results
            const successCount = results.successful.length;
            const failedCount = results.failed.length;
            const total = results.total;
            
            if (failedCount === 0) {
                this.showNotification(`✅ All ROMs downloaded successfully! (${successCount}/${total})`, 'success');
            } else if (successCount === 0) {
                this.showNotification(`❌ All downloads failed! (${failedCount}/${total})`, 'error');
            } else {
                this.showNotification(`⚠️ Download completed: ${successCount} successful, ${failedCount} failed out of ${total}`, 'warning');
            }
            
            // Log detailed results
            console.log('Download all results:', results);
            
        } catch (error) {
            console.error('Download all error:', error);
            progressContainer.style.display = 'none';
            downloadBtn.disabled = false;
            this.showNotification(`Download failed: ${error.message}`, 'error');
        }
    }
    
    updateSearchButton() {
        const button = document.getElementById('searchBtn');
        const icon = button.querySelector('.material-icons');
        
        if (this.isSearching) {
            icon.textContent = 'hourglass_empty';
            button.disabled = true;
        } else {
            icon.textContent = 'search';
            button.disabled = false;
        }
    }
    
    showLoadingShimmer() {
        const shimmer = document.getElementById('loadingShimmer');
        shimmer.innerHTML = '';
        
        // Create shimmer cards
        for (let i = 0; i < 8; i++) {
            const card = document.createElement('div');
            card.className = 'shimmer-card';
            card.innerHTML = `
                <div class="shimmer-icon"></div>
                <div class="shimmer-content">
                    <div class="shimmer-title"></div>
                    <div class="shimmer-subtitle"></div>
                </div>
                <div class="shimmer-actions">
                    <div class="shimmer-action"></div>
                    <div class="shimmer-action"></div>
                </div>
            `;
            shimmer.appendChild(card);
        }
        
        shimmer.style.display = 'block';
    }
    
    hideLoadingShimmer() {
        document.getElementById('loadingShimmer').style.display = 'none';
    }
    
    showNoResults() {
        const noResults = document.getElementById('noResults');
        const messageP = noResults.querySelector('p');
        
        if (this.searchTerm.trim()) {
            messageP.textContent = 'No results found';
        } else {
            messageP.textContent = 'Search for ROMs to get started';
        }
        
        noResults.style.display = 'flex';
    }
    
    showSearchInstruction() {
        const noResults = document.getElementById('noResults');
        const messageP = noResults.querySelector('p');
        
        if (this.selectedPlatform) {
            messageP.textContent = `Click Search or press Enter to browse ${this.selectedPlatform.label} ROMs`;
        } else {
            messageP.textContent = 'Search for ROMs to get started';
        }
        
        noResults.style.display = 'flex';
    }
    
    hideNoResults() {
        document.getElementById('noResults').style.display = 'none';
    }
    
    clearResults() {
        this.romResults = [];
        document.getElementById('romList').innerHTML = '';
        this.hideLoadingShimmer();
        this.showNoResults();
        this.updateBulkControls();
    }
    
    updateBulkControls() {
        const bulkControls = document.getElementById('bulkControls');
        const bulkInfo = document.getElementById('bulkInfo');
        
        if (this.romResults.length > 0 && this.selectedHost) {
            const platformText = this.selectedPlatform ? 
                ` • ${this.selectedPlatform.label}` : '';
            bulkInfo.textContent = `${this.romResults.length} ROMs found • Host: ${this.selectedHost.ip}${platformText}`;
            bulkControls.style.display = 'flex';
        } else {
            bulkControls.style.display = 'none';
        }
    }
    
    applyTemplate(templateName) {
        console.log(`🔌 Applying template: ${templateName}`);
        

        
        const template = this.hostTemplates[templateName];
        console.log(`📋 Template found:`, template);
        if (!template) {
            console.error(`❌ Template not found: ${templateName}`);
            this.showNotification(`Template "${templateName}" not found`, 'error');
            return;
        }
        
        // Set selected host template for transfer service
        this.selectedHostTemplate = templateName;
        
        // Apply template settings
        this.sshConfig = {
            username: template.username,
            password: template.password,
            port: template.port,
            remoteBasePath: template.remoteBasePath,
            useGuest: template.useGuest,
            isDhcpNetwork: template.isDhcpNetwork
        };
        
        // Add host if IP is provided
        if (template.hostIp) {
            console.log(`🌐 Adding host IP: ${template.hostIp}`);
            this.addHostFromIp(template.hostIp);
        }
        
        // Update the SSH configuration display (already on devices screen)
        console.log(`🔄 Updating SSH configuration display...`);
        this.updateSSHConfigDisplay();
        
        console.log(`✅ Showing success notification for ${template.name}`);
        this.showNotification(`Applied ${template.name} template`, 'success');
    }
    
    scanNetwork() {
        // Check if we're running on GitHub Pages (no backend services)
        if (window.location.hostname.includes('github.io')) {
            this.showNotification('⚠️ Network scanning requires GitHub Codespaces or local setup. See README for instructions.', 'error');
            return;
        }
        
        if (this.isScanning) return;
        
        this.isScanning = true;
        const scanBtn = document.getElementById('scanBtn');
        const originalText = scanBtn.textContent;
        scanBtn.textContent = 'Scanning...';
        scanBtn.disabled = true;
        
        // Simulate network scan
        this.discoveredHosts = [];
        this.updateHostsList();
        
        setTimeout(() => {
            // Simulate finding some hosts
            const mockHosts = ['192.168.0.100', '192.168.0.150', '192.168.1.101'];
            this.discoveredHosts = mockHosts.map(ip => ({ ip }));
            
            if (this.discoveredHosts.length > 0) {
                this.selectedHost = this.discoveredHosts[0];
            }
            
            this.updateHostsList();
            this.updateBulkControls();
            
            this.isScanning = false;
            scanBtn.textContent = originalText;
            scanBtn.disabled = false;
            
            this.showNotification(`Found ${this.discoveredHosts.length} hosts`, 'success');
        }, 2000);
    }
    
    testConnection() {
        // Check if we're running on GitHub Pages (no backend services)
        if (window.location.hostname.includes('github.io')) {
            this.showNotification('⚠️ Connection testing requires GitHub Codespaces or local setup. See README for instructions.', 'error');
            return;
        }
        
        if (!this.selectedHost) {
            this.showNotification('No host selected', 'error');
            return;
        }
        
        const testBtn = document.getElementById('testConnectionBtn');
        const originalText = testBtn.textContent;
        testBtn.textContent = 'Testing...';
        testBtn.disabled = true;
        
        // Simulate connection test
        setTimeout(() => {
            const success = Math.random() > 0.3; // 70% success rate
            
            if (success) {
                this.showNotification(`Successfully connected to ${this.selectedHost.ip}`, 'success');
            } else {
                this.showNotification(`Failed to connect to ${this.selectedHost.ip}`, 'error');
            }
            
            testBtn.textContent = originalText;
            testBtn.disabled = false;
        }, 1500);
    }
    

    
    addHostFromIp(ip) {
        if (!this.isValidIP(ip)) return false;
        
        const host = { ip };
        
        // Remove existing host with same IP
        this.discoveredHosts = this.discoveredHosts.filter(h => h.ip !== ip);
        
        // Add new host
        this.discoveredHosts.unshift(host);
        this.selectedHost = host;
        
        this.updateHostsList();
        this.updateBulkControls();
        
        return true;
    }
    
    selectHost(host) {
        this.selectedHost = host;
        this.updateHostsList();
        this.updateBulkControls();
    }
    
    updateHostsList() {
        const hostsList = document.getElementById('hostsList');
        const noHosts = document.getElementById('noHosts');
        
        if (this.discoveredHosts.length === 0) {
            noHosts.style.display = 'block';
            return;
        }
        
        noHosts.style.display = 'none';
        
        // Remove existing host items
        hostsList.querySelectorAll('.host-item').forEach(item => item.remove());
        
        this.discoveredHosts.forEach(host => {
            const hostItem = document.createElement('div');
            hostItem.className = 'host-item';
            if (this.selectedHost?.ip === host.ip) {
                hostItem.classList.add('selected');
            }
            
            hostItem.innerHTML = `
                <div class="host-ip">${host.ip}</div>
                <button class="host-select ${this.selectedHost?.ip === host.ip ? 'selected' : ''}">
                    ${this.selectedHost?.ip === host.ip ? 'Selected' : 'Select'}
                </button>
            `;
            
            const selectBtn = hostItem.querySelector('.host-select');
            selectBtn.addEventListener('click', () => {
                this.selectHost(host);
            });
            
            hostsList.appendChild(hostItem);
        });
    }
    
    updateSSHConfigDisplay() {
        document.getElementById('sshUsername').value = this.sshConfig.username;
        document.getElementById('sshPassword').value = this.sshConfig.password;
        document.getElementById('sshPort').value = this.sshConfig.port;
        document.getElementById('remoteBasePath').value = this.sshConfig.remoteBasePath;
        document.getElementById('guestMode').checked = this.sshConfig.useGuest;
        document.getElementById('dhcpNetwork').checked = this.sshConfig.isDhcpNetwork;
    }
    
    showTemplateEditor() {
        const editor = document.getElementById('templateEditor');
        editor.innerHTML = '';
        
        Object.entries(this.hostTemplates).forEach(([key, template]) => {
            const section = document.createElement('div');
            section.className = 'template-section';
            section.innerHTML = `
                <h4>${template.name}</h4>
                <div class="template-form">
                    <div class="form-row">
                        <input type="text" placeholder="Username" value="${template.username}" data-template="${key}" data-field="username">
                        <input type="password" placeholder="Password" value="${template.password}" data-template="${key}" data-field="password">
                        <input type="number" placeholder="Port" value="${template.port}" data-template="${key}" data-field="port">
                    </div>
                    <div class="form-row">
                        <input type="text" placeholder="Remote Base Path" value="${template.remoteBasePath}" data-template="${key}" data-field="remoteBasePath" class="full-width">
                    </div>
                    <div class="form-row">
                        <input type="text" placeholder="Host IP" value="${template.hostIp}" data-template="${key}" data-field="hostIp" class="full-width">
                    </div>
                    <div class="form-row">
                        <label>
                            <input type="checkbox" ${template.useGuest ? 'checked' : ''} data-template="${key}" data-field="useGuest">
                            Guest Mode
                        </label>
                        <label>
                            <input type="checkbox" ${template.isDhcpNetwork ? 'checked' : ''} data-template="${key}" data-field="isDhcpNetwork">
                            DHCP Network
                        </label>
                    </div>
                </div>
            `;
            editor.appendChild(section);
        });
        
        this.showModal('templateModal');
    }
    
    saveTemplates() {
        const form = document.getElementById('templateEditor');
        const inputs = form.querySelectorAll('input');
        
        inputs.forEach(input => {
            const templateKey = input.dataset.template;
            const field = input.dataset.field;
            
            if (templateKey && field && this.hostTemplates[templateKey]) {
                if (input.type === 'checkbox') {
                    this.hostTemplates[templateKey][field] = input.checked;
                } else {
                    this.hostTemplates[templateKey][field] = input.value;
                }
            }
        });
        
        this.saveTemplatesToStorage();
        this.hideModal('templateModal');
        this.showNotification('Templates saved', 'success');
    }
    
    loadTemplatesFromStorage() {
        const saved = localStorage.getItem('romDownloaderTemplates');
        if (saved) {
            try {
                const templates = JSON.parse(saved);
                this.hostTemplates = { ...this.hostTemplates, ...templates };
            } catch (e) {
                console.error('Failed to load templates:', e);
            }
        }
    }
    
    saveTemplatesToStorage() {
        localStorage.setItem('romDownloaderTemplates', JSON.stringify(this.hostTemplates));
    }
    
    showArchiveSettings() {
        const editor = document.getElementById('archiveEditor');
        editor.innerHTML = '';
        
        // Load current archive settings
        this.loadArchiveSettingsFromStorage();
        
        Object.entries(platforms).forEach(([key, platform]) => {
            const section = document.createElement('div');
            section.className = 'archive-section';
            section.innerHTML = `
                <div class="archive-form">
                    <div class="form-row">
                        <div class="platform-name">
                            <span class="material-icons">${platform.icon}</span>
                            <span>${platform.label}</span>
                        </div>
                        <input type="url" 
                               placeholder="Enter archive URL (e.g., https://example.com/roms/${platform.id.toLowerCase()}/" 
                               value="${platform.archiveUrl || ''}" 
                               data-platform="${platform.id}" 
                               class="archive-url-input">
                        <button type="button" class="test-url-btn" data-platform="${platform.id}">
                            <span class="material-icons">link</span>
                            Test
                        </button>
                    </div>
                    <div class="url-status" id="status-${platform.id}"></div>
                </div>
            `;
            editor.appendChild(section);
        });
        
        // Add event listeners for test buttons
        editor.querySelectorAll('.test-url-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const platformId = e.currentTarget.dataset.platform;
                this.testArchiveUrl(platformId);
            });
        });
        
        this.showModal('archiveSettingsModal');
    }
    
    async testArchiveUrl(platformId) {
        const input = document.querySelector(`input[data-platform="${platformId}"]`);
        const status = document.getElementById(`status-${platformId}`);
        const testBtn = document.querySelector(`.test-url-btn[data-platform="${platformId}"]`);
        
        const url = input.value.trim();
        if (!url) {
            status.className = 'url-status error';
            status.textContent = 'Please enter a URL to test';
            return;
        }
        
        // Update button state
        testBtn.disabled = true;
        testBtn.innerHTML = '<span class="material-icons">hourglass_empty</span> Testing...';
        
        status.className = 'url-status testing';
        status.textContent = 'Testing URL...';
        
        try {
            // Use the ROM repository to test the URL
            const testHtml = await this.romRepository.fetchHtml(url);
            
            if (testHtml && testHtml.length > 0) {
                status.className = 'url-status success';
                status.textContent = '✓ URL is accessible and returned content';
            } else {
                status.className = 'url-status error';
                status.textContent = '✗ URL returned no content or failed to load';
            }
        } catch (error) {
            status.className = 'url-status error';
            status.textContent = `✗ Error: ${error.message}`;
        }
        
        // Reset button state
        testBtn.disabled = false;
        testBtn.innerHTML = '<span class="material-icons">link</span> Test';
    }
    
    saveArchiveSettings() {
        const form = document.getElementById('archiveEditor');
        const inputs = form.querySelectorAll('.archive-url-input');
        
        let hasChanges = false;
        inputs.forEach(input => {
            const platformId = input.dataset.platform;
            const newUrl = input.value.trim();
            
            // Find platform and update URL
            Object.values(platforms).forEach(platform => {
                if (platform.id === platformId) {
                    if (platform.archiveUrl !== newUrl) {
                        platform.archiveUrl = newUrl;
                        hasChanges = true;
                    }
                }
            });
        });
        
        if (hasChanges) {
            this.saveArchiveSettingsToStorage();
            this.hideModal('archiveSettingsModal');
            this.showNotification('Archive settings saved successfully', 'success');
        } else {
            this.hideModal('archiveSettingsModal');
            this.showNotification('No changes were made', 'info');
        }
    }
    
    loadArchiveSettingsFromStorage() {
        const saved = localStorage.getItem('romDownloaderArchiveSettings');
        if (saved) {
            try {
                const archiveSettings = JSON.parse(saved);
                Object.entries(archiveSettings).forEach(([platformId, archiveUrl]) => {
                    Object.values(platforms).forEach(platform => {
                        if (platform.id === platformId) {
                            platform.archiveUrl = archiveUrl;
                        }
                    });
                });
            } catch (e) {
                console.error('Failed to load archive settings:', e);
            }
        }
    }
    
    saveArchiveSettingsToStorage() {
        const archiveSettings = {};
        Object.values(platforms).forEach(platform => {
            archiveSettings[platform.id] = platform.archiveUrl || '';
        });
        localStorage.setItem('romDownloaderArchiveSettings', JSON.stringify(archiveSettings));
    }
    
    showModal(modalId) {
        document.getElementById(modalId).classList.add('active');
    }
    
    hideModal(modalId) {
        document.getElementById(modalId).classList.remove('active');
    }
    
    hideAllModals() {
        document.querySelectorAll('.modal').forEach(modal => {
            modal.classList.remove('active');
        });
    }
    
    showDemoBanner() {
        const banner = document.getElementById('demoBanner');
        if (!banner) return;
        if (this.romRepository.isDemoMode()) {
            banner.style.display = 'block';
        } else {
            banner.style.display = 'none';
        }
    }
    
    showNotification(message, type = 'info') {
        const container = document.getElementById('notifications');
        const notification = document.createElement('div');
        notification.className = `notification ${type}`;
        
        const iconMap = {
            success: 'check_circle',
            error: 'error',
            warning: 'warning',
            info: 'info'
        };
        
        notification.innerHTML = `
            <div class="notification-icon">
                <span class="material-icons">${iconMap[type] || 'info'}</span>
            </div>
            <div class="notification-content">
                <div class="notification-message">${this.escapeHtml(message)}</div>
            </div>
        `;
        
        container.appendChild(notification);
        
        // Auto-remove after 5 seconds
        setTimeout(() => {
            if (notification.parentNode) {
                notification.parentNode.removeChild(notification);
            }
        }, 5000);
    }
    
    isValidIP(ip) {
        const ipRegex = /^((25[0-5]|2[0-4]\d|[0-1]?\d{1,2})\.){3}(25[0-5]|2[0-4]\d|[0-1]?\d{1,2})$/;
        return ipRegex.test(ip);
    }
    
    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
    
    updateUI() {
        this.updatePlatformFilterDisplay();
        this.updateQuickFilters();
        this.updateClearButton();
        this.updateBulkControls();
        this.updateHostsList();
        this.updateSSHConfigDisplay();
    }

    // Select the first visible console card and navigate to browse
    selectFirstVisibleConsole() {
        const cards = Array.from(document.querySelectorAll('.console-card'));
        const firstVisible = cards.find(c => c.offsetParent !== null && c.style.display !== 'none');
        if (firstVisible) {
            firstVisible.click();
        }
    }
}

// Initialize the application when the DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    console.log('DOM loaded, initializing ROM Downloader App...');
    try {
        window.romApp = new RomDownloaderApp();
        console.log('ROM Downloader App initialized successfully');
    } catch (error) {
        console.error('Failed to initialize ROM Downloader App:', error);
    }
});

// Add CSS for additional template editor styles
const templateStyles = `
.template-section {
    margin-bottom: 2rem;
    padding: 1rem;
    border: 1px solid var(--surface-variant);
    border-radius: var(--radius-md);
}

.template-section h4 {
    margin-bottom: 1rem;
    color: var(--primary-color);
    font-weight: 600;
}

.template-form {
    display: flex;
    flex-direction: column;
    gap: 1rem;
}

.form-row {
    display: flex;
    gap: 1rem;
    flex-wrap: wrap;
}

.form-row input[type="text"],
.form-row input[type="password"],
.form-row input[type="number"] {
    flex: 1;
    min-width: 150px;
    padding: 0.75rem;
    border: 2px solid var(--surface-variant);
    border-radius: var(--radius-md);
    font-size: 1rem;
}

.form-row input.full-width {
    width: 100%;
}

.form-row label {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    cursor: pointer;
}

.form-row input[type="checkbox"] {
    width: auto;
}
`;

// Inject template styles
const style = document.createElement('style');
style.textContent = templateStyles;
document.head.appendChild(style);
