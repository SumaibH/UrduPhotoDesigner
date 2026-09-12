package com.webscare.urducanvas.common.utils

import android.graphics.Paint

/**
 * Rounds the corners and ends of a text stroke.
 *
 * Every stroked text layer — under-stroke, primary stroke, in the canvas and in the
 * style grid's thumbnails — goes through here, so there is one answer to what a text
 * stroke's corners look like rather than one per draw path.
 *
 * Why round rather than Paint's default [Paint.Join.MITER]: a mitred corner runs the two
 * outer edges out until they meet, so the sharper the corner the further the spike, up to
 * the miter limit — four times the half-width, which at the stroke widths a tube or
 * bubble style needs is tens of pixels of dagger hanging off the glyph. Nastaleeq is made
 * of sharp corners: every kashida terminal, every tooth, every place a stroke turns back
 * on itself. At the two-to-six pixel widths the existing catalogue uses the difference is
 * a rounded rather than pointed corner and nothing else; at twenty-plus it is the
 * difference between a marker stroke and a cluster of spikes.
 *
 * [Paint.Cap.ROUND] is set for completeness. Glyph outlines are closed contours, so caps
 * have nothing to act on — a stroked glyph has no free ends — but a paint that says
 * round joins and butt caps invites the reader to wonder which one is load-bearing.
 */
fun Paint.applyTextStrokeShape() {
    strokeJoin = Paint.Join.ROUND
    strokeCap = Paint.Cap.ROUND
}
