package com.datawatchdog.util

import android.content.Context
import android.database.Cursor
import android.provider.Telephony
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.regex.Pattern

data class BundleInfo(
    val provider: String,
    val expiryDate: Long,
    val totalMB: Long,
    val usedMB: Long
)

class SmsParser(private val context: Context) {

    fun parseBundleFromSms(): BundleInfo? {
        val cursor = context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(Telephony.Sms.BODY, Telephony.Sms.DATE),
            null,
            null,
            "${Telephony.Sms.DATE} DESC LIMIT 50"
        ) ?: return null

        var bundleInfo: BundleInfo? = null

        cursor.use {
            while (it.moveToNext()) {
                val body = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.BODY))
                val provider = detectProvider(body)

                if (provider != null) {
                    val expiryDate = parseExpiryDate(body)
                    if (expiryDate != null) {
                        bundleInfo = BundleInfo(
                            provider = provider,
                            expiryDate = expiryDate,
                            totalMB = parseTotalMB(body),
                            usedMB = parseUsedMB(body)
                        )
                        break
                    }
                }
            }
        }

        return bundleInfo
    }

    private fun detectProvider(text: String): String? {
        return when {
            text.contains("MTN", ignoreCase = true) -> "MTN"
            text.contains("Vodafone", ignoreCase = true) -> "Vodafone"
            text.contains("AirtelTigo", ignoreCase = true) -> "AirtelTigo"
            text.contains("Airtel", ignoreCase = true) -> "Airtel"
            else -> null
        }
    }

    private fun parseExpiryDate(text: String): Long? {
        val patterns = listOf(
            "valid till\\s+([0-9]{1,2}[/-][0-9]{1,2}[/-][0-9]{2,4})",
            "expires on\\s+([0-9]{1,2}[/-][0-9]{1,2}[/-][0-9]{2,4})",
            "expire[sd]?\\s+([0-9]{1,2}[/-][0-9]{1,2}[/-][0-9]{2,4})",
            "valid until\\s+([0-9]{1,2}[/-][0-9]{1,2}[/-][0-9]{2,4})",
            "([0-9]{1,2}[/-][0-9]{1,2}[/-][0-9]{2,4})"
        )

        for (patternStr in patterns) {
            val pattern = Pattern.compile(patternStr, Pattern.CASE_INSENSITIVE)
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val dateStr = matcher.group(1)
                return parseDate(dateStr)
            }
        }

        return null
    }

    private fun parseDate(dateStr: String): Long? {
        val formats = listOf(
            "dd/MM/yyyy",
            "dd-MM-yyyy",
            "MM/dd/yyyy",
            "MM-dd-yyyy",
            "yyyy/MM/dd",
            "yyyy-MM-dd"
        )

        for (format in formats) {
            try {
                val sdf = SimpleDateFormat(format, Locale.getDefault())
                val date = sdf.parse(dateStr) ?: continue
                return date.time
            } catch (e: Exception) {
                continue
            }
        }

        return null
    }

    private fun parseTotalMB(text: String): Long {
        val pattern = Pattern.compile("([0-9]+)\\s*(?:MB|GB)", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(text)
        if (matcher.find()) {
            val value = matcher.group(1)?.toLongOrNull() ?: return 0
            return if (text.substring(matcher.start()).contains("GB", ignoreCase = true)) {
                value * 1024
            } else {
                value
            }
        }
        return 0
    }

    private fun parseUsedMB(text: String): Long {
        val pattern = Pattern.compile("used\\s+([0-9]+)\\s*(?:MB|GB)", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(text)
        if (matcher.find()) {
            val value = matcher.group(1)?.toLongOrNull() ?: return 0
            return if (text.substring(matcher.start()).contains("GB", ignoreCase = true)) {
                value * 1024
            } else {
                value
            }
        }
        return 0
    }
}
