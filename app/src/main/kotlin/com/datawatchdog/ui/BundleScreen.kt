package com.datawatchdog.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
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
import com.datawatchdog.viewmodel.BundleViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BundleScreen(viewModel: BundleViewModel) {
    val bundleInfo by viewModel.bundleInfo.collectAsState()
    val prediction by viewModel.exhaustionPrediction.collectAsState()

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

                    // Progress bar
                    val progress = if (bundle.totalMB > 0) {
                        (bundle.usedMB.toFloat() / bundle.totalMB.toFloat()).coerceIn(0f, 1f)
                    } else {
                        0f
                    }

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
                    val expiryColor = if (hoursRemaining < 24) Color(0xFFFF6B6B) else Color(0xFF51CF66)

                    Text(
                        "Expires in $hoursRemaining hours",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = expiryColor
                    )

                    val expiryDate = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                        .format(Date(bundle.expiryDate))
                    Text(
                        expiryDate,
                        fontSize = 12.sp,
                        color = Color(0xFFBBBBBB),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // Prediction
            prediction?.let { pred ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "Prediction",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            "At current usage rate:",
                            fontSize = 12.sp,
                            color = Color(0xFFBBBBBB)
                        )

                        Text(
                            String.format("%.2f MB/min", pred.avgUsagePerMinute),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF51CF66),
                            modifier = Modifier.padding(top = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            "Bundle will exhaust:",
                            fontSize = 12.sp,
                            color = Color(0xFFBBBBBB)
                        )

                        val exhaustDate = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                            .format(Date(pred.exhaustionTime))
                        Text(
                            exhaustDate,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        } ?: run {
            Text(
                "No bundle info available",
                fontSize = 14.sp,
                color = Color(0xFFBBBBBB),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}
