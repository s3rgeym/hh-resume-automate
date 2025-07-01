package com.github.s3rgeym.hh_resume_automate.ui.components

import android.content.SharedPreferences
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.github.s3rgeym.hh_resume_automate.R
import com.github.s3rgeym.hh_resume_automate.api.ApiClient
import com.github.s3rgeym.hh_resume_automate.ui.home.HomeViewModel
import com.github.s3rgeym.hh_resume_automate.ui.home.HomeViewModelFactory

@Composable
fun HomeScreen(
  client: ApiClient,
  sharedPrefs: SharedPreferences,
  navController: NavController,
  snackbarHostState: SnackbarHostState,
) {
  val context = LocalContext.current
  val viewModel: HomeViewModel = viewModel(
    factory = HomeViewModelFactory(context, client, sharedPrefs, snackbarHostState)
  )

  val resumes by viewModel.resumes.collectAsState()
  val selectedResumeId by viewModel.selectedResumeId.collectAsState()
  val searchQuery by viewModel.searchQuery.collectAsState()
  val coverLetter by viewModel.coverLetter.collectAsState()
  val alwaysAttach by viewModel.alwaysAttachCoverLetter.collectAsState()

  val isVacancyApplyRunning by viewModel.isVacancyApplyRunning.collectAsState()
  val isResumeUpdateRunning by viewModel.isResumeUpdateRunning.collectAsState()

  AppScaffold(
    client = client,
    sharedPrefs = sharedPrefs,
    navController = navController,
    snackbarHostState = snackbarHostState,
    title = stringResource(R.string.home),
  ) {
    var dropdownExpanded by remember { mutableStateOf(false) }
    var showSearchQueryHelpDialog by remember { mutableStateOf(false) }
    var showCoverLetterHelpDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
      OutlinedButton(
        onClick = { dropdownExpanded = true },
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraSmall,
        // Или shape = RectangleShape // Для полностью квадратных углов
      ) {
        Text(
          resumes.find { it["id"] == selectedResumeId }?.get("title").toString()
            ?: "Выберите резюме",
          modifier = Modifier.fillMaxWidth()
        )
      }
      DropdownMenu(
        expanded = dropdownExpanded,
        onDismissRequest = { dropdownExpanded = false },
        modifier = Modifier.fillMaxWidth()
      ) {
        resumes.forEach { resume ->
          DropdownMenuItem(
            text = { Text(resume["title"].toString(), modifier = Modifier.fillMaxWidth()) },
            onClick = {
              viewModel.selectResume(resume["id"].toString())
              dropdownExpanded = false
            },
            modifier = Modifier.fillMaxWidth()
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
      value = searchQuery,
      onValueChange = viewModel::updateSearchQuery,
      label = { Text("Поиск вакансий") },
      placeholder = { Text("PHP-программист, Москва от 200 тысяч рублей") },
      enabled = selectedResumeId != null,
      singleLine = true,
      keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
      modifier = Modifier.fillMaxWidth(),
      trailingIcon = {
        IconButton(
          onClick = { showSearchQueryHelpDialog = true },
        ) {
          Icon(
            imageVector = Icons.Filled.Info,
            contentDescription = "Справка по поиску вакансий"
          )
        }
      }
    )
    if (showSearchQueryHelpDialog) {
      AlertDialog(
        onDismissRequest = { showSearchQueryHelpDialog = false },
//        icon = {
//          Icon(Icons.Filled.Lightbulb, contentDescription = "Справка")
//        },
        title = { Text("Поиск по вакансиям") },
        text = {
          Text("Это поле необязательное и служит лишь для более точного подбора подходящих вакансий. Если оно пустое, то будут рассмотрены все вакансии, соответствующие выбранному резюме.")
        },
        confirmButton = {
          TextButton(onClick = { showSearchQueryHelpDialog = false }) {
            Text("Понятно")
          }
        }
      )
    }

    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
      value = coverLetter,
      onValueChange = viewModel::updateCoverLetter,
      label = { Text("Сопроводительное письмо") },
      placeholder = { Text("Пример: {Здравствуйте|Добрый день|...}! Меня заинтересовала вакансия %vacancyName%...") },
      enabled = selectedResumeId != null,
      modifier = Modifier.fillMaxWidth(),
      trailingIcon = {
        IconButton(onClick = { showCoverLetterHelpDialog = true }) {
          Icon(Icons.Filled.Info, contentDescription = "Справка по сопроводительным письмам")
        }
      }
    )
    if (showCoverLetterHelpDialog) {
      AlertDialog(
        onDismissRequest = { showCoverLetterHelpDialog = false },
        title = { Text("Сопроводительные письма") },
        text = {
          Text(
            text = buildAnnotatedString {
              append("Используйте следующий синтаксис для динамического создания сопроводительных писем:\n\n")
              append("* ")
              withStyle(style = MaterialTheme.typography.bodySmall.toSpanStyle().copy(fontWeight = FontWeight.Bold)) {
                append("{вариант1 | вариант2 | ... }")
              }
              append(" — выбрать случайно один из вариантов.\n\n")
              append("* ")
              withStyle(style = MaterialTheme.typography.bodySmall.toSpanStyle().copy(fontWeight = FontWeight.Bold)) {
                append("%varName%")
              }
              append(" заменяет текст на значение переменной. Поддерживаемые переменные: ")
              withStyle(style = MaterialTheme.typography.bodySmall.toSpanStyle().copy(fontWeight = FontWeight.Bold)) {
                append("vacancyName")
              }
              append(", ")
              withStyle(style = MaterialTheme.typography.bodySmall.toSpanStyle().copy(fontWeight = FontWeight.Bold)) {
                append("firstName")
              }
              append(", ")
              withStyle(style = MaterialTheme.typography.bodySmall.toSpanStyle().copy(fontWeight = FontWeight.Bold)) {
                append("lastName")
              }
              append(".")
            },
            style = MaterialTheme.typography.bodyMedium
          )
        },
        confirmButton = {
          TextButton(onClick = { showCoverLetterHelpDialog = false }) {
            Text("Понятно")
          }
        }
      )
    }

    Spacer(modifier = Modifier.height(16.dp))

    Row(verticalAlignment = Alignment.CenterVertically) {
      Switch(
        checked = alwaysAttach,
        onCheckedChange = viewModel::toggleAlwaysAttachCoverLetter,
        enabled = selectedResumeId != null
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text("Всегда с сопроводительным")
    }

    Spacer(modifier = Modifier.height(24.dp))

    val vacancyApplyButtonText = if (isVacancyApplyRunning) "Остановить рассылку" else "Начать рассылку"
    Button(
      onClick = {
        if (isVacancyApplyRunning) {
          viewModel.stopVacancyApplyAutomation()
        } else {
          viewModel.startVacancyApplyAutomation()
        }
      },
      enabled = selectedResumeId != null,
      modifier = Modifier.fillMaxWidth(),
      shape = MaterialTheme.shapes.extraSmall
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        if (isVacancyApplyRunning) {
          Icon(Icons.Filled.Stop, contentDescription = "Остановить рассылку")
        } else {
          Icon(Icons.Filled.PlayArrow, contentDescription = "Начать рассылку")
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(vacancyApplyButtonText)
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    Row(verticalAlignment = Alignment.CenterVertically) {
      Checkbox(
        checked = isResumeUpdateRunning,
        onCheckedChange = { checked ->
          viewModel.toggleAutoUpdateResume(checked)
        },
        enabled = selectedResumeId != null
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text("Автоподнятие резюме")
    }
  }
}
