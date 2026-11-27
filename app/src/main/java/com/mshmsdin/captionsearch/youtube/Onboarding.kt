package com.mshmsdin.captionsearch.youtube

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.layout.wrapContentWidth
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mshmsdin.captionsearch.data.CaptionRepository
import com.mshmsdin.captionsearch.data.ChannelItem
import com.mshmsdin.captionsearch.data.YouTubeIcons
import kotlinx.coroutines.Dispatchers
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
            Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            YouTubeLogo(Modifier.height(26.dp))
            Spacer(Modifier.height(10.dp))
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                userScrollEnabled = !importing,
                key = { it }
            ) { page ->
                PageEntrance(active = pagerState.currentPage == page) {
                    when (page) {
                        0 -> OnboardContent(
                            tag = "بدون إنترنت · بحث فوري",
                            title = "ابحث في كلام الشيخ أينما كنت",
                            body = "بعد استيراد واحد، يعمل التطبيق بالكامل دون اتصال: اكتب كلمة فتظهر كل اللحظات التي قيلت فيها، لا عناوين فقط.",
                            note = "2,087,985 كابشن · 10,794 مقطع",
                            scheme = scheme
                        ) { SearchVisual(scheme) }
                        1 -> OnboardContent(
                            tag = "سؤال وجواب",
                            title = "من السؤال إلى الجواب",
                            body = "آلاف الأسئلة مربوطًا بمكان إجابتها داخل المقطع: اختر السؤال فيقفز المشغّل إلى الثانية التي يبدأ فيها الجواب.",
                            note = "102,484 مدخلًا مفهرسًا",
                            scheme = scheme
                        ) { QnaVisual(scheme) }
                        2 -> OnboardContent(
                            tag = "مشغّل ذكي",
                            title = "الكابشن والفصول",
                            body = "شغّل المقطع، تنقّل بين الفصول، واضغط أي سطر من الكابشن لينتقل الفيديو إلى تلك اللحظة. شارة CC تعني أن المقطع مفهرس.",
                            note = null,
                            scheme = scheme
                        ) { PlayerVisual(scheme) }
                        3 -> OnboardContent(
                            tag = "تصفح مخصّص",
                            title = "كل المحتوى بين يديك",
                            body = "الرئيسية وشورتس والمواضيع والقنوات الأربع، مع قوائمها ومواضيعها. ومن الإعدادات: حجم البطاقة وستة ألوان.",
                            note = "4 قنوات · 9,765 موضوعًا · 145 قائمة",
                            scheme = scheme
                        ) { BrowseVisual(channels, scheme) }
                        else -> ImportPage(importing, importDone, onImport, scheme)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            PageDots(pagerState.currentPage, ONBOARD_PAGES, scheme)
            Spacer(Modifier.height(10.dp))
            val onImportPage = pagerState.currentPage == IMPORT_PAGE
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (pagerState.currentPage > 0 && !importing) {
                    OutlinedButton(
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                        },
                        modifier = Modifier.weight(1f).height(52.dp),
                        border = BorderStroke(1.dp, scheme.outline),
                        shape = RoundedCornerShape(26.dp)
                    ) { Text("السابق", fontSize = 16.sp) }
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
                    modifier = Modifier.weight(1f).height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = scheme.primary,
                        contentColor = scheme.onPrimary,
                        disabledContainerColor = scheme.surfaceVariant,
                        disabledContentColor = scheme.onSurfaceVariant.copy(alpha = 0.55f)
                    )
                ) { Text(if (onImportPage) "البَدأ" else "التالي", fontSize = 16.sp) }
            }
        }
    }
}

@Composable
private fun PageEntrance(active: Boolean, content: @Composable () -> Unit) {
    val reveal = remember { androidx.compose.animation.core.Animatable(if (active) 1f else 0f) }
    LaunchedEffect(active) {
        if (active) reveal.animateTo(1f, tween(520, easing = FastOutSlowInEasing))
        else reveal.snapTo(0f)
    }
    Column(
        Modifier.fillMaxSize().graphicsLayer(
            alpha = reveal.value,
            translationY = (1f - reveal.value) * 28
        )
    ) { content() }
}

@Composable
private fun OnboardContent(
    tag: String,
    title: String,
    body: String,
    note: String?,
    scheme: androidx.compose.material3.ColorScheme,
    visual: @Composable () -> Unit
) {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        visual()
        Spacer(Modifier.height(18.dp))
        Surface(
            shape = RoundedCornerShape(50),
            color = scheme.primaryContainer,
            modifier = Modifier.wrapContentWidth()
        ) {
            Text(tag, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                color = scheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall,
            color = scheme.onSurface, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant, textAlign = TextAlign.Center)
        if (note != null) {
            Spacer(Modifier.height(14.dp))
            Text(note, fontSize = 12.sp, color = scheme.primary, textAlign = TextAlign.Center)
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
private fun SearchVisual(scheme: androidx.compose.material3.ColorScheme) {
    Column(Modifier.fillMaxWidth()) {
        Surface(shape = RoundedCornerShape(26.dp), color = scheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Search, null, tint = scheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text("الصلاة", color = scheme.onSurface, fontSize = 14.sp)
                Spacer(Modifier.weight(1f))
                Box(Modifier.width(2.dp).height(16.dp).background(scheme.primary))
            }
        }
        Spacer(Modifier.height(10.dp))
        listOf(0, 2).forEach { idx ->
            Row(Modifier.fillMaxWidth().padding(vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(ChannelArt.covers[idx]), null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(96.dp, 54.dp).clip(RoundedCornerShape(8.dp)))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("فأول ما يجب على المسلم تعلمه الصلاة", fontSize = 12.sp,
                        color = scheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("محمد بن شمس الدين · 00:04:12", fontSize = 10.sp,
                        color = scheme.onSurfaceVariant, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun QnaVisual(scheme: androidx.compose.material3.ColorScheme) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("ما حكم صلاة الجماعة؟" to "00:14:22",
            "هل يُشترط العلم قبل العمل؟" to "00:02:07").forEach { (q, t) ->
            Surface(shape = RoundedCornerShape(14.dp), color = scheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PlayArrow, null, tint = scheme.primary,
                        modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(q, fontSize = 12.sp, color = scheme.onSurface, maxLines = 2)
                        Text("ينتقل إلى $t", fontSize = 10.sp, color = scheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerVisual(scheme: androidx.compose.material3.ColorScheme) {
    Column(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(12.dp))) {
            Image(painterResource(ChannelArt.covers[1]), null,
                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            Box(Modifier.align(Alignment.Center).size(48.dp)
                .background(Color.Black.copy(alpha = 0.5f), CircleShape), Alignment.Center) {
                Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(30.dp))
            }
            Surface(color = Color.Black.copy(alpha = 0.6f), shape = RoundedCornerShape(4.dp),
                modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp)) {
                Text("CC", fontSize = 9.sp, color = Color.White,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        listOf("00:00:12" to "الحمد لله والصلاة والسلام على رسول الله…",
            "00:00:31" to "أما بعد، فإن أصدق الحديث كتاب الله…",
            "00:01:04" to "وخير الهدي هدي محمد صلى الله عليه وسلم…").forEach { (t, s) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                Text(t, fontSize = 10.sp, color = scheme.primary, modifier = Modifier.width(52.dp))
                Text(s, fontSize = 11.sp, color = scheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun BrowseVisual(channels: List<ChannelItem>, scheme: androidx.compose.material3.ColorScheme) {
    val teasers = if (channels.isEmpty()) fallbackTeasers else channels.map {
        ChannelTeaser(it.title, "${it.count} مقطع", ChannelArt.iconFor(it.title), it.avatar)
    }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        CoverMosaic()
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            teasers.take(4).forEach { ch ->
                if (ch.avatar != null) ChannelAvatar(ch.avatar, 38)
                else Image(painterResource(ch.iconRes), null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(38.dp).clip(CircleShape))
            }
        }
    }
}

@Composable
private fun CoverMosaic(modifier: Modifier = Modifier) {
    val cards = listOf(
        Triple(0, -11f, -60), Triple(1, -6f, -32),
        Triple(3, 6f, 32), Triple(4, 11f, 60), Triple(2, 0f, 0)
    )
    Box(modifier.fillMaxWidth().height(132.dp), contentAlignment = Alignment.Center) {
        cards.forEach { (i, angle, dx) ->
            val w = if (i == 2) 152.dp else 120.dp
            Image(
                painterResource(ChannelArt.covers[i]), null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .offset(x = dx.dp, y = (if (i == 2) 0 else 12).dp)
                    .graphicsLayer { rotationZ = angle }
                    .shadow(if (i == 2) 16.dp else 8.dp, RoundedCornerShape(14.dp))
                    .clip(RoundedCornerShape(14.dp))
                    .size(w, w * 9f / 16f)
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
                Text("اكتمل الاستيراد", style = MaterialTheme.typography.titleLarge,
                    color = scheme.onSurface, textAlign = TextAlign.Center)
            }
            else -> {
                Box(Modifier.size(96.dp).background(scheme.primaryContainer, CircleShape),
                    Alignment.Center) {
                    Icon(YouTubeIcons.VideoFilled, null, tint = scheme.primary,
                        modifier = Modifier.size(46.dp))
                }
                Spacer(Modifier.height(22.dp))
                Text("استيراد قاعدة البيانات", style = MaterialTheme.typography.headlineSmall,
                    color = scheme.onSurface, textAlign = TextAlign.Center)
                Spacer(Modifier.height(10.dp))
                Text("اختر ملف القاعدة مرة واحدة، وسيُحفظ على جهازك ليعمل التطبيق بدون إنترنت.",
                    style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant,
                    textAlign = TextAlign.Center)
                Spacer(Modifier.height(24.dp))
                OutlinedButton(
                    onClick = onImport,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    border = BorderStroke(1.dp, scheme.outline),
                    shape = RoundedCornerShape(26.dp)
                ) { Text("استيراد قاعدة البيانات", fontSize = 15.sp) }
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
        modifier = Modifier.size(84.dp).graphicsLayer { scaleX = pulse; scaleY = pulse },
        color = scheme.primary,
        trackColor = scheme.primary.copy(alpha = 0.15f),
        strokeWidth = 6.dp,
        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
    )
}
