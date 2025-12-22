package com.example.romdownloader.network

import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.sftp.SFTPClient
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import java.io.File
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security

data class HostConfig(
    val host: String,
    val port: Int = 22,
    val username: String = "root",
    val password: String = "root"
)

class SshUploader {
    fun upload(
        localFile: File,
        platformDirName: String,
        host: HostConfig,
        baseOverride: String? = null
    ): Result<String> {
        return runCatching {
            try {
                Security.removeProvider("BC")
                Security.insertProviderAt(BouncyCastleProvider(), 1)
            } catch (_: Throwable) {}
            SSHClient().use { ssh ->
                ssh.addHostKeyVerifier(PromiscuousVerifier())
                try {
                    // Increase timeouts
                    ssh.connectTimeout = 15000
                    ssh.timeout = 20000
                } catch (_: Throwable) { }
                ssh.connect(host.host, host.port)
                try {
                    // Try a few common passwords if the provided one fails
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
                            // try next
                        }
                    }
                    if (!authed) throw IllegalStateException("SSH auth failed for ${host.username}@${host.host}")

                    ssh.newSFTPClient().use { sftp ->
                        val base = baseOverride?.takeIf { it.isNotBlank() } ?: resolveRomsBaseDir(sftp)
                        val remoteDir = "$base/$platformDirName"
                        ensureDirRecursive(sftp, remoteDir)
                        val remotePath = "$remoteDir/${localFile.name}"
                        sftp.put(localFile.absolutePath, remotePath)
                        return@runCatching remotePath
                    }
                } finally {
                    ssh.disconnect()
                }
            }
        }
    }

    private fun resolveRomsBaseDir(sftp: SFTPClient): String {
        return try {
            sftp.stat("/storage/roms"); "/storage/roms"
        } catch (_: Exception) {
            try {
                sftp.stat("/roms"); "/roms"
            } catch (_: Exception) {
                // Fallback to /roms
                "/roms"
            }
        }
    }

    private fun ensureDirRecursive(sftp: SFTPClient, path: String) {
        val parts = path.trim('/').split('/')
        var current = ""
        for (p in parts) {
            current = if (current.isEmpty()) "/$p" else "$current/$p"
            try {
                sftp.stat(current)
            } catch (_: Exception) {
                sftp.mkdir(current)
            }
        }
    }
}
