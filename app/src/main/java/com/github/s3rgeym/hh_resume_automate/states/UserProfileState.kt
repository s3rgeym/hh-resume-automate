package com.github.s3rgeym.hh_resume_automate.states

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.github.s3rgeym.hh_resume_automate.api.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserProfileState {
    var firstName by mutableStateOf("")
    var lastName by mutableStateOf("")

    suspend fun loadUserData(client: ApiClient) {
        try {
            val response = withContext(Dispatchers.IO) {
                client.api("GET", "/me")
            }
            firstName = response["first_name"] as? String ?: ""
            lastName = response["last_name"] as? String ?: ""
        } catch (e: Exception) {
            println("Error fetching user data: $e")
        }
    }
}