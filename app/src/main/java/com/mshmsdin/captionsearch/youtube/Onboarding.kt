package com.mshmsdin.captionsearch.youtube

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mshmsdin.captionsearch.data.CaptionRepository
import com.mshmsdin.captionsearch.data.ChannelItem
import com.mshmsdin.captionsearch.data.YouTubeIcons
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val ONBOARD_PAGES = 5
private const val IMPORT_PAGE = ONBOARD_PAGES - 1

private data class ChannelTeaser(val title: String, val desc: String, val iconRes: Int, val avatar: ByteArray? = null)

private val fallbackTeasers = listOf(
    ChannelTeaser("محمد بن شمس الدين", "الدروس والمحاضرات", ChannelArt.iconFor("محمد بن شمس الدين")),
    ChannelTeaser("الردود العلمية", "ردود على الشبهات", ChannelArt.iconFor("الردود العلمية")),
    ChannelTeaser("كشف التلبيس", "تفنيد الأباطيل", ChannelArt.iconFor("كشف التلبيس")),
    ChannelTeaser("بثوث محمد بن شمس الدين", "اللقاءات المباشرة", ChannelArt.iconFor("بثوث محمد بن شمس الدين"))
)

private data class DemoStep(val query: String, val headline: String, val results: List<Pair<String, String>>)

private val demos = listOf(
    DemoStep(
        "الصلاة", "ابحث بدون إنترنت",
        listOf(
            "فأول ما يجب على المسلم تعلمه الصلاة" to "الشيخ · 00:04:12",
            "والصلاة نور، وهي عمود الدين" to "الشيخ · 01:12:45"
        )
    ),
    DemoStep(
        "الجماعة", "من السؤال إلى الجواب",
        listOf(
            "ما حكم صلاة الجماعة؟ قال النبي صلى الله عليه وسلم" to "ردود العلمية · 00:14:22",
            "فضل صلاة الجماعة على فردها درجات" to "الشيخ · 00:16:03"
        )
    ),
    DemoStep(
        "الكابشن", "الكابشن والفصول",
        listOf(
            "الحمد لله والصلاة والسلام على رسول الله" to "00:00:12",
            "أما بعد، فإن أصدق الحديث كتاب الله" to "00:00:31"
        )
    ),
    DemoStep(
        "المواضيع", "كل المحتوى في مكان واحد",
        listOf(
            "تصفح المواضيع عند الشيخ محمد بن شمس الدين" to "9,765 موضوعًا",
            "قوائم الشيخ الموثقة في التطبيق" to "145 قائمة"
        )
    )
)

@Composable
fun OnboardingScreen(
    repository: CaptionRepository,
    step: Int,
    importing: Boolean,
    importDone: Boolean,
    onImport: () -> Unit,
    onStart: () -> Unit
) {
    var channels by remember { mutableStateOf<List<ChannelItem>>(emptyList()) }
    LaunchedEffect(Unit) { channels = withContext(Dispatchers.IO) { repository.channels() } }
    val scheme = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(
        initialPage = if (step >= 2 || importDone) IMPORT_PAGE else 0,
        pageCount = { ONBOARD_PAGES }
    )
    LaunchedEffect(importDone) {
        if (importDone) pagerState.animateScrollToPage(IMPORT_PAGE)
    }
    Surface(Modifier.fillMaxSize(), color = scheme.background) {
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            YouTubeLogo(Modifier.height(30.dp))
            Spacer(Modifier.height(6.dp))
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                userScrollEnabled = !importing,
                key = { it }
            ) { page ->
                val active = pagerState.currentPage == page
                var sceneDone by remember { mutableStateOf(false) }
                LaunchedEffect(active) { if (!active) sceneDone = false }
                when (page) {
                    in 0 until 4 -> {
                        val demo = demos[page]
                        val heroExtra: @Composable () -> Unit = when (page) {
                            2 -> { { PlayerHero(scheme) } }
                            3 -> { { BrowseHero(channels) } }
                            else -> { {} }
                        }
                        PageScaffold(active, demo.headline, sceneDone, scheme) {
                            TypingDemo(
                                active = active,
                                demo = demo,
                                scheme = scheme,
                                heroExtra = heroExtra
                            ) { sceneDone = true }
                        }
                    }
                    else -> ImportPage(importing, importDone, onImport, scheme)
                }
            }
            Spacer(Modifier.height(10.dp))
            PageDots(pagerState.currentPage, ONBOARD_PAGES, scheme)
            Spacer(Modifier.height(12.dp))
            val onImportPage = pagerState.currentPage == IMPORT_PAGE
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (pagerState.currentPage > 0 && !importing) {
                    OutlinedButton(
                        onClick = { scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1) } },
                        modifier = Modifier.weight(1f).height(56.dp),
                        border = BorderStroke(1.dp, scheme.outline),
                        shape = RoundedCornerShape(28.dp)
                    ) { Text("السابق", fontSize = 17.sp) }
                }
                Button(
                    onClick = {
                        if (onImportPage) {
                            if (importDone) onStart()
                        } else scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    },
                    enabled = !onImportPage || (importDone && !importing),
                    modifier = Modifier.weight(1f).height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = scheme.primary, contentColor = scheme.onPrimary,
                        disabledContainerColor = scheme.surfaceVariant,
                        disabledContentColor = scheme.onSurfaceVariant.copy(alpha = 0.55f)
                    )
                ) { Text(if (onImportPage) "البَدأ" else "التالي", fontSize = 17.sp) }
            }
        }
    }
}

@Composable
private fun PageScaffold(
    active: Boolean,
    headline: String,
    showHeadline: Boolean,
    scheme: androidx.compose.material3.ColorScheme,
    hero: @Composable () -> Unit
) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { hero() }
        Spacer(Modifier.height(12.dp))
        if (showHeadline) AnimatedHeadline(headline, scheme)
        else Spacer(Modifier.height(52.dp))
    }
}

@Composable
private fun AnimatedHeadline(text: String, scheme: androidx.compose.material3.ColorScheme) {
    val a = remember { Animatable(0f) }
    LaunchedEffect(text) {
        a.snapTo(0f)
        delay(80)
        a.animateTo(1f, spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessLow))
    }
    Box(Modifier.fillMaxWidth().height(52.dp), contentAlignment = Alignment.Center) {
        Surface(
            color = scheme.primaryContainer,
            shape = RoundedCornerShape(26.dp),
            modifier = Modifier.fillMaxWidth().graphicsLayer { scaleX = 0.6f + 0.4f * a.value }
        ) {
            Text(
                text, fontSize = 24.sp, fontWeight = FontWeight.Bold,
                color = scheme.onPrimaryContainer, textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 10.dp).graphicsLayer {
                    alpha = a.value
                    translationY = (1f - a.value) * 16
                }
            )
        }
    }
}

@Composable
private fun RevealItem(
    active: Boolean,
    index: Int,
    content: @Composable () -> Unit
) {
    val v = remember { Animatable(0f) }
    LaunchedEffect(active) {
        if (!active) {
            v.snapTo(0f)
            return@LaunchedEffect
        }
        v.snapTo(0f)
        delay(index * 90L)
        v.animateTo(1f, spring(dampingRatio = 0.68f, stiffness = Spring.StiffnessMediumLow))
    }
    Box(Modifier.graphicsLayer {
        alpha = v.value.coerceIn(0f, 1f)
        translationY = (1f - v.value) * 40
        val s = 0.9f + 0.1f * v.value
        scaleX = s
        scaleY = s
    }) { content() }
}

@Composable
private fun TypingDemo(
    active: Boolean,
    demo: DemoStep,
    scheme: androidx.compose.material3.ColorScheme,
    heroExtra: @Composable () -> Unit,
    onDone: () -> Unit
) {
    var typed by remember { mutableStateOf(0) }
    val done = typed >= demo.query.length
    LaunchedEffect(active) {
        if (!active) {
            typed = 0
            return@LaunchedEffect
        }
        typed = 0
        delay(420)
        while (typed < demo.query.length) {
            delay(130)
            typed++
        }
    }
    LaunchedEffect(done) {
        if (done) {
            delay(1100)
            onDone()
        }
    }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = scheme.surfaceContainerHigh,
            tonalElevation = 4.dp,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(Modifier.padding(horizontal = 22.dp, vertical = 22.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Search, null, tint = scheme.primary,
                    modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(16.dp))
                Text(demo.query.take(typed), fontSize = 23.sp, fontWeight = FontWeight.Bold,
                    color = scheme.onSurface)
                if (!done) Box(Modifier.width(2.5.dp).height(26.dp).background(scheme.primary))
            }
        }
        Spacer(Modifier.height(14.dp))
        heroExtra()
        Spacer(Modifier.height(12.dp))
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            demo.results.forEachIndexed { i, (caption, meta) ->
                RevealItem(active = done, index = i) {
                    ResultCard(caption, meta, demo.query, scheme)
                }
            }
        }
    }
}

@Composable
private fun ResultCard(
    text: String, meta: String, word: String,
    scheme: androidx.compose.material3.ColorScheme
) {
    Surface(shape = RoundedCornerShape(18.dp), color = scheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(ChannelArt.covers[0]), null, contentScale = ContentScale.Crop,
                modifier = Modifier.size(120.dp, 68.dp).clip(RoundedCornerShape(12.dp)))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                val i = text.indexOf(word)
                val annotated = if (i < 0) AnnotatedString(text) else buildAnnotatedString {
                    append(text.substring(0, i))
                    withStyle(SpanStyle(color = scheme.onPrimaryContainer,
                        background = scheme.primaryContainer, fontWeight = FontWeight.Bold)) {
                        append(word)
                    }
                    append(text.substring(i + word.length))
                }
                Text(annotated, fontSize = 14.sp, color = scheme.onSurface,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(5.dp))
                Text(meta, fontSize = 11.sp, color = scheme.onSurfaceVariant, maxLines = 1)
            }
        }
    }
}

@Composable
private fun PlayerHero(scheme: androidx.compose.material3.ColorScheme) {
    Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(16.dp))) {
        Image(painterResource(ChannelArt.covers[1]), null,
            contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        Box(Modifier.align(Alignment.Center).size(56.dp)
            .background(Color.Black.copy(alpha = 0.5f), CircleShape), Alignment.Center) {
            Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(34.dp))
        }
        Surface(color = Color.Black.copy(alpha = 0.65f), shape = RoundedCornerShape(6.dp),
            modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)) {
            Text("CC", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
        }
    }
}

@Composable
private fun BrowseHero(channels: List<ChannelItem>) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        CoverMosaic()
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            fallbackTeasers.take(4).forEach { ch ->
                val real = channels.firstOrNull { it.title.contains(ch.title.take(5)) }?.avatar
                if (real != null) ChannelAvatar(real, 44)
                else Image(painterResource(ch.iconRes), null, contentScale = ContentScale.Crop,
                    modifier = Modifier.size(44.dp).clip(CircleShape))
            }
        }
    }
}

@Composable
private fun CoverMosaic(modifier: Modifier = Modifier) {
    val cards = listOf(
        Triple(0, -11f, -70), Triple(1, -6f, -37),
        Triple(3, 6f, 37), Triple(4, 11f, 70), Triple(2, 0f, 0)
    )
    Box(modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
        cards.forEach { (i, angle, dx) ->
            val w = if (i == 2) 184.dp else 140.dp
            Image(
                painterResource(ChannelArt.covers[i]), null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .offset(x = dx.dp, y = (if (i == 2) 0 else 10).dp)
                    .graphicsLayer { rotationZ = angle }
                    .shadow(if (i == 2) 16.dp else 8.dp, RoundedCornerShape(14.dp))
                    .clip(RoundedCornerShape(14.dp))
                    .size(w, w * 9f / 16f)
            )
        }
    }
}

@Composable
private fun PageDots(current: Int, total: Int, scheme: androidx.compose.material3.ColorScheme) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically) {
        repeat(total) { i ->
            val active = i == current
            Box(
                Modifier.height(8.dp).width(if (active) 22.dp else 8.dp)
                    .clip(CircleShape)
                    .background(if (active) scheme.primary else scheme.outlineVariant)
            )
        }
    }
}

@Composable
private fun ImportPage(
    importing: Boolean,
    done: Boolean,
    onImport: () -> Unit,
    scheme: androidx.compose.material3.ColorScheme
) {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when {
            importing -> ImportBusyContent(scheme)
            done -> {
                Box(Modifier.size(88.dp).background(scheme.primaryContainer, CircleShape),
                    Alignment.Center) {
                    Icon(Icons.Default.Check, null, tint = scheme.primary,
                        modifier = Modifier.size(44.dp))
                }
                Spacer(Modifier.height(20.dp))
                Text("اكتمل الاستيراد", fontSize = 22.sp, fontWeight = FontWeight.Bold,
                    color = scheme.onSurface, textAlign = TextAlign.Center)
            }
            else -> {
                Box(Modifier.size(120.dp).background(scheme.primaryContainer, CircleShape),
                    Alignment.Center) {
                    Icon(YouTubeIcons.VideoFilled, null, tint = scheme.primary,
                        modifier = Modifier.size(60.dp))
                }
                Spacer(Modifier.height(24.dp))
                Text("استيراد قاعدة البيانات", fontSize = 24.sp, fontWeight = FontWeight.Bold,
                    color = scheme.onSurface, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                Text("مرة واحدة… ثم يعمل بدون إنترنت", fontSize = 15.sp,
                    color = scheme.onSurfaceVariant, textAlign = TextAlign.Center)
                Spacer(Modifier.height(26.dp))
                OutlinedButton(
                    onClick = onImport,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    border = BorderStroke(1.dp, scheme.outline),
                    shape = RoundedCornerShape(27.dp)
                ) { Text("استيراد قاعدة البيانات", fontSize = 16.sp) }
            }
        }
    }
}

@Composable
private fun ImportBusyContent(scheme: androidx.compose.material3.ColorScheme) {
    val t = rememberInfiniteTransition(label = "import")
    val pulse by t.animateFloat(
        initialValue = 0.94f, targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    CircularProgressIndicator(
        modifier = Modifier.size(96.dp).graphicsLayer { scaleX = pulse; scaleY = pulse },
        color = scheme.primary,
        trackColor = scheme.primary.copy(alpha = 0.15f),
        strokeWidth = 7.dp,
        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
    )
}
