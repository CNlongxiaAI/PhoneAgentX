package com.phoneagentx.core.engine

import android.util.Log
import com.phoneagentx.core.model.SkillStep
import com.phoneagentx.core.network.PhoneAgentXApiClient
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*

/**
 * PhoneAgentX Skill 执行引擎
 * 声明�?JSON 自动化引擎，15+ 步骤类型
 */
class SkillEngine(
    private val bridge: BridgeServerManager,
    private val apiClient: PhoneAgentXApiClient,
    private val aiProviderFactory: () -> AiProvider?
) {
    private val TAG = "SkillEngine"

    // ── 状�?──
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

    // ── 执行入口 ──

    suspend fun runSkill(skillJson: JsonObject): RunResult {
        reset()
        _state.value = EngineState.LOADING

        val steps = skillJson["steps"]?.jsonArray
        if (steps.isNullOrEmpty()) {
            _state.value = EngineState.ERROR
            return RunResult("failed", 0, 0, "Skill 没有步骤")
        }

        _currentSkill.value = skillJson
        _state.value = EngineState.RUNNING
        shouldStop = false

        var completed = 0
        for (i in steps.indices) {
            if (shouldStop) break

            _currentStepIndex.value = i
            val step = steps[i].jsonObject

            try {
                executeStep(step)
                completed++
            } catch (e: Exception) {
                Log.e(TAG, "步骤 ${step["id"]} 执行失败: ${e.message}")
                // 继续执行或停止取决于 step 配置
                if (step["on_fail"] != null) {
                    // 处理失败路由
                } else {
                    _state.value = EngineState.ERROR
                    return RunResult("error", completed, steps.size, e.message)
                }
            }
        }

        val status = if (shouldStop) "stopped" else "finished"
        _state.value = if (shouldStop) EngineState.STOPPED else EngineState.FINISHED

        return RunResult(status, completed, steps.size)
    }

    suspend fun runInstruction(instruction: String): RunResult {
        // 创建合成 Skill 用于自然语言指令
        val syntheticSkill = buildJsonObject {
            put("name", "_instruction")
            put("steps", buildJsonArray {
                add(buildJsonObject {
                    put("id", "user_instruction")
                    put("type", "ai_act")
                    put("prompt", instruction)
                    put("max_loops", 10)
                })
            })
        }
        return runSkill(syntheticSkill)
    }

    // ── 步骤执行 ──

    private suspend fun executeStep(step: JsonObject) {
        val stepType = step["type"]?.jsonPrimitive?.contentOrNull ?: return
        val stepId = step["id"]?.jsonPrimitive?.contentOrNull ?: "unknown"
        Log.d(TAG, "执行步骤: $stepId ($stepType)")

        when (stepType) {
            "api" -> executeApiStep(step)
            "ai_check" -> executeAiCheckStep(step)
            "ai_act" -> executeAiActStep(step)
            "ai_summary" -> executeAiSummaryStep(step)
            "click" -> executeClickStep(step)
            "type" -> executeTypeStep(step)
            "swipe" -> executeSwipeStep(step)
            "wait" -> executeWaitStep(step)
            "wait_until_changed" -> executeWaitUntilChanged(step)
            "loop" -> executeLoopStep(step)
            "condition" -> executeConditionStep(step)
            "set_var" -> executeSetVarStep(step)
            "prompt_user" -> executePromptUserStep(step)
            "open_url" -> executeOpenUrlStep(step)
            else -> Log.w(TAG, "未知步骤类型: $stepType")
        }
    }

    // ── 步骤类型实现 ──

    private suspend fun executeApiStep(step: JsonObject) {
        val action = step["action"]?.jsonPrimitive?.contentOrNull ?: return
        val params = step["params"]?.jsonObject ?: JsonObject(emptyMap())

        when (action) {
            "open_app" -> bridge.openApp(params["app_name"]?.jsonPrimitive?.contentOrNull ?: "")
            "press_home" -> bridge.pressHome()
            "press_back" -> bridge.pressBack()
            "screenshot" -> bridge.takeScreenshot()
            "subscribe_events" -> bridge.subscribeAccessibilityEvents()
            "unsubscribe_events" -> bridge.unsubscribeAccessibilityEvents()
            "scroll" -> bridge.scroll(params["direction"]?.jsonPrimitive?.contentOrNull ?: "down")
            else -> Log.w(TAG, "未知 API action: $action")
        }
    }

    private suspend fun executeAiCheckStep(step: JsonObject) {
        val prompt = step["prompt"]?.jsonPrimitive?.contentOrNull ?: return
        val branches = step["branches"]?.jsonArray
        val defaultBranch = step["default_branch"]?.jsonObject

        // 截图发给 AI 分析
        val screenshot = bridge.takeCompressedScreenshot()
        val ai = aiProviderFactory() ?: return

        val result = ai.analyze(prompt = prompt, screenshotBase64 = screenshot)

        // 匹配分支
        var matched = false
        if (branches != null) {
            for (branch in branches) {
                val matchKeyword = branch.jsonObject["match"]?.jsonPrimitive?.contentOrNull ?: ""
                if (result.contains(matchKeyword, ignoreCase = true)) {
                    Log.d(TAG, "分支匹配: ${branch.jsonObject["goto"]}")
                    matched = true
                    // TODO: 实现 goto 跳转
                    break
                }
            }
        }

        if (!matched && defaultBranch != null) {
            Log.d(TAG, "使用默认分支: ${defaultBranch["goto"]}")
        }
    }

    private suspend fun executeAiActStep(step: JsonObject) {
        val prompt = step["prompt"]?.jsonPrimitive?.contentOrNull ?: return
        val maxLoops = step["max_loops"]?.jsonPrimitive?.intOrNull ?: 8

        val ai = aiProviderFactory() ?: return

        repeat(maxLoops) { loop ->
            if (shouldStop) return@repeat

            val screenshot = bridge.takeCompressedScreenshot()
            val result = ai.analyze(prompt = prompt, screenshotBase64 = screenshot)

            // 解析 AI 动作并执�?            val action = parseAiAction(result)
            if (action != null) {
                executeAiAction(action)
            }

            // 检查是否完�?            if (result.contains("finished", ignoreCase = true)) {
                return
            }
        }
    }

    private suspend fun executeAiSummaryStep(step: JsonObject) {
        val prompt = step["prompt"]?.jsonPrimitive?.contentOrNull ?: return
        val output = step["output"]?.jsonPrimitive?.contentOrNull ?: "result"

        val ai = aiProviderFactory() ?: return
        val result = ai.analyze(prompt = prompt)

        // 保存到变量（通过 bridge�?        bridge.setVariable(output, result)
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

    private suspend fun executeWaitUntilChanged(step: JsonObject) {
        val timeout = step["timeout"]?.jsonPrimitive?.longOrNull ?: 30000
        val stableMs = step["stable_ms"]?.jsonPrimitive?.longOrNull ?: 600

        // 简单实现：等待固定时间
        delay(minOf(timeout, 5000))
    }

    private suspend fun executeLoopStep(step: JsonObject) {
        val loopSteps = step["loop_steps"]?.jsonArray ?: return
        val maxIterations = step["max_iterations"]?.jsonPrimitive?.intOrNull ?: 50
        val maxDuration = step["max_duration"]?.jsonPrimitive?.longOrNull ?: 600000

        val startTime = System.currentTimeMillis()

        repeat(maxIterations) { i ->
            if (shouldStop) return@repeat
            if (System.currentTimeMillis() - startTime > maxDuration) {
                Log.d(TAG, "Loop 超时退�?)
                return@repeat
            }

            for (loopStep in loopSteps) {
                if (shouldStop) break
                executeStep(loopStep.jsonObject)
            }
        }
    }

    private fun executeConditionStep(step: JsonObject) {
        // 条件分支：检查变量决定跳�?        val expression = step["expression"]?.jsonPrimitive?.contentOrNull ?: ""
        val goto = step["goto"]?.jsonPrimitive?.contentOrNull ?: "next"

        // TODO: 实现表达式求值和跳转
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
        val title = step["title"]?.jsonPrimitive?.contentOrNull ?: "等待输入"
        val timeout = step["timeout"]?.jsonPrimitive?.longOrNull ?: 60000

        // TODO: 发送通知等待用户交互
        // 简化：直接继续执行
        delay(1000)
    }

    private fun executeOpenUrlStep(step: JsonObject) {
        val url = step["url"]?.jsonPrimitive?.contentOrNull ?: return
        bridge.openUrl(url)
    }

    // ── AI 动作解析 ──

    private fun parseAiAction(result: String): AiAction? {
        // 解析 Thought: ... Action: ... 格式
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
                Log.d(TAG, "AI 标记任务完成")
            }
            else -> Log.w(TAG, "未知 AI 动作: ${action.type}")
        }
    }

    // ── 控制 ──

    fun stop() {
        shouldStop = true
        _state.value = EngineState.STOPPED
    }

    fun reset() {
        shouldStop = false
        _state.value = EngineState.IDLE
        _currentStepIndex.value = -1
        bridge.clearVariables()
    }

    data class AiAction(val type: String, val params: Map<String, String>)
}