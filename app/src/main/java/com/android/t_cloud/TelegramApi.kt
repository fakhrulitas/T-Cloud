package com.android.t_cloud

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

interface TelegramApi {
    @GET("getUpdates")
    fun getUpdates(): Call<UpdateResponse>

    @GET("getFile")
    fun getFile(@Query("file_id") fileId: String): Call<FileResponse>

    @Multipart
    @POST("sendDocument")
    fun sendDocument(
        @Part document: MultipartBody.Part,
        @Query("chat_id") chatId: String
    ): Call<ResponseBody>
}