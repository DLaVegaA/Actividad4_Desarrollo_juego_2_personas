package com.equipoea.Tankwar.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// --- ESTE ES EL TEMA OSCURO (GUINDA) ---
private val DarkColorScheme = darkColorScheme(
    primary = OscuroGuinda_primary,
    onPrimary = OscuroGuinda_onPrimary,
    primaryContainer = OscuroGuinda_primaryContainer,
    onPrimaryContainer = OscuroGuinda_onPrimaryContainer,
    secondary = OscuroGuinda_secondary,
    onSecondary = OscuroGuinda_onSecondary,
    secondaryContainer = OscuroGuinda_secondaryContainer,
    onSecondaryContainer = OscuroGuinda_onSecondaryContainer,
    tertiary = OscuroGuinda_tertiary,
    onTertiary = OscuroGuinda_onTertiary,
    tertiaryContainer = OscuroGuinda_tertiaryContainer,
    onTertiaryContainer = OscuroGuinda_onTertiaryContainer,
    error = OscuroGuinda_error,
    onError = OscuroGuinda_onError,
    background = OscuroGuinda_background,
    onBackground = OscuroGuinda_onBackground,
    surface = OscuroGuinda_surface,
    onSurface = OscuroGuinda_onSurface
)

// --- ESTE ES EL TEMA CLARO (AZUL CLARO) ---
private val LightColorScheme = lightColorScheme(
    primary = ClaroAzul_primary,
    onPrimary = ClaroAzul_onPrimary,
    primaryContainer = ClaroAzul_primaryContainer,
    onPrimaryContainer = ClaroAzul_onPrimaryContainer,
    secondary = ClaroAzul_secondary,
    onSecondary = ClaroAzul_onSecondary,
    secondaryContainer = ClaroAzul_secondaryContainer,
    onSecondaryContainer = ClaroAzul_onSecondaryContainer,
    tertiary = ClaroAzul_tertiary,
    onTertiary = ClaroAzul_onTertiary,
    tertiaryContainer = ClaroAzul_tertiaryContainer,
    onTertiaryContainer = ClaroAzul_onTertiaryContainer,
    error = ClaroAzul_error,
    onError = ClaroAzul_onError,
    background = ClaroAzul_background,
    onBackground = ClaroAzul_onBackground,
    surface = ClaroAzul_surface,
    onSurface = ClaroAzul_onSurface
)

@Composable
fun TankWarTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Actualizamos el color del fondo y la barra de estado
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()

            // Hacemos que los iconos (batería, hora) sean claros en tema oscuro
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // Asumo que ya tienes tu archivo Typography.kt
        content = content
    )
}