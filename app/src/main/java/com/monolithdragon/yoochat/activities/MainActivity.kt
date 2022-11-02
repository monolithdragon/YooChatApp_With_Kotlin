package com.monolithdragon.yoochat.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging
import com.monolithdragon.yoochat.R
import com.monolithdragon.yoochat.adapters.ConversationAdapter
import com.monolithdragon.yoochat.databinding.ActivityMainBinding
import com.monolithdragon.yoochat.listeners.UserListener
import com.monolithdragon.yoochat.models.Conversation
import com.monolithdragon.yoochat.models.Message
import com.monolithdragon.yoochat.models.User
import com.monolithdragon.yoochat.utilities.Constants
import com.monolithdragon.yoochat.utilities.PreferenceManager

class MainActivity : BaseActivity(), UserListener {
    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var database: FirebaseFirestore

    private var receiverId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initFields()
        setListeners()
        loadUserDetails()
        getToken()
        listenConversation()
    }

    private fun setListeners() {
        binding.imageSignOut.setOnClickListener {
            signOut()
        }

        binding.fabUsers.setOnClickListener {
            switchToUsersActivity()
        }
    }

    private fun initFields() {
        auth = Firebase.auth
        preferenceManager = PreferenceManager(this@MainActivity)
        database = Firebase.firestore
    }

    private fun loadUserDetails() {
        binding.textName.text = preferenceManager.getString(Constants.KEY_USER_NAME)
        binding.imageProfile.setImageBitmap(
            getUserImage(
                preferenceManager.getString(Constants.KEY_USER_IMAGE)
            )
        )
    }

    private fun getToken() {
        Firebase.messaging.token.addOnSuccessListener(this::updateToken)
    }

    private fun updateToken(token: String) {
        preferenceManager.putString(Constants.KEY_USER_TOKEN, token)

        database.collection(Constants.KEY_COLLECTION_USERS)
            .document(preferenceManager.getString(Constants.KEY_USER_ID)!!)
            .update(Constants.KEY_USER_TOKEN, token)
            .addOnFailureListener {
                showMessage(getString(R.string.unable_update_token))
            }
    }

    private fun listenConversation() {
        checkMessageRemotely(Constants.KEY_CHAT_SENDER_ID)
        checkMessageRemotely(Constants.KEY_CHAT_RECEIVER_ID)
    }

    private fun checkMessageRemotely(key: String) {
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
            .whereEqualTo(key, preferenceManager.getString(Constants.KEY_USER_ID))
            .addSnapshotListener(eventListener)
    }

    @SuppressLint("NotifyDataSetChanged")
    private val eventListener = EventListener<QuerySnapshot> { value, error ->
        if (error != null) {
            return@EventListener
        }

        if (value != null) {
            val conversations: MutableList<Conversation> = mutableListOf()

            for (documentChange in value.documentChanges) {
                if (documentChange.type == DocumentChange.Type.ADDED) {
                    val conversation = Conversation()
                    val message = Message()
                    message.message = documentChange.document.getString(Constants.KEY_CHAT_MESSAGE)
                    message.senderId =
                        documentChange.document.getString(Constants.KEY_CHAT_SENDER_ID)
                    message.receiverId =
                        documentChange.document.getString(Constants.KEY_CHAT_RECEIVER_ID)
                    message.updateAt = documentChange.document.getDate(Constants.KEY_CHAT_CREATE_AT)
                    conversation.conversationMessage = message

                    receiverId = documentChange.document.getString(Constants.KEY_CHAT_RECEIVER_ID)

                    if (preferenceManager.getString(Constants.KEY_USER_ID)
                            .equals(documentChange.document.getString(Constants.KEY_CHAT_SENDER_ID))
                    ) {
                        conversation.conversationId =
                            documentChange.document.getString(Constants.KEY_CHAT_RECEIVER_ID)
                        conversation.conversationName =
                            documentChange.document.getString(Constants.KEY_CONVERSATION_RECEIVER_NAME)
                        conversation.conversationImage =
                            documentChange.document.getString(Constants.KEY_CONVERSATION_RECEIVER_IMAGE)
                    } else {
                        conversation.conversationId =
                            documentChange.document.getString(Constants.KEY_CHAT_SENDER_ID)
                        conversation.conversationName =
                            documentChange.document.getString(Constants.KEY_CONVERSATION_SENDER_NAME)
                        conversation.conversationImage =
                            documentChange.document.getString(Constants.KEY_CONVERSATION_SENDER_IMAGE)
                    }

                    conversations.add(conversation)
                } else if (documentChange.type == DocumentChange.Type.MODIFIED) {
                    for (conversation in conversations) {
                        if (conversation.conversationMessage?.senderId.equals(documentChange.document.getString(
                                Constants.KEY_CHAT_SENDER_ID))
                            && conversation.conversationMessage?.receiverId.equals(documentChange.document.getString(
                                Constants.KEY_CHAT_RECEIVER_ID))
                        ) {
                            conversation.conversationMessage?.message =
                                documentChange.document.getString(Constants.KEY_CHAT_MESSAGE)
                            conversation.conversationMessage?.updateAt =
                                documentChange.document.getDate(Constants.KEY_CHAT_CREATE_AT)
                            break
                        }
                    }
                }
            }

            if (conversations.isNotEmpty()) {
                conversations.sort()
                val isOnline = preferenceManager.getBoolean(Constants.KEY_USER_ONLINE)!!

                val adapter = ConversationAdapter(conversations, this@MainActivity, isOnline)
                binding.conversationRecyclerView.adapter = adapter
                adapter.notifyDataSetChanged()
                binding.conversationRecyclerView.visibility = View.VISIBLE
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun signOut() {
        showMessage(getString(R.string.signout))

        val updates = hashMapOf<String, Any>(
            Constants.KEY_USER_TOKEN to FieldValue.delete()
        )

        database.collection(Constants.KEY_COLLECTION_USERS)
            .document(preferenceManager.getString(Constants.KEY_USER_ID)!!)
            .update(updates)
            .addOnSuccessListener {
                preferenceManager.clear()
                auth.signOut()
                switchToSignInActivity()
            }
            .addOnFailureListener {
                showMessage(getString(R.string.unable_to_sign_out))
            }
    }

    private fun showMessage(message: String) {
        Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
    }

    private fun switchToSignInActivity() {
        val intent = Intent(this@MainActivity, SignInActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK.or(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
    }

    private fun switchToUsersActivity() {
        val intent = Intent(this@MainActivity, UsersActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK.or(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
    }

    private fun getUserImage(encodedImage: String?): Bitmap {
        val bytes = Base64.decode(encodedImage, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    override fun onClickListener(user: User) {
        val intent = Intent(this@MainActivity, ChatActivity::class.java)
        intent.putExtra(Constants.KEY_RECEIVER_USER, user)
        startActivity(intent)
        finish()
    }
}