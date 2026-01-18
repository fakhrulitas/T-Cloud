package com.android.t_cloud

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class FileAdapter(
    private val fileList: List<Document>,
    private val onClick: (Document) -> Unit,
    private val onLongClick: (Document, Int) -> Unit
) : RecyclerView.Adapter<FileAdapter.FileViewHolder>() {

    class FileViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvFileName: TextView = view.findViewById(R.id.tvFileName)
        val ivThumbnail: ImageView = view.findViewById(R.id.ivThumbnail)
        val tvFileDate: TextView = view.findViewById(R.id.tvFileDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_file, parent, false)
        return FileViewHolder(view)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val file = fileList[position]
        holder.tvFileName.text = file.file_name ?: "Tanpa Nama"
        val fileName = file.file_name?.lowercase() ?: ""

        // Menggunakan ikon asli punyamu
        val iconRes = when {
            file.isFolder -> R.drawable.ic_baseline_folder_24
            fileName.endsWith(".pdf") -> R.drawable.ic_baseline_picture_as_pdf_24
            fileName.endsWith(".mp4") || fileName.endsWith(".mkv") -> R.drawable.ic_baseline_movie_24
            fileName.endsWith(".jpg") || fileName.endsWith(".png") || fileName.endsWith(".jpeg") -> R.drawable.ic_baseline_image_24
            else -> R.drawable.ic_baseline_insert_drive_file_24
        }
        holder.ivThumbnail.setImageResource(iconRes)

        // Logika Warna
        val iconColor = when {
            file.isFolder -> Color.parseColor("#F1BD42")
            fileName.endsWith(".pdf") -> Color.RED
            fileName.endsWith(".mp4") -> Color.BLUE
            fileName.endsWith(".jpg") || fileName.endsWith(".png") -> Color.parseColor("#4CAF50")
            else -> Color.GRAY
        }
        holder.ivThumbnail.setColorFilter(iconColor)

        if (file.isFolder) {
            holder.tvFileDate.text = "Folder"
        } else {
            val currentDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
            holder.tvFileDate.text = currentDate
        }

        holder.itemView.setOnClickListener { onClick(file) }
        holder.itemView.setOnLongClickListener {
            onLongClick(file, position)
            true
        }
    }

    override fun getItemCount(): Int = fileList.size
}