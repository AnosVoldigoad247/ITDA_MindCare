package com.example.itdamindcare

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class QuotesAdapter(private val quotesList: List<Quote>) :
    RecyclerView.Adapter<QuotesAdapter.QuoteViewHolder>() {

    // ViewHolder memegang referensi ke view untuk setiap item
    class QuoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val quoteTextView: TextView = itemView.findViewById(R.id.teksQuote) // Sesuaikan dengan ID di item_quote.xml
    }

    // Membuat ViewHolder baru (dipanggil oleh layout manager)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuoteViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.activity_quotes_adapter, parent, false) // Gunakan layout item_quote.xml
        return QuoteViewHolder(itemView)
    }

    // Mengganti konten view (dipanggil oleh layout manager)
    override fun onBindViewHolder(holder: QuoteViewHolder, position: Int) {
        val currentQuote = quotesList[position]
        holder.quoteTextView.text = currentQuote.text
        // Set data lain jika ada
    }

    // Mengembalikan ukuran dataset (dipanggil oleh layout manager)
    override fun getItemCount() = quotesList.size
}