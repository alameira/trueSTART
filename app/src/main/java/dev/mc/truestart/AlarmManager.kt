package dev.mc.truestart

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date

class AlarmManager(private val context: Context) {

    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'")

    private val prefsKey = "alarms_list"
    private val prefsName = "AlarmPrefs"
    private val tag = "AlarmManager"

    private val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    fun scheduleAlarm(alarm: JSONObject) {
        val alarmTime = alarm.getString("alarmTime")
        val alarmTimeMillis = getTimeMillisFromFormattedString(alarmTime)

        val intent = Intent(context, AlarmReceiver::class.java)
        intent.putExtra("alarmTime", alarmTime)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.getInt("id"),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarmClockInfo = AlarmManager.AlarmClockInfo(alarmTimeMillis!!, pendingIntent)
        try {
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
        } catch (e: SecurityException) {
            e.message?.let { Log.e(tag, it) }
        }
    }

    fun cancelAlarm(position: Int) {
        val alarm = getAlarmFromStore(position)

        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.getInt("id"),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
    }

    fun addAlarmToStore(alarm: JSONObject) {
        alarm.put("id", Instant.now().epochSecond)

        val jsonArray = getAlarmsFromStore()
        jsonArray.put(alarm)

        prefs.edit { putString(prefsKey, jsonArray.toString()) }
    }

    fun removeAlarmFromStore(position: Int) {
        val jsonArray = getAlarmsFromStore()
        if (jsonArray.length() > 0) {
            jsonArray.remove(position)
            prefs.edit { putString(prefsKey, jsonArray.toString()) }
        }
    }

    fun getAlarmsFromStore(): JSONArray {
        val jsonString = prefs.getString(prefsKey, null) ?: "[]"
        return JSONArray(jsonString)
    }

    fun getFormattedTimeString(time: Date): String {
        return dateTimeFormat.format(time)
    }

    fun getTimeMillisFromFormattedString(formattedTimeString: String): Long? {
        return getTimeFromFormattedString(formattedTimeString)?.time
    }

    private fun getTimeFromFormattedString(formattedTimeString: String): Date? {
        return dateTimeFormat.parse(formattedTimeString)
    }

    private fun getAlarmFromStore(position: Int): JSONObject {
        val jsonArray = getAlarmsFromStore()
        return jsonArray.getJSONObject(position)
    }
}