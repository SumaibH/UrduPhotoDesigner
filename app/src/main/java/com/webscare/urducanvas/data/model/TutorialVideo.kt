package com.webscare.urducanvas.data.model

/**
 * One video from the UrduCanvas YouTube channel, as it appears in the Tutorials list.
 *
 * Everything here comes straight out of the channel's Atom feed — no API key, no quota.
 */
data class TutorialVideo(
    val id: String,
    val title: String,
    val description: String,
    val publishedAtMillis: Long,
    val thumbnailUrl: String,
    val views: Long
) {
    /** The page to open when the video cannot be handed to the YouTube app. */
    val watchUrl: String get() = "https://www.youtube.com/watch?v=$id"
}
