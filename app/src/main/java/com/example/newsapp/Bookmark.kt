package com.example.newsapp

/**
 * Represents a single bookmarked news article.
 *
 * @property url         The unique URL of the article (serves as the key).
 * @property title       The headline or title of the article.
 * @property description A short snippet or description of the article, if available.
 * @property image       URL to a thumbnail image for the article, if available.
 * @property sourceName  Name of the news outlet or source that published the article.
 * @property publishedAt The publication date/time in ISO 8601 format, if provided.
 */
data class Bookmark(
    val url: String,            // Unique key (article URL)
    val title: String,          // Headline or title text
    val description: String?,   // Optional summary or snippet
    val image: String?,         // Optional thumbnail URL
    val sourceName: String?,    // Optional name of the news source
    val publishedAt: String?    // Optional publish timestamp
)
