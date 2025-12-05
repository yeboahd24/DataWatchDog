package com.datawatchdog.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.datawatchdog.viewmodel.DashboardViewModel

@Composable
fun DashboardScreen(viewModel: DashboardViewModel) {
    val totalMobileUsage by viewModel.totalMobileUsage.collectAsState()
    val totalWifiUsage by viewModel.totalWifiUsage.collectAsState()
    val topApps by viewModel.topApps.collectAsState()
    val bundleInfo by viewModel.bundleInfo.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            "Data Watchdog",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Usage Summary
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

        // Bundle Info
        bundleInfo?.let {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Bundle: ${it.provider}",
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

        // Top Apps
        Text(
            "Top 5 Apps",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        topApps.forEach { app ->
            AppUsageRow(
                appName = app.appName,
                mobile = String.format("%.2f MB", app.getTotalMobile() / (1024.0 * 1024.0)),
                wifi = String.format("%.2f MB", app.getTotalWifi() / (1024.0 * 1024.0))
            )
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
