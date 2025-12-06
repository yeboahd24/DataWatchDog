package com.datawatchdog.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.datawatchdog.util.DataUsageTracker
import com.datawatchdog.viewmodel.AppListViewModel
import com.datawatchdog.viewmodel.TrackingViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TrackingScreen(trackingVM: TrackingViewModel, appListVM: AppListViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val activeTracking by trackingVM.activeTracking.collectAsState()
    val completedTrackings by trackingVM.completedTrackings.collectAsState()
    var showAppSelector by remember { mutableStateOf(false) }
    val installedApps = remember {
        com.datawatchdog.util.InstalledAppsProvider(context).getAllInstalledApps()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(16.dp)
    ) {
        Text(
            "Track App Usage",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        activeTracking?.let { tracking ->
            ActiveTrackingCard(tracking, onStop = { trackingVM.stopTracking() })
        } ?: run {
            Button(
                onClick = { showAppSelector = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF51CF66))
            ) {
                Text("Start Tracking App")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "Tracking History",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyColumn {
            items(completedTrackings) { tracking ->
                CompletedTrackingCard(tracking, onDelete = { trackingVM.deleteTracking(tracking.id) })
            }
        }
    }

    if (showAppSelector) {
        InstalledAppSelectorDialog(
            apps = installedApps,
            onDismiss = { showAppSelector = false },
            onSelect = { packageName, appName ->
                trackingVM.startTracking(packageName, appName)
                showAppSelector = false
            }
        )
    }
}

@Composable
fun ActiveTrackingCard(tracking: com.datawatchdog.db.AppTrackingEntity, onStop: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E3A1E))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "🔴 Tracking: ${tracking.appName}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF51CF66)
                )
            }

            val duration = (System.currentTimeMillis() - tracking.startTime) / 1000
            val minutes = duration / 60
            val seconds = duration % 60

            Text(
                "Duration: ${minutes}m ${seconds}s",
                fontSize = 14.sp,
                color = Color.White,
                modifier = Modifier.padding(top = 8.dp)
            )

            Text(
                "Started: ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(tracking.startTime))}",
                fontSize = 12.sp,
                color = Color(0xFFBBBBBB),
                modifier = Modifier.padding(top = 4.dp)
            )

            Button(
                onClick = onStop,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B6B))
            ) {
                Text("Stop Tracking")
            }
        }
    }
}

@Composable
fun CompletedTrackingCard(tracking: com.datawatchdog.db.AppTrackingEntity, onDelete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    tracking.appName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Text("🗑️", fontSize = 16.sp)
                }
            }

            val duration = ((tracking.endTime ?: 0) - tracking.startTime) / 1000
            val minutes = duration / 60

            Text(
                "Duration: ${minutes} minutes",
                fontSize = 12.sp,
                color = Color(0xFFBBBBBB),
                modifier = Modifier.padding(top = 4.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Mobile", fontSize = 11.sp, color = Color(0xFFBBBBBB))
                    Text(
                        String.format("%.2f MB", tracking.getTotalMobileUsed() / (1024.0 * 1024.0)),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Column {
                    Text("WiFi", fontSize = 11.sp, color = Color(0xFFBBBBBB))
                    Text(
                        String.format("%.2f MB", tracking.getTotalWifiUsed() / (1024.0 * 1024.0)),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Column {
                    Text("Total", fontSize = 11.sp, color = Color(0xFFBBBBBB))
                    Text(
                        String.format("%.2f MB", tracking.getTotalUsed() / (1024.0 * 1024.0)),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF51CF66)
                    )
                }
            }

            Text(
                SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(tracking.startTime)),
                fontSize = 11.sp,
                color = Color(0xFF888888),
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun InstalledAppSelectorDialog(
    apps: List<com.datawatchdog.util.InstalledApp>,
    onDismiss: () -> Unit,
    onSelect: (String, String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredApps = if (searchQuery.isEmpty()) {
        apps
    } else {
        apps.filter { it.appName.contains(searchQuery, ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select App to Track") },
        text = {
            Column {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search apps...") },
                    placeholder = { Text("WhatsApp, YouTube, etc.") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.height(400.dp)) {
                    items(filteredApps) { app ->
                        TextButton(
                            onClick = { onSelect(app.packageName, app.appName) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(app.appName, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
