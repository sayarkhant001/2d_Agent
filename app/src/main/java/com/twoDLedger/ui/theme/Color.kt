package com.twoDLedger.ui.theme

import androidx.compose.ui.graphics.Color

// ── 2D Ledger: Royal Cobalt & Electric Cyan / Solar Gold FinTech Palette ────
// Distinct from 3D Agent: Modern deep royal blue, vibrant cyan, and solar amber

val md_theme_light_primary            = Color(0xFF1D4ED8)  // Royal Cobalt Blue
val md_theme_light_onPrimary          = Color(0xFFFFFFFF)
val md_theme_light_primaryContainer   = Color(0xFFDBEAFE)  // Soft Ice Blue container
val md_theme_light_onPrimaryContainer = Color(0xFF1E3A8A)

val md_theme_light_secondary          = Color(0xFFD97706)  // Radiant Solar Gold
val md_theme_light_onSecondary        = Color(0xFFFFFFFF)
val md_theme_light_secondaryContainer = Color(0xFFFEF3C7)  // Soft Amber Chip
val md_theme_light_onSecondaryContainer = Color(0xFF78350F)

val md_theme_light_tertiary           = Color(0xFF0891B2)  // Electric Cyan
val md_theme_light_onTertiary         = Color(0xFFFFFFFF)
val md_theme_light_tertiaryContainer  = Color(0xFFCFFAFE)
val md_theme_light_onTertiaryContainer = Color(0xFF164E63)

val md_theme_light_error              = Color(0xFFDC2626)  // Crimson
val md_theme_light_errorContainer     = Color(0xFFFEE2E2)
val md_theme_light_onError            = Color(0xFFFFFFFF)
val md_theme_light_onErrorContainer   = Color(0xFF7F1D1D)

val md_theme_light_background         = Color(0xFFF8FAFC)  // Clean crisp Slate tint
val md_theme_light_onBackground       = Color(0xFF0F172A)
val md_theme_light_surface            = Color(0xFFFFFFFF)  // Pure card canvas
val md_theme_light_onSurface          = Color(0xFF0F172A)
val md_theme_light_surfaceVariant     = Color(0xFFF1F5F9)
val md_theme_light_onSurfaceVariant   = Color(0xFF334155)
val md_theme_light_outline            = Color(0xFFCBD5E1)
val md_theme_light_outlineVariant     = Color(0xFFE2E8F0)

// ── Dark theme: Midnight Navy & Glowing Cyan / Electric Blue ────────────────
val md_theme_dark_primary             = Color(0xFF60A5FA)  // Glowing Blue
val md_theme_dark_onPrimary           = Color(0xFF0B1938)
val md_theme_dark_primaryContainer    = Color(0xFF1E40AF)
val md_theme_dark_onPrimaryContainer  = Color(0xFFDBEAFE)

val md_theme_dark_secondary           = Color(0xFFFBBF24)  // Radiant Amber
val md_theme_dark_onSecondary         = Color(0xFF451A03)
val md_theme_dark_secondaryContainer  = Color(0xFF78350F)
val md_theme_dark_onSecondaryContainer = Color(0xFFFEF3C7)

val md_theme_dark_tertiary            = Color(0xFF22D3EE)  // Electric Cyan
val md_theme_dark_onTertiary          = Color(0xFF083344)
val md_theme_dark_tertiaryContainer   = Color(0xFF0E7490)
val md_theme_dark_onTertiaryContainer = Color(0xFFCFFAFE)

val md_theme_dark_error               = Color(0xFFF87171)
val md_theme_dark_errorContainer      = Color(0xFF7F1D1D)
val md_theme_dark_onError             = Color(0xFF450A0A)
val md_theme_dark_onErrorContainer    = Color(0xFFFECACA)

val md_theme_dark_background          = Color(0xFF0A0F1D)  // Deep Midnight Navy
val md_theme_dark_onBackground        = Color(0xFFF1F5F9)
val md_theme_dark_surface             = Color(0xFF111827)  // Elevated Slate Navy
val md_theme_dark_onSurface           = Color(0xFFF1F5F9)
val md_theme_dark_surfaceVariant      = Color(0xFF1E293B)
val md_theme_dark_onSurfaceVariant    = Color(0xFF94A3B8)
val md_theme_dark_outline             = Color(0xFF334155)
val md_theme_dark_outlineVariant      = Color(0xFF1E293B)

// ── Semantic Color Tokens for 2D Screens ─────────────────────────────────────
val CobaltPrimary       = Color(0xFF1D4ED8)
val CobaltLight         = Color(0xFFDBEAFE)
val CobaltDark          = Color(0xFF1E3A8A)
val CobaltMedium        = Color(0xFF2563EB)
val CobaltSoftBg        = Color(0xFFF8FAFC)

// Aliases for seamless UI compatibility
val EmeraldPrimary      = CobaltPrimary
val EmeraldLight        = CobaltLight
val EmeraldDark         = CobaltDark
val EmeraldMedium       = CobaltMedium
val EmeraldSoftBg       = CobaltSoftBg

val GoldAccent          = Color(0xFFD97706)
val GoldContainer       = Color(0xFFFEF3C7)
val GoldDark            = Color(0xFF92400E)

// Payout / Winner Semantic Chips
val WinExactRed         = Color(0xFFDC2626)  // ပေါက်သီး (Direct Match)
val WinExactBg          = Color(0xFFFEE2E2)
val WinPermGold         = Color(0xFFD97706)  // ပတ်လည်
val WinPermBg           = Color(0xFFFEF3C7)
val WinNearBlue         = Color(0xFF0284C7)  // ကပ်သီး
val WinNearBg           = Color(0xFFE0F2FE)

val CardBorderSubtle    = Color(0xFFE2E8F0)
val CardBorderDark      = Color(0xFF334155)

// ── Keypad Color Tokens ──────────────────────────────────────────────────────
val KeypadDigitBgLight   = Color(0xFFFFFFFF)
val KeypadDigitBgDark    = Color(0xFF1E293B)
val KeypadDigitText      = Color(0xFF0F172A)
val KeypadBorderLight    = Color(0xFFCBD5E1)
val KeypadShadowLight    = Color(0xFF94A3B8)

val KeypadActionEmerald  = Color(0xFF2563EB)  // R (Permutation) key (Royal Blue)
val KeypadActionTeal     = Color(0xFF0891B2)  // Doubles (အပူး) key (Cyan)
val KeypadBackspaceRed   = Color(0xFFDC2626)  // ⌫ Backspace key
val KeypadClearAmber     = Color(0xFFD97706)  // ရှင်း Clear key
val KeypadSubmitGold     = Color(0xFFEAB308)  // OK / Enter key
val KeypadSubmitBg       = Color(0xFF1D4ED8)  // Royal Cobalt Submit
val KeypadFocusRing      = Color(0xFF3B82F6)  // Glowing Blue focus ring

// ── 2D Modern Dark FinTech Theme Colors ─────────────────────────────────────
val SlateBackground     = Color(0xFF0A0F1D)
val SlateDarkBackground = Color(0xFF060913)
val SlateSurface        = Color(0xFF111827)
val SlateSurfaceVariant = Color(0xFF1E293B)
val CardBorder          = Color(0xFF1E293B)
val PrimaryGold         = Color(0xFFF59E0B)
val TextPrimary         = Color(0xFFF8FAFC)
val TextSecondary       = Color(0xFF94A3B8)
val TextMuted           = Color(0xFF64748B)
