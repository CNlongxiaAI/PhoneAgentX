package com.phoneagentx

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.phoneagentx.core.engine.SkillEngine
import com.phoneagentx.core.network.PhoneAgentXApiClient
import com.phoneagentx.core.settings.SettingsManager
import com.phoneagentx.core.socket.TutuSocketClient

class PhoneAgentXApp : Application() {

    // ── 全局单例 ──
    lateinit var socketClient: TutuSocketClient
        private set

    lateinit var bridge: BridgeServerManager
        private set

    lateinit var skillEngine: SkillEngine
        private set

    lateinit var settingsManager: SettingsManager
        private set

    lateinit var apiClient: PhoneAgentXApiClient
        private set

    // ── 通知渠道 ──
    companion object {
        const val CHANNEL_ADB = "adb_pairing"
        const val CHANNEL_NODE = "node_host"
        const val CHANNEL_SKILL = "skill_running"

        lateinit var instance: PhoneAgentXApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        createNotificationChannels()
        initCoreComponents()
    }

    private fun createNotificationChannels() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        listOf(
            NotificationChannel(CHANNEL_ADB, "ADB 配对", NotificationManager.IMPORTANCE_LOW)
                .apply { description = "无线 ADB 配对服务" },
            NotificationChannel(CHANNEL_NODE, "节点运行", NotificationManager.IMPORTANCE_LOW)
                .apply { description = "PhoneAgentX 节点服务" },
            NotificationChannel(CHANNEL_SKILL, "技能执�?, NotificationManager.IMPORTANCE_LOW)
                .apply { description = "技能运行通知" }
        ).forEach { nm.createNotificationChannel(it) }
    }

    private fun initCoreComponents() {
        // 设置管理
        settingsManager = SettingsManager(this)

        // Socket 客户端（连接�?TutuGui Server�?        socketClient = TutuSocketClient("127.0.0.1", 28200)

        // Bridge Server 管理�?        bridge = BridgeServerManager(this, socketClient)

        // API 客户�?        apiClient = PhoneAgentXApiClient()

        // Skill 引擎
        skillEngine = SkillEngine(
            bridge = bridge,
            apiClient = apiClient,
            aiProviderFactory = { AiProviderManager.getProvider() }
        )
    }

    fun getConnectionState() = socketClient.connectionState.value

    fun isConnected() = socketClient.connectionState.value == ConnectionState.CONNECTED
}

enum class ConnectionState {
    DISCONNECTED, CONNECTING, CONNECTED, AUTHENTICATED, ERROR
}