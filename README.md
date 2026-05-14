# AgentClaw

<h1 align="center">
  <img src="docs/assets/logo_banner.png" alt="AgentClaw" width="560" />
</h1>

<p align="center">
  <b>OpenClaw Phone Node — 让 AI 在手机上真正长出手和脚</b>
</p>

<p align="center">
  <a href="https://github.com/longxiashouji/AgentClaw">GitHub</a> •
  <a href="SPEC.md">项目规格</a> •
  <a href="docs/node-host-guide.md">Node Host 开发指南</a> •
  <a href="docs/skill-dev-guide.md">Skill 开发指南</a> •
  <a href="skills/">Skill 库</a>
</p>

<p align="center">
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-GPL%20v3-blue.svg" alt="License: GPL v3" /></a>
  <img src="https://img.shields.io/badge/Android-9%2B-green.svg" alt="Android 9+" />
  <img src="https://img.shields.io/badge/Kotlin-1.9.22-purple.svg" alt="Kotlin" />
  <img src="https://img.shields.io/badge/Jetpack%20Compose-Material%203-blue.svg" alt="Compose" />
  <a href="https://github.com/longxiashouji/AgentClaw/stargazers"><img src="https://img.shields.io/github/stars/longxiashouji/AgentClaw?style=social" alt="Stars" /></a>
  <a href="https://github.com/longxiashouji/AgentClaw/network/members"><img src="https://img.shields.io/github/forks/longxiashouji/AgentClaw?style=social" alt="Forks" /></a>
  <a href="https://github.com/longxiashouji/AgentClaw/issues"><img src="https://img.shields.io/github/issues/longxiashouji/AgentClaw" alt="Issues" /></a>
</p>

---

## 什么是 AgentClaw？

AgentClaw 是 OpenClaw 的**手机端节点运行时**，让你的手机成为 AI 的延伸。它让 AI 真正"看见"屏幕、"操控"应用、"执行"任务 — 完全开源，运行在本地设备上。

通过声明式 **Skill 引擎**，你可以用 JSON 创建和分享自动化技能，无需编码。

**愿景：** 让每个人都能创建和分享手机 AI 技能，让 AI 真正拥有与物理世界交互的能力。

---

### 为什么选择 AgentClaw？

大多数手机自动化工具（Auto.js、Hamibot 等）依赖 Android 的 **无障碍服务（AccessibilityService）**。这条路有致命缺陷。AgentClaw 选择**系统级 ADB 协议**，从根本上解决这些问题：

> **系统级、不可检测**
> 无障碍服务运行在应用层。微信、抖音、淘宝、支付宝等主流应用会主动检测无障碍服务 — 触发风控甚至**封号**。AgentClaw 通过 ADB 在系统层操作，完全透明于目标应用，等同于真实手指触摸，**任何应用都无法检测**。

> **稳定可靠，一次配对永久生效**
> 无障碍权限脆弱 — 系统更新容易破坏、不同 OEM ROM 表现不一致、被系统提示打断或被回收。AgentClaw 使用标准 ADB 协议，行为一致稳定。一次配对，永久使用，无需重复授权。

> **无盲区覆盖**
> 无障碍服务只能与有无障碍节点的 UI 元素交互 — 游戏、Canvas、WebView 统统无法触及。AgentClaw 支持任意屏幕位置的触摸、滑动、按键输入，完全框架无关。

**一句话：** 无障碍方案是"在别人的地盘上偷偷摸摸"，随时有被检测的风险。AgentClaw 是"持系统级权限在操作"——稳定、安全、不可检测。

---

### AI 的手和脚 — 不只是自动化

AgentClaw 不只是另一个 RPA 工具。通过与大型语言模型（LLM）的深度集成，它成为 **AI 与物理世界之间的桥梁**：

- **AI 能"看见"** — 截图 + 视觉分析，理解屏幕上的任何内容
- **AI 能"思考"** — 根据屏幕状态和上下文做出智能决策
- **AI 能"行动"** — ADB 操作将决策转化为真实的触摸、滑动、输入
- **AI 能"学习"** — 声明式 Skill 系统让能力不断积累和进化

---

### 核心特性

- **Skill 引擎** — 基于 JSON 的声明式自动化引擎，支持 15+ 步骤类型：API 调用、AI 视觉分析、条件分支、循环、用户提示等
- **AI 驱动操作** — 利用 LLM 理解截图、定位 UI 元素、做出智能决策
- **系统级 ADB** — 自包含 ADB 实现，支持 mDNS 发现、TLS 配对（SPAKE2）、密钥持久化 — 无需电脑，零检测风险
- **OpenClaw Node** — 作为 OpenClaw 官方节点接入，AI 通过 `nodes.invoke` 直接控制手机
- **Skill 市场** — 浏览、搜索、一键运行社区创建的技能
- **开放生态** — 用 JSON 创建自己的技能，贡献社区，构建个人手机 AI 助手

---

### 工作原理

```
┌─────────────────────┐
│   OpenClaw AI        │
│  (PC / NAS / 云)      │
└──────────┬──────────┘
           │ nodes.invoke
           ▼
┌─────────────────────┐
│  OpenClaw Gateway   │
│    (:18789)          │
└──────────┬──────────┘
           │ WebSocket (node.invoke.request)
           ▼
┌─────────────────────┐
│  AgentClaw Node Host │
│  (agentclaw-node-host.js)
└──────────┬──────────┘
           │ HTTP (:18790)
           ▼
┌─────────────────────┐
│ Bridge Server (Kotlin)│
│  (Android App)        │
└──────────┬──────────┘
           │ Socket (:28200)
           ▼
┌─────────────────────┐
│  设备控制层           │
│  Touch / Swipe /     │
│  Screenshot / UI     │
└─────────────────────┘
```

---

## 快速开始

### 环境要求

| 要求 | 版本 |
|------|------|
| Android | 9+ (API 28)，推荐 11+ 获取最佳无线调试体验 |
| Java | 17 |
| Kotlin | 1.9.22 |
| Android Gradle Plugin | 8.2.2 |
| Gradle | 8.4 |
| Node.js | 18+（用于运行 Node Host）|

### 构建

1. **克隆仓库**

```bash
git clone https://github.com/longxiashouji/AgentClaw.git
cd AgentClaw
```

2. **安装 Node.js 依赖**

```bash
npm install
```

3. **构建 Android App**

```bash
./gradlew assembleDebug
./gradlew installDebug
```

4. **启动 Node Host**

```bash
node agentclaw-node-host.js
```

### 首次运行

1. 在手机上开启**无线调试**（开发者选项）
2. 打开 AgentClaw App，完成 ADB 配对
3. 在 App 中输入 PC 的 OpenClaw Gateway 地址，点击连接
4. AI 即可通过 OpenClaw 控制你的手机

---

## Skill 生态系统

AgentClaw 的强大之处来自其**可扩展的 Skill 系统**。Skills 是用简单 JSON 文件定义的，可以自动化手机上的几乎任何操作。

### 内置 Skills

| 分类 | 技能 |
|------|------|
| 社交 | 微信托管自动回复 |
| 工具 | AgentClaw 状态监控 |
| 日常 | 更多技能持续更新中... |

### 创建自己的 Skill

Skills 是声明式步骤结构的 JSON 文件。以下是最小示例：

```json
{
  "name": "hello-world",
  "display_name": "Hello World",
  "version": "1.0.0",
  "steps": [
    {
      "id": "check_screen",
      "type": "ai_check",
      "prompt": "描述你在屏幕上看到的内容",
      "save_as": "screen_info"
    },
    {
      "id": "report",
      "type": "ai_summary",
      "prompt": "总结：${screen_info}",
      "output": "result"
    }
  ]
}
```

完整指南请参阅 **[Skill 开发指南](docs/skill-dev-guide.md)**。

### 贡献 Skills

欢迎创建和分享技能！通过 Pull Request 提交到 [`skills/`](skills/) 目录。

---

## 技术栈

| 层级 | 技术 |
|------|------|
| UI | Jetpack Compose + Material 3 |
| 状态管理 | ViewModel + StateFlow / SharedFlow |
| 网络 | Kotlinx Serialization JSON + 自定义 Socket 协议 |
| ADB | 自实现 ADB v2（TLS、SPAKE2 配对）|
| Native | CMake + C++（BoringSSL SPAKE2）|
| AI | 可插拔 AI Provider（支持 OpenAI / Claude / Gemini 等）|
| Node Host | Node.js（连接 OpenClaw Gateway）|

---

## 项目结构

```
agentclaw/
├── agentclaw-node-host.js     # Node Host 主脚本（连接 Gateway）
├── agentclaw-app/             # Android App 源码
│   └── src/main/java/com/agentclaw/
│       ├── core/
│       │   ├── engine/        # Skill 执行引擎（核心）
│       │   ├── socket/       # TutuSocketClient（TCP）
│       │   ├── bridge/       # HTTP → Socket 桥接
│       │   ├── model/        # 数据模型
│       │   ├── network/      # API 客户端
│       │   └── settings/    # 设置管理
│       ├── feature/
│       │   └── home/         # 主界面
│       └── service/          # 前台服务
├── skills/                    # 开源 Skill 库
├── docs/
│   ├── node-host-guide.md   # Node Host 开发指南
│   └── skill-dev-guide.md   # Skill 开发文档
└── package.json
```

---

## 致谢

AgentClaw 的实现站在以下优秀开源项目的肩膀上：

- **[scrcpy](https://github.com/Genymobile/scrcpy)** — 出色的屏幕投射工具，启发了我们的设备控制层。AgentClaw 的设备控制服务基于 scrcpy-server 构建。
- **[Shizuku](https://github.com/RikkaApps/Shizuku)** — 开创了使用 ADB 进行应用级权限提升的方法，极大启发了我们的无线 ADB 实现。

特别感谢这些项目的开发者和社区。

---

## 作者

**阿龙 / Long** — [GitHub](https://github.com/longxiashouji) • [Email](mailto:long@example.com)

---

## 开源协议

AgentClaw 采用 **GNU General Public License v3.0** 开源。

```
Copyright (C) 2025 阿龙 / Long

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.
```

---

## 寻求帮助

我们特别欢迎以下领域的贡献：

- **Skill 引擎核心算法优化** — 优化步骤执行流程，减少不必要的等待，改进命令批处理
- **AI 分析准确性** — 为 `ai_check`/`ai_act` 步骤改进提示词工程，减少误判，提高 UI 元素识别率
- **Token 消耗优化** — 平衡 AI 分析质量与 token 成本，实现更智能的截图策略，减少冗余 AI 调用
- **错误恢复** — 更健壮的失败处理和重试策略

如果你对 RPA 自动化或 LLM 驱动的智能体感兴趣，这是很好的切入点！

---

<p align="center">
  用 ❤️ 为开源社区打造
</p>