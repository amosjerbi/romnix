# GitHub Deployment Commands

## After creating your GitHub repository, run these commands:

### 1. Set Git Identity (if not already done)
```bash
git config --global user.name "Your Name"
git config --global user.email "your.email@example.com"
```

### 2. Connect to GitHub Repository
**Replace `yourusername` with your actual GitHub username:**

```bash
# Add your GitHub repository as remote origin
git remote add origin https://github.com/yourusername/rom-downloader-web.git

# Rename branch from master to main (GitHub standard)
git branch -M main

# Push your code to GitHub
git push -u origin main
```

### 3. Enable GitHub Pages
After pushing, go to your repository on GitHub:
1. Click **Settings** tab
2. Scroll to **Pages** section
3. Source: "Deploy from a branch"
4. Branch: "main"
5. Folder: "/ (root)"
6. Click **Save**

### 4. Test GitHub Codespaces
1. In your repository, click **"Code"** button
2. Click **"Codespaces"** tab  
3. Click **"Create codespace on main"**
4. Wait 2-3 minutes for setup
5. All services will start automatically!

## Your URLs will be:
- **GitHub Pages**: https://yourusername.github.io/rom-downloader-web
- **GitHub Repo**: https://github.com/yourusername/rom-downloader-web
- **Codespaces**: Accessed through your repository

## Troubleshooting
If you get authentication errors, you may need to:
1. Use GitHub CLI: `gh auth login`
2. Or use Personal Access Token instead of password
3. Or use SSH keys for authentication

## Need Help?
Check the DEPLOYMENT_GUIDE.md for detailed instructions!
