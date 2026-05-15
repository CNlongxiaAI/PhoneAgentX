package com.phoneagentx.feature.home

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
import com.phoneagentx.ConnectionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PhoneAgentX", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { viewModel.refreshStatus() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
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

            item {
                ConnectionStatusCard(
                    state = uiState.connectionState,
                    onConnect = { viewModel.connect() },
                    onDisconnect = { viewModel.disconnect() }
                )
            }

            item {
                AdbPairingCard(
                    isPaired = uiState.isAdbPaired,
                    onStartPairing = { viewModel.startAdbPairing() }
                )
            }

            item {
                Text("Quick Actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            items(uiState.quickActions) { action ->
                QuickActionCard(action = action, onClick = { viewModel.executeQuickAction(action) })
            }

            item {
                Text("Built-in Skills", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
                        ConnectionState.CONNECTED -> "Connected"
                        ConnectionState.AUTHENTICATED -> "Authenticated"
                        ConnectionState.CONNECTING -> "Connecting..."
                        ConnectionState.DISCONNECTED -> "Disconnected"
                        ConnectionState.ERROR -> "Error"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = when (state) {
                        ConnectionState.CONNECTED, ConnectionState.AUTHENTICATED -> "Ready for AI automation"
                        ConnectionState.CONNECTING -> "Please wait..."
                        ConnectionState.DISCONNECTED -> "Tap connect button"
                        ConnectionState.ERROR -> "Check phone settings"
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
                Text(if (state == ConnectionState.CONNECTED || state == ConnectionState.AUTHENTICATED) "Disconnect" else "Connect")
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
                Text("ADB Wireless Pairing", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = if (isPaired) "Paired (wireless debugging enabled)" else "Pairing required",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (!isPaired) {
                Button(onClick = onStartPairing) { Text("Start Pairing") }
            } else {
                Icon(Icons.Default.CheckCircle, contentDescription = "Paired", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
                    "screenshot" -> Icons.Default.Screenshot
                    "explore" -> Icons.Default.Explore
                    "build" -> Icons.Default.Build
                    else -> Icons.Default.Star
                },
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(action.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                Text(action.description, style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
                    "globe" -> Icons.Default.Public
                    "star" -> Icons.Default.Star
                    else -> Icons.Default.Star
                },
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(skill.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                Text(skill.description, style = MaterialTheme.typography.bodySmall)
                Text("${skill.estimatedTime} | ~${skill.estimatedTokens} tokens",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
            FilledTonalButton(onClick = onClick) {
                Text("Run")
            }
        }
    }
}
