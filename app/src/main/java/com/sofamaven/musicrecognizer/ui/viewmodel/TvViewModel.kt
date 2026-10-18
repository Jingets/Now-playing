package com.sofamaven.musicrecognizer.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sofamaven.musicrecognizer.audio.AudioCaptureManager
import com.sofamaven.musicrecognizer.data.local.SettingsDataStore
import com.sofamaven.musicrecognizer.data.model.AppSettings
import com.sofamaven.musicrecognizer.data.model.RecognitionMode
import com.sofamaven.musicrecognizer.data.model.TrackInfo
import com.sofamaven.musicrecognizer.data.repository.RecognitionRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface RecognitionStatus {
    data object Idle : RecognitionStatus
    data class Listening(val remainingSeconds: Int) : RecognitionStatus
    data object Analyzing : RecognitionStatus
    data class Error(val message: String) : RecognitionStatus
}

data class TvUiState(
    val status: RecognitionStatus = RecognitionStatus.Idle,
    val currentTrack: TrackInfo? = null,
    val transientErrorMessage: String? = null,
    val isContinuousActive: Boolean = false,
    val nextContinuousScanInSeconds: Int = 180
)

class TvViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsDataStore = SettingsDataStore(application)
    private val repository = RecognitionRepository()
    private val audioCaptureManager = AudioCaptureManager(application)

    val settings: StateFlow<AppSettings> = settingsDataStore.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppSettings()
    )

    private val _uiState = MutableStateFlow(TvUiState())
    val uiState: StateFlow<TvUiState> = _uiState.asStateFlow()

    private var recognitionJob: Job? = null
    private var continuousLoopJob: Job? = null
    private var errorDismissJob: Job? = null

    init {
        // Observe recognition mode changes to start/stop continuous loop
        viewModelScope.launch {
            settings.collect { currentSettings ->
                if (currentSettings.recognitionMode == RecognitionMode.CONTINUOUS) {
                    startContinuousRecognitionLoop()
                } else {
                    stopContinuousRecognitionLoop()
                }
            }
        }
    }

    /**
     * Triggered by user pressing "Распознать трек" D-Pad button or automatically by continuous timer.
     */
    fun startRecognition() {
        if (recognitionJob?.isActive == true) return

        recognitionJob = viewModelScope.launch {
            val currentSettings = settings.value

            _uiState.value = _uiState.value.copy(
                status = RecognitionStatus.Listening(remainingSeconds = 7),
                transientErrorMessage = null
            )

            // Step 1: Record audio for 7 seconds
            val audioResult = audioCaptureManager.captureAudioClip(currentSettings.inputSource) { secondsLeft ->
                _uiState.value = _uiState.value.copy(
                    status = RecognitionStatus.Listening(remainingSeconds = secondsLeft)
                )
            }

            // Step 2: Analyze audio fragment with selected API
            audioResult.fold(
                onSuccess = { audioFile ->
                    _uiState.value = _uiState.value.copy(status = RecognitionStatus.Analyzing)

                    val recognitionResult = repository.recognize(audioFile, currentSettings)

                    // Always clean up the temporary audio file
                    audioFile.delete()

                    recognitionResult.fold(
                        onSuccess = { newTrack ->
                            val currentTrack = _uiState.value.currentTrack
                            // Flicker prevention: if same track, do not trigger flicker
                            val isSameTrack = currentTrack?.title.equals(newTrack.title, ignoreCase = true) &&
                                    currentTrack?.artist.equals(newTrack.artist, ignoreCase = true)

                            _uiState.value = _uiState.value.copy(
                                status = RecognitionStatus.Idle,
                                currentTrack = if (isSameTrack) currentTrack else newTrack,
                                transientErrorMessage = null
                            )
                        },
                        onFailure = { error ->
                            showTransientError(error.localizedMessage ?: "Не удалось распознать трек")
                        }
                    )
                },
                onFailure = { error ->
                    showTransientError(error.localizedMessage ?: "Сбой при записи аудио")
                }
            )
        }
    }

    private fun showTransientError(message: String) {
        _uiState.value = _uiState.value.copy(
            status = RecognitionStatus.Error(message),
            transientErrorMessage = message
        )

        errorDismissJob?.cancel()
        errorDismissJob = viewModelScope.launch {
            // Dismiss error after 4.5 seconds and return to Idle
            delay(4500)
            _uiState.value = _uiState.value.copy(
                status = RecognitionStatus.Idle,
                transientErrorMessage = null
            )
        }
    }

    private fun startContinuousRecognitionLoop() {
        if (continuousLoopJob?.isActive == true) return

        continuousLoopJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isContinuousActive = true)

            while (true) {
                // Perform recognition
                startRecognition()
                recognitionJob?.join()

                // Wait 180 seconds (3 minutes) before next recognition cycle
                for (sec in 180 downTo 1) {
                    _uiState.value = _uiState.value.copy(nextContinuousScanInSeconds = sec)
                    delay(1000)
                }
            }
        }
    }

    private fun stopContinuousRecognitionLoop() {
        continuousLoopJob?.cancel()
        continuousLoopJob = null
        _uiState.value = _uiState.value.copy(isContinuousActive = false)
    }

    // Settings mutators
    fun updateSettings(newSettings: AppSettings) {
        viewModelScope.launch {
            settingsDataStore.updateAllSettings(newSettings)
        }
    }

    override fun onCleared() {
        super.onCleared()
        recognitionJob?.cancel()
        continuousLoopJob?.cancel()
        errorDismissJob?.cancel()
    }
}
