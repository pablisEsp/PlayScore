package ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Define color constants
private val Green = Color(0xFF14B82A)         // Main green color #14B82A
private val DarkGreen = Color(0xFF0D8A1F)     // Darker green for variants
private val LightGreen = Color(0xFF26D13D)    // Lighter green
private val ComplementaryGreen = Color(0xFF4CAF50) // Complementary green
private val DarkComplementaryGreen = Color(0xFF388E3C) // Darker complementary
private val LightBackground = Color(0xFFF5F5F5)
private val DarkBackground = Color(0xFF121212)
private val DarkSurface = Color(0xFF1E1E1E)
private val ErrorColor = Color(0xFFE53935)
private val DarkErrorColor = Color(0xFFEF5350)

// Light theme colors with Material3 ColorScheme
private val LightColorScheme = lightColorScheme(
    primary = Green,
    onPrimary = Color.White,
    primaryContainer = DarkGreen,
    onPrimaryContainer = Color.White,
    secondary = ComplementaryGreen,
    onSecondary = Color.White,
    secondaryContainer = DarkComplementaryGreen,
    onSecondaryContainer = Color.White,
    tertiary = ComplementaryGreen,
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = Color(0xFF212121),
    surface = Color.White,
    onSurface = Color(0xFF212121),
    error = ErrorColor,
    onError = Color.White
)

// Dark theme colors with Material3 ColorScheme
private val DarkColorScheme = darkColorScheme(
    primary = LightGreen,
    onPrimary = Color.Black,
    primaryContainer = Green,
    onPrimaryContainer = Color.White,
    secondary = ComplementaryGreen,
    onSecondary = Color.Black,
    secondaryContainer = DarkComplementaryGreen,
    onSecondaryContainer = Color.White,
    tertiary = ComplementaryGreen,
    onTertiary = Color.Black,
    background = DarkBackground,
    onBackground = Color.White,
    surface = DarkSurface,
    onSurface = Color.White,
    error = DarkErrorColor,
    onError = Color.Black
)

// Custom shapes
val AppShapes = Shapes(
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(12.dp)
)

// Custom typography for Material3
val AppTypography = Typography(
    displayLarge = TextStyle(
        fontWeight = FontWeight.Light,
        fontSize = 96.sp,
        letterSpacing = (-1.5).sp
    ),
    displayMedium = TextStyle(
        fontWeight = FontWeight.Light,
        fontSize = 60.sp,
        letterSpacing = (-0.5).sp
    ),
    displaySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 48.sp,
        letterSpacing = 0.sp
    ),
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 34.sp,
        letterSpacing = 0.25.sp
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        letterSpacing = 0.15.sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        letterSpacing = 0.15.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        letterSpacing = 0.25.sp
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        letterSpacing = 1.25.sp
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        letterSpacing = 0.4.sp
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
        letterSpacing = 1.5.sp
    )
)

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
