package com.ftpserver.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Binder
import android.os.Build
import android.os.Environment
import android.os.IBinder
import androidx.core.app.NotificationCompat
import org.apache.ftpserver.FtpServer
import org.apache.ftpserver.FtpServerFactory
import org.apache.ftpserver.ftplet.Authority
import org.apache.ftpserver.ftplet.UserManager
import org.apache.ftpserver.listener.ListenerFactory
import org.apache.ftpserver.usermanager.PropertiesUserManagerFactory
import org.apache.ftpserver.usermanager.impl.BaseUser
import org.apache.ftpserver.usermanager.impl.WritePermission
import java.io.File
import java.net.InetAddress
import java.net.NetworkInterface
import kotlin.random.Random

class FTPServerService : Service() {
    
    private var ftpServer: FtpServer? = null
    private val binder = LocalBinder()
    private var isRunning = false
    private var currentPassword: String = generateRandomPassword()
    
    companion object {
        const val CHANNEL_ID = "FTPServerChannel"
        const val NOTIFICATION_ID = 1
        const val PORT = 2221
        const val USERNAME = "android"
        const val ACTION_STOP = "com.ftpserver.app.ACTION_STOP"
        const val EXTRA_PASSWORD = "extra_password"
        
        fun generateRandomPassword(): String {
            val chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789"
            return (1..8).map { chars[Random.nextInt(chars.length)] }.joinToString("")
        }
        
        fun getIPAddress(): String {
            try {
                val interfaces = NetworkInterface.getNetworkInterfaces()
                while (interfaces.hasMoreElements()) {
                    val networkInterface = interfaces.nextElement()
                    val addresses = networkInterface.inetAddresses
                    while (addresses.hasMoreElements()) {
                        val address = addresses.nextElement()
                        if (!address.isLoopbackAddress && address is java.net.Inet4Address) {
                            return address.hostAddress ?: "unknown"
                        }
                    }
                }
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
            // Check for password from intent
            intent?.getStringExtra(EXTRA_PASSWORD)?.let {
                if (!isRunning) {
                    currentPassword = it
                }
            }
        }
        return START_STICKY
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        
        // Register broadcast receiver for stop action
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(stopReceiver, IntentFilter(ACTION_STOP), Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(stopReceiver, IntentFilter(ACTION_STOP))
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "FTP Server",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "FTP Server Running Status"
                setShowBadge(true)
                enableVibration(false)
                setSound(null, null)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        // Intent to open MainActivity when notification is tapped
        val notificationIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        // Intent to stop server - use Service intent directly
        val stopIntent = Intent(this, FTPServerService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("FTP Server Running")
            .setContentText("Tap to open • IP: ${getIPAddress()}:$PORT")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Stop",
                stopPendingIntent
            )
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(false)
            .build()
    }
    
    fun startFTPServer(password: String? = null): Boolean {
        if (isRunning) return true
        
        // Use provided password or generate new one
        password?.let { currentPassword = it }
        
        try {
            val serverFactory = FtpServerFactory()
            
            // Create listener
            val listenerFactory = ListenerFactory()
            listenerFactory.port = PORT
            serverFactory.addListener("default", listenerFactory.createListener())
            
            // Create user manager
            val userManagerFactory = PropertiesUserManagerFactory()
            val userManager: UserManager = userManagerFactory.createUserManager()
            
            // Create user
            val user = BaseUser()
            user.name = USERNAME
            user.password = currentPassword
            
            // Set home directory to root of external storage (entire phone storage)
            val homeDir = Environment.getExternalStorageDirectory()
            user.homeDirectory = homeDir.absolutePath
            
            // Set permissions
            val authorities = mutableListOf<Authority>()
            authorities.add(WritePermission())
            user.authorities = authorities
            
            userManager.save(user)
            serverFactory.userManager = userManager
            
            // Create and start server
            ftpServer = serverFactory.createServer()
            ftpServer?.start()
            
            isRunning = true
            startForeground(NOTIFICATION_ID, createNotification())
            
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
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun isServerRunning(): Boolean = isRunning
    
    fun getPassword(): String = currentPassword
    
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