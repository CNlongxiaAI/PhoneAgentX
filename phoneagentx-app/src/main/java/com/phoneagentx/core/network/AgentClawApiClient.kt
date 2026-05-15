package com.phoneagentx.core.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class PhoneAgentXApiClient {
    private val TAG = "PhoneAgentXApiClient"
    private val BASE_URL = "https://api.PhoneAgentX.dev"

    private val json = Json { ignoreUnknownKeys = true }

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
            Log.e(TAG, "Failed to get skill list: ${e.message}")
            emptyList()
        }
    }

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
            Log.e(TAG, "Failed to get skill detail: ${e.message}")
            null
        }
    }

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

                conn.responseCode
                conn.disconnect()
            } catch (e: Exception) {
                // Silent failure
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