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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mshmsdin.captionsearch.data.VideoItem
import com.mshmsdin.captionsearch.data.YouTubeIcons
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun displayDate(raw: String): String {
    val clean = raw.substringBefore('T')
    val p = clean.split("-")
    return if (p.size == 3) "${p[2]}/${p[1]}/${p[0].takeLast(2)}" else clean
}

fun dateTimeLabel(date: String, duration: String): String =
    "${displayDate(date)} ${durationText(duration)}"

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

private const val CC_PLATE =
    "M464 0 L48 0 C21.5 0 0 21.5 0 48 L0 336 C0 362.5 21.5 384 48 384 L464 384 " +
        "C490.5 384 512 362.5 512 336 L512 48 C512 21.5 490.5 0 464 0 Z"

private const val CC_GLYPH_RIGHT =
    "M418.55 222.41 C421.35 219.91 425.65 220.31 427.75 223.31 L447.25 251.01 " +
        "C448.95 253.41 448.75 256.61 446.75 258.71 C393.25 315.61 274.05 290.81 274.05 190.81 " +
        "C274.05 93.51 395.75 71.31 446.55 120.71 C448.65 122.71 449.05 123.91 447.55 126.41 " +
        "L430.05 156.91 C428.15 160.01 423.85 160.91 420.95 158.61 " +
        "C380.15 126.61 326.35 143.71 326.35 189.81 C326.35 237.81 377.35 260.31 418.55 222.41 " +
        "L418.55 222.41 Z"

private const val CC_GLYPH_LEFT =
    "M208.15 222.41 C210.95 219.91 215.25 220.31 217.35 223.31 L236.85 251.01 " +
        "C238.55 253.41 238.35 256.61 236.35 258.71 C182.75 315.51 63.55 290.81 63.55 190.81 " +
        "C63.55 93.51 185.25 71.31 236.05 120.71 C238.15 122.71 238.55 123.91 237.05 126.41 " +
        "L219.55 156.91 C217.65 160.01 213.35 160.91 210.45 158.61 " +
        "C169.65 126.61 115.85 143.71 115.85 189.81 C115.95 237.81 166.95 260.31 208.15 222.41 Z"

private fun ccBadgeVector(plate: Color, glyph: Color): ImageVector =
    ImageVector.Builder(
        name = "CcBadge",
        defaultWidth = 512.dp,
        defaultHeight = 384.dp,
        viewportWidth = 512f,
        viewportHeight = 384f
    ).apply {
        addPath(pathData = addPathNodes(CC_PLATE), fill = SolidColor(plate))
        addPath(pathData = addPathNodes(CC_GLYPH_RIGHT), fill = SolidColor(glyph))
        addPath(pathData = addPathNodes(CC_GLYPH_LEFT), fill = SolidColor(glyph))
    }.build()

private val CcBadgeOn by lazy { ccBadgeVector(Color.White, Color.Black) }
private val CcBadgeOff by lazy {
    ccBadgeVector(Color(0xFF8E8E8E), Color(0xFFEDEDED))
}

@Composable
fun CaptionBadge(hasCaptions: Boolean) {
    Image(
        imageVector = if (hasCaptions) CcBadgeOn else CcBadgeOff,
        contentDescription = if (hasCaptions) "كابشن متوفر" else null,
        contentScale = ContentScale.Fit,
        modifier = Modifier.size(30.dp, 23.dp)
    )
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
            Box(Modifier.align(Alignment.TopEnd).padding(6.dp)) { CaptionBadge(item.caption) }
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
                Text(dateTimeLabel(item.date, item.duration), fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
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
            Box(Modifier.align(Alignment.TopEnd).padding(6.dp)) { CaptionBadge(item.caption) }
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
            Box(Modifier.align(Alignment.TopEnd).padding(6.dp)) { CaptionBadge(item.caption) }
            Box(Modifier.align(Alignment.BottomEnd)) { DurationBadge(item, small = true) }
        }
        Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f)) {
            Text(item.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 2,
                overflow = TextOverflow.Ellipsis, lineHeight = 18.sp)
            Spacer(Modifier.height(4.dp))
            Text(item.channel, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            Text(dateTimeLabel(item.date, item.duration), fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
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
