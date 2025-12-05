package com.datawatchdog.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.datawatchdog.db.AppDatabase
import com.datawatchdog.util.SmsParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val scope = CoroutineScope(Dispatchers.IO)
            scope.launch {
                val db = AppDatabase.getDatabase(context)
                val smsParser = SmsParser(context)
                val bundle = smsParser.parseBundleFromSms()

                if (bundle != null) {
                    val bundleEntity = com.datawatchdog.db.BundleEntity(
                        expiryDate = bundle.expiryDate,
                        totalMB = bundle.totalMB,
                        usedMB = bundle.usedMB,
                        provider = bundle.provider,
                        lastUpdated = System.currentTimeMillis()
                    )
                    db.bundleDao().updateBundle(bundleEntity)
                }
            }
        }
    }
}
