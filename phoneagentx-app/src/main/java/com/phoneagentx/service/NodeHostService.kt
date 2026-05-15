package com.phoneagentx.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.phoneagentx.PhoneAgentXApp
import com.phoneagentx.MainActivity
import kotlinx.coroutines.*
import java.io.File

class NodeHostService : Service() {

    private val TAG = "NodeHostService"
    private val NOTIFICATION_ID = 2001

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var nodeProcess: Process? = null

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "NodeHostService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "NodeHostService starting")

        val notification = createNotification("PhoneAgentX Node Running")
        startForeground(NOTIFICATION_ID, notification)

        scope.launch {
            startNodeHost()
        }

        return START_STICKY
    }

    private fun startNodeHost() {
        try {
            val nodePath = File(filesDir, "node/bin/node")
            val scriptPath = extractAsset("phoneagentx-node-host.js")

            if (!nodePath.exists()) {
                Log.e(TAG, "Node.js not found. Please install OpenClaw runtime first.")
                return
            }

            val processBuilder = ProcessBuilder(
                nodePath.absolutePath,
                scriptPath.absolutePath
            )
            processBuilder.environment()["PhoneAgentX_BRIDGE_URL"] = "http://127.0.0.1:18790"
            processBuilder.environment()["OPENCLAW_GATEWAY_URL"] = "ws://127.0.0.1:18789"

            nodeProcess = processBuilder.start()

            nodeProcess?.inputStream?.bufferedReader()?.forEachLine { line ->
                Log.d(TAG, "[Node] $line")
            }

            nodeProcess?.errorStream?.bufferedReader()?.forEachLine { line ->
                Log.e(TAG, "[Node Error] $line")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start Node Host: ${e.message}")
        }
    }

    private fun extractAsset(assetName: String): File {
        val destFile = File(cacheDir, assetName)
        if (!destFile.exists()) {
            assets.open(assetName).use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
        return destFile
    }

    private fun createNotification(content: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, PhoneAgentXApp.CHANNEL_NODE)
            .setContentTitle("PhoneAgentX")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        Log.i(TAG, "NodeHostService stopping")
        nodeProcess?.destroy()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
