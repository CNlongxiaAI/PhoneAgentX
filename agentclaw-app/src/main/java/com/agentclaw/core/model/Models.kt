package com.agentclaw.core.model

import kotlinx.serialization.Serializable

@Serializable
data class SkillItem(
    val id: Int,
    val slug: String,
    val displayName: String,
    val displayNameEn: String = "",
    val description: String = "",
    val icon: String = "",
    val category: String = "",
    val version: String = "",
    val tags: List<String> = emptyList(),
    val estimatedTime: String = "",
    val estimatedTokens: Int = 0
)

@Serializable
data class SkillStep(
    val id: String = "",
    val type: String = "",
    val action: String = "",
    val params: Map<String, String> = emptyMap(),
    val prompt: String = "",
    val saveAs: String = "",
    val maxLoops: Int = 8,
    val branches: List<StepBranch> = emptyList(),
    val defaultBranch: StepBranch? = null,
    val output: String = "",
    val label: String = "",
    val duration: Long = 0,
    val timeout: Long = 0,
    val stableMs: Long = 600,
    val varName: String = "",
    val op: String = "assign",
    val value: String = "",
    val expression: String = "",
    val goto: String = "",
    val title: String = "",
    val fields: List<StepField> = emptyList(),
    val timeoutAction: String = "use_default",
    val loopSteps: List<SkillStep> = emptyList(),
    val maxIterations: Int = 50,
    val maxDuration: Long = 600000,
    val eventTypes: List<String> = emptyList(),
    val x: Int = 0,
    val y: Int = 0,
    val x1: Int = 0,
    val y1: Int = 0,
    val x2: Int = 0,
    val y2: Int = 0,
    val durationMs: Int = 300,
    val text: String = "",
    val url: String = ""
)

@Serializable
data class StepBranch(
    val match: String = "",
    val goto: String = "next",
    val label: String = ""
)

@Serializable
data class StepField(
    val key: String = "",
    val label: String = "",
    val type: String = "text",
    val default: String = "",
    val options: List<String> = emptyList(),
    val placeholder: String = ""
)

enum class SkillCategory(val key: String, val labelZh: String) {
    ALL("", "全部"),
    SOCIAL("social", "社交"),
    ENTERTAINMENT("entertainment", "娱乐"),
    DAILY("daily", "日常"),
    SHOPPING("shopping", "购物"),
    TOOLS("tools", "工具")
}