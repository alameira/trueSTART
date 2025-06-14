package dev.mc.truestart

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.time.Instant

class WeatherWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    private val tag = "WeatherWorker"

    private val alarmManager = AlarmManager(context)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val jsonArray = alarmManager.getAlarmsFromStore()

            for (i in 0 until jsonArray.length()) {
                val alarm = jsonArray.getJSONObject(i)
                val alarmTimeMillis = alarmManager.getTimeMillisFromFormattedString(alarm.getString("alarmTime"))
                val forecastTimeMillis = alarmManager.getTimeMillisFromFormattedString(alarm.getString("time"))

                val now = Instant.now()
                if (alarmTimeMillis != null && forecastTimeMillis != null &&
                    Instant.ofEpochMilli(alarmTimeMillis).isAfter(now) &&
                    Instant.ofEpochMilli(forecastTimeMillis).isAfter(now)) {

                    val client = OkHttpClient()
                    val urlBuilder = HttpUrl.Builder()
                        .scheme("https")
                        .host("<base-url>")
                        .addPathSegment("<endpoint>")

                    for (key in alarm.keys()) {
                        urlBuilder.addQueryParameter(key, alarm.getString(key))
                    }

                    val request = Request.Builder()
                        .url(urlBuilder.build())
                        .addHeader("x-apikey", "<api-key>")
                        .get()
                        .build()

                    Log.d(tag, "Making request for alarm: $alarm")

                    val response = client.newCall(request).execute()
                    if (!response.isSuccessful) return@withContext Result.retry()

                    val body = response.body?.string() ?: return@withContext Result.retry()
                    val json = JSONObject(body)

                    val result = json.getBoolean("result")
                    if (result) {
                        alarmManager.scheduleAlarm(alarm)
                    } else {
                        alarmManager.cancelAlarm(i)
                    }
                    Log.d(tag, "trueSTART API returned: $result")
                } else {
                    Log.d(tag, "Old alarm: $alarm")
                }
            }

            Result.success()
        } catch (e: Exception) {
            e.message?.let { Log.e(tag, it) }
            Result.retry()
        }
    }
}
