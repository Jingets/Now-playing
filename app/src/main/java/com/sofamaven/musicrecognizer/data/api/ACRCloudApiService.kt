package com.sofamaven.musicrecognizer.data.api

import android.util.Base64
import com.sofamaven.musicrecognizer.data.model.ACRCloudResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Url
import java.security.SignatureException
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

interface ACRCloudApiService {

    @Multipart
    @POST
    suspend fun identifyAudio(
        @Url endpointUrl: String,
        @Part sample: MultipartBody.Part,
        @Part("sample_bytes") sampleBytes: RequestBody,
        @Part("access_key") accessKey: RequestBody,
        @Part("data_type") dataType: RequestBody,
        @Part("signature") signature: RequestBody,
        @Part("signature_version") signatureVersion: RequestBody,
        @Part("timestamp") timestamp: RequestBody
    ): Response<ACRCloudResponse>
}

object ACRCloudSignatureUtil {

    /**
     * Generates HMAC-SHA1 signature according to ACRCloud API specification:
     * StringToSign = HTTP-METHOD + "\n" + HTTP-URI + "\n" + ACCESS_KEY + "\n" + DATA_TYPE + "\n" + SIGNATURE_VERSION + "\n" + TIMESTAMP
     */
    fun generateSignature(
        httpMethod: String,
        httpUri: String,
        accessKey: String,
        dataType: String,
        signatureVersion: String,
        timestamp: String,
        accessSecret: String
    ): String {
        val stringToSign = "$httpMethod\n$httpUri\n$accessKey\n$dataType\n$signatureVersion\n$timestamp"
        return hmacSha1(stringToSign, accessSecret)
    }

    private fun hmacSha1(data: String, key: String): String {
        return try {
            val signingKey = SecretKeySpec(key.toByteArray(Charsets.UTF_8), "HmacSHA1")
            val mac = Mac.getInstance("HmacSHA1")
            mac.init(signingKey)
            val rawHmac = mac.doFinal(data.toByteArray(Charsets.UTF_8))
            Base64.encodeToString(rawHmac, Base64.NO_WRAP)
        } catch (e: Exception) {
            throw SignatureException("Failed to generate HMAC-SHA1: " + e.message)
        }
    }
}
