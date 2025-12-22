package com.example.romdownloader.network

import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class HostScanner {
    fun getLocalSubnetPrefix(): String? {
        val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
        for (ni in interfaces) {
            val addrs = Collections.list(ni.inetAddresses)
            for (addr in addrs) {
                if (addr is Inet4Address && !addr.isLoopbackAddress) {
                    val ip = addr.hostAddress
                    val parts = ip.split('.')
                    if (parts.size == 4) return parts.take(3).joinToString(".")
                }
            }
        }
        return null
    }

    fun scanQuick(prefix: String, ranges: List<IntRange> = listOf(1..10, 100..110, 130..140, 150..160), timeoutMs: Int = 400): List<String> {
        val executor = Executors.newFixedThreadPool(16)
        val found = Collections.synchronizedList(mutableListOf<String>())
        for (range in ranges) {
            for (i in range) {
                val host = "$prefix.$i"
                executor.submit {
                    try {
                        val reachable = java.net.InetAddress.getByName(host).isReachable(timeoutMs)
                        if (reachable) found.add(host)
                    } catch (_: Exception) { }
                }
            }
        }
        executor.shutdown()
        executor.awaitTermination(15, TimeUnit.SECONDS)
        return found.distinct().sorted()
    }
}
