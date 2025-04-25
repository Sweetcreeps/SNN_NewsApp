# Swift News Network (SNN)

A lightweight Android news reader built with Jetpack Compose, Retrofit and Coil.  Users can browse top headlines by category, search for articles, and bookmark favorites for later.

---

## 📦 Build & Run

### Prerequisites

- **Android Studio** Arctic Fox (2020.3.1) or newer  
- **Android SDK**: Compile & target SDK 35  
- **JDK**: Java 11  

### Setup

1. **Clone the repository**  

2. **Open in Android Studio**

   - Choose Open an existing project and point to the project root.

   - Wait for Gradle to sync and download dependencies.

3. **Run on device or emulator**
   - Select an Android 8.0+ emulator or connected device.
   - Emulate a device that has API 31 or above 
   - Click Run ▶️ (or use Shift+F10 on Windows/Linux, ⌘R on macOS).


## 🔗 Third-Party Libraries


| Library | Purpose                 
| ------ | --------------------- |
Retrofit 2 (com.squareup.retrofit2) | HTTP client for calling the GNews REST API
Gson Converter (converter-gson) | JSON ⇄ Kotlin data class serialization
Coil (io.coil-kt:coil-compose) | Async image loading & caching in Compose UIs
Accompanist Pager (accompanist-pager) | Horizontal paging for category tabs
Material 3 Components (material3) | Modern UI widgets and theming
Navigation-Compose (navigation-compose) | (Optional) in-app navigation support
Kotlin Coroutines (kotlinx-coroutines) | Background threading & asynchronous tasks
Jetpack Compose (androidx.compose.*) | Declarative UI framework
Gson (com.google.code.gson) | Bookmarks serialization into SharedPreferences

## ⚠️ Known Issues & Limitations
- No offline caching,
Articles are fetched on demand; if you lose connectivity you’ll see an error message but no stored content.

- API rate limits,
Free GNews keys are limited (≈100 requests/day). Heavy scrolling or repeated searches can exhaust the quota.

- API loading times,
changing categories too fast may cause the API to not keep up which will temporarly show the standard error message  

- Basic error handling,
Network/API failures show a simple red message. There’s no retry button or detailed diagnostics.

- Pagination behavior,
The feed loads 10 articles per page. Very long scroll sessions can feel repetitive.

- Single-screen flow,
 Feed, search, and bookmarks all live in one Composable. Could be refactored to separate screens for clarity.

- Limited theming,
Only a basic blue gradient is applied. No dark-mode support or custom color theming out of the box.

