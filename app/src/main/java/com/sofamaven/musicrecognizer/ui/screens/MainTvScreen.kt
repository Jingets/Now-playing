package com.sofamaven.musicrecognizer.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import coil.compose.AsyncImage
import com.sofamaven.musicrecognizer.data.model.RecognitionMode
import com.sofamaven.musicrecognizer.data.model.TrackInfo
import com.sofamaven.musicrecognizer.ui.components.CopyrightWatermark
import com.sofamaven.musicrecognizer.ui.viewmodel.RecognitionStatus
import com.sofamaven.musicrecognizer.ui.viewmodel.TvUiState

@Composable
fun MainTvScreen(
    uiState: TvUiState,
    recognitionMode: RecognitionMode,
    onRecognizeClick: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val currentTrack = uiState.currentTrack
    val isBusy = uiState.status is RecognitionStatus.Listening || uiState.status is RecognitionStatus.Analyzing

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0A0E17))) {

        // ================= Layer 1: Blurred Crossfade Album Backdrop =================
        Crossfade(
            targetState = currentTrack?.artworkUrl,
            animationSpec = tween(durationMillis = 1000),
            label = "BackdropCrossfade"
        ) { artworkUrl ->
            if (!artworkUrl.isNullOrBlank()) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = artworkUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(radius = 50.dp)
                            .scale(1.2f)
                    )
                    // Dark cinematic overlay gradient to guarantee text contrast from 3 meters
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.75f),
                                        Color(0xFF070A10).copy(alpha = 0.88f)
                                    )
                                )
                            )
                    )
                }
            } else {
                // Ambient TV subtle gradient when no track is recognized yet
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF1E293B).copy(alpha = 0.6f),
                                    Color(0xFF090D16)
                                )
                            )
                        )
                )
            }
        }

        // ================= Layer 2: Main TV UI Content =================
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            // Top Header: Centered 2-line title as requested
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text(
                    text = "Сейчас воспроизводится",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Now playing",
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 1.2.sp
                )
            }

            // Center Content: Album artwork or Screensaver
            if (currentTrack != null) {
                TrackDisplayCenter(track = currentTrack)
            } else {
                EmptyStateScreensaver(
                    status = uiState.status,
                    isContinuousActive = uiState.isContinuousActive,
                    nextScanIn = uiState.nextContinuousScanInSeconds
                )
            }

            // Bottom Controls Bar with D-Pad focus styling
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                // Primary Action Button: "Распознать трек"
                DpadTvButton(
                    text = when (val s = uiState.status) {
                        is RecognitionStatus.Listening -> "Слушаю комнату... (${s.remainingSeconds}с)"
                        is RecognitionStatus.Analyzing -> "Распознаю музыку..."
                        else -> "Распознать трек"
                    },
                    isLoading = isBusy,
                    isEnabled = !isBusy,
                    isPrimary = true,
                    onClick = onRecognizeClick
                )

                Spacer(modifier = Modifier.width(28.dp))

                // Secondary Button: "Настройки"
                DpadTvButton(
                    text = "Настройки",
                    isLoading = false,
                    isEnabled = !isBusy,
                    isPrimary = false,
                    onClick = onNavigateToSettings
                )
            }
        }

        // ================= Layer 3: Transient Error Banner (dismisses after 4-5s) =================
        if (!uiState.transientErrorMessage.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 90.dp)
                    .background(Color(0xFFDC2626).copy(alpha = 0.92f), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFFFCA5A5), RoundedCornerShape(12.dp))
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(
                    text = uiState.transientErrorMessage,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }
        }

        // ================= Layer 4: Mandatory Copyright Overlay =================
        CopyrightWatermark()
    }
}

@Composable
private fun TrackDisplayCenter(track: TrackInfo) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(vertical = 12.dp)
    ) {
        // High-res album artwork with rounded corners & shadow
        Box(
            modifier = Modifier
                .size(280.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF1E293B))
                .border(2.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
        ) {
            if (!track.artworkUrl.isNullOrBlank()) {
                AsyncImage(
                    model = track.artworkUrl,
                    contentDescription = "Обложка альбома",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Vinyl record placeholder
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .background(Color(0xFF0F172A), CircleShape)
                            .border(6.dp, Color(0xFF334155), CircleShape)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Large song title readable from 3 meters
        Text(
            text = track.title,
            color = Color.White,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Artist and Album text
        Text(
            text = buildString {
                append(track.artist)
                if (track.album.isNotBlank()) {
                    append(" • ")
                    append(track.album)
                }
            },
            color = Color(0xFFE2E8F0).copy(alpha = 0.85f),
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun EmptyStateScreensaver(
    status: RecognitionStatus,
    isContinuousActive: Boolean,
    nextScanIn: Int
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(vertical = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(200.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF4F46E5).copy(alpha = 0.25f), Color.Transparent)
                    ),
                    CircleShape
                )
                .border(2.dp, Color.White.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            when (status) {
                is RecognitionStatus.Listening -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(72.dp),
                        color = Color(0xFF818CF8),
                        strokeWidth = 5.dp
                    )
                }
                is RecognitionStatus.Analyzing -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(72.dp),
                        color = Color(0xFFF59E0B),
                        strokeWidth = 5.dp
                    )
                }
                else -> {
                    Text(
                        text = "♫",
                        fontSize = 72.sp,
                        color = Color.White.copy(alpha = 0.35f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        Text(
            text = "Настройте API ключ в сервисе или включите музыку\nи нажмите кнопку распознавания",
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 20.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )

        if (isContinuousActive) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Непрерывный режим активен: следующий замер через $nextScanIn сек",
                color = Color(0xFF38BDF8),
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

/**
 * TV Button with animated scale, glowing border and color transition on D-Pad focus.
 */
@Composable
fun DpadTvButton(
    text: String,
    isLoading: Boolean,
    isEnabled: Boolean,
    isPrimary: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.08f else 1.0f,
        animationSpec = tween(durationMillis = 150),
        label = "DpadScale"
    )

    val backgroundColor = when {
        !isEnabled -> Color(0xFF334155).copy(alpha = 0.5f)
        isFocused && isPrimary -> Color(0xFF4F46E5)
        isFocused && !isPrimary -> Color(0xFF334155)
        isPrimary -> Color(0xFF3730A3)
        else -> Color(0xFF1E293B)
    }

    val borderColor = when {
        isFocused -> Color.White
        isPrimary -> Color(0xFF6366F1).copy(alpha = 0.6f)
        else -> Color.White.copy(alpha = 0.2f)
    }

    Button(
        onClick = onClick,
        enabled = isEnabled,
        colors = ButtonDefaults.colors(
            containerColor = backgroundColor,
            contentColor = Color.White,
            focusedContainerColor = backgroundColor,
            focusedContentColor = Color.White
        ),
        shape = ButtonDefaults.shape(shape = RoundedCornerShape(14.dp)),
        modifier = Modifier
            .scale(scale)
            .height(56.dp)
            .border(if (isFocused) 3.dp else 1.dp, borderColor, RoundedCornerShape(14.dp))
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.5.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Text(
                text = text,
                fontSize = 18.sp,
                fontWeight = if (isFocused) FontWeight.Bold else FontWeight.SemiBold,
                color = if (isEnabled) Color.White else Color.White.copy(alpha = 0.4f)
            )
        }
    }
}
