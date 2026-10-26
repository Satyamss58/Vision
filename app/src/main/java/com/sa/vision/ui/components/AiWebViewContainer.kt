package com.sa.vision.ui.components

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Bitmap
import android.os.Build
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.sa.vision.model.AiPlatform
import com.sa.vision.model.GridConfig
import com.sa.vision.util.ScreenCaptureUtil
import kotlinx.coroutines.launch
import java.util.EnumMap

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun AiWebViewContainer(
    selectedPlatform: AiPlatform,
    pendingPromptToSend: String?,
    isGridOverlayVisible: Boolean = false,
    gridConfig: GridConfig = GridConfig(),
    captureTrigger: Long = 0L,
    onPromptSent: () -> Unit,
    onNavigationStateChanged: (canGoBack: Boolean, canGoForward: Boolean) -> Unit,
    onProgressChanged: (isLoading: Boolean, progress: Int) -> Unit,
    onWebViewCaptured: ((Bitmap) -> Unit)? = null,
    onReloadRequested: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Store WebViews for each AI platform in a persistent map so tab switching preserves session state
    val webViewMap = remember { EnumMap<AiPlatform, WebView>(AiPlatform::class.java) }

    DisposableEffect(Unit) {
        onDispose {
            webViewMap.values.forEach { webView ->
                webView.stopLoading()
                webView.destroy()
            }
            webViewMap.clear()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AiPlatform.entries.forEach { platform ->
            val isSelected = platform == selectedPlatform

            AndroidView(
                factory = { ctx ->
                    webViewMap.getOrPut(platform) {
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )

                            // 1. Enable Cookie persistence
                            val cookieManager = CookieManager.getInstance()
                            cookieManager.setAcceptCookie(true)
                            cookieManager.setAcceptThirdPartyCookies(this, true)

                            // 2. WebSettings configuration
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                databaseEnabled = true
                                allowFileAccess = true
                                allowContentAccess = true
                                loadWithOverviewMode = true
                                useWideViewPort = true
                                javaScriptCanOpenWindowsAutomatically = true
                                setSupportMultipleWindows(false)
                                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE

                                // Custom modern Chrome User-Agent to prevent login restriction issues
                                userAgentString = "Mozilla/5.0 (Linux; Android 13; SM-S918B) AppleWebKit/537.36 (KHTML, Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                            }

                            // 3. Set WebChromeClient & WebViewClient
                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    if (isSelected) {
                                        onProgressChanged(newProgress < 100, newProgress)
                                    }
                                }

                                override fun onPermissionRequest(request: PermissionRequest?) {
                                    request?.grant(request.resources)
                                }
                            }

                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    super.onPageStarted(view, url, favicon)
                                    if (isSelected) {
                                        onNavigationStateChanged(view?.canGoBack() == true, view?.canGoForward() == true)
                                    }
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    // Flush cookies to disk when page finishes loading
                                    CookieManager.getInstance().flush()
                                    if (isSelected) {
                                        onNavigationStateChanged(view?.canGoBack() == true, view?.canGoForward() == true)
                                    }
                                }

                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): Boolean {
                                    return false
                                }
                            }

                            // Load platform initial URL
                            loadUrl(platform.url)
                        }
                    }
                },
                update = { webView ->
                    if (isSelected) {
                        onNavigationStateChanged(webView.canGoBack(), webView.canGoForward())
                    }
                },
                modifier = Modifier.fillMaxSize(),
                onRelease = {}
            )
        }

        // Render Compose Grid Overlay Canvas layer when enabled
        if (isGridOverlayVisible) {
            GridOverlayCanvas(
                gridConfig = gridConfig,
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    // Effect to handle prompt injection into the active WebView when requested
    LaunchedEffect(pendingPromptToSend, selectedPlatform) {
        if (!pendingPromptToSend.isNullOrBlank()) {
            val activeWebView = webViewMap[selectedPlatform]
            activeWebView?.let { webView ->
                injectPromptIntoWebView(webView, pendingPromptToSend)
                onPromptSent()
            }
        }
    }

    // Effect to handle screen capture requests
    LaunchedEffect(captureTrigger) {
        if (captureTrigger > 0L) {
            val activeWebView = webViewMap[selectedPlatform]
            if (activeWebView != null && onWebViewCaptured != null) {
                val activity = context as? Activity
                val bitmap = ScreenCaptureUtil.captureViewBitmap(activeWebView, activity)
                if (bitmap != null) {
                    onWebViewCaptured(bitmap)
                }
            }
        }
    }
}

/**
 * Helper to get or control active webview navigation actions.
 */
fun performWebViewBack(webViewMap: EnumMap<AiPlatform, WebView>, platform: AiPlatform): Boolean {
    val webView = webViewMap[platform]
    return if (webView != null && webView.canGoBack()) {
        webView.goBack()
        true
    } else false
}

fun performWebViewForward(webViewMap: EnumMap<AiPlatform, WebView>, platform: AiPlatform): Boolean {
    val webView = webViewMap[platform]
    return if (webView != null && webView.canGoForward()) {
        webView.goForward()
        true
    } else false
}

fun performWebViewReload(webViewMap: EnumMap<AiPlatform, WebView>, platform: AiPlatform) {
    webViewMap[platform]?.reload()
}

/**
 * Injects prompt text into the active AI Web platform's input field using JS DOM manipulation.
 */
private fun injectPromptIntoWebView(webView: WebView, text: String) {
    val escapedText = text.replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")

    val jsCode = """
        (function() {
            const promptText = "$escapedText";
            
            // Try common input element selectors across Gemini, Claude, and ChatGPT
            let el = document.querySelector("#prompt-textarea") || 
                     document.querySelector("div[contenteditable='true']") ||
                     document.querySelector("textarea") ||
                     document.querySelector("p.data-placeholder");
                     
            if (el) {
                el.focus();
                if (el.tagName === 'TEXTAREA' || el.tagName === 'INPUT') {
                    el.value = promptText;
                    el.dispatchEvent(new Event('input', { bubbles: true }));
                    el.dispatchEvent(new Event('change', { bubbles: true }));
                } else {
                    el.innerText = promptText;
                    el.dispatchEvent(new Event('input', { bubbles: true }));
                }
                return "SUCCESS";
            }
            return "ELEMENT_NOT_FOUND";
        })();
    """.trimIndent()

    webView.evaluateJavascript(jsCode, null)
}
