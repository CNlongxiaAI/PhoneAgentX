# AgentClaw Skill 开发指南

## 概述

Skill 是声明式 JSON 自动化任务，AgentClaw 引擎解析并执行。

## Skill 结构

```json
{
  "name": "skill-name",
  "display_name": "技能显示名",
  "version": "1.0.0",
  "author": "author",
  "description": "技能描述",
  "icon": "icon-name",
  "category": "category",
  "tags": ["tag1", "tag2"],
  "estimated_time": "5min",
  "estimated_tokens": 2000,
  "requires": { "apps": ["com.example.app"] },
  "config": {},
  "steps": [...]
}
```

## 步骤类型

### 1. api - 设备 API 调用

```json
{
  "id": "open_wechat",
  "type": "api",
  "action": "open_app",
  "params": { "app_name": "com.tencent.mm" }
}
```

可用 action：
- `open_app` - 启动应用
- `press_home` - 按 Home 键
- `press_back` - 按返回键
- `screenshot` - 截图
- `subscribe_events` - 订阅事件
- `unsubscribe_events` - 取消订阅

### 2. ai_check - AI 视觉分析 + 条件分支

```json
{
  "id": "check_state",
  "type": "ai_check",
  "prompt": "观察当前界面，判断是否在首页",
  "branches": [
    { "match": "正常", "goto": "next_step" },
    { "match": "异常", "goto": "handle_error" }
  ],
  "default_branch": { "goto": "next_step" }
}
```

### 3. ai_act - AI 视觉分析 + 执行操作

```json
{
  "id": "handle_popup",
  "type": "ai_act",
  "prompt": "当前有弹窗，请关闭它",
  "max_loops": 3
}
```

### 4. ai_summary - AI 生成总结

```json
{
  "id": "summary",
  "type": "ai_summary",
  "prompt": "任务完成，总结结果：${variable}",
  "output": "result"
}
```

### 5. loop - 循环

```json
{
  "id": "main_loop",
  "type": "loop",
  "label": "主循环",
  "max_duration": 600000,
  "max_iterations": 100,
  "loop_steps": [...]
}
```

### 6. condition - 条件分支

```json
{
  "id": "check_done",
  "type": "condition",
  "expression": "_count >= 10",
  "goto": "summary"
}
```

### 7. set_var - 变量操作

```json
{
  "id": "init_count",
  "type": "set_var",
  "var": "_count",
  "op": "assign",
  "value": "0"
}
```

支持 op：
- `assign` - 赋值
- `increment` - 递增
- `append` - 追加到数组

### 8. wait - 等待

```json
{
  "id": "wait_load",
  "type": "wait",
  "duration": 2000
}
```

### 9. wait_until_changed - 等待界面变化

```json
{
  "id": "wait_stable",
  "type": "wait_until_changed",
  "timeout": 10000,
  "stable_ms": 800
}
```

### 10. wait_for_event - 等待事件

```json
{
  "id": "wait_notify",
  "type": "wait_for_event",
  "event_types": ["notification_state_changed"],
  "timeout": 60000,
  "save_as": "notify"
}
```

### 11. prompt_user - 用户输入

```json
{
  "id": "ask_count",
  "type": "prompt_user",
  "title": "设置参数",
  "fields": [
    { "key": "count", "label": "数量", "type": "select", "options": ["1", "2", "3"], "default": "2" }
  ],
  "timeout": 30,
  "timeout_action": "use_default"
}
```

### 12. click / type / swipe - 基础操作

```json
{
  "id": "tap_confirm",
  "type": "click",
  "x": 540,
  "y": 960
}
```

## 变量引用

使用 `${variable}` 引用变量：

```json
{
  "id": "summary",
  "type": "ai_summary",
  "prompt": "共处理 ${_count} 条消息"
}
```

内置变量：
- `${input.field_name}` - 用户输入
- `${screen_info}` - AI 截图分析结果

## 完整示例

```json
{
  "name": "auto-like-wechat-moments",
  "display_name": "微信朋友圈自动点赞",
  "version": "1.0.0",
  "steps": [
    { "id": "open_wechat", "type": "api", "action": "open_app", "params": { "app_name": "com.tencent.mm" } },
    { "id": "go_to_moments", "type": "ai_act", "prompt": "点击发现Tab，进入朋友圈", "max_loops": 5 },
    { "id": "like_loop", "type": "loop", "max_iterations": 20, "loop_steps": [
      { "id": "scroll", "type": "api", "action": "scroll", "params": { "direction": "down" } },
      { "id": "wait", "type": "wait", "duration": 1000 },
      { "id": "find_like", "type": "ai_act", "prompt": "找到朋友圈中的点赞按钮并点击", "max_loops": 3 },
      { "id": "count", "type": "set_var", "var": "_count", "op": "increment", "value": "1" }
    ]},
    { "id": "summary", "type": "ai_summary", "prompt": "点赞完成，共点赞 ${_count} 条", "output": "result" }
  ]
}
```

## 贡献 Skill

欢迎提交 Pull Request 到 `skills/` 目录！

格式要求：
1. 遵循 Skill JSON 结构
2. `version` 必须是 semver 格式
3. `steps` 至少包含 2 个步骤
4. 测试通过后再提交