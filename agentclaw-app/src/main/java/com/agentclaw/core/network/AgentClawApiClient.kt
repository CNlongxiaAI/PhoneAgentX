package com.agentclaw.core.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class AgentClawApiClient {
    private val TAG = "AgentClawApiClient"
    private val BASE_URL = "https://api.agentclaw.dev" // 预留 API 端点

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * 获取 Skill 列表
     */
    suspend fun getSkillList(): List<SkillItem> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/skills")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 10000
            conn.readTimeout = 10000

            if (conn.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val response = reader.readText()
                reader.close()

                val json = Json.parseToJsonElement(response).jsonObject
                val list = json["list"]?.jsonArray ?: return@withContext emptyList()

                list.mapNotNull { item ->
                    try {
                        val obj = item.jsonObject
                        SkillItem(
                            id = obj["id"]?.jsonPrimitive?.intOrNull ?: 0,
                            slug = obj["slug"]?.jsonPrimitive?.contentOrNull ?: "",
                            displayName = obj["display_name"]?.jsonPrimitive?.contentOrNull ?: "",
                            description = obj["description"]?.jsonPrimitive?.contentOrNull ?: "",
                            icon = obj["icon"]?.jsonPrimitive?.contentOrNull ?: "star",
                            category = obj["category"]?.jsonPrimitive?.contentOrNull ?: ""
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取 Skill 列表失败: ${e.message}")
            emptyList()
        }
    }

    /**
     * 获取 Skill 详情
     */
    suspend fun getSkillDetail(slug: String): SkillDetail? = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/skills/$slug")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 10000
            conn.readTimeout = 10000

            if (conn.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val response = reader.readText()
                reader.close()

                val json = Json.parseToJsonElement(response).jsonObject
                val data = json["data"]?.jsonObject ?: return@withContext null

                SkillDetail(
                    slug = data["slug"]?.jsonPrimitive?.contentOrNull ?: "",
                    displayName = data["display_name"]?.jsonPrimitive?.contentOrNull ?: "",
                    description = data["description"]?.jsonPrimitive?.contentOrNull ?: "",
                    version = data["version"]?.jsonPrimitive?.contentOrNull ?: "",
                    steps = data["steps"]?.jsonArray?.map { it.jsonObject } ?: emptyList()
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取 Skill 详情失败: ${e.message}")
            null
        }
    }

    /**
     * 报告 Skill 执行结果（匿名统计）
     */
    suspend fun reportSkillResult(slug: String, success: Boolean, durationMs: Long) {
        withContext(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/stats")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.connectTimeout = 5000
                conn.readTimeout = 5000

                val body = """{"slug":"$slug","success":$success,"duration_ms":$durationMs}"""
                conn.outputStream.write(body.toByteArray())
                conn.outputStream.flush()

                conn.responseCode // 等待响应
                conn.disconnect()
            } catch (e: Exception) {
                // 静默失败，不影响主流程
            }
        }
    }

    data class SkillItem(
        val id: Int,
        val slug: String,
        val displayName: String,
        val description: String,
        val icon: String,
        val category: String
    )

    data class SkillDetail(
        val slug: String,
        val displayName: String,
        val description: String,
        val version: String,
        val steps: List<JsonObject>
    )
}