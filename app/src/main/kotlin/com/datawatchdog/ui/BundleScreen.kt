package com.datawatchdog.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.datawatchdog.db.AppDatabase
import com.datawatchdog.db.BundleEntity
import com.datawatchdog.viewmodel.BundleViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun BundleScreen(viewModel: BundleViewModel) {
    val context = LocalContext.current
    val bundleInfo by viewModel.bundleInfo.collectAsState()
    val scope = rememberCoroutineScope()
    var showTestDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(16.dp)
    ) {
        Text(
            "Bundle & Predictions",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        bundleInfo?.let { bundle ->
            BundleCard(bundle)
        } ?: run {
            NoBundleCard(onAddTestBundle = { showTestDialog = true })
        }

        if (showTestDialog) {
            TestBundleDialog(
                onDismiss = { showTestDialog = false },
                onConfirm = { provider, totalMB, usedMB, daysRemaining ->
                    scope.launch {
                        val expiryDate = System.currentTimeMillis() + (daysRemaining * 24 * 60 * 60 * 1000L)
                        val db = AppDatabase.getDatabase(context)
                        db.bundleDao().updateBundle(
                            BundleEntity(
                                expiryDate = expiryDate,
                                totalMB = totalMB,
                                usedMB = usedMB,
                                provider = provider,
                                lastUpdated = System.currentTimeMillis()
                            )
                        )
                        viewModel.refresh()
                        showTestDialog = false
                    }
                }
            )
        }
    }
}

@Composable
fun BundleCard(bundle: BundleEntity) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                bundle.provider,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(16.dp))

            val progress = if (bundle.totalMB > 0) {
                (bundle.usedMB.toFloat() / bundle.totalMB.toFloat()).coerceIn(0f, 1f)
            } else 0f

            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = when {
                    progress > 0.8f -> Color(0xFFFF6B6B)
                    progress > 0.5f -> Color(0xFFFFA500)
                    else -> Color(0xFF51CF66)
                }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "${bundle.usedMB} MB used",
                    fontSize = 12.sp,
                    color = Color(0xFFBBBBBB)
                )
                Text(
                    "${bundle.totalMB} MB total",
                    fontSize = 12.sp,
                    color = Color(0xFFBBBBBB)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            val hoursRemaining = (bundle.expiryDate - System.currentTimeMillis()) / (1000 * 60 * 60)
            val daysRemaining = hoursRemaining / 24
            val expiryColor = if (hoursRemaining < 24) Color(0xFFFF6B6B) else Color(0xFF51CF66)

            Text(
                "Expires in $daysRemaining days ($hoursRemaining hours)",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = expiryColor
            )

            val expiryDate = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                .format(Date(bundle.expiryDate))
            Text(
                expiryDate,
                fontSize = 12.sp,
                color = Color(0xFFBBBBBB),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun NoBundleCard(onAddTestBundle: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "No Bundle Info",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                "Receive an SMS from your carrier or add test data",
                fontSize = 14.sp,
                color = Color(0xFFBBBBBB),
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
            )
            Button(onClick = onAddTestBundle) {
                Text("Add Test Bundle")
            }
        }
    }
}

@Composable
fun TestBundleDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Long, Long, Int) -> Unit
) {
    var provider by remember { mutableStateOf("MTN") }
    var totalMB by remember { mutableStateOf("5120") }
    var usedMB by remember { mutableStateOf("1024") }
    var daysRemaining by remember { mutableStateOf("15") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Test Bundle") },
        text = {
            Column {
                OutlinedTextField(
                    value = provider,
                    onValueChange = { provider = it },
                    label = { Text("Provider") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = totalMB,
                    onValueChange = { totalMB = it },
                    label = { Text("Total MB") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = usedMB,
                    onValueChange = { usedMB = it },
                    label = { Text("Used MB") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = daysRemaining,
                    onValueChange = { daysRemaining = it },
                    label = { Text("Days Remaining") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        provider,
                        totalMB.toLongOrNull() ?: 5120,
                        usedMB.toLongOrNull() ?: 1024,
                        daysRemaining.toIntOrNull() ?: 15
                    )
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
