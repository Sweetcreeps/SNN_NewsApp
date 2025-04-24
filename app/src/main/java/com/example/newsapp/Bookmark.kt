package com.example.newsapp

data class Bookmark(
    val url: String,            // Unique key
    val title: String,
    val description: String?,
    val image: String?,
    val sourceName: String?,
    val publishedAt: String?
)
