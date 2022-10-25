package com.monolithdragon.yoochat.adapters

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.monolithdragon.yoochat.databinding.ItemContainerUserBinding
import com.monolithdragon.yoochat.listeners.UserListener
import com.monolithdragon.yoochat.models.User

class UserAdapter(private var users: List<User>, private val userListener: UserListener) :
        RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    inner class UserViewHolder(private val binding: ItemContainerUserBinding) :
            RecyclerView.ViewHolder(binding.root) {
        fun bind(user: User, userListener: UserListener) {
            binding.textName.text = user.name
            binding.textEmail.text = user.email
            binding.imageProfile.setImageBitmap(getUserImage(user.profileImage))
            binding.root.setOnClickListener {
                userListener.onClickListener(user)
            }
        }

        private fun getUserImage(encodedImage: String?): Bitmap {
            val bytes = Base64.decode(encodedImage, Base64.DEFAULT)
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = ItemContainerUserBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(users[position], userListener)
    }

    override fun getItemCount(): Int = users.size

}