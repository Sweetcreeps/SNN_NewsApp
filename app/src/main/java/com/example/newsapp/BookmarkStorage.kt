package com.example.newsapp


import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object BookmarkStorage {
    private const val PREFS_NAME = "bookmark_prefs"
    private const val KEY_BOOKMARKS = "bookmarks"

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getBookmarks(context: Context): List<Bookmark> {
        val prefs = getPreferences(context)
        val json = prefs.getString(KEY_BOOKMARKS, null)
        return if (!json.isNullOrEmpty()) {
            val type = object : TypeToken<List<Bookmark>>() {}.type
            Gson().fromJson(json, type)
        } else {
            emptyList()
        }
    }

    fun saveBookmarks(context: Context, bookmarks: List<Bookmark>) {
        val prefs = getPreferences(context)
        prefs.edit().apply {
            putString(KEY_BOOKMARKS, Gson().toJson(bookmarks))
            apply()
        }
    }

    fun addBookmark(context: Context, bookmark: Bookmark) {
        val bookmarks = getBookmarks(context).toMutableList()
        if (bookmarks.none { it.url == bookmark.url }) {
            bookmarks.add(bookmark)
            saveBookmarks(context, bookmarks)
        }
    }

    fun removeBookmark(context: Context, bookmark: Bookmark) {
        val bookmarks = getBookmarks(context).toMutableList()
        val filtered = bookmarks.filter { it.url != bookmark.url }
        saveBookmarks(context, filtered)
    }

    fun isBookmarked(context: Context, bookmark: Bookmark): Boolean {
        return getBookmarks(context).any { it.url == bookmark.url }
    }
}
