package io.github.uveshm003.anchor.feature.home.scan

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.uveshm003.anchor.core.designsystem.component.AnchorButton
import io.github.uveshm003.anchor.core.designsystem.component.AnchorIconButton
import io.github.uveshm003.anchor.core.designsystem.component.AnchorIcons
import io.github.uveshm003.anchor.core.designsystem.component.AnchorTopAppBar
import io.github.uveshm003.anchor.core.qr.QrAnalyzer
import io.github.uveshm003.anchor.feature.home.R
import java.util.concurrent.Executors

@Composable
fun ScanRoute(onClose: () -> Unit, viewModel: ScanViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED,
        )
    }
    // The camera permission is asked for only here, when the user chose to scan.
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    LaunchedEffect(Unit) { if (!granted) permission.launch(Manifest.permission.CAMERA) }
    ScanScreen(
        state = state,
        cameraGranted = granted,
        onClose = onClose,
        onScanAgain = viewModel::reset,
        onAskPermission = { permission.launch(Manifest.permission.CAMERA) },
        camera = { CameraPreview(viewModel::onScanned) },
    )
}

@Composable
fun ScanScreen(
    state: ScanState,
    cameraGranted: Boolean,
    onClose: () -> Unit,
    onScanAgain: () -> Unit,
    onAskPermission: () -> Unit,
    modifier: Modifier = Modifier,
    camera: @Composable () -> Unit = {},
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            AnchorTopAppBar(
                title = stringResource(R.string.feature_home_scan_title),
                navigationIcon = {
                    AnchorIconButton(AnchorIcons.Close, stringResource(R.string.feature_home_close), onClose)
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.widthIn(max = 560.dp).fillMaxWidth().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val scanning = state == ScanState.Scanning || state == ScanState.NotAnAnchor
                when {
                    !cameraGranted -> {
                        Text(stringResource(R.string.feature_home_scan_permission))
                        AnchorButton(text = stringResource(R.string.feature_home_scan_allow), onClick = onAskPermission)
                    }

                    scanning -> {
                        Box(Modifier.fillMaxWidth().aspectRatio(1f)) { camera() }
                        Message(
                            if (state ==
                                ScanState.NotAnAnchor
                            ) {
                                R.string.feature_home_scan_not_anchor
                            } else {
                                R.string.feature_home_scan_hint
                            },
                        )
                    }

                    else -> {
                        Result(state)
                        AnchorButton(text = stringResource(R.string.feature_home_close), onClick = onClose)
                        if (state !is ScanState.Ended && state !is ScanState.Started) {
                            AnchorButton(text = stringResource(R.string.feature_home_scan_again), onClick = onScanAgain)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Result(state: ScanState) {
    val text = when (state) {
        is ScanState.Started -> stringResource(R.string.feature_home_scan_started, state.modeName)

        ScanState.Ended -> stringResource(R.string.feature_home_scan_ended)

        is ScanState.WrongAnchor -> state.name?.let { stringResource(R.string.feature_home_scan_wrong_named, it) }
            ?: stringResource(R.string.feature_home_scan_wrong)

        ScanState.Unknown -> stringResource(R.string.feature_home_scan_unknown)

        else -> stringResource(R.string.feature_home_scan_rejected)
    }
    Text(
        text,
        style = MaterialTheme.typography.headlineSmall,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
}

@Composable
private fun Message(text: Int) {
    Text(
        stringResource(text),
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.semantics {
            liveRegion =
                LiveRegionMode.Polite
        },
    )
}

/** Back camera preview with an offline QR analyzer, bound to this composable's lifecycle. */
@Composable
private fun CameraPreview(onText: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnText by rememberUpdatedState(onText)
    val executor = remember { Executors.newSingleThreadExecutor() }
    val previewView = remember { PreviewView(context) }
    DisposableEffect(lifecycleOwner) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            val provider = providerFuture.get()
            val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { it.setAnalyzer(executor, QrAnalyzer { text -> currentOnText(text) }) }
            provider.unbindAll()
            provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
        }, ContextCompat.getMainExecutor(context))
        onDispose {
            if (providerFuture.isDone) providerFuture.get().unbindAll()
            executor.shutdown()
        }
    }
    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
}
