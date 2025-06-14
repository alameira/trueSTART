package dev.mc.truestart

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.util.Log
import androidx.core.app.NotificationCompat


class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("AlarmReceiver", "Broadcast receiver called!")
        showNotification(context, intent)
        playRingtone(context)
        // TODO: open MainActivity with highlighted alarm
    }

    private fun playRingtone(context: Context) {
        // TODO: get this working in silent mode
        val alarmUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val ringtone = RingtoneManager.getRingtone(context, alarmUri)
        ringtone.play()
    }

    private fun showNotification(context: Context, intent: Intent) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (manager.areNotificationsEnabled()) {
            val channelId = "alarm_channel"

            val channel = NotificationChannel(
                channelId,
                "Alarm Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)

            val notification = NotificationCompat.Builder(context, channelId)
                .setContentTitle("Alarm Triggered")
                .setContentText("It's trueSTART time -> " + intent.getStringExtra("alarmTime"))
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()

            manager.notify(System.currentTimeMillis().toInt(), notification)
        }
    }
}
