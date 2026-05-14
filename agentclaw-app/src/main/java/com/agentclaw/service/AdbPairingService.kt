package com.agentclaw.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.agentclaw.AgentClawApp
import com.agentclaw.MainActivity

class AdbPairingService : Service() {

    private val TAG = "AdbPairingService"
    private val NOTIFICATION_ID = 2002

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "AdbPairingService 创建")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "AdbPairingService 启动")

        val pendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, AgentClawApp.CHANNEL_ADB)
            .setContentTitle("ADB 配对服务")
            .setContentText("等待 ADB 配对...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)

        // TODO: 实现 ADB 配对逻辑
        // 1. 启动 ADB daemon
        // 2. 监听配对请求
        // 3. 完成 SPAKE2 握手
        // 4. 存储配对信息

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        Log.i(TAG, "AdbPairingService 停止")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}