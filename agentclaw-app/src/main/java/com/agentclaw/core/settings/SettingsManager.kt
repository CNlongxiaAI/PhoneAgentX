package com.agentclaw.core.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<androidx.datastore.preferences.core.Preferences> by preferencesDataStore(name = "settings")

class SettingsManager(private val context: Context) {

    companion object {
        val ADB_HOST = stringPreferencesKey("adb_host")
        val ADB_PORT = intPreferencesKey("adb_port")
        val AI_PROVIDER = stringPreferencesKey("ai_provider")
        val AI_API_KEY = stringPreferencesKey("ai_api_key")
        val AI_MODEL = stringPreferencesKey("ai_model")
        val GATEWAY_URL = stringPreferencesKey("gateway_url")
        val NODE_ENABLED = booleanPreferencesKey("node_enabled")
        val AUTO_CONNECT = booleanPreferencesKey("auto_connect")
    }

    val adbHost = context.dataStore.data.map { it[ADB_HOST] ?: "127.0.0.1" }
    val adbPort = context.dataStore.data.map { it[ADB_PORT] ?: 5555 }
    val aiProvider = context.dataStore.data.map { it[AI_PROVIDER] ?: "deepseek" }
    val aiApiKey = context.dataStore.data.map { it[AI_API_KEY] ?: "" }
    val aiModel = context.dataStore.data.map { it[AI_MODEL] ?: "deepseek-chat" }
    val gatewayUrl = context.dataStore.data.map { it[GATEWAY_URL] ?: "ws://127.0.0.1:18789" }
    val nodeEnabled = context.dataStore.data.map { it[NODE_ENABLED] ?: false }
    val autoConnect = context.dataStore.data.map { it[AUTO_CONNECT] ?: true }

    suspend fun setAdbHost(value: String) {
        context.dataStore.updateData { it.toMutablePreferences().apply { set(ADB_HOST, value) } }
    }

    suspend fun setAdbPort(value: Int) {
        context.dataStore.updateData { it.toMutablePreferences().apply { set(ADB_PORT, value) } }
    }

    suspend fun setAiProvider(value: String) {
        context.dataStore.updateData { it.toMutablePreferences().apply { set(AI_PROVIDER, value) } }
    }

    suspend fun setAiApiKey(value: String) {
        context.dataStore.updateData { it.toMutablePreferences().apply { set(AI_API_KEY, value) } }
    }

    suspend fun setAiModel(value: String) {
        context.dataStore.updateData { it.toMutablePreferences().apply { set(AI_MODEL, value) } }
    }

    suspend fun setGatewayUrl(value: String) {
        context.dataStore.updateData { it.toMutablePreferences().apply { set(GATEWAY_URL, value) } }
    }

    suspend fun setNodeEnabled(value: Boolean) {
        context.dataStore.updateData { it.toMutablePreferences().apply { set(NODE_ENABLED, value) } }
    }

    suspend fun getAll(): Map<String, Any?> {
        val prefs = context.dataStore.data.first()
        return mapOf(
            "adb_host" to prefs[ADB_HOST],
            "adb_port" to prefs[ADB_PORT],
            "ai_provider" to prefs[AI_PROVIDER],
            "ai_api_key" to prefs[AI_API_KEY]?.takeIf { it.isNotEmpty() },
            "ai_model" to prefs[AI_MODEL],
            "gateway_url" to prefs[GATEWAY_URL],
            "node_enabled" to prefs[NODE_ENABLED],
            "auto_connect" to prefs[AUTO_CONNECT]
        )
    }
}