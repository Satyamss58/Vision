package com.sa.vision.gesture

import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.webkit.WebView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object TouchSimulator {

    /**
     * Simulates MotionEvent TAP (ACTION_DOWN -> ACTION_UP) on target View.
     * Also dispatches DOM JavaScript touch/click event if the target is a WebView.
     */
    fun simulateTap(view: View, x: Float, y: Float, onComplete: ((Boolean) -> Unit)? = null) {
        val downTime = SystemClock.uptimeMillis()
        val downEvent = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, x, y, 0)
        val upEvent = MotionEvent.obtain(downTime, downTime + 50, MotionEvent.ACTION_UP, x, y, 0)

        view.dispatchTouchEvent(downEvent)
        view.dispatchTouchEvent(upEvent)

        downEvent.recycle()
        upEvent.recycle()

        if (view is WebView) {
            dispatchJsClickAt(view, x, y)
        }

        onComplete?.invoke(true)
    }

    /**
     * Simulates MotionEvent HOLD / Long Press (ACTION_DOWN -> delay -> ACTION_UP).
     */
    fun simulateHold(
        view: View,
        x: Float,
        y: Float,
        durationMs: Long = 1000L,
        scope: CoroutineScope,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        val downTime = SystemClock.uptimeMillis()
        val downEvent = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, x, y, 0)
        view.dispatchTouchEvent(downEvent)
        downEvent.recycle()

        scope.launch(Dispatchers.Main) {
            delay(durationMs)
            val upTime = SystemClock.uptimeMillis()
            val upEvent = MotionEvent.obtain(downTime, upTime, MotionEvent.ACTION_UP, x, y, 0)
            view.dispatchTouchEvent(upEvent)
            upEvent.recycle()

            if (view is WebView) {
                dispatchJsLongPressAt(view, x, y)
            }
            onComplete?.invoke(true)
        }
    }

    /**
     * Simulates MotionEvent SCROLL / SWIPE (ACTION_DOWN -> series of ACTION_MOVE -> ACTION_UP).
     */
    fun simulateScroll(
        view: View,
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        durationMs: Long = 300L,
        steps: Int = 10,
        scope: CoroutineScope,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        scope.launch(Dispatchers.Main) {
            val downTime = SystemClock.uptimeMillis()
            val downEvent = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, startX, startY, 0)
            view.dispatchTouchEvent(downEvent)
            downEvent.recycle()

            val stepDuration = (durationMs / steps.coerceAtLeast(1)).coerceAtLeast(10L)

            for (i in 1..steps) {
                delay(stepDuration)
                val alpha = i.toFloat() / steps
                val currentX = startX + (endX - startX) * alpha
                val currentY = startY + (endY - startY) * alpha
                val moveTime = SystemClock.uptimeMillis()
                val moveEvent = MotionEvent.obtain(downTime, moveTime, MotionEvent.ACTION_MOVE, currentX, currentY, 0)
                view.dispatchTouchEvent(moveEvent)
                moveEvent.recycle()
            }

            delay(20)
            val upTime = SystemClock.uptimeMillis()
            val upEvent = MotionEvent.obtain(downTime, upTime, MotionEvent.ACTION_UP, endX, endY, 0)
            view.dispatchTouchEvent(upEvent)
            upEvent.recycle()

            if (view is WebView) {
                val dx = (endX - startX).toInt()
                val dy = (endY - startY).toInt()
                val jsScroll = "window.scrollBy(${-dx}, ${-dy});"
                view.evaluateJavascript(jsScroll, null)
            }

            onComplete?.invoke(true)
        }
    }

    /**
     * Focuses active input field in WebView and types text via JS / key events.
     */
    fun simulateType(
        view: View?,
        text: String,
        cellX: Float? = null,
        cellY: Float? = null,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        if (view is WebView) {
            val escapedText = text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
            val xParam = cellX ?: -1f
            val yParam = cellY ?: -1f

            val jsCode = """
                (function() {
                    let x = $xParam;
                    let y = $yParam;
                    let el = null;
                    if (x > 0 && y > 0) {
                        el = document.elementFromPoint(x, y);
                    }
                    if (!el || el === document.body) {
                        el = document.activeElement;
                    }
                    if (!el || el === document.body) {
                        el = document.querySelector("#prompt-textarea") || 
                             document.querySelector("div[contenteditable='true']") ||
                             document.querySelector("textarea") ||
                             document.querySelector("input[type='text']") ||
                             document.querySelector("input:not([type='hidden'])");
                    }
                    if (el) {
                        el.focus();
                        if (el.tagName === 'TEXTAREA' || el.tagName === 'INPUT') {
                            el.value = "$escapedText";
                            el.dispatchEvent(new Event('input', { bubbles: true }));
                            el.dispatchEvent(new Event('change', { bubbles: true }));
                        } else {
                            el.innerText = "$escapedText";
                            el.dispatchEvent(new Event('input', { bubbles: true }));
                        }
                        return true;
                    }
                    return false;
                })();
            """.trimIndent()

            view.evaluateJavascript(jsCode) { result ->
                onComplete?.invoke(result == "true")
            }
        } else {
            onComplete?.invoke(true)
        }
    }

    private fun dispatchJsClickAt(webView: WebView, x: Float, y: Float) {
        val jsCode = """
            (function() {
                var el = document.elementFromPoint($x, $y);
                if (el) {
                    el.focus();
                    el.click();
                    var evt = new MouseEvent('click', { bubbles: true, cancelable: true, clientX: $x, clientY: $y });
                    el.dispatchEvent(evt);
                }
            })();
        """.trimIndent()
        webView.evaluateJavascript(jsCode, null)
    }

    private fun dispatchJsLongPressAt(webView: WebView, x: Float, y: Float) {
        val jsCode = """
            (function() {
                var el = document.elementFromPoint($x, $y);
                if (el) {
                    el.focus();
                    var evt = new MouseEvent('contextmenu', { bubbles: true, cancelable: true, clientX: $x, clientY: $y });
                    el.dispatchEvent(evt);
                }
            })();
        """.trimIndent()
        webView.evaluateJavascript(jsCode, null)
    }
}
