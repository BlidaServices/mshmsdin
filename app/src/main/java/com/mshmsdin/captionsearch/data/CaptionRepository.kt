package com.mshmsdin.captionsearch.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Statement
import java.util.Locale
import java.util.zip.GZIPInputStream
import java.util.zip.ZipInputStream

class CaptionRepository(private val context: Context) {

    private var conn: Connection? = null

    var dbFileName: String? = null
        private set

    var hasCues: Boolean = false
        private set

    var ftsSupported: Boolean = false
        private set

    var ftsProbeError: String? = null
        private set

    private var hasThumbImage: Boolean = false
    private var hasChannelAvatar: Boolean = false
    private var hasTextN: Boolean = false

    val isLoaded: Boolean get() = conn != null
    var openError: String? = null
        private set

    val dbSizeBytes: Long
        get() = try {
            File(context.filesDir, DB_NAME).length()
        } catch (t: Throwable) { 0L }

    fun openExisting(): Boolean {
        val file = File(context.filesDir, DB_NAME)
        if (!file.exists()) return false
        return open(file)
    }

    fun importFrom(uri: Uri): String? {
        val tmp = File(context.filesDir, "import.tmp")
        val dest = File(context.filesDir, DB_NAME)

        return try {
            if (tmp.exists()) tmp.delete()

            val input = context.contentResolver.openInputStream(uri)
                ?: return "عذرًا، تعذر فتح الملف المختار"

            val extracted = FileOutputStream(tmp).use { out -> extractDatabase(input, uri, out) }
            if (!extracted) {
                tmp.delete()
                return "الملف المضغوط لا يحتوي على قاعدة بيانات"
            }

            if (!looksLikeDatabase(tmp)) {
                tmp.delete()
                return "الملف المختار ليس قاعدة بيانات SQLite صالحة"
            }

            close()

            if (dest.exists()) dest.delete()
            if (!tmp.renameTo(dest)) {
                tmp.copyTo(dest, overwrite = true)
                tmp.delete()
            }

            if (!open(dest)) {
                dest.delete()
                return "فشل فتح قاعدة البيانات: ${openError ?: "غير معروف"}"
            }
            null
        } catch (t: Throwable) {
            try { tmp.delete() } catch (_: Throwable) {}
            t.localizedMessage ?: "حدث خطأ غير متوقع أثناء استيراد الملف"
        }
    }

    private fun displayName(uri: Uri): String? {
        var name: String? = null
        try {
            context.contentResolver.query(
                uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null
            )?.use { c ->
                if (c.moveToFirst()) {
                    val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (i >= 0) name = c.getString(i)
                }
            }
        } catch (_: Throwable) {}
        return name ?: uri.lastPathSegment
    }

    private fun isDbName(name: String): Boolean {
        val n = name.lowercase(Locale.ROOT)
        return n.endsWith(".db") || n.endsWith(".sqlite") || n.endsWith(".sqlite3")
    }

    private fun copyRaw(input: InputStream, out: OutputStream) {
        val buffer = ByteArray(256 * 1024)
        var read: Int
        while (input.read(buffer).also { read = it } >= 0) {
            out.write(buffer, 0, read)
        }
    }

    private fun copyExactly(input: InputStream, out: OutputStream, count: Long) {
        var left = count
        val buffer = ByteArray(256 * 1024)
        while (left > 0) {
            val want = minOf(left.toInt(), buffer.size)
            val read = input.read(buffer, 0, want)
            if (read < 0) break
            out.write(buffer, 0, read)
            left -= read
        }
    }

    private fun skipExactly(input: InputStream, count: Long) {
        var left = count
        val buffer = ByteArray(8192)
        while (left > 0) {
            val want = minOf(left.toInt(), buffer.size)
            val read = input.read(buffer, 0, want)
            if (read < 0) break
            left -= read
        }
    }

    private fun extractTar(input: InputStream, out: OutputStream): Boolean {
        val header = ByteArray(512)
        while (true) {
            var off = 0
            while (off < 512) {
                val read = input.read(header, off, 512 - off)
                if (read < 0) return false
                off += read
            }
            if (header.all { it == 0.toByte() }) return false

            val name = String(header, 0, 100, Charsets.US_ASCII)
                .substringBefore('\u0000').trim()
            val sizeText = String(header, 124, 12, Charsets.US_ASCII)
                .trim('\u0000', ' ').trim()
            val size = if (sizeText.isEmpty()) 0L else sizeText.toLong(8)
            val type = header[156]
            val isFile = type == '0'.code.toByte() || type == 0.toByte() || type == '7'.code.toByte()

            if (isFile && isDbName(name)) {
                copyExactly(input, out, size)
                out.flush()
                return true
            }

            skipExactly(input, size)
            val padding = (512 - (size % 512)) % 512
            if (padding > 0) skipExactly(input, padding)
        }
    }

    private fun extractDatabase(input: InputStream, uri: Uri, out: OutputStream): Boolean {
        val name = (displayName(uri) ?: "").lowercase(Locale.ROOT)
        val buffered = BufferedInputStream(input)
        return when {
            name.endsWith(".zip") -> ZipInputStream(buffered).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        copyRaw(zip, out)
                        out.flush()
                        return true
                    }
                    entry = zip.nextEntry
                }
                false
            }
            name.endsWith(".tar.gz") || name.endsWith(".tgz") ->
                GZIPInputStream(buffered).use { extractTar(it, out) }
            name.endsWith(".gz") ->
                GZIPInputStream(buffered).use { copyRaw(it, out); out.flush(); true }
            name.endsWith(".tar") -> extractTar(buffered, out)
            else -> { copyRaw(buffered, out); out.flush(); true }
        }
    }

    private fun looksLikeDatabase(file: File): Boolean {
        if (!file.exists() || file.length() < 100) return false
        return try {
            DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}").use { c ->
                c.createStatement().use { s ->
                    s.executeQuery(
                        "SELECT count(*) FROM sqlite_master WHERE type='table' AND name='videos'"
                    ).use { rs -> rs.next() && rs.getInt(1) == 1 }
                }
            }
        } catch (t: Throwable) {
            false
        }
    }

    private fun open(file: File): Boolean {
        close()
        return try {
            val c = DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}")
            conn = c
            dbFileName = file.name
            hasCues = tableExists(c, "cues")
            hasTextN = tableExists(c, "cues_fts") && columnExists(c, "cues_fts", "text_n")
            hasThumbImage = columnExists(c, "videos", "thumb_image")
            hasChannelAvatar = columnExists(c, "channels", "avatar")
            runCatching {
                c.createStatement().use { s ->
                    s.execute("PRAGMA mmap_size=536870912")
                    s.execute("PRAGMA cache_size=-65536")
                    s.execute("PRAGMA temp_store=MEMORY")
                }
            }
            topicsCache = null
            channelFeedCache.clear()
            feedCache.clear()
            ftsSupported = probeFts(c)
            openError = null
            true
        } catch (t: Throwable) {
            conn = null
            openError = t.localizedMessage ?: t.javaClass.simpleName
            false
        }
    }

    fun close() {
        try {
            conn?.close()
        } catch (_: Throwable) {}
        conn = null
        dbFileName = null
        hasCues = false
        ftsSupported = false
        ftsProbeError = null
        topicsCache = null
        channelFeedCache.clear()
        feedCache.clear()
    }

    private fun tableExists(c: Connection, name: String): Boolean {
        return try {
            c.createStatement().use { s ->
                s.executeQuery(
                    "SELECT count(*) FROM sqlite_master WHERE type='table' AND name='$name'"
                ).use { rs -> rs.next() && rs.getInt(1) == 1 }
            }
        } catch (t: Throwable) {
            false
        }
    }

    private fun columnExists(c: Connection, table: String, column: String): Boolean {
        return try {
            c.createStatement().use { s ->
                s.executeQuery("PRAGMA table_info($table)").use { rs ->
                    while (rs.next()) {
                        if (rs.getString(2) == column) return true
                    }
                }
            }
            false
        } catch (t: Throwable) {
            false
        }
    }

    private fun probeFts(c: Connection): Boolean {
        val target = when {
            tableExists(c, "cues_fts") -> "cues_fts"
            tableExists(c, "videos_fts") -> "videos_fts"
            else -> null
        } ?: run {
            ftsProbeError = "لا توجد جداول FTS في القاعدة"
            return false
        }
        return try {
            c.createStatement().use { s ->
                s.executeQuery("SELECT 1 FROM $target WHERE $target MATCH 'test' LIMIT 1").use { it.next() }
            }
            ftsProbeError = null
            true
        } catch (t: Throwable) {
            ftsProbeError = t.localizedMessage ?: t.javaClass.simpleName
            false
        }
    }

    fun search(rawQuery: String, limit: Int = 80): SearchResults {
        val c = conn ?: return SearchResults()
        val query = rawQuery.trim()
        if (query.isEmpty()) return SearchResults()

        val start = System.currentTimeMillis()
        val usedFts = ftsSupported
        val cueResults = if (hasCues) searchCues(c, query, limit) else emptyList()
        val videoResults = searchVideos(c, query, limit)
        val elapsed = System.currentTimeMillis() - start
        return SearchResults(cueResults, videoResults, elapsed, usedFts)
    }

    private fun <T> query(c: Connection, sql: String, args: Array<String>, read: (ResultSet) -> T): T {
        val ps = c.prepareStatement(sql)
        try {
            args.forEachIndexed { i, a -> ps.setString(i + 1, a) }
            ps.executeQuery().use { rs -> return read(rs) }
        } finally {
            try { ps.close() } catch (_: Throwable) {}
        }
    }

    private fun searchCues(c: Connection, query: String, limit: Int): List<CueResult> {
        val projection = """c.rowid AS cue_row, c.video_rowid, c.video_id,
            c.start_time, c.end_time, c.text,
            v.rowid AS v_row, v.id, v.title, v.description, v.date, v.time, v.caption,
            v.chapters_json, v.chapters"""

        if (ftsSupported) {
            val sql = """SELECT $projection
                FROM cues_fts JOIN cues c ON c.rowid = cues_fts.rowid
                JOIN videos v ON v.rowid = c.video_rowid
                WHERE cues_fts MATCH ? LIMIT ?"""
            val rows = runCatching {
                query(c, sql, arrayOf(toMatch(query), limit.toString())) { rs -> readCueResults(rs) }
            }.getOrNull()?.sortedBy { it.cue.rowId }
            if (rows != null) return rows
            ftsSupported = false
        }

        val sql = """SELECT $projection
            FROM cues c JOIN videos v ON v.rowid = c.video_rowid
            WHERE c.text LIKE ? OR v.title LIKE ? OR v.description LIKE ?
            ORDER BY c.rowid LIMIT ?"""
        val like = "%$query%"
        return runCatching {
            query(c, sql, arrayOf(like, like, like, limit.toString())) { rs -> readCueResults(rs) }
        }.getOrDefault(emptyList())
    }

    private fun searchVideos(c: Connection, query: String, limit: Int): List<VideoResult> {
        if (ftsSupported) {
            val sql = """SELECT v.rowid AS v_row, v.id, v.title, v.description, v.date, v.time, v.caption,
                v.chapters_json, v.chapters
                FROM videos_fts JOIN videos v ON v.rowid = videos_fts.rowid
                WHERE videos_fts MATCH ? LIMIT ?"""
            val rows = runCatching {
                query(c, sql, arrayOf(toMatch(query), limit.toString())) { rs -> readVideoResults(rs) }
            }.getOrNull()?.sortedBy { it.video.rowId }
            if (rows != null) return rows
            ftsSupported = false
        }

        val sql = """SELECT rowid AS v_row, id, title, description, date, time, caption,
            chapters_json, chapters
            FROM videos
            WHERE title LIKE ? OR description LIKE ?
            ORDER BY rowid LIMIT ?"""
        val like = "%$query%"
        return runCatching {
            query(c, sql, arrayOf(like, like, limit.toString())) { rs -> readVideoResults(rs) }
        }.getOrDefault(emptyList())
    }

    fun cuesOf(videoRowId: Long): List<Cue> {
        val c = conn ?: return emptyList()
        if (!hasCues) return emptyList()
        return runCatching {
            query(
                c,
                """SELECT rowid, video_rowid, video_id, start_time, end_time, text
                   FROM cues WHERE video_rowid = ? ORDER BY rowid""",
                arrayOf(videoRowId.toString())
            ) { rs ->
                buildList {
                    while (rs.next()) {
                        add(
                            Cue(
                                rowId = rs.getLong(1),
                                videoRowId = rs.getLong(2),
                                videoId = rs.getString(3) ?: "",
                                startTime = rs.getString(4) ?: "",
                                endTime = rs.getString(5) ?: "",
                                text = rs.getString(6) ?: ""
                            )
                        )
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    fun video(rowId: Long): Video? {
        val c = conn ?: return null
        return runCatching {
            query(
                c,
                """SELECT rowid, id, title, description, date, time, caption,
                   chapters_json, chapters
                   FROM videos WHERE rowid = ?""",
                arrayOf(rowId.toString())
            ) { rs -> if (rs.next()) readVideoRow(rs) else null }
        }.getOrNull()
    }

    fun stats(): Pair<Int, Long> {
        val c = conn ?: return 0 to 0L
        return try {
            val videos = c.createStatement().use { s ->
                s.executeQuery("SELECT count(*) FROM videos").use { rs ->
                    if (rs.next()) rs.getInt(1) else 0
                }
            }
            val cues = if (tableExists(c, "cues")) {
                c.createStatement().use { s ->
                    s.executeQuery("SELECT max(rowid) FROM cues").use { rs ->
                        if (rs.next() && !rs.wasNull()) rs.getLong(1) else 0L
                    }
                }
            } else 0L
            videos to cues
        } catch (t: Throwable) {
            0 to 0L
        }
    }

    private fun readCueResults(rs: ResultSet): List<CueResult> {
        return buildList {
            while (rs.next()) {
                val video = Video(
                    rowId = rs.getLong(7),
                    id = rs.getString(8) ?: "",
                    title = rs.getString(9) ?: "",
                    description = rs.getString(10) ?: "",
                    date = rs.getString(11) ?: "",
                    time = rs.getString(12) ?: "",
                    caption = rs.getInt(13) == 1,
                    summary = "",
                    chapters = parseChapters(chapterSource(rs.getString(14), rs.getString(15))),
                    hashtags = emptyList(),
                    questions = emptyList()
                )
                val cue = Cue(
                    rowId = rs.getLong(1),
                    videoRowId = rs.getLong(2),
                    videoId = rs.getString(3) ?: "",
                    startTime = rs.getString(4) ?: "",
                    endTime = rs.getString(5) ?: "",
                    text = rs.getString(6) ?: ""
                )
                add(CueResult(cue, video))
            }
        }
    }

    private fun readVideoResults(rs: ResultSet): List<VideoResult> {
        return buildList {
            while (rs.next()) {
                add(VideoResult(readVideoRow(rs), ""))
            }
        }
    }

    private fun chapterSource(json: String?, text: String?): String =
        if (!text.isNullOrBlank()) text else (json ?: "")

    private fun readVideoRow(rs: ResultSet): Video {
        return Video(
            rowId = rs.getLong(1),
            id = rs.getString(2) ?: "",
            title = rs.getString(3) ?: "",
            description = rs.getString(4) ?: "",
            date = rs.getString(5) ?: "",
            time = rs.getString(6) ?: "",
            caption = rs.getInt(7) == 1,
            summary = "",
            chapters = parseChapters(chapterSource(rs.getString(8), rs.getString(9))),
            hashtags = emptyList(),
            questions = emptyList()
        )
    }

    private fun parseChapters(raw: String?): List<com.mshmsdin.captionsearch.data.Chapter> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val arr = org.json.JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    add(com.mshmsdin.captionsearch.data.Chapter(
                        time = o.optString("time", ""),
                        title = o.optString("title", "")
                    ))
                }
            }
        } catch (t: Throwable) {
            emptyList()
        }
    }

    private fun parseStringList(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val arr = org.json.JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val v = arr.optString(i, "")
                    if (v.isNotBlank()) add(v)
                }
            }
        } catch (t: Throwable) {
            emptyList()
        }
    }

    private fun videoSelect(): String = """
        SELECT v.rowid, v.id, v.title, COALESCE(ch.title,''), COALESCE(v.date,''),
        COALESCE(v.time,''), COALESCE(v.duration,''), COALESCE(v.caption,0),
        COALESCE(v.is_live,0), COALESCE(v.is_short,0), COALESCE(v.published_at,'')
        FROM videos v LEFT JOIN channels ch ON ch.channel_id = v.channel_id
    """

    private fun readVideoItems(rs: ResultSet): List<VideoItem> = buildList {
        while (rs.next()) {
            add(
                VideoItem(
                    rowId = rs.getLong(1),
                    id = rs.getString(2) ?: "",
                    title = rs.getString(3) ?: "",
                    channel = rs.getString(4) ?: "",
                    date = rs.getString(5) ?: "",
                    time = rs.getString(6) ?: "",
                    duration = rs.getString(7) ?: "",
                    caption = rs.getInt(8) == 1,
                    live = rs.getInt(9) == 1,
                    short = rs.getInt(10) == 1,
                    thumb = null,
                    description = "",
                    avatar = null,
                    publishedAt = rs.getString(11) ?: ""
                )
            )
        }
    }

    private fun queryVideos(sql: String, args: List<Any>): List<VideoItem> {
        val c = conn ?: return emptyList()
        val items = runCatching {
            c.prepareStatement(sql).use { ps ->
                args.forEachIndexed { i, a -> ps.setObject(i + 1, a) }
                ps.executeQuery().use { rs -> readVideoItems(rs) }
            }
        }.getOrDefault(emptyList())
        if (items.isEmpty()) return items

        val ids = items.map { it.rowId }
        val marks = ids.joinToString(",") { "?" }
        val extras = mutableMapOf<Long, Pair<ByteArray?, String>>()
        runCatching {
            val thumbExpr = if (hasThumbImage) "thumb_image" else "NULL"
            c.prepareStatement(
                "SELECT rowid,$thumbExpr,COALESCE(description,'') FROM videos WHERE rowid IN ($marks)"
            ).use { ps ->
                ids.forEachIndexed { i, id -> ps.setLong(i + 1, id) }
                ps.executeQuery().use { rs ->
                    while (rs.next()) extras[rs.getLong(1)] = rs.getBytes(2) to (rs.getString(3) ?: "")
                }
            }
        }
        val avatars = mutableMapOf<Long, ByteArray>()
        if (hasChannelAvatar) {
            runCatching {
                c.prepareStatement(
                    "SELECT v.rowid,ch.avatar FROM videos v JOIN channels ch ON ch.channel_id=v.channel_id " +
                        "WHERE v.rowid IN ($marks) AND ch.avatar IS NOT NULL"
                ).use { ps ->
                    ids.forEachIndexed { i, id -> ps.setLong(i + 1, id) }
                    ps.executeQuery().use { rs ->
                        while (rs.next()) rs.getBytes(2)?.let { avatars[rs.getLong(1)] = it }
                    }
                }
            }
        }
        return items.map { item ->
            val extra = extras[item.rowId]
            val thumb = extra?.first
            item.copy(
                thumb = thumb,
                description = extra?.second ?: "",
                avatar = avatars[item.rowId] ?: thumb
            )
        }
    }

    private fun runVideos(where: String, args: List<Any>, limit: Int, offset: Int): List<VideoItem> {
        val clause = if (where.isBlank()) "" else " WHERE $where"
        val sql = videoSelect() + clause +
            " ORDER BY v.date DESC, v.time DESC LIMIT ? OFFSET ?"
        return queryVideos(sql, args + listOf(limit, offset))
    }

    private val feedCache = LinkedHashMap<String, List<VideoItem>>()
    private val cacheLock = Any()

    fun home(offset: Int = 0, limit: Int = 40, includeShorts: Boolean = false): List<VideoItem> {
        val key = "home|$includeShorts|$limit"
        if (offset == 0) synchronized(cacheLock) { feedCache[key]?.let { return it } }
        val items = if (includeShorts) runVideos("", emptyList(), limit, offset)
        else runVideos("v.is_short=0", emptyList(), limit, offset)
        if (offset == 0) synchronized(cacheLock) {
            if (feedCache.size >= 8) feedCache.clear()
            feedCache[key] = items
        }
        return items
    }

    fun shorts(offset: Int = 0, limit: Int = 40): List<VideoItem> {
        val key = "shorts|$limit"
        if (offset == 0) synchronized(cacheLock) { feedCache[key]?.let { return it } }
        val items = runVideos("v.is_short=1", emptyList(), limit, offset)
        if (offset == 0) synchronized(cacheLock) {
            if (feedCache.size >= 8) feedCache.clear()
            feedCache[key] = items
        }
        return items
    }

    fun videoByRow(rowId: Long): VideoItem? {
        val list = runVideos("v.rowid = ?", listOf(rowId), 1, 0)
        return list.firstOrNull()
    }

    private var topicsCache: List<TopicItem>? = null
    private val channelFeedCache = LinkedHashMap<String, List<VideoItem>>()

    fun channelFeed(channelId: String, kind: String = "all", offset: Int = 0, limit: Int = 30): List<VideoItem> {
        val key = "$channelId|$kind|$offset|$limit"
        if (offset == 0) synchronized(cacheLock) { channelFeedCache[key]?.let { return it } }
        val clause = when (kind) {
            "all" -> "v.channel_id=?"
            "short" -> "v.channel_id=? AND v.is_short=1"
            "live" -> "v.channel_id=? AND v.is_live=1"
            else -> throw IllegalArgumentException("channelFeed: unknown kind '$kind'")
        }
        val sql = videoSelect() + " WHERE $clause ORDER BY v.rowid ASC LIMIT ? OFFSET ?"
        val items = queryVideos(sql, listOf(channelId, limit, offset))
        if (offset == 0) synchronized(cacheLock) {
            if (channelFeedCache.size >= 4) channelFeedCache.clear()
            channelFeedCache[key] = items
        }
        return items
    }

    fun topics(limit: Int = 80, sort: String = "popular"): List<TopicItem> {
        val c = conn ?: return emptyList()
        val cached = topicsCache
        val all = if (cached == null) {
            val list = runCatching {
                c.createStatement().use { s ->
                    s.executeQuery(
                        "SELECT topic_id,name,parent,video_count FROM topics ORDER BY video_count DESC, name"
                    ).use { rs ->
                        buildList {
                            while (rs.next()) {
                                add(
                                    TopicItem(
                                        id = rs.getInt(1),
                                        name = rs.getString(2) ?: "",
                                        parent = rs.getString(3) ?: "",
                                        count = rs.getInt(4)
                                    )
                                )
                            }
                        }
                    }
                }
            }.getOrDefault(emptyList())
            topicsCache = list
            list
        } else cached
        return when (sort) {
            "name_asc" -> all.sortedBy { it.name }.take(limit)
            "name_desc" -> all.sortedByDescending { it.name }.take(limit)
            else -> all.take(limit)
        }
    }

    fun topicsOf(videoId: String): List<TopicItem> {
        val c = conn ?: return emptyList()
        return runCatching {
            c.prepareStatement(
                "SELECT t.topic_id,t.name,t.parent,vt.rank " +
                    "FROM video_topics vt JOIN topics t ON t.topic_id=vt.topic_id " +
                    "WHERE vt.video_id=? ORDER BY vt.rank LIMIT 5"
            ).use { ps ->
                ps.setString(1, videoId)
                ps.executeQuery().use { rs ->
                    buildList {
                        while (rs.next()) {
                            add(
                                TopicItem(
                                    id = rs.getInt(1),
                                    name = rs.getString(2) ?: "",
                                    parent = rs.getString(3) ?: "",
                                    count = rs.getInt(4)
                                )
                            )
                        }
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    fun channels(): List<ChannelItem> {
        val c = conn ?: return emptyList()
        return runCatching {
            c.createStatement().use { s ->
                s.executeQuery(
                    "SELECT ch.channel_id,ch.title,COALESCE(v.cnt,0)," +
                        "${if (hasChannelAvatar) "ch.avatar" else "NULL"} " +
                        "FROM channels ch LEFT JOIN " +
                        "(SELECT channel_id,COUNT(*) cnt FROM videos GROUP BY channel_id) v " +
                        "ON v.channel_id=ch.channel_id ORDER BY v.cnt DESC"
                ).use { rs ->
                    buildList {
                        while (rs.next()) {
                            add(
                                ChannelItem(
                                    id = rs.getString(1) ?: "",
                                    title = rs.getString(2) ?: "",
                                    count = rs.getInt(3),
                                    avatar = rs.getBytes(4)
                                )
                            )
                        }
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    fun channelInfo(channelId: String): ChannelInfo? {
        val c = conn ?: return null
        return runCatching {
            c.prepareStatement(
                "SELECT ch.channel_id,ch.title,COALESCE(ch.description,'')," +
                    "${if (hasChannelAvatar) "ch.avatar" else "NULL"}," +
                    "COUNT(v.id)," +
                    "SUM(CASE WHEN COALESCE(v.caption,0)=1 THEN 1 ELSE 0 END)," +
                    "SUM(CASE WHEN COALESCE(v.caption,0)=0 THEN 1 ELSE 0 END)," +
                    "SUM(CASE WHEN COALESCE(v.is_short,0)=1 THEN 1 ELSE 0 END)," +
                    "SUM(CASE WHEN COALESCE(v.is_live,0)=1 THEN 1 ELSE 0 END)," +
                    "(SELECT COUNT(*) FROM playlists p WHERE p.channel_id=ch.channel_id) " +
                    "FROM channels ch LEFT JOIN videos v ON v.channel_id=ch.channel_id " +
                    "WHERE ch.channel_id=? GROUP BY ch.channel_id"
            ).use { ps ->
                ps.setString(1, channelId)
                ps.executeQuery().use { rs ->
                    if (!rs.next()) null else ChannelInfo(
                        id = rs.getString(1) ?: "",
                        title = rs.getString(2) ?: "",
                        description = rs.getString(3) ?: "",
                        avatar = rs.getBytes(4),
                        total = rs.getInt(5),
                        captions = rs.getInt(6),
                        noCaptions = rs.getInt(7),
                        shorts = rs.getInt(8),
                        lives = rs.getInt(9),
                        playlists = rs.getInt(10)
                    )
                }
            }
        }.getOrDefault(null)
    }

    fun searchSuggestions(query: String, limit: Int = 12, channelId: String? = null): List<SuggestionItem> {
        val c = conn ?: return emptyList()
        if (query.isBlank()) return emptyList()
        val like = "%${query.trim()}%"
        val out = mutableListOf<SuggestionItem>()
        runCatching {
            val titleSql = if (channelId == null) {
                "SELECT DISTINCT title FROM videos WHERE title LIKE ? ORDER BY date DESC LIMIT ?"
            } else {
                "SELECT DISTINCT title FROM videos WHERE title LIKE ? AND channel_id=? ORDER BY date DESC LIMIT ?"
            }
            c.prepareStatement(titleSql).use { ps ->
                ps.setString(1, like)
                if (channelId == null) ps.setInt(2, limit) else { ps.setString(2, channelId); ps.setInt(3, limit) }
                ps.executeQuery().use { rs ->
                    while (rs.next()) out.add(SuggestionItem(rs.getString(1) ?: "", "video"))
                }
            }
            val topicSql = if (channelId == null) {
                "SELECT name FROM topics WHERE name LIKE ? ORDER BY video_count DESC LIMIT ?"
            } else {
                "SELECT t.name FROM topics t JOIN video_topics vt ON vt.topic_id=t.topic_id " +
                    "JOIN videos v ON v.id=vt.video_id WHERE t.name LIKE ? AND v.channel_id=? " +
                    "GROUP BY t.topic_id ORDER BY COUNT(v.id) DESC LIMIT ?"
            }
            c.prepareStatement(topicSql).use { ps ->
                ps.setString(1, like)
                if (channelId == null) ps.setInt(2, limit) else { ps.setString(2, channelId); ps.setInt(3, limit) }
                ps.executeQuery().use { rs ->
                    while (rs.next()) out.add(SuggestionItem(rs.getString(1) ?: "", "topic"))
                }
            }
        }
        return out.distinctBy { it.label }.take(limit)
    }

    fun chaptersOf(videoRowId: Long): List<ChapterItem> {
        val c = conn ?: return emptyList()
        return runCatching {
            c.prepareStatement("SELECT chapters FROM videos WHERE rowid=?").use { ps ->
                ps.setLong(1, videoRowId)
                ps.executeQuery().use { rs ->
                    if (!rs.next()) emptyList() else {
                        val raw = rs.getString(1) ?: ""
                        if (raw.isBlank()) emptyList() else {
                            val arr = org.json.JSONArray(raw)
                            buildList {
                                for (i in 0 until arr.length()) {
                                    val o = arr.optJSONObject(i) ?: continue
                                    val start = o.optString("start", "")
                                    val title = o.optString("title", "")
                                    if (start.isNotBlank() && title.isNotBlank()) add(ChapterItem(start, title))
                                }
                            }
                        }
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    fun playlists(): List<PlaylistItem> {
        val c = conn ?: return emptyList()
        return runCatching {
            c.createStatement().use { s ->
                s.executeQuery(
                    "SELECT p.playlist_id,p.title,COUNT(pv.video_id) " +
                        "FROM playlists p LEFT JOIN playlist_videos pv ON pv.playlist_id=p.playlist_id " +
                        "GROUP BY p.playlist_id ORDER BY COUNT(pv.video_id) DESC LIMIT 80"
                ).use { rs ->
                    buildList {
                        while (rs.next()) {
                            add(
                                PlaylistItem(
                                    id = rs.getString(1) ?: "",
                                    title = rs.getString(2) ?: "",
                                    count = rs.getInt(3)
                                )
                            )
                        }
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    fun channelPlaylists(channelId: String): List<PlaylistItem> {
        val c = conn ?: return emptyList()
        return runCatching {
            c.prepareStatement(
                "SELECT p.playlist_id,p.title,COUNT(DISTINCT pv.video_id) " +
                    "FROM playlists p JOIN playlist_videos pv ON pv.playlist_id=p.playlist_id " +
                    "JOIN videos v ON v.id=pv.video_id WHERE v.channel_id=? " +
                    "GROUP BY p.playlist_id ORDER BY COUNT(DISTINCT pv.video_id) DESC"
            ).use { ps ->
                ps.setString(1, channelId)
                ps.executeQuery().use { rs ->
                    buildList {
                        while (rs.next()) {
                            add(PlaylistItem(
                                id = rs.getString(1) ?: "",
                                title = rs.getString(2) ?: "",
                                count = rs.getInt(3),
                                cover = null
                            ))
                        }
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    fun channelTopics(channelId: String): List<TopicItem> {
        val c = conn ?: return emptyList()
        return runCatching {
            c.prepareStatement(
                "SELECT t.topic_id,t.name,t.parent,COUNT(DISTINCT v.id) " +
                    "FROM topics t JOIN video_topics vt ON vt.topic_id=t.topic_id " +
                    "JOIN videos v ON v.id=vt.video_id WHERE v.channel_id=? " +
                    "GROUP BY t.topic_id ORDER BY COUNT(DISTINCT v.id) DESC"
            ).use { ps ->
                ps.setString(1, channelId)
                ps.executeQuery().use { rs ->
                    buildList {
                        while (rs.next()) {
                            add(TopicItem(
                                id = rs.getInt(1),
                                name = rs.getString(2) ?: "",
                                parent = rs.getString(3) ?: "",
                                count = rs.getInt(4),
                                cover = null
                            ))
                        }
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    fun playlistCover(playlistId: String): ByteArray? {
        val c = conn ?: return null
        if (!hasThumbImage) return null
        return runCatching {
            c.prepareStatement(
                "SELECT v.thumb_image FROM playlist_videos pv JOIN videos v ON v.id=pv.video_id " +
                    "WHERE pv.playlist_id=? AND v.thumb_image IS NOT NULL ORDER BY pv.position LIMIT 1"
            ).use { ps ->
                ps.setString(1, playlistId)
                ps.executeQuery().use { rs -> if (rs.next()) rs.getBytes(1) else null }
            }
        }.getOrNull()
    }

    fun topicCover(topicId: Int): ByteArray? {
        val c = conn ?: return null
        if (!hasThumbImage) return null
        return runCatching {
            c.prepareStatement(
                "SELECT v.thumb_image FROM video_topics vt JOIN videos v ON v.id=vt.video_id " +
                    "WHERE vt.topic_id=? AND v.thumb_image IS NOT NULL ORDER BY v.rowid LIMIT 1"
            ).use { ps ->
                ps.setInt(1, topicId)
                ps.executeQuery().use { rs -> if (rs.next()) rs.getBytes(1) else null }
            }
        }.getOrNull()
    }

    fun searchHits(query: String, channelId: String? = null, limit: Int = 80): List<SearchHit> {
        val c = conn ?: return emptyList()
        if (query.isBlank()) return emptyList()

        fun run(match: String): List<SearchHit> {
            val expr = if (hasTextN) "text_n : ($match)" else match
            val sql = "SELECT c.video_rowid,c.video_id,c.start_time,c.text,v.title,ch.title,v.date,v.time " +
                "FROM cues_fts JOIN cues c ON c.rowid=cues_fts.rowid " +
                "JOIN videos v ON v.rowid=c.video_rowid " +
                "LEFT JOIN channels ch ON ch.channel_id=v.channel_id " +
                "WHERE cues_fts MATCH ?" + (if (channelId == null) "" else " AND v.channel_id=?") +
                " LIMIT ?"
            val args = buildList {
                add(expr)
                if (channelId != null) add(channelId)
                add((limit * 4).toString())
            }.toTypedArray()
            val rows = query(c, sql, args) { rs ->
                buildList {
                    while (rs.next()) {
                        add(Triple(
                            SearchHit(
                                videoRowId = rs.getLong(1),
                                videoId = rs.getString(2) ?: "",
                                start = rs.getString(3) ?: "",
                                text = rs.getString(4) ?: "",
                                title = rs.getString(5) ?: "",
                                channel = rs.getString(6) ?: "",
                                thumb = null,
                                avatar = null
                            ),
                            rs.getString(7) ?: "",
                            rs.getString(8) ?: ""
                        ))
                    }
                }
            }
            val hits = rows
                .sortedWith(compareByDescending<Triple<SearchHit, String, String>> { it.second }
                    .thenByDescending { it.third })
                .take(limit)
                .map { it.first }
            if (hits.isEmpty()) return emptyList()
            val ids = hits.map { it.videoRowId }
            val marks = ids.joinToString(",") { "?" }
            val thumbs = mutableMapOf<Long, ByteArray?>()
            runCatching {
                val thumbExpr = if (hasThumbImage) "thumb_image" else "NULL"
                c.prepareStatement("SELECT rowid,$thumbExpr FROM videos WHERE rowid IN ($marks)").use { ps ->
                    ids.forEachIndexed { i, id -> ps.setLong(i + 1, id) }
                    ps.executeQuery().use { rs ->
                        while (rs.next()) thumbs[rs.getLong(1)] = rs.getBytes(2)
                    }
                }
            }
            return hits.map { hit ->
                val thumb = thumbs[hit.videoRowId]
                hit.copy(thumb = thumb, avatar = thumb)
            }
        }

        if (!ftsSupported) return emptyList()
        val match = toMatch(query)
        if (match.isBlank()) return emptyList()
        val first = runCatching { run(match) }
        if (first.isSuccess) return first.getOrThrow()
        val sanitized = Regex("[\\p{L}\\p{N}]+").findAll(query)
            .joinToString(" AND ") { "\"$it\"" }
        if (sanitized.isBlank()) return emptyList()
        return runCatching { run(sanitized) }.getOrDefault(emptyList())
    }

    fun filtered(f: FilterState, offset: Int = 0, limit: Int = 40): List<VideoItem> {
        val where = mutableListOf<String>()
        val args = mutableListOf<Any>()
        if (f.query.isNotBlank()) {
            val like = "%${f.query.trim()}%"
            where.add("(v.title LIKE ? OR v.description LIKE ?)")
            args.add(like); args.add(like)
        }
        if (f.channelIds.isNotEmpty()) {
            where.add("v.channel_id IN (${f.channelIds.joinToString(",") { "?" }})")
            args.addAll(f.channelIds)
        }
        if (f.topicId != null) {
            where.add("v.id IN (SELECT vt.video_id FROM video_topics vt WHERE vt.topic_id=?)")
            args.add(f.topicId)
        }
        if (!f.topicName.isNullOrBlank()) {
            val like = "%${f.topicName}%"
            where.add(
                "v.id IN (SELECT vt.video_id FROM video_topics vt JOIN topics t " +
                    "ON t.topic_id=vt.topic_id WHERE t.name LIKE ? OR t.parent LIKE ?)"
            )
            args.add(like); args.add(like)
        }
        if (!f.playlistId.isNullOrBlank()) {
            where.add("v.id IN (SELECT video_id FROM playlist_videos WHERE playlist_id=?)")
            args.add(f.playlistId)
        }
        if (f.caption == 0 || f.caption == 1) {
            where.add("v.caption=?")
            args.add(f.caption)
        }
        when (f.kind) {
            "live" -> where.add("v.is_live=1")
            "short" -> where.add("v.is_short=1")
            "normal" -> where.add("v.is_short=0 AND v.is_live=0")
        }
        if (f.fromDate.isNotBlank()) {
            where.add("v.date >= ?"); args.add(f.fromDate)
        }
        if (f.toDate.isNotBlank()) {
            where.add("v.date <= ?"); args.add(f.toDate)
        }
        val durationSeconds =
            "((CAST(substr(v.duration,1,2) AS INTEGER)*3600)+" +
                "(CAST(substr(v.duration,4,2) AS INTEGER)*60)+" +
                "CAST(substr(v.duration,7,2) AS INTEGER))"
        if (f.minSeconds != null) {
            where.add("$durationSeconds >= ?"); args.add(f.minSeconds)
        }
        if (f.maxSeconds != null) {
            where.add("$durationSeconds <= ?"); args.add(f.maxSeconds)
        }
        return runVideos(where.joinToString(" AND "), args, limit, offset)
    }

    companion object {
        const val DB_NAME = "captions.db"

        init {
            try { Class.forName("org.sqlite.JDBC") } catch (_: Throwable) {}
        }

        fun toMatch(query: String): String {
            fun clean(t: String) = t
                .replace(Regex("[\\u064B-\\u065F\\u0670\\u06D6-\\u06ED]"), "")
                .replace(Regex("[^\\p{L}\\p{N}]"), "")
            return query.trim().split(Regex("\\s+"))
                .map(::clean)
                .filter { it.isNotEmpty() }
                .joinToString(" AND ") { "\"$it\"" }
        }
    }
}
