package com.mshmsdin.captionsearch.data

data class Chapter(
    val time: String,
    val title: String
)

data class Video(
    val rowId: Long,
    val id: String,
    val title: String,
    val description: String,
    val date: String,
    val time: String,
    val caption: Boolean,
    val summary: String = "",
    val chapters: List<Chapter> = emptyList(),
    val hashtags: List<String> = emptyList(),
    val questions: List<String> = emptyList()
)

data class Cue(
    val rowId: Long,
    val videoRowId: Long,
    val videoId: String,
    val startTime: String,
    val endTime: String,
    val text: String
)

data class CueResult(
    val cue: Cue,
    val video: Video
)

data class VideoResult(
    val video: Video,
    val snippet: String
)

data class SearchResults(
    val cues: List<CueResult> = emptyList(),
    val videos: List<VideoResult> = emptyList(),
    val queryTimeMs: Long = 0L,
    val usedFts: Boolean = false
) {
    val isEmpty: Boolean get() = cues.isEmpty() && videos.isEmpty()
}
