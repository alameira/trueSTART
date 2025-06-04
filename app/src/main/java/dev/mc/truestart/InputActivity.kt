package dev.mc.truestart

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar

class InputActivity: AppCompatActivity() {

    private lateinit var alarmTime: EditText
    private lateinit var forecastTime: EditText
    private lateinit var featureName: Spinner
    private lateinit var featureThreshold: EditText
    private lateinit var probability: EditText
    private lateinit var operator: Spinner
    private lateinit var scheduleAlarm: Button

    private val prefsKey = "alarms_list"
    private val prefsName = "AlarmPrefs"
    private val tag = "InputActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_input)

        alarmTime = findViewById(R.id.alarmTime)
        forecastTime = findViewById(R.id.forecastTime)
        featureName = findViewById(R.id.featureName)
        featureThreshold = findViewById(R.id.featureThreshold)
        probability = findViewById(R.id.probability)
        operator = findViewById(R.id.operator)
        scheduleAlarm = findViewById(R.id.scheduleAlarm)

        alarmTime.setOnClickListener {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)

            val timePicker = TimePickerDialog(this, { _, selectedHour, selectedMinute ->
                alarmTime.setText(String.format("%02d:%02d", selectedHour, selectedMinute))
            }, hour, minute, true)

            timePicker.show()
        }
        forecastTime.setOnClickListener {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)

            val timePicker = TimePickerDialog(this, { _, selectedHour, selectedMinute ->
                calendar.apply {
                    set(Calendar.HOUR_OF_DAY, selectedHour)
                    set(Calendar.MINUTE, selectedMinute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                forecastTime.setText(SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'").format(calendar.time))
            }, hour, minute, true)

            timePicker.show()
        }

        scheduleAlarm.setOnClickListener {
            var jsonObject = JSONObject()
            jsonObject.put("alarmTime", alarmTime.text.toString())
            jsonObject.put("time", forecastTime.text.toString())
            jsonObject.put("feature", featureName.selectedItem.toString())
            jsonObject.put("threshold", featureThreshold.text.toString())
            jsonObject.put("probability", probability.text.toString())
            jsonObject.put("operator", operator.selectedItem.toString())

            createAlarm(jsonObject)

            setResult(Activity.RESULT_OK)
            this.finish()
        }
    }

    @SuppressLint("ScheduleExactAlarm")
    private fun createAlarm(jsonObject: JSONObject) {
        val (selectedHour, selectedMinute) = jsonObject.getString("alarmTime").split(":").map { it.toInt() }

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, selectedHour)
            set(Calendar.MINUTE, selectedMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            if (before(Calendar.getInstance())) {
                add(Calendar.DATE, 1)
            }
        }

        Toast.makeText(this, "Alarm set for $jsonObject", Toast.LENGTH_SHORT).show()

        scheduleAlarm(calendar)

        addAlarmToPreferences(jsonObject)
    }

    private fun scheduleAlarm(calendar: Calendar) {
        val intent = Intent(this, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            calendar.timeInMillis.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarmClockInfo = AlarmManager.AlarmClockInfo(calendar.timeInMillis, pendingIntent)
        try {
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
        } catch (e: SecurityException) {
            e.message?.let { Log.e(tag, it) }
        }
    }

    private fun addAlarmToPreferences(jsonObject: JSONObject) {
        val prefs = getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val jsonArray = JSONArray(prefs.getString(prefsKey, "[]"))
        jsonArray.put(jsonObject)
        prefs.edit { putString(prefsKey, jsonArray.toString()) }
    }
}