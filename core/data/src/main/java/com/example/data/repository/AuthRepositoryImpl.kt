package com.example.data.repository

import com.example.domain.model.User
import com.example.domain.repository.AuthRepository
import com.example.domain.util.AppResult
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
) : AuthRepository {

    private val _isAuthenticated = MutableStateFlow(firebaseAuth.currentUser != null)

    override val isAuthenticated: StateFlow<Boolean> = _isAuthenticated

    override val currentUser: User?
        get() = firebaseAuth.currentUser?.let { firebaseUser ->
            User(
                id = firebaseUser.uid,
                email = firebaseUser.email ?: "",
                displayName = firebaseUser.displayName
            )
        }

    private val authStateListener = FirebaseAuth.AuthStateListener { auth ->
        _isAuthenticated.value = auth.currentUser != null
    }

    init {
        firebaseAuth.addAuthStateListener(authStateListener)
    }

    override suspend fun login(email: String, pass: String): AppResult<Unit> {
        return try {
            firebaseAuth.signInWithEmailAndPassword(email, pass).await()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(e.message)
        }
    }

    override suspend fun register(email: String, pass: String): AppResult<Unit> {
        return try {
            firebaseAuth.createUserWithEmailAndPassword(email, pass).await()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(e.message)
        }
    }

    override fun logout() {
        firebaseAuth.signOut()
    }
}