package com.monolithdragon.yoochat.models

data class Conversation(
    var conversationMessage: Message? = null,
    var conversationId: String? = null,
    var conversationName: String? = null,
    var conversationImage: String? = null
): Comparable<Conversation> {
    override fun compareTo(other: Conversation): Int {
        return this.conversationMessage?.updateAt?.compareTo(other.conversationMessage?.updateAt)!!
    }
}