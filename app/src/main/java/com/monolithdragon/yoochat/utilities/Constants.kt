package com.monolithdragon.yoochat.utilities

class Constants {
    companion object {
        const val KEY_PREFERENCE_NAME = "YooChatPreference"
        const val KEY_COLLECTION_USERS = "users"
        const val KEY_USER_ID = "id"
        const val KEY_USER_NAME = "name"
        const val KEY_USER_EMAIL = "email"
        const val KEY_USER_IMAGE = "profileImage"
        const val KEY_USER_TOKEN = "token"
        const val KEY_RECEIVER_USER = "receiverUser"
        const val KEY_COLLECTION_MESSAGES = "messages"
        const val KEY_CHAT_MESSAGE = "message"
        const val KEY_CHAT_SENDER_ID = "senderId"
        const val KEY_CHAT_RECEIVER_ID = "receiverId"
        const val KEY_CHAT_CREATE_AT = "createAt"
        const val KEY_COLLECTION_CONVERSATIONS = "conversations"
        const val KEY_CONVERSATION_SENDER_NAME = "senderName"
        const val KEY_CONVERSATION_RECEIVER_NAME = "receiverName"
        const val KEY_CONVERSATION_SENDER_IMAGE = "senderImage"
        const val KEY_CONVERSATION_RECEIVER_IMAGE = "receiverImage"
        const val KEY_USER_ONLINE = "online"
        const val KEY_INVITATION_TYPE = "type"

        const val INVITATION_AUDIO = "audio"
        const val INVITATION_VIDEO = "video"

        const val BASE_URL = "https://fcm.googleapis.com/fcm/"

        private const val SERVER_KEY = "AAAAp6VGD2g:APA91bHj2LUzHwDQa0JKglNDmQ9_8jCish0I9VIM3yJCddDxbGzx__f6dihnX1U2R2ymvLNz7cr8_8E86-Dir8JpT3ewyuIc4Vbg3fM5v9RcERrN1q67l3LYTPZDRUd8nGrGcSqK6AC1"
        private const val CONTENT_TYPE = "application/json"

        const val REMOTE_MESSAGE_DATA = "data"
        const val REMOTE_MESSAGE_REGISTRATION_IDS = "registration_ids"
        const val REMOTE_MESSAGE_INVITATION = "invitation"
        const val REMOTE_MESSAGE_MEETING_TYPE = "meetingType"
        const val REMOTE_MESSAGE_INVITATION_RESPONSE = "invitationResponse"
        const val REMOTE_MESSAGE_INVITATION_ACCEPTED = "accepted"
        const val REMOTE_MESSAGE_INVITATION_REJECTED = "rejected"
        const val REMOTE_MESSAGE_INVITATION_CANCELLED = "cancelled"

        const val REMOTE_MESSAGE_MEETING_ROOM = "meetingRoom"

        val remoteMessageHeaders = hashMapOf(
            "Authorization" to "key=${SERVER_KEY}",
            "Content-Type" to CONTENT_TYPE
        )
    }
}