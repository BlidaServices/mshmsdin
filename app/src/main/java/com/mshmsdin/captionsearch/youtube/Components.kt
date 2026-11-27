package com.mshmsdin.captionsearch.youtube

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mshmsdin.captionsearch.data.VideoItem
import com.mshmsdin.captionsearch.data.YouTubeIcons
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun durationText(raw: String): String {
    val p = raw.split(":")
    if (p.size != 3) return raw
    val h = p[0].toIntOrNull() ?: 0
    val m = p[1].toIntOrNull() ?: 0
    val s = p[2].toIntOrNull() ?: 0
    return if (h == 0) "%d:%02d".format(m, s) else "%d:%02d:%02d".format(h, m, s)
}

fun secondsOf(raw: String): Int {
    val p = raw.substringBefore('.').split(":")
    if (p.size != 3) return 0
    val h = p[0].toIntOrNull() ?: 0
    val m = p[1].toIntOrNull() ?: 0
    val s = p[2].toIntOrNull() ?: 0
    return h * 3600 + m * 60 + s
}

fun timeText(raw: String): String {
    val p = raw.substringBefore('.').split(":")
    if (p.size != 3) return raw
    val h = p[0].toIntOrNull() ?: 0
    val m = p[1].toIntOrNull() ?: 0
    val s = p[2].toIntOrNull() ?: 0
    return if (h == 0) "%02d:%02d".format(m, s) else "%02d:%02d:%02d".format(h, m, s)
}

@Composable
fun Thumbnail(bytes: ByteArray?, modifier: Modifier) {
    val bitmap by produceState<ImageBitmap?>(initialValue = null, key1 = bytes) {
        value = withContext(Dispatchers.IO) {
            if (bytes == null || bytes.size < 100) null
            else runCatching {
                BitmapFactory.decodeByteArray(
                    bytes, 0, bytes.size,
                    BitmapFactory.Options().apply { inSampleSize = 2 }
                )?.asImageBitmap()
            }.getOrNull()
        }
    }
    val bmp = bitmap
    if (bmp != null) {
        Image(bmp, null, modifier, contentScale = ContentScale.Crop)
    } else {
        Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant), Alignment.Center) {
            Icon(Icons.Default.PlayArrow, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun LazyCover(key: String, load: () -> ByteArray?, modifier: Modifier) {
    val bytes by produceState<ByteArray?>(initialValue = null, key1 = key) {
        value = withContext(Dispatchers.IO) { load() }
    }
    Thumbnail(bytes, modifier)
}

@Composable
fun ChannelAvatar(bytes: ByteArray?, size: Int = 36, onClick: (() -> Unit)? = null) {
    val modifier = Modifier
        .size(size.dp)
        .clip(CircleShape)
        .let { if (onClick != null) it.clickable(onClick = onClick) else it }
    Thumbnail(bytes, modifier)
}

@Composable
fun TypeBadge(item: VideoItem) {
    if (item.live) {
        Surface(
            color = Color.Black.copy(alpha = 0.78f),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier.padding(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(Color(0xFFE02020)))
                Spacer(Modifier.width(4.dp))
                Text("LIVE", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    } else if (item.short) {
        Surface(color = Color(0xFFE02020), shape = CircleShape, modifier = Modifier.padding(4.dp)) {
            Icon(YouTubeIcons.ShortsFilled, null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun DurationBadge(item: VideoItem, modifier: Modifier = Modifier, small: Boolean = false) {
    if (item.live) return
    Surface(color = Color.Black.copy(alpha = 0.78f), shape = RoundedCornerShape(4.dp),
        modifier = modifier.padding(if (small) 4.dp else 6.dp)) {
        Text(durationText(item.duration), color = Color.White, fontSize = if (small) 10.sp else 11.sp,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
    }
}

@Composable
fun VideoCard(item: VideoItem, style: String = "large", onClick: () -> Unit) {
    when (style) {
        "small" -> VideoCardSmall(item, onClick)
        "medium" -> VideoCardMedium(item, onClick)
        else -> VideoCardLarge(item, onClick)
    }
}

@Composable
private fun VideoCardLarge(item: VideoItem, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 10.dp)) {
        Box {
            Thumbnail(item.thumb, Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(10.dp)))
            Box(Modifier.align(Alignment.TopStart)) { TypeBadge(item) }
            Box(Modifier.align(Alignment.BottomEnd)) { DurationBadge(item) }
        }
        Spacer(Modifier.height(9.dp))
        Row {
            ChannelAvatar(item.avatar, 36)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(item.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 2,
                    overflow = TextOverflow.Ellipsis, lineHeight = 20.sp)
                Spacer(Modifier.height(4.dp))
                Text(item.channel, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${item.date} · ${if (item.caption) "كابشن" else "بدون كابشن"}",
                    fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
    }
}

@Composable
private fun VideoCardMedium(item: VideoItem, onClick: () -> Unit) {
    Column(Modifier.clickable(onClick = onClick).padding(4.dp)) {
        Box {
            Thumbnail(item.thumb, Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(8.dp)))
            Box(Modifier.align(Alignment.TopStart)) { TypeBadge(item) }
            Box(Modifier.align(Alignment.BottomEnd)) { DurationBadge(item) }
        }
        Spacer(Modifier.height(6.dp))
        Row {
            ChannelAvatar(item.avatar, 28) { onClick() }
            Spacer(Modifier.width(7.dp))
            Column(Modifier.weight(1f)) {
                Text(item.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 2,
                    overflow = TextOverflow.Ellipsis, lineHeight = 17.sp)
                Text(item.channel, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
    }
}

@Composable
private fun VideoCardSmall(item: VideoItem, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 7.dp)) {
        Box {
            Thumbnail(item.thumb, Modifier.size(150.dp, 84.dp).clip(RoundedCornerShape(7.dp)))
            Box(Modifier.align(Alignment.TopStart)) { TypeBadge(item) }
            Box(Modifier.align(Alignment.BottomEnd)) { DurationBadge(item, small = true) }
        }
        Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f)) {
            Text(item.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 2,
                overflow = TextOverflow.Ellipsis, lineHeight = 18.sp)
            Spacer(Modifier.height(4.dp))
            Text(item.channel, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            Text(item.date, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

@Composable
fun YouTubeLogo(modifier: Modifier = Modifier) {
    Image(
        imageVector = YouTubeIcons.lockup(Color.White),
        contentDescription = "YouTube",
        modifier = modifier.height(20.dp)
    )
}
