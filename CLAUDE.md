# AgentClaw

> **作者：阿龙 / Long**

## 项目简介

AgentClaw 是 OpenClaw 的手机端节点运行时。让 OpenClaw AI 在手机上拥有手和脚。

## 模块

- `agentclaw-node-host.js` - Node Host（连接 Gateway）
- `agentclaw-app/` - Android App 源码
- `skills/` - 开源 Skill 库

## 开发

```bash
# 启动 Node Host
node agentclaw-node-host.js
```

## 技术栈

- Kotlin + Jetpack Compose
- ADB v2 (TLS + SPAKE2)
- Node.js（连接 OpenClaw Gateway）
- JSON Socket 协议

## 版权声明

Copyright (C) 2025 阿龙 / Long. All rights reserved.