package com.example.data.repository

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import com.example.domain.model.User
import com.example.domain.repository.AuthRepository
import com.example.domain.util.AppResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    @ApplicationContext private val context: Context
) : AuthRepository {

    private val _isAuthenticated = MutableStateFlow(firebaseAuth.currentUser != null)

    override val isAuthenticated: StateFlow<Boolean> = _isAuthenticated

    override val currentUser: User?
        get() = firebaseAuth.currentUser?.let { firebaseUser ->
            User(
                id = firebaseUser.uid,
                email = firebaseUser.email ?: "",
                displayName = firebaseUser.displayName,
                photoUrl = firebaseUser.photoUrl.toString()
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

    override suspend fun register(email: String, pass: String, name: String): AppResult<Unit> {
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, pass).await()

            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build()
            result.user?.updateProfile(profileUpdates)?.await()

            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(e.message)
        }
    }


    override suspend fun uploadPhoto(uri: String): AppResult<Unit> {
        return try {
            val imageUri = uri.toUri()
            val localFilePath = saveImageToLocalStorage(imageUri)
            val request = UserProfileChangeRequest.Builder()
                .setPhotoUri(Uri.parse(localFilePath))
                .build()
            firebaseAuth.currentUser?.updateProfile(request)?.await()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(e.message)
        }
    }

    private suspend fun saveImageToLocalStorage(uri: Uri): String {
        return withContext(Dispatchers.IO) {
            val fileName = "user_avatar_${System.currentTimeMillis()}.jpg"
            val outputFile = File(context.filesDir, "avatars/$fileName")

            outputFile.parentFile?.mkdirs()

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                outputFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            outputFile.absolutePath
        }
    }

    override fun logout() {
        firebaseAuth.signOut()
    }
}