package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = BrandGreenPrimary,
    secondary = BrandGreenSecondary,
    tertiary = BrandGreenLight,
    background = Color(0xFF111612), // Deep forest charcoal
    surface = Color(0xFF1B221C),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFFF5FAF6),
    onSurface = Color(0xFFF5FAF6)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = BrandGreenPrimary,
    secondary = BrandGreenSecondary,
    tertiary = BrandGreenDark,
    background = Color(0xFFF7FAF7), // Pure whitish-green background
    surface = Color(0xFFFFFFFF), // Pure white surfaces
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF1A1F1A),
    onSurface = Color(0xFF1A1F1A)
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Disable dynamicColor to enforce high-contrast brand colors
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
