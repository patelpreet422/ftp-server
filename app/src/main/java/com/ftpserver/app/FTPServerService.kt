package com.ftpserver.app

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Binder
import android.os.Environment
import android.os.IBinder
import androidx.core.content.ContextCompat
import org.apache.ftpserver.FtpServer
import org.apache.ftpserver.FtpServerFactory
import org.apache.ftpserver.ftplet.Authority
import org.apache.ftpserver.ftplet.UserManager
import org.apache.ftpserver.listener.ListenerFactory
import org.apache.ftpserver.usermanager.PropertiesUserManagerFactory
import org.apache.ftpserver.usermanager.impl.BaseUser
import org.apache.ftpserver.usermanager.impl.WritePermission
import kotlin.random.Random

class FTPServerService : Service() {

    private var ftpServer: FtpServer? = null
    private val binder = LocalBinder()
    private var isRunning = false
    private var currentPassword: String = generateRandomPassword()
    private var currentUsername: String = DEFAULT_USERNAME

    companion object {
        const val PORT = 2221
        const val DEFAULT_USERNAME = "android"
        const val ACTION_STOP = "com.ftpserver.app.ACTION_STOP"
        const val EXTRA_PASSWORD = "extra_password"
        const val EXTRA_USERNAME = "extra_username"

        fun generateRandomPassword(): String {
            val chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789"
            return (1..8).map { chars[Random.nextInt(chars.length)] }.joinToString("")
        }

        fun getIPAddress(): String {
            try {
                data class CandidateAddress(
                    val ip: String,
                    val interfaceName: String,
                    val priority: Int
                )

                val candidates = mutableListOf<CandidateAddress>()
                val interfaces = java.net.NetworkInterface.getNetworkInterfaces()

                while (interfaces.hasMoreElements()) {
                    val networkInterface = interfaces.nextElement()
                    if (!networkInterface.isUp) continue

                    val interfaceName = networkInterface.name.lowercase()
                    val addresses = networkInterface.inetAddresses

                    while (addresses.hasMoreElements()) {
                        val address = addresses.nextElement()
                        if (address.isLoopbackAddress || address !is java.net.Inet4Address) continue

                        val ip = address.hostAddress ?: continue
                        val ipParts = ip.split(".").mapNotNull { it.toIntOrNull() }
                        if (ipParts.size != 4) continue

                        var priority = 0

                        when {
                            interfaceName.startsWith("wlan") -> priority += 100
                            interfaceName.startsWith("eth") -> priority += 90
                            interfaceName.startsWith("ap") -> priority += 80
                            interfaceName.startsWith("usb") -> priority += 70
                            interfaceName.startsWith("rmnet") -> priority += 10
                            interfaceName.startsWith("tun") -> priority += 5
                        }

                        when {
                            ipParts[0] == 192 && ipParts[1] == 168 -> priority += 50
                            ipParts[0] == 10 -> priority += 45
                            ipParts[0] == 172 && ipParts[1] in 16..31 -> priority += 40
                            ipParts[0] == 100 && ipParts[1] in 64..127 -> priority += 1
                        }

                        candidates.add(CandidateAddress(ip, interfaceName, priority))
                    }
                }

                return candidates.maxByOrNull { it.priority }?.ip ?: "unknown"
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return "unknown"
        }
    }

    private val stopReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_STOP) {
                stopFTPServer()
            }
        }
    }

    inner class LocalBinder : Binder() {
        fun getService(): FTPServerService = this@FTPServerService
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopFTPServer()
        } else {
            if (!isRunning) {
                intent?.getStringExtra(EXTRA_PASSWORD)?.let {
                    currentPassword = it
                }
                intent?.getStringExtra(EXTRA_USERNAME)?.let {
                    currentUsername = it
                }
            }
        }
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()

        ContextCompat.registerReceiver(
            this,
            stopReceiver,
            IntentFilter(ACTION_STOP),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    fun startFTPServer(username: String? = null, password: String? = null): Boolean {
        if (isRunning) return true

        username?.let { currentUsername = it }
        password?.let { currentPassword = it }

        try {
            val serverFactory = FtpServerFactory()

            val listenerFactory = ListenerFactory()
            listenerFactory.port = PORT
            serverFactory.addListener("default", listenerFactory.createListener())

            val userManagerFactory = PropertiesUserManagerFactory()
            val userManager: UserManager = userManagerFactory.createUserManager()

            val user = BaseUser()
            user.name = currentUsername
            user.password = currentPassword

            val homeDir = Environment.getExternalStorageDirectory()
            user.homeDirectory = homeDir.absolutePath

            val authorities = mutableListOf<Authority>()
            authorities.add(WritePermission())
            user.authorities = authorities

            userManager.save(user)
            serverFactory.userManager = userManager

            ftpServer = serverFactory.createServer()
            ftpServer?.start()

            isRunning = true
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    fun stopFTPServer() {
        if (!isRunning) return

        try {
            ftpServer?.stop()
            ftpServer = null
            isRunning = false
            stopSelf()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun isServerRunning(): Boolean = isRunning

    fun getPassword(): String = currentPassword

    fun getUsername(): String = currentUsername

    fun regeneratePassword(): String {
        if (!isRunning) {
            currentPassword = generateRandomPassword()
        }
        return currentPassword
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(stopReceiver)
        } catch (e: Exception) {
            // Receiver might not be registered
        }
        stopFTPServer()
    }
}