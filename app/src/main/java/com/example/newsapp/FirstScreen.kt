package com.example.newsapp

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.transform.RoundedCornersTransformation
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// ----------------------
// API definitions
// ----------------------

/**
 * Holds the API key constant for GNews.
 */
object ApiConstants {
    const val NEWS_API_KEY = "730a4ea2870ede4f405899aec4dba2f1"
}

/**
 * Represents the source of a news article (e.g. "BBC News").
 */
data class Source(
    val name: String,
    val url: String?
)

/**
 * Wraps the response from the GNews API.
 */
data class GNewsResponse(
    val totalArticles: Int,
    val articles: List<Article>
)

/**
 * Model for a single news article.
 *
 * @property title The headline text.
 * @property description A brief summary or excerpt (if available).
 * @property url Link to the full story.
 * @property image URL of the article’s thumbnail.
 * @property source Information about the publishing outlet.
 * @property publishedAt Publication timestamp in ISO-8601 format.
 */
data class Article(
    val title: String,
    val description: String?,
    val url: String,
    val image: String?,
    val source: Source?,
    val publishedAt: String?
)

/**
 * Defines the Retrofit interface for fetching top headlines and searching.
 */
interface GNewsApiService {
    @GET("top-headlines")
    suspend fun getTopHeadlines(
        @Query("topic") topic: String,
        @Query("lang") lang: String = "en",      // language filter
        @Query("max") max: Int = 10,             // max items per page
        @Query("page") page: Int,                // page number for pagination
        @Query("token") token: String = ApiConstants.NEWS_API_KEY
    ): GNewsResponse

    @GET("search")
    suspend fun searchNews(
        @Query("q") query: String,
        @Query("lang") lang: String = "en",
        @Query("max") max: Int = 10,
        @Query("page") page: Int,
        @Query("token") token: String = ApiConstants.NEWS_API_KEY
    ): GNewsResponse
}

/**
 * Singleton to build and provide the GNews Retrofit service.
 */
object GNewsApi {
    private const val BASE_URL = "https://gnews.io/api/v4/"
    val retrofitService: GNewsApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GNewsApiService::class.java)
    }
}

// ----------------------
// UI Code
// ----------------------

/**
 * The main composable that switches between:
 * - Bookmark view
 * - Search results
 * - Category-based news feed
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FirstScreen() {
    // State flags for toggling modes
    var isSearchMode by remember { mutableStateOf(false) }
    var showBookmarks by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // Available categories for the feed pager
    val categories = listOf("General", "Business", "Sports", "Technology")
    val pagerState = rememberPagerState(pageCount = { categories.size })
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        // App header with search & bookmark toggles
        Header(
            isSearchMode = isSearchMode,
            searchQuery = searchQuery,
            onSearchTextChange = { searchQuery = it },
            onSearchToggle    = { isSearchMode = !isSearchMode },
            onBookmarkClick   = { showBookmarks = !showBookmarks }
        )

        // Decide which section to display:
        when {
            showBookmarks -> {
                // Bookmark list view
                BookmarkScreenContent(onClose = { showBookmarks = false })
            }
            isSearchMode && searchQuery.isNotBlank() -> {
                // Search results if there’s a non-empty query
                SearchNewsSection(searchQuery = searchQuery)
            }
            else -> {
                // Standard news feed with category tabs
                Spacer(modifier = Modifier.height(8.dp))

                // Category selector row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment   = Alignment.CenterVertically
                ) {
                    categories.forEachIndexed { index, category ->
                        Button(
                            onClick = { coroutineScope.launch { pagerState.animateScrollToPage(index) } },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (pagerState.currentPage == index)
                                    Color(0xFF1E88E5) else Color.LightGray
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = category,
                                color = Color.White,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Pager to swipe between feeds
                HorizontalPager(state = pagerState) { page ->
                    when (categories[page]) {
                        "General"    -> GeneralPage()
                        "Business"   -> BusinessPage()
                        "Sports"     -> SportsPage()
                        "Technology" -> TechnologyPage()
                        else         -> PlaceholderPage(categories[page])
                    }
                }
            }
        }
    }
}

/**
 * App header:
 * - Shows title/subtitle or search field depending on mode.
 * - Contains buttons for toggling search/bookmarks.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Header(
    isSearchMode: Boolean,
    searchQuery: String,
    onSearchTextChange: (String) -> Unit,
    onSearchToggle: () -> Unit,
    onBookmarkClick: () -> Unit
) {
    // Gradient background for a modern look
    val gradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFF1E88E5), Color(0xFF42A5F5))
    )

    TopAppBar(
        modifier = Modifier
            .fillMaxWidth()
            .background(gradient),
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
        title = {
            if (!isSearchMode) {
                // Normal title + subtitle
                Column {
                    Text(
                        text = "Swift News Network",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = Color.White
                    )
                    Text(
                        text = "Your trusted source",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            } else {
                // Inline search input
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchTextChange,
                    singleLine = true,
                    label = { Text("Search news...", color = Color.White) },
                    textStyle = LocalTextStyle.current.copy(color = Color.White),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        cursorColor         = Color.White,
                        focusedBorderColor  = Color.White,
                        unfocusedBorderColor= Color.White,
                        focusedLabelColor   = Color.White,
                        unfocusedLabelColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        navigationIcon = {
            if (!isSearchMode) {
                // App logo box
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.Gray, shape = RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "SNN",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        color = Color.White
                    )
                }
            }
        },
        actions = {
            // Search toggle button
            IconButton(onClick = onSearchToggle) {
                Icon(
                    imageVector   = Icons.Default.Search,
                    contentDescription = "Toggle Search",
                    tint = Color.White
                )
            }
            // Bookmark toggle button
            IconButton(onClick = onBookmarkClick) {
                Icon(
                    imageVector   = Icons.Default.BookmarkBorder,
                    contentDescription = "Toggle Bookmarks",
                    tint = Color.White
                )
            }
        }
    )
}

/**
 * Shows search results by calling the GNews ".search" endpoint.
 */
@Composable
fun SearchNewsSection(searchQuery: String) {
    var articles by remember { mutableStateOf<List<Article>>(emptyList()) }
    var page     by remember { mutableStateOf(1) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // Fetch new pages whenever query or page changes
    LaunchedEffect(searchQuery, page) {
        if (searchQuery.isNotBlank()) {
            isLoading = true
            try {
                val resp = GNewsApi.retrofitService.searchNews(
                    query = searchQuery,
                    page  = page,
                    max   = 10
                )
                articles = articles + resp.articles
            } catch (e: Exception) {
                errorMessage = "Error fetching search results: ${e.message}"
            } finally {
                isLoading = false
            }
        } else {
            // Clear list if query is emptied
            articles = emptyList()
        }
    }

    // Show any error at top
    errorMessage?.let { msg ->
        Text(msg, color = Color.Red, modifier = Modifier.padding(8.dp))
    }

    LazyColumn {
        items(articles) { article ->
            ArticleRow(article = article)
            Divider(
                color = Color.LightGray,
                thickness = 1.dp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
        if (isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
        // Load next page on scroll end
        if (!isLoading && articles.isNotEmpty()) {
            item {
                Spacer(Modifier.height(16.dp))
                LaunchedEffect(Unit) { page++ }
            }
        }
    }
}

// Below: Composables for each category’s feed header + list.

/** Displays "General News" header and feed. */
@Composable
fun GeneralPage() {
    Column {
        Text(
            "General News",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(8.dp)
        )
        NewsListPage("General")
    }
}

/** Displays "Business News" header and feed. */
@Composable
fun BusinessPage() {
    Column {
        Text(
            "Business News",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(8.dp)
        )
        NewsListPage("Business")
    }
}

/** Displays "Sports News" header and feed. */
@Composable
fun SportsPage() {
    Column {
        Text(
            "Sports News",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(8.dp)
        )
        NewsListPage("Sports")
    }
}

/** Displays "Technology News" header and feed. */
@Composable
fun TechnologyPage() {
    Column {
        Text(
            "Technology News",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(8.dp)
        )
        NewsListPage("Technology")
    }
}

/**
 * Generic feed loader for a given category.
 * Maps UI category to GNews “topic” parameter.
 */
@Composable
fun NewsListPage(apiCategory: String) {
    var articles by remember { mutableStateOf<List<Article>>(emptyList()) }
    var page       by remember { mutableStateOf(1) }
    var isLoading  by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Determine the GNews topic string
    val topic = when (apiCategory) {
        "General"    -> "breaking-news"
        "Business"   -> "business"
        "Sports"     -> "sports"
        "Technology" -> "technology"
        else         -> "breaking-news"
    }

    LaunchedEffect(page, apiCategory) {
        isLoading = true
        try {
            val resp = GNewsApi.retrofitService.getTopHeadlines(
                topic = topic, page = page, max = 10
            )
            articles = articles + resp.articles
        } catch (e: Exception) {
            errorMessage = "Error fetching news: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    errorMessage?.let { msg ->
        Text(msg, color = Color.Red, modifier = Modifier.padding(8.dp))
    }

    LazyColumn {
        items(articles) { article ->
            ArticleRow(article)
            Divider(
                color = Color.LightGray,
                thickness = 1.dp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
        if (isLoading) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
        if (!isLoading && articles.isNotEmpty()) {
            item {
                Spacer(Modifier.height(16.dp))
                LaunchedEffect(Unit) { page++ }
            }
        }
    }
}

/**
 * Renders a single article row with:
 * - Thumbnail image
 * - Headline + description
 * - Source name & date
 * - Bookmark toggle (uses SharedPreferences)
 * Tapping the row (excluding the bookmark) opens the article URL.
 */
@Composable
fun ArticleRow(article: Article) {
    val context = LocalContext.current
    var isBookmarked by remember { mutableStateOf(false) }

    // Check bookmark status when article changes
    LaunchedEffect(article.url) {
        isBookmarked = BookmarkStorage.isBookmarked(context, article.toBookmark())
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable {
                // Open in browser
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(article.url)))
            }
    ) {
        // Thumbnail
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(article.image)
                .crossfade(true)
                .transformations(RoundedCornersTransformation(4f))
                .build(),
            contentDescription  = article.title,
            contentScale        = ContentScale.Crop,
            modifier             = Modifier
                .size(100.dp)
                .background(Color.LightGray, RoundedCornerShape(4.dp))
        )

        Spacer(Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            // Headline + bookmark button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    article.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                IconButton(onClick = {
                    if (isBookmarked) {
                        BookmarkStorage.removeBookmark(context, article.toBookmark())
                    } else {
                        BookmarkStorage.addBookmark(context, article.toBookmark())
                    }
                    isBookmarked = !isBookmarked
                }) {
                    Icon(
                        imageVector   = if (isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                        contentDescription = if (isBookmarked) "Remove Bookmark" else "Add Bookmark",
                        tint          = if (isBookmarked) Color(0xFF1E88E5) else Color.Gray
                    )
                }
            }

            // Short description snippet
            article.description?.let {
                Spacer(Modifier.height(4.dp))
                Text(
                    it,
                    fontSize = 14.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Source & date row
            if (article.source != null || article.publishedAt != null) {
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    article.source?.let { src ->
                        Text("Source: ${src.name}", fontSize = 12.sp, color = Color.DarkGray)
                    }
                    article.publishedAt?.let { date ->
                        Text(date.take(10), fontSize = 12.sp, color = Color.DarkGray)
                    }
                }
            }
        }
    }
}

/**
 * Bookmark data model as saved in SharedPreferences.
 */
fun Article.toBookmark() = Bookmark(
    url         = url,
    title       = title,
    description = description,
    image       = image,
    sourceName  = source?.name,
    publishedAt = publishedAt
)

/**
 * Shows the bookmarked items list.
 * Refreshes automatically when an item is removed.
 */
@Composable
fun BookmarkScreenContent(onClose: () -> Unit) {
    val context = LocalContext.current
    var bookmarks by remember { mutableStateOf(BookmarkStorage.getBookmarks(context)) }

    // Reload the bookmarks list
    fun refresh() {
        bookmarks = BookmarkStorage.getBookmarks(context)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header with close button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E88E5))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Bookmarks",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            Button(onClick = onClose, colors = ButtonDefaults.buttonColors(containerColor = Color.White)) {
                Text("Close", color = Color(0xFF1E88E5))
            }
        }

        if (bookmarks.isEmpty()) {
            // Empty state message
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No bookmarks yet", fontSize = 20.sp)
            }
        } else {
            // List of bookmarks
            LazyColumn(modifier = Modifier.padding(8.dp)) {
                items(bookmarks) { bookmark ->
                    BookmarkRow(bookmark = bookmark, onRemove = {
                        BookmarkStorage.removeBookmark(context, it)
                        refresh()
                    })
                    Divider(color = Color.LightGray, thickness = 1.dp)
                }
            }
        }
    }
}

/**
 * A single bookmark row with thumbnail, text, and a remove button.
 */
@Composable
fun BookmarkRow(bookmark: Bookmark, onRemove: (Bookmark) -> Unit) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable {
                // Open the original article link
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(bookmark.url)))
            },
        verticalAlignment = Alignment.Top
    ) {
        // Thumbnail from saved bookmark
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(bookmark.image)
                .crossfade(true)
                .transformations(RoundedCornersTransformation(4f))
                .build(),
            contentDescription = bookmark.title,
            contentScale       = ContentScale.Crop,
            modifier            = Modifier
                .size(100.dp)
                .background(Color.LightGray, RoundedCornerShape(4.dp))
        )

        Spacer(Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            // Title + remove icon
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    bookmark.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                IconButton(onClick = { onRemove(bookmark) }) {
                    Icon(
                        imageVector   = Icons.Filled.Bookmark,
                        contentDescription = "Remove Bookmark",
                        tint = Color(0xFF1E88E5)
                    )
                }
            }

            // Description snippet
            bookmark.description?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Source & date row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                bookmark.sourceName?.let {
                    Text("Source: $it", fontSize = 12.sp, color = Color.DarkGray)
                }
                bookmark.publishedAt?.let {
                    Text(it.take(10), fontSize = 12.sp, color = Color.DarkGray)
                }
            }
        }
    }
}

/**
 * Placeholder for unsupported categories.
 */
@Composable
fun PlaceholderPage(category: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("No content for $category yet", fontSize = 20.sp)
    }
}
