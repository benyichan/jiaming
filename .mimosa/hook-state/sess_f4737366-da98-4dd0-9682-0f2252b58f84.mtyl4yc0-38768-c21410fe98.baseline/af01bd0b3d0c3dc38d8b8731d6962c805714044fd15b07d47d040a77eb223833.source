package com.xiaojiaoyin.jiaming.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// 小脚印系视觉：薄荷绿主色 + 柔和浅底
val Mint = Color(0xFF2E9E76)
val MintLight = Color(0xFFE4F6EE)
val MintDeep = Color(0xFF1F7A5B)
val PaperBg = Color(0xFFFAFDFB)
val WarnAmber = Color(0xFFB26A00)
val FailRed = Color(0xFFB3402E)
val InkDark = Color(0xFF1D2B26)

private val LightScheme = lightColorScheme(
    primary = Mint,
    onPrimary = Color.White,
    primaryContainer = MintLight,
    onPrimaryContainer = MintDeep,
    secondary = MintDeep,
    secondaryContainer = MintLight,
    onSecondaryContainer = MintDeep,
    background = PaperBg,
    onBackground = InkDark,
    surface = Color.White,
    onSurface = InkDark,
    surfaceVariant = MintLight,
    onSurfaceVariant = Color(0xFF41564D),
    outline = Color(0xFF9DB4AA),
    error = FailRed,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF3F9F6),
    surfaceContainer = Color(0xFFEDF5F0),
    surfaceContainerHigh = Color(0xFFE7F1EB),
    surfaceContainerHighest = Color(0xFFE1EDE6),
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF6BCBA4),
    onPrimary = Color(0xFF003826),
    primaryContainer = Color(0xFF1F5241),
    onPrimaryContainer = MintLight,
    secondary = Color(0xFF9BD4BC),
    background = Color(0xFF101613),
    onBackground = Color(0xFFDCE8E2),
    surface = Color(0xFF161D1A),
    onSurface = Color(0xFFDCE8E2),
    surfaceVariant = Color(0xFF24322C),
    onSurfaceVariant = Color(0xFFA8C0B5),
    error = Color(0xFFFFB4A6),
)

private val AppTypography = Typography(
    displaySmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 36.sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    bodyLarge = TextStyle(fontSize = 16.sp),
    bodyMedium = TextStyle(fontSize = 14.sp),
    labelMedium = TextStyle(fontSize = 12.sp),
)

@Composable
fun JiamingTheme(content: @Composable () -> Unit) {
    // M1 主题跟随系统深浅色；薄荷绿品牌色两套适配
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkScheme else LightScheme,
        typography = AppTypography,
        content = content,
    )
}
