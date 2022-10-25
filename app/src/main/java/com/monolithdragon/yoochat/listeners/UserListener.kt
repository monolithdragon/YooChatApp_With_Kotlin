package com.monolithdragon.yoochat.listeners

import com.monolithdragon.yoochat.models.User

interface UserListener {
    fun onClickListener(user: User)
}