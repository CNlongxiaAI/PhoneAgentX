package com.phoneagentx.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phoneagentx.PhoneAgentXApp
import com.phoneagentx.ConnectionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val isAdbPaired: Boolean = false,
    val quickActions: List<QuickAction> = listOf(
        QuickAction("screenshot", "截图", "获取当前屏幕截图"),
        QuickAction("explore", "探索界面", "AI 分析当前界面状�?),
        QuickAction("build", "测试点击", "测试 tap �?swipe 操作")
    ),
    val skills: List<SkillInfo> = listOf(
        SkillInfo("wechat-auto-reply", "微信托管", "AI 自动回复微信消息", "message-circle", "30分钟", 5000),
        SkillInfo("browse-tiktok", "刷抖�?, "自动浏览短视频并汇报", "play-circle", "1分钟", 1500),
        SkillInfo("check-weather", "查看天气", "获取当前天气信息", "globe", "10�?, 500),
        SkillInfo("daily-news", "每日新闻", "获取今日新闻摘要", "star", "30�?, 1000)
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
            val state = app.socketClient.connectionState.value
            _uiState.value = _uiState.value.copy(connectionState = state)
        }
    }

    fun connect() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(connectionState = ConnectionState.CONNECTING)
            try {
                PhoneAgentXApp.instance.socketClient.connect()
                PhoneAgentXApp.instance.bridge.start()
                _uiState.value = _uiState.value.copy(connectionState = PhoneAgentXApp.instance.socketClient.connectionState.value)
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
        // TODO: 启动 ADB 配对流程
    }

    fun executeQuickAction(action: QuickAction) {
        viewModelScope.launch {
            when (action.icon) {
                "screenshot" -> {
                    // 执行截图
                }
                "explore" -> {
                    // AI 分析界面
                }
                "build" -> {
                    // 测试操作
                }
            }
        }
    }

    fun runSkill(skill: SkillInfo) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                // 加载并运�?Skill
                val skillEngine = PhoneAgentXApp.instance.skillEngine
                // TODO: 实际加载 Skill JSON 并运�?                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }
}