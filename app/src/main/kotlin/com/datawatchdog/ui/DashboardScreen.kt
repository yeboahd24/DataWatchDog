package com.datawatchdog.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.datawatchdog.viewmodel.DashboardViewModel

@Composable
fun DashboardScreen(viewModel: DashboardViewModel) {
    val totalMobileUsage by viewModel.totalMobileUsage.collectAsState()
    val totalWifiUsage by viewModel.totalWifiUsage.collectAsState()
    val topApps by viewModel.topApps.collectAsState()
    val bundleInfo by viewModel.bundleInfo.collectAsState()
    val drainAlerts by viewModel.drainAlerts.collectAsState()
    val recentUsage by viewModel.recentUsage.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Data Watchdog",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Monitor your data usage",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
                
                FilledTonalButton(
                    onClick = { viewModel.refresh() },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Refresh",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            UsageCard(
                title = "Mobile",
                usage = String.format("%.2f MB", totalMobileUsage),
                modifier = Modifier.weight(1f)
            )
            UsageCard(
                title = "WiFi",
                usage = String.format("%.2f MB", totalWifiUsage),
                modifier = Modifier.weight(1f)
            )
        }

        if (drainAlerts.isNotEmpty()) {
            Text(
                "⚠️ Drain Alerts",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF6B6B),
                modifier = Modifier.padding(bottom = 12.dp)
            )
            drainAlerts.take(3).forEach { alert ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2D1B1B))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                alert.appName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF6B6B)
                            )
                            Text(
                                "Draining ${String.format("%.2f", alert.drainRate)} MB/min",
                                fontSize = 12.sp,
                                color = Color(0xFFFFAAAA),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        bundleInfo?.let {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "📦 Bundle: ${it.provider}",
                        fontSize = 14.sp,
                        color = Color(0xFFBBBBBB)
                    )
                    Text(
                        "${it.usedMB}/${it.totalMB} MB",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    val hoursRemaining = (it.expiryDate - System.currentTimeMillis()) / (1000 * 60 * 60)
                    Text(
                        "Expires in $hoursRemaining hours",
                        fontSize = 12.sp,
                        color = if (hoursRemaining < 24) Color(0xFFFF6B6B) else Color(0xFF51CF66),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        Text(
            "Top 5 Apps (Last Hour)",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (recentUsage.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
            ) {
                Text(
                    "No recent activity. Use some apps to see data.",
                    fontSize = 14.sp,
                    color = Color(0xFFBBBBBB),
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            recentUsage.forEach { app ->
                AppUsageRow(
                    appName = app.appName,
                    mobile = String.format("%.2f MB", app.getTotalMobile() / (1024.0 * 1024.0)),
                    wifi = String.format("%.2f MB", app.getTotalWifi() / (1024.0 * 1024.0))
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Top 5 Apps (Today)",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (topApps.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
            ) {
                Text(
                    "No data yet. Use some apps and wait 10 seconds.",
                    fontSize = 14.sp,
                    color = Color(0xFFBBBBBB),
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            topApps.forEach { app ->
                AppUsageRow(
                    appName = app.appName,
                    mobile = String.format("%.2f MB", app.getTotalMobile() / (1024.0 * 1024.0)),
                    wifi = String.format("%.2f MB", app.getTotalWifi() / (1024.0 * 1024.0))
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun UsageCard(title: String, usage: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontSize = 12.sp, color = Color(0xFFBBBBBB))
            Text(
                usage,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun AppUsageRow(appName: String, mobile: String, wifi: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(appName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(
                    "Mobile: $mobile | WiFi: $wifi",
                    fontSize = 12.sp,
                    color = Color(0xFFBBBBBB),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
