package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ==========================================
// Sophisticated Dark Design Theme Colors
// ==========================================

// Core Surfaces & Canvas
val SophisticatedBg = Color(0xFF111318)               // Main background canvas
val SophisticatedSurface = Color(0xFF1E2025)          // Bottom navigation, app bars, sheets, dialogs
val SophisticatedSurfaceVariant = Color(0xFF2D2F31)   // Chips, cards, text fields, item placeholders
val SophisticatedContainer = Color(0xFF3F4759)        // Active pills, borders, video placeholders
val SophisticatedBorder = Color(0xFF44474F)           // Outline borders

// Primary & Secondary Accents
val SophisticatedPrimary = Color(0xFFA8C7FF)          // Ice blue accent, selected border, badges
val SophisticatedOnPrimary = Color(0xFF003061)        // Contrast text on primary badges & chips
val SophisticatedPrimaryContainer = Color(0xFF00468B) // Deep blue container
val SophisticatedSecondary = Color(0xFFD0BCFF)        // Lavender FAB accent
val SophisticatedOnSecondary = Color(0xFF381E72)      // Contrast text on secondary FAB

// Typography Tokens
val SophisticatedTextPrimary = Color(0xFFE2E2E6)      // High contrast title/body text
val SophisticatedTextSecondary = Color(0xFFC4C6D0)    // Secondary/muted text, chip labels
val SophisticatedTextTertiary = Color(0xFF8E9099)     // Hints, timestamps, placeholders

// Status & Action Tokens
val SophisticatedDestructive = Color(0xFFFFB4AB)      // Destructive red accent
val SophisticatedOnDestructive = Color(0xFF690005)    // Contrast text on destructive button
val SophisticatedDestructiveBg = Color(0xFF93000A)    // Destructive container
val SophisticatedSuccess = Color(0xFF7BD797)          // Success green accent
val SophisticatedSuccessBg = Color(0xFF005322)        // Success container

// Backward Compatibility Aliases for HMX Components
val HmxPrimary = SophisticatedPrimary
val HmxSecondary = SophisticatedSecondary
val HmxAccent = SophisticatedPrimary
val HmxBackgroundDark = SophisticatedBg
val HmxBackgroundCard = SophisticatedSurfaceVariant
val HmxSurfaceDark = SophisticatedSurface
val HmxSurfaceElevated = SophisticatedSurfaceVariant
val HmxGlassOverlay = Color(0x333F4759)
val HmxGlassBorder = Color(0xFF3F4759)
val HmxDestructive = Color(0xFFFF5449)
val HmxSuccess = SophisticatedSuccess
val HmxTextPrimary = SophisticatedTextPrimary
val HmxTextSecondary = SophisticatedTextSecondary
val HmxTextTertiary = SophisticatedTextTertiary

// Theme Gradients
val SophisticatedBrandGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFFA8C7FF), Color(0xFF80A9FF))
)

val SophisticatedFabGradient = Brush.linearGradient(
    colors = listOf(Color(0xFFD0BCFF), Color(0xFFC0A0FF))
)

val SophisticatedSetupGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFF111318), Color(0xFF1A1C22), Color(0xFF111318))
)

val HmxBrandGradient = SophisticatedBrandGradient
val HmxSetupGradient = SophisticatedSetupGradient
val HmxGalleryBackgroundGradient = Brush.verticalGradient(
    colors = listOf(SophisticatedBg, SophisticatedSurface)
)

