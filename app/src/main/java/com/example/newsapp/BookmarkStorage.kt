package com.example.newsapp

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Simple key-value storage for bookmarked articles using SharedPreferences.
 *
 * Stores a JSON list of [Bookmark] objects under a single preferences file.
 */
object BookmarkStorage {
    // Name of the SharedPreferences file
    private const val PREFS_NAME = "bookmark_prefs"

    // Key under which the JSON-encoded list of bookmarks is stored
    private const val KEY_BOOKMARKS = "bookmarks"

    /**
     * Retrieve the SharedPreferences instance for this app.
     */
    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Load the list of saved bookmarks.
     *
     * If no bookmarks have been saved yet, returns an empty list.
     */
    fun getBookmarks(context: Context): List<Bookmark> {
        val prefs = getPreferences(context)
        val json = prefs.getString(KEY_BOOKMARKS, null)
        return if (!json.isNullOrEmpty()) {
            // Deserialize the JSON string back into a List<Bookmark>
            val type = object : TypeToken<List<Bookmark>>() {}.type
            Gson().fromJson(json, type)
        } else {
            emptyList()
        }
    }

    /**
     * Persist the given list of bookmarks to SharedPreferences.
     *
     * @param bookmarks Full list of bookmarks to save.
     */
    fun saveBookmarks(context: Context, bookmarks: List<Bookmark>) {
        val prefs = getPreferences(context)
        prefs.edit().apply {
            // Convert bookmarks list to JSON and store under KEY_BOOKMARKS
            putString(KEY_BOOKMARKS, Gson().toJson(bookmarks))
            apply()
        }
    }

    /**
     * Add a new bookmark if it hasn't already been saved.
     *
     * @param bookmark The bookmark to add.
     */
    fun addBookmark(context: Context, bookmark: Bookmark) {
        val current = getBookmarks(context).toMutableList()
        // Avoid duplicates by URL
        if (current.none { it.url == bookmark.url }) {
            current.add(bookmark)
            saveBookmarks(context, current)
        }
    }

    /**
     * Remove an existing bookmark from storage.
     *
     * @param bookmark The bookmark to remove.
     */
    fun removeBookmark(context: Context, bookmark: Bookmark) {
        val current = getBookmarks(context).toMutableList()
        // Keep only those whose URL does not match
        val filtered = current.filter { it.url != bookmark.url }
        saveBookmarks(context, filtered)
    }

    /**
     * Check if a given article is already bookmarked.
     *
     * @return `true` if a bookmark with the same URL exists.
     */
    fun isBookmarked(context: Context, bookmark: Bookmark): Boolean {
        return getBookmarks(context).any { it.url == bookmark.url }
    }
}
