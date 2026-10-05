package com.shiftsleep.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Night-shift palette: deep teal night + mint accent (avoid purple/cream AI defaults).
private val Night = Color(0xFF0B1F2A)
private val NightElevated = Color(0xFF143344)
private val Mist = Color(0xFFE8F4F2)
private val Mint = Color(0xFF7EC8C8)
private val Amber = Color(0xFFE6B35A)
private val SoftRed = Color(0xFFE07A6D)

private val colors = darkColorScheme(
    primary = Mint,
    onPrimary = Night,
    secondary = Amber,
    onSecondary = Night,
    tertiary = SoftRed,
    background = Night,
    onBackground = Mist,
    surface = NightElevated,
    onSurface = Mist,
    surfaceVariant = Color(0xFF1C4556),
    onSurfaceVariant = Color(0xFFB7D0D0),
    outline = Color(0xFF3A6675),
)

private val typography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 40.sp,
        lineHeight = 46.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
    ),
)

@Composable
fun ShiftSleepTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = colors,
        typography = typography,
        content = content,
    )
}
