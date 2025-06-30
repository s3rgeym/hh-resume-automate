package com.github.s3rgeym.hh_resume_automate.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// --- Простая и контрастная схема: Синий и Золотой ---

// Светлая тема
private val LightColorScheme = lightColorScheme(
    // Основной цвет (Синий)
    primary = Color(0xFF1976D2),              // Насыщенный синий (Blue 700)
    onPrimary = Color.White,                  // Белый текст на синем
    primaryContainer = Color(0xFFBBDEFB),     // Очень светлый синий (Blue 100)
    onPrimaryContainer = Color(0xFF0D47A1),   // Очень темный синий (Blue 900)

    // Вторичный/акцентный цвет (Золотой)
    secondary = Color(0xFFFFA000),            // Насыщенный янтарный/золотой (Amber 700)
    onSecondary = Color.Black,                // Черный текст на золотом
    secondaryContainer = Color(0xFFFFECB3),   // Очень светлый золотой (Amber 100)
    onSecondaryContainer = Color(0xFFE65100), // Темно-оранжевый

    // Фон и поверхности
    background = Color(0xFFFDFBFF),           // Почти белый фон
    onBackground = Color(0xFF1A1C1E),         // Черный текст на фоне
    surface = Color(0xFFFDFBFF),              // Поверхности (карточки и т.д.)
    onSurface = Color(0xFF1A1C1E),            // Черный текст на поверхностях

    // Ошибки и прочее
    error = Color(0xFFB00020),
    onError = Color.White
)

// Темная тема
private val DarkColorScheme = darkColorScheme(
    // Основной цвет (Темно-синий)
    primary = Color(0xFF0D47A1),              // Очень темный синий (Blue 900)
    onPrimary = Color.White,                  // Белый текст на нем
    primaryContainer = Color(0xFF1565C0),     // Просто темный синий (Blue 800)
    onPrimaryContainer = Color.White,         // Белый текст на нем

    // Вторичный/акцентный цвет (Золотой)
    secondary = Color(0xFFFFCA28),            // Яркий, светлый золотой (Amber 300)
    onSecondary = Color.Black,                // Черный текст на нем
    secondaryContainer = Color(0xFFB26A00),   // Темный золотой
    onSecondaryContainer = Color.White,       // Белый текст на нем

    // Фон и поверхности
    background = Color(0xFF121212),           // Очень темный фон
    onBackground = Color.White,               // Белый текст на фоне
    surface = Color(0xFF1E1E1E),              // Поверхности чуть светлее фона
    onSurface = Color.White,                  // Белый текст на поверхностях

    // Ошибки и прочее
    error = Color(0xFFCF6679),
    onError = Color.Black
)


@Composable
fun HHResumeAutomateTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val shapes = Shapes(
        extraSmall = RoundedCornerShape(4.dp), // Для маленьких компонентов, вроде чипов
        small = RoundedCornerShape(8.dp),      // Для небольших кнопок, текстовых полей
        medium = RoundedCornerShape(12.dp),    // Для большинства кнопок
        large = RoundedCornerShape(16.dp),     // Для больших элементов
        extraLarge = RoundedCornerShape(24.dp) // Для очень больших компонентов
    )

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = shapes,
        typography = Typography, // Убедитесь, что у вас определен объект Typography
        content = content
    )
}