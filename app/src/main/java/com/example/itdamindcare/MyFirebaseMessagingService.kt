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

        // Handle pesan data (opsional, jika Anda mengirim data payload untuk notifikasi)
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
            // Contoh: Jika Anda mengirim judul dan isi dari data payload
            val titleFromData = remoteMessage.data["title"]
            val bodyFromData = remoteMessage.data["body"]
            if (!titleFromData.isNullOrEmpty() && !bodyFromData.isNullOrEmpty()) {
                sendNotification(titleFromData, bodyFromData)
                return // Jika sudah ditangani dari data, mungkin tidak perlu proses notifikasi di bawah
            }
            // Logika WorkManager Anda bisa tetap di sini jika diperlukan untuk data payload
            // if (needsToBeScheduled()) {
            //     scheduleJob()
            // } else {
            //     handleNow()
            // }
        }

        // Handle pesan notifikasi (ketika aplikasi di foreground)
        // Ini akan dipanggil jika payload FCM mengandung objek "notification"
        remoteMessage.notification?.let { notification ->
            Log.d(TAG, "Message Notification Title: ${notification.title}")
            Log.d(TAG, "Message Notification Body: ${notification.body}")

            // PENTING: Panggil sendNotification agar muncul pop-up saat aplikasi di foreground
            sendNotification(notification.title ?: getString(R.string.default_notification_title), notification.body ?: "")
        }
    }

    // Fungsi ini bisa Anda hapus jika tidak menggunakan WorkManager untuk notifikasi
    // private fun needsToBeScheduled() = false // Sesuaikan jika perlu
    // private fun scheduleJob() {
    //     val work = OneTimeWorkRequest.Builder(MyWorker::class.java).build()
    //     WorkManager.getInstance(this).beginWith(work).enqueue()
    // }
    // private fun handleNow() {
    //     Log.d(TAG, "Short lived task is done.")
    // }

    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")
        sendRegistrationToServer(token)
    }

    private fun sendRegistrationToServer(token: String?) {
        // TODO: Implement this method to send token to your app server.
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

        // Gunakan ID channel dari string.xml
        val channelId = getString(R.string.default_notification_channel_id)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            // Pastikan R.mipmap.ic_launcher adalah dari package aplikasi Anda
            .setSmallIcon(com.example.itdamindcare.R.mipmap.ic_launcher) // Gunakan R dari package aplikasi Anda
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // PENTING untuk heads-up di < Oreo

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Untuk Android Oreo (API 26) ke atas, Notification Channel WAJIB dibuat
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Gunakan nama channel dari string.xml
            val channelName = getString(R.string.default_notification_channel_name)
            // PENTING: Gunakan IMPORTANCE_HIGH untuk heads-up notification
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            )
            // channel.description = "Deskripsi channel Anda" // Opsional
            notificationManager.createNotificationChannel(channel)
        }

        // Gunakan ID notifikasi yang unik agar tidak saling menimpa
        val notificationId = System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    companion object {
        private const val TAG = "MyFirebaseMsgService"
    }

    // Hapus MyWorker jika tidak digunakan
    // internal class MyWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {
    //     override fun doWork(): Result {
    //         return Result.success()
    //     }
    // }
}