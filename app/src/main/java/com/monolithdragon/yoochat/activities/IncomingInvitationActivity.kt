package com.monolithdragon.yoochat.activities

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Base64
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.monolithdragon.yoochat.R
import com.monolithdragon.yoochat.api.Client
import com.monolithdragon.yoochat.databinding.ActivityIncomingInvitationBinding
import com.monolithdragon.yoochat.models.User
import com.monolithdragon.yoochat.utilities.Constants
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.net.URL
import org.jitsi.meet.sdk.*

class IncomingInvitationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityIncomingInvitationBinding
    private lateinit var receiverUser: User

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityIncomingInvitationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadReceiverUserDetails()
        initFields()
        setListeners()
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

    private fun setListeners() {
        binding.imageAcceptInvitation.setOnClickListener {
            sendInvitationResponse(Constants.REMOTE_MESSAGE_INVITATION_ACCEPTED)
        }

        binding.imageRejectInvitation.setOnClickListener {
            sendInvitationResponse(Constants.REMOTE_MESSAGE_INVITATION_REJECTED)
        }
    }

    private fun initFields() {
        val meetingType = intent.getStringExtra(Constants.REMOTE_MESSAGE_MEETING_TYPE)

        if (!meetingType.isNullOrEmpty()) {
            if (meetingType == Constants.INVITATION_AUDIO) {
                binding.imageMeetingType.setImageResource(R.drawable.ic_phone_call)
            } else  if (meetingType == Constants.INVITATION_VIDEO) {
                binding.imageMeetingType.setImageResource(R.drawable.ic_video_call)
            }
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

    private fun getBitmapFromEncoded(encodedImage: String?): Bitmap? {
        if (!encodedImage.isNullOrEmpty()) {
            val bytes = Base64.decode(encodedImage, Base64.DEFAULT)
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }

        return null
    }

    private fun sendInvitationResponse(type: String) {
        try {
            val tokens = JSONArray()
            tokens.put(receiverUser.token)

            val data = JSONObject()
            data.put(Constants.KEY_INVITATION_TYPE, Constants.REMOTE_MESSAGE_INVITATION_RESPONSE)
            data.put(Constants.REMOTE_MESSAGE_INVITATION_RESPONSE, type)

            val body = JSONObject()
            body.put(Constants.REMOTE_MESSAGE_DATA, data)
            body.put(Constants.REMOTE_MESSAGE_REGISTRATION_IDS, tokens)

            sendInvitation(body.toString(), type)

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
                            if (type == Constants.REMOTE_MESSAGE_INVITATION_ACCEPTED) {
                                try {
                                    val meetingType = intent.getStringExtra(Constants.REMOTE_MESSAGE_MEETING_TYPE)
                                    val serverUrl = URL("https://meet.jit.si")
                                    val builder = JitsiMeetConferenceOptions.Builder()
                                    builder.setServerURL(serverUrl)
                                    builder.setFeatureFlag("welcomepage.enabled", false)
                                    builder.setRoom(intent.getStringExtra(Constants.REMOTE_MESSAGE_MEETING_ROOM))
                                    if (meetingType == Constants.INVITATION_AUDIO) {
                                        builder.setVideoMuted(false)
                                    }

                                    JitsiMeetActivity.launch(this@IncomingInvitationActivity, builder.build())
                                    finish()
                                } catch (e: Exception) {
                                    showMessage(e.message!!)
                                    finish()
                                }
                            } else {
                                showMessage("Invitation rejected")
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
                if (type == Constants.REMOTE_MESSAGE_INVITATION_CANCELLED) {
                    showMessage("Invitation Cancelled")
                    finish()
                }
            }
        }
    }

    private fun showMessage(message: String) {
        Toast.makeText(this@IncomingInvitationActivity, message, Toast.LENGTH_SHORT).show()
    }

}