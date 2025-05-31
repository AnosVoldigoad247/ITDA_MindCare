package com.example.itdamindcare // Pastikan package ini sesuai dengan aplikasi Anda

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
// import androidx.work.OneTimeWorkRequest // Uncomment jika Anda benar-benar menggunakan WorkManager
// import androidx.work.WorkManager        // Uncomment jika Anda benar-benar menggunakan WorkManager
// import androidx.work.Worker             // Uncomment jika Anda benar-benar menggunakan WorkManager
// import androidx.work.WorkerParameters   // Uncomment jika Anda benar-benar menggunakan WorkManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "From: ${remoteMessage.from}")

        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
            val titleFromData = remoteMessage.data["title"]
            val bodyFromData = remoteMessage.data["body"]
            if (!titleFromData.isNullOrEmpty() && !bodyFromData.isNullOrEmpty()) {
                sendNotification(titleFromData, bodyFromData)
            }
        }

        remoteMessage.notification?.let { notification ->
            Log.d(TAG, "Message Notification Title: ${notification.title}")
            Log.d(TAG, "Message Notification Body: ${notification.body}")

            sendNotification(notification.title ?: getString(R.string.default_notification_title), notification.body ?: "")
        }
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")
        sendRegistrationToServer(token)
    }

    private fun sendRegistrationToServer(token: String?) {
        Log.d(TAG, "sendRegistrationTokenToServer($token)")
    }

    private fun sendNotification(title: String, messageBody: String) {
        val intent = Intent(this, MainActivity::class.java) // Activity yang dibuka saat notifikasi diklik
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val requestCode = System.currentTimeMillis().toInt() // Request code unik untuk PendingIntent

        val pendingIntentFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT
        } else {
            PendingIntent.FLAG_ONE_SHOT
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            requestCode,
            intent,
            pendingIntentFlag
        )

        val channelId = getString(R.string.default_notification_channel_id)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = getString(R.string.default_notification_channel_name)
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notificationId = System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    companion object {
        private const val TAG = "MyFirebaseMsgService"
    }

}