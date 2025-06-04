package com.example.cash

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import java.text.SimpleDateFormat
import java.util.*

object BudgetAlertManager {
    private const val CHANNEL_ID = "budget_alert_channel"
    private const val CHANNEL_NAME = "Budget Alerts"

    // type: "day", "week", "month"
    @SuppressLint("StringFormatInvalid")
    fun checkAndAlert(context: Context, userEmail: String, type: String, totalExpense: Float) {
        val prefs = context.getSharedPreferences("budget_prefs", Context.MODE_PRIVATE)
        val budgetKey = "budget_${type}_$userEmail"

        val budget = prefs.getFloat(budgetKey, 0f)
        if (budget <= 0f) return

        val percent = totalExpense / budget
        val dateKey = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())

        val alert50Key = "budget_alert_${type}_50_${userEmail}_$dateKey"
        val alert100Key = "budget_alert_${type}_100_${userEmail}_$dateKey"

        // 50% 通知
        if (percent >= 0.5f && !prefs.getBoolean(alert50Key, false)) {
            val message = context.getString(R.string.budget_exceed_50)
            sendNotification(
                context,
                "Budget > 50%",
                message
            )
            prefs.edit().putBoolean(alert50Key, true).apply()
        }

        // 100% 通知
        if (percent >= 1f && !prefs.getBoolean(alert100Key, false)) {
            val message = context.getString(R.string.budget_exceed_100)
            sendNotification(
                context,
                "Budget 100%",
                message
            )
            prefs.edit().putBoolean(alert100Key, true).apply()
        }
    }

    // 不需要 resetAlertFlags，如果用日期key，每天自動換key，不會舊的true影響新的一天

    private fun getBudgetTypeText(type: String, context: Context): String {
        return when (type) {
            "day" -> context.getString(R.string.budget_daily)
            "week" -> context.getString(R.string.budget_weekly)
            "month" -> context.getString(R.string.budget_monthly)
            else -> ""
        }
    }

    private fun sendNotification(context: Context, title: String, message: String) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 系統預設通知聲音 URI
        val soundUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)

        // Android O 以上要註冊 Channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Budget alert notification"
                setSound(soundUri, null) // 設定 Channel 的聲音
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.notification_budget) // 你要自備一個小圖示
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setSound(soundUri)
            .build()

        // 通知 ID 用 type 做區分
        val notifyId = (title + message).hashCode() // 保證同天同類型只出現一次通知

        notificationManager.notify(notifyId, notification)
    }
}
