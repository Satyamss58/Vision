package com.sa.vision

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.sa.vision.automation.LoopStatus
import com.sa.vision.ui.components.AiTabRow
import com.sa.vision.ui.components.AiWebViewContainer
import com.sa.vision.ui.components.AutomationOverlay
import com.sa.vision.ui.components.CapturedInspectionDialog
import com.sa.vision.ui.components.TopPromptBar
import com.sa.vision.ui.components.WebControlBar
import com.sa.vision.ui.theme.VisionTheme
import com.sa.vision.ui.viewmodel.MainViewModel
import com.sa.vision.util.GridOverlayUtil
import com.sa.vision.util.ScreenCaptureUtil
import com.sa.vision.util.SystemPromptUtil
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            VisionTheme {
                VisionMainScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun VisionMainScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var pendingPromptToSend by remember { mutableStateOf<String?>(null) }
    var completionDialogMessage by remember { mutableStateOf<String?>(null) }

    // Speech recognition result launcher
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.setListening(false)
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = matches?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                viewModel.appendPromptText(spokenText)
                scope.launch {
                    snackbarHostState.showSnackbar("Speech recognized: $spokenText")
                }
            }
        }
    }

    // Permission launcher for Mic
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchVoiceInput(context, speechLauncher) { isListening ->
                viewModel.setListening(isListening)
            }
        } else {
            Toast.makeText(context, "Microphone permission is required for voice input", Toast.LENGTH_SHORT).show()
        }
    }

    val onVoiceClick = {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            launchVoiceInput(context, speechLauncher) { isListening ->
                viewModel.setListening(isListening)
            }
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val onSendClick = {
        if (uiState.promptText.isNotBlank()) {
            val textToSend = uiState.promptText.trim()
            pendingPromptToSend = textToSend

            copyToClipboard(context, textToSend)

            scope.launch {
                snackbarHostState.showSnackbar("Prompt submitted to ${uiState.selectedPlatform.displayName}")
            }
        }
    }

    val onStartAutomation = {
        val task = uiState.promptText.ifBlank { "Perform vision task" }
        viewModel.startVisionAutomation(task)
        scope.launch {
            snackbarHostState.showSnackbar("Vision Automation Loop Started")
        }
    }

    // Monitor completion / failure state of automation loop
    LaunchedEffect(uiState.automationState.status) {
        when (uiState.automationState.status) {
            LoopStatus.COMPLETED -> {
                completionDialogMessage = "Automation Completed!\nResult: ${uiState.automationState.resultMessage ?: "Success"}"
            }
            LoopStatus.FAILED -> {
                completionDialogMessage = "Automation Failed.\nError: ${uiState.automationState.errorMessage ?: "Unknown error"}"
            }
            else -> {}
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Column {
                TopPromptBar(
                    promptText = uiState.promptText,
                    onPromptChange = { viewModel.updatePromptText(it) },
                    onSendPrompt = onSendClick,
                    onVoiceInputClick = onVoiceClick,
                    onStartAutomationClick = { onStartAutomation() },
                    isAutomationActive = uiState.automationState.status != LoopStatus.IDLE,
                    isListening = uiState.isListening
                )
                AiTabRow(
                    selectedPlatform = uiState.selectedPlatform,
                    onPlatformSelected = { viewModel.selectPlatform(it) }
                )
                AnimatedVisibility(
                    visible = uiState.isLoading || uiState.isCapturing,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    LinearProgressIndicator(
                        progress = { if (uiState.isCapturing) 1f else uiState.progress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                }
            }
        },
        bottomBar = {
            WebControlBar(
                canGoBack = uiState.canGoBack,
                canGoForward = uiState.canGoForward,
                isGridOverlayVisible = uiState.isGridOverlayVisible,
                onBackClick = {},
                onForwardClick = {},
                onRefreshClick = {},
                onHomeClick = {
                    viewModel.selectPlatform(uiState.selectedPlatform)
                },
                onToggleGridClick = {
                    viewModel.toggleGridOverlay()
                    scope.launch {
                        val status = if (!uiState.isGridOverlayVisible) "Grid Overlay Enabled" else "Grid Overlay Disabled"
                        snackbarHostState.showSnackbar(status)
                    }
                },
                onCaptureScreenClick = {
                    viewModel.triggerScreenCapture()
                },
                onCopyPromptClick = {
                    if (uiState.promptText.isNotBlank()) {
                        copyToClipboard(context, uiState.promptText)
                        scope.launch {
                            snackbarHostState.showSnackbar("Prompt copied to clipboard")
                        }
                    } else {
                        Toast.makeText(context, "Prompt field is empty", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AiWebViewContainer(
                selectedPlatform = uiState.selectedPlatform,
                pendingPromptToSend = pendingPromptToSend,
                isGridOverlayVisible = uiState.isGridOverlayVisible,
                gridConfig = uiState.gridConfig,
                captureTrigger = uiState.captureTrigger,
                onPromptSent = {
                    pendingPromptToSend = null
                },
                onNavigationStateChanged = { canGoBack, canGoForward ->
                    viewModel.updateNavigationState(canGoBack, canGoForward)
                },
                onProgressChanged = { isLoading, progress ->
                    viewModel.updateLoadingProgress(isLoading, progress)
                },
                onWebViewCaptured = { rawBitmap ->
                    scope.launch {
                        val composited = GridOverlayUtil.drawGridOnBitmap(rawBitmap, uiState.gridConfig)
                        val scaledComposited = ScreenCaptureUtil.scaleBitmap(composited, 1080)
                        val base64 = ScreenCaptureUtil.bitmapToBase64(scaledComposited)

                        val densityDpi = context.resources.displayMetrics.densityDpi
                        val promptContext = SystemPromptUtil.buildVisionSystemPrompt(
                            screenWidth = rawBitmap.width,
                            screenHeight = rawBitmap.height,
                            gridConfig = uiState.gridConfig,
                            densityDpi = densityDpi,
                            userTask = uiState.promptText.ifBlank { null }
                        )

                        viewModel.onBitmapCaptured(
                            rawBitmap = rawBitmap,
                            compositedBitmap = scaledComposited,
                            promptContext = promptContext,
                            base64 = base64
                        )

                        if (uiState.automationState.status == LoopStatus.IDLE) {
                            snackbarHostState.showSnackbar("Screen capture & grid overlay generated!")
                        }
                    }
                }
            )

            // Automation Overlay Layer
            AutomationOverlay(
                automationState = uiState.automationState,
                onStopClick = { viewModel.stopVisionAutomation() },
                onPauseClick = { viewModel.pauseVisionAutomation() },
                onResumeClick = { viewModel.resumeVisionAutomation() }
            )
        }

        // Display Inspection Modal Dialog when manual screen capture is ready (and not automating)
        if (uiState.automationState.status == LoopStatus.IDLE) {
            uiState.lastCompositedBitmap?.let { bitmap ->
                CapturedInspectionDialog(
                    bitmap = bitmap,
                    gridConfig = uiState.gridConfig,
                    promptContext = uiState.lastPromptContext ?: "",
                    base64String = uiState.base64Image ?: "",
                    onDismissRequest = {
                        viewModel.clearCapturedState()
                    },
                    onCopyPrompt = { text ->
                        copyToClipboard(context, text)
                        scope.launch {
                            snackbarHostState.showSnackbar("Vision AI Prompt copied to clipboard")
                        }
                    },
                    onUsePromptInInput = { text ->
                        viewModel.appendPromptText(text)
                        viewModel.clearCapturedState()
                        scope.launch {
                            snackbarHostState.showSnackbar("Vision AI Prompt appended to input bar")
                        }
                    }
                )
            }
        }

        // Display Final Automation Result Dialog
        completionDialogMessage?.let { message ->
            AlertDialog(
                onDismissRequest = { completionDialogMessage = null },
                title = { Text(text = "Vision Automation Result") },
                text = { Text(text = message) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            completionDialogMessage = null
                            viewModel.stopVisionAutomation()
                        }
                    ) {
                        Text("OK")
                    }
                }
            )
        }
    }
}

private fun launchVoiceInput(
    context: Context,
    launcher: androidx.activity.result.ActivityResultLauncher<Intent>,
    onStateChange: (Boolean) -> Unit
) {
    try {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your prompt for AI...")
        }
        onStateChange(true)
        launcher.launch(intent)
    } catch (e: Exception) {
        onStateChange(false)
        Toast.makeText(context, "Voice recognition is not available on this device", Toast.LENGTH_SHORT).show()
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("AI Prompt", text)
    clipboard.setPrimaryClip(clip)
}
