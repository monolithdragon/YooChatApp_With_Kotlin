package com.monolithdragon.yoochat.activities

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.monolithdragon.yoochat.R
import com.monolithdragon.yoochat.api.Client
import com.monolithdragon.yoochat.databinding.ActivityOutgoingInvitationBinding
import com.monolithdragon.yoochat.models.User
import com.monolithdragon.yoochat.utilities.Constants
import com.monolithdragon.yoochat.utilities.PreferenceManager
import org.jitsi.meet.sdk.JitsiMeetActivity
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.net.URL
import java.util.*

class OutgoingInvitationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOutgoingInvitationBinding
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var receiverUser: User
    private lateinit var meetingRoom: String

    private var meetingType: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityOutgoingInvitationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferenceManager = PreferenceManager(this@OutgoingInvitationActivity)

        loadReceiverUserDetails()
        setListener()
        setMeeting()
    }

    override fun onStart() {
        super.onStart()

        LocalBroadcastManager.getInstance(applicationContext).registerReceiver(
            invitationResponseReceiver,
            IntentFilter(Constants.REMOTE_MESSAGE_INVITATION_RESPONSE)
        )
    }

    override fun onStop() {
        super.onStop()

        LocalBroadcastManager.getInstance(applicationContext).unregisterReceiver(
            invitationResponseReceiver
        )
    }

    private fun setListener() {
        binding.imageRejectInvitation.setOnClickListener {
            cancelInvitationResponse()
        }
    }

    private fun setMeeting() {
        meetingType = intent.getStringExtra(Constants.KEY_INVITATION_TYPE)!!

        if (!meetingType.isNullOrEmpty()) {
            if (meetingType == Constants.INVITATION_AUDIO) {
                binding.imageMeetingType.setImageResource(R.drawable.ic_phone_call)
            } else if (meetingType == Constants.INVITATION_VIDEO) {
                binding.imageMeetingType.setImageResource(R.drawable.ic_video_call)
            }

            initiateMeeting(meetingType!!)
        }
    }

    private fun loadReceiverUserDetails() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            receiverUser =
                intent.getParcelableExtra(Constants.KEY_RECEIVER_USER, User::class.java)!!
        }

        binding.textName.text = receiverUser.name
        binding.imageProfile.setImageBitmap(getBitmapFromEncoded(receiverUser.profileImage))
    }

    private fun initiateMeeting(meetingType: String) {
        try {
            val tokens = JSONArray()
            tokens.put(receiverUser.token)

            val data = JSONObject()
            data.put(Constants.KEY_USER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
            data.put(Constants.KEY_USER_NAME,
                     preferenceManager.getString(Constants.KEY_USER_NAME))
            data.put(Constants.KEY_USER_TOKEN,
                     preferenceManager.getString(Constants.KEY_USER_TOKEN))

            data.put(Constants.KEY_INVITATION_TYPE, Constants.REMOTE_MESSAGE_INVITATION)
            data.put(Constants.REMOTE_MESSAGE_MEETING_TYPE, meetingType)

            meetingRoom = preferenceManager.getString(Constants.KEY_USER_ID) + "_" +
                    UUID.randomUUID().toString().substring(0, 5)

            data.put(Constants.REMOTE_MESSAGE_MEETING_ROOM, meetingRoom)

            val body = JSONObject()
            body.put(Constants.REMOTE_MESSAGE_DATA, data)
            body.put(Constants.REMOTE_MESSAGE_REGISTRATION_IDS, tokens)

            sendInvitation(body.toString(), Constants.REMOTE_MESSAGE_INVITATION)

        } catch (e: Exception) {
            showMessage(e.message!!)
        }
    }

    private fun cancelInvitationResponse() {
        try {
            val tokens = JSONArray()
            tokens.put(receiverUser.token)

            val data = JSONObject()
            data.put(Constants.KEY_INVITATION_TYPE, Constants.REMOTE_MESSAGE_INVITATION_RESPONSE)
            data.put(Constants.REMOTE_MESSAGE_INVITATION_RESPONSE,
                     Constants.REMOTE_MESSAGE_INVITATION_CANCELLED)

            val body = JSONObject()
            body.put(Constants.REMOTE_MESSAGE_DATA, data)
            body.put(Constants.REMOTE_MESSAGE_REGISTRATION_IDS, tokens)

            sendInvitation(body.toString(), Constants.REMOTE_MESSAGE_INVITATION_RESPONSE)

        } catch (e: Exception) {
            showMessage(e.message!!)
        }
    }

    private fun sendInvitation(messageBody: String, type: String) {
        try {
            Client.api.sendNotification(Constants.remoteMessageHeaders, messageBody)
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
                            if (type == Constants.REMOTE_MESSAGE_INVITATION) {
                                showMessage("Invitation sent successfully")
                            } else if (type == Constants.REMOTE_MESSAGE_INVITATION_RESPONSE) {
                                showMessage("Invitation cancelled")
                                finish()
                            }
                        } else {
                            showMessage("Error: " + response.code())
                            finish()
                        }
                    }

                    override fun onFailure(call: Call<String>, t: Throwable) {
                        showMessage(t.message!!)
                        finish()
                    }

                })
        } catch (e: Exception) {
            showMessage(e.message!!)
            finish()
        }
    }

    private val invitationResponseReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val type = intent?.getStringExtra(Constants.REMOTE_MESSAGE_INVITATION_RESPONSE)
            if (!type.isNullOrEmpty()) {
                if (type == Constants.REMOTE_MESSAGE_INVITATION_ACCEPTED) {
                    try {
                        val serverUrl = URL("https://meet.jit.si")
                        val builder = JitsiMeetConferenceOptions.Builder()
                        builder.setServerURL(serverUrl)
                        builder.setFeatureFlag("welcomepage.enabled", false)
                        builder.setRoom(meetingRoom)
                        if (meetingType == Constants.INVITATION_AUDIO) {
                            builder.setVideoMuted(false)
                        }

                        JitsiMeetActivity.launch(this@OutgoingInvitationActivity, builder.build())
                        finish()
                    } catch (e: Exception) {
                        showMessage(e.message!!)

                    }
                } else if (type == Constants.REMOTE_MESSAGE_INVITATION_REJECTED) {
                    showMessage("Invitation Rejected")
                    finish()
                }
            }
        }
    }

    private fun getBitmapFromEncoded(encodedImage: String?): Bitmap? {
        if (!encodedImage.isNullOrEmpty()) {
            val bytes = Base64.decode(encodedImage, Base64.DEFAULT)
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }

        return null
    }

    private fun showMessage(message: String) {
        Toast.makeText(this@OutgoingInvitationActivity, message, Toast.LENGTH_SHORT).show()
    }
}