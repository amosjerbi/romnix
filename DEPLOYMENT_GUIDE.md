# 🚀 GitHub Deployment Guide

Complete step-by-step guide to deploy ROM Downloader Web to GitHub with both GitHub Pages and Codespaces support.

## 📋 Prerequisites

- GitHub account
- Git installed locally (or use GitHub Desktop)
- This project ready on your local machine

## 🎯 Step-by-Step Deployment

### Step 1: Create GitHub Repository

1. **Go to GitHub.com** and sign in
2. **Click "New Repository"** (green button or "+" menu)
3. **Repository Settings**:
   - **Repository name**: `rom-downloader-web` (or your preferred name)
   - **Description**: "Web-based ROM downloader with device transfer for retro gaming handhelds"
   - **Visibility**: Choose Public (for GitHub Pages) or Private
   - **Don't initialize** with README, .gitignore, or license (we have them already)
4. **Click "Create repository"**

### Step 2: Connect Local Repository to GitHub

In your terminal, from the project directory:

```bash
# Set your Git identity if not already done
git config --global user.name "Your Name"
git config --global user.email "your.email@example.com"

# Add GitHub as remote origin (replace 'yourusername' with your GitHub username)
git remote add origin https://github.com/yourusername/rom-downloader-web.git

# Rename branch to main (if it's currently master)
git branch -M main

# Push to GitHub
git push -u origin main
```

### Step 3: Enable GitHub Pages

1. **Go to your repository on GitHub**
2. **Click "Settings"** tab
3. **Scroll down to "Pages"** section in the left sidebar
4. **Configure Source**:
   - Source: "Deploy from a branch"
   - Branch: "main"
   - Folder: "/ (root)"
5. **Click "Save"**

**Your GitHub Pages site will be available at:**
`https://yourusername.github.io/rom-downloader-web`

*Note: It may take a few minutes to deploy initially.*

### Step 4: Verify GitHub Actions

1. **Go to "Actions" tab** in your repository
2. **Check deployment workflow**: You should see a workflow run for "Deploy ROM Downloader to GitHub Pages"
3. **Wait for completion**: Green checkmark means successful deployment

### Step 5: Test GitHub Pages

1. **Visit your GitHub Pages URL**
2. **Verify the interface loads** (will be in demo mode)
3. **Test navigation** between Devices and Consoles tabs
4. **Confirm responsive design** on mobile/desktop

## 🖥️ Setting Up GitHub Codespaces

### Enable Codespaces (if needed)

1. **Go to your repository**
2. **Check if Codespaces is available**: Look for the "Code" button
3. **If not visible**: 
   - Go to your GitHub user Settings → Codespaces
   - Enable Codespaces for your account

### Test Codespaces

1. **Click the green "Code" button** in your repository
2. **Select "Codespaces" tab**
3. **Click "Create codespace on main"**
4. **Wait for setup** (2-3 minutes)
5. **Verify services start**: You should see all three services initialize
6. **Test the web interface**: Should auto-open with full functionality

## 📝 Update Repository Links

Now update your repository with the correct links:

### Step 6: Update README.md

Replace placeholder links in `README.md`:

```markdown
# Change this line:
🌐 **[Live Demo](https://yourusername.github.io/rom-downloader-web)**

# To your actual URL:
🌐 **[Live Demo](https://YOUR-ACTUAL-USERNAME.github.io/rom-downloader-web)**
```

And update the Codespaces badge:
```markdown
# Change this line:
[![Open in GitHub Codespaces](https://github.com/codespaces/badge.svg)](https://github.com/yourusername/rom-downloader-web/codespaces/new)

# To your actual repository:
[![Open in GitHub Codespaces](https://github.com/codespaces/badge.svg)](https://github.com/YOUR-ACTUAL-USERNAME/rom-downloader-web/codespaces/new)
```

### Step 7: Commit and Push Updates

```bash
# Edit README.md with your actual links
# Then commit the changes:

git add README.md
git commit -m "Update repository links to actual GitHub URLs"
git push origin main
```

## 🎮 Repository Features

Your deployed repository now includes:

### ✅ **GitHub Pages (Demo Mode)**
- **URL**: `https://yourusername.github.io/rom-downloader-web`
- **Features**: Full UI, demo ROM data, no actual downloads
- **Updates**: Automatically deployed on every push to main branch

### ✅ **GitHub Codespaces (Full Functionality)**
- **Access**: Click "Code" → "Codespaces" → "Create codespace"
- **Features**: Real ROM downloads, device transfers, SSH support
- **Environment**: Pre-configured with all dependencies

### ✅ **Automated Deployment**
- **GitHub Actions**: Auto-deploys to Pages on every commit
- **Service Orchestration**: Codespaces starts all services automatically
- **Documentation**: Comprehensive guides for users

## 🔧 Customization Options

### Adding Custom ROM Sources

Edit `platforms.js` to add archive URLs:

```javascript
const platforms = {
    NES: {
        id: "nes",
        label: "NES",
        archiveUrl: "", // Users must configure their own ROM sources
        extensions: ["7z", "zip", "nes"],
        icon: "sports_esports"
    }
    // ...
}
```

### Modifying Handheld Templates

Edit the templates in `app.js`:

```javascript
this.hostTemplates = {
    rocknix: {
        name: 'Rocknix',
        username: 'root',
        password: 'rocknix',
        port: '22',
        remoteBasePath: '/storage/roms',
        // ...
    }
    // Add your custom templates here
}
```

### Styling Customization

Modify `styles.css` to change the appearance:

```css
:root {
    --primary-color: #6B46C1;     /* Change primary color */
    --background-color: #FAFAFA;   /* Change background */
    /* ... other CSS variables ... */
}
```

## 📊 Repository Management

### Monitoring Usage

- **GitHub Pages**: Check Settings → Pages for deployment status
- **Codespaces**: Monitor usage in your GitHub account settings
- **Actions**: View deployment logs in Actions tab

### Security Considerations

- **SSH Credentials**: Never commit real SSH credentials
- **API Keys**: Use GitHub Secrets for any API keys
- **CORS**: Proxy server handles cross-origin requests safely

## 🎉 You're Done!

Your ROM Downloader Web is now fully deployed with:

- 🌐 **Live demo** at your GitHub Pages URL
- 🖥️ **Full functionality** via GitHub Codespaces
- 🔄 **Automatic updates** when you push changes
- 📖 **Complete documentation** for users

**Share your repository** with the retro gaming community and enjoy managing ROMs for your handheld devices!

---

## 🆘 Troubleshooting

### GitHub Pages Not Working
- Check if Pages is enabled in repository Settings
- Verify the workflow completed successfully in Actions tab  
- Wait a few minutes for DNS propagation

### Codespaces Not Starting
- Ensure you have Codespaces enabled in your account
- Try creating a new Codespace
- Check the setup logs in the terminal

### Links Not Working
- Double-check you replaced "yourusername" with your actual GitHub username
- Ensure repository is public if using GitHub Pages with free account

**Need help?** Create an issue in your repository or check the troubleshooting guides!
