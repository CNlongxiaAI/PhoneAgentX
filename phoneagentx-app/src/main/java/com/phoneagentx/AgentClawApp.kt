package com.phoneagentx

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.phoneagentx.core.BridgeServerManager
import com.phoneagentx.core.engine.AiProviderManager
import com.phoneagentx.core.engine.SkillEngine
import com.phoneagentx.core.network.PhoneAgentXApiClient
import com.phoneagentx.core.settings.SettingsManager
import com.phoneagentx.core.socket.TutuSocketClient

class PhoneAgentXApp : Application() {

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
            NotificationChannel(CHANNEL_ADB, "ADB Pairing", NotificationManager.IMPORTANCE_LOW)
                .apply { description = "Wireless ADB pairing service" },
            NotificationChannel(CHANNEL_NODE, "Node Host", NotificationManager.IMPORTANCE_LOW)
                .apply { description = "PhoneAgentX node service" },
            NotificationChannel(CHANNEL_SKILL, "Skill Running", NotificationManager.IMPORTANCE_LOW)
                .apply { description = "Skill execution notifications" }
        ).forEach { nm.createNotificationChannel(it) }
    }

    private fun initCoreComponents() {
        settingsManager = SettingsManager(this)
        socketClient = TutuSocketClient("127.0.0.1", 28200)
        bridge = BridgeServerManager(this, socketClient)
        apiClient = PhoneAgentXApiClient()
        skillEngine = SkillEngine(
            bridge = bridge,
            apiClient = apiClient,
            aiProviderFactory = { AiProviderManager.getProvider() }
        )
    }

    fun getConnectionState() = socketClient.connectionState.value
    fun isConnected() = socketClient.connectionState.value == com.phoneagentx.core.socket.ConnectionState.CONNECTED
}

enum class ConnectionState {
    DISCONNECTED, CONNECTING, CONNECTED, AUTHENTICATED, ERROR
}
