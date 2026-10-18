package com.sofamaven.musicrecognizer.data.repository

import com.sofamaven.musicrecognizer.data.api.ACRCloudApiService
import com.sofamaven.musicrecognizer.data.api.ACRCloudSignatureUtil
import com.sofamaven.musicrecognizer.data.api.AudDApiService
import com.sofamaven.musicrecognizer.data.model.AppSettings
import com.sofamaven.musicrecognizer.data.model.RecognitionService
import com.sofamaven.musicrecognizer.data.model.TrackInfo
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

class RecognitionRepository {

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .build()
    }

    private val auddRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.audd.io/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private val acrRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://identify-eu-west-1.acrcloud.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private val auddService: AudDApiService by lazy {
        auddRetrofit.create(AudDApiService::class.java)
    }

    private val acrService: ACRCloudApiService by lazy {
        acrRetrofit.create(ACRCloudApiService::class.java)
    }

    suspend fun recognize(audioFile: File, settings: AppSettings): Result<TrackInfo> {
        if (!audioFile.exists() || audioFile.length() == 0L) {
            return Result.failure(Exception("Аудиозапись пуста или не удалось сохранить файл."))
        }

        return try {
            when (settings.selectedService) {
                RecognitionService.AUDD -> recognizeWithAudD(audioFile, settings.auddApiKey)
                RecognitionService.ACR_CLOUD -> recognizeWithACRCloud(
                    audioFile = audioFile,
                    host = settings.acrHost,
                    accessKey = settings.acrAccessKey,
                    accessSecret = settings.acrAccessSecret
                )
            }
        } catch (e: Exception) {
            Result.failure(Exception("Сетевой сбой: ${e.localizedMessage ?: "Проверьте подключение к интернету"}"))
        }
    }

    private suspend fun recognizeWithAudD(file: File, apiKey: String): Result<TrackInfo> {
        if (apiKey.isBlank()) {
            return Result.failure(Exception("API ключ AudD не заполнен. Перейдите в Настройки и укажите токен."))
        }

        val requestFile = file.asRequestBody("audio/wav".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
        val tokenBody = apiKey.toRequestBody("text/plain".toMediaTypeOrNull())
        val extraBody = "apple_music,spotify".toRequestBody("text/plain".toMediaTypeOrNull())

        val response = auddService.recognizeAudio(body, tokenBody, extraBody)
        if (!response.isSuccessful) {
            return Result.failure(Exception("Ошибка сервера AudD (Код: ${response.code()}). Проверьте настройки API ключа."))
        }

        val data = response.body() ?: return Result.failure(Exception("Пустой ответ от сервера AudD."))

        if (data.status != "success") {
            val errorMsg = data.error?.errorMessage ?: "Не удалось распознать, попробуйте еще раз."
            return Result.failure(Exception(errorMsg))
        }

        val result = data.result
        if (result == null || result.title.isNullOrBlank()) {
            return Result.failure(Exception("Музыка в комнате не распознана. Попробуйте еще раз."))
        }

        // Extract high-resolution artwork: Spotify > Apple Music > null
        var artworkUrl: String? = result.spotify?.album?.images?.firstOrNull()?.url
        if (artworkUrl.isNullOrBlank() && result.appleMusic?.artwork?.url != null) {
            artworkUrl = result.appleMusic.artwork.url
                .replace("{w}", "1000")
                .replace("{h}", "1000")
        }

        val track = TrackInfo(
            title = result.title,
            artist = result.artist ?: "Неизвестный исполнитель",
            album = result.album ?: "",
            artworkUrl = artworkUrl,
            releaseDate = result.releaseDate
        )

        return Result.success(track)
    }

    private suspend fun recognizeWithACRCloud(
        audioFile: File,
        host: String,
        accessKey: String,
        accessSecret: String
    ): Result<TrackInfo> {
        if (accessKey.isBlank() || accessSecret.isBlank()) {
            return Result.failure(Exception("ACRCloud Access Key или Secret не указаны. Заполните их в Настройках."))
        }

        val cleanHost = host.removePrefix("https://").removePrefix("http://").trimEnd('/')
        val endpointUrl = "https://$cleanHost/v1/identify"
        val httpMethod = "POST"
        val httpUri = "/v1/identify"
        val dataType = "audio"
        val signatureVersion = "1"
        val timestamp = (System.currentTimeMillis() / 1000).toString()

        val signature = ACRCloudSignatureUtil.generateSignature(
            httpMethod = httpMethod,
            httpUri = httpUri,
            accessKey = accessKey,
            dataType = dataType,
            signatureVersion = signatureVersion,
            timestamp = timestamp,
            accessSecret = accessSecret
        )

        val sampleBytes = audioFile.length().toString()
        val requestFile = audioFile.asRequestBody("audio/wav".toMediaTypeOrNull())
        val samplePart = MultipartBody.Part.createFormData("sample", audioFile.name, requestFile)

        val plainType = "text/plain".toMediaTypeOrNull()

        val response = acrService.identifyAudio(
            endpointUrl = endpointUrl,
            sample = samplePart,
            sampleBytes = sampleBytes.toRequestBody(plainType),
            accessKey = accessKey.toRequestBody(plainType),
            dataType = dataType.toRequestBody(plainType),
            signature = signature.toRequestBody(plainType),
            signatureVersion = signatureVersion.toRequestBody(plainType),
            timestamp = timestamp.toRequestBody(plainType)
        )

        if (!response.isSuccessful) {
            return Result.failure(Exception("Ошибка ACRCloud HTTP: ${response.code()}. Проверьте Host и ключи."))
        }

        val data = response.body() ?: return Result.failure(Exception("Пустой ответ от сервера ACRCloud."))

        // Code 0 means success
        if (data.status.code != 0) {
            val msg = when (data.status.code) {
                1001 -> "Музыка не найдена в базе ACRCloud. Попробуйте еще раз."
                2004, 3000 -> "Неверный Access Key или Secret. Проверьте настройки API ключа."
                else -> "Ошибка ACRCloud: ${data.status.msg}"
            }
            return Result.failure(Exception(msg))
        }

        val musicList = data.metadata?.music
        val topMatch = musicList?.firstOrNull()
            ?: return Result.failure(Exception("Музыка в комнате не распознана."))

        val artists = topMatch.artists?.joinToString(", ") { it.name } ?: "Неизвестный исполнитель"
        val albumName = topMatch.album?.name ?: ""

        // Extract artwork from Deezer or Spotify external metadata
        val artworkUrl = topMatch.externalMetadata?.deezer?.album?.coverXl
            ?: topMatch.externalMetadata?.deezer?.album?.coverBig
            ?: topMatch.externalMetadata?.spotify?.album?.images?.firstOrNull()?.url

        val track = TrackInfo(
            title = topMatch.title,
            artist = artists,
            album = albumName,
            artworkUrl = artworkUrl,
            releaseDate = topMatch.releaseDate
        )

        return Result.success(track)
    }
}
