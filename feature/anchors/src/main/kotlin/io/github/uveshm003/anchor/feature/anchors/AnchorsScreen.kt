package io.github.uveshm003.anchor.feature.anchors

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.uveshm003.anchor.core.designsystem.component.AnchorIcons
import io.github.uveshm003.anchor.core.designsystem.component.AnchorTopAppBar
import io.github.uveshm003.anchor.core.designsystem.component.EmptyState

/** Placeholder until the Anchors screen lands (docs/mvp.md). */
@Composable
fun AnchorsScreen(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize()) {
        AnchorTopAppBar(title = stringResource(R.string.feature_anchors_title))
        Box(modifier = Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyState(
                icon = AnchorIcons.Nfc,
                title = stringResource(R.string.feature_anchors_title),
                body = stringResource(R.string.feature_anchors_placeholder),
            )
        }
    }
}
