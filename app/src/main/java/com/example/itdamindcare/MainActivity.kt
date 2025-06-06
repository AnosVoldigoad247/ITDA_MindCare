package com.example.itdamindcare

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.itdamindcare.databinding.ActivityMainBinding // Pastikan R di sini benar
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        intent.extras?.let {
            for (key in it.keySet()) {
                val value = intent.extras?.get(key)
                Log.d(TAG, "Key: $key Value: $value")
            }
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        replaceFragment(Beranda())
        askNotificationPermission()
        logRegToken()
        storeTokenToFirestore()

        binding.bottomNavigationView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.beranda -> replaceFragment(Beranda())
                R.id.riwayat -> replaceFragment(Riwayat())
                R.id.profil -> replaceFragment(Profile()) // Pastikan nama kelas sesuai
                R.id.pengaturan -> replaceFragment(Pengaturan())
                else -> {
                    replaceFragment(Beranda())
                }
            }
            true
        }
    }

    private fun storeTokenToFirestore() {
        lifecycleScope.launch {
            try {
                val storedToken = getAndStoreRegToken()
                Log.i(TAG, "Attempted to store FCM token. Token: $storedToken")
            } catch (e: Exception) {
                Log.e(TAG, "Error during getAndStoreRegToken call from storeTokenToFirestore", e)
                Toast.makeText(this@MainActivity, "Gagal menyimpan token FCM ke Firestore.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun runtimeEnableAutoInit() {
        Firebase.messaging.isAutoInitEnabled = true
        Log.d(TAG, "FCM auto-init enabled.")
    }

    fun subscribeTopics() {
        Firebase.messaging.subscribeToTopic("weather")
            .addOnCompleteListener { task ->
                val msg = if (task.isSuccessful) {
                    "Berhasil subscribe ke topik 'weather'"
                } else {
                    "Gagal subscribe ke topik 'weather'"
                }
                Log.d(TAG, msg)
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
    }

    fun logRegToken() {
        Firebase.messaging.getToken().addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }
            val token = task.result
            val msg = "FCM Registration token (for logging): $token"
            Log.d(TAG, msg)
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d(TAG, "Notification permission granted.")
            Toast.makeText(this, "Izin notifikasi diberikan.", Toast.LENGTH_SHORT).show()
            storeTokenToFirestore()
        } else {
            Toast.makeText(
                this,
                "Izin notifikasi ditolak. Beberapa fitur mungkin tidak berfungsi.",
                Toast.LENGTH_LONG
            ).show()
            Log.w(TAG, "Notification permission denied by user.")
        }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    Log.d(TAG, "Notification permission already granted.")
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    AlertDialog.Builder(this)
                        .setTitle("Izin Notifikasi Diperlukan")
                        .setMessage("Aplikasi ini memerlukan izin notifikasi untuk memberi Anda pembaruan penting. Izinkan?")
                        .setPositiveButton("OK") { _, _ ->
                            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        .setNegativeButton("Lain Kali") { dialog, _ ->
                            dialog.dismiss()
                            Toast.makeText(this, "Izin notifikasi tidak diberikan saat ini.", Toast.LENGTH_SHORT).show()
                        }
                        .show()
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            Log.d(TAG, "Notification permission not required for this API level or granted by default.")
        }
    }

    private suspend fun getAndStoreRegToken(): String {
        val token = Firebase.messaging.token.await()
        Log.d(TAG, "FCM Token fetched in getAndStoreRegToken: $token")

        val deviceToken = hashMapOf(
            "token" to token,
            "timestamp" to FieldValue.serverTimestamp(),
        )

        val userId = Firebase.auth.currentUser?.uid
        if (userId != null) {
            try {
                Firebase.firestore.collection("fcmTokens").document(userId)
                    .set(deviceToken).await()
                Log.d(TAG, "FCM token for user $userId stored/updated successfully in Firestore.")
            } catch (e: Exception) {
                Log.e(TAG, "Error storing FCM token for user $userId in Firestore", e)
                throw e
            }
        } else {
            Log.w(TAG, "User not logged in. FCM token for specific user NOT stored in Firestore.")
        }
        return token
    }

    private fun replaceFragment(fragment: Fragment) {
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.frame_layout, fragment)
        fragmentTransaction.commit()
    }
}