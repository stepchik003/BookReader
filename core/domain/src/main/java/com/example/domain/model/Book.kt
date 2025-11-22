package com.example.domain.model

data class Book(
    val id: String? = null,
    val title: String,
    val author: String,
    val fileUrl: String,
    val userId: String,
    val localPath: String? = null
)