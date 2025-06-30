package com.github.s3rgeym.hh_resume_automate.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face // Используется для "Техническая поддержка"
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.github.s3rgeym.hh_resume_automate.R // Убедитесь, что R здесь доступен
import com.github.s3rgeym.hh_resume_automate.api.ApiClient
import com.github.s3rgeym.hh_resume_automate.states.UserProfileState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    client: ApiClient,
    sharedPrefs: android.content.SharedPreferences,
    navController: NavController,
    snackbarHostState: SnackbarHostState,
    title: String,
    content: @Composable (innerPadding: PaddingValues) -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val userProfileState = remember { UserProfileState() }
    val context = LocalContext.current

    val githubUrl = "https://github.com/s3rgeym"
    val telegramSupportUrl = "https://t.me/hh_resume_automate"

    // Функция для выхода из аккаунта
    val logout: () -> Unit = {
        with(sharedPrefs.edit()) {
            clear() // Удаляем все данные аутентификации
            apply() // Применяем изменения
        }
        // Переходим на экран авторизации и очищаем Back Stack
        navController.navigate("auth") {
            popUpTo(navController.graph.startDestinationId) { inclusive = true }
        }
    }

    // Загружаем данные пользователя при первом отображении компонента
    LaunchedEffect(Unit) {
        userProfileState.loadUserData(client)
    }

    ModalNavigationDrawer(
        drawerContent = {
            ModalDrawerSheet {
                // Column с возможностью прокрутки для содержимого Drawer
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState()) // Здесь применяется скролл
                ) {
                    Text(
                        // Используем stringResource для форматированной строки приветствия
                        text = stringResource(
                            R.string.drawer_greeting,
                            userProfileState.firstName,
                            userProfileState.lastName
                        ),
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.titleMedium
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    NavigationDrawerItem(
                        label = { Text(stringResource(R.string.home)) },
                        icon = { Icon(Icons.Filled.Home, contentDescription = stringResource(R.string.home_content_description)) },
                        selected = navController.currentDestination?.route == "home",
                        onClick = {
                            navController.navigate("home") {
                                popUpTo("home") { inclusive = true } // Очищаем Back Stack до home
                                launchSingleTop = true // Избегаем дублирования home
                            }
                            scope.launch { drawerState.close() }
                        }
                    )
                    NavigationDrawerItem(
                        label = { Text(stringResource(R.string.advanced_options)) },
                        icon = { Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.advanced_options_content_description)) },
                        selected = navController.currentDestination?.route == "advancedOptions",
                        onClick = {
                            navController.navigate("advancedOptions") {
                                launchSingleTop = true // Избегаем дублирования advancedOptions
                            }
                            scope.launch { drawerState.close() }
                        }
                    )

                    NavigationDrawerItem(
                        label = { Text(stringResource(R.string.technical_support)) },
                        icon = { Icon(Icons.Filled.Face, contentDescription = stringResource(R.string.technical_support_content_description)) },
                        selected = false,
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(telegramSupportUrl))
                            context.startActivity(intent)
                            scope.launch { drawerState.close() }
                        }
                    )

                    NavigationDrawerItem(
                        label = { Text(stringResource(R.string.about_author)) },
                        icon = { Icon(Icons.Filled.Info, contentDescription = stringResource(R.string.about_author_content_description)) },
                        selected = false,
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(githubUrl))
                            context.startActivity(intent)
                            scope.launch { drawerState.close() }
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    NavigationDrawerItem(
                        label = { Text(stringResource(R.string.logout)) },
                        icon = { Icon(Icons.Filled.Logout, contentDescription = stringResource(R.string.logout_content_description)) },
                        selected = false,
                        onClick = {
                            logout()
                            scope.launch { drawerState.close() }
                        }
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }
        },
        drawerState = drawerState,
        gesturesEnabled = true, // Включено открытие Drawer свайпом
        modifier = Modifier.fillMaxSize()
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(text = title, color = MaterialTheme.colorScheme.onPrimary) },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch {
                                // Переключаем состояние Drawer (открыть/закрыть)
                                if (drawerState.isClosed) {
                                    drawerState.open()
                                } else {
                                    drawerState.close()
                                }
                            }
                        }) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = stringResource(R.string.menu_content_description),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            },
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { innerPadding ->
            // Важно: Применяем padding от Scaffold к содержимому
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp, 18.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                content(innerPadding) // Здесь будет отображаться содержимое текущего экрана
            }
        }
    }
}