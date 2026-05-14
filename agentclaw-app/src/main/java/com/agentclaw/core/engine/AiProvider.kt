package com.agentclaw.core.engine

import android.util.Log
import com.agentclaw.AgentClawApp
import kotlinx.coroutines.delay

/**
 * AI Provider 抽象接口
 */
interface AiProvider {
    suspend fun analyze(
        prompt: String,
        screenshotBase64: String? = null,
        uiTreeJson: String? = null,
        history: List<AiMessage> = emptyList()
    ): String
}

data class AiMessage(val role: String, val content: String)

/**
 * AI Provider 管理器
 */
object AiProviderManager {
    private var provider: AiProvider? = null

    fun setProvider(p: AiProvider) {
        provider = p
    }

    fun getProvider(): AiProvider? = provider

    // 默认使用本地模拟 Provider（用于测试）
    fun getDefaultProvider(): AiProvider = LocalTestProvider()
}

/**
 * 本地测试用 AI Provider
 */
class LocalTestProvider : AiProvider {
    private val TAG = "LocalTestProvider"

    override suspend fun analyze(
        prompt: String,
        screenshotBase64: String?,
        uiTreeJson: String?,
        history: List<AiMessage>
    ): String {
        Log.d(TAG, "分析提示: ${prompt.take(100)}")
        delay(500) // 模拟延迟

        // 根据 prompt 内容返回模拟结果
        return when {
            prompt.contains("判断", ignoreCase = true) -> "正常"
            prompt.contains("描述", ignoreCase = true) -> "当前界面是主页面，可以看到菜单按钮"
            prompt.contains("回复", ignoreCase = true) -> "好的，已发送。finished"
            prompt.contains("点击", ignoreCase = true) -> "Action: click(540, 960)"
            else -> "已完成操作"
        }
    }
}

/**
 * DeepSeek Provider
 */
class DeepSeekProvider(apiKey: String, model: String = "deepseek-chat") : AiProvider {
    private val apiKey = apiKey
    private val model = model

    override suspend fun analyze(
        prompt: String,
        screenshotBase64: String?,
        uiTreeJson: String?,
        history: List<AiMessage>
    ): String {
        // TODO: 实现 DeepSeek API 调用
        return "Not implemented"
    }
}