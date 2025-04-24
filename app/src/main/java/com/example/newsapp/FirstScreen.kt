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
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Bookmark
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

// API constants for GNews with your provided key.
object ApiConstants {
    const val NEWS_API_KEY = "730a4ea2870ede4f405899aec4dba2f1"
}

// Data model for the news source.
data class Source(
    val name: String,
    val url: String?
)

// Data model for the GNews API response.
data class GNewsResponse(
    val totalArticles: Int,
    val articles: List<Article>
)

// Data model for an Article – includes title, description, URL, image, source, and publishedAt.
data class Article(
    val title: String,
    val description: String?,
    val url: String,
    val image: String?,
    val source: Source?,       // News outlet.
    val publishedAt: String?   // Publication date (ISO 8601).
)

// Retrofit API interface for GNews.
interface GNewsApiService {
    @GET("top-headlines")
    suspend fun getTopHeadlines(
        @Query("topic") topic: String,
        @Query("lang") lang: String = "en",
        @Query("max") max: Int = 10,
        @Query("page") page: Int,
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

// Singleton object to create the Retrofit service instance for GNews.
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FirstScreen() {
    // Toggle search mode and bookmarks mode.
    var isSearchMode by remember { mutableStateOf(false) }
    var showBookmarks by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // Categories for news feed.
    val categories = listOf("General", "Business", "Sports", "Technology")
    val pagerState = rememberPagerState(pageCount = { categories.size })
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        // Header with actions for search and bookmarks.
        Header(
            isSearchMode = isSearchMode,
            searchQuery = searchQuery,
            onSearchTextChange = { newValue -> searchQuery = newValue },
            onSearchToggle = { isSearchMode = !isSearchMode },
            onBookmarkClick = { showBookmarks = !showBookmarks }
        )

        // If bookmarks view is toggled on, display bookmarks.
        if (showBookmarks) {
            BookmarkScreenContent(onClose = { showBookmarks = false })
        }
        // Else, if in search mode and a query is provided, display search results.
        else if (isSearchMode && searchQuery.isNotBlank()) {
            SearchNewsSection(searchQuery = searchQuery)
        }
        // Otherwise, show the standard news feed.
        else {
            Spacer(modifier = Modifier.height(8.dp))
            // Category tabs.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                categories.forEachIndexed { index, category ->
                    Button(
                        onClick = { coroutineScope.launch { pagerState.animateScrollToPage(index) } },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (pagerState.currentPage == index) Color(0xFF1E88E5) else Color.LightGray
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

            // Horizontal Pager showing news for the selected category.
            HorizontalPager(state = pagerState) { page ->
                when (categories[page]) {
                    "General" -> GeneralPage()
                    "Business" -> BusinessPage()
                    "Sports" -> SportsPage()
                    "Technology" -> TechnologyPage()

                }
            }
        }
    }
}

/**
 * Header composable.
 * When not in search mode, shows the title and subtitle.
 * When in search mode, shows an OutlinedTextField for entering search queries.
 * Also includes actions for toggling search and bookmarks.
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
    val gradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFF1E88E5), Color(0xFF42A5F5))
    )
    TopAppBar(
        title = {
            if (!isSearchMode) {
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
                // Show inline search field.
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchTextChange,
                    singleLine = true,
                    label = { Text("Search news...", color = Color.White) },
                    textStyle = LocalTextStyle.current.copy(color = Color.White),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        cursorColor = Color.White,
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.White,
                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        navigationIcon = {
            if (!isSearchMode) {
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
            IconButton(onClick = onSearchToggle) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Toggle Search",
                    tint = Color.White
                )
            }
            IconButton(onClick = onBookmarkClick) {
                Icon(
                    imageVector = Icons.Default.BookmarkBorder,
                    contentDescription = "Toggle Bookmarks",
                    tint = Color.White
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent
        ),
        modifier = Modifier
            .fillMaxWidth()
            .background(gradient)
    )
}

/**
 * Displays the search results by calling the GNews search endpoint.
 */
@Composable
fun SearchNewsSection(searchQuery: String) {
    var articles by remember { mutableStateOf<List<Article>>(emptyList()) }
    var page by remember { mutableStateOf(1) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(searchQuery, page) {
        if (searchQuery.isNotBlank()) {
            isLoading = true
            try {
                val response = GNewsApi.retrofitService.searchNews(
                    query = searchQuery,
                    page = page,
                    max = 10
                )
                articles = articles + response.articles
            } catch (e: Exception) {
                errorMessage = "Error fetching search results: ${e.message}"
            } finally {
                isLoading = false
            }
        } else {
            articles = emptyList()
        }
    }

    errorMessage?.let { msg ->
        Text(text = msg, color = Color.Red, modifier = Modifier.padding(8.dp))
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
        if (!isLoading && articles.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                LaunchedEffect(Unit) {
                    page++
                }
            }
        }
    }
}

/**
 * Displays the news feed for the "General" category.
 */
@Composable
fun GeneralPage() {
    Column {
        Text(
            text = "General News",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(8.dp)
        )
        NewsListPage(apiCategory = "General")
    }
}

/**
 * Displays the news feed for the "Business" category.
 */
@Composable
fun BusinessPage() {
    Column {
        Text(
            text = "Business News",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(8.dp)
        )
        NewsListPage(apiCategory = "Business")
    }
}

/**
 * Displays the news feed for the "Sports" category.
 */
@Composable
fun SportsPage() {
    Column {
        Text(
            text = "Sports News",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(8.dp)
        )
        NewsListPage(apiCategory = "Sports")
    }
}

/**
 * Displays the news feed for the "Technology" category.
 */
@Composable
fun TechnologyPage() {
    Column {
        Text(
            text = "Technology News",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(8.dp)
        )
        NewsListPage(apiCategory = "Technology")
    }
}

/**
 * Displays a list of news articles from the GNews API for the specified category.
 */
@Composable
fun NewsListPage(apiCategory: String) {
    var articles by remember { mutableStateOf<List<Article>>(emptyList()) }
    var page by remember { mutableStateOf(1) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val topic = when (apiCategory) {
        "General" -> "breaking-news"
        "Business" -> "business"
        "Sports" -> "sports"
        "Technology" -> "technology"
        else -> "breaking-news"
    }

    LaunchedEffect(page, apiCategory) {
        isLoading = true
        try {
            val response = GNewsApi.retrofitService.getTopHeadlines(
                topic = topic,
                page = page,
                max = 10
            )
            articles = articles + response.articles
        } catch (e: Exception) {
            errorMessage = "Error fetching news: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    errorMessage?.let { msg ->
        Text(text = msg, color = Color.Red, modifier = Modifier.padding(8.dp))
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
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
        if (!isLoading && articles.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                LaunchedEffect(Unit) {
                    page++
                }
            }
        }
    }
}

/**
 * Converts an Article to a Bookmark object.
 */
fun Article.toBookmark(): Bookmark {
    return Bookmark(
        url = this.url,
        title = this.title,
        description = this.description,
        image = this.image,
        sourceName = this.source?.name,
        publishedAt = this.publishedAt
    )
}

/**
 * Displays a single article row with:
 * - A thumbnail on the left.
 * - Title, description, and a row with the source on the left and published date on the right.
 * - A bookmark icon that toggles bookmark status via BookmarkStorage.
 * Tapping the row (aside from the bookmark icon) opens the article URL in a browser.
 */
@Composable
fun ArticleRow(article: Article) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isBookmarked by remember { mutableStateOf(false) }

    // Update the bookmark state when the article URL changes.
    LaunchedEffect(article.url) {
        isBookmarked = BookmarkStorage.isBookmarked(context, article.toBookmark())
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(article.url))
                context.startActivity(intent)
            }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(article.image)
                .crossfade(true)
                .transformations(RoundedCornersTransformation(4f))
                .build(),
            contentDescription = article.title,
            modifier = Modifier
                .size(100.dp)
                .background(Color.LightGray, shape = RoundedCornerShape(4.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Text(
                    text = article.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                IconButton(onClick = {
                    if (isBookmarked) {
                        BookmarkStorage.removeBookmark(context, article.toBookmark())
                    } else {
                        BookmarkStorage.addBookmark(context, article.toBookmark())
                    }
                    isBookmarked = !isBookmarked
                }) {
                    if (isBookmarked) {
                        Icon(
                            imageVector = Icons.Filled.Bookmark,
                            contentDescription = "Remove Bookmark",
                            tint = Color(0xFF1E88E5)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.BookmarkBorder,
                            contentDescription = "Add Bookmark",
                            tint = Color.Gray
                        )
                    }
                }
            }
            article.description?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = it,
                    fontSize = 14.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (article.source != null || article.publishedAt != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    article.source?.let { src ->
                        Text(
                            text = "Source: ${src.name}",
                            fontSize = 12.sp,
                            color = Color.DarkGray
                        )
                    }
                    article.publishedAt?.let { published ->
                        Text(
                            text = published.take(10),
                            fontSize = 12.sp,
                            color = Color.DarkGray
                        )
                    }
                }
            }
        }
    }
}

/**
 * Displays the bookmarks view using SharedPreferences.
 */
@Composable
fun BookmarkScreenContent(onClose: () -> Unit) {
    val context = LocalContext.current
    // Use a mutable state to track bookmarks, so we can refresh the list when one is removed.
    var bookmarks by remember { mutableStateOf(BookmarkStorage.getBookmarks(context)) }

    // Helper function to refresh the bookmarks from SharedPreferences.
    fun refreshBookmarks() {
        bookmarks = BookmarkStorage.getBookmarks(context)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header for the bookmarks view with a Close button.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E88E5))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Bookmarks",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = onClose,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White)
            ) {
                Text(text = "Close", color = Color(0xFF1E88E5))
            }
        }

        if (bookmarks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "No bookmarks yet", fontSize = 20.sp)
            }
        } else {
            LazyColumn(modifier = Modifier.padding(8.dp)) {
                items(bookmarks) { bookmark ->
                    BookmarkRow(bookmark = bookmark, onRemove = { b ->
                        BookmarkStorage.removeBookmark(context, b)
                        refreshBookmarks()
                    })
                    Divider(color = Color.LightGray, thickness = 1.dp)
                }
            }
        }
    }
}

@Composable
fun BookmarkRow(bookmark: Bookmark, onRemove: (Bookmark) -> Unit) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable {
                // Open article URL when the row is tapped.
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(bookmark.url))
                context.startActivity(intent)
            }
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(bookmark.image)
                    .crossfade(true)
                    .transformations(RoundedCornersTransformation(4f))
                    .build(),
                contentDescription = bookmark.title,
                modifier = Modifier
                    .size(100.dp)
                    .background(Color.LightGray, shape = RoundedCornerShape(4.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Text(
                        text = bookmark.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { onRemove(bookmark) }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Bookmark,
                            contentDescription = "Remove Bookmark",
                            tint = Color(0xFF1E88E5)
                        )
                    }
                }
                bookmark.description?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    bookmark.sourceName?.let {
                        Text(text = "Source: $it", fontSize = 12.sp, color = Color.DarkGray)
                    }
                    bookmark.publishedAt?.let {
                        Text(text = it.take(10), fontSize = 12.sp, color = Color.DarkGray)
                    }
                }
            }
        }
    }
}


/**
 * Displays a single bookmarked item.
 */
@Composable
fun BookmarkRow(bookmark: Bookmark) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(bookmark.url))
                context.startActivity(intent)
            }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(bookmark.image)
                .crossfade(true)
                .transformations(RoundedCornersTransformation(4f))
                .build(),
            contentDescription = bookmark.title,
            modifier = Modifier
                .size(100.dp)
                .background(Color.LightGray, shape = RoundedCornerShape(4.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = bookmark.title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        bookmark.description?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            bookmark.sourceName?.let {
                Text(text = "Source: $it", fontSize = 12.sp, color = Color.DarkGray)
            }
            bookmark.publishedAt?.let {
                Text(text = it.take(10), fontSize = 12.sp, color = Color.DarkGray)
            }
        }
    }
}
