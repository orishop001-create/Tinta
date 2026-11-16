package id.tinta.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

data class TintaColors(
    val key: String,
    val label: String,
    val dark: Boolean,
    val bg: Color,
    val surface: Color,
    val ink: Color,
    val muted: Color,
    val accent: Color,
    val onAccent: Color,
    val editor: Color,
    val border: Color,
    val danger: Color,
    val accentSoft: Color,
)

val Kertas = TintaColors(
    key = "kertas", label = "Kertas", dark = false,
    bg = Color(0xFFF4EFE4), surface = Color(0xFFFBF7EE), ink = Color(0xFF241F18),
    muted = Color(0xFF635949), accent = Color(0xFF1F6B5A), onAccent = Color(0xFFFBF9F3),
    editor = Color(0xFFFCF8F0), border = Color(0xFFD9CFBE), danger = Color(0xFF9E3527),
    accentSoft = Color(0xFFE3EEE8),
)

val Sepia = TintaColors(
    key = "sepia", label = "Sepia", dark = false,
    bg = Color(0xFFEBDFC9), surface = Color(0xFFF8EEDB), ink = Color(0xFF2A2117),
    muted = Color(0xFF665741), accent = Color(0xFF1F6B5A), onAccent = Color(0xFFFBF9F3),
    editor = Color(0xFFFAF1E1), border = Color(0xFFD4C3A6), danger = Color(0xFF993423),
    accentSoft = Color(0xFFE2E9DE),
)

val Malam = TintaColors(
    key = "malam", label = "Malam", dark = true,
    bg = Color(0xFF121615), surface = Color(0xFF191F1D), ink = Color(0xFFEDE7DB),
    muted = Color(0xFFB6AC9B), accent = Color(0xFF83CDB1), onAccent = Color(0xFF0B1A15),
    editor = Color(0xFF171D1B), border = Color(0xFF63736A), danger = Color(0xFFF09B8B),
    accentSoft = Color(0xFF23352F),
)

val Grafit = TintaColors(
    key = "grafit", label = "Grafit", dark = true,
    bg = Color(0xFF181B1E), surface = Color(0xFF202428), ink = Color(0xFFE9E7E2),
    muted = Color(0xFFB1AAA0), accent = Color(0xFFD7B475), onAccent = Color(0xFF221B0E),
    editor = Color(0xFF1C2023), border = Color(0xFF6F7982), danger = Color(0xFFF19A8A),
    accentSoft = Color(0xFF3A3427),
)

val TintaPalettes = listOf(Kertas, Sepia, Malam, Grafit)
val LocalTintaColors = staticCompositionLocalOf { Kertas }

val CoverGradients: List<List<Color>> = listOf(
    listOf(Color(0xFFB96A45), Color(0xFF8F4A30)),
    listOf(Color(0xFF2C5D6B), Color(0xFF183C48)),
    listOf(Color(0xFF4F6B45), Color(0xFF334B2D)),
    listOf(Color(0xFF79506A), Color(0xFF57374C)),
    listOf(Color(0xFFC79A47), Color(0xFF946B28)),
)

private val TintaTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.6).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.3).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 21.sp,
        lineHeight = 27.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 13.sp,
        lineHeight = 19.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.3.sp,
    ),
)

private val TintaShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun TintaTheme(themeKey: String, content: @Composable () -> Unit) {
    val palette = TintaPalettes.firstOrNull { it.key == themeKey } ?: Kertas
    val scheme = if (palette.dark) {
        darkColorScheme(
            primary = palette.accent,
            onPrimary = palette.onAccent,
            primaryContainer = palette.accentSoft,
            onPrimaryContainer = palette.ink,
            secondary = palette.accent,
            onSecondary = palette.onAccent,
            background = palette.bg,
            onBackground = palette.ink,
            surface = palette.surface,
            onSurface = palette.ink,
            surfaceVariant = palette.bg,
            onSurfaceVariant = palette.muted,
            outline = palette.border,
            error = palette.danger,
            onError = palette.onAccent,
        )
    } else {
        lightColorScheme(
            primary = palette.accent,
            onPrimary = palette.onAccent,
            primaryContainer = palette.accentSoft,
            onPrimaryContainer = palette.ink,
            secondary = palette.accent,
            onSecondary = palette.onAccent,
            background = palette.bg,
            onBackground = palette.ink,
            surface = palette.surface,
            onSurface = palette.ink,
            surfaceVariant = palette.bg,
            onSurfaceVariant = palette.muted,
            outline = palette.border,
            error = palette.danger,
            onError = palette.onAccent,
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity ?: return@SideEffect
            val window = activity.window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !palette.dark
                isAppearanceLightNavigationBars = !palette.dark
            }
            @Suppress("DEPRECATION")
            window.navigationBarColor = palette.bg.toArgb()
        }
    }

    MaterialTheme(
        colorScheme = scheme,
        typography = TintaTypography,
        shapes = TintaShapes,
    ) {
        CompositionLocalProvider(LocalTintaColors provides palette) { content() }
    }
}
