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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.datawatchdog.service.DataMonitorService
import com.datawatchdog.ui.AppListScreen
import com.datawatchdog.ui.BundleScreen
import com.datawatchdog.ui.DashboardScreen
import com.datawatchdog.viewmodel.AppListViewModel
import com.datawatchdog.viewmodel.BundleViewModel
import com.datawatchdog.viewmodel.DashboardViewModel

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
            val dashboardVM = DashboardViewModel(this)
            val appListVM = AppListViewModel(this)
            val bundleVM = BundleViewModel(this)

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color(0xFF121212)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    when (currentScreen.intValue) {
                        0 -> DashboardScreen(dashboardVM)
                        1 -> AppListScreen(appListVM)
                        2 -> BundleScreen(bundleVM)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1E1E1E))
                            .padding(8.dp)
                    ) {
                        Button(
                            onClick = { currentScreen.intValue = 0 },
                            modifier = Modifier
                                .weight(1f)
                                .padding(4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (currentScreen.intValue == 0) Color(0xFF51CF66) else Color(0xFF333333)
                            )
                        ) {
                            Text("Dashboard", fontSize = 12.sp, color = Color.White)
                        }

                        Button(
                            onClick = { currentScreen.intValue = 1 },
                            modifier = Modifier
                                .weight(1f)
                                .padding(4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (currentScreen.intValue == 1) Color(0xFF51CF66) else Color(0xFF333333)
                            )
                        ) {
                            Text("Apps", fontSize = 12.sp, color = Color.White)
                        }

                        Button(
                            onClick = { currentScreen.intValue = 2 },
                            modifier = Modifier
                                .weight(1f)
                                .padding(4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (currentScreen.intValue == 2) Color(0xFF51CF66) else Color(0xFF333333)
                            )
                        ) {
                            Text("Bundle", fontSize = 12.sp, color = Color.White)
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
}
