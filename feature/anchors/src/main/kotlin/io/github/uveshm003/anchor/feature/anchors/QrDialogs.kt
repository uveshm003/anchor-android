package io.github.uveshm003.anchor.feature.anchors

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.uveshm003.anchor.core.designsystem.component.AnchorIcons
import io.github.uveshm003.anchor.core.model.AnchorType
import io.github.uveshm003.anchor.core.nfc.NfcAvailability
import io.github.uveshm003.anchor.core.qr.QrAnchorCode
import io.github.uveshm003.anchor.core.qr.QrExport

/** NFC sticker (when NFC is on) or a printable QR code (any phone). */
@Composable
internal fun ChooseKindDialog(nfc: NfcAvailability, onChoose: (AnchorType) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.feature_anchors_choose_title)) },
        text = { Text(stringResource(R.string.feature_anchors_choose_body)) },
        confirmButton = {
            Column(horizontalAlignment = Alignment.End) {
                TextButton(onClick = { onChoose(AnchorType.Nfc) }, enabled = nfc == NfcAvailability.Enabled) {
                    Text(stringResource(R.string.feature_anchors_choose_nfc))
                }
                TextButton(onClick = {
                    onChoose(AnchorType.Qr)
                }) { Text(stringResource(R.string.feature_anchors_choose_qr)) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.feature_anchors_cancel)) }
            }
        },
    )
}

/** Shows the new QR code once; Anchor keeps only its hash, so printing or sharing has to happen now. */
@Composable
internal fun QrCreatedDialog(created: Pairing.QrCreated, onDone: () -> Unit) {
    val context = LocalContext.current
    val bitmap = remember(created.payload) { QrAnchorCode.bitmap(created.payload, QR_PX).asImageBitmap() }
    val instructions = stringResource(R.string.feature_anchors_qr_print_instructions)
    val description = stringResource(R.string.feature_anchors_qr_image, created.name)
    AlertDialog(
        onDismissRequest = {},
        icon = { Icon(painterResource(AnchorIcons.QrCode), contentDescription = null) },
        title = { Text(created.name) },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Image(
                    bitmap = bitmap,
                    contentDescription = description,
                    modifier = Modifier.size(220.dp).background(Color.White).padding(8.dp),
                )
                Text(stringResource(R.string.feature_anchors_qr_once))
            }
        },
        confirmButton = {
            TextButton(onClick = {
                context.startActivity(
                    QrExport(context.applicationContext).shareIntent(created.name, created.payload, instructions),
                )
            }) { Text(stringResource(R.string.feature_anchors_qr_share)) }
        },
        dismissButton = { TextButton(onClick = onDone) { Text(stringResource(R.string.feature_anchors_done)) } },
    )
}

private const val QR_PX = 512
