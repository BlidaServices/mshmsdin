# mshmsdin — CaptionSearch DB

The core artifact is `database/youtube_captions.db` (~443 MB, SQLite + FTS5): captions of the Mohammed bin Shams al-Din YouTube channel, used by the Android app in `app/` (imported on-device as `captions.db`; see `CaptionRepository.kt`, `DB_NAME` — DB is NOT in app assets).

## DB facts (verified against the file)
- Tables: `videos` (6638, both channels), `channels` (registry: `channel_id` PK, title, description, published_at, subscriber_count, video_count; `videos.channel_id` → `channels.channel_id`), `cues` (1544977), `playlists` (145), `playlist_videos` (4944), `qna_questions` (3886), `qna_final` (263), `qna_analysis` (713), `book_chapters_backup_20261113`.
- Main channel `UCNQR1yu8BLrSihTabql3rVQ` (محمد بن شمس الدين), second `UCiu_Wwm_NCows_g4TEPA8Dg` (الردود العلمية لمحمد بن شمس الدين), third `UCkhd6kml-kPqu-chWubSsBA` (كشف التلبيس — @mshmsdin-rd, 369 videos), fourth `UCn1khdngXHHFuZWcLY9pzCQ` (بثوث محمد بن شمس الدين — @ShmsLive, 3787 videos, mostly is_short per ≤60s heuristic) — 2nd imported 2026-11-17 from `/home/bidjad/mshmsdin/youtube_captions_second.db`, 3rd/4th imported 2026-11-17 via Data API v3 (metadata only; API reports caption=false for both). Rows are globally ordered date DESC (newest first) across all channels. API reports 5041/1619/368/3787 videos vs 5021/1617/369/3787 imported.
- `videos` columns include `caption` (0/1), `is_live`, `is_short`, `chapters`/`chapters_json`, plus API static metadata added 2026-11-17: `published_at`, `tags` (JSON array), `category_id`, `default_language`, `definition`, `licensed_content`, `privacy_status`, `made_for_kids`, `live_scheduled_at`, `live_started_at`, `live_ended_at`, `thumb_default`, `thumb_medium`, `thumb_high`. Statistics (views/likes) intentionally NOT stored — they'd go stale; refetch on demand. `caption_language` — NOT NULL only for foreign-language videos (`en`, `hi`); Arabic-caption videos and no-caption videos are NULL by design (user decision: Arabic needs no tag; only tag non-Arabic). Unique index `idx_videos_id` on `videos(id)`; `cues.video_rowid` → `videos.rowid` (index `idx_cues_video`), plus `video_id` denormalized.
- FTS5 external-content tables: `videos_fts(title, description)` and `cues_fts(text, text_n)`, both `content=` the base table, `content_rowid='rowid'`, `tokenize="unicode61"` (note: `scripts/python/build_dbs.py` shows `remove_diacritics 2` and a different `videos` schema — the script is outdated; trust the DB file, not the script).
- `cues.text_n` holds normalized Arabic (harakat stripped + word-prefix-expanded alternatives, see `normalize()` in build_dbs.py) so prefix/stemming search matches; FTS queries should target `text_n`.
- Only `cues` has FTS sync triggers (`cues_ai/ad/au`). `videos_fts` has NO triggers — after editing `videos` you must rebuild/sync `videos_fts` manually (e.g. `INSERT INTO videos_fts(videos_fts) VALUES('rebuild')`) or search results will be stale.
- No `sqlite3` CLI guaranteed — use `python3 -c "import sqlite3; ..."`, open read-only with `file:...?mode=ro` URI when inspecting.
- The Android app queries via `cues_fts MATCH ... JOIN cues`, `videos_fts MATCH ... JOIN videos` (see `CaptionRepository.kt` ~L180-215); `toMatch()` appends `*` to each whitespace-separated token for prefix matching.

## Rules for mutating the DB
- ALWAYS copy first: `cp database/youtube_captions.db database/youtube_captions_before_<what>.db` (per scripts/README.md).
- After direct edits to `cues`/`videos`, verify FTS is in sync (`cues_fts` via triggers is automatic; `videos_fts` is not).
- `book_chapters_backup_20261113` is a backup table, not live data.
- `srt/`, `chapters_output/`, `summary_output*/` are huge generated artifacts — don't read or commit.

## Pipeline
- `scripts/python/` + `scripts/shell/` build/refresh the DB: scan_channel → fetch captions → summarize (Gemini) → extract chapters → fix times. See `scripts/README.md` (Arabic) for order and per-script roles.
- Scripts use hardcoded absolute paths (`/home/bidjad/mshmsdin/...` — note: repo now lives at `/home/bidjad/android/projects/mshmsdin`); fix paths or they will fail.
- API keys (Gemini, YouTube Data API) come from a `secrets/` dir not in this repo.
- Pipeline output formats and DB write patterns in old scripts may not match current schema — inspect the live DB before assuming.

## App build (only if touching the APK)
- From `app/`: `./gradlew :app:assembleDebug`. No tests/lint/CI; verify via build.
- `local.properties` holds `sdk.dir` + signing keys — never commit/echo. Wrapper URL is a local file (`/home/bidjad/Gradle-Files/gradle-9.3.1-bin.zip`), machine-specific.
- `gradle/cachedVersions.json` pins dep versions via `eachDependency`; edit it, not only `libs.versions.toml`. `repositoriesMode = FAIL_ON_PROJECT_REPOS`. gradle.properties disables daemon/parallel/caching.
- Release APK bundles `app/app/libs/sqlite-jdbc-3.41.2.2.jar` + `jniLibs/<abi>/libsqlitejdbc.so`.
