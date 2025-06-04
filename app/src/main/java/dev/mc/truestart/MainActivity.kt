package dev.mc.truestart

import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var addAlarmButton: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AlarmAdapter
    private var alarms = mutableListOf<String>()

    private val prefsKey = "alarms_list"
    private val prefsName = "AlarmPrefs"
    private val tag = "MainActivity"

    private lateinit var resultLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        addAlarmButton = findViewById(R.id.addAlarmButton)
        recyclerView = findViewById(R.id.recyclerView)
        adapter = AlarmAdapter(alarms) { alarm, position ->
            cancelAlarm(alarm)
            alarms.removeAt(position)
            adapter.notifyDataSetChanged()
            removeAlarmFromPreferences(position)
            Toast.makeText(this, "Deleted alarm: $alarm", Toast.LENGTH_SHORT).show()
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        loadAlarms()

        resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                loadAlarms()
            }
        }
        addAlarmButton.setOnClickListener {
            val intent = Intent(this, InputActivity::class.java)
            resultLauncher.launch(intent)
        }

        startRequestWorker()
    }

    private fun loadAlarms() {
        alarms.clear()

        val prefs = getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val jsonString = prefs.getString(prefsKey, null) ?: return
        val jsonArray = JSONArray(jsonString)
        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(i)
            val alarmString = StringBuilder()
            for (key in jsonObject.keys()) {
                alarmString.append("$key: ${jsonObject.getString(key)}\n")
            }
            alarms.add(alarmString.toString())
        }
        adapter.notifyDataSetChanged()
    }

    private fun cancelAlarm(alarm: String) {
        val regex = Regex("alarmTime: (.*)\\n")
        val alarmTime = regex.find(alarm)?.groups?.get(1)?.value
        if (alarmTime != null) {
            val (hour, minute) = alarmTime.split(":").map { it.toInt() }
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val intent = Intent(this, AlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                this,
                calendar.timeInMillis.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent)
        }
    }

    private fun startRequestWorker() {
        val workRequest = PeriodicWorkRequestBuilder<WeatherWorker>(15, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "weather-checker",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    private fun removeAlarmFromPreferences(position: Int) {
        val prefs = getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val jsonArray = JSONArray(prefs.getString(prefsKey, "[]"))
        if (jsonArray.length() > 0) {
            jsonArray.remove(position)
            prefs.edit { putString(prefsKey, jsonArray.toString()) }
        }
    }
}
