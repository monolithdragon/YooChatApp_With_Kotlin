package com.monolithdragon.yoochat.models

import java.util.Date

data class Message(
    var message: String? = null,
    var senderId: String? = null,
    var receiverId: String? = null,
    var createAt: String? = null,
    var updateAt: Date? = null
): Comparable<Message> {
    override fun compareTo(other: Message): Int {
        return this.updateAt?.compareTo(other.updateAt)!!
    }
}