package com.sofamaven.musicrecognizer.data.api

import com.sofamaven.musicrecognizer.data.model.AudDResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface AudDApiService {

    @Multipart
    @POST("/")
    suspend fun recognizeAudio(
        @Part file: MultipartBody.Part,
        @Part("api_token") apiToken: RequestBody,
        @Part("return") returnExtra: RequestBody
    ): Response<AudDResponse>
}
