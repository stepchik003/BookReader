package com.example.data.firestore

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val BOOKS_COLLECTION = "books"
    }

    suspend fun saveBookMetadata(metadata: BookMetadata): String {
        return try {
            val documentRef = firestore.collection(BOOKS_COLLECTION)
                .add(metadata)
                .await()
            documentRef.id
        } catch (e: Exception) {
            throw IOException(e)
        }
    }

    suspend fun getAllUserBooks(userId: String): Map<String, BookMetadata> {
        return try {
            val snapshot = firestore.collection(BOOKS_COLLECTION)
                .whereEqualTo("userId", userId)
                .get()
                .await()

            snapshot.documents.mapNotNull { document ->
                val metadata = document.toObject(BookMetadata::class.java)
                if (metadata != null) {
                    document.id to metadata
                } else {
                    null
                }
            }.toMap()

        } catch (e: Exception) {
            throw IOException(e)
        }
    }
}