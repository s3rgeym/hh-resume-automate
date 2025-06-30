package com.github.s3rgeym.hh_resume_automate.ui.components

import android.annotation.SuppressLint
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.github.s3rgeym.hh_resume_automate.api.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun AuthScreen(
    client: ApiClient,
    sharedPrefs: android.content.SharedPreferences,
    snackbarHostState: SnackbarHostState,
    onAuthorized: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    webViewClient = object : WebViewClient() {
                        @Deprecated("Deprecated in Java")
                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            url: String?
                        ): Boolean {
                            if (url?.startsWith("hhandroid://") == true) {
                                val uri = Uri.parse(url)
                                val code = uri.getQueryParameter("code")
                                if (code != null) {
                                    coroutineScope.launch {
                                        try {
                                            withContext(Dispatchers.IO) {
                                                client.authenticate(code)
                                            }
                                            //client.saveToPrefs(sharedPrefs)
                                            onAuthorized()
                                        } catch (e: Exception) {
                                            snackbarHostState.showSnackbar("Ошибка: ${e.message}")
                                        }
                                    }
                                } else {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Код авторизации не найден")
                                    }
                                }
                                return true
                            }
                            return false
                        }
                    }
                    loadUrl(client.getAuthorizeUrl())
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        )
    }
}