package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val WhiteIdeColorScheme = lightColorScheme(
  primary = IdeBluePrimary,
  onPrimary = IdeWhite,
  primaryContainer = IdeBlueSoft,
  onPrimaryContainer = IdeBluePrimary,
  secondary = IdeCyanAccent,
  onSecondary = IdeWhite,
  secondaryContainer = IdeSurfaceVariant,
  onSecondaryContainer = IdeTextPrimary,
  tertiary = IdeEmeraldGreen,
  onTertiary = IdeWhite,
  background = IdeBackground,
  onBackground = IdeTextPrimary,
  surface = IdeSurface,
  onSurface = IdeTextPrimary,
  surfaceVariant = IdeSurfaceVariant,
  onSurfaceVariant = IdeTextSecondary,
  outline = IdeBorder,
  outlineVariant = IdeBorderDark,
  error = IdeRoseError,
  onError = IdeWhite
)

@Composable
fun MyApplicationTheme(
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = WhiteIdeColorScheme,
    typography = Typography,
    content = content
  )
}

