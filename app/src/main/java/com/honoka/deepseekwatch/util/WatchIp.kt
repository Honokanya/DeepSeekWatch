package com.honoka.deepseekwatch.util

import java.net.Inet4Address
import java.net.NetworkInterface

/** 枚举网卡获取 WiFi IPv4（无需权限） */
fun getLocalIpv4(): String? {
    val interfaces = NetworkInterface.getNetworkInterfaces() ?: return null
    for (iface in interfaces) {
        if (!iface.isUp || iface.isLoopback) continue
        for (addr in iface.inetAddresses) {
            val ip = addr as? Inet4Address ?: continue
            if (!ip.isLoopbackAddress) return ip.hostAddress
        }
    }
    return null
}
