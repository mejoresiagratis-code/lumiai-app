package com.mejoresiagratis.lumiai.ui.settings

import android.annotation.SuppressLint
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import com.mejoresiagratis.lumiai.R

/**
 * Política de privacidad y Términos: WebView INTERNO, no un Intent al navegador (14-ago, petición
 * explícita de Pablo) — el patrón de la mayoría de apps: se lee sin salir nunca de LumiAI, un
 * único botón de volver (barra superior + gesto/hardware, ambos mapeados al historial del propio
 * WebView antes de cerrar la pantalla). Navegación restringida al MISMO DOMINIO por seguridad: si
 * el documento legal enlazara alguna vez a otro sitio, esa carga se ignora en vez de seguirse —
 * "nunca sale de la app" también cubre los enlaces internos del propio documento.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalWebScreen(
    title: String,
    url: String,
    onBack: () -> Unit
) {
    var loading by remember { mutableStateOf(true) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    fun handleBack() {
        val wv = webViewRef
        if (wv != null && wv.canGoBack()) wv.goBack() else onBack()
    }

    // Gesto/botón físico de atrás: retrocede en el documento antes de cerrar la pantalla.
    BackHandler(enabled = true) { handleBack() }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = { handleBack() }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_back),
                            contentDescription = stringResource(R.string.back_cd)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors()
            )
        }
    ) { padding ->
        if (loading) {
            LinearProgressIndicator(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxWidth()
            )
        }
        LegalWebView(
            url = url,
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            onWebViewCreated = { webViewRef = it },
            onLoadingChange = { loading = it }
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun LegalWebView(
    url: String,
    modifier: Modifier,
    onWebViewCreated: (WebView) -> Unit,
    onLoadingChange: (Boolean) -> Unit
) {
    val host = remember(url) { runCatching { url.toUri().host }.getOrNull() }
    val latestUrl = rememberUpdatedState(url)

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                // Documento propio y controlado (mejoresiagratis.com) — JS habilitado para que
                // se renderice igual que en el navegador; el riesgo lo acota la restricción de
                // dominio de abajo, no la ausencia de JS.
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView,
                        request: WebResourceRequest
                    ): Boolean {
                        val reqHost = request.url.host
                        // true = bloquear (no se sigue); false = dejar navegar dentro del WebView.
                        return host != null && reqHost != host
                    }
                    override fun onPageFinished(view: WebView, url: String?) {
                        onLoadingChange(false)
                    }
                }
                loadUrl(latestUrl.value)
                onWebViewCreated(this)
            }
        }
    )
}
