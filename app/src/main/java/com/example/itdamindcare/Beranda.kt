package com.example.itdamindcare

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import com.example.itdamindcare.databinding.FragmentBerandaBinding
import android.os.Handler
import android.os.Looper
import android.content.Context
import android.util.DisplayMetrics
import android.util.Log
import android.widget.Toast
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView

// Tambahan Firebase
import com.google.firebase.database.*
import com.google.firebase.auth.FirebaseAuth


data class Quote(val text: String = "")

class Beranda : Fragment() {

    private var _binding: FragmentBerandaBinding? = null
    private val binding get() = _binding!!

    private lateinit var quotesAdapter: QuotesAdapter
    private val dailyQuotesList = mutableListOf<Quote>()

    // Firebase Realtime Database instance
    private val realtimeDatabase = FirebaseDatabase.getInstance()
    private var quotesListener: ValueEventListener? = null

    // Auto scroll
    private val autoScrollHandler = Handler(Looper.getMainLooper())
    private lateinit var autoScrollRunnable: Runnable
    private val AUTO_SCROLL_DELAY_MS = 6000L
    private var currentPosition = 0
    private lateinit var slowVerticalLayoutManager: SlowScrollLinearLayoutManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBerandaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inisialisasi layout manager
        slowVerticalLayoutManager = SlowScrollLinearLayoutManager(
            requireContext(),
            RecyclerView.VERTICAL,
            false
        )
        binding.quotes?.layoutManager = slowVerticalLayoutManager

        quotesAdapter = QuotesAdapter(dailyQuotesList)
        binding.quotes?.adapter = quotesAdapter

        val snapHelper = PagerSnapHelper()
        snapHelper.attachToRecyclerView(binding.quotes)

        quotes()
        namaPengguna() // ← Tambahan untuk ambil nama user
    }

    private fun namaPengguna() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val uid = currentUser.uid
            val userRef = realtimeDatabase.getReference("users").child(uid).child("nama")

            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // Pastikan binding masih ada sebelum digunakan
                    _binding?.let { // Gunakan safe call dan _binding
                        val nama = snapshot.getValue(String::class.java)
                        if (!nama.isNullOrEmpty()) {
                            it.tvNama?.text = nama // Gunakan 'it' (merujuk ke _binding yang non-null)
                        } else {
                            it.tvNama?.text = "Pengguna"
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FirebaseError", "Gagal memuat nama: ${error.message}", error.toException())
                    // Periksa context sebelum menampilkan Toast jika Fragment mungkin sudah detached
                    if (isAdded && context != null) {
                        Toast.makeText(requireContext(), "Gagal memuat nama pengguna", Toast.LENGTH_SHORT).show()
                    }
                }
            })
        } else {
            // Pastikan binding masih ada sebelum digunakan
            _binding?.let { // Gunakan safe call dan _binding
                it.tvNama?.text = "Belum login"
            }
        }
    }

    private fun quotes() {
        val quotesRef = realtimeDatabase.getReference("quotes")

        quotesListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                dailyQuotesList.clear()
                for (quoteSnapshot in snapshot.children) {
                    val text = quoteSnapshot.child("text").getValue(String::class.java)
                    if (!text.isNullOrEmpty()) {
                        dailyQuotesList.add(Quote(text))
                    }
                }

                dailyQuotesList.shuffle()
                quotesAdapter.notifyDataSetChanged()

                if (dailyQuotesList.isNotEmpty()) {
                    setupAutoScroll()
                    autoScrollHandler.removeCallbacks(autoScrollRunnable)
                    autoScrollHandler.postDelayed(autoScrollRunnable, AUTO_SCROLL_DELAY_MS)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "Gagal mengambil data quotes: ${error.message}", error.toException())
                Toast.makeText(requireContext(), "Gagal memuat kutipan. Silakan coba lagi.", Toast.LENGTH_SHORT).show()
            }
        }

        quotesRef.addValueEventListener(quotesListener!!)
    }

    private fun setupAutoScroll() {
        autoScrollRunnable = object : Runnable {
            override fun run() {
                // Tambahkan pengecekan untuk _binding juga
                if (quotesAdapter.itemCount == 0 || !isAdded || _binding == null) {
                    autoScrollHandler.removeCallbacks(this) // Hentikan jika tidak valid
                    return
                }

                // Gunakan _binding dengan aman setelah pengecekan
                val currentBinding = _binding!! // Aman karena sudah dicek null di atas

                val snapView = currentBinding.quotes?.let { rv ->
                    rv.layoutManager?.let { lm -> PagerSnapHelper().findSnapView(lm) }
                }
                currentPosition = snapView?.let { slowVerticalLayoutManager.getPosition(it) } ?: currentPosition
                currentPosition = (currentPosition + 1) % quotesAdapter.itemCount
                currentBinding.quotes?.smoothScrollToPosition(currentPosition)
                autoScrollHandler.postDelayed(this, AUTO_SCROLL_DELAY_MS)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (dailyQuotesList.isNotEmpty() && ::autoScrollRunnable.isInitialized) {
            autoScrollHandler.removeCallbacks(autoScrollRunnable)
            autoScrollHandler.postDelayed(autoScrollRunnable, AUTO_SCROLL_DELAY_MS)
        }
    }

    override fun onPause() {
        super.onPause()
        if (::autoScrollRunnable.isInitialized) {
            autoScrollHandler.removeCallbacks(autoScrollRunnable)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::autoScrollRunnable.isInitialized) {
            autoScrollHandler.removeCallbacks(autoScrollRunnable)
        }
        quotesListener?.let {
            realtimeDatabase.getReference("quotes").removeEventListener(it)
        }
        _binding = null
    }
}

// LayoutManager kustom agar scroll-nya lambat
class SlowScrollLinearLayoutManager(context: Context, orientation: Int, reverseLayout: Boolean) :
    LinearLayoutManager(context, orientation, reverseLayout) {

    private val MILLISECONDS_PER_INCH = 300f

    override fun smoothScrollToPosition(
        recyclerView: RecyclerView,
        state: RecyclerView.State,
        position: Int
    ) {
        val linearSmoothScroller = object : LinearSmoothScroller(recyclerView.context) {
            override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics): Float {
                return MILLISECONDS_PER_INCH / displayMetrics.densityDpi
            }
        }
        linearSmoothScroller.targetPosition = position
        startSmoothScroll(linearSmoothScroller)
    }
}
