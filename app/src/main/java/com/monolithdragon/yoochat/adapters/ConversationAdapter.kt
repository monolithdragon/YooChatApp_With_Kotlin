package com.monolithdragon.yoochat.adapters

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.monolithdragon.yoochat.databinding.ItemContainerConversationBinding
import com.monolithdragon.yoochat.listeners.UserListener
import com.monolithdragon.yoochat.models.Conversation
import com.monolithdragon.yoochat.models.User

class ConversationAdapter(private val conversations: MutableList<Conversation>, private val conversationListener: UserListener): RecyclerView.Adapter<ConversationAdapter.ConversationViewHolder>() {

    inner class ConversationViewHolder(private val binding: ItemContainerConversationBinding) :
            RecyclerView.ViewHolder(binding.root) {

        fun bind(conversation: Conversation, conversationListener: UserListener) {
            binding.imageProfile.setImageBitmap(getUserImage(conversation.conversationImage))
            binding.textName.text = conversation.conversationName
            binding.textRecentMessage.text = conversation.conversationMessage?.message

            binding.root.setOnClickListener {
                val user = User()
                user.id = conversation.conversationId
                user.name = conversation.conversationName
                user.profileImage = conversation.conversationImage

                conversationListener.onClickListener(user)
            }
        }

        private fun getUserImage(encodedImage: String?): Bitmap {
            val bytes = Base64.decode(encodedImage, Base64.DEFAULT)
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationViewHolder {
        val view = ItemContainerConversationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return ConversationViewHolder(view)
    }

    override fun onBindViewHolder(holder: ConversationViewHolder, position: Int) {
        holder.bind(conversations[position], conversationListener)
    }

    override fun getItemCount(): Int = conversations.size
}