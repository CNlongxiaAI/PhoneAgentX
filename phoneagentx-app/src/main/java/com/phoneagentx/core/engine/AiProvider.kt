package com.phoneagentx.core.engine

import android.util.Log
import kotlinx.coroutines.delay

interface AiProvider {
    suspend fun analyze(
        prompt: String,
        screenshotBase64: String? = null,
        uiTreeJson: String? = null,
        history: List<AiMessage> = emptyList()
    ): String
}

data class AiMessage(val role: String, val content: String)

object AiProviderManager {
    private var provider: AiProvider? = null

    fun setProvider(p: AiProvider) {
        provider = p
    }

    fun getProvider(): AiProvider? = provider

    fun getDefaultProvider(): AiProvider = LocalTestProvider()
}

class LocalTestProvider : AiProvider {
    private val TAG = "LocalTestProvider"

    override suspend fun analyze(
        prompt: String,
        screenshotBase64: String?,
        uiTreeJson: String?,
        history: List<AiMessage>
    ): String {
        Log.d(TAG, "Analyze prompt: ${prompt.take(100)}")
        delay(500)

        return when {
            prompt.contains("判断", ignoreCase = true) -> "Normal"
            prompt.contains("描述", ignoreCase = true) -> "Current screen is main page, menu visible"
            prompt.contains("回复", ignoreCase = true) -> "Done. finished"
            prompt.contains("点击", ignoreCase = true) -> "Action: click(540, 960)"
            else -> "Operation completed"
        }
    }
}

class DeepSeekProvider(apiKey: String, model: String = "deepseek-chat") : AiProvider {
    private val apiKey = apiKey
    private val model = model

    override suspend fun analyze(
        prompt: String,
        screenshotBase64: String?,
        uiTreeJson: String?,
        history: List<AiMessage>
    ): String {
        return "Not implemented"
    }
}