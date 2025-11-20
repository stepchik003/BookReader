package com.example.domain.repository

import com.example.domain.model.User
import com.example.domain.util.AppResult
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {

    val currentUser: User?
    val isAuthenticated: StateFlow<Boolean>

    suspend fun login(email: String, pass: String): AppResult<Unit>

    suspend fun register(email: String, pass: String): AppResult<Unit>

    fun logout()
}