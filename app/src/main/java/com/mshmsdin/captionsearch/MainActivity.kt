package com.mshmsdin.captionsearch

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.mshmsdin.captionsearch.data.CaptionRepository
import com.mshmsdin.captionsearch.data.FilterState
import com.mshmsdin.captionsearch.data.VideoItem
import com.mshmsdin.captionsearch.data.VideoTarget
import com.mshmsdin.captionsearch.data.YouTubeIcons
import com.mshmsdin.captionsearch.ui.theme.CaptionSearchTheme
import com.mshmsdin.captionsearch.youtube.ChannelDetailView
import com.mshmsdin.captionsearch.youtube.ChannelsView
import com.mshmsdin.captionsearch.youtube.ChannelUiState
import com.mshmsdin.captionsearch.youtube.OnboardingScreen
import com.mshmsdin.captionsearch.youtube.HomeView
import com.mshmsdin.captionsearch.youtube.SearchView
import com.mshmsdin.captionsearch.youtube.SettingsSheet
import com.mshmsdin.captionsearch.youtube.ShortsView
import com.mshmsdin.captionsearch.youtube.TopicResultsView
import com.mshmsdin.captionsearch.youtube.TopicsView
import com.mshmsdin.captionsearch.youtube.YouTubeLogo
import com.mshmsdin.captionsearch.youtube.secondsOf
import com.mshmsdin.captionsearch.youtube.VideoView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(0xFF000000.toInt()))
        setContent { CaptionAppRoot() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CaptionAppRoot() {
    val context = LocalContext.current
    val repository = remember { CaptionRepository(context.applicationContext) }
    val scope = rememberCoroutineScope()

    var isLoaded by remember { mutableStateOf(false) }
    var checking by remember { mutableStateOf(true) }
    var version by remember { mutableStateOf(0L) }
    var importing by remember { mutableStateOf(false) }
    var tab by remember { mutableStateOf(0) }
    var style by remember { mutableStateOf("large") }
    var theme by remember { mutableStateOf("royal") }
    var filters by remember { mutableStateOf(FilterState()) }
    var listResult by remember { mutableStateOf<Pair<FilterState, String>?>(null) }
    var searchChannel by remember { mutableStateOf<String?>(null) }
    var selected by remember { mutableStateOf<VideoTarget?>(null) }
    var channelUi by remember { mutableStateOf<ChannelUiState?>(null) }
    var searching by remember { mutableStateOf(false) }
    var searchFocus by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var showSettings by remember { mutableStateOf(false) }
    val prefs = remember { context.getSharedPreferences("captionsearch_prefs", Context.MODE_PRIVATE) }
    var onboardingFinished by remember { mutableStateOf(prefs.getBoolean("onboarding_done", false)) }
    var onboardingStep by remember { mutableStateOf(if (onboardingFinished) 2 else 0) }
    var importDone by remember { mutableStateOf(false) }

    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        importing = true
        importDone = false
        scope.launch {
            val error = try {
                withContext(Dispatchers.IO) { repository.importFrom(uri) }
            } catch (t: Throwable) {
                t.message ?: "فشل الاستيراد"
            } finally {
                importing = false
            }
            checking = false
            isLoaded = repository.isLoaded
            if (error == null) {
                version++
                importDone = true
                Toast.makeText(context, "تم", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                onboardingStep = 1
            }
        }
    }

    LaunchedEffect(Unit) {
        val opened = withContext(Dispatchers.IO) { repository.openExisting() }
        checking = false
        isLoaded = opened
        if (opened) version++
    }

    CaptionSearchTheme(darkTheme = true, themeName = theme) {
        if (!onboardingFinished) {
            OnboardingScreen(
                repository = repository,
                step = onboardingStep,
                importing = importing,
                importDone = importDone,
                onImport = { importer.launch(arrayOf("*/*")) },
                onStart = {
                    onboardingStep = 2
                    prefs.edit().putBoolean("onboarding_done", true).apply()
                    onboardingFinished = true
                }
            )
            return@CaptionSearchTheme
        }

        if (checking || !isLoaded) {
            Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
                Column(Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center) {
                    Button(onClick = { importer.launch(arrayOf("*/*")) }) { Text("استيراد قاعدة البيانات") }
                }
            }
            return@CaptionSearchTheme
        }

        val current = selected
        if (current != null) {
            BackHandler { selected = null }
            Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
                VideoView(repository, current.item, current.start, current.highlight) { sec ->
                    selected = current.copy(start = sec)
                }
            }
            return@CaptionSearchTheme
        }

        if (searching) {
            BackHandler { searching = false; searchChannel = null }
            Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
                SearchView(repository, query, { query = it }, searchChannel,
                    autoFocus = searchFocus,
                    onAutoFocus = { searchFocus = false },
                    onOpenSuggestion = { s ->
                        if (s.kind == "topic" && s.topicId != null) {
                            listResult = FilterState(topicId = s.topicId) to s.label
                            searching = false
                        } else if (!s.videoId.isNullOrBlank()) {
                            scope.launch {
                                val item = withContext(Dispatchers.IO) { repository.videoById(s.videoId) }
                                if (item != null) selected = VideoTarget(item, secondsOf(s.start ?: "00:00:00"))
                            }
                        } else {
                            query = s.label
                        }
                    }) { hit ->
                    scope.launch {
                        val item = withContext(Dispatchers.IO) { repository.videoByRow(hit.videoRowId) }
                        if (item != null) selected = VideoTarget(item, secondsOf(hit.start), query)
                    }
                }
            }
            return@CaptionSearchTheme
        }

        val list = listResult
        if (list != null) {
            BackHandler { listResult = null }
            Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
                TopicResultsView(repository, list.first, list.second, style) { selected = VideoTarget(it, 0) }
            }
            return@CaptionSearchTheme
        }

        val currentChannel = channelUi
        if (currentChannel != null) {
            BackHandler { channelUi = null }
            Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
                ChannelDetailView(repository, currentChannel, version, style,
                    onOpen = { selected = VideoTarget(it, 0) },
                    onListResult = { filter, title -> listResult = filter to title },
                    onSearch = {
                        searchChannel = currentChannel.id
                        query = ""
                        searchFocus = true
                        searching = true
                    })
            }
            return@CaptionSearchTheme
        }

        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = { YouTubeLogo() },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    actions = {
                        IconButton(onClick = {
                            searchChannel = null
                            query = ""
                            searchFocus = true
                            searching = true
                        }, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Default.Search, "بحث")
                        }
                        IconButton(onClick = { showSettings = true }, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Default.Settings, "الإعدادات")
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(selected = tab == 0, onClick = { tab = 0 },
                        icon = { Icon(Icons.Default.Home, null) }, label = { Text("الرئيسية") })
                    NavigationBarItem(selected = tab == 1, onClick = { tab = 1 },
                        icon = { Icon(if (tab == 1) YouTubeIcons.ShortsFilled else YouTubeIcons.ShortsOutlined, "شورتس") }, label = { Text("شورتس") })
                    NavigationBarItem(selected = tab == 2, onClick = { tab = 2 },
                        icon = { Icon(Icons.Default.List, null) }, label = { Text("المواضيع") })
                    NavigationBarItem(selected = tab == 3, onClick = { tab = 3 },
                        icon = { Icon(Icons.Default.AccountCircle, null) }, label = { Text("القنوات") })
                }
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)
                .nestedScroll(scrollBehavior.nestedScrollConnection)) {
                when (tab) {
                    0 -> HomeView(repository, version, style) { selected = VideoTarget(it, 0) }
                    1 -> ShortsView(repository, version) { selected = VideoTarget(it, 0) }
                    2 -> TopicsView(repository, version) { filter, label ->
                        filters = filter
                        listResult = filter to label
                    }
                    3 -> ChannelsView(repository, version) { channelUi = ChannelUiState(it) }
                }
            }
        }

        if (showSettings) {
            SettingsSheet(style, { style = it }, theme, { theme = it },
                onImport = { importer.launch(arrayOf("*/*")) },
                onDismiss = { showSettings = false })
        }
    }
}
