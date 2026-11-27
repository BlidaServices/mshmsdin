package com.mshmsdin.captionsearch.youtube

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mshmsdin.captionsearch.data.CaptionRepository
import com.mshmsdin.captionsearch.data.ChannelItem
import com.mshmsdin.captionsearch.data.YouTubeIcons
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
    onNext: () -> Unit,
    onImport: () -> Unit,
    onStart: () -> Unit
) {
    var channels by remember { mutableStateOf<List<ChannelItem>>(emptyList()) }
    LaunchedEffect(Unit) { channels = withContext(Dispatchers.IO) { repository.channels() } }
    val scheme = MaterialTheme.colorScheme
    Surface(Modifier.fillMaxSize(), color = scheme.background) {
        when {
            importing -> ImportBusy()
            step == 0 -> WelcomePage(channels, onNext, scheme)
            importDone -> ImportDonePage(onStart, scheme)
            else -> ImportIntroPage(onImport, scheme)
        }
    }
}

@Composable
private fun WelcomePage(
    channels: List<ChannelItem>,
    onNext: () -> Unit,
    scheme: androidx.compose.material3.ColorScheme
) {
    val fade = remember { Animatable(0f) }
    LaunchedEffect(Unit) { fade.animateTo(1f, tween(600, easing = FastOutSlowInEasing)) }
    val teasers = if (channels.isEmpty()) fallbackTeasers else channels.map { ch ->
        ChannelTeaser(ch.title, "${ch.count} مقطع", ChannelArt.iconFor(ch.title), ch.avatar)
    }
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 24.dp, vertical = 16.dp)
            .graphicsLayer(alpha = fade.value),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        YouTubeLogo(Modifier.height(26.dp))
        Spacer(Modifier.height(20.dp))
        Text("ابحث في كلام الشيخ لحظيًا", style = MaterialTheme.typography.headlineSmall,
            color = scheme.onSurface, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text("اكتب كلمة فتظهر المقاطع التي قيلت فيها، من الدروس والردود والبثوث.",
            style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant,
            textAlign = TextAlign.Center)
        CoverMosaic()
        Spacer(Modifier.height(12.dp))
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            teasers.forEach { ch ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = scheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ListItem(
                        leadingContent = {
                            if (ch.avatar != null) {
                                ChannelAvatar(ch.avatar, 40)
                            } else {
                                Image(
                                    painter = painterResource(ch.iconRes),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(40.dp).clip(CircleShape)
                                )
                            }
                        },
                        headlineContent = {
                            Text(ch.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                                color = scheme.onSurface, maxLines = 1)
                        },
                        supportingContent = {
                            Text(ch.desc, fontSize = 11.sp, color = scheme.onSurfaceVariant, maxLines = 1)
                        }
                    )
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Button(onClick = onNext, Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = scheme.primary)) {
            Text("التالي", fontSize = 16.sp)
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
                painter = painterResource(ChannelArt.covers[i]),
                contentDescription = null,
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
private fun ImportIntroPage(onImport: () -> Unit, scheme: androidx.compose.material3.ColorScheme) {
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(Modifier.size(96.dp).background(scheme.primaryContainer, CircleShape), Alignment.Center) {
            Icon(YouTubeIcons.VideoFilled, null, tint = scheme.primary, modifier = Modifier.size(46.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text("استيراد قاعدة البيانات", style = MaterialTheme.typography.headlineSmall,
            color = scheme.onSurface, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text("اختر ملف القاعدة مرة واحدة، وسيُحفظ على جهازك ليعمل التطبيق بدون إنترنت.",
            style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant,
            textAlign = TextAlign.Center)
        Spacer(Modifier.height(30.dp))
        Button(onClick = onImport, Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = scheme.primary)) {
            Text("استيراد قاعدة البيانات", fontSize = 15.sp)
        }
    }
}

@Composable
private fun ImportBusy() {
    val scheme = MaterialTheme.colorScheme
    val t = rememberInfiniteTransition(label = "import")
    val pulse by t.animateFloat(
        initialValue = 0.94f, targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            modifier = Modifier.size(84.dp).graphicsLayer { scaleX = pulse; scaleY = pulse },
            color = scheme.primary,
            trackColor = scheme.primary.copy(alpha = 0.15f),
            strokeWidth = 6.dp,
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }
}

@Composable
private fun ImportDonePage(onStart: () -> Unit, scheme: androidx.compose.material3.ColorScheme) {
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(Modifier.size(88.dp).background(scheme.primaryContainer, CircleShape), Alignment.Center) {
            Icon(Icons.Default.Check, null, tint = scheme.primary, modifier = Modifier.size(44.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text("اكتمل الاستيراد", style = MaterialTheme.typography.titleMedium, color = scheme.onSurface)
        Spacer(Modifier.height(28.dp))
        Button(onClick = onStart, Modifier.fillMaxWidth().height(54.dp),
            colors = ButtonDefaults.buttonColors(containerColor = scheme.primary)) {
            Text("البَدأ", fontSize = 17.sp)
        }
    }
}
