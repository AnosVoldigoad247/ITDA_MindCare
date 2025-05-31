package com.example.itdamindcare

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class RiwayatAdapter(private val riwayatList: List<Riwayat1>) :
    RecyclerView.Adapter<RiwayatAdapter.RiwayatViewHolder>() {

    class RiwayatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTgl: TextView = itemView.findViewById(R.id.tvTgl)
        val tvPersen: TextView = itemView.findViewById(R.id.tvPersen)
        val tvHasil: TextView = itemView.findViewById(R.id.tvHasil)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RiwayatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.activity_riwayat_adapter, parent, false) // Pastikan nama file XML-nya `item_riwayat.xml`
        return RiwayatViewHolder(view)
    }

    override fun onBindViewHolder(holder: RiwayatViewHolder, position: Int) {
        val riwayat = riwayatList[position]
        holder.tvTgl.text = riwayat.tanggal
        holder.tvPersen.text = riwayat.persen
        holder.tvHasil.text = riwayat.hasil
        // Jika ingin mengatur gambar secara dinamis, bisa atur di sini
        // holder.imageView.setImageResource(R.drawable.riwayat)
    }

    override fun getItemCount(): Int = riwayatList.size
}
