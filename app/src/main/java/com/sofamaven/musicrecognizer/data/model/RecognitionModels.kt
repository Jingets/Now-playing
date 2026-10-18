package com.sofamaven.musicrecognizer.data.model

import com.google.gson.annotations.SerializedName

/**
 * Unified representation of a recognized track for the 10-foot UI
 */
data class TrackInfo(
    val title: String,
    val artist: String,
    val album: String = "",
    val artworkUrl: String? = null,
    val releaseDate: String? = null,
    val recognizedAtMillis: Long = System.currentTimeMillis()
)

enum class RecognitionService(val displayName: String, val defaultEndpoint: String) {
    AUDD("AudD API", "https://api.audd.io/"),
    ACR_CLOUD("ACRCloud API", "https://identify-eu-west-1.acrcloud.com/v1/identify")
}

enum class AudioInputSource(val label: String, val description: String) {
    REMOTE_MIC("Встроенный микрофон пульта", "Голосовой ввод пульта Android TV"),
    USB_STICK("USB-стик в разъёме ТВ", "Захват с аппаратного USB порта (AudioSource.MIC)")
}

enum class RecognitionMode(val title: String) {
    MANUAL("По кнопке (вручную)"),
    CONTINUOUS("Непрерывный (каждые 3 минуты)")
}

data class AppSettings(
    val selectedService: RecognitionService = RecognitionService.AUDD,
    val auddApiKey: String = "",
    val acrHost: String = "identify-eu-west-1.acrcloud.com",
    val acrAccessKey: String = "",
    val acrAccessSecret: String = "",
    val inputSource: AudioInputSource = AudioInputSource.REMOTE_MIC,
    val recognitionMode: RecognitionMode = RecognitionMode.MANUAL
)

// =================== AudD Data Transfer Objects ===================
data class AudDResponse(
    @SerializedName("status") val status: String,
    @SerializedName("result") val result: AudDResult?,
    @SerializedName("error") val error: AudDError?
)

data class AudDResult(
    @SerializedName("artist") val artist: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("album") val album: String?,
    @SerializedName("release_date") val releaseDate: String?,
    @SerializedName("song_link") val songLink: String?,
    @SerializedName("spotify") val spotify: AudDSpotify?,
    @SerializedName("apple_music") val appleMusic: AudDAppleMusic?
)

data class AudDSpotify(
    @SerializedName("album") val album: AudDSpotifyAlbum?
)

data class AudDSpotifyAlbum(
    @SerializedName("images") val images: List<AudDImage>?
)

data class AudDAppleMusic(
    @SerializedName("artwork") val artwork: AudDAppleArtwork?
)

data class AudDAppleArtwork(
    @SerializedName("url") val url: String?
)

data class AudDImage(
    @SerializedName("url") val url: String,
    @SerializedName("height") val height: Int?,
    @SerializedName("width") val width: Int?
)

data class AudDError(
    @SerializedName("error_code") val errorCode: Int?,
    @SerializedName("error_message") val errorMessage: String?
)

// =================== ACRCloud Data Transfer Objects ===================
data class ACRCloudResponse(
    @SerializedName("status") val status: ACRStatus,
    @SerializedName("metadata") val metadata: ACRMetadata?
)

data class ACRStatus(
    @SerializedName("code") val code: Int,
    @SerializedName("msg") val msg: String,
    @SerializedName("version") val version: String?
)

data class ACRMetadata(
    @SerializedName("music") val music: List<ACRMusicItem>?
)

data class ACRMusicItem(
    @SerializedName("title") val title: String,
    @SerializedName("artists") val artists: List<ACRArtist>?,
    @SerializedName("album") val album: ACRAlbum?,
    @SerializedName("release_date") val releaseDate: String?,
    @SerializedName("external_metadata") val externalMetadata: ACRExternalMetadata?
)

data class ACRArtist(
    @SerializedName("name") val name: String
)

data class ACRAlbum(
    @SerializedName("name") val name: String
)

data class ACRExternalMetadata(
    @SerializedName("spotify") val spotify: ACRSpotify?,
    @SerializedName("deezer") val deezer: ACRDeezer?
)

data class ACRSpotify(
    @SerializedName("album") val album: ACRSpotifyAlbum?
)

data class ACRSpotifyAlbum(
    @SerializedName("id") val id: String?,
    @SerializedName("images") val images: List<AudDImage>?
)

data class ACRDeezer(
    @SerializedName("album") val album: ACRDeezerAlbum?
)

data class ACRDeezerAlbum(
    @SerializedName("cover_big") val coverBig: String?,
    @SerializedName("cover_xl") val coverXl: String?
)
