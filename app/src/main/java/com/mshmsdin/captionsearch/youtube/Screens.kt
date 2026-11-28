@file:OptIn(ExperimentalMaterial3Api::class)

package com.mshmsdin.captionsearch.youtube

import android.content.Context
import android.content.Intent
import android.net.NetworkCapabilities
import android.net.ConnectivityManager
import android.util.Log
import android.view.View
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader
import com.mshmsdin.captionsearch.data.CaptionRepository
import com.mshmsdin.captionsearch.data.ChannelInfo
import com.mshmsdin.captionsearch.data.FilterState
import com.mshmsdin.captionsearch.data.SearchHit
import com.mshmsdin.captionsearch.data.VideoItem
import com.mshmsdin.captionsearch.data.YouTubeIcons
import com.mshmsdin.captionsearch.ui.theme.FONT_NAMES
import com.mshmsdin.captionsearch.ui.theme.THEME_NAMES
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private fun isOnline(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
    val network = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(network) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

private data class LocalHit(val text: String, val start: String, val kind: String)

private const val HOME_PAGE = 10
private const val SHORTS_PAGE = 10
private const val CHANNEL_PAGE = 10
private const val TOPICS_INITIAL = 20
private const val TOPICS_PAGE = 20

@Composable
fun EmptyState(text: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), Alignment.Center) {
        Text(text, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun HighlightedText(
    text: String,
    query: String?,
    fontSize: androidx.compose.ui.unit.TextUnit = 13.sp,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    modifier: Modifier = Modifier
) {
    val markColor = MaterialTheme.colorScheme.onPrimaryContainer
    val markBackground = MaterialTheme.colorScheme.primaryContainer
    val annotated = androidx.compose.runtime.remember(text, query, markColor, markBackground) {
        val q = query?.trim().orEmpty()
        val at = if (q.isEmpty()) -1 else text.indexOf(q, ignoreCase = true)
        if (at < 0) buildAnnotatedString { append(text) }
        else buildAnnotatedString {
            append(text.substring(0, at))
            withStyle(SpanStyle(
                color = markColor,
                background = markBackground,
                fontWeight = FontWeight.Bold
            )) {
                append(text.substring(at, at + q.length))
            }
            append(text.substring(at + q.length))
        }
    }
    Text(annotated, modifier = modifier, fontSize = fontSize, maxLines = maxLines, overflow = overflow)
}

@Composable
fun HomeView(repository: CaptionRepository, version: Long, style: String, onOpen: (VideoItem) -> Unit) {
    var videos by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var loadingMore by remember { mutableStateOf(false) }
    var ended by remember { mutableStateOf(false) }
    val listState = remember { androidx.compose.foundation.lazy.LazyListState() }

    LaunchedEffect(version) {
        loading = true
        ended = false
        videos = emptyList()
        try {
            val page = withContext(Dispatchers.IO) { repository.home(0, HOME_PAGE, false) }
            videos = page
            ended = page.size < HOME_PAGE
        } finally {
            loading = false
        }
    }

    LaunchedEffect(version) {
        snapshotFlow {
            (listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1) to videos.size
        }
            .distinctUntilChanged()
            .collect { (last, size) ->
                if (last < 0 || size == 0) return@collect
                if (loading || loadingMore || ended) return@collect
                if (last < size - 4) return@collect
                loadingMore = true
                try {
                    val page = withContext(Dispatchers.IO) { repository.home(videos.size, HOME_PAGE, false) }
                    if (page.isNotEmpty()) videos = videos + page
                    if (page.size < HOME_PAGE) ended = true
                } finally {
                    loadingMore = false
                }
            }
    }

    when {
        videos.isEmpty() && loading -> Box(Modifier.fillMaxSize())
        videos.isEmpty() -> EmptyState("لا توجد مقاطع")
        else -> LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 4.dp, bottom = 12.dp)
        ) {
            items(count = videos.size, key = { videos[it].rowId }) { i ->
                VideoCard(videos[i], style) { onOpen(videos[i]) }
            }
        }
    }
}

@Composable
fun ShortsView(repository: CaptionRepository, version: Long, onOpen: (VideoItem) -> Unit) {
    var videos by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var loadingMore by remember { mutableStateOf(false) }
    var ended by remember { mutableStateOf(false) }
    val gridState = remember { androidx.compose.foundation.lazy.grid.LazyGridState() }

    LaunchedEffect(version) {
        loading = true
        ended = false
        videos = emptyList()
        try {
            val page = withContext(Dispatchers.IO) { repository.shorts(0, SHORTS_PAGE) }
            videos = page
            ended = page.size < SHORTS_PAGE
        } finally {
            loading = false
        }
    }

    LaunchedEffect(version) {
        snapshotFlow {
            (gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1) to videos.size
        }
            .distinctUntilChanged()
            .collect { (last, size) ->
                if (last < 0 || size == 0) return@collect
                if (loading || loadingMore || ended) return@collect
                if (last < size - 6) return@collect
                loadingMore = true
                try {
                    val page = withContext(Dispatchers.IO) { repository.shorts(videos.size, SHORTS_PAGE) }
                    if (page.isNotEmpty()) videos = videos + page
                    if (page.size < SHORTS_PAGE) ended = true
                } finally {
                    loadingMore = false
                }
            }
    }

    when {
        videos.isEmpty() && loading -> Box(Modifier.fillMaxSize())
        videos.isEmpty() -> EmptyState("لا توجد مقاطع قصيرة")
        else -> LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(6.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(count = videos.size, key = { videos[it].rowId }) { i ->
                Column(Modifier.clickable { onOpen(videos[i]) }) {
                    Box {
                        Thumbnail(videos[i].thumb,
                            Modifier.fillMaxWidth().aspectRatio(9f / 16f).clip(RoundedCornerShape(10.dp)))
                        Box(Modifier.align(Alignment.TopStart)) { TypeBadge(videos[i]) }
                        Box(Modifier.align(Alignment.TopEnd).padding(6.dp)) { CaptionBadge(videos[i].caption) }
                    }
                    Text(videos[i].title, fontSize = 12.sp, maxLines = 2,
                        overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 5.dp))
                }
            }
        }
    }
}

@Composable
fun TopicsView(repository: CaptionRepository, version: Long, onCategory: (FilterState, String) -> Unit) {
    var topics by remember { mutableStateOf<List<com.mshmsdin.captionsearch.data.TopicItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var loadingMore by remember { mutableStateOf(false) }
    var ended by remember { mutableStateOf(false) }
    val listState = remember { androidx.compose.foundation.lazy.LazyListState() }
    LaunchedEffect(version) {
        loading = true
        ended = false
        try {
            val page = withContext(Dispatchers.IO) { repository.topics(TOPICS_INITIAL) }
            topics = page
            ended = page.size < TOPICS_INITIAL
        } finally {
            loading = false
        }
    }
    LaunchedEffect(version) {
        snapshotFlow {
            (listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1) to topics.size
        }.distinctUntilChanged().collect { (last, size) ->
            if (last < 0 || size == 0) return@collect
            if (loading || loadingMore || ended) return@collect
            if (last < size - 4) return@collect
            loadingMore = true
            try {
                val page = withContext(Dispatchers.IO) { repository.topics(size + TOPICS_PAGE) }
                if (page.size > size) topics = page
                ended = page.size <= size
            } finally {
                loadingMore = false
            }
        }
    }
    LazyColumn(Modifier.fillMaxSize(), state = listState) {
        item { Text("المواضيع (${topics.size})", style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) }
        items(count = topics.size) { i ->
            val topic = topics[i]
            Row(Modifier.fillMaxWidth().clickable { onCategory(FilterState(topicId = topic.id), topic.name) }
                .padding(horizontal = 14.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = androidx.compose.foundation.shape.CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(40.dp)) {
                    Box(Modifier, Alignment.Center) { Text(topic.count.toString(), fontSize = 11.sp) }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(topic.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(topic.parent, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            HorizontalDivider()
        }
    }
}

@Composable
fun TopicResultsView(
    repository: CaptionRepository,
    filter: FilterState,
    title: String,
    style: String,
    onOpen: (VideoItem) -> Unit
) {
    var videos by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var loadingMore by remember { mutableStateOf(false) }
    var ended by remember { mutableStateOf(false) }
    val listState = remember { androidx.compose.foundation.lazy.LazyListState() }
    LaunchedEffect(filter) {
        loading = true
        ended = false
        try {
            val page = withContext(Dispatchers.IO) { repository.filtered(filter, 0, HOME_PAGE) }
            videos = page
            ended = page.size < HOME_PAGE
        } finally {
            loading = false
        }
    }
    LaunchedEffect(filter) {
        snapshotFlow {
            (listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1) to videos.size
        }.distinctUntilChanged().collect { (last, size) ->
            if (last < 0 || size == 0) return@collect
            if (loading || loadingMore || ended) return@collect
            if (last < size - 4) return@collect
            loadingMore = true
            try {
                val page = withContext(Dispatchers.IO) { repository.filtered(filter, size, HOME_PAGE) }
                if (page.isNotEmpty()) videos = videos + page
                if (page.size < HOME_PAGE) ended = true
            } finally {
                loadingMore = false
            }
        }
    }
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleMedium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            when {
                videos.isEmpty() && loading -> Box(Modifier.weight(1f))
                videos.isEmpty() -> EmptyState("لا توجد مقاطع", Modifier.weight(1f))
                else -> LazyColumn(Modifier.weight(1f), state = listState) {
                    items(count = videos.size) { i -> VideoCard(videos[i], style) { onOpen(videos[i]) } }
                }
            }
        }
    }
}

@Composable
fun ChannelsView(repository: CaptionRepository, version: Long, onOpen: (String) -> Unit) {
    var channels by remember { mutableStateOf<List<com.mshmsdin.captionsearch.data.ChannelItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    LaunchedEffect(version) {
        loading = true
        try { channels = withContext(Dispatchers.IO) { repository.channels() } }
        finally { loading = false }
    }
    when {
        loading -> Box(Modifier.fillMaxSize())
        channels.isEmpty() -> EmptyState("لا توجد قنوات")
        else -> LazyColumn(Modifier.fillMaxSize()) {
            items(count = channels.size, key = { channels[it].id }) { i ->
                val ch = channels[i]
                Row(Modifier.fillMaxWidth().clickable { onOpen(ch.id) }.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    ChannelAvatar(ch.avatar, 52)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(ch.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${ch.count} مقطع", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                HorizontalDivider()
            }
        }
    }
}

class ChannelUiState(val id: String) {
    var tab by mutableStateOf("videos")
    var kind by mutableStateOf("")
    var videos by mutableStateOf<List<VideoItem>>(emptyList())
    var ended by mutableStateOf(false)
    val videosList = androidx.compose.foundation.lazy.LazyListState()
    val playlistsList = androidx.compose.foundation.lazy.LazyListState()
    val topicsList = androidx.compose.foundation.lazy.LazyListState()
}

@Composable
fun ChannelDetailView(
    repository: CaptionRepository,
    ui: ChannelUiState,
    version: Long,
    style: String,
    onOpen: (VideoItem) -> Unit,
    onListResult: (FilterState, String) -> Unit,
    onSearch: () -> Unit
) {
    val channelId = ui.id
    val tab = ui.tab
    val videos = ui.videos
    val ended = ui.ended
    val listState = ui.videosList
    var info by remember(channelId) { mutableStateOf<ChannelInfo?>(null) }
    var videosLoading by remember { mutableStateOf(true) }
    var loadingMore by remember { mutableStateOf(false) }
    var playlists by remember { mutableStateOf<List<com.mshmsdin.captionsearch.data.PlaylistItem>>(emptyList()) }
    var topics by remember { mutableStateOf<List<com.mshmsdin.captionsearch.data.TopicItem>>(emptyList()) }
    var playlistsLoading by remember(channelId) { mutableStateOf(true) }
    var topicsLoading by remember(channelId) { mutableStateOf(true) }
    LaunchedEffect(channelId, version) {
        info = withContext(Dispatchers.IO) { repository.channelInfo(channelId) }
    }
    LaunchedEffect(channelId, version, tab) {
        if (tab != "playlists" || playlists.isNotEmpty()) return@LaunchedEffect
        playlistsLoading = true
        try { playlists = withContext(Dispatchers.IO) { repository.channelPlaylists(channelId) } }
        finally { playlistsLoading = false }
    }
    LaunchedEffect(channelId, version, tab) {
        if (tab != "topics" || topics.isNotEmpty()) return@LaunchedEffect
        topicsLoading = true
        try { topics = withContext(Dispatchers.IO) { repository.channelTopics(channelId) } }
        finally { topicsLoading = false }
    }
    LaunchedEffect(channelId, version, tab) {
        if (tab == "playlists" || tab == "topics") return@LaunchedEffect
        val kind = when (tab) {
            "live" -> "live"
            "shorts" -> "short"
            else -> "all"
        }
        if (ui.kind == kind && ui.videos.isNotEmpty()) {
            videosLoading = false
            return@LaunchedEffect
        }
        videosLoading = true
        ui.ended = false
        ui.videos = emptyList()
        ui.kind = kind
        try {
            val page = withContext(Dispatchers.IO) {
                repository.channelFeed(channelId, kind, 0, CHANNEL_PAGE)
            }
            ui.videos = page
            ui.ended = page.size < CHANNEL_PAGE
        } finally {
            videosLoading = false
        }
    }
    LaunchedEffect(channelId, version, tab) {
        if (tab == "playlists" || tab == "topics") return@LaunchedEffect
        val kind = when (tab) {
            "live" -> "live"
            "shorts" -> "short"
            else -> "all"
        }
        snapshotFlow {
            (listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1) to videos.size
        }.distinctUntilChanged().collect { (last, size) ->
            if (last < 0 || size == 0) return@collect
            if (videosLoading || loadingMore || ended) return@collect
            if (last < size - 4) return@collect
            loadingMore = true
            try {
                val page = withContext(Dispatchers.IO) {
                    repository.channelFeed(channelId, kind, videos.size, CHANNEL_PAGE)
                }
                if (page.isNotEmpty()) ui.videos = ui.videos + page
                if (page.size < CHANNEL_PAGE) ui.ended = true
            } finally {
                loadingMore = false
            }
        }
    }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            val current = info
            if (current != null) {
                ChannelAvatar(current.avatar, 46)
                Spacer(Modifier.width(10.dp))
                Text(current.title, style = MaterialTheme.typography.titleMedium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            } else {
                Spacer(Modifier.weight(1f))
            }
            IconButton(onClick = onSearch, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.Search, "بحث في القناة")
            }
        }
        val tabs = listOf("videos" to "الفيديوهات", "live" to "البثوث", "shorts" to "شورتس",
            "playlists" to "القوائم", "topics" to "المواضيع")
        Row(Modifier.fillMaxWidth()) {
            tabs.forEach { (v, l) ->
                val selected = tab == v
                Column(Modifier.weight(1f).clickable { ui.tab = v },
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(l, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    Box(Modifier.fillMaxWidth().height(2.dp)
                        .background(if (selected) Color.White else Color.Transparent))
                }
            }
        }
        when (tab) {
            "playlists" -> LazyColumn(Modifier.weight(1f), state = ui.playlistsList) {
                if (playlists.isEmpty() && !playlistsLoading) item {
                    Text("لا توجد قوائم", fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(24.dp))
                }
                items(count = playlists.size) { i ->
                    val item = playlists[i]
                    Row(Modifier.fillMaxWidth()
                        .clickable { onListResult(FilterState(playlistId = item.id, channelIds = setOf(channelId)), item.title) }
                        .padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        LazyCover("pl-${item.id}", { repository.playlistCover(item.id) },
                            Modifier.size(120.dp, 68.dp).clip(RoundedCornerShape(8.dp)))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(item.title, fontWeight = FontWeight.SemiBold, maxLines = 1,
                                overflow = TextOverflow.Ellipsis)
                            Text("${item.count} مقطع", fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    HorizontalDivider()
                }
            }
            "topics" -> LazyColumn(Modifier.weight(1f), state = ui.topicsList) {
                if (topics.isEmpty() && !topicsLoading) item {
                    Text("لا توجد مواضيع", fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(24.dp))
                }
                items(count = topics.size) { i ->
                    val item = topics[i]
                    Row(Modifier.fillMaxWidth()
                        .clickable { onListResult(FilterState(topicId = item.id, channelIds = setOf(channelId)), item.name) }
                        .padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        LazyCover("topic-${item.id}", { repository.topicCover(item.id) },
                            Modifier.size(120.dp, 68.dp).clip(RoundedCornerShape(8.dp)))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(item.name, fontWeight = FontWeight.SemiBold, maxLines = 1,
                                overflow = TextOverflow.Ellipsis)
                            Text("${item.count} مقطع · ${item.parent}", fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    HorizontalDivider()
                }
            }
            else -> when {
                videos.isEmpty() && videosLoading -> Box(Modifier.weight(1f))
                videos.isEmpty() -> EmptyState("لا توجد مقاطع", Modifier.weight(1f))
                else -> LazyColumn(Modifier.weight(1f), state = listState) {
                    items(count = videos.size) { i -> VideoCard(videos[i], style) { onOpen(videos[i]) } }
                }
            }
        }
    }
}

@Composable
fun SearchView(
    repository: CaptionRepository,
    query: String,
    onQuery: (String) -> Unit,
    channelId: String?,
    autoFocus: Boolean = false,
    onAutoFocus: () -> Unit = {},
    onOpenSuggestion: (com.mshmsdin.captionsearch.data.SuggestionItem) -> Unit,
    onOpenHit: (SearchHit) -> Unit
) {
    var suggestions by remember { mutableStateOf<List<com.mshmsdin.captionsearch.data.SuggestionItem>>(emptyList()) }
    var hits by remember { mutableStateOf<List<SearchHit>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var lastError by remember { mutableStateOf<String?>(null) }
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(autoFocus) {
        if (!autoFocus) return@LaunchedEffect
        delay(120)
        focusRequester.requestFocus()
        keyboard?.show()
        onAutoFocus()
    }
    LaunchedEffect(query, channelId) {
        suggestions = emptyList()
        hits = emptyList()
        lastError = null
        if (query.isBlank()) {
            loading = false
            return@LaunchedEffect
        }
        loading = true
        try {
            delay(120)
            val pair = withContext(Dispatchers.IO) {
                repository.searchSuggestions(query, 12, channelId) to
                    repository.searchHits(query, channelId, 80)
            }
            suggestions = pair.first
            hits = pair.second
        } catch (t: CancellationException) {
            throw t
        } catch (t: Throwable) {
            lastError = "${t.javaClass.simpleName}: ${t.message}"
        } finally {
            loading = false
        }
    }
    Column(Modifier.fillMaxSize().imePadding()) {
        Row(Modifier.padding(start = 16.dp, end = 12.dp, top = 2.dp),
            verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = query,
                onValueChange = onQuery,
                placeholder = { Text("بحث") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
                trailingIcon = if (query.isNotEmpty()) {
                    { IconButton(onClick = { onQuery("") }, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.Close, "مسح")
                    } }
                } else null,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant,
                    errorIndicatorColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.weight(1f).focusRequester(focusRequester)
            )
        }
        if (suggestions.isNotEmpty() && query.isNotBlank()) {
            Column(Modifier.padding(horizontal = 16.dp)) {
                suggestions.take(8).forEach { s ->
                    Text("${s.label}  ·  ${s.kind}", modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenSuggestion(s) }
                        .padding(vertical = 8.dp),
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            HorizontalDivider()
        }
        when {
            loading -> Box(Modifier.weight(1f))
            lastError != null -> Text("خطأ: $lastError", fontSize = 12.sp,
                color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
            query.isBlank() -> EmptyState("اكتب كلمة للبحث", Modifier.weight(1f))
            hits.isEmpty() -> EmptyState("لا توجد نتائج", Modifier.weight(1f))
            else -> LazyColumn(Modifier.weight(1f)) {
                items(count = hits.size, key = { hits[it].videoRowId to hits[it].start }) { i ->
                    val hit = hits[i]
                    Row(Modifier.fillMaxWidth().clickable { onOpenHit(hit) }
                        .padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Thumbnail(hit.thumb, Modifier.size(120.dp, 68.dp).clip(RoundedCornerShape(8.dp)))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            HighlightedText(hit.text, query, fontSize = 13.sp, maxLines = 2,
                                overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.height(5.dp))
                            Text("${hit.title} · ${hit.channel} · ${timeText(hit.start)}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
fun SettingsSheet(
    currentStyle: String,
    onStyle: (String) -> Unit,
    currentTheme: String,
    onTheme: (String) -> Unit,
    currentFont: String,
    onFont: (String) -> Unit,
    onImport: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp)) {
            Text("الإعدادات", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
            Button(onClick = onImport, Modifier.fillMaxWidth()) { Text("استيراد قاعدة البيانات") }
            Spacer(Modifier.height(14.dp))
            Text("شكل العرض", style = MaterialTheme.typography.titleSmall)
            Row {
                listOf("large" to "كبيرة", "medium" to "بطاقتين", "small" to "صغيرة").forEach { (v, l) ->
                    FilterChip(selected = currentStyle == v, onClick = { onStyle(v) }, label = { Text(l) },
                        modifier = Modifier.padding(end = 6.dp))
                }
            }
            Spacer(Modifier.height(14.dp))
            Text("السمة", style = MaterialTheme.typography.titleSmall)
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                THEME_NAMES.forEach { t ->
                    FilterChip(selected = currentTheme == t, onClick = { onTheme(t) }, label = { Text(t) },
                        modifier = Modifier.padding(end = 6.dp))
                }
            }
            Spacer(Modifier.height(14.dp))
            Text("الخط", style = MaterialTheme.typography.titleSmall)
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                FONT_NAMES.forEach { f ->
                    FilterChip(selected = currentFont == f, onClick = { onFont(f) },
                        label = { Text(f) }, modifier = Modifier.padding(end = 6.dp))
                }
            }
            Spacer(Modifier.height(20.dp))
            TextButton(onClick = onDismiss, Modifier.fillMaxWidth()) { Text("إغلاق") }
        }
    }
}

@Composable
fun VideoView(
    repository: CaptionRepository,
    item: VideoItem,
    start: Int,
    highlight: String?,
    onTime: (Int) -> Unit
) {
    val context = LocalContext.current
    val originHost = "com.mshmsdin.captionsearch"
    val origin = "https://$originHost"
    val playerUrl = "$origin/assets/youtube_player.html?v=${android.net.Uri.encode(item.id)}" +
        "&start=$start&origin=${android.net.Uri.encode(origin)}"
    val online = remember(item.id) { isOnline(context) }
    var chapters by remember { mutableStateOf<List<com.mshmsdin.captionsearch.data.ChapterItem>>(emptyList()) }
    var cues by remember { mutableStateOf<List<com.mshmsdin.captionsearch.data.Cue>>(emptyList()) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var playerReady by remember(item.id) { mutableStateOf(false) }
    var playing by remember(item.id) { mutableStateOf(false) }
    var bridgeAlive by remember(item.id) { mutableStateOf(false) }
    var jsDuration by remember(item.id) { mutableFloatStateOf(0f) }
    var position by remember(item.id) { mutableFloatStateOf(start.toFloat()) }
    var scrub by remember(item.id) { mutableStateOf<Float?>(null) }
    var follow by remember(item.id) { mutableStateOf(true) }
    var inQuery by remember(item.id) { mutableStateOf("") }
    var descExpanded by remember(item.id) { mutableStateOf(false) }
    var chaptersOpen by remember(item.id) { mutableStateOf(false) }
    val cueListState = rememberLazyListState()

    LaunchedEffect(item.rowId) {
        val data = withContext(Dispatchers.IO) {
            repository.chaptersOf(item.rowId) to repository.cuesOf(item.rowId)
        }
        chapters = data.first
        cues = data.second
    }

    val duration = remember(item.duration, cues, jsDuration) {
        when {
            jsDuration > 1f -> jsDuration
            durationSeconds(item.duration) > 0 -> durationSeconds(item.duration).toFloat()
            cues.isNotEmpty() -> secondsOf(cues.last().startTime) + 90f
            else -> 1f
        }.coerceAtLeast(1f)
    }
    val activeCueIdx = remember(cues, position) {
        if (cues.isEmpty()) -1
        else cues.indexOfLast { secondsOf(it.startTime) <= position.toInt() }
    }
    val activeChapterIdx = remember(chapters, position) {
        if (chapters.isEmpty()) -1
        else chapters.indexOfLast { secondsOf(it.start) <= position.toInt() }
    }

    fun js(code: String) { webView?.evaluateJavascript(code, null) }

    val seek: (Int) -> Unit = { sec ->
        position = sec.toFloat()
        follow = true
        onTime(sec)
        js("window.seekTo($sec);")
    }

    val listDragged by cueListState.interactionSource.collectIsDraggedAsState()
    LaunchedEffect(listDragged) { if (listDragged) follow = false }
    LaunchedEffect(activeCueIdx, follow) {
        if (follow && activeCueIdx >= 0) cueListState.animateScrollToItem(activeCueIdx)
    }
    LaunchedEffect(playing, bridgeAlive) {
        if (!playing || bridgeAlive) return@LaunchedEffect
        while (true) {
            delay(500)
            if (scrub == null) position = (position + 0.5f).coerceAtMost(duration)
        }
    }

    val inHits = remember(inQuery, cues) {
        if (inQuery.isBlank()) emptyList()
        else cues.filter { it.text.contains(inQuery, ignoreCase = true) }
            .take(60)
            .map { LocalHit(it.text, it.startTime, "") }
    }


    fun openYoutubeAt(sec: Int) {
        val intent = Intent(Intent.ACTION_VIEW,
            android.net.Uri.parse("https://www.youtube.com/watch?v=${item.id}&t=${sec}s"))
        context.startActivity(Intent.createChooser(intent, "YouTube"))
    }

    val openYoutube = { openYoutubeAt(start) }

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).clipToBounds().background(Color.Black)) {
                if (online) {
                    key(item.id) {
                        AndroidView(
                            factory = { ctx ->
                                WebView(ctx).apply {
                                    setBackgroundColor(android.graphics.Color.BLACK)
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    settings.mediaPlaybackRequiresUserGesture = false
                                    settings.useWideViewPort = false
                                    settings.loadWithOverviewMode = false
                                    webChromeClient = WebChromeClient()
                                    val assetLoader = WebViewAssetLoader.Builder()
                                        .setDomain(originHost)
                                        .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
                                        .build()
                                    webViewClient = object : WebViewClient() {
                                        override fun shouldInterceptRequest(
                                            view: WebView,
                                            request: android.webkit.WebResourceRequest
                                        ): android.webkit.WebResourceResponse? =
                                            assetLoader.shouldInterceptRequest(request.url)

                                        @Suppress("DEPRECATION")
                                        override fun shouldInterceptRequest(
                                            view: WebView,
                                            url: String?
                                        ): android.webkit.WebResourceResponse? =
                                            assetLoader.shouldInterceptRequest(android.net.Uri.parse(url))
                                    }
                                    val cookieManager = CookieManager.getInstance()
                                    cookieManager.setAcceptCookie(true)
                                    cookieManager.setAcceptThirdPartyCookies(this, true)
                                    addJavascriptInterface(object {
                                        @JavascriptInterface
                                        fun onError(code: Int) {
                                            Log.e("YTWeb", "player error=$code")
                                        }
                                        @JavascriptInterface
                                        fun onReady() {
                                            post { playerReady = true; playing = true }
                                        }
                                    }, "AndroidError")
                                    addJavascriptInterface(object {
                                        @JavascriptInterface
                                        fun onTime(sec: Double, dur: Double, state: Int) {
                                            post {
                                                bridgeAlive = true
                                                if (dur > 1.0) jsDuration = dur.toFloat()
                                                playing = state == 1 || state == 3
                                                if (scrub == null) position = sec.toFloat()
                                            }
                                        }
                                    }, "AndroidPlayer")
                                    loadUrl(playerUrl)
                                    webView = this
                                }
                            },
                            modifier = Modifier.fillMaxSize().clipToBounds(),
                            onRelease = { wv ->
                                if (webView === wv) webView = null
                                wv.loadUrl("about:blank")
                                wv.removeAllViews()
                                wv.destroy()
                            }
                        )
                        DisposableEffect(item.id) {
                            onDispose { webView?.onPause() }
                        }
                    }
                }
                Thumbnail(item.thumb, Modifier.fillMaxSize())
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.15f)))

                if (online) {
                    Box(
                        Modifier.align(Alignment.Center).size(66.dp).clip(CircleShape)
                            .background(Color.Black.copy(alpha = if (playing) 0.30f else 0.55f))
                            .clickable {
                                if (!playerReady) return@clickable
                                playing = !playing
                                js("window.playPause && window.playPause();")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (playing) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(Modifier.size(5.dp, 24.dp).background(Color.White, RoundedCornerShape(1.5.dp)))
                                Box(Modifier.size(5.dp, 24.dp).background(Color.White, RoundedCornerShape(1.5.dp)))
                            }
                        } else {
                            Icon(Icons.Default.PlayArrow, "تشغيل", tint = Color.White,
                                modifier = Modifier.size(38.dp))
                        }
                    }
                }

                Column(
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                        .background(Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.80f))))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    ChapterSeekBar(
                        position = position,
                        duration = duration,
                        chapters = chapters,
                        title = item.title,
                        onScrub = { scrub = it },
                        onCommit = { sec ->
                            scrub = null
                            seek(sec.toInt())
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (chapters.isNotEmpty()) {
                    Row(
                        Modifier.align(Alignment.TopEnd).padding(10.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black.copy(alpha = 0.65f))
                            .clickable { chaptersOpen = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(YouTubeIcons.PlaylistOutlined, null, tint = Color.White,
                            modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("الفصول ${chapters.size}", fontSize = 12.sp,
                            fontWeight = FontWeight.Medium, color = Color.White)
                    }
                }
            }
            Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp)) {
                Text(item.title, style = MaterialTheme.typography.titleMedium,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(dateTimeLabel(item.publishedAt.ifBlank { item.date }, item.duration),
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f))
                    val rot by animateFloatAsState(
                        targetValue = if (descExpanded) 180f else 0f,
                        animationSpec = tween(200), label = "chevron"
                    )
                    Box(Modifier.size(32.dp).clip(CircleShape)
                        .clickable { descExpanded = !descExpanded },
                        contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.KeyboardArrowDown,
                            contentDescription = if (descExpanded) "إخفاء الوصف" else "عرض الوصف",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp).graphicsLayer { rotationZ = rot })
                    }
                }
                AnimatedVisibility(
                    visible = descExpanded,
                    enter = expandVertically(tween(220)) + fadeIn(tween(220)),
                    exit = shrinkVertically(tween(180)) + fadeOut(tween(120))
                ) {
                    Text(item.description.ifBlank { "لا يوجد وصف" }, fontSize = 13.sp,
                        lineHeight = 20.sp, color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 6.dp, bottom = 4.dp))
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ChannelAvatar(item.avatar, 34)
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(item.channel, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    TextButton(onClick = openYoutube) { Text("يوتيوب") }
                }
                Spacer(Modifier.height(6.dp))
                TextField(
                    value = inQuery,
                    onValueChange = { inQuery = it },
                    placeholder = { Text("ابحث في نص المقطع فقط", fontSize = 12.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {}),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (inQuery.isNotBlank()) {
                LazyColumn(
                    Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    item {
                        Text("نتائج في النص (${inHits.size})", fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 6.dp))
                    }
                    if (inHits.isEmpty()) item {
                        Text("لا توجد نتائج في نص هذا المقطع", fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp))
                    }
                    items(inHits) { hit ->
                        Row(Modifier.fillMaxWidth().clickable { seek(secondsOf(hit.start)) }
                            .padding(horizontal = 6.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.Top) {
                            Text(timeText(hit.start), fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.width(64.dp))
                            HighlightedText(hit.text, inQuery, fontSize = 13.sp,
                                maxLines = 3, modifier = Modifier.weight(1f))
                        }
                        HorizontalDivider()
                    }
                }
            } else {
                LazyColumn(
                    Modifier.weight(1f),
                    state = cueListState,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    if (cues.isEmpty()) item {
                        Text("لا يوجد نص", fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    itemsIndexed(cues, key = { _, cue -> cue.rowId }) { i, cue ->
                        val matched = i == activeCueIdx ||
                            (!highlight.isNullOrBlank() &&
                                cue.text.contains(highlight, ignoreCase = true))
                        Row(Modifier.fillMaxWidth().clickable { seek(secondsOf(cue.startTime)) }
                            .background(if (matched) MaterialTheme.colorScheme.primaryContainer
                                        else Color.Transparent)
                            .padding(horizontal = 6.dp, vertical = 6.dp)) {
                            Text(timeText(cue.startTime),
                                color = if (matched) MaterialTheme.colorScheme.onPrimaryContainer
                                        else MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp, modifier = Modifier.width(68.dp))
                            if (matched) HighlightedText(cue.text, highlight ?: inQuery,
                                fontSize = 13.sp, maxLines = Int.MAX_VALUE,
                                modifier = Modifier.weight(1f))
                            else Text(cue.text, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            if (chaptersOpen && chapters.isNotEmpty()) {
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                val activeIdx = activeChapterIdx
                ModalBottomSheet(
                    onDismissRequest = { chaptersOpen = false },
                    sheetState = sheetState,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        Text("الفصول", style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 10.dp))
                        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                            itemsIndexed(chapters, key = { _, ch -> ch.start + ch.title }) { idx, ch ->
                                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        chaptersOpen = false
                                        seek(secondsOf(ch.start))
                                    }
                                    .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.width(112.dp).aspectRatio(16f / 9f)) {
                                        Thumbnail(item.thumb,
                                            Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)))
                                        Box(
                                            Modifier.align(Alignment.Center)
                                                .clip(RoundedCornerShape(6.dp))
                                                .clickable { openYoutubeAt(secondsOf(ch.start)) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(YouTubeIcons.YouTube, "فتح في يوتيوب",
                                                modifier = Modifier.size(46.dp, 32.dp))
                                        }
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(ch.title, fontSize = 13.sp,
                                            fontWeight = if (idx == activeIdx) FontWeight.SemiBold
                                                         else FontWeight.Normal,
                                            maxLines = 2, overflow = TextOverflow.Ellipsis,
                                            color = MaterialTheme.colorScheme.onSurface)
                                        Spacer(Modifier.height(2.dp))
                                        Text(timeText(ch.start), fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    if (idx == activeIdx) {
                                        Icon(Icons.Default.PlayArrow, null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ChapterSeekBar(
    position: Float,
    duration: Float,
    chapters: List<com.mshmsdin.captionsearch.data.ChapterItem>,
    title: String,
    onScrub: (Float) -> Unit,
    onCommit: (Float) -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = Color.White
) {
    val measurer = rememberTextMeasurer()
    var widthPx by remember { mutableFloatStateOf(1f) }
    var dragging by remember { mutableStateOf(false) }
    var dragX by remember { mutableFloatStateOf(0f) }
    val dur = duration.coerceAtLeast(1f)
    val frac = (if (dragging) dragX / widthPx else position / dur).coerceIn(0f, 1f)
    val nowSec = (frac * dur).toInt()

    Column(modifier) {
        Row(Modifier.fillMaxWidth().padding(bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text(
                chapters.lastOrNull { secondsOf(it.start) <= nowSec }?.title ?: title,
                fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.White,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text("${fmtClock(nowSec)} / ${fmtClock(duration.toInt())}",
                fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.padding(start = 8.dp))
        }
        Canvas(
            Modifier.fillMaxWidth().height(46.dp)
                .onSizeChanged { widthPx = it.width.toFloat().coerceAtLeast(1f) }
                .pointerInput(dur) {
                    detectTapGestures { o ->
                        onCommit((o.x / size.width.toFloat()).coerceIn(0f, 1f) * dur)
                    }
                }
                .pointerInput(dur) {
                    detectHorizontalDragGestures(
                        onDragStart = { o ->
                            dragging = true
                            dragX = o.x
                            onScrub((o.x / size.width.toFloat()).coerceIn(0f, 1f) * dur)
                        },
                        onHorizontalDrag = { change, _ ->
                            change.consume()
                            dragX = change.position.x
                            onScrub((change.position.x / size.width.toFloat()).coerceIn(0f, 1f) * dur)
                        },
                        onDragEnd = {
                            dragging = false
                            onCommit((dragX / size.width.toFloat()).coerceIn(0f, 1f) * dur)
                        },
                        onDragCancel = { dragging = false }
                    )
                }
        ) {
            val barY = size.height - 11.dp.toPx()
            val barH = 4.dp.toPx()
            val rad = CornerRadius(barH / 2f)
            drawRoundRect(Color.White.copy(alpha = 0.30f),
                Offset(0f, barY - barH / 2), Size(size.width, barH), rad)
            drawRoundRect(accent,
                Offset(0f, barY - barH / 2), Size(size.width * frac, barH), rad)

            chapters.forEachIndexed { i, ch ->
                val cs = secondsOf(ch.start).toFloat() / dur * size.width
                val next = chapters.getOrNull(i + 1)
                    ?.let { secondsOf(it.start).toFloat() / dur * size.width } ?: size.width
                if (i > 0) drawLine(
                    Color.White.copy(alpha = 0.9f),
                    Offset(cs, barY - barH / 2 - 5.dp.toPx()),
                    Offset(cs, barY + barH / 2 + 5.dp.toPx()),
                    1.5.dp.toPx()
                )
                val segW = next - cs
                if (segW > 44.dp.toPx()) {
                    val laid = measurer.measure(
                        AnnotatedString(ch.title),
                        style = TextStyle(fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.78f)),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        constraints = Constraints(
                            maxWidth = (segW - 8.dp.toPx()).toInt().coerceAtLeast(1))
                    )
                    clipRect(cs + 2.dp.toPx(), 0f, next - 2.dp.toPx(), size.height) {
                        drawText(laid, topLeft = Offset(cs + 3.dp.toPx(), 0f))
                    }
                }
            }
            drawCircle(Color.White, 7.dp.toPx(), Offset(size.width * frac, barY))
            drawCircle(accent, 4.dp.toPx(), Offset(size.width * frac, barY))
            if (dragging) {
                val bubble = measurer.measure(
                    AnnotatedString(fmtClock(nowSec)),
                    style = TextStyle(fontSize = 11.sp, color = Color.Black,
                        fontWeight = FontWeight.Bold)
                )
                val bw = bubble.size.width + 14.dp.toPx()
                val bh = bubble.size.height + 6.dp.toPx()
                val bx = (size.width * frac - bw / 2).coerceIn(0f, size.width - bw)
                drawRoundRect(Color.White, Offset(bx, barY - bh - 14.dp.toPx()),
                    Size(bw, bh), CornerRadius(6.dp.toPx()))
                drawText(bubble,
                    topLeft = Offset(bx + 7.dp.toPx(), barY - bh - 14.dp.toPx() + 3.dp.toPx()))
            }
        }
    }
}

private fun durationSeconds(text: String): Int {
    val p = text.split(':').mapNotNull { it.trim().toIntOrNull() }
    return when (p.size) {
        3 -> p[0] * 3600 + p[1] * 60 + p[2]
        2 -> p[0] * 60 + p[1]
        1 -> p[0]
        else -> 0
    }
}

private fun fmtClock(sec: Int): String {
    val s = sec.coerceAtLeast(0)
    val h = s / 3600
    val m = (s % 3600) / 60
    val r = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, r) else "%02d:%02d".format(m, r)
}
