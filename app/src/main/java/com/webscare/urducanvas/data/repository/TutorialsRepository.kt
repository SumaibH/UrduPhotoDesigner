package com.webscare.urducanvas.data.repository

import android.content.Context
import android.util.Log
import android.util.Xml
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.webscare.urducanvas.common.sealed.Response
import com.webscare.urducanvas.common.utils.Constants
import com.webscare.urducanvas.data.model.TutorialVideo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "TutorialsRepo"

/**
 * Loads the tutorial videos published on the UrduCanvas YouTube channel.
 *
 * The channel's Atom feed carries everything the list needs — id, title, description,
 * publish date, thumbnail and view count — so there is no API key, no quota and no
 * JSON API in the way. The parse is done with the platform's own XmlPullParser rather
 * than adding an XML converter dependency.
 *
 * Results are cached on disk so the screen opens instantly on a second visit and still
 * shows something useful with no connection. A stale cache always beats an error state:
 * if the network fails and a cache exists, the cache is what the user sees.
 */
@Singleton
class TutorialsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val gson: Gson
) {

    private val cacheFile: File get() = File(context.cacheDir, "tutorials.json")

    /**
     * Emits [Response.Loading], then the cached list if one is fresh enough, otherwise
     * the freshly fetched list. On a network failure with a cache present the cache is
     * emitted as a success — an outdated tutorial list is still a usable one.
     */
    fun getTutorials(forceRefresh: Boolean = false): Flow<Response<List<TutorialVideo>>> = flow {
        emit(Response.Loading)

        val cached = readCache()
        if (!forceRefresh && cached != null && cached.isFresh) {
            emit(Response.Success(cached.videos))
            return@flow
        }

        // Show what we have while the network call runs, so the shimmer is only ever
        // seen on a genuinely cold start.
        if (cached != null && cached.videos.isNotEmpty()) {
            emit(Response.Processing(cached.videos))
        }

        try {
            val videos = fetchFeed()
            if (videos.isEmpty()) {
                // An empty parse means the feed shape changed; do not overwrite a good
                // cache with nothing.
                if (cached != null && cached.videos.isNotEmpty()) {
                    emit(Response.Success(cached.videos))
                } else {
                    emit(Response.Success(emptyList()))
                }
                return@flow
            }
            writeCache(videos)
            emit(Response.Success(videos))
        } catch (e: Exception) {
            Log.w(TAG, "Tutorial feed fetch failed", e)
            if (cached != null && cached.videos.isNotEmpty()) {
                emit(Response.Success(cached.videos))
            } else {
                emit(Response.Error(e.message ?: "Could not load tutorials"))
            }
        }
    }.flowOn(Dispatchers.IO)

    // ── Network ───────────────────────────────────────────────────────────────

    private fun fetchFeed(): List<TutorialVideo> {
        val request = Request.Builder().url(Constants.YOUTUBE_FEED_URL).build()
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Feed responded ${response.code}")
            val body = response.body ?: error("Feed returned an empty body")
            return parseFeed(body.byteStream())
        }
    }

    /**
     * Pulls the `<entry>` elements out of the Atom feed.
     *
     * Namespaced names (`yt:videoId`, `media:thumbnail`) are matched on the local name
     * only, because the parser runs with namespace processing off — that way a change
     * to YouTube's namespace prefixes cannot silently empty the list.
     */
    private fun parseFeed(input: java.io.InputStream): List<TutorialVideo> {
        val videos = mutableListOf<TutorialVideo>()
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(input, null)

        var inEntry = false
        var id: String? = null
        var title: String? = null
        var description = ""
        var published = 0L
        var thumbnail: String? = null
        var views = 0L

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            val name = parser.name
            when (event) {
                XmlPullParser.START_TAG -> when {
                    name == "entry" -> {
                        inEntry = true
                        id = null; title = null; description = ""
                        published = 0L; thumbnail = null; views = 0L
                    }

                    !inEntry -> Unit

                    name.endsWith("videoId") -> id = parser.nextText().trim()

                    // The feed carries both <title> and <media:title>; the first one
                    // inside the entry wins so the later duplicate cannot blank it.
                    name.endsWith("title") && title == null -> title = parser.nextText().trim()

                    name.endsWith("description") -> description = parser.nextText().trim()

                    name == "published" -> published = parseAtomDate(parser.nextText())

                    name.endsWith("thumbnail") ->
                        thumbnail = parser.getAttributeValue(null, "url")

                    name.endsWith("statistics") ->
                        views = parser.getAttributeValue(null, "views")?.toLongOrNull() ?: 0L
                }

                XmlPullParser.END_TAG -> if (name == "entry") {
                    inEntry = false
                    val videoId = id
                    if (!videoId.isNullOrBlank()) {
                        videos += TutorialVideo(
                            id = videoId,
                            title = title.orEmpty(),
                            description = description,
                            publishedAtMillis = published,
                            // The feed's own thumbnail is hqdefault; fall back to
                            // deriving it so a missing media:thumbnail is not a blank row.
                            thumbnailUrl = thumbnail
                                ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg",
                            views = views
                        )
                    }
                }
            }
            event = parser.next()
        }
        return videos
    }

    private fun parseAtomDate(raw: String): Long = try {
        ATOM_DATE_FORMAT.get()?.parse(raw.trim())?.time ?: 0L
    } catch (e: Exception) {
        0L
    }

    // ── Disk cache ────────────────────────────────────────────────────────────

    private class CachedFeed(val fetchedAtMillis: Long, val videos: List<TutorialVideo>) {
        val isFresh: Boolean
            get() = System.currentTimeMillis() - fetchedAtMillis < CACHE_TTL_MS
    }

    private fun readCache(): CachedFeed? = try {
        val file = cacheFile
        if (!file.exists()) null else {
            val type = object : TypeToken<List<TutorialVideo>>() {}.type
            val videos: List<TutorialVideo> = gson.fromJson(file.readText(), type) ?: emptyList()
            CachedFeed(file.lastModified(), videos)
        }
    } catch (e: Exception) {
        Log.w(TAG, "Tutorial cache unreadable, ignoring it", e)
        null
    }

    private fun writeCache(videos: List<TutorialVideo>) {
        try {
            // Write to a temp file and rename, so a kill mid-write cannot leave a
            // half-written cache that then fails to parse on the next launch.
            val tmp = File(context.cacheDir, "tutorials.json.tmp")
            tmp.writeText(gson.toJson(videos))
            if (!tmp.renameTo(cacheFile)) {
                cacheFile.writeText(tmp.readText())
                tmp.delete()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not cache tutorials", e)
        }
    }

    companion object {
        private const val CACHE_TTL_MS = 6L * 60 * 60 * 1000 // 6 hours

        // SimpleDateFormat is not thread safe and this repository is a singleton.
        private val ATOM_DATE_FORMAT = object : ThreadLocal<SimpleDateFormat>() {
            override fun initialValue(): SimpleDateFormat =
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
        }
    }
}
