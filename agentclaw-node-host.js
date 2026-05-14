/**
 * AgentClaw Node Host
 * 
 * 连接 OpenClaw Gateway，作为手机节点注册到 AI 系统。
 * AI 通过 nodes.invoke 调用手机命令：截图、点击、滑动、安装应用等。
 * 
 * 架构：Node Host → Bridge Server (:18790) → Socket (:28200) → TutuGui
 */
const WebSocket = require('ws');
const crypto = require('crypto');
const https = require('https');
const http = require('http');

// ── 配置 ──
const GATEWAY_URL = process.env.OPENCLAW_GATEWAY_URL || 'ws://127.0.0.1:18789';
const BRIDGE_PORT = 18790;
const BRIDGE_HOST = '127.0.0.1';

// ── 设备身份（Ed25519）─
const DEVICE_PRIVATE_KEY = Buffer.from(process.env.AGENTCLAW_PRIVATE_KEY || '...', 'hex');
const DEVICE_PUBLIC_KEY = DEVICE_PRIVATE_KEY.slice(32); // 简化，实际需要从密钥提取

function deriveDeviceId(publicKey) {
    return crypto.createHash('sha256').update(publicKey).digest('hex');
}

function base64UrlEncode(buf) {
    return buf.toString('base64')
        .replaceAll('+', '-')
        .replaceAll('/', '_')
        .replace(/=+$/g, '');
}

function signPayload(payload) {
    const hmac = crypto.createHmac('sha256', DEVICE_PRIVATE_KEY);
    hmac.update(payload);
    return base64UrlEncode(hmac.digest());
}

// ── 设备命令映射 ──
const COMMANDS = [
    'device.screenshot', 'device.tap', 'device.long_click', 'device.swipe', 'device.scroll',
    'device.type', 'device.press_key', 'device.click_by_text', 'device.open_app',
    'device.ui_tree', 'device.find_element', 'device.read_ui_text', 'device.info',
    'device.status', 'device.shell',
    'app.list', 'app.info', 'app.stop', 'app.uninstall', 'app.install', 'app.clear_data',
    'sms.send', 'sms.read',
    'call.accept', 'call.end', 'call.make',
    'audio.open', 'audio.close'
];

// ── Node Host 核心 ──
class AgentClawNodeHost {
    constructor() {
        this.ws = null;
        this.deviceId = deriveDeviceId(DEVICE_PUBLIC_KEY);
        this.pendingRequests = new Map();
        this.reqIdCounter = 0;
        this.connected = false;
    }

    // 1. 连接到 Gateway
    async connect() {
        return new Promise((resolve, reject) => {
            console.log(`[AgentClaw] 连接 Gateway: ${GATEWAY_URL}`);
            
            this.ws = new WebSocket(GATEWAY_URL);

            this.ws.on('open', () => {
                console.log('[AgentClaw] WebSocket 已连接，发送 connect 请求...');
                this.sendConnectRequest();
            });

            this.ws.on('message', (data) => {
                const msg = JSON.parse(data);
                this.handleMessage(msg);
            });

            this.ws.on('close', () => {
                console.log('[AgentClaw] 连接断开，5秒后重连...');
                this.connected = false;
                setTimeout(() => this.connect(), 5000);
            });

            this.ws.on('error', (err) => {
                console.error('[AgentClaw] WebSocket 错误:', err.message);
                reject(err);
            });

            // 超时处理
            setTimeout(() => {
                if (!this.connected) reject(new Error('连接超时'));
            }, 10000);
        });
    }

    // 2. 发送 connect 请求
    sendConnectRequest() {
        const nonce = crypto.randomBytes(16).toString('hex');
        const timestamp = Date.now();

        // 签名 payload
        const authPayload = `v3|${this.deviceId}|node-host|node|node|operator.admin|${timestamp}||${nonce}|android|`;
        const signature = signPayload(authPayload);

        const connectReq = {
            type: 'req',
            id: String(++this.reqIdCounter),
            method: 'connect',
            params: {
                minProtocol: 3,
                maxProtocol: 3,
                client: {
                    id: this.deviceId,
                    displayName: 'AgentClaw',
                    version: '1.0.0',
                    platform: 'android',
                    mode: 'node',
                    deviceFamily: 'android'
                },
                role: 'node',
                scopes: ['operator.admin'],
                caps: ['device', 'sms', 'call', 'audio', 'app'],
                commands: COMMANDS,
                auth: {
                    token: '',
                    signature: signature,
                    signedAt: timestamp,
                    nonce: nonce
                },
                device: {
                    id: this.deviceId,
                    publicKey: base64UrlEncode(DEVICE_PUBLIC_KEY),
                    signature: signature,
                    signedAt: timestamp,
                    nonce: nonce
                }
            }
        };

        this.ws.send(JSON.stringify(connectReq));
    }

    // 3. 处理 Gateway 消息
    handleMessage(msg) {
        console.log('[AgentClaw] 收到消息:', msg.type);

        if (msg.type === 'event' && msg.event === 'connect.challenge') {
            // 收到挑战，无需响应，connect 请求已包含所有认证信息
        }
        else if (msg.type === 'req' && msg.method === 'node.invoke.request') {
            this.handleInvokeRequest(msg);
        }
        else if (msg.type === 'event' && msg.event === 'node.invoke.result') {
            // 远程调用的结果（不常见）
        }
        else if (msg.type === 'req' && msg.method === 'node.invoke.result') {
            // 我们的 invoke 结果被 Gateway 确认
        }
    }

    // 4. 处理 invoke 请求（AI 调用命令）
    async handleInvokeRequest(req) {
        const { id, nodeId, command, paramsJSON, timeoutMs } = req.payload;
        console.log(`[AgentClaw] invoke: ${command} params=${paramsJSON}`);

        try {
            const params = JSON.parse(paramsJSON || '{}');
            const result = await this.executeCommand(command, params);
            
            this.sendInvokeResult({
                id,
                nodeId: this.deviceId,
                ok: true,
                payloadJSON: JSON.stringify(result)
            });
        } catch (err) {
            console.error(`[AgentClaw] 命令执行失败: ${command}`, err);
            this.sendInvokeResult({
                id,
                nodeId: this.deviceId,
                ok: false,
                payloadJSON: JSON.stringify({ error: err.message })
            });
        }
    }

    sendInvokeResult(result) {
        const msg = {
            type: 'req',
            id: String(++this.reqIdCounter),
            method: 'node.invoke.result',
            params: result
        };
        this.ws.send(JSON.stringify(msg));
    }

    // 5. 执行设备命令
    async executeCommand(command, params) {
        const [category, action] = command.split('.');

        // 转发到 Bridge Server
        const response = await this.httpPost(`/api/${action}`, params);
        return response;
    }

    // 6. HTTP 转发到 Bridge Server
    httpPost(path, data) {
        return new Promise((resolve, reject) => {
            const postData = JSON.stringify(data);
            const options = {
                hostname: BRIDGE_HOST,
                port: BRIDGE_PORT,
                path: path,
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Content-Length': Buffer.byteLength(postData)
                }
            };

            const req = http.request(options, (res) => {
                let body = '';
                res.on('data', chunk => body += chunk);
                res.on('end', () => {
                    try { resolve(JSON.parse(body)); }
                    catch { resolve(body); }
                });
            });

            req.on('error', reject);
            req.write(postData);
            req.end();
        });
    }
}

// ── 启动 ──
const nodeHost = new AgentClawNodeHost();
nodeHost.connect()
    .then(() => {
        console.log('[AgentClaw] ✅ 已连接到 OpenClaw Gateway');
    })
    .catch((err) => {
        console.error('[AgentClaw] ❌ 连接失败:', err.message);
        process.exit(1);
    });