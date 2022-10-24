package com.monolithdragon.yoochat.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.icu.text.SimpleDateFormat
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Base64
import android.view.View
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.monolithdragon.yoochat.adapters.MessageAdapter
import com.monolithdragon.yoochat.databinding.ActivityChatBinding
import com.monolithdragon.yoochat.models.Message
import com.monolithdragon.yoochat.models.User
import com.monolithdragon.yoochat.utilities.Constants
import com.monolithdragon.yoochat.utilities.PreferenceManager
import java.time.Instant
import java.util.Date

class ChatActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChatBinding
    private lateinit var database: FirebaseFirestore
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var senderId: String
    private lateinit var receiverUser: User

    private var messages: MutableList<Message> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initFields()
        setListener()
        loadReceiverUserDetails()
        listenMessages()
    }

    private fun initFields() {
        database = Firebase.firestore
        preferenceManager = PreferenceManager(this@ChatActivity)
        senderId = preferenceManager.getString(Constants.KEY_USER_ID)!!
    }

    private fun setListener() {
        binding.imageBack.setOnClickListener {
            switchBackToMainActivity()
        }

        binding.layoutSend.setOnClickListener {
            if (binding.inputMessage.text.isNotEmpty()) {
                sendMessage()
            }
        }
    }

    private fun loadReceiverUserDetails() {
        receiverUser = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Constants.KEY_RECEIVER_USER, User::class.java)!!
        } else {
            intent.getParcelableExtra(Constants.KEY_RECEIVER_USER)!!
        }

        binding.textName.text = receiverUser.name
    }

    private fun sendMessage() {
        val chatMessages = hashMapOf(
            Constants.KEY_CHAT_MESSAGE to binding.inputMessage.text.toString(),
            Constants.KEY_CHAT_SENDER_ID to senderId,
            Constants.KEY_CHAT_RECEIVER_ID to receiverUser.id,
            Constants.KEY_CHAT_CREATE_AT to Date.from(Instant.now())
        )

        database.collection(Constants.KEY_COLLECTION_MESSAGES)
            .add(chatMessages)

        binding.inputMessage.text = null
    }

    private fun listenMessages() {
        checkMessageRemotely(
            senderId,
            receiverUser.id
        )

        checkMessageRemotely(
            receiverUser.id,
            senderId
        )
    }

    private fun checkMessageRemotely(senderId: String?, receiverId: String?) {
        database.collection(Constants.KEY_COLLECTION_MESSAGES)
            .whereEqualTo(Constants.KEY_CHAT_SENDER_ID, senderId)
            .whereEqualTo(Constants.KEY_CHAT_RECEIVER_ID, receiverId)
            .addSnapshotListener(eventListener)
    }

    @SuppressLint("NotifyDataSetChanged")
    private val eventListener = EventListener<QuerySnapshot> { value, error ->
        if (error != null) {
            return@EventListener
        }

        if (value != null) {
            val count = messages.size

            for (documentumChange in value.documentChanges) {
                if (documentumChange.type == DocumentChange.Type.ADDED) {
                    val chatMessage = Message()
                    chatMessage.message = documentumChange.document.getString(Constants.KEY_CHAT_MESSAGE)
                    chatMessage.createAt =
                        getReadableDateTime(documentumChange.document.getDate(Constants.KEY_CHAT_CREATE_AT))
                    chatMessage.senderId =
                        documentumChange.document.getString(Constants.KEY_CHAT_SENDER_ID)
                    chatMessage.receiverId =
                        documentumChange.document.getString(Constants.KEY_CHAT_RECEIVER_ID)
                    chatMessage.updateAt = documentumChange.document.getDate(Constants.KEY_CHAT_CREATE_AT)
                    messages.add(chatMessage)
                }
            }

            if (messages.isNotEmpty()) {
                messages.sort()

                val adapter = MessageAdapter(messages, senderId, getBitmapFromEncoded(receiverUser.profileImage)!!)
                binding.chatRecyclerView.adapter = adapter

                if (count == 0) {
                    adapter.notifyDataSetChanged()
                } else {
                    adapter.notifyItemRangeInserted(messages.size, messages.size)
                    binding.chatRecyclerView.smoothScrollToPosition(messages.size - 1)
                }

                binding.chatRecyclerView.visibility = View.VISIBLE
            }

        }

        binding.progressBar.visibility = View.GONE
    }

    private fun getBitmapFromEncoded(encodedImage: String?): Bitmap? {
        if (encodedImage!!.isNotEmpty()) {
            val bytes = Base64.decode(encodedImage, Base64.DEFAULT)
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }

        return null
    }

    @SuppressLint("SimpleDateFormat")
    private fun getReadableDateTime(date: Date?): String? {
        return SimpleDateFormat("MM-dd-yyyy HH:mm").format(date)
    }

    private fun switchBackToMainActivity() {
        val intent = Intent(this@ChatActivity, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK.or(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
    }

}