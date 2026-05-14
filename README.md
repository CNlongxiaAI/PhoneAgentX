# AgentClaw

OpenClaw 手机节点运行时 — 让 AI 在手机上长出手和脚。

## 功能

- 📱 **系统级 ADB 控制** — 开发者 USB 权限，不可被检测
- 🤖 **OpenClaw Node** — 通过 nodes.invoke 控制手机
- ⚡ **声明式 Skill 引擎** — JSON 定义手机自动化
- 🎯 **内置技能市场** — 开源 Skill 库

## 快速开始

### 1. 安装 APK

下载最新 APK 并安装到 Android 手机（Android 9+）。

### 2. 开启开发者选项

设置 → 关于手机 → 连续点击版本号 7 次开启开发者模式

### 3. 开启无线调试

开发者选项 → 开启无线调试

### 4. ADB 配对

```bash
# 电脑端执行
adb pair <手机IP>:5555
```

### 5. 连接 Gateway

在 App 中输入 PC 的 Gateway URL，点击连接

### 6. AI 开始控制

```
AI: "帮我打开微信，发送消息你好"
AgentClaw → 执行 → 手机响应
```

## 支持的命令

| 分类 | 命令 |
|------|------|
| 设备 | device.screenshot, device.tap, device.swipe, device.type |
| 应用 | app.list, app.info, app.install, app.stop |
| 通信 | sms.send, sms.read, call.make |

## 技术栈

- Kotlin + Jetpack Compose
- ADB v2（TLS + SPAKE2 配对）
- Socket 协议（端口 28200）
- Node.js（连接 OpenClaw Gateway）

## 开源协议

**GPL v3.0**

Copyright (C) 2025 阿龙 / Long

## 贡献

欢迎提交 Issue 和 Pull Request！