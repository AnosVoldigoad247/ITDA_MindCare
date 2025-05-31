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
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner

class Daftar : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    private lateinit var etNama: EditText
    private lateinit var etNim: EditText
    private lateinit var spinnerProdi: Spinner
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnDaftar: Button
    private lateinit var tvLogin: TextView

    private var selectedProdi: String? = null

    // Data class untuk menyimpan informasi pengguna, termasuk program studi
    data class UserData(
        val nama: String? = null,
        val nim: String? = null,
        val prodi: String? = null, // Tambahkan field prodi
        val email: String? = null
    ) {
        // Konstruktor tanpa argumen diperlukan untuk Firebase deserialization
        // Jika semua properti memiliki nilai default null, Kotlin akan otomatis men-generate-nya.
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_daftar)

        auth = Firebase.auth
        database = Firebase.database.reference

        etNama = findViewById(R.id.etNama)
        etNim = findViewById(R.id.etNim)
        spinnerProdi = findViewById(R.id.spinnerProdi) // Pastikan ID ini benar di activity_daftar.xml
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnDaftar = findViewById(R.id.daftar)
        tvLogin = findViewById(R.id.btnLogin)

        setupSpinner()

        btnDaftar.setOnClickListener {
            val nama = etNama.text.toString().trim()
            val nim = etNim.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (nama.isEmpty() || nim.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Semua field harus diisi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val promptSpinner = resources.getStringArray(R.array.program_studi_array)[0]
            if (selectedProdi == null || selectedProdi == promptSpinner) {
                Toast.makeText(this, "Silakan pilih program studi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "Password minimal 6 karakter", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnDaftar.isEnabled = false
            btnDaftar.text = "Mendaftarkan..."

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    btnDaftar.isEnabled = true
                    btnDaftar.text = getString(R.string.daftar_button)

                    if (task.isSuccessful) {
                        Log.d("DaftarActivity", "createUserWithEmail:success")
                        val firebaseUser = auth.currentUser
                        firebaseUser?.let { user ->
                            // Sertakan 'selectedProdi' saat membuat objek UserData
                            val userData = UserData(nama, nim, selectedProdi, email)
                            database.child("users").child(user.uid).setValue(userData)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Pendaftaran berhasil.", Toast.LENGTH_SHORT).show()
                                    val intent = Intent(this, Login::class.java)
                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    startActivity(intent)
                                    finish()
                                }
                                .addOnFailureListener { e ->
                                    Log.w("DaftarActivity", "Gagal menyimpan data pengguna ke database.", e)
                                    Toast.makeText(this, "Gagal menyimpan data pengguna: ${e.message}", Toast.LENGTH_LONG).show()
                                    user.delete().addOnCompleteListener { deleteTask ->
                                        if (deleteTask.isSuccessful) {
                                            Log.d("DaftarActivity", "Akun pengguna di Firebase Auth dihapus setelah gagal menyimpan data.")
                                        }
                                    }
                                }
                        }
                    } else {
                        Log.w("DaftarActivity", "createUserWithEmail:failure", task.exception)
                        if (task.exception is FirebaseAuthUserCollisionException) {
                            Toast.makeText(baseContext, "Email sudah terdaftar.", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(baseContext, "Pendaftaran gagal: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
        }

        tvLogin.setOnClickListener {
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
        }
    }

    private fun setupSpinner() {
        ArrayAdapter.createFromResource(
            this,
            R.array.program_studi_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerProdi.adapter = adapter
        }

        spinnerProdi.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                // Mendapatkan item yang dipilih.
                // Item pertama (posisi 0) adalah "Pilih Program Studi", jadi kita abaikan jika itu yang dipilih.
                if (position > 0) {
                    selectedProdi = parent.getItemAtPosition(position).toString()
                } else {
                    selectedProdi = null // Atau Anda bisa set ke nilai default jika item prompt dipilih
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Dipanggil ketika pilihan saat ini menghilang dari adapter.
                // Biasanya tidak perlu melakukan apa-apa di sini untuk kasus sederhana.
                selectedProdi = null
            }
        }
    }
}