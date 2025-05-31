package com.example.itdamindcare

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database

class Login : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference // Tambahkan referensi database

    private lateinit var etNimLogin: EditText // Pengguna akan input NIM di sini
    private lateinit var etPasswordLogin: EditText
    private lateinit var btnLogin: Button
    private lateinit var tvDaftar: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login) // Pastikan layout login Anda meminta NIM

        auth = Firebase.auth
        database = Firebase.database.reference.child("users") // Referensi ke node "users"

        if (auth.currentUser != null) {
            navigateToMainActivity()
            return
        }

        etNimLogin = findViewById(R.id.etNim) // Pastikan ID ini untuk input NIM di XML
        etPasswordLogin = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.login)
        tvDaftar = findViewById(R.id.btnDaftar)

        btnLogin.setOnClickListener {
            val nimInput = etNimLogin.text.toString().trim()
            val passwordInput = etPasswordLogin.text.toString().trim()

            if (nimInput.isEmpty() || passwordInput.isEmpty()) {
                Toast.makeText(this, "NIM dan Password harus diisi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnLogin.isEnabled = false
            btnLogin.text = "Mencari pengguna..."

            // Langkah 1: Cari email berdasarkan NIM di Realtime Database
            queryEmailByNim(nimInput) { emailFromDb ->
                if (emailFromDb != null) {
                    btnLogin.text = "Logging in..."
                    // Langkah 2: Lakukan sign-in dengan email yang ditemukan
                    auth.signInWithEmailAndPassword(emailFromDb, passwordInput)
                        .addOnCompleteListener(this) { task ->
                            btnLogin.isEnabled = true
                            btnLogin.text = getString(R.string.login_button) // Sesuaikan dengan string Anda

                            if (task.isSuccessful) {
                                Log.d("LoginActivity", "signInWithEmail:success")
                                Toast.makeText(baseContext, "Login berhasil.", Toast.LENGTH_SHORT).show()
                                navigateToMainActivity()
                            } else {
                                Log.w("LoginActivity", "signInWithEmail:failure", task.exception)
                                Toast.makeText(baseContext, "Login gagal: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                } else {
                    // NIM tidak ditemukan di database
                    btnLogin.isEnabled = true
                    btnLogin.text = getString(R.string.login_button) // Sesuaikan
                    Toast.makeText(this, "NIM tidak terdaftar atau terjadi kesalahan.", Toast.LENGTH_LONG).show()
                    Log.w("LoginActivity", "NIM not found in database: $nimInput")
                }
            }
        }

        tvDaftar.setOnClickListener {
            val intent = Intent(this, Daftar::class.java)
            startActivity(intent)
        }
    }

    private fun queryEmailByNim(nim: String, callback: (String?) -> Unit) {
        // Query untuk mencari user dengan nim yang cocok
        // Kita mengasumsikan struktur data di Realtime Database adalah "users" -> UID -> {nama, nim, email}
        // Karena kita tidak tahu UID-nya, kita perlu melakukan query pada field 'nim'
        // Ini bisa kurang efisien jika data sangat besar, pertimbangkan struktur data yang dioptimalkan untuk query ini jika perlu.

        database.orderByChild("nim").equalTo(nim)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        // NIM ditemukan, ambil emailnya
                        // Diasumsikan NIM unik, jadi kita ambil yang pertama
                        for (userSnapshot in snapshot.children) {
                            val user = userSnapshot.getValue(Daftar.UserData::class.java) // Gunakan UserData class dari Daftar.kt
                            callback(user?.email) // Panggil callback dengan email yang ditemukan
                            return // Keluar setelah menemukan yang pertama
                        }
                    } else {
                        // NIM tidak ditemukan
                        callback(null)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w("LoginActivity", "Database query cancelled or failed.", error.toException())
                    Toast.makeText(baseContext, "Gagal mengambil data: ${error.message}", Toast.LENGTH_SHORT).show()
                    callback(null) // Panggil callback dengan null jika ada error
                }
            })
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}