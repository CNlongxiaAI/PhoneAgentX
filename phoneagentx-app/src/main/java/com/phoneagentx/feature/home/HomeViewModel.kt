package com.phoneagentx.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phoneagentx.PhoneAgentXApp
import com.phoneagentx.ConnectionState
import com.phoneagentx.core.socket.ConnectionState as SocketConnectionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val isAdbPaired: Boolean = false,
    val quickActions: List<QuickAction> = listOf(
        QuickAction("screenshot", "Screenshot", "Capture current screen"),
        QuickAction("explore", "Explore UI", "AI analyze current interface"),
        QuickAction("build", "Test Ops", "Test tap/swipe operations")
    ),
    val skills: List<SkillInfo> = listOf(
        SkillInfo("wechat-auto-reply", "WeChat Auto-Reply", "AI auto-reply WeChat messages", "message-circle", "30min", 5000),
        SkillInfo("browse-tiktok", "Browse TikTok", "Auto browse short videos", "play-circle", "1min", 1500),
        SkillInfo("check-weather", "Weather", "Get current weather", "globe", "10min", 500),
        SkillInfo("daily-news", "Daily News", "Get today's news summary", "star", "30min", 1000)
    ),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class QuickAction(
    val icon: String,
    val name: String,
    val description: String
)

data class SkillInfo(
    val slug: String,
    val name: String,
    val description: String,
    val icon: String,
    val estimatedTime: String,
    val estimatedTokens: Int
)

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        refreshStatus()
    }

    fun refreshStatus() {
        viewModelScope.launch {
            val app = PhoneAgentXApp.instance
            val rawState = app.socketClient.connectionState.value
            val state = when (rawState) {
                SocketConnectionState.DISCONNECTED -> ConnectionState.DISCONNECTED
                SocketConnectionState.CONNECTING -> ConnectionState.CONNECTING
                SocketConnectionState.CONNECTED -> ConnectionState.CONNECTED
                SocketConnectionState.AUTHENTICATED -> ConnectionState.AUTHENTICATED
                SocketConnectionState.ERROR -> ConnectionState.ERROR
            }
            _uiState.value = _uiState.value.copy(connectionState = state)
        }
    }

    fun connect() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(connectionState = ConnectionState.CONNECTING)
            try {
                PhoneAgentXApp.instance.socketClient.connect()
                PhoneAgentXApp.instance.bridge.start()
                val rawState = PhoneAgentXApp.instance.socketClient.connectionState.value
                val state = when (rawState) {
                    SocketConnectionState.DISCONNECTED -> ConnectionState.DISCONNECTED
                    SocketConnectionState.CONNECTING -> ConnectionState.CONNECTING
                    SocketConnectionState.CONNECTED -> ConnectionState.CONNECTED
                    SocketConnectionState.AUTHENTICATED -> ConnectionState.AUTHENTICATED
                    SocketConnectionState.ERROR -> ConnectionState.ERROR
                }
                _uiState.value = _uiState.value.copy(connectionState = state)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(connectionState = ConnectionState.ERROR, error = e.message)
            }
        }
    }

    fun disconnect() {
        PhoneAgentXApp.instance.socketClient.disconnect()
        PhoneAgentXApp.instance.bridge.stop()
        _uiState.value = _uiState.value.copy(connectionState = ConnectionState.DISCONNECTED)
    }

    fun startAdbPairing() {
        // TODO: Launch ADB pairing flow
    }

    fun executeQuickAction(action: QuickAction) {
        viewModelScope.launch {
            when (action.icon) {
                "screenshot" -> {
                    PhoneAgentXApp.instance.bridge.takeScreenshot()
                }
                "explore" -> {
                    // AI analyze interface
                }
                "build" -> {
                    // Test operations
                }
            }
        }
    }

    fun runSkill(skill: SkillInfo) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val skillEngine = PhoneAgentXApp.instance.skillEngine
                // TODO: Load and run skill JSON
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }
}
