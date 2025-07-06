package com.github.s3rgeym.hh_resume_automate.worker

import android.content.Context
import android.util.Log
import androidx.work.WorkerParameters
import com.github.s3rgeym.hh_resume_automate.api.ApiClient
import com.github.s3rgeym.hh_resume_automate.api.ApiException
import com.github.s3rgeym.hh_resume_automate.api.LimitExceededException
import kotlinx.coroutines.delay
import kotlin.random.Random

class VacancyApplyWorker(
    context: Context,
    private val params: WorkerParameters
) : NotificationWorker(context, params) {

    private val TAG = "VacancyApplyWorker"

    private val resumeId = inputData.getString("resume_id")
    private val searchQuery = inputData.getString("query")
    private val coverLetter = inputData.getString("cover_letter")
    private val alwaysAttach = inputData.getBoolean("always_attach", false)

    private val sharedPrefs = context.getSharedPreferences("hh_resume_automate", Context.MODE_PRIVATE)
    private val client = ApiClient(sharedPrefs = sharedPrefs)

    private var firstName: String = ""
    private var lastName: String = ""

    override suspend fun doWork(): Result {
        if (resumeId.isNullOrEmpty()) {
            val errorMessage = "Ошибка: Не указан ID резюме."
            Log.e(TAG, errorMessage)
            showNotification("❌ $errorMessage")
            return Result.failure()
        }

        showNotification("📨 Рассылка откликов запущена...")

        try {
            val meResponse = client.api("GET", "/me")
            this.firstName = meResponse["first_name"] as? String ?: ""
            this.lastName = meResponse["last_name"] as? String ?: ""
            Log.d(TAG, "Получены данные пользователя: $firstName $lastName")
        } catch (e: Exception) {
            val errorMessage = "Ошибка при получении данных пользователя: ${e.message ?: "Неизвестная ошибка"}"
            Log.e(TAG, errorMessage, e)
            showNotification("❌ $errorMessage")
            return Result.retry()
        }

        return try {
            applySimilarVacancies()
            showNotification("✅ Все отклики отправлены.")
            Result.success()
        } catch (e: Exception) {
            val errorMessage = "Ошибка при рассылке: ${e.message ?: "Неизвестная ошибка"}"
            Log.e(TAG, errorMessage, e)
            showNotification("❌ $errorMessage")
            Result.retry()
        }
    }

    private suspend fun applySimilarVacancies() {
        for (page in 0 until 20) {
            if (page > 1) {
                val pageDelayMillis = Random.nextLong(1000, 3000)
                Log.d(TAG, "Delaying for $pageDelayMillis ms before fetching next page (no items on current).")
                delay(pageDelayMillis)
            }

            val requestParams = mutableMapOf<String, String>("page" to "$page", "per_page" to "100")
            searchQuery?.takeIf { it.isNotBlank() }?.let {
                requestParams["text"] = it.trim()
            }

            val response: Map<String, Any?>
            try {
                response = client.api("GET", "/resumes/$resumeId/similar_vacancies", requestParams)
            } catch (e: ApiException) {
                // Если произошла ошибка API при получении вакансий, нотификация уже будет показана
                // в doWork, если это общая ошибка. Если это специфическая API ошибка здесь,
                // она должна быть обработана как фатальная для текущей попытки рассылки.
                val errorMessage = "Ошибка API при получении вакансий на странице $page: ${e.message}"
                Log.e(TAG, errorMessage, e)
                showNotification("❌ $errorMessage") // Уведомляем пользователя
                throw e // Перебрасываем ошибку, чтобы doWork мог решить, повторять или нет
            }

            val items = response["items"] as? List<Map<String, Any>>
            if (items.isNullOrEmpty()) {
                Log.d(TAG, "No items found on page $page, or unexpected response format.")
                val pages = (response["pages"] as? Number)?.toInt() ?: 0
                if (page >= pages - 1) {
                    Log.d(TAG, "Reached last page or processed all available pages ($page of $pages) with no new items.)")
                    break
                }
                continue
            }

            for (vacancy in items) {
                if (vacancy["has_test"] == true || vacancy["archived"] == true) {
                    Log.d(TAG, "Skipping vacancy with ID ${vacancy["id"]} (has_test or archived).")
                    continue
                }
                if ((vacancy["relations"] as? List<*>)?.isNotEmpty() == true) {
                    Log.d(TAG, "Skipping vacancy with ID ${vacancy["id"]} (already applied).")
                    continue
                }

                val vacancyId = vacancy["id"]?.toString()
                if (vacancyId.isNullOrEmpty()) {
                    Log.w(TAG, "Vacancy ID is null or empty, skipping vacancy: $vacancy")
                    continue
                }
                val vacancyName = vacancy["name"]?.toString() ?: "Вакансия (ID: $vacancyId)"

                // --- Рандомный просмотр вакансии и аккаунта работодателя с задержками ---
                if (Random.nextInt(100) < 50) { // 50% шанс просмотра вакансии
                    try {
                        Log.d(TAG, "Просмотр вакансии '$vacancyName'.")
                        client.api("GET", "/vacancies/$vacancyId") // Просмотр вакансии
                        val viewDelayMillis = Random.nextLong(3000, 5000) // 3 до 5 секунд
                        Log.d(TAG, "Задержка на $viewDelayMillis мс после просмотра вакансии.")
                        delay(viewDelayMillis)

                        if (Random.nextInt(100) < 25) { // 25% шанс просмотра аккаунта работодателя после просмотра вакансии
                            val employerId = (vacancy["employer"] as? Map<String, Any>)?.get("id")?.toString()
                            if (!employerId.isNullOrEmpty()) {
                                Log.d(TAG, "Просмотр аккаунта работодателя #$employerId для вакансии '$vacancyName'.")
                                client.api("GET", "/employers/$employerId") // Просмотр аккаунта работодателя
                                val employerViewDelayMillis = Random.nextLong(3000, 5000) // 3 до 5 секунд
                                Log.d(TAG, "Задержка на $employerViewDelayMillis мс после просмотра аккаунта работодателя.")
                                delay(employerViewDelayMillis)
                            }
                        }
                    } catch (e: ApiException) {
                        val errorMessage = "Ошибка API при просмотре '$vacancyName': ${e.message}"
                        Log.w(TAG, errorMessage, e)
                        showNotification("❌ $errorMessage")
                        continue
                    }
                }


                val payload = mutableMapOf<String, Any>(
                    "resume_id" to resumeId!!,
                    "vacancy_id" to vacancyId
                )

                if (vacancy["response_letter_required"] == true || alwaysAttach) {
                    val templateVars = mutableMapOf(
                        "vacancyName" to vacancyName,
                        "firstName" to this.firstName,
                        "lastName" to this.lastName
                    )
                    val message = expandTemplate(coverLetter ?: "", templateVars)
                    payload["message"] = message
                    Log.d(TAG, "Attaching cover letter for '$vacancyName': '$message'")
                }

                val vacancyUrl = vacancy["alternate_url"]?.toString() ?: "n/a"

                try {
                    client.api("POST", "/negotiations", payload)
                    showNotification("✅ Отклик на $vacancyUrl ($vacancyName)")
                    Log.i(TAG, "Successfully applied to vacancy: $vacancyUrl")
                } catch (e: LimitExceededException) {
                    Log.w(TAG, "API Limit Exceeded. Stopping application process.", e)
                    showNotification("⚠️ Достигнут лимит откликов.")
                    return
                } catch (e: ApiException) { // <-- Изменения здесь
                    val errorMessage = "Ошибка API при отклике на $vacancyUrl ($vacancyName): ${e.message}"
                    Log.w(TAG, "${e.message}: ${e.json} (vacancy: $vacancy)")
                    showNotification("❌ $errorMessage") // <-- Показываем нотификацию об ошибке
                    // Продолжаем цикл, чтобы попытаться откликнуться на другие вакансии
                    continue
                }

                val delayMillis = Random.nextLong(3000, 5000)
                Log.d(TAG, "Delaying for $delayMillis ms before next application.")
                delay(delayMillis)
            }

            val pages = (response["pages"] as? Number)?.toInt() ?: 0
            if (page >= pages - 1) {
                Log.d(TAG, "Reached last page or processed all available pages ($page of $pages).")
                break
            }
        }
    }

    private fun expandTemplate(input: String, vars: Map<String, String>): String {
        val pattern = Regex("\\{([^{}]+)\\}")
        var result = input

        while (true) {
            val match = pattern.find(result) ?: break
            val options = match.groupValues[1].split("|").map { it.trim() }
            result = result.replaceRange(match.range, options.randomOrNull().orEmpty())
        }

        for ((k, v) in vars) {
            result = result.replace("%$k%", v)
        }

        return result
    }
}
