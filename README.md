# PhoneAgentX

<p align="center">
  <img src="docs/assets/logo_banner.png" alt="PhoneAgentX" width="560" onerror="this.style.display='none'" />
</p>

<p align="center">
  <strong>让 AI 控制你的手机跑自动化任务</strong>
</p>

<p align="center">
  <a href="https://github.com/longxiashouji/PhoneAgentX">GitHub</a>
  &nbsp;·&nbsp;
  <a href="SPEC.md">项目规格</a>
  &nbsp;·&nbsp;
  <a href="docs/node-host-guide.md">Node Host 开发指南</a>
  &nbsp;·&nbsp;
  <a href="docs/skill-dev-guide.md">Skill 开发指南</a>
  &nbsp;·&nbsp;
  <a href="skills/">Skill 库</a>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/License-GPL%20v3-blue.svg" alt="License" />
  <img src="https://img.shields.io/badge/Android-9%2B-green.svg" alt="Android" />
  <img src="https://img.shields.io/badge/Kotlin-1.9.22-purple.svg" alt="Kotlin" />
  <img src="https://img.shields.io/badge/Jetpack%20Compose-Material%203-blue.svg" alt="Compose" />
  <a href="https://github.com/longxiashouji/PhoneAgentX/stargazers">
    <img src="https://img.shields.io/github/stars/longxiashouji/PhoneAgentX?style=social" alt="Stars" />
  </a>
  <a href="https://github.com/longxiashouji/PhoneAgentX/network/members">
    <img src="https://img.shields.io/github/forks/longxiashouji/PhoneAgentX?style=social" alt="Forks" />
  </a>
  <a href="https://github.com/longxiashouji/PhoneAgentX/issues">
    <img src="https://img.shields.io/github/issues/longxiashouji/PhoneAgentX" alt="Issues" />
  </a>
</p>

---

## 目录

- [这是什么？](#这是什么)
- [核心原理](#核心原理)
- [工作流程](#工作流程)
- [快速开始](#快速开始)
- [Skill 生态](#skill-生态)
- [技术架构](#技术架构)
- [项目结构](#项目结构)
- [常见问题](#常见问题)
- [技术栈](#技术栈)
- [致谢](#致谢)
- [作者](#作者)

---

## 这是什么？

**PhoneAgentX** 是 OpenClaw 的手机端节点运行时。它让 AI 能够像真人一样操作你的手机：

| AI 能做的事 | 说明 |
|-------------|------|
| 📸 看见屏幕 | 截图、分析界面、理解内容 |
| 👆 操作手机 | 点击、滑动、输入、打开应用 |
| 📱 控制应用 | 发微信、发短信、打电话 |
| ⚡ 自动化任务 | 用 JSON 定义复杂流程，全自动执行 |

### 一句话理解

你对 AI 说"帮我打开微信，给朋友发消息：明天吃饭"，PhoneAgentX 让 AI 真正做到。

---

## 核心原理

### 为什么不用无障碍服务？

大多数手机自动化工具（Auto.js、Hamibot 等）用 **无障碍服务（AccessibilityService）**，但有三个致命问题：

| 问题 | 后果 |
|------|------|
| ❌ 会被检测 | 微信、抖音、淘宝会识别无障碍服务 → 封号或风控 |
| ❌ 不稳定 | 系统更新后经常失效、不同手机表现不同 |
| ❌ 有盲区 | 游戏、Canvas、WebView 无法操作 |

### PhoneAgentX 怎么做的？

用 **系统级 ADB 协议**，这是 Android 官方的开发者工具：

- ✅ 完全无法检测 — ADB 就是真人在操作，应用分不清
- ✅ 永久稳定 — 一次配对，永远可用，不受系统更新影响
- ✅ 无任何盲区 — 游戏、短视频、任何界面都能操作

### 原理对比

```
无障碍服务方案：
  应用层操作 → 容易被检测 → 可能封号 ❌

PhoneAgentX 方案：
  系统层 ADB → 等同真实手指 → 完全安全 ✅
```

---

## 工作流程

```
┌─────────────────────────────────────────────────────────────┐
│                        完整数据流                            │
└─────────────────────────────────────────────────────────────┘

  1️⃣ 你对 AI 说话
         ↓
  2️⃣ OpenClaw AI 思考决策
         ↓
  3️⃣ Gateway 收到 nodes.invoke 请求
         ↓
  4️⃣ PhoneAgentX Node Host 接收命令
         ↓
  5️⃣ Bridge Server 转发到手机
         ↓
  6️⃣ 手机执行：触摸 / 滑动 / 截图 / 输入
```

### 详细架构

```
┌────────────────┐      nodes.invoke       ┌─────────────────┐
│  OpenClaw AI   │ ──────────────────────▶  │  OpenClaw       │
│  （你的电脑）    │                         │  Gateway        │
└────────────────┘                         └────────┬────────┘
                                                    │
                                                    │ WebSocket
                                                    ↓
                                         ┌─────────────────────┐
                                         │ PhoneAgentX         │
                                         │ Node Host           │
                                         │ (Node.js 脚本)       │
                                         └──────────┬──────────┘
                                                    │
                                         ┌──────────▼──────────┐
                                         │ Bridge Server       │
                                         │ (:18790 HTTP 接口)   │
                                         │  Android App 内      │
                                         └──────────┬──────────┘
                                                    │
                                         ┌──────────▼──────────┐
                                         │ 设备控制层           │
                                         │ Socket (:28200)     │
                                         └──────────┬──────────┘
                                                    │
                                         ┌──────────▼──────────┐
                                         │ 手机硬件             │
                                         │ 触摸 / 滑动 / 截图   │
                                         └─────────────────────┘
```

---

## 快速开始

### 环境要求

| 要求 | 最低版本 | 推荐版本 |
|------|----------|----------|
| Android 手机 | 9 (API 28) | 11+ |
| Java | 17 | 17 |
| Gradle | 8.4 | 8.4 |
| Node.js | 18 | 20+ |

### 步骤一：构建 App

```bash
# 1. 克隆代码
git clone https://github.com/longxiashouji/PhoneAgentX.git
cd PhoneAgentX

# 2. 安装 Node 依赖（Node Host 需要）
npm install

# 3. 构建 Android App
./gradlew assembleDebug

# 4. 安装到手机
./gradlew installDebug
```

### 步骤二：开启无线调试

1. 手机打开 设置 → 关于手机
2. 连续点击 版本号 7 次，开启开发者模式
3. 返回设置 → 开发者选项
4. 开启 无线调试

### 步骤三：配对 ADB

```bash
# 在电脑上执行（手机会显示配对码）
adb pair 手机IP:5555
# 输入手机显示的配对码
```

### 步骤四：启动 Node Host

```bash
# 配置环境变量（可选）
export OPENCLAW_GATEWAY_URL=ws://你的电脑IP:18789

# 启动
node phoneagentx-node-host.js
```

### 步骤五：开始使用

1. 打开手机上的 PhoneAgentX App
2. 输入 OpenClaw Gateway 地址
3. 点击连接
4. 现在 AI 可以控制你的手机了！

---

## Skill 生态

### 什么是 Skill？

**Skill = 自动化任务模板**

用 JSON 定义，包含多个步骤，PhoneAgentX 自动执行。

### 示例：微信托管

```json
{
  "name": "wechat-auto-reply",
  "display_name": "微信托管自动回复",
  "version": "1.0.0",
  "steps": [
    {
      "id": "open_wechat",
      "type": "api",
      "action": "open_app",
      "params": { "package": "com.tencent.mm" }
    },
    {
      "id": "wait_message",
      "type": "wait_for_event",
      "event": "wechat_message"
    },
    {
      "id": "auto_reply",
      "type": "ai_act",
      "prompt": "根据收到的消息，生成合适的自动回复"
    }
  ]
}
```

### 内置 Skills

| 分类 | 名称 | 说明 |
|------|------|------|
| 社交 | 微信托管自动回复 | 自动回复微信消息 |
| 工具 | 状态监控 | 查看 PhoneAgentX 运行状态 |

### 创建自己的 Skill

详见 [Skill 开发指南](docs/skill-dev-guide.md)

---

## 技术架构

### 模块说明

| 模块 | 文件 | 作用 |
|------|------|------|
| **Node Host** | `phoneagentx-node-host.js` | 连接 Gateway，接收 AI 命令 |
| **Bridge Server** | `BridgeServerManager.kt` | HTTP 接口，转发命令到设备 |
| **Socket 客户端** | `TutuSocketClient.kt` | 与设备控制层通信 |
| **Skill 引擎** | `SkillEngine.kt` | 解析和执行 Skill |

### 技术栈

| 层级 | 技术选型 |
|------|----------|
| **UI** | Jetpack Compose + Material 3 |
| **状态管理** | ViewModel + StateFlow |
| **网络** | Kotlinx Serialization JSON |
| **协议** | 自定义 JSON Socket（端口 28200）|
| **ADB** | ADB v2 + TLS + SPAKE2 配对 |
| **Native** | CMake + C++（BoringSSL）|
| **Node Host** | Node.js + ws（WebSocket）|
| **AI** | 可插拔 Provider（OpenAI / Claude / Gemini）|

---

## 项目结构

```
PhoneAgentX/
│
├── phoneagentx-node-host.js        # Node Host 主程序
│                                 #    连接 Gateway，转发命令到手机
│
├── phoneagentx-app/                # Android App 源码
│   └── src/main/java/com/phoneagentx/
│       ├── PhoneAgentXApp.kt       #    Application 入口
│       ├── MainActivity.kt          #    主界面
│       │
│       ├── core/                   #    核心模块
│       │   ├── BridgeServerManager.kt   # HTTP → Socket 桥接
│       │   ├── TutuSocketClient.kt      # Socket 通信
│       │   ├── SkillEngine.kt           # Skill 执行引擎
│       │   ├── AiProvider.kt            # AI 接口抽象
│       │   ├── Models.kt                # 数据模型
│       │   ├── PhoneAgentXApiClient.kt  # API 客户端
│       │   └── SettingsManager.kt       # 设置管理
│       │
│       ├── feature/                 #    功能模块
│       │   └── home/              #    主界面
│       │       ├── HomeScreen.kt
│       │       └── HomeViewModel.kt
│       │
│       ├── service/               #    后台服务
│       │   ├── NodeHostService.kt     # 节点服务
│       │   └── AdbPairingService.kt   # ADB 配对
│       │
│       └── ui/                    #    UI 组件
│           └── theme/             #    Material 3 主题
│
├── skills/                        # Skill 库
│   ├── phoneagentx-status.json    #    状态监控
│   └── wechat-auto-reply.json     #    微信自动回复
│
├── docs/                          # 文档
│   ├── node-host-guide.md         #    Node Host 开发指南
│   └── skill-dev-guide.md         #    Skill 开发文档
│
├── SPEC.md                        # 项目规格书
├── README.md                      # 说明文档
├── CLAUDE.md                      # 开发者指南
└── package.json                   # Node 依赖
```

---

## 常见问题

### Q: 支持哪些手机？
A: Android 9 (API 28) 及以上，大部分主流机型都能用。

### Q: 需要 root 吗？
A: 不需要。使用官方无线调试功能，不需要 root 权限。

### Q: 为什么用 ADB 而不是无障碍服务？
A: 因为 ADB 是系统级协议，无法被应用检测，更稳定。

### Q: PhoneAgentX 和其他项目有什么关系？
A: PhoneAgentX 是完全独立开发的开源项目，使用 OpenClaw Node 架构。

### Q: 可以控制多台手机吗？
A: 可以，每个手机运行一个 Node Host，连接到同一个 Gateway。

---

## 技术栈

| 分类 | 技术 |
|------|------|
| Android UI | Jetpack Compose + Material 3 |
| 状态管理 | ViewModel + StateFlow / SharedFlow |
| 网络协议 | Kotlinx Serialization JSON |
| 设备控制 | 自定义 Socket 协议 |
| ADB | ADB v2（TLS + SPAKE2）|
| Native | CMake + C++（BoringSSL）|
| Node Host | Node.js + ws |
| AI | 可插拔架构 |
| 构建 | Gradle 8.4 |

---

## 致谢

PhoneAgentX 的实现参考了以下优秀开源项目：

- **[scrcpy](https://github.com/Genymobile/scrcpy)** — 优秀的屏幕投射工具，其设计启发了 PhoneAgentX 的设备控制层
- **[Shizuku](https://github.com/RikkaApps/Shizuku)** — 开创了 ADB 应用级权限的先河，启发了无线 ADB 的实现

---

## 作者

**阿龙 / Long**

- GitHub: https://github.com/longxiashouji
- Email: 963737104@qq.com
- 微信: clawai

---

## 开源协议

**GNU General Public License v3.0 (GPL v3.0)**

```
Copyright (C) 2025 阿龙 / Long

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.
```

---

<p align="center">
  用 ❤️ 为开源社区打造
</p>