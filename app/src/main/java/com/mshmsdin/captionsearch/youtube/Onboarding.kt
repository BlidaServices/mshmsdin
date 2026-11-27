package com.mshmsdin.captionsearch.youtube

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mshmsdin.captionsearch.data.YouTubeIcons

private data class ChannelTeaser(val title: String, val desc: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val teasers = listOf(
    ChannelTeaser("محمد بن شمس الدين", "الدروس والمحاضرات والخطب", YouTubeIcons.HomeFilled),
    ChannelTeaser("الردود العلمية", "ردود على الشبهات والاعتراضات", YouTubeIcons.VideoFilled),
    ChannelTeaser("كشف التلبيس", "تفنيد الأباطيل والدعاوى", YouTubeIcons.TopicsFilled),
    ChannelTeaser("البثوث المباشرة", "اللقاءات والأسئلة المفتوحة", YouTubeIcons.LiveFilled)
)

@Composable
fun OnboardingScreen(
    step: Int,
    importing: Boolean,
    importDone: Boolean,
    onNext: () -> Unit,
    onImport: () -> Unit,
    onStart: () -> Unit
) {
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(step) {
        reveal.snapTo(0f)
        reveal.animateTo(1f, tween(750, easing = FastOutSlowInEasing))
    }
    val scheme = MaterialTheme.colorScheme
    Surface(Modifier.fillMaxSize(), color = scheme.background) {
        Box(
            Modifier.fillMaxSize().background(Brush.linearGradient(
                listOf(scheme.background, scheme.primaryContainer.copy(alpha = 0.45f), scheme.background)
            ))
        ) {
            when (step) {
                0 -> WelcomePage(reveal.value, onNext, scheme.primary, scheme)
                1 -> ImportIntroPage(reveal.value, onImport, scheme)
                else -> ImportProgressPage(reveal.value, importing, importDone, onStart, scheme)
            }
        }
    }
}

@Composable
private fun WelcomePage(
    p: Float,
    onNext: () -> Unit,
    accent: Color,
    scheme: androidx.compose.material3.ColorScheme
) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(Modifier.height(24.dp))
        Image(
            YouTubeIcons.lockup(Color.White),
            contentDescription = "YouTube",
            modifier = Modifier.height(34.dp).graphicsLayer(alpha = p, translationY = (1f - p) * 40)
        )
        Text(
            "مرحبًا بك",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.graphicsLayer(alpha = p, translationY = (1f - p) * 30)
        )
        Text(
            "ابحث في كلام الشيخ محمد بن شمس الدين لحظيًا — في الدروس، الردود، كشف التلبيس، والبثوث. اكتب كلمة واقرأ المقطع الذي قيلت فيه مباشرة.",
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp).graphicsLayer(alpha = p)
        )
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            teasers.forEachIndexed { i, ch ->
                val local = (p - i * 0.14f).coerceIn(0f, 1f)
                Card(
                    Modifier.fillMaxWidth().graphicsLayer(
                        alpha = local,
                        translationX = (1f - local) * (if (i % 2 == 0) -60f else 60f)
                    ),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = scheme.surface.copy(alpha = 0.92f))
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(42.dp).background(accent.copy(alpha = 0.22f), CircleShape),
                            Alignment.Center
                        ) { Icon(ch.icon, null, tint = accent, modifier = Modifier.size(22.dp)) }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(ch.title, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                                fontSize = 14.sp, color = scheme.onSurface, maxLines = 1)
                            Text(ch.desc, fontSize = 11.sp, color = scheme.onSurfaceVariant, maxLines = 1)
                        }
                    }
                }
            }
        }
        Button(
            onClick = onNext,
            Modifier.fillMaxWidth().height(52.dp).graphicsLayer(alpha = p),
            colors = ButtonDefaults.buttonColors(containerColor = accent)
        ) { Text("التالي", fontSize = 16.sp) }
    }
}

@Composable
private fun ImportIntroPage(
    p: Float,
    onImport: () -> Unit,
    scheme: androidx.compose.material3.ColorScheme
) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier.size(120.dp).background(scheme.primary.copy(alpha = 0.16f), CircleShape)
                .graphicsLayer(alpha = p, scaleX = 0.8f + p * 0.2f, scaleY = 0.8f + p * 0.2f),
            Alignment.Center
        ) {
            Icon(YouTubeIcons.VideoFilled, null, tint = scheme.primary, modifier = Modifier.size(56.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text("القاعدة جاهزة للاستيراد", style = MaterialTheme.typography.headlineSmall,
            color = Color.White, textAlign = TextAlign.Center,
            modifier = Modifier.graphicsLayer(alpha = p))
        Spacer(Modifier.height(10.dp))
        Text(
            "اختر ملف قاعدة البيانات مرة واحدة ليتم حفظه على جهازك، ويعمل التطبيق بعدها بدون إنترنت.",
            style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant,
            textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(32.dp))
        Button(onClick = onImport, Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = scheme.primary)) {
            Text("استيراد قاعدة البيانات", fontSize = 15.sp)
        }
    }
}

@Composable
private fun ImportProgressPage(
    p: Float,
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
                modifier = Modifier.size(84.dp).graphicsLayer(alpha = p),
                color = scheme.primary,
                strokeWidth = 7.dp
            )
            Spacer(Modifier.height(24.dp))
            Text("جارٍ تجهيز القاعدة...", style = MaterialTheme.typography.titleMedium,
                color = Color.White)
        } else {
            Box(
                Modifier.size(96.dp).background(scheme.primary.copy(alpha = 0.18f), CircleShape),
                Alignment.Center
            ) {
                Icon(Icons.Default.Check, null, tint = scheme.primary, modifier = Modifier.size(48.dp))
            }
            Spacer(Modifier.height(20.dp))
            Text("تم بنجاح", style = MaterialTheme.typography.headlineSmall, color = Color.White)
            Spacer(Modifier.height(28.dp))
            Button(
                onClick = onStart,
                Modifier.fillMaxWidth().height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = scheme.primary)
            ) { Text("البَدأ", fontSize = 17.sp) }
        }
    }
}
