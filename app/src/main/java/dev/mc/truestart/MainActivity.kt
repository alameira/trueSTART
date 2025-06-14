package dev.mc.truestart

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

// TODO: !Implement MVVM architecture!

class MainActivity : AppCompatActivity() {

    private lateinit var addAlarmButton: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AlarmAdapter
    private lateinit var alarmManager: AlarmManager
    private lateinit var resultLauncher: ActivityResultLauncher<Intent>

    private var alarms = mutableListOf<String>()

    private val tag = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        alarmManager = AlarmManager(this)

        addAlarmButton = findViewById(R.id.addAlarmButton)
        recyclerView = findViewById(R.id.recyclerView)
        adapter = AlarmAdapter(alarms) { alarm, position ->
            alarmManager.cancelAlarm(position)
            alarmManager.removeAlarmFromStore(position)

            alarms.removeAt(position)
            adapter.notifyDataSetChanged()
            Toast.makeText(this, "Deleted alarm: $alarm", Toast.LENGTH_SHORT).show()
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        loadAlarms()

        resultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
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

    override fun onResume() {
        super.onResume()
        checkPermissions()
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasAlarmPermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasAlarmPermission) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
            }
        }
    }

    private fun loadAlarms() {
        alarms.clear()

        val jsonArray = alarmManager.getAlarmsFromStore()
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

    private fun startRequestWorker() {
        val connectedConstraint = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val periodicWorkRequest = PeriodicWorkRequestBuilder<WeatherWorker>(15, TimeUnit.MINUTES)
            .setConstraints(connectedConstraint)
            .build()
        val oneTimeRequest = OneTimeWorkRequestBuilder<WeatherWorker>()
            .setConstraints(connectedConstraint)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "weather-checker-periodic",
            ExistingPeriodicWorkPolicy.KEEP,
            periodicWorkRequest
        )
        WorkManager.getInstance(this).enqueueUniqueWork(
            "weather-checker-initial",
            ExistingWorkPolicy.KEEP,
            oneTimeRequest
        )
    }
}
