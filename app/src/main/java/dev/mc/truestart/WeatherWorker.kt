package dev.mc.truestart

import android.content.Context
import android.util.Log
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

class WeatherWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    private val prefsKey = "alarms_list"
    private val tag = "WeatherWorker"

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(tag, "Calling API...")

            val prefs = applicationContext.getSharedPreferences("AlarmPrefs", Context.MODE_PRIVATE)
            val jsonArray = JSONArray(prefs.getString(prefsKey, "[]"))

            for (i in 0 until jsonArray.length()) {
                val alarm = jsonArray.getJSONObject(i)

                val client = OkHttpClient()
                val urlBuilder = HttpUrl.Builder()
                    .scheme("https")
                    .host("<api-base-url>")
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

                val value = json.getString("result")
                Log.d(tag, "trueSTART API returned: $value")
            }

            Result.success()
        } catch (e: Exception) {
            e.message?.let { Log.e(tag, it) }
            Result.retry()
        }
    }
}
