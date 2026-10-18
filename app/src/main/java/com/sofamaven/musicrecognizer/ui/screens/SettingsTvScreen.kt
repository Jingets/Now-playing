package com.sofamaven.musicrecognizer.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import com.sofamaven.musicrecognizer.data.model.AppSettings
import com.sofamaven.musicrecognizer.data.model.AudioInputSource
import com.sofamaven.musicrecognizer.data.model.RecognitionMode
import com.sofamaven.musicrecognizer.data.model.RecognitionService
import com.sofamaven.musicrecognizer.ui.components.CopyrightWatermark

@Composable
fun SettingsTvScreen(
    currentSettings: AppSettings,
    onSaveAndExit: (AppSettings) -> Unit
) {
    var selectedService by remember(currentSettings) { mutableStateOf(currentSettings.selectedService) }
    var auddApiKey by remember(currentSettings) { mutableStateOf(currentSettings.auddApiKey) }
    var acrHost by remember(currentSettings) { mutableStateOf(currentSettings.acrHost) }
    var acrAccessKey by remember(currentSettings) { mutableStateOf(currentSettings.acrAccessKey) }
    var acrAccessSecret by remember(currentSettings) { mutableStateOf(currentSettings.acrAccessSecret) }
    var inputSource by remember(currentSettings) { mutableStateOf(currentSettings.inputSource) }
    var recognitionMode by remember(currentSettings) { mutableStateOf(currentSettings.recognitionMode) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F172A), Color(0xFF070B14))
                )
            )
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp, vertical = 24.dp)
        ) {
            item {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 20.dp)
                ) {
                    Text(
                        text = "Сервисный экран / Настройки",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Section 1: Music Recognition Service Selection
            item {
                SettingsSectionCard(title = "1. Выбор музыкального сервиса") {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        TvChoiceChip(
                            label = "AudD API (audd.io)",
                            isSelected = selectedService == RecognitionService.AUDD,
                            onClick = { selectedService = RecognitionService.AUDD }
                        )
                        TvChoiceChip(
                            label = "ACRCloud API (acrcloud.com)",
                            isSelected = selectedService == RecognitionService.ACR_CLOUD,
                            onClick = { selectedService = RecognitionService.ACR_CLOUD }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Section 2: API Key Configuration
            item {
                SettingsSectionCard(title = "2. Ключи доступа к API") {
                    if (selectedService == RecognitionService.AUDD) {
                        TvTextField(
                            label = "AudD API Token",
                            value = auddApiKey,
                            placeholder = "Введите токен из личного кабинета audd.io",
                            onValueChange = { auddApiKey = it }
                        )
                    } else {
                        TvTextField(
                            label = "ACRCloud Host",
                            value = acrHost,
                            placeholder = "например: identify-eu-west-1.acrcloud.com",
                            onValueChange = { acrHost = it }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        TvTextField(
                            label = "ACRCloud Access Key",
                            value = acrAccessKey,
                            placeholder = "Введите ваш Access Key",
                            onValueChange = { acrAccessKey = it }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        TvTextField(
                            label = "ACRCloud Access Secret",
                            value = acrAccessSecret,
                            placeholder = "Введите ваш Access Secret",
                            onValueChange = { acrAccessSecret = it }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Section 3: Brief Instruction & Limits Warning
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Instruction Block
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFF1E293B).copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Column {
                            Text(
                                text = "Инструкция получения ключа:",
                                color = Color(0xFF38BDF8),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "1. Зарегистрируйтесь на сайте audd.io или acrcloud.com.\n2. Скопируйте ваш Token / API Key.\n3. Введите его в поле выше.",
                                color = Color(0xFFCBD5E1),
                                fontSize = 14.sp,
                                lineHeight = 20.sp
                            )
                        }
                    }

                    // Limit Warning Block
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFF451A03).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Column {
                            Text(
                                text = "Предупреждение о лимитах:",
                                color = Color(0xFFFBBF24),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Внимание: бесплатное количество распознаваний в сервисе ограничено правилами выбранного тарифа (обычно до 100 запросов в день).",
                                color = Color(0xFFFDE68A),
                                fontSize = 14.sp,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Section 4: Microphone Input Source
            item {
                SettingsSectionCard(title = "3. Блок выбора микрофона") {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        TvChoiceChip(
                            label = "Встроенный микрофон пульта",
                            description = "Голосовой ввод пульта Android TV",
                            isSelected = inputSource == AudioInputSource.REMOTE_MIC,
                            onClick = { inputSource = AudioInputSource.REMOTE_MIC }
                        )
                        TvChoiceChip(
                            label = "USB-стик в разъёме телевизора",
                            description = "Фиксированный AudioSource.MIC с USB порта",
                            isSelected = inputSource == AudioInputSource.USB_STICK,
                            onClick = { inputSource = AudioInputSource.USB_STICK }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Section 5: Recognition Mode
            item {
                SettingsSectionCard(title = "4. Режим работы") {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        TvChoiceChip(
                            label = "По кнопке (ручной)",
                            description = "Запуск замера только по нажатию D-Pad",
                            isSelected = recognitionMode == RecognitionMode.MANUAL,
                            onClick = { recognitionMode = RecognitionMode.MANUAL }
                        )
                        TvChoiceChip(
                            label = "Непрерывное распознавание",
                            description = "Автозапись 7 секунд каждые 3 минуты (180 сек)",
                            isSelected = recognitionMode == RecognitionMode.CONTINUOUS,
                            onClick = { recognitionMode = RecognitionMode.CONTINUOUS }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Section 6: Action Buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 60.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    DpadTvButton(
                        text = "Сохранить и применить",
                        isLoading = false,
                        isEnabled = true,
                        isPrimary = true,
                        onClick = {
                            val newSettings = AppSettings(
                                selectedService = selectedService,
                                auddApiKey = auddApiKey.trim(),
                                acrHost = acrHost.trim(),
                                acrAccessKey = acrAccessKey.trim(),
                                acrAccessSecret = acrAccessSecret.trim(),
                                inputSource = inputSource,
                                recognitionMode = recognitionMode
                            )
                            onSaveAndExit(newSettings)
                        }
                    )

                    DpadTvButton(
                        text = "Отмена",
                        isLoading = false,
                        isEnabled = true,
                        isPrimary = false,
                        onClick = { onSaveAndExit(currentSettings) }
                    )
                }
            }
        }

        // Persistent Copyright Watermark
        CopyrightWatermark()
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF131D2E), RoundedCornerShape(14.dp))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
            .padding(20.dp)
    ) {
        Column {
            Text(
                text = title,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun TvChoiceChip(
    label: String,
    description: String? = null,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.05f else 1.0f,
        animationSpec = tween(durationMillis = 150),
        label = "ChipScale"
    )

    Box(
        modifier = Modifier
            .scale(scale)
            .background(
                color = if (isSelected) Color(0xFF4338CA) else Color(0xFF1E293B),
                shape = RoundedCornerShape(10.dp)
            )
            .border(
                width = if (isFocused) 3.dp else 1.dp,
                color = if (isFocused) Color.White else if (isSelected) Color(0xFF818CF8) else Color.White.copy(alpha = 0.15f),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (isSelected) "● " else "○ ",
                    color = if (isSelected) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.4f),
                    fontSize = 16.sp
                )
                Text(
                    text = label,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
            if (description != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun TvTextField(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Column {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.75f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(6.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal
            ),
            cursorBrush = SolidColor(Color(0xFF818CF8)),
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F172A), RoundedCornerShape(10.dp))
                .border(
                    width = if (isFocused) 2.5.dp else 1.dp,
                    color = if (isFocused) Color(0xFF818CF8) else Color.White.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(10.dp)
                )
                .onFocusChanged { isFocused = it.isFocused }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            decorationBox = { innerTextField ->
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = Color.White.copy(alpha = 0.35f),
                        fontSize = 15.sp
                    )
                }
                innerTextField()
            }
        )
    }
}
