package com.phoneagentx.core

import android.content.Context
import android.util.Log
import com.phoneagentx.core.socket.TutuSocketClient
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.ServerSocket
import java.net.Socket

/**
 * Bridge Server 管理�? * HTTP (:18790) �?Socket (:28200) �?TutuGui Server
 */
class BridgeServerManager(
    private val context: Context,
    private val socketClient: TutuSocketClient
) {
    private val TAG = "BridgeServerManager"
    private val PORT = 18790
    private var serverSocket: ServerSocket? = null
    private var running = false

    // 变量存储
    private val variables = mutableMapOf<String, String>()

    // API 端点映射
    private val endpoints = mapOf(
        "/api/screenshot" to { p: JsonObject -> runBlocking { handleScreenshot(p) } },
        "/api/tap" to { p: JsonObject -> handleTap(p) },
        "/api/swipe" to { p: JsonObject -> handleSwipe(p) },
        "/api/type" to { p: JsonObject -> handleType(p) },
        "/api/open_app" to { p: JsonObject -> handleOpenApp(p) },
        "/api/get_ui_tree" to { p: JsonObject -> runBlocking { handleGetUiTree(p) } },
        "/api/device_info" to { p: JsonObject -> runBlocking { handleDeviceInfo(p) } },
        "/api/status" to { p: JsonObject -> handleStatus(p) },
        "/api/list_packages" to { p: JsonObject -> runBlocking { handleListPackages(p) } },
        "/api/force_stop_app" to { p: JsonObject -> runBlocking { handleForceStopApp(p) } },
        "/api/send_sms" to { p: JsonObject -> runBlocking { handleSendSms(p) } },
        "/api/read_sms" to { p: JsonObject -> runBlocking { handleReadSms(p) } },
        "/api/make_call" to { p: JsonObject -> handleMakeCall(p) },
        "/api/accept_call" to { p: JsonObject -> handleAcceptCall(p) },
        "/api/end_call" to { p: JsonObject -> handleEndCall(p) }
    )

    fun start() {
        if (running) return
        running = true

        Thread {
            try {
                serverSocket = ServerSocket(PORT)
                Log.i(TAG, "Bridge Server 启动于端�?$PORT")

                while (running) {
                    try {
                        val client = serverSocket!!.accept()
                        Thread { handleRequest(client) }.start()
                    } catch (e: Exception) {
                        if (running) Log.e(TAG, "接受连接失败: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Server 启动失败: ${e.message}")
            }
        }.start()
    }

    fun stop() {
        running = false
        serverSocket?.close()
        serverSocket = null
    }

    private fun handleRequest(client: Socket) {
        try {
            val reader = BufferedReader(InputStreamReader(client.getInputStream()))
            val writer = OutputStreamWriter(client.getOutputStream())

            val requestLine = reader.readLine() ?: return
            val parts = requestLine.split(" ")
            if (parts.size < 2) return

            val method = parts[0]
            val path = parts[1].split("?").first()

            var contentLength = 0
            var line: String?
            while (reader.readLine().also { line = it } != "") {
                if (line!!.lowercase().startsWith("content-length:")) {
                    contentLength = line!!.substringAfter("content-length:").trim().toIntOrNull() ?: 0
                }
            }

            val body = if (contentLength > 0) {
                val buf = CharArray(contentLength)
                reader.read(buf, 0, contentLength)
                String(buf)
            } else ""

            val handler = endpoints[path]
            val responseJson = if (handler != null) {
                try {
                    val params = if (body.isNotEmpty()) Json.parseToJsonElement(body).jsonObject else JsonObject(emptyMap())
                    handler(params)
                } catch (e: Exception) {
                    buildJsonObject { put("success", false); put("error", e.message.toString()) }
                }
            } else {
                buildJsonObject { put("success", false); put("error", "Unknown endpoint: $path") }
            }

            val responseStr = responseJson.toString()
            writer.write("HTTP/1.1 200 OK\r\n")
            writer.write("Content-Type: application/json\r\n")
            writer.write("Content-Length: ${responseStr.toByteArray().size}\r\n")
            writer.write("\r\n")
            writer.write(responseStr)
            writer.flush()

            client.close()
        } catch (e: Exception) {
            Log.e(TAG, "处理请求失败: ${e.message}")
        }
    }

    // ── API Handlers ──

    private suspend fun handleScreenshot(params: JsonObject): JsonObject {
        val reqId = socketClient.nextReqId()
        val cmd = buildJsonObject { put("type", "screenshot"); put("reqId", reqId); put("maxSize", 1080); put("quality", 80) }
        val resp = socketClient.sendAndWait(cmd, 15000)
        return resp ?: buildJsonObject { put("success", false); put("error", "截图超时") }
    }

    private fun handleTap(params: JsonObject): JsonObject {
        val x = params["x"]?.jsonPrimitive?.intOrNull ?: return error("x required")
        val y = params["y"]?.jsonPrimitive?.intOrNull ?: return error("y required")
        val screenSize = getScreenSize()
        socketClient.sendFireAndForget(buildJsonObject {
            put("type", "touch"); put("action", 0); put("x", x); put("y", y)
            put("screenWidth", screenSize.first); put("screenHeight", screenSize.second)
        })
        Thread.sleep(50)
        socketClient.sendFireAndForget(buildJsonObject {
            put("type", "touch"); put("action", 1); put("x", x); put("y", y)
            put("screenWidth", screenSize.first); put("screenHeight", screenSize.second)
        })
        return buildJsonObject { put("success", true); put("action", "tap") }
    }

    private fun handleSwipe(params: JsonObject): JsonObject {
        val x1 = params["x1"]?.jsonPrimitive?.intOrNull ?: return error("x1 required")
        val y1 = params["y1"]?.jsonPrimitive?.intOrNull ?: return error("y1 required")
        val x2 = params["x2"]?.jsonPrimitive?.intOrNull ?: return error("x2 required")
        val y2 = params["y2"]?.jsonPrimitive?.intOrNull ?: return error("y2 required")
        val durationMs = params["durationMs"]?.jsonPrimitive?.intOrNull ?: 300
        socketClient.sendFireAndForget(buildJsonObject {
            put("type", "swipe"); put("x1", x1); put("y1", y1); put("x2", x2); put("y2", y2); put("durationMs", durationMs)
        })
        return buildJsonObject { put("success", true) }
    }

    private fun handleType(params: JsonObject): JsonObject {
        val text = params["text"]?.jsonPrimitive?.contentOrNull ?: return error("text required")
        socketClient.sendFireAndForget(buildJsonObject { put("type", "text"); put("text", text) })
        return buildJsonObject { put("success", true) }
    }

    private fun handleOpenApp(params: JsonObject): JsonObject {
        val packageName = params["package"]?.jsonPrimitive?.contentOrNull ?: params["app_name"]?.jsonPrimitive?.contentOrNull ?: return error("package required")
        socketClient.sendFireAndForget(buildJsonObject { put("type", "start_app"); put("package", packageName) })
        return buildJsonObject { put("success", true) }
    }

    private suspend fun handleGetUiTree(params: JsonObject): JsonObject {
        socketClient.sendFireAndForget(buildJsonObject { put("type", "get_ui_nodes"); put("mode", 2) })
        val resp = socketClient.sendAndWait(buildJsonObject { put("type", "get_ui_nodes"); put("reqId", socketClient.nextReqId()); put("mode", 2) }, 15000)
        return resp ?: buildJsonObject { put("nodes", JsonArray(emptyList())) }
    }

    private suspend fun handleDeviceInfo(params: JsonObject): JsonObject {
        val reqId = socketClient.nextReqId()
        val cmd = buildJsonObject { put("type", "get_device_info"); put("reqId", reqId) }
        return socketClient.sendAndWait(cmd, 5000) ?: buildJsonObject { put("success", false) }
    }

    private fun handleStatus(params: JsonObject): JsonObject {
        return buildJsonObject { put("connected", socketClient.isConnected()); put("connectionState", socketClient.connectionState.value.name) }
    }

    private suspend fun handleListPackages(params: JsonObject): JsonObject {
        val reqId = socketClient.nextReqId()
        val cmd = buildJsonObject { put("type", "list_packages"); put("reqId", reqId); put("thirdPartyOnly", params["thirdPartyOnly"]?.jsonPrimitive?.booleanOrNull ?: true) }
        return socketClient.sendAndWait(cmd, 10000) ?: buildJsonObject { put("success", false) }
    }

    private suspend fun handleForceStopApp(params: JsonObject): JsonObject {
        val packageName = params["package"]?.jsonPrimitive?.contentOrNull ?: return error("package required")
        val reqId = socketClient.nextReqId()
        val cmd = buildJsonObject { put("type", "force_stop_app"); put("reqId", reqId); put("package", packageName) }
        return socketClient.sendAndWait(cmd, 5000) ?: buildJsonObject { put("success", false) }
    }

    private suspend fun handleSendSms(params: JsonObject): JsonObject {
        val destination = params["destination"]?.jsonPrimitive?.contentOrNull ?: return error("destination required")
        val text = params["text"]?.jsonPrimitive?.contentOrNull ?: return error("text required")
        val reqId = socketClient.nextReqId()
        val cmd = buildJsonObject { put("type", "send_sms"); put("reqId", reqId); put("destination", destination); put("text", text) }
        return socketClient.sendAndWait(cmd, 10000) ?: buildJsonObject { put("success", false) }
    }

    private suspend fun handleReadSms(params: JsonObject): JsonObject {
        val reqId = socketClient.nextReqId()
        val cmd = buildJsonObject { put("type", "read_sms"); put("reqId", reqId) }
        return socketClient.sendAndWait(cmd, 10000) ?: buildJsonObject { put("success", false) }
    }

    private fun handleMakeCall(params: JsonObject): JsonObject {
        val number = params["number"]?.jsonPrimitive?.contentOrNull ?: return error("number required")
        socketClient.sendFireAndForget(buildJsonObject { put("type", "make_call"); put("number", number) })
        return buildJsonObject { put("success", true) }
    }

    private fun handleAcceptCall(params: JsonObject): JsonObject {
        socketClient.sendFireAndForget(buildJsonObject { put("type", "accept_call") })
        return buildJsonObject { put("success", true) }
    }

    private fun handleEndCall(params: JsonObject): JsonObject {
        socketClient.sendFireAndForget(buildJsonObject { put("type", "end_call") })
        return buildJsonObject { put("success", true) }
    }

    private fun error(msg: String) = buildJsonObject { put("success", false); put("error", msg) }

    // ── 变量管理 ──

    fun setVariable(name: String, value: String) { variables[name] = value }
    fun getVariable(name: String): String? = variables[name]
    fun clearVariables() { variables.clear() }

    // ── 设备操作 ──

    fun tap(x: Int, y: Int) = handleTap(buildJsonObject { put("x", x); put("y", y) })
    fun swipe(x1: Int, y1: Int, x2: Int, y2: Int, durationMs: Int = 300) = handleSwipe(buildJsonObject { put("x1", x1); put("y1", y1); put("x2", x2); put("y2", y2); put("durationMs", durationMs) })
    fun typeText(text: String) = handleType(buildJsonObject { put("text", text) })
    fun openApp(name: String) = handleOpenApp(buildJsonObject { put("app_name", name) })
    fun pressHome() = socketClient.sendFireAndForget(buildJsonObject { put("type", "command"); put("cmd", "HOME") })
    fun pressBack() = socketClient.sendFireAndForget(buildJsonObject { put("type", "command"); put("cmd", "BACK") })
    fun scroll(direction: String) {
        val (w, h) = getScreenSize()
        val cx = w / 2
        val (x1, y1, x2, y2) = when (direction) {
            "down" -> listOf(cx, (h * 0.8).toInt(), cx, (h * 0.15).toInt())
            else -> listOf(cx, (h * 0.2).toInt(), cx, (h * 0.85).toInt())
        }
        handleSwipe(buildJsonObject { put("x1", x1); put("y1", y1); put("x2", x2); put("y2", y2); put("durationMs", 350) })
    }

    suspend fun takeScreenshot(): String? {
        val resp = handleScreenshot(buildJsonObject())
        return resp["data"]?.jsonPrimitive?.contentOrNull
    }

    suspend fun takeCompressedScreenshot(): String? {
        // TODO: 实现压缩
        return takeScreenshot()
    }

    fun openUrl(url: String) {
        // 通过 socket 命令打开 URL
        socketClient.sendFireAndForget(buildJsonObject { put("type", "open_url"); put("url", url) })
    }

    suspend fun subscribeAccessibilityEvents() {
        val cmd = buildJsonObject {
            put("type", "subscribe_accessibility_events")
            put("reqId", socketClient.nextReqId())
            putJsonArray("eventTypes") { add("notification_state_changed"); add("window_state_changed") }
            putJsonArray("packages") { add("com.tencent.mm") }
            put("debounceMs", 500)
        }
        socketClient.sendFireAndForget(cmd)
    }

    fun unsubscribeAccessibilityEvents() {
        socketClient.sendFireAndForget(buildJsonObject { put("type", "unsubscribe_accessibility_events"); put("reqId", socketClient.nextReqId()) })
    }

    private fun getScreenSize(): Pair<Int, Int> = Pair(1080, 1920) // TODO: 从设备获�?}