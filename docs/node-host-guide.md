# AgentClaw Node Host 开发指南

## 概述

AgentClaw Node Host 负责将手机节点连接到 OpenClaw Gateway，使 AI 能够通过 `nodes.invoke` 调用手机命令。

## 架构

```
OpenClaw AI
    ↓ nodes.invoke
OpenClaw Gateway (:18789)
    ↓ WebSocket
AgentClaw Node Host
    ↓ HTTP (:18790)
AgentClaw Bridge Server
    ↓ Socket (:28200)
TutuGui Server
```

## WebSocket 连接流程

### 1. 握手连接

```javascript
const ws = new WebSocket('ws://127.0.0.1:18789');
```

### 2. 接收挑战（可选）

Gateway 可能发送 `connect.challenge` 事件，Node 无需特殊响应。

### 3. 发送 connect 请求

```javascript
const connectReq = {
  type: 'req',
  id: '1',
  method: 'connect',
  params: {
    minProtocol: 3,
    maxProtocol: 3,
    client: {
      id: '<sha256-hex>',        // 必须：deviceId = SHA256(publicKey)
      displayName: 'AgentClaw',  // AI 引用节点的名字
      version: '1.0.0',
      platform: 'android',
      mode: 'node'
    },
    role: 'node',
    scopes: ['operator.admin'],
    caps: ['device', 'sms', 'call', 'audio', 'app'],
    commands: COMMANDS,          // 允许的命令列表
    auth: { signature, signedAt, nonce },
    device: { id, publicKey, signature, signedAt, nonce }
  }
};
ws.send(JSON.stringify(connectReq));
```

## 设备身份 (Ed25519)

### 生成密钥对

```javascript
const crypto = require('crypto');

// 生成私钥（32字节）
const privateKey = crypto.randomBytes(32);

// 从私钥导出公钥（简化版，实际需要 Ed25519 曲线运算）
const publicKey = derivePublicKey(privateKey); // 32字节

// deviceId = SHA256(publicKey)
const deviceId = crypto.createHash('sha256').update(publicKey).digest('hex');
```

### 签名 Payload

```javascript
// v3 格式
const payload = `v3|${deviceId}|node-host|node|node|operator.admin|${timestamp}||${nonce}|android|`;

// Ed25519 签名
const signature = ed25519.sign(Buffer.from(payload), privateKey);

// base64url 编码
const sigBase64Url = signature.toString('base64')
  .replaceAll('+', '-')
  .replaceAll('/', '_')
  .replace(/=+$/g, '');
```

## invoke 请求处理

### 接收

```javascript
ws.on('message', (data) => {
  const msg = JSON.parse(data);
  if (msg.type === 'event' && msg.event === 'node.invoke.request') {
    const { id, nodeId, command, paramsJSON, timeoutMs } = msg.payload;
    // 处理命令
  }
});
```

### 响应

```javascript
const result = {
  type: 'req',
  id: '2',
  method: 'node.invoke.result',
  params: {
    id: 'invoke-uuid',
    nodeId: '<sha256-hex>',  // 必须与注册时的 deviceId 一致
    ok: true,
    payloadJSON: JSON.stringify({ ok: true, action: 'tap' })
  }
};
ws.send(JSON.stringify(result));
```

## 命令映射

| Node 命令 | Bridge 端点 | HTTP 方法 |
|-----------|------------|-----------|
| `device.screenshot` | `/api/screenshot` | POST |
| `device.tap` | `/api/tap` | POST |
| `device.swipe` | `/api/swipe` | POST |
| `device.type` | `/api/type` | POST |
| `device.open_app` | `/api/open_app` | POST |
| `device.ui_tree` | `/api/get_ui_tree` | POST |
| `app.list` | `/api/list_packages` | POST |
| `sms.send` | `/api/send_sms` | POST |
| `call.make` | `/api/make_call` | POST |

## 错误处理

### 常见错误

| 错误码 | 说明 | 处理 |
|--------|------|------|
| `DEVICE_AUTH_DEVICE_ID_MISMATCH` | device.id 与公钥推导的不一致 | 检查 deviceId 生成逻辑 |
| `DEVICE_AUTH_SIGNATURE_INVALID` | 签名验证失败 | 检查签名 payload 格式 |
| `nodeId mismatch` | invoke result 的 nodeId 不匹配 | 确保返回的 nodeId 是 deviceId |

### 重连机制

```javascript
ws.on('close', () => {
  console.log('连接断开，5秒后重连...');
  setTimeout(() => connect(), 5000);
});
```

## 测试

```bash
# 启动 Node Host
node agentclaw-node-host.js

# 测试 Gateway 连接
curl -X POST http://127.0.0.1:18790/api/device_info
```