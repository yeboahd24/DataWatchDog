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

    data class DataUsageInfo(
        val usedData: Double,
        val totalData: Double,
        val unit: String,
        val remainingData: Double = totalData - usedData,
        val percentage: Double = if (totalData > 0) (usedData / totalData) * 100 else 0.0,
        val carrier: String? = null,
        val validUntil: String? = null
    )

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

    fun parseDataUsageSms(smsBody: String, sender: String): DataUsageInfo? {
        return try {
            val normalizedBody = smsBody.lowercase().trim()
            val normalizedSender = sender.lowercase()
            
            // Try existing African carrier parser first
            val bundleInfo = parseBundleFromSmsBody(normalizedBody)
            if (bundleInfo != null) {
                return DataUsageInfo(
                    usedData = bundleInfo.usedMB.toDouble() / 1024.0,
                    totalData = bundleInfo.totalMB.toDouble() / 1024.0,
                    unit = "GB",
                    carrier = bundleInfo.provider
                )
            }
            
            // Try international carrier patterns
            parseInternationalCarriers(normalizedBody, normalizedSender)
                ?: parseGenericDataUsage(normalizedBody)
        } catch (e: Exception) {
            println("SmsParser: Error parsing SMS: ${e.message}")
            null
        }
    }

    private fun parseBundleFromSmsBody(text: String): BundleInfo? {
        val provider = detectProvider(text) ?: return null
        val expiryDate = parseExpiryDate(text) ?: return null
        val totalMB = parseTotalMB(text)
        val usedMB = parseUsedMB(text)
        
        if (totalMB > 0) {
            return BundleInfo(
                provider = provider,
                expiryDate = expiryDate,
                totalMB = totalMB,
                usedMB = usedMB
            )
        }
        return null
    }

    private fun detectProvider(text: String): String? {
        return when {
            text.contains("MTN", ignoreCase = true) -> "MTN"
            text.contains("Vodafone", ignoreCase = true) -> "Vodafone"
            text.contains("AirtelTigo", ignoreCase = true) -> "AirtelTigo"
            text.contains("Airtel", ignoreCase = true) -> "Airtel"
            text.contains("Glo", ignoreCase = true) -> "Glo"
            text.contains("9mobile", ignoreCase = true) -> "9mobile"
            text.contains("Safaricom", ignoreCase = true) -> "Safaricom"
            else -> null
        }
    }

    private fun parseInternationalCarriers(text: String, sender: String): DataUsageInfo? {
        return when {
            // US carriers
            sender.contains("verizon") || sender.contains("vzw") -> parseVerizon(text)
            sender.contains("att") || sender.contains("at&t") -> parseATT(text)
            sender.contains("tmobile") || sender.contains("t-mobile") -> parseTMobile(text)
            sender.contains("sprint") -> parseSprint(text)
            
            // Generic short code patterns
            sender.matches(Regex("""\d{4,6}""")) -> parseGenericDataUsage(text)
            
            else -> null
        }
    }

    private fun parseVerizon(text: String): DataUsageInfo? {
        val patterns = listOf(
            Regex("""you'?ve used (\d+\.?\d*)\s*(gb|mb) of your (\d+\.?\d*)\s*(gb|mb)"""),
            Regex("""(\d+\.?\d*)\s*(gb|mb) used of (\d+\.?\d*)\s*(gb|mb) plan""")
        )
        return parseWithPatterns(patterns, text, "Verizon")
    }
    
    private fun parseATT(text: String): DataUsageInfo? {
        val patterns = listOf(
            Regex("""you have used (\d+\.?\d*)\s*(gb|mb) of your (\d+\.?\d*)\s*(gb|mb)"""),
            Regex("""data usage: (\d+\.?\d*)\s*(gb|mb)/(\d+\.?\d*)\s*(gb|mb)""")
        )
        return parseWithPatterns(patterns, text, "AT&T")
    }
    
    private fun parseTMobile(text: String): DataUsageInfo? {
        val patterns = listOf(
            Regex("""you'?ve used (\d+\.?\d*)\s*(gb|mb) of (\d+\.?\d*)\s*(gb|mb)"""),
            Regex("""usage alert: (\d+\.?\d*)\s*(gb|mb)/(\d+\.?\d*)\s*(gb|mb)""")
        )
        return parseWithPatterns(patterns, text, "T-Mobile")
    }
    
    private fun parseSprint(text: String): DataUsageInfo? {
        val patterns = listOf(
            Regex("""you have used (\d+\.?\d*)\s*(gb|mb) of (\d+\.?\d*)\s*(gb|mb)"""),
            Regex("""(\d+\.?\d*)\s*(gb|mb) of your (\d+\.?\d*)\s*(gb|mb) allowance""")
        )
        return parseWithPatterns(patterns, text, "Sprint")
    }

    private fun parseGenericDataUsage(text: String): DataUsageInfo? {
        val patterns = listOf(
            Regex("""(\d+\.?\d*)\s*(gb|mb).*?(\d+\.?\d*)\s*(gb|mb)"""),
            Regex("""used:?\s*(\d+\.?\d*)\s*(gb|mb).*?total:?\s*(\d+\.?\d*)\s*(gb|mb)"""),
            Regex("""(\d+\.?\d*)\s*(gb|mb)\s*(?:of|out of|/)\s*(\d+\.?\d*)\s*(gb|mb)""")
        )
        return parseWithPatterns(patterns, text, null)
    }

    private fun parseWithPatterns(patterns: List<Regex>, text: String, carrier: String?): DataUsageInfo? {
        for (pattern in patterns) {
            val match = pattern.find(text)
            if (match != null) {
                return try {
                    val groups = match.groupValues
                    val used = groups[1].toDoubleOrNull() ?: continue
                    val usedUnit = groups[2].lowercase()
                    val total = groups[3].toDoubleOrNull() ?: continue
                    val totalUnit = groups[4].lowercase()
                    
                    // Normalize to GB
                    val normalizedUsed = if (usedUnit == "mb") used / 1024.0 else used
                    val normalizedTotal = if (totalUnit == "mb") total / 1024.0 else total
                    
                    DataUsageInfo(
                        usedData = normalizedUsed,
                        totalData = normalizedTotal,
                        unit = "GB",
                        carrier = carrier
                    )
                } catch (e: Exception) {
                    continue
                }
            }
        }
        return null
    }

    fun isDataUsageSms(smsBody: String, sender: String): Boolean {
        val normalizedBody = smsBody.lowercase()
        val normalizedSender = sender.lowercase()
        
        val dataKeywords = listOf("data", "usage", "allowance", "plan", "gb", "mb", "bundle")
        val carrierKeywords = listOf("mtn", "vodafone", "airteltigo", "airtel", "glo", "9mobile", 
                                   "safaricom", "verizon", "vzw", "att", "at&t", "tmobile", "t-mobile", "sprint")
        
        val hasDataKeywords = dataKeywords.any { normalizedBody.contains(it) }
        val isFromCarrier = carrierKeywords.any { normalizedSender.contains(it) } 
            || sender.matches(Regex("""\d{4,6}""")) // Short codes
            
        return hasDataKeywords && isFromCarrier
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
