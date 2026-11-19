package io.github.uveshm003.anchor.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight

private val Base = Typography()

/** Material 3 type scale on the system font (no bundled fonts), with heavier headings for hierarchy. */
internal val AnchorTypography = Base.copy(
    displayLarge = Base.displayLarge.copy(fontWeight = FontWeight.SemiBold),
    displayMedium = Base.displayMedium.copy(fontWeight = FontWeight.SemiBold),
    displaySmall = Base.displaySmall.copy(fontWeight = FontWeight.SemiBold),
    headlineLarge = Base.headlineLarge.copy(fontWeight = FontWeight.SemiBold),
    headlineMedium = Base.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
    headlineSmall = Base.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
    titleLarge = Base.titleLarge.copy(fontWeight = FontWeight.Medium),
)
