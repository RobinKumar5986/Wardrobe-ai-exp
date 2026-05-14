package com.kgjr.wardrobeaiexp

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.util.Base64
import android.view.ViewGroup
import android.webkit.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.kgjr.wardrobeaiexp.ui.theme.WardrobeAiExpTheme
import java.io.ByteArrayOutputStream
import androidx.core.graphics.createBitmap

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WardrobeAiExpTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SimpleDarkWebView("https://gemini.google.com")
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SimpleDarkWebView(url: String) {
    val context = LocalContext.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = androidx.compose.ui.graphics.Color.Black
    ) { padding ->
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            factory = { ctx ->
                WebView(ctx).apply {
                    setBackgroundColor(Color.BLACK)
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        allowFileAccess = true
                        allowContentAccess = true
                    }

                    if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                        WebSettingsCompat.setForceDark(settings, WebSettingsCompat.FORCE_DARK_ON)
                    }

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)

                            val personBase64 = getBase64FromDrawable(context, R.drawable.person)
                            val shirtBase64 = getBase64FromDrawable(context, R.drawable.shirt)

                            val js = """
                                (function() {
                                    function tryInject() {
                                        const inputArea = document.querySelector('div[contenteditable="true"]');
                                        
                                        if (inputArea) {
                                            const injectAll = async () => {
                                                try {
                                                    const pasteImage = async (base64, name) => {
                                                        const res = await fetch('data:image/png;base64,' + base64);
                                                        const blob = await res.blob();
                                                        const file = new File([blob], name, { type: 'image/png' });
                                                        const dataTransfer = new DataTransfer();
                                                        dataTransfer.items.add(file);
                                                        const event = new ClipboardEvent('paste', {
                                                            clipboardData: dataTransfer,
                                                            bubbles: true,
                                                            cancelable: true
                                                        });
                                                        inputArea.dispatchEvent(event);
                                                    };

                                                    // 1. Paste Person
                                                    await pasteImage('$personBase64', 'person.png');
                                                    await new Promise(r => setTimeout(r, 1000));

                                                    // 2. Paste Shirt
                                                    await pasteImage('$shirtBase64', 'shirt.png');
                                                    await new Promise(r => setTimeout(r, 1000));

                                                    // 3. Inject Text Prompt
                                                    inputArea.focus();
                                                    document.execCommand('insertText', false, "The shirt image is here and the person image is there. Please make the person try this shirt on and generate an image of them in an 'X' pose.");
                                                    
                                                    // Trigger input event to enable Send button
                                                    inputArea.dispatchEvent(new Event('input', { bubbles: true }));
                                                    
                                                    // 4. Click Send Button
//                                                    setTimeout(() => {
//                                                        const sendButton = document.querySelector('button[aria-label*="Send"], button.send-button, .send-icon-container button');
//                                                        if (sendButton) {
//                                                            sendButton.click();
//                                                        }
//                                                    }, 1000);
                                                    
                                                } catch (e) {
                                                    console.error("Injection failed", e);
                                                }
                                            };
                                            injectAll();
                                        } else {
                                            setTimeout(tryInject, 2000);
                                        }
                                    }
                                    tryInject();
                                })();
                            """.trimIndent()

                            view?.evaluateJavascript(js, null)
                        }
                    }
                    loadUrl(url)
                }
            }
        )
    }
}

fun getBase64FromDrawable(context: android.content.Context, drawableId: Int): String {
    val drawable = ContextCompat.getDrawable(context, drawableId) ?: return ""
    val bitmap = createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)

    val outputStream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
    return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
}