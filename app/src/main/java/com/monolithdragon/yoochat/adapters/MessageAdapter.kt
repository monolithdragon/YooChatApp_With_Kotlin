package com.monolithdragon.yoochat.adapters

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.monolithdragon.yoochat.databinding.ItemContainerReceivedMessageBinding
import com.monolithdragon.yoochat.databinding.ItemContainerSentMessageBinding
import com.monolithdragon.yoochat.models.Message

class MessageAdapter(
    private var messages: MutableList<Message>,
    private val senderId: String,
    private val profileImage: Bitmap,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val VIEW_SENT_MESSAGE = 1
        const val VIEW_RECEIVED_MESSAGE = 2
    }


    inner class SentMessageViewHolder(private val binding: ItemContainerSentMessageBinding) :
            RecyclerView.ViewHolder(binding.root) {

            fun bind(message: Message) {
                binding.textMessage.text = message.message
                binding.textDateTime.text = message.createAt
            }

    }

    inner class ReceivedMessageViewHolder(private val binding: ItemContainerReceivedMessageBinding) :
            RecyclerView.ViewHolder(binding.root) {

        fun bind(message: Message, profileImage: Bitmap) {
            binding.imageProfile.setImageBitmap(profileImage)
            binding.textMessage.text = message.message
            binding.textDateTime.text = message.createAt
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        when (viewType) {
            VIEW_SENT_MESSAGE -> {
                val view = ItemContainerSentMessageBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )

                return SentMessageViewHolder(view)
            }
            VIEW_RECEIVED_MESSAGE -> {
                val view = ItemContainerReceivedMessageBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )

                return ReceivedMessageViewHolder(view)
            }
            else -> null!!
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (getItemViewType(position) == VIEW_SENT_MESSAGE) {
            (holder as SentMessageViewHolder).bind(messages[position])
        } else {
            (holder as ReceivedMessageViewHolder).bind(messages[position], profileImage)
        }
    }

    override fun getItemViewType(position: Int): Int {
        if (messages[position].senderId == senderId) {
            return VIEW_SENT_MESSAGE
        }

        return VIEW_RECEIVED_MESSAGE
    }

    override fun getItemCount(): Int = messages.size
}