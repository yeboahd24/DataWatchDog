package com.datawatchdog

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.datawatchdog.service.DataMonitorService
import com.datawatchdog.ui.AppListScreen
import com.datawatchdog.ui.BundleScreen
import com.datawatchdog.ui.DashboardScreen
import com.datawatchdog.ui.PermissionRequestScreen
import com.datawatchdog.ui.TrackingScreen
import com.datawatchdog.viewmodel.AppListViewModel
import com.datawatchdog.viewmodel.BundleViewModel
import com.datawatchdog.viewmodel.DashboardViewModel
import com.datawatchdog.viewmodel.TrackingViewModel
import android.app.AppOpsManager
import android.content.Context
import androidx.compose.runtime.*

class MainActivity : ComponentActivity() {
    private val requiredPermissions = arrayOf(
        Manifest.permission.PACKAGE_USAGE_STATS,
        Manifest.permission.READ_SMS,
        Manifest.permission.POST_NOTIFICATIONS
    )

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            startMonitoringService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestPermissions()

        setContent {
            val currentScreen = remember { mutableIntStateOf(0) }
            var hasUsagePermission by remember { mutableStateOf(hasUsageStatsPermission()) }
            val dashboardVM = DashboardViewModel(this)
            val appListVM = AppListViewModel(this)
            val bundleVM = BundleViewModel(this)
            val trackingVM = TrackingViewModel(this)

            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (!hasUsagePermission) {
                        PermissionRequestScreen(
                            onPermissionGranted = {
                                hasUsagePermission = true
                            }
                        )
                    } else {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Box(modifier = Modifier.weight(1f)) {
                                when (currentScreen.intValue) {
                                    0 -> DashboardScreen(dashboardVM)
                                    1 -> AppListScreen(appListVM)
                                    2 -> BundleScreen(bundleVM)
                                    3 -> TrackingScreen(trackingVM, appListVM)
                                }
                            }

                            // Modern bottom navigation
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    NavigationItem(
                                        icon = Icons.Default.Dashboard,
                                        label = "Dashboard",
                                        isSelected = currentScreen.intValue == 0,
                                        onClick = { currentScreen.intValue = 0 }
                                    )
                                    
                                    NavigationItem(
                                        icon = Icons.Default.Apps,
                                        label = "Apps",
                                        isSelected = currentScreen.intValue == 1,
                                        onClick = { currentScreen.intValue = 1 }
                                    )
                                    
                                    NavigationItem(
                                        icon = Icons.Default.DataUsage,
                                        label = "Bundle",
                                        isSelected = currentScreen.intValue == 2,
                                        onClick = { currentScreen.intValue = 2 }
                                    )
                                    
                                    NavigationItem(
                                        icon = Icons.Default.Timeline,
                                        label = "Track",
                                        isSelected = currentScreen.intValue == 3,
                                        onClick = { currentScreen.intValue = 3 }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun requestPermissions() {
        val missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (missingPermissions.isNotEmpty()) {
            permissionLauncher.launch(missingPermissions)
        } else {
            startMonitoringService()
        }
    }

    private fun startMonitoringService() {
        val intent = Intent(this, DataMonitorService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    @Suppress("DEPRECATION")
    private fun hasUsageStatsPermission(): Boolean {
        return try {
            val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                packageName
            )
            mode == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            false
        }
    }
}

@Composable
fun NavigationItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .padding(4.dp),
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) 
            MaterialTheme.colorScheme.primaryContainer 
        else 
            Color.Transparent
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(24.dp),
                tint = if (isSelected) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
