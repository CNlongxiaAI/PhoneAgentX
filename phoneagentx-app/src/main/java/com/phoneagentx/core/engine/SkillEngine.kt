package com.phoneagentx.core.engine

import android.util.Log
import com.phoneagentx.core.BridgeServerManager
import com.phoneagentx.core.network.PhoneAgentXApiClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*

class SkillEngine(
    private val bridge: BridgeServerManager,
    private val apiClient: PhoneAgentXApiClient,
    private val aiProviderFactory: () -> AiProvider?
) {
    private val TAG = "SkillEngine"

    enum class EngineState { IDLE, LOADING, RUNNING, PAUSED, STOPPED, FINISHED, ERROR }
    enum class StepStatus { PENDING, ACTIVE, DONE, ERROR, SKIP }

    data class RunResult(
        val status: String,
        val completedSteps: Int,
        val totalSteps: Int,
        val errorMessage: String? = null,
        val summaryText: String? = null
    )

    private val _state = MutableStateFlow(EngineState.IDLE)
    val state: StateFlow<EngineState> = _state.asStateFlow()

    private val _currentStepIndex = MutableStateFlow(-1)
    val currentStepIndex: StateFlow<Int> = _currentStepIndex.asStateFlow()

    private val _currentSkill = MutableStateFlow<JsonObject?>(null)
    val currentSkill: StateFlow<JsonObject?> = _currentSkill.asStateFlow()

    @Volatile private var shouldStop = false

    suspend fun runSkill(skillJson: JsonObject): RunResult {
        reset()
        _state.value = EngineState.LOADING

        val steps = skillJson["steps"]?.jsonArray
        if (steps.isNullOrEmpty()) {
            _state.value = EngineState.ERROR
            return RunResult("error", 0, 0, "No steps found")
        }

        _currentSkill.value = skillJson
        _state.value = EngineState.RUNNING
        var completed = 0

        try {
            for (i in 0 until steps.size) {
                if (shouldStop) break
                _currentStepIndex.value = i
                executeStep(steps[i].jsonObject)
                if (!shouldStop) completed++
            }
            _state.value = if (shouldStop) EngineState.STOPPED else EngineState.FINISHED
        } catch (e: Exception) {
            _state.value = EngineState.ERROR
            return RunResult("error", completed, steps.size, e.message)
        }

        return RunResult("success", completed, steps.size)
    }

    private suspend fun executeStep(step: JsonObject) {
        val type = step["type"]?.jsonPrimitive?.contentOrNull ?: return
        Log.d(TAG, "Executing step: $type")

        when (type) {
            "click" -> executeClickStep(step)
            "type" -> executeTypeStep(step)
            "swipe" -> executeSwipeStep(step)
            "wait" -> executeWaitStep(step)
            "screenshot" -> executeScreenshotStep(step)
            "open_app" -> executeOpenAppStep(step)
            "ai_action" -> executeAiActionStep(step)
            "ai_summary" -> executeAiSummaryStep(step)
            "loop" -> executeLoopStep(step)
            "condition" -> executeConditionStep(step)
            "set_var" -> executeSetVarStep(step)
            "prompt_user" -> executePromptUserStep(step)
            "open_url" -> executeOpenUrlStep(step)
            "wait_until_changed" -> executeWaitUntilChanged(step)
            else -> Log.w(TAG, "Unknown step type: $type")
        }
    }

    private fun executeClickStep(step: JsonObject) {
        val x = step["x"]?.jsonPrimitive?.intOrNull ?: return
        val y = step["y"]?.jsonPrimitive?.intOrNull ?: return
        bridge.tap(x, y)
    }

    private fun executeTypeStep(step: JsonObject) {
        val text = step["text"]?.jsonPrimitive?.contentOrNull ?: return
        bridge.typeText(text)
    }

    private fun executeSwipeStep(step: JsonObject) {
        val x1 = step["x1"]?.jsonPrimitive?.intOrNull ?: return
        val y1 = step["y1"]?.jsonPrimitive?.intOrNull ?: return
        val x2 = step["x2"]?.jsonPrimitive?.intOrNull ?: return
        val y2 = step["y2"]?.jsonPrimitive?.intOrNull ?: return
        val duration = step["durationMs"]?.jsonPrimitive?.intOrNull ?: 300
        bridge.swipe(x1, y1, x2, y2, duration)
    }

    private suspend fun executeWaitStep(step: JsonObject) {
        val duration = step["duration"]?.jsonPrimitive?.longOrNull ?: 1000
        delay(duration)
    }

    private suspend fun executeScreenshotStep(step: JsonObject) {
        val key = step["save_to"]?.jsonPrimitive?.contentOrNull ?: "screenshot"
        val result = bridge.takeScreenshot()
        if (result != null) bridge.setVariable(key, result)
    }

    private fun executeOpenAppStep(step: JsonObject) {
        val packageName = step["package"]?.jsonPrimitive?.contentOrNull ?: return
        bridge.openApp(packageName)
    }

    private suspend fun executeAiActionStep(step: JsonObject) {
        val prompt = step["prompt"]?.jsonPrimitive?.contentOrNull ?: return
        val ai = aiProviderFactory() ?: return

        val screenshot = bridge.takeCompressedScreenshot()
        val result = ai.analyze(prompt = prompt, screenshotBase64 = screenshot)
        val action = parseAiAction(result)

        if (action != null) {
            executeAiAction(action)
        }

        if (result.contains("finished", ignoreCase = true)) {
            return
        }
    }

    private suspend fun executeAiSummaryStep(step: JsonObject) {
        val prompt = step["prompt"]?.jsonPrimitive?.contentOrNull ?: return
        val output = step["output"]?.jsonPrimitive?.contentOrNull ?: "result"

        val ai = aiProviderFactory() ?: return
        val result = ai.analyze(prompt = prompt)
        bridge.setVariable(output, result)
    }

    private suspend fun executeLoopStep(step: JsonObject) {
        val loopSteps = step["loop_steps"]?.jsonArray ?: return
        val maxIterations = step["max_iterations"]?.jsonPrimitive?.intOrNull ?: 50
        val maxDuration = step["max_duration"]?.jsonPrimitive?.longOrNull ?: 600000

        val startTime = System.currentTimeMillis()

        repeat(maxIterations) { i ->
            if (shouldStop) return@repeat
            if (System.currentTimeMillis() - startTime > maxDuration) {
                Log.d(TAG, "Loop timeout")
                return@repeat
            }

            for (loopStep in loopSteps) {
                if (shouldStop) break
                executeStep(loopStep.jsonObject)
            }
        }
    }

    private fun executeConditionStep(step: JsonObject) {
        val expression = step["expression"]?.jsonPrimitive?.contentOrNull ?: ""
        val goto = step["goto"]?.jsonPrimitive?.contentOrNull ?: "next"
        Log.d(TAG, "Condition: $expression -> $goto")
    }

    private fun executeSetVarStep(step: JsonObject) {
        val varName = step["var"]?.jsonPrimitive?.contentOrNull ?: return
        val op = step["op"]?.jsonPrimitive?.contentOrNull ?: "assign"
        val value = step["value"]?.jsonPrimitive?.contentOrNull ?: ""

        when (op) {
            "assign" -> bridge.setVariable(varName, value)
            "increment" -> {
                val current = bridge.getVariable(varName)?.toIntOrNull() ?: 0
                bridge.setVariable(varName, (current + (value.toIntOrNull() ?: 1)).toString())
            }
            "append" -> {
                val current = bridge.getVariable(varName) ?: ""
                bridge.setVariable(varName, if (current.isEmpty()) value else "$current,$value")
            }
        }
    }

    private suspend fun executePromptUserStep(step: JsonObject) {
        val title = step["title"]?.jsonPrimitive?.contentOrNull ?: "Waiting for input"
        val timeout = step["timeout"]?.jsonPrimitive?.longOrNull ?: 60000
        delay(1000)
    }

    private fun executeOpenUrlStep(step: JsonObject) {
        val url = step["url"]?.jsonPrimitive?.contentOrNull ?: return
        bridge.openUrl(url)
    }

    private suspend fun executeWaitUntilChanged(step: JsonObject) {
        val timeout = step["timeout"]?.jsonPrimitive?.longOrNull ?: 30000
        delay(minOf(timeout, 5000))
    }

    private fun parseAiAction(result: String): AiAction? {
        val actionMatch = Regex("""Action:\s*(\w+)\s*\(([^)]*)\)""", RegexOption.IGNORE_CASE)
            .find(result) ?: return null

        val actionType = actionMatch.groupValues[1].lowercase()
        val paramsStr = actionMatch.groupValues[2]

        val params = mutableMapOf<String, String>()
        if (paramsStr.isNotEmpty()) {
            paramsStr.split(",").forEach { part ->
                val kv = part.split("=", limit = 2)
                if (kv.size == 2) {
                    params[kv[0].trim()] = kv[1].trim()
                }
            }
        }

        return AiAction(actionType, params)
    }

    private suspend fun executeAiAction(action: AiAction) {
        when (action.type) {
            "click" -> {
                val x = action.params["x"]?.toIntOrNull() ?: return
                val y = action.params["y"]?.toIntOrNull() ?: return
                bridge.tap(x, y)
            }
            "type" -> {
                val text = action.params["text"] ?: return
                bridge.typeText(text)
            }
            "swipe" -> {
                val x1 = action.params["x1"]?.toIntOrNull() ?: return
                val y1 = action.params["y1"]?.toIntOrNull() ?: return
                val x2 = action.params["x2"]?.toIntOrNull() ?: return
                val y2 = action.params["y2"]?.toIntOrNull() ?: return
                bridge.swipe(x1, y1, x2, y2)
            }
            "open_app" -> {
                val name = action.params["name"] ?: return
                bridge.openApp(name)
            }
            "finished" -> {
                Log.d(TAG, "AI signaled finished")
            }
        }
    }

    fun stop() {
        shouldStop = true
    }

    private fun reset() {
        shouldStop = false
        _currentStepIndex.value = -1
        _currentSkill.value = null
    }

    data class AiAction(val type: String, val params: Map<String, String>)
}