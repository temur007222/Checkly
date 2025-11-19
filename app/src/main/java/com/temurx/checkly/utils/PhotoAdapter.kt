package com.temurx.checkly.utils

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.temurx.checkly.databinding.PhotoItemBinding

class PhotoAdapter(
    private val photos: MutableList<Uri>,
    private val onPhotoDeleted: ((String) -> Unit)? = null
) : RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder>() {

    inner class PhotoViewHolder(val binding: PhotoItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val binding = PhotoItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PhotoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val uri = photos[position]
        holder.binding.photoImage.setImageURI(uri)

        holder.binding.deleteIcon.setOnClickListener {
            val photoUrl = uri.toString()
            photos.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, photos.size)

            // Notify fragment to remove from Firestore
            onPhotoDeleted?.invoke(photoUrl)
        }
    }

    override fun getItemCount(): Int = photos.size

    fun addPhoto(uri: Uri) {
        photos.add(uri)
        notifyItemInserted(photos.size - 1)
    }

    fun updatePhotos(photoUrls: List<String>) {
        photos.clear()
        // Convert string URLs to URIs
        val uris = photoUrls.mapNotNull { url ->
            try {
                Uri.parse(url)
            } catch (e: Exception) {
                null
            }
        }
        photos.addAll(uris)
        notifyDataSetChanged()
    }

    fun getPhotos(): List<Uri> = photos.toList()

    fun clearPhotos() {
        photos.clear()
        notifyDataSetChanged()
    }
}