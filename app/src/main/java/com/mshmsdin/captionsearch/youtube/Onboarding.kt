package com.mshmsdin.captionsearch.youtube

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mshmsdin.captionsearch.data.CaptionRepository
import com.mshmsdin.captionsearch.data.ChannelItem
import com.mshmsdin.captionsearch.data.YouTubeIcons
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class ChannelTeaser(val title: String, val desc: String, val icon: ImageVector, val avatar: ByteArray? = null)

private val fallbackTeasers = listOf(
    ChannelTeaser("محمد بن شمس الدين", "الدروس والمحاضرات", YouTubeIcons.HomeFilled),
    ChannelTeaser("الردود العلمية", "ردود على الشبهات", YouTubeIcons.VideoFilled),
    ChannelTeaser("كشف التلبيس", "تفنيد الأباطيل", YouTubeIcons.TopicsFilled),
    ChannelTeaser("بثوث محمد بن شمس الدين", "اللقاءات المباشرة", YouTubeIcons.LiveFilled)
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
    var covers by remember { mutableStateOf<List<ByteArray?>>(emptyList()) }
    LaunchedEffect(Unit) {
        channels = withContext(Dispatchers.IO) { repository.channels() }
        covers = withContext(Dispatchers.IO) { repository.home(0, 6, false).map { it.thumb } }
    }
    val scheme = MaterialTheme.colorScheme
    Surface(Modifier.fillMaxSize(), color = scheme.background) {
        when (step) {
            0 -> WelcomePage(channels, covers, onNext, scheme)
            1 -> ImportIntroPage(onImport, scheme)
            else -> ImportProgressPage(importing, importDone, onStart, scheme)
        }
    }
}

@Composable
private fun WelcomePage(
    channels: List<ChannelItem>,
    covers: List<ByteArray?>,
    onNext: () -> Unit,
    scheme: androidx.compose.material3.ColorScheme
) {
    val fade = remember { Animatable(0f) }
    LaunchedEffect(Unit) { fade.animateTo(1f, tween(600, easing = FastOutSlowInEasing)) }
    val teasers = if (channels.isEmpty()) fallbackTeasers else channels.map { ch ->
        ChannelTeaser(ch.title, "${ch.count} مقطع", YouTubeIcons.VideoFilled, ch.avatar)
    }
    Column(
        Modifier.fillMaxSize().padding(24.dp).graphicsLayer(alpha = fade.value),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(28.dp))
        YouTubeLogo(Modifier.height(26.dp))
        Spacer(Modifier.height(28.dp))
        Text(
            "ابحث في كلام الشيخ لحظيًا",
            style = MaterialTheme.typography.headlineSmall,
            color = scheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "اكتب كلمة فتظهر لك المقاطع التي قيلت فيها، من دروس وردود وبثوث الشيخ محمد بن شمس الدين.",
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (covers.isNotEmpty()) {
            Spacer(Modifier.height(22.dp))
            LazyRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(covers.take(5)) { bytes ->
                    Thumbnail(
                        bytes,
                        Modifier.size(132.dp, 76.dp).clip(RoundedCornerShape(12.dp))
                    )
                }
            }
        }
        Spacer(Modifier.height(22.dp))
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            teasers.forEach { ch ->
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = scheme.surface)
                ) {
                    Row(Modifier.padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
                        ChannelAvatar(ch.avatar, 40)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(ch.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                                color = scheme.onSurface, maxLines = 1)
                            Text(ch.desc, fontSize = 11.sp, color = scheme.onSurfaceVariant, maxLines = 1)
                        }
                        Icon(ch.icon, null, tint = scheme.primary, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Button(
            onClick = onNext,
            Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = scheme.primary)
        ) { Text("التالي", fontSize = 16.sp) }
    }
}

@Composable
private fun ImportIntroPage(
    onImport: () -> Unit,
    scheme: androidx.compose.material3.ColorScheme
) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier.size(96.dp).background(scheme.primaryContainer, CircleShape),
            Alignment.Center
        ) {
            Icon(YouTubeIcons.VideoFilled, null, tint = scheme.primary, modifier = Modifier.size(46.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text("استيراد قاعدة البيانات", style = MaterialTheme.typography.headlineSmall,
            color = scheme.onSurface, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(
            "اختر ملف القاعدة مرة واحدة، وسيُحفظ على جهازك ليعمل التطبيق بدون إنترنت.",
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(30.dp))
        Button(onClick = onImport, Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = scheme.primary)) {
            Text("استيراد قاعدة البيانات", fontSize = 15.sp)
        }
    }
}

@Composable
private fun ImportProgressPage(
    importing: Boolean,
    done: Boolean,
    onStart: () -> Unit,
    scheme: androidx.compose.material3.ColorScheme
) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (importing) {
            CircularProgressIndicator(
                modifier = Modifier.size(88.dp),
                color = scheme.primary,
                strokeWidth = 8.dp
            )
        } else {
            Box(
                Modifier.size(88.dp).background(scheme.primaryContainer, CircleShape),
                Alignment.Center
            ) {
                androidx.compose.material3.Icon(
                    androidx.compose.material.icons.Icons.Default.Check, null,
                    tint = scheme.primary, modifier = Modifier.size(44.dp)
                )
            }
            Spacer(Modifier.height(24.dp))
            Text("اكتمل الاستيراد", style = MaterialTheme.typography.titleMedium,
                color = scheme.onSurface)
            Spacer(Modifier.height(28.dp))
            Button(onClick = onStart, Modifier.fillMaxWidth().height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = scheme.primary)) {
                Text("البَدأ", fontSize = 17.sp)
            }
        }
    }
}
