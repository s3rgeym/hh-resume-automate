package com.github.s3rgeym.hh_resume_automate.ui.components

import android.content.SharedPreferences
import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.github.s3rgeym.hh_resume_automate.R
import com.github.s3rgeym.hh_resume_automate.api.ApiClient
import com.github.s3rgeym.hh_resume_automate.api.ApiException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AdvancedOptionsScreen(
    client: ApiClient,
    sharedPrefs: SharedPreferences,
    navController: NavController,
    snackbarHostState: SnackbarHostState,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Log.d("AdvancedOptionsScreen", "Компонент загружен")

    val accessExpiresAtDate = if (client.accessExpiresAt > 0) {
        Date(client.accessExpiresAt)
    } else {
        null
    }

    val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault())
    val formattedExpiryDate = accessExpiresAtDate?.let { dateFormat.format(it) } ?: "Неизвестно"

    fun refreshAccessToken() {
        coroutineScope.launch(Dispatchers.IO) {  // <-- Переключаем корутину на IO
            try {
                Log.d("AdvancedOptionsScreen", "Начало обновления токена")
                client.refreshAccessToken()
                Log.d("AdvancedOptionsScreen", "Токен успешно обновлен!")
                //client.saveToPrefs(sharedPrefs)

                // Переключаемся обратно на главный поток перед вызовом UI-операций
                withContext(Dispatchers.Main) {
                    snackbarHostState.showSnackbar(context.getString(R.string.token_refresh_success))
                }
            } catch (e: ApiException) {
                Log.e("AdvancedOptionsScreen", "Ошибка обновления токена: ${e.json}")

                withContext(Dispatchers.Main) {
                    snackbarHostState.showSnackbar(context.getString(R.string.token_refresh_error, e.json))
                }
            }
        }
    }


    AppScaffold(
        navController = navController,
        sharedPrefs = sharedPrefs,
        title = stringResource(R.string.advanced_options),
        client = client,
        snackbarHostState = snackbarHostState,
    ) {
        SelectionContainer {
            Column {
                OutlinedTextField(
                    value = client.accessToken ?: "Нет токена",
                    onValueChange = {},
                    label = { Text(stringResource(R.string.access_token)) },
                    readOnly = true,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = client.refreshToken ?: "Нет токена",
                    onValueChange = {},
                    label = { Text(stringResource(R.string.refresh_token)) },
                    readOnly = true,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = formattedExpiryDate,
                    onValueChange = {},
                    label = { Text(stringResource(R.string.token_expiry)) },
                    readOnly = true,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
