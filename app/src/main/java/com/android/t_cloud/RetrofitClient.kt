package com.android.t_cloud

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private const val BASE_URL =
        "https://api.telegram.org/bot8550143265:AAE3fsI6k692b2XirLh_tHHwGv2rVY1nfqc/"

    val api: TelegramApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TelegramApi::class.java)
    }
}
