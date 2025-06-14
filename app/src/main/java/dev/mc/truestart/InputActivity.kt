package dev.mc.truestart

import android.annotation.SuppressLint
import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.util.Calendar

class InputActivity: AppCompatActivity() {

    private lateinit var alarmTime: EditText
    private lateinit var forecastTime: EditText
    private lateinit var featureName: Spinner
    private lateinit var featureThreshold: EditText
    private lateinit var probability: EditText
    private lateinit var operator: Spinner
    private lateinit var scheduleAlarm: Button
    private lateinit var alarmManager: AlarmManager

    private val tag = "InputActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_input)

        alarmManager = AlarmManager(this)

        alarmTime = findViewById(R.id.alarmTime)
        forecastTime = findViewById(R.id.forecastTime)
        featureName = findViewById(R.id.featureName)
        featureThreshold = findViewById(R.id.featureThreshold)
        probability = findViewById(R.id.probability)
        operator = findViewById(R.id.operator)
        scheduleAlarm = findViewById(R.id.scheduleAlarm)

        alarmTime.setOnClickListener {
            pickDateTime(alarmTime)
        }
        forecastTime.setOnClickListener {
            pickDateTime(forecastTime)
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
        alarmManager.addAlarmToStore(jsonObject)

        Toast.makeText(this, "Alarm set for $jsonObject", Toast.LENGTH_SHORT).show()
    }

    private fun pickDateTime(editTextView: EditText) {
        val currentDateTime = Calendar.getInstance()
        val startYear = currentDateTime.get(Calendar.YEAR)
        val startMonth = currentDateTime.get(Calendar.MONTH)
        val startDay = currentDateTime.get(Calendar.DAY_OF_MONTH)
        val startHour = currentDateTime.get(Calendar.HOUR_OF_DAY)
        val startMinute = currentDateTime.get(Calendar.MINUTE)

        DatePickerDialog(this, { _, year, month, day ->
            TimePickerDialog(this, { _, hour, minute ->
                val pickedDateTime = Calendar.getInstance()
                pickedDateTime.set(year, month, day, hour, minute)

                editTextView.setText(alarmManager.getFormattedTimeString(pickedDateTime.time))
            }, startHour, startMinute, false).show()
        }, startYear, startMonth, startDay).show()
    }
}