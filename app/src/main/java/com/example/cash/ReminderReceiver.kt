package com.example.cash

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import java.util.*

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        // 讀取語言設定（從 settings SharedPreferences）
        val prefs: SharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val isEnglish = prefs.getBoolean("language_english", false)
        val locale = if (isEnglish) Locale.ENGLISH else Locale("zh")
        val localizedContext = getLocalizedContext(context, locale)

        // 語系切換後使用 localized context 抓取文字資源
        val title = localizedContext.getString(R.string.reminder_title)
        val text = localizedContext.getString(R.string.reminder_text)

        // 使用不同語言對應不同頻道 ID（避免 Android Cache）
        val channelId = if (isEnglish) "reminder_channel_en" else "reminder_channel_zh"

        // 通知管理器
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 🔔 通知聲音
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        // Android 8.0+ 建立 NotificationChannel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // ❗頻道名稱和描述都要使用對應語系 context
            val channel = NotificationChannel(
                channelId,
                localizedContext.getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = localizedContext.getString(R.string.channel_description)
                enableLights(true)
                enableVibration(true)
                setSound(
                    soundUri,
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
            }
            notificationManager.createNotificationChannel(channel)
        }

        // 點擊通知會打開 MainActivity
        val intentToApp = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intentToApp,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 建立並發送通知
        val notification = NotificationCompat.Builder(localizedContext, channelId)
            .setSmallIcon(R.drawable.cash)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(soundUri)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(1001, notification)
    }

    // 建立有語言設定的 context
    private fun getLocalizedContext(baseContext: Context, locale: Locale): Context {
        Locale.setDefault(locale)
        val config = baseContext.resources.configuration
        config.setLocale(locale)
        return baseContext.createConfigurationContext(config)
    }
}
