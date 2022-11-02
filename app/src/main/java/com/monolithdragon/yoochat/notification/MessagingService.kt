package com.monolithdragon.yoochat.notification

import android.app.PendingIntent
import android.content.Intent
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.monolithdragon.yoochat.activities.ChatActivity
import com.monolithdragon.yoochat.models.User
import com.monolithdragon.yoochat.utilities.Constants

class MessagingService: FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        if (!remoteMessage.data.isNullOrEmpty()) {
            val user = User()
            user.id = remoteMessage.data[Constants.KEY_USER_ID]
            user.name = remoteMessage.data[Constants.KEY_USER_NAME]
            user.token = remoteMessage.data[Constants.KEY_USER_TOKEN]

            val title = user.name
            val message = remoteMessage.data[Constants.KEY_CHAT_MESSAGE]

            val intent = Intent(this@MessagingService, ChatActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK.or(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            intent.putExtra(Constants.KEY_RECEIVER_USER, user)
            val pendingIntent = PendingIntent.getActivity(this@MessagingService, 0, intent, PendingIntent.FLAG_IMMUTABLE)

            val notificationManager = YooNotificationManager(this@MessagingService)
            notificationManager.notification(title, message, pendingIntent)
        }
    }
}