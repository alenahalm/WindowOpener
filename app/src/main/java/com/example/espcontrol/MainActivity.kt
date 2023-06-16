package com.example.espcontrol

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import com.example.espcontrol.databinding.ActivityMainBinding
import okhttp3.*
import okhttp3.internal.notify
import org.json.JSONObject
import java.io.IOException
import java.util.*
import kotlin.concurrent.schedule

class MainActivity : AppCompatActivity() {

    private lateinit var request: Request
    private lateinit var binding: ActivityMainBinding
    private lateinit var pref: SharedPreferences
    val client = OkHttpClient()

    private val CHANNEL_ID = "channel_id"
    private val notificationId = 101

    var isNormal = true

    lateinit var handler: Handler


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        pref = getSharedPreferences("MyPref", MODE_PRIVATE)

        createNotificationChannel()
        binding.open.setOnClickListener {
            post("open")
        }
        binding.close.setOnClickListener {
            post("close")
        }
        handler = Handler(Looper.getMainLooper())
        handler.post(object: Runnable{
            override fun run() {
                post("data")
                Log.d("mytag", "running...${binding.tvCO2.text}")
                handler.postDelayed(this, 1000)
            }
        })

    }


    private fun saveIP(ip: String) {
        val editor = pref.edit()
        editor.putString("ip", ip)
        editor.apply()
    }

    private fun post(post: String) {
        var resp = ""
        request = Request.Builder().url("http://192.168.61.182/$post").build()
        if (post == "open" || post == "close") {
            request = Request.Builder().url("http://192.168.61.171/$post").build()
        }
        try {
            client.newCall(request).enqueue(object: Callback{
                override fun onFailure(call: Call, e: IOException) {
                    Log.d("mytag", "Failure")
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        if (!response.isSuccessful) {
                            Log.d("mytag", "HTTP ERROR")
                        } else {
                            val body = response?.body?.string().toString()
                            resp = body
                            putValues(resp)
                        }
                    }
                }
            })
        } catch (i: IOException) {}
    }

    private fun putValues(jsonString: String) {
        val temp = JSONObject(jsonString).getInt("temp")
        val co2 = JSONObject(jsonString).getInt("co2")

        if (isNormal && (co2 > 800 || temp > 30) ) {
            sendNotification("Превышение CO2 в помещении! Срочно откройте окно")
            isNormal = false
        }
        if (!isNormal && co2 < 800 && temp < 30) {
            sendNotification("Содержание CO2 снова в норме. Можете закрыть окно")
            isNormal = true
        }

        binding.tvTemp.text = temp.toString() + " °C"
        binding.tvCO2.text = co2.toString() + " ppm"
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Notification Title"
            val descriptionText = "Notification Description"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)

        }
    }
    private fun sendNotification(text: String) {
        Log.d("mytag", "SEND NOTIFICATION")
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.icon)
            .setContentTitle(resources.getString(R.string.app_name))
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        with(NotificationManagerCompat.from(this)) {
            notify(notificationId, builder.build())
        }
    }
}