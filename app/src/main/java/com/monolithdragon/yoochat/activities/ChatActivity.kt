package com.monolithdragon.yoochat.activities

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.icu.text.SimpleDateFormat
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.monolithdragon.yoochat.R
import com.monolithdragon.yoochat.adapters.MessageAdapter
import com.monolithdragon.yoochat.api.Client
import com.monolithdragon.yoochat.databinding.ActivityChatBinding
import com.monolithdragon.yoochat.models.Message
import com.monolithdragon.yoochat.models.User
import com.monolithdragon.yoochat.utilities.Constants
import com.monolithdragon.yoochat.utilities.PreferenceManager
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.time.Instant
import java.util.*

class ChatActivity : BaseActivity() {
    private lateinit var binding: ActivityChatBinding
    private lateinit var database: FirebaseFirestore
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var senderId: String
    private lateinit var receiverUser: User

    private var messages: MutableList<Message> = mutableListOf()
    private var conversationId: String? = null
    private var isReceiverAvailable: Boolean = false

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

        binding.imageCall.setOnClickListener {
            showMessage("Click call")
        }

        binding.imageVideoCall.setOnClickListener {
            showMessage("Click video call")
        }
    }

    private fun loadReceiverUserDetails() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            receiverUser =
                intent.getParcelableExtra(Constants.KEY_RECEIVER_USER, User::class.java)!!
        }

        binding.textName.text = receiverUser.name
    }

    private fun sendMessage() {
        val message = binding.inputMessage.text.toString()
        val createAt = Date.from(Instant.now())

        val chatMessages = hashMapOf(
            Constants.KEY_CHAT_MESSAGE to message,
            Constants.KEY_CHAT_SENDER_ID to senderId,
            Constants.KEY_CHAT_RECEIVER_ID to receiverUser.id,
            Constants.KEY_CHAT_CREATE_AT to createAt
        )

        database.collection(Constants.KEY_COLLECTION_MESSAGES)
            .add(chatMessages)

        if (!conversationId.isNullOrEmpty()) {
            updateConversation(message, createAt)
        } else {
            val conversions = hashMapOf(
                Constants.KEY_CHAT_MESSAGE to message,
                Constants.KEY_CHAT_CREATE_AT to createAt,
                Constants.KEY_CHAT_SENDER_ID to senderId,
                Constants.KEY_CONVERSATION_SENDER_NAME to preferenceManager.getString(Constants.KEY_USER_NAME),
                Constants.KEY_CONVERSATION_SENDER_IMAGE to preferenceManager.getString(Constants.KEY_USER_IMAGE),
                Constants.KEY_CHAT_RECEIVER_ID to receiverUser.id,
                Constants.KEY_CONVERSATION_RECEIVER_NAME to receiverUser.name,
                Constants.KEY_CONVERSATION_RECEIVER_IMAGE to receiverUser.profileImage,
            )

            database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .add(conversions)
        }

        if (!isReceiverAvailable) {
            try {
                val tokens = JSONArray()
                tokens.put(receiverUser.token)

                val data = JSONObject()
                data.put(Constants.KEY_USER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
                data.put(Constants.KEY_USER_NAME,
                         preferenceManager.getString(Constants.KEY_USER_NAME))
                data.put(Constants.KEY_USER_TOKEN,
                         preferenceManager.getString(Constants.KEY_USER_TOKEN))
                data.put(Constants.KEY_CHAT_MESSAGE, message)

                val body = JSONObject()
                body.put(Constants.REMOTE_MESSAGE_DATA, data)
                body.put(Constants.REMOTE_MESSAGE_REGISTRATION_IDS, tokens)

                askNotificationPermission(body.toString())
            } catch (e: Exception) {
                showMessage(e.message!!)
            }
        }

        binding.inputMessage.text = null
    }

    private fun sendNotification(notification: String) {

        try {
            Client.api.sendNotification(Constants.remoteMessageHeaders, notification)
                .enqueue(object : Callback<String> {
                    override fun onResponse(
                        call: Call<String>,
                        response: Response<String>,
                    ) {
                        if (response.isSuccessful) {
                            try {
                                if (!response.body().isNullOrEmpty()) {
                                    val responseJson = JSONObject(response.body()!!)
                                    val results = responseJson.getJSONArray("results")
                                    if (responseJson.getInt("failure") == 1) {
                                        val error = results.get(0) as JSONObject
                                        return showMessage(error.getString("error"))
                                    }
                                }
                            } catch (e: JSONException) {
                                e.printStackTrace()
                            }

                            showMessage("Notification sent successfully")
                        } else {
                            showMessage("Error: " + response.code())
                        }
                    }

                    override fun onFailure(call: Call<String>, t: Throwable) {
                        showMessage(t.message!!)
                    }

                })
        } catch (e: Exception) {
            showMessage(e.message!!)
        }
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
                    chatMessage.message =
                        documentumChange.document.getString(Constants.KEY_CHAT_MESSAGE)
                    chatMessage.createAt =
                        getReadableDateTime(documentumChange.document.getDate(Constants.KEY_CHAT_CREATE_AT))
                    chatMessage.senderId =
                        documentumChange.document.getString(Constants.KEY_CHAT_SENDER_ID)
                    chatMessage.receiverId =
                        documentumChange.document.getString(Constants.KEY_CHAT_RECEIVER_ID)
                    chatMessage.updateAt =
                        documentumChange.document.getDate(Constants.KEY_CHAT_CREATE_AT)
                    messages.add(chatMessage)
                }
            }

            if (messages.isNotEmpty()) {
                messages.sort()

                val adapter = MessageAdapter(messages,
                                             senderId,
                                             getBitmapFromEncoded(receiverUser.profileImage)!!)
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

        if (conversationId == null) {
            checkForConversation()
        }
    }

    private fun checkForConversation() {
        if (messages.size > 0) {
            checkForConversionRemotely(
                senderId,
                receiverUser.id
            )

            checkForConversionRemotely(
                receiverUser.id,
                senderId
            )
        }
    }

    private fun checkForConversionRemotely(senderId: String?, receiverId: String?) {
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
            .whereEqualTo(Constants.KEY_CHAT_SENDER_ID, senderId)
            .whereEqualTo(Constants.KEY_CHAT_RECEIVER_ID, receiverId)
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful && task.result != null && task.result.documents.size != 0) {
                    conversationId = task.result.documents[0].id
                }
            }
    }

    private fun updateConversation(message: String, date: Date) {
        val updates = hashMapOf<String, Any>(
            Constants.KEY_CHAT_MESSAGE to message,
            Constants.KEY_CHAT_CREATE_AT to date
        )

        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS).document(conversationId!!)
            .update(updates)
    }

    private fun getBitmapFromEncoded(encodedImage: String?): Bitmap? {
        if (!encodedImage.isNullOrEmpty()) {
            val bytes = Base64.decode(encodedImage, Base64.DEFAULT)
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }

        return null
    }

    @SuppressLint("SimpleDateFormat")
    private fun getReadableDateTime(date: Date?): String? {
        return SimpleDateFormat("MM-dd-yyyy HH:mm").format(date)
    }

    private fun showMessage(message: String) {
        Toast.makeText(this@ChatActivity, message, Toast.LENGTH_SHORT).show()
    }

    private fun switchBackToMainActivity() {
        val intent = Intent(this@ChatActivity, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK.or(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
    }

    private fun listenAvailabilityOfReceiver() {
        database.collection(Constants.KEY_COLLECTION_USERS)
            .document(receiverUser.id!!)
            .addSnapshotListener(this@ChatActivity) { value, error ->
                if (error != null) {
                    return@addSnapshotListener
                }

                if (value != null) {
                    if (value.getBoolean(Constants.KEY_USER_ONLINE) != null) {
                        isReceiverAvailable = value.getBoolean(Constants.KEY_USER_ONLINE)!!
                    }

                    receiverUser.token = value.getString(Constants.KEY_USER_TOKEN)
                }
            }
    }

    override fun onResume() {
        super.onResume()

        listenAvailabilityOfReceiver()
    }

    // Declare the launcher at the top of your Activity/Fragment:
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            showMessage("Notifications permission granted")
        } else {
            showMessage("FCM can't post notifications without POST_NOTIFICATIONS permission")
        }
    }

    private fun askNotificationPermission(notification: String) {
        // This is only necessary for API level >= 33 (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this@ChatActivity,
                                                  Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                sendNotification(notification)
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                showNotificationPermissionRationale()
            } else {
                // Directly ask for the permission
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun showNotificationPermissionRationale() {

        MaterialAlertDialogBuilder(this@ChatActivity,
                                   R.style.Body_ThemeOverlay_MaterialComponents_MaterialAlertDialog)
            .setTitle("Alert")
            .setMessage("Notification permission is required, to show notification")
            .setPositiveButton("Ok") { _, _ ->
                if (Build.VERSION.SDK_INT >= 33) {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

}