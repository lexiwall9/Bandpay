package com.example.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.R

object AdminNotificationManager {
    private const val CHANNEL_ID = "registration_requests"

    fun showRegistrationRequest(context: Context, name: String) {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Solicitudes de registro",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply { description = "Solicitudes que requieren aprobación" }
            )
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Nueva solicitud de registro")
            .setContentText("$name espera la aprobación de su cuenta.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context)
            .notify(name.hashCode(), notification)
    }
}
