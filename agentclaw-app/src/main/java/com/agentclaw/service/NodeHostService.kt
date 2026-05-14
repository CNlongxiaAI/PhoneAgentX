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
import com.agentclaw.R
import kotlinx.coroutines.*

class NodeHostService : Service() {

    private val TAG = "NodeHostService"
    private val NOTIFICATION_ID = 2001

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var nodeProcess: Process? = null

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "NodeHostService 创建")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "NodeHostService 启动")

        val notification = createNotification("AgentClaw 节点服务运行中")
        startForeground(NOTIFICATION_ID, notification)

        scope.launch {
            startNodeHost()
        }

        return START_STICKY
    }

    private fun startNodeHost() {
        try {
            // 启动 Node Host 进程
            // Node.js 环境需要在 APK 中预装
            val nodePath = filesDir.resolve("node/bin/node")
            val scriptPath = assetsFilePath("agentclaw-node-host.js")

            if (!nodePath.exists()) {
                Log.e(TAG, "Node.js 未找到，请先安装 OpenClaw 运行时")
                return
            }

            val processBuilder = ProcessBuilder(
                nodePath.absolutePath,
                scriptPath.absolutePath
            )
            processBuilder.environment()["AGENTCLAW_BRIDGE_URL"] = "http://127.0.0.1:18790"
            processBuilder.environment()["OPENCLAW_GATEWAY_URL"] = "ws://127.0.0.1:18789"

            nodeProcess = processBuilder.start()

            // 读取输出日志
            nodeProcess?.inputStream?.bufferedReader()?.forEachLine { line ->
                Log.d(TAG, "[Node] $line")
            }

            nodeProcess?.errorStream?.bufferedReader()?.forEachLine { line ->
                Log.e(TAG, "[Node Error] $line")
            }
        } catch (e: Exception) {
            Log.e(TAG, "启动 Node Host 失败: ${e.message}")
        }
    }

    private fun assetsFilePath(assetName: String): String {
        // 从 assets 提取文件到缓存目录
        val destFile = cacheDir.resolve(assetName)
        if (!destFile.exists()) {
            assets.open(assetName).use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
        return destFile.absolutePath
    }

    private fun createNotification(content: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, AgentClawApp.CHANNEL_NODE)
            .setContentTitle("AgentClaw")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        Log.i(TAG, "NodeHostService 停止")
        nodeProcess?.destroy()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}