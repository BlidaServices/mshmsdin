package com.mshmsdin.captionsearch.data

data class VideoItem(
    val rowId: Long,
    val id: String,
    val title: String,
    val channel: String,
    val date: String,
    val time: String,
    val duration: String,
    val caption: Boolean,
    val live: Boolean,
    val short: Boolean,
    val thumb: ByteArray?,
    val description: String = "",
    val avatar: ByteArray? = null,
    val publishedAt: String = ""
)

data class TopicItem(
    val id: Int,
    val name: String,
    val parent: String,
    val count: Int,
    val latestDate: String = "",
    val cover: ByteArray? = null
)

data class ChannelItem(
    val id: String,
    val title: String,
    val count: Int,
    val avatar: ByteArray? = null
)

data class ChannelInfo(
    val id: String,
    val title: String,
    val description: String,
    val avatar: ByteArray?,
    val total: Int,
    val captions: Int,
    val noCaptions: Int,
    val shorts: Int,
    val lives: Int,
    val playlists: Int
)

data class PlaylistItem(
    val id: String,
    val title: String,
    val count: Int,
    val cover: ByteArray? = null
)

data class SearchHit(
    val videoRowId: Long,
    val videoId: String,
    val title: String,
    val channel: String,
    val start: String,
    val text: String,
    val thumb: ByteArray?,
    val avatar: ByteArray?
)

data class VideoTarget(
    val item: VideoItem,
    val start: Int,
    val highlight: String? = null
)

data class ChapterItem(
    val start: String,
    val title: String
)

data class SuggestionItem(
    val label: String,
    val kind: String
)

data class FilterState(
    val query: String = "",
    val channelIds: Set<String> = emptySet(),
    val topicId: Int? = null,
    val topicName: String? = null,
    val playlistId: String? = null,
    val caption: Int = -1,
    val kind: String = "all",
    val fromDate: String = "",
    val toDate: String = "",
    val minSeconds: Int? = null,
    val maxSeconds: Int? = null
)
