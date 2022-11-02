package com.monolithdragon.yoochat.api

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.HeaderMap
import retrofit2.http.POST

interface ServiceApi {
    @POST("send")
    fun sendNotification(
        @HeaderMap headers: HashMap<String, String>,
        @Body messageBody: String
    ): Call<String>
}