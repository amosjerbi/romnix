package com.romnix.app.network

import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.sftp.RemoteResourceInfo
import net.schmizz.sshj.sftp.SFTPClient
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security

data class RemoteFile(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long,
    val permissions: String,
    val modifiedTime: Long
)

class SshFileManager {
    
    fun listFiles(host: HostConfig, remotePath: String): Result<List<RemoteFile>> {
        return runCatching {
            initBouncyCastle()
            SSHClient().use { ssh ->
                connectAndAuth(ssh, host)
                ssh.newSFTPClient().use { sftp ->
                    val files = mutableListOf<RemoteFile>()
                    val listing = try {
                        sftp.ls(remotePath)
                    } catch (e: Exception) {
                        android.util.Log.e("SshFileManager", "Failed to list $remotePath: ${e.message}")
                        throw IllegalStateException("Cannot access directory: ${e.message}")
                    }
                    
                    for (item in listing) {
                        // Skip . and .. entries
                        if (item.name == "." || item.name == "..") continue
                        
                        files.add(
                            RemoteFile(
                                name = item.name,
                                path = item.path,
                                isDirectory = item.isDirectory,
                                size = item.attributes.size,
                                permissions = formatPermissions(item),
                                modifiedTime = item.attributes.mtime
                            )
                        )
                    }
                    
                    // Sort: directories first, then by name
                    files.sortWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
                    return@runCatching files
                }
            }
        }
    }
    
    fun renameFile(host: HostConfig, oldPath: String, newPath: String): Result<String> {
        return runCatching {
            initBouncyCastle()
            SSHClient().use { ssh ->
                connectAndAuth(ssh, host)
                ssh.newSFTPClient().use { sftp ->
                    sftp.rename(oldPath, newPath)
                    return@runCatching "Successfully renamed to $newPath"
                }
            }
        }
    }
    
    fun deleteFile(host: HostConfig, remotePath: String, isDirectory: Boolean): Result<String> {
        return runCatching {
            initBouncyCastle()
            SSHClient().use { ssh ->
                connectAndAuth(ssh, host)
                ssh.newSFTPClient().use { sftp ->
                    if (isDirectory) {
                        deleteDirectoryRecursive(sftp, remotePath)
                    } else {
                        sftp.rm(remotePath)
                    }
                    return@runCatching "Successfully deleted"
                }
            }
        }
    }
    
    fun copyFile(host: HostConfig, sourcePath: String, destPath: String): Result<String> {
        return runCatching {
            initBouncyCastle()
            SSHClient().use { ssh ->
                connectAndAuth(ssh, host)
                ssh.startSession().use { session ->
                    val command = "cp -r \"$sourcePath\" \"$destPath\""
                    val cmd = session.exec(command)
                    cmd.inputStream.bufferedReader().readText() // Consume output
                    val error = cmd.errorStream.bufferedReader().readText()
                    cmd.join(10, java.util.concurrent.TimeUnit.SECONDS)
                    
                    if (cmd.exitStatus != 0) {
                        throw IllegalStateException("Copy failed: $error")
                    }
                    
                    return@runCatching "Successfully copied to $destPath"
                }
            }
        }
    }
    
    fun createDirectory(host: HostConfig, remotePath: String): Result<String> {
        return runCatching {
            initBouncyCastle()
            SSHClient().use { ssh ->
                connectAndAuth(ssh, host)
                ssh.newSFTPClient().use { sftp ->
                    sftp.mkdir(remotePath)
                    return@runCatching "Directory created successfully"
                }
            }
        }
    }
    
    fun getFileInfo(host: HostConfig, remotePath: String): Result<RemoteFile> {
        return runCatching {
            initBouncyCastle()
            SSHClient().use { ssh ->
                connectAndAuth(ssh, host)
                ssh.newSFTPClient().use { sftp ->
                    val attrs = sftp.stat(remotePath)
                    val isDir = attrs.type == net.schmizz.sshj.sftp.FileMode.Type.DIRECTORY
                    
                    return@runCatching RemoteFile(
                        name = remotePath.substringAfterLast('/'),
                        path = remotePath,
                        isDirectory = isDir,
                        size = attrs.size,
                        permissions = attrs.permissions.toString(),
                        modifiedTime = attrs.mtime
                    )
                }
            }
        }
    }
    
    fun downloadFile(host: HostConfig, remotePath: String, localFile: java.io.File, onProgress: ((Long, Long) -> Unit)? = null): Result<String> {
        return runCatching {
            initBouncyCastle()
            SSHClient().use { ssh ->
                connectAndAuth(ssh, host)
                ssh.newSFTPClient().use { sftp ->
                    // Ensure parent directory exists
                    localFile.parentFile?.mkdirs()
                    
                    // Delete existing file if it exists
                    if (localFile.exists()) {
                        localFile.delete()
                    }
                    
                    android.util.Log.d("SshFileManager", "Downloading $remotePath to ${localFile.absolutePath}")
                    
                    // Get file size for progress tracking
                    val remoteFileSize = try {
                        sftp.stat(remotePath).size
                    } catch (e: Exception) {
                        0L
                    }
                    
                    // Simple download using SFTP get method
                    sftp.get(remotePath, localFile.absolutePath)
                    
                    // Report completion if progress callback is provided
                    onProgress?.invoke(remoteFileSize, remoteFileSize)
                    
                    android.util.Log.d("SshFileManager", "Download completed: ${localFile.length()} bytes")
                    return@runCatching "File downloaded to ${localFile.absolutePath}"
                }
            }
        }
    }

    fun uploadFileToPath(host: HostConfig, localFile: java.io.File, remotePath: String): Result<String> {
        return runCatching {
            initBouncyCastle()
            SSHClient().use { ssh ->
                connectAndAuth(ssh, host)
                ssh.newSFTPClient().use { sftp ->
                    // Ensure local file exists
                    if (!localFile.exists()) {
                        throw IllegalStateException("Local file does not exist: ${localFile.absolutePath}")
                    }
                    
                    // Ensure remote parent directory exists
                    val parentPath = remotePath.substringBeforeLast('/')
                    if (parentPath.isNotEmpty() && parentPath != remotePath) {
                        try {
                            ensureDirectoryExists(sftp, parentPath)
                        } catch (e: Exception) {
                            android.util.Log.w("SshFileManager", "Could not ensure parent directory exists: $parentPath", e)
                        }
                    }
                    
                    android.util.Log.d("SshFileManager", "Uploading ${localFile.name} (${localFile.length()} bytes) to $remotePath")
                    
                    // SFTP put() uses binary mode by default
                    sftp.put(localFile.absolutePath, remotePath)
                    
                    android.util.Log.d("SshFileManager", "Upload completed successfully")
                    return@runCatching "File uploaded to $remotePath"
                }
            }
        }
    }

    private fun ensureDirectoryExists(sftp: net.schmizz.sshj.sftp.SFTPClient, path: String) {
        val parts = path.split("/").filter { it.isNotEmpty() }
        var currentPath = if (path.startsWith("/")) "/" else ""
        
        for (part in parts) {
            currentPath += if (currentPath.endsWith("/")) part else "/$part"
            try {
                sftp.stat(currentPath)
            } catch (e: Exception) {
                // Directory doesn't exist, create it
                sftp.mkdir(currentPath)
            }
        }
    }
    
    private fun deleteDirectoryRecursive(sftp: SFTPClient, path: String) {
        val items = sftp.ls(path)
        for (item in items) {
            if (item.name == "." || item.name == "..") continue
            
            if (item.isDirectory) {
                deleteDirectoryRecursive(sftp, item.path)
            } else {
                sftp.rm(item.path)
            }
        }
        sftp.rmdir(path)
    }
    
    private fun initBouncyCastle() {
        try {
            Security.removeProvider("BC")
            Security.insertProviderAt(BouncyCastleProvider(), 1)
        } catch (_: Throwable) {}
    }
    
    private fun connectAndAuth(ssh: SSHClient, host: HostConfig) {
        ssh.addHostKeyVerifier(PromiscuousVerifier())
        try {
            ssh.connectTimeout = 15000
            ssh.timeout = 20000
        } catch (_: Throwable) {}
        
        ssh.connect(host.host, host.port)
        
        val passwordsToTry = buildList {
            if (host.password.isNotEmpty()) add(host.password)
            add("root"); add("rocknix"); add("muos"); add("")
        }.distinct()
        
        var authed = false
        for (pwd in passwordsToTry) {
            try {
                ssh.authPassword(host.username, pwd)
                authed = true
                break
            } catch (_: Exception) {
                // try next password
            }
        }
        
        if (!authed) {
            throw IllegalStateException("SSH auth failed for ${host.username}@${host.host}")
        }
    }
    
    private fun formatPermissions(item: RemoteResourceInfo): String {
        val perms = item.attributes.permissions
        return perms.toString()
    }
}

