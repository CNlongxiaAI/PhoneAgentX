package com.phoneagentx.core.socket

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap

enum class ConnectionState {
    DISCONNECTED, CONNECTING, CONNECTED, AUTHENTICATED, ERROR
}

class TutuSocketClient(
    private val host: String = "127.0.0.1",
    private val port: Int = 28200
) {
    private val TAG = "TutuSocketClient"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true }

    private var socket: Socket? = null
    private var reader: BufferedReader? = null
    private var outputStream: OutputStream? = null
    private var readJob: Job? = null

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _messages = MutableSharedFlow<JsonObject>(extraBufferCapacity = 64)
    val messages: SharedFlow<JsonObject> = _messages.asSharedFlow()

    private val pendingRequests = ConcurrentHashMap<String, CompletableDeferred<JsonObject>>()
    private val reqIdCounter = java.util.concurrent.atomic.AtomicLong(0)

    fun nextReqId(): String = "ac-${reqIdCounter.incrementAndGet()}"

    fun connect(token: String? = null, force: Boolean = false) {
        Log.d(TAG, "connect() state=${_connectionState.value} force=$force")

        if (_connectionState.value != ConnectionState.DISCONNECTED) {
            if (force) disconnect() else return
        }

        _connectionState.value = ConnectionState.CONNECTING

        scope.launch {
            try {
                socket = Socket(host, port)
                reader = BufferedReader(InputStreamReader(socket!!.getInputStream(), "UTF-8"))
                outputStream = socket!!.getOutputStream()

                _connectionState.value = ConnectionState.CONNECTED
                Log.i(TAG, "Socket connected")

                startReading()

                if (!token.isNullOrEmpty()) {
                    authenticate(token)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Connection failed: ${e.message}")
                _connectionState.value = ConnectionState.ERROR
            }
        }
    }

    private fun startReading() {
        readJob?.cancel()
        readJob = scope.launch {
            try {
                while (isActive) {
                    val line = reader?.readLine() ?: break
                    if (line.isEmpty()) continue

                    try {
                        val msg = json.parseToJsonElement(line).jsonObject
                        handleMessage(msg)
                    } catch (e: Exception) {
                        Log.w(TAG, "Parse failed: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Read loop ended: ${e.message}")
                _connectionState.value = ConnectionState.DISCONNECTED
            }
        }
    }

    private fun handleMessage(msg: JsonObject) {
        val type = msg["type"]?.jsonPrimitive?.contentOrNull ?: return
        Log.d(TAG, "Received: $type")

        val reqId = msg["reqId"]?.jsonPrimitive?.contentOrNull
        if (reqId != null && pendingRequests.containsKey(reqId)) {
            val deferred = pendingRequests.remove(reqId)
            deferred?.complete(msg)
            return
        }

        scope.launch { _messages.emit(msg) }
    }

    suspend fun sendAndWait(cmd: JsonObject, timeoutMs: Long = 15000): JsonObject? {
        val reqId = cmd["reqId"]?.jsonPrimitive?.contentOrNull ?: nextReqId()
        val cmdWithId = buildJsonObject {
            cmd.forEach { (k, v) -> put(k, v) }
            put("reqId", reqId)
        }

        val deferred = CompletableDeferred<JsonObject>()
        pendingRequests[reqId] = deferred

        sendFireAndForget(cmdWithId)

        return try {
            withTimeoutOrNull(timeoutMs) { deferred.await() }
        } catch (e: Exception) {
            Log.w(TAG, "Request timeout: $reqId")
            pendingRequests.remove(reqId)
            null
        }
    }

    fun sendFireAndForget(cmd: JsonObject) {
        try {
            val line = cmd.toString() + "\n"
            outputStream?.write(line.toByteArray(charset("UTF-8")))
            outputStream?.flush()
        } catch (e: Exception) {
            Log.e(TAG, "Send failed: ${e.message}")
        }
    }

    private suspend fun authenticate(token: String) {
        val resp = sendAndWait(buildJsonObject {
            put("type", "authenticate")
            put("token", token)
            put("reqId", nextReqId())
        })
        if (resp?.get("success")?.jsonPrimitive?.booleanOrNull == true) {
            _connectionState.value = ConnectionState.AUTHENTICATED
            Log.i(TAG, "Authenticated")
        }
    }

    fun disconnect() {
        readJob?.cancel()
        readJob = null
        try { socket?.close() } catch (e: Exception) { }
        socket = null
        reader = null
        outputStream = null
        _connectionState.value = ConnectionState.DISCONNECTED
        pendingRequests.clear()
    }

    fun isConnected() = _connectionState.value == ConnectionState.CONNECTED ||
                         _connectionState.value == ConnectionState.AUTHENTICATED

    fun hasPending(reqId: String) = pendingRequests.containsKey(reqId)
}