package com.example.romdownloader.network

import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.sftp.SFTPClient
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security

class SshTester {
    fun test(host: HostConfig): Result<String> {
        return runCatching {
            // Ensure BouncyCastle provider present (replace Android's stub BC)
            try {
                Security.removeProvider("BC")
                Security.insertProviderAt(BouncyCastleProvider(), 1)
            } catch (_: Throwable) {}
            SSHClient().use { ssh ->
                ssh.addHostKeyVerifier(PromiscuousVerifier())
                try {
                    ssh.connectTimeout = 15000
                    ssh.timeout = 20000
                } catch (_: Throwable) { }
                ssh.connect(host.host, host.port)
                try {
                    val passwords = buildList {
                        if (host.password.isNotEmpty()) add(host.password)
                        add("root"); add("rocknix"); add("muos"); add("")
                    }.distinct()
                    var authed = false
                    for (pwd in passwords) {
                        try {
                            ssh.authPassword(host.username, pwd)
                            authed = true
                            break
                        } catch (_: Exception) { }
                    }
                    if (!authed) throw IllegalStateException("SSH auth failed for ${host.username}@${host.host}")

                    ssh.newSFTPClient().use { sftp ->
                        val base = resolveRomsBaseDir(sftp)
                        // If we got here, connection and SFTP are OK
                        return@runCatching "Connected. ROM base: $base"
                    }
                } finally {
                    ssh.disconnect()
                }
            }
        }
    }

    private fun resolveRomsBaseDir(sftp: SFTPClient): String {
        val candidates = listOf("/storage/roms", "/roms", "/mnt/mmc/ROMS", "/mnt/sdcard/ROMS")
        for (c in candidates) {
            try { sftp.stat(c); return c } catch (_: Exception) {}
        }
        return "/roms"
    }
}
