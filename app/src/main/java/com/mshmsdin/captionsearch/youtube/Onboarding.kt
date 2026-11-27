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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.vector.ImageVector
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

private data class DemoResult(
    val title: String,
    val meta: String,
    val coverRes: Int? = null,
    val avatar: ByteArray? = null,
    val icon: ImageVector? = null
)

private data class DemoStep(val query: String, val headline: String, val results: List<DemoResult>)

private fun buildDemos(channels: List<ChannelItem>): List<DemoStep> {
    val ch = { name: String ->
        channels.firstOrNull { it.title.contains(name) }?.avatar
    }
    return listOf(
        DemoStep(
            "الصلاة", "ابحث بدون إنترنت",
            listOf(
                DemoResult("فأول ما يجب على المسلم تعلمه الصلاة", "الشيخ · 00:04:12",
                    coverRes = ChannelArt.covers[0]),
                DemoResult("والصلاة نور، وهي عمود الدين", "الشيخ · 01:12:45",
                    coverRes = ChannelArt.covers[2])
            )
        ),
        DemoStep(
            "النووي", "ابحث داخل الكابشن",
            listOf(
                DemoResult("قال الإمام النووي رحمه الله في شرحه", "كابشن · 00:12:04",
                    coverRes = ChannelArt.covers[1]),
                DemoResult("وكذلك ذكر النووي أن السنة هي الحكم", "كابشن · 00:27:31",
                    coverRes = ChannelArt.covers[3])
            )
        ),
        DemoStep(
            "القوائم", "قوائم ومواضيع منظمة",
            listOf(
                DemoResult("قائمة دروس التوحيد", "12 مقطعًا · قائمة",
                    coverRes = ChannelArt.covers[2]),
                DemoResult("قائمة الردود العلمية", "8 مقاطع · قائمة",
                    coverRes = ChannelArt.covers[4]),
                DemoResult("موضوع الصلاة", "1,204 مقطع · موضوع",
                    coverRes = ChannelArt.covers[0]),
                DemoResult("موضوع العقيدة", "986 مقطع · موضوع",
                    coverRes = ChannelArt.covers[5])
            )
        ),
        DemoStep(
            "القنوات", "قنوات الشيخ",
            listOf(
                DemoResult("محمد بن شمس الدين", "5,021 مقطع",
                    avatar = ch("شمس الدين"), icon = YouTubeIcons.HomeFilled),
                DemoResult("الردود العلمية", "1,617 مقطع",
                    avatar = ch("الردود"), icon = YouTubeIcons.VideoFilled),
                DemoResult("كشف التلبيس", "369 مقطع",
                    avatar = ch("التلبيس"), icon = YouTubeIcons.TopicsFilled),
                DemoResult("بثوث محمد بن شمس الدين", "3,787 مقطع",
                    avatar = ch("بثوث"), icon = YouTubeIcons.LiveFilled)
            )
        )
    )
}

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
    val demos = remember(channels) { buildDemos(channels) }
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
                        PageScaffold(active, demo.headline, sceneDone, scheme) {
                            TypingDemo(active, demo, scheme) { sceneDone = true }
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
            Text(text, fontSize = 24.sp, fontWeight = FontWeight.Bold,
                color = scheme.onPrimaryContainer, textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 10.dp).graphicsLayer {
                    alpha = a.value
                    translationY = (1f - a.value) * 16
                })
        }
    }
}

@Composable
private fun RevealItem(active: Boolean, index: Int, content: @Composable () -> Unit) {
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
            delay(1200)
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
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            demo.results.forEachIndexed { i, result ->
                RevealItem(active = done, index = i) {
                    DemoResultCard(result, demo.query, scheme)
                }
            }
        }
    }
}

@Composable
private fun DemoResultCard(
    result: DemoResult,
    word: String,
    scheme: androidx.compose.material3.ColorScheme
) {
    Surface(shape = RoundedCornerShape(16.dp), color = scheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(9.dp), verticalAlignment = Alignment.CenterVertically) {
            if (result.avatar != null) {
                ChannelAvatar(result.avatar, 52)
            } else if (result.coverRes != null) {
                Image(painterResource(result.coverRes), null, contentScale = ContentScale.Crop,
                    modifier = Modifier.size(116.dp, 66.dp).clip(RoundedCornerShape(11.dp)))
            } else if (result.icon != null) {
                Box(Modifier.size(52.dp).background(scheme.primaryContainer, CircleShape),
                    Alignment.Center) {
                    Icon(result.icon, null, tint = scheme.primary,
                        modifier = Modifier.size(28.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                val i = result.title.indexOf(word)
                val annotated = if (i < 0) AnnotatedString(result.title) else buildAnnotatedString {
                    append(result.title.substring(0, i))
                    withStyle(SpanStyle(color = scheme.onPrimaryContainer,
                        background = scheme.primaryContainer, fontWeight = FontWeight.Bold)) {
                        append(word)
                    }
                    append(result.title.substring(i + word.length))
                }
                Text(annotated, fontSize = 13.sp, color = scheme.onSurface,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(5.dp))
                Text(result.meta, fontSize = 11.sp, color = scheme.onSurfaceVariant, maxLines = 1)
            }
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
