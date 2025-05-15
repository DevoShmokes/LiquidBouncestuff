/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.features.misc.proxy

import io.netty.handler.proxy.HttpProxyHandler
import io.netty.handler.proxy.Socks5ProxyHandler
import net.ccbluex.liquidbounce.api.thirdparty.IpInfoApi
import java.net.InetSocketAddress

/**
 * Contains serializable proxy data
 */
data class Proxy(
    val host: String,
    val port: Int,
    val credentials: Credentials?,
    val type: Type?,
    var forwardAuthentication: Boolean = false,
    var ipInfo: IpInfoApi.IpData? = null,
    var favorite: Boolean = false
) {

    enum class Type {
        HTTP,
        SOCKS5,
    }

    val address
        get() = InetSocketAddress(host, port)

    fun handler() = when (type ?: Type.SOCKS5) {
        Type.HTTP -> if (credentials == null) {
            HttpProxyHandler(address)
        } else {
            HttpProxyHandler(address, credentials.username, credentials.password)
        }
        Type.SOCKS5 -> if (credentials == null) {
            Socks5ProxyHandler(address)
        } else {
            Socks5ProxyHandler(address, credentials.username, credentials.password)
        }
    }

    class Credentials(val username: String, val password: String)

    companion object {

        val NONE = Proxy("", 0, null, Type.SOCKS5)

        /**
         * Parse a proxy string into a [Proxy] object.
         *
         * Detects the proxy type based on the prefix of the string.
         * - `http://` for HTTP proxy
         * - `socks5://` or `socks5h://` for SOCKS5 proxy
         *
         * Accepts the following formats:
         * - `username:password:host:port`
         * - `username:password@host:port`
         *
         * What is NOT supported:
         * - `hostname:port:username:password`
         */
        fun parse(text: String): Proxy {
            val proxyType = when {
                @Suppress("HttpUrlsUsage")
                text.startsWith("http://") -> Type.HTTP
                text.startsWith("socks5://") || text.startsWith("socks5h://") -> Type.SOCKS5
                else -> Type.SOCKS5 // Default to SOCKS5
            }
            val proxyText = text.substringAfter("://")

            return when {
                // username:password@host:port
                proxyText.contains("@") -> {
                    val credentials = proxyText.substringBefore("@")
                    val hostPort = proxyText.substringAfter("@")

                    val username = credentials.substringBefore(":")
                    val password = credentials.substringAfter(":")
                    val host = hostPort.substringBefore(":")
                    val port = hostPort.substringAfter(":").toInt()

                    Proxy(host, port, credentials(username, password), proxyType)
                }

                // username:password:host:port
                proxyText.count { it == ':' } == 3 -> {
                    val parts = proxyText.split(":")
                    val username = parts[0]
                    val password = parts[1]
                    val host = parts[2]
                    val port = parts[3].toInt()

                    Proxy(host, port, credentials(username, password), proxyType)
                }

                // host:port
                else -> {
                    val parts = proxyText.split(":")
                    val host = parts[0]
                    val port = parts[1].toInt()

                    Proxy(host, port, null, proxyType)
                }

            }
        }

        @JvmStatic
        fun credentials(username: String, password: String): Credentials? {
            return if (username.isBlank() || password.isBlank()) {
                null
            } else {
                Credentials(username, password)
            }
        }
    }

}
