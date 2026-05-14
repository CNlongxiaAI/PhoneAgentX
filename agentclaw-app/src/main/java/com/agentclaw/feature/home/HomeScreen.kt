package com.agentclaw.feature.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.agentclaw.AgentClawApp
import com.agentclaw.ConnectionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AgentClaw", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { viewModel.refreshStatus() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // 连接状态卡片
            item {
                ConnectionStatusCard(
                    state = uiState.connectionState,
                    onConnect = { viewModel.connect() },
                    onDisconnect = { viewModel.disconnect() }
                )
            }

            // ADB 配对卡片
            item {
                AdbPairingCard(
                    isPaired = uiState.isAdbPaired,
                    onStartPairing = { viewModel.startAdbPairing() }
                )
            }

            // 快捷操作
            item {
                Text("快捷操作", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            items(uiState.quickActions) { action ->
                QuickActionCard(action = action, onClick = { viewModel.executeQuickAction(action) })
            }

            // 内置技能
            item {
                Text("内置技能", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            items(uiState.skills) { skill ->
                SkillCard(skill = skill, onClick = { viewModel.runSkill(skill) })
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun ConnectionStatusCard(
    state: ConnectionState,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (state) {
                ConnectionState.CONNECTED, ConnectionState.AUTHENTICATED -> MaterialTheme.colorScheme.primaryContainer
                ConnectionState.CONNECTING -> MaterialTheme.colorScheme.tertiaryContainer
                else -> MaterialTheme.colorScheme.errorContainer
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = when (state) {
                        ConnectionState.CONNECTED -> "已连接 TutuGui"
                        ConnectionState.AUTHENTICATED -> "已认证"
                        ConnectionState.CONNECTING -> "连接中..."
                        ConnectionState.DISCONNECTED -> "未连接"
                        ConnectionState.ERROR -> "连接错误"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = when (state) {
                        ConnectionState.CONNECTED, ConnectionState.AUTHENTICATED -> "可以开始 AI 自动化"
                        ConnectionState.CONNECTING -> "请稍候"
                        ConnectionState.DISCONNECTED -> "点击连接按钮"
                        ConnectionState.ERROR -> "请检查手机设置"
                    },
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Button(
                onClick = if (state == ConnectionState.CONNECTED || state == ConnectionState.AUTHENTICATED) onDisconnect else onConnect,
                colors = if (state == ConnectionState.CONNECTED || state == ConnectionState.AUTHENTICATED)
                    ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                else
                    ButtonDefaults.buttonColors()
            ) {
                Text(if (state == ConnectionState.CONNECTED || state == ConnectionState.AUTHENTICATED) "断开" else "连接")
            }
        }
    }
}

@Composable
fun AdbPairingCard(isPaired: Boolean, onStartPairing: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("ADB 无线配对", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = if (isPaired) "已配对（开发者选项中的无线调试）" else "需要配对 ADB",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (!isPaired) {
                Button(onClick = onStartPairing) { Text("开始配对") }
            } else {
                Icon(Icons.Default.CheckCircle, contentDescription = "已配对", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun QuickActionCard(action: QuickAction, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (action.icon) {
                    "screenshot" -> Icons.Default.ScreenshotMonitor
                    "explore" -> Icons.Default.Explore
                    "build" -> Icons.Default.Build
                    else -> Icons.Default.Star
                },
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(action.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(action.description, style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
fun SkillCard(skill: SkillInfo, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (skill.icon) {
                    "message-circle" -> Icons.Default.Message
                    "play-circle" -> Icons.Default.PlayCircle
                    "globe" -> Icons.Default.Language
                    "star" -> Icons.Default.Star
                    else -> Icons.Default.Star
                },
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(skill.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(skill.description, style = MaterialTheme.typography.bodySmall)
                Text("${skill.estimatedTime} · 约 ${skill.estimatedTokens} tokens", style = MaterialTheme.typography.labelSmall)
            }
            IconButton(onClick = onClick) {
                Icon(Icons.Default.PlayArrow, contentDescription = "运行")
            }
        }
    }
}