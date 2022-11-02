package com.monolithdragon.yoochat.api

import com.monolithdragon.yoochat.utilities.Constants
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory

class Client {

    companion object {
        private val retrofit by lazy {
            Retrofit.Builder()
                .baseUrl(Constants.BASE_URL)
                .addConverterFactory(ScalarsConverterFactory.create())
                .build()
        }

        val api: ServiceApi by lazy {
            retrofit.create(ServiceApi::class.java)
        }
    }
}