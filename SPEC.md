# AgentClaw - 手机 AI 自动化节点

> **让 OpenClaw AI 在手机上长出手和脚**

---

## 一、项目定位

**AgentClaw** 是 OpenClaw 的手机端节点运行时。它让 OpenClaw AI 能够：

- 📱 **「看见」** 手机屏幕（截图 + UI 元素树）
- 👆 **「操作」** 手机（点击/滑动/输入/安装应用）
- ⚡ **「执行」** 任何 Skill 自动化任务

核心设计：
- **系统级 ADB 协议** — 开发者 USB 权限，等同真实手指，应用无法检测
- **内置完整 OpenClaw** — 一键安装，无需用户折腾环境
- **声明式 Skill 引擎** — JSON 定义手机自动化，无需写代码

---

## 二、架构设计

```
OpenClaw AI (PC/NAS/云)
    ↓ nodes.invoke
OpenClaw Gateway (:18789)
    ↓ WebSocket (node.invoke.request)
AgentClaw Node Host
    ↓ HTTP (:18790)
AgentClaw Bridge Server (Android)
    ↓ Socket (:28200)
Device Control Server
    ↓
手机硬件（触摸/滑动/截图/UI树）
```

---

## 三、核心能力

### 3.1 系统级 ADB（开发者 USB 权限）

```
开发者选项 → 无线调试 → ADB 配对 → 系统级控制
```

- **不可被检测**：ADB 协议在系统层，应用无法区分真实手指和 AI 操作
- **稳定可靠**：一次配对永久生效，不受无障碍服务被回收的影响
- **全系统覆盖**：游戏/Canvas/WebView 都能操作，无死角

### 3.2 OpenClaw Node Host

```javascript
// AgentClaw Node Host 核心逻辑
class AgentClawNodeHost {
    async connect() {
        // 1. Ed25519 设备签名
        // 2. WebSocket 连接到 Gateway
        // 3. 注册 28 个设备命令
    }

    async onInvoke(command, params) {
        // device.screenshot / device.tap / device.swipe / sms.send / etc.
    }
}
```

**支持 28 个命令：**

| 分类 | 命令 |
|------|------|
| 设备 | `device.screenshot`, `device.tap`, `device.swipe`, `device.scroll`, `device.type` |
| 应用 | `app.list`, `app.info`, `app.stop`, `app.install`, `app.clear_data` |
| 通信 | `sms.send`, `sms.read`, `call.make`, `call.accept`, `call.end` |
| 音频 | `audio.open`, `audio.close` |

### 3.3 Skill 引擎

**声明式 JSON Skill，手机端执行。**

```json
{
  "name": "wechat-auto-reply",
  "display_name": "微信托管自动回复",
  "steps": [
    { "id": "open_wechat", "type": "api", "action": "open_app" },
    { "id": "check_state", "type": "ai_check", "prompt": "判断是否在微信主界面" },
    { "id": "subscribe", "type": "api", "action": "subscribe_events" },
    { "id": "auto_reply_loop", "type": "loop", "loop_steps": [
      { "id": "wait_notify", "type": "wait_for_event" },
      { "id": "reply", "type": "ai_act", "prompt": "回复微信消息" }
    ]},
    { "id": "summary", "type": "ai_summary", "prompt": "生成托管报告" }
  ]
}
```

---

## 四、项目结构

```
agentclaw/
├── agentclaw-node-host.js     # Node Host 主脚本（连接 Gateway）
├── agentclaw-bridge-server.kt # Bridge Server（HTTP → Socket）
├── agentclaw-app/            # Android App 源码
│   ├── app/src/main/java/com/agentclaw/
│   │   ├── core/
│   │   │   ├── engine/       # Skill Engine
│   │   │   ├── socket/      # Socket 客户端
│   │   │   ├── bridge/      # Bridge Server
│   │   │   └── model/       # 数据模型
│   │   └── feature/
│   │       └── home/        # 主界面
├── skills/                   # 开源 Skill 库
├── docs/
│   ├── node-host-guide.md   # Node Host 开发指南
│   └── skill-dev-guide.md   # Skill 开发文档
├── SPEC.md
└── README.md
```

---

## 五、技术栈

| 组件 | 技术 |
|------|------|
| Android App | Kotlin + Jetpack Compose + Material 3 |
| ADB 协议 | 自实现 ADB v2（TLS + SPAKE2 配对）|
| Native | CMake + C++（BoringSSL SPAKE2）|
| Socket | JSON-line 协议（端口 28200）|
| Node Host | Node.js（连接 OpenClaw Gateway）|
| Skill 格式 | JSON（15+ 步骤类型）|
| 内置 OpenClaw | 2026.3.24 版本 |

---

## 六、Skill 市场（开源）

| 分类 | 技能 |
|------|------|
| 社交 | 微信托管自动回复、朋友圈自动点赞 |
| 娱乐 | 刷抖音、刷小红书 |
| 日常 | 查看天气、每日新闻、设闹钟 |
| 工具 | 屏幕翻译、照片清理 |

**开源协议：** 鼓励社区贡献 Skill Pull Request

---

## 七、Roadmap

- [x] Phase 1: AgentClaw Node Host（连接 OpenClaw Gateway）
- [x] Phase 2: Android App（ADB 配对 + Bridge Server）
- [x] Phase 3: Skill Engine（15+ 步骤类型）
- [ ] Phase 4: 内置 OpenClaw 运行时
- [ ] Phase 5: Skill 市场 + 开源 Skill 库
- [ ] Phase 6: 多手机同时控制

---

## 八、设计原则

1. **系统级控制** — ADB 权限，透明不可检测
2. **一键安装** — 不需要用户折腾 Termux / git clone
3. **模块化** — Node Host 可以独立运行，不依赖 App UI
4. **开源优先** — Skill 库完全开源，社区共建
5. **OpenClaw Native** — 作为 Node 接入，而不是独立 AI

---

**愿景：** 让 AI 在手机上真正拥有手和脚，任何人都能创建和分享手机自动化技能。

---

## 开源协议

**GPL v3.0** — 允许开源、使用、修改、分发

Copyright (C) 2025 阿龙 / Long