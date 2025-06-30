package com.github.s3rgeym.hh_resume_automate.ui.home

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.material3.SnackbarHostState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.github.s3rgeym.hh_resume_automate.api.ApiClient
import com.github.s3rgeym.hh_resume_automate.api.ApiException
import com.github.s3rgeym.hh_resume_automate.worker.ResumeUpdateWorker
import com.github.s3rgeym.hh_resume_automate.worker.VacancyApplyWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class HomeViewModel(
  private val context: Context,
  private val client: ApiClient,
  private val sharedPrefs: SharedPreferences,
  private val snackbarHostState: SnackbarHostState,
) : ViewModel() {

  private val VACANCY_APPLY_PERIODIC_WORK_NAME = "vacancy_apply_periodic_work"
  private val VACANCY_APPLY_ONE_TIME_WORK_NAME = "vacancy_apply_one_time_work"
  private val RESUME_UPDATE_PERIODIC_WORK_NAME = "resume_update_periodic_work"
  private val RESUME_UPDATE_ONE_TIME_WORK_NAME = "resume_update_one_time_work"

  private val DEFAULT_COVER_LETTER = "{Здравствуйте|Добрый день|Приветствую}! {Меня заинтересовала|Понравилась} ваша {вакансия|позиция} %vacancyName%. {Мои компетенции соответствуют требованиям|Уверен в своих силах, основываясь на опыте|Я обладаю всем необходимым}, чтобы {эффективно|успешно} {выполнять|исполнять} {поставленные задачи|ваши требования}. {С радостью обсужу детали|С нетерпением жду возможности пообщаться|Готов ответить на вопросы} {на собеседовании|в удобное для вас время}. {С уважением|С наулучшими пожеланиями|Благодарю за внимание}, %firstName%."
  private val SHARED_PREFS_SELECTED_RESUME_ID = "selected_resume_id"
  private val SHARED_PREFS_SEARCH_QUERY = "search_query"
  private val SHARED_PREFS_ALWAYS_ATTACH_COVER_LETTER = "always_attach_cover_letter"
  private val SHARED_PREFS_COVER_LETTER = "cover_letter"

  private val _resumes = MutableStateFlow<List<Map<String, Any>>>(emptyList())
  val resumes: StateFlow<List<Map<String, Any>>> = _resumes.asStateFlow()

  private val _selectedResumeId = MutableStateFlow<String?>(null)
  val selectedResumeId: StateFlow<String?> = _selectedResumeId.asStateFlow()

  private val _searchQuery = MutableStateFlow("")
  val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

  private val _alwaysAttachCoverLetter = MutableStateFlow(false)
  val alwaysAttachCoverLetter: StateFlow<Boolean> = _alwaysAttachCoverLetter.asStateFlow()

  private val _coverLetter = MutableStateFlow(DEFAULT_COVER_LETTER)
  val coverLetter: StateFlow<String> = _coverLetter.asStateFlow()

  private val _isVacancyApplyRunning = MutableStateFlow(false)
  val isVacancyApplyRunning: StateFlow<Boolean> = _isVacancyApplyRunning.asStateFlow()

  private val _isResumeUpdateRunning = MutableStateFlow(false)
  val isResumeUpdateRunning: StateFlow<Boolean> = _isResumeUpdateRunning.asStateFlow()

  init {
    viewModelScope.launch {
      withContext(Dispatchers.IO) {
        _selectedResumeId.value = sharedPrefs.getString(SHARED_PREFS_SELECTED_RESUME_ID, null)
        _alwaysAttachCoverLetter.value = sharedPrefs.getBoolean(SHARED_PREFS_ALWAYS_ATTACH_COVER_LETTER, false)
        _coverLetter.value = sharedPrefs.getString(SHARED_PREFS_COVER_LETTER, DEFAULT_COVER_LETTER) ?: DEFAULT_COVER_LETTER
        _searchQuery.value = sharedPrefs.getString(SHARED_PREFS_SEARCH_QUERY, "") ?: ""
      }
      loadResumes()
      observeWorkerStates()
    }
  }

  private fun observeWorkerStates() {
    WorkManager.getInstance(context)
      .getWorkInfosForUniqueWorkLiveData(VACANCY_APPLY_PERIODIC_WORK_NAME)
      .asFlow()
      .onEach { workInfos ->
        _isVacancyApplyRunning.value = workInfos.any {
          it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING
        }
      }
      .launchIn(viewModelScope)

    WorkManager.getInstance(context)
      .getWorkInfosForUniqueWorkLiveData(RESUME_UPDATE_PERIODIC_WORK_NAME)
      .asFlow()
      .onEach { workInfos ->
        _isResumeUpdateRunning.value = workInfos.any {
          it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING
        }
      }
      .launchIn(viewModelScope)
  }

  private suspend fun loadResumes() {
    try {
      val data = withContext(Dispatchers.IO) {
        client.api("GET", "/resumes/mine")
      }
      val items = data["items"] as? List<Map<String, Any>> ?: emptyList()
      _resumes.value = items
    } catch (e: ApiException) {
      viewModelScope.launch {
        snackbarHostState.showSnackbar("❌ Ошибка при загрузке резюме: ${e.message ?: "неизвестная"}")
      }
    }
  }

  fun selectResume(id: String) {
    val currentResumeId = _selectedResumeId.value
    if (id != currentResumeId) {
      val wasVacancyApplyRunning = _isVacancyApplyRunning.value
      val wasResumeUpdateRunning = _isResumeUpdateRunning.value

      stopVacancyApplyAutomation()
      stopResumeUpdateAutomation()

      _selectedResumeId.value = id
      viewModelScope.launch(Dispatchers.IO) {
        sharedPrefs.edit().putString(SHARED_PREFS_SELECTED_RESUME_ID, id).apply()
      }

      if (wasVacancyApplyRunning) {
        startVacancyApplyAutomation()
      }
      if (wasResumeUpdateRunning) {
        startResumeUpdateAutomation()
      }

      viewModelScope.launch {
        snackbarHostState.showSnackbar("✅ Резюме изменено. Автоматизации перезапущены при необходимости.")
      }
    }
  }

  fun updateSearchQuery(query: String) {
    _searchQuery.value = query
    viewModelScope.launch(Dispatchers.IO) {
      sharedPrefs.edit().putString(SHARED_PREFS_SEARCH_QUERY, query).apply()
    }
  }

  fun toggleAlwaysAttachCoverLetter(enabled: Boolean) {
    _alwaysAttachCoverLetter.value = enabled
    viewModelScope.launch(Dispatchers.IO) {
      sharedPrefs.edit().putBoolean(SHARED_PREFS_ALWAYS_ATTACH_COVER_LETTER, enabled).apply()
    }
  }

  fun updateCoverLetter(message: String) {
    _coverLetter.value = message
    viewModelScope.launch(Dispatchers.IO) {
      sharedPrefs.edit().putString(SHARED_PREFS_COVER_LETTER, message).apply()
    }
  }

  fun toggleAutoUpdateResume(enabled: Boolean) {
    if (enabled) {
      startResumeUpdateAutomation()
    } else {
      stopResumeUpdateAutomation()
    }
  }

  fun startVacancyApplyAutomation() {
    val resumeId = selectedResumeId.value
    if (resumeId.isNullOrEmpty()) {
      viewModelScope.launch {
        snackbarHostState.showSnackbar("⚠️ Выберите резюме для запуска рассылки откликов.")
      }
      return
    }

    enqueueVacancyApplyOneTimeWork(
      resumeId,
      searchQuery.value,
      coverLetter.value,
      alwaysAttachCoverLetter.value
    )
    enqueueVacancyApplyPeriodicWork(
      resumeId,
      searchQuery.value,
      coverLetter.value,
      alwaysAttachCoverLetter.value
    )
    viewModelScope.launch {
      snackbarHostState.showSnackbar("✅ Рассылка откликов запущена (каждые 24 часа).")
    }
  }

  fun stopVacancyApplyAutomation() {
    WorkManager.getInstance(context).cancelUniqueWork(VACANCY_APPLY_PERIODIC_WORK_NAME)
    WorkManager.getInstance(context).cancelUniqueWork(VACANCY_APPLY_ONE_TIME_WORK_NAME)
    viewModelScope.launch {
      snackbarHostState.showSnackbar("❌ Рассылка откликов остановлена.")
    }
  }

  fun startResumeUpdateAutomation() {
    val resumeId = selectedResumeId.value
    if (resumeId.isNullOrEmpty()) {
      viewModelScope.launch {
        snackbarHostState.showSnackbar("⚠️ Выберите резюме для запуска автоподнятия.")
      }
      stopResumeUpdateAutomation()
      return
    }

    enqueueResumeUpdateOneTimeWork(resumeId)
    enqueueResumeUpdatePeriodicWork(resumeId)
    viewModelScope.launch {
      snackbarHostState.showSnackbar("✅ Обновление резюме активно (каждые 4 часа).")
    }
  }

  fun stopResumeUpdateAutomation() {
    WorkManager.getInstance(context).cancelUniqueWork(RESUME_UPDATE_PERIODIC_WORK_NAME)
    WorkManager.getInstance(context).cancelUniqueWork(RESUME_UPDATE_ONE_TIME_WORK_NAME)
    viewModelScope.launch {
      snackbarHostState.showSnackbar("❌ Обновление резюме остановлено.")
    }
  }

  private fun enqueueVacancyApplyOneTimeWork(
    resumeId: String,
    query: String,
    coverLetter: String,
    alwaysAttach: Boolean
  ) {
    val inputData = workDataOf(
      "resume_id" to resumeId,
      "query" to query,
      "cover_letter" to coverLetter,
      "always_attach" to alwaysAttach
    )

    val constraints = Constraints.Builder()
      .setRequiredNetworkType(NetworkType.CONNECTED)
      .build()

    val oneTimeRequest = OneTimeWorkRequestBuilder<VacancyApplyWorker>()
      .setInputData(inputData)
      .setConstraints(constraints)
      .build()

    WorkManager.getInstance(context).enqueueUniqueWork(
      VACANCY_APPLY_ONE_TIME_WORK_NAME,
      ExistingWorkPolicy.REPLACE,
      oneTimeRequest
    )
  }

  private fun enqueueVacancyApplyPeriodicWork(
    resumeId: String,
    query: String,
    coverLetter: String,
    alwaysAttach: Boolean
  ) {
    val inputData = workDataOf(
      "resume_id" to resumeId,
      "query" to query,
      "cover_letter" to coverLetter,
      "always_attach" to alwaysAttach
    )

    val constraints = Constraints.Builder()
      .setRequiredNetworkType(NetworkType.CONNECTED)
      .build()

    val vacancyApplyRequest = PeriodicWorkRequestBuilder<VacancyApplyWorker>(
      24, TimeUnit.HOURS,
      15, TimeUnit.MINUTES
    )
      .setInputData(inputData)
      .setConstraints(constraints)
      .build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
      VACANCY_APPLY_PERIODIC_WORK_NAME,
      ExistingPeriodicWorkPolicy.UPDATE,
      vacancyApplyRequest
    )
  }

  private fun enqueueResumeUpdateOneTimeWork(resumeId: String) {
    val inputData = workDataOf(
      "resume_id" to resumeId
    )

    val constraints = Constraints.Builder()
      .setRequiredNetworkType(NetworkType.CONNECTED)
      .build()

    val oneTimeRequest = OneTimeWorkRequestBuilder<ResumeUpdateWorker>()
      .setInputData(inputData)
      .setConstraints(constraints)
      .build()

    WorkManager.getInstance(context).enqueueUniqueWork(
      RESUME_UPDATE_ONE_TIME_WORK_NAME,
      ExistingWorkPolicy.REPLACE,
      oneTimeRequest
    )
  }

  private fun enqueueResumeUpdatePeriodicWork(resumeId: String) {
    val inputData = workDataOf(
      "resume_id" to resumeId
    )

    val constraints = Constraints.Builder()
      .setRequiredNetworkType(NetworkType.CONNECTED)
      .build()

    val resumeUpdateRequest = PeriodicWorkRequestBuilder<ResumeUpdateWorker>(
      4, TimeUnit.HOURS,
      15, TimeUnit.MINUTES
    )
      .setInputData(inputData)
      .setConstraints(constraints)
      .build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
      RESUME_UPDATE_PERIODIC_WORK_NAME,
      ExistingPeriodicWorkPolicy.UPDATE,
      resumeUpdateRequest
    )
  }

  override fun onCleared() {
    super.onCleared()
    //stopVacancyApplyAutomation()
    //stopResumeUpdateAutomation()
  }
}
