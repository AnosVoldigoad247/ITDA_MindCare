package com.example.itdamindcare

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.itdamindcare.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        replaceFragment(Beranda())

        binding.bottomNavigationView.setOnItemSelectedListener{
            when (it.itemId) {
                R.id.beranda -> replaceFragment(Beranda())
                R.id.riwayat -> replaceFragment(Riwayat())
                R.id.profil -> replaceFragment(Profile())
                R.id.pengaturan -> replaceFragment(Pengaturan())

                else -> {
                    replaceFragment(Beranda())
                }
            }
            true
        }
    }
    private fun replaceFragment(fragment: Fragment) {
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.frame_layout,fragment)
        fragmentTransaction.commit()
    }
}