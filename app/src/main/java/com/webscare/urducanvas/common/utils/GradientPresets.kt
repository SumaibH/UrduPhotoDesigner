package com.webscare.urducanvas.common.utils

import androidx.core.graphics.toColorInt
import com.webscare.urducanvas.common.canvas.enums.GradientType
import com.webscare.urducanvas.common.canvas.model.GradientItem

object GradientPresets {
    /**
     * The gradient catalogue.
     *
     * The first thirteen groups are the original mood ramps. Everything after them was
     * added because the catalogue was all one thing: every entry was LINEAR and every
     * entry ran at 135°, while the renderer has supported radial and sweep, any angle and
     * any number of stops all along. Metals in particular cannot be done with two stops —
     * what reads as metal is the tight highlight band around the middle of the ramp.
     */
    val defaultList: List<GradientItem> = listOf(

        // ── Pitch Black & Deep Dark ───────────────────────────────────────────
        GradientItem(colors = listOf("#0D0D0D".toColorInt(), "#1A1A2E".toColorInt(), "#16213E".toColorInt()), positions = listOf(0f, 0.5f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#000000".toColorInt(), "#0F0C29".toColorInt(), "#302B63".toColorInt()), positions = listOf(0f, 0.5f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#000000".toColorInt(), "#1A1A1A".toColorInt()), positions = listOf(0f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#0D0D0D".toColorInt(), "#2C003E".toColorInt()), positions = listOf(0f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#010101".toColorInt(), "#0A0A23".toColorInt(), "#1B1B3A".toColorInt()), positions = listOf(0f, 0.5f, 1f), angle = 135f, type = GradientType.LINEAR),

        // ── Dark & Moody ──────────────────────────────────────────────────────
        GradientItem(colors = listOf("#0F2027".toColorInt(), "#203A43".toColorInt(), "#2C5364".toColorInt()), positions = listOf(0f, 0.5f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#141E30".toColorInt(), "#243B55".toColorInt()), positions = listOf(0f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#1A1A2E".toColorInt(), "#16213E".toColorInt(), "#0F3460".toColorInt()), positions = listOf(0f, 0.5f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#232526".toColorInt(), "#414345".toColorInt()), positions = listOf(0f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#16222A".toColorInt(), "#3A6073".toColorInt()), positions = listOf(0f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#1F1C2C".toColorInt(), "#928DAB".toColorInt()), positions = listOf(0f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#200122".toColorInt(), "#6F0000".toColorInt()), positions = listOf(0f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#0F0C29".toColorInt(), "#302B63".toColorInt(), "#24243E".toColorInt()), positions = listOf(0f, 0.5f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#000428".toColorInt(), "#004E92".toColorInt()), positions = listOf(0f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#2D1B69".toColorInt(), "#11998E".toColorInt()), positions = listOf(0f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#373B44".toColorInt(), "#4286F4".toColorInt()), positions = listOf(0f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#1A1A2E".toColorInt(), "#E94560".toColorInt()), positions = listOf(0f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#0D0D0D".toColorInt(), "#2980B9".toColorInt(), "#6DD5FA".toColorInt()), positions = listOf(0f, 0.5f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#2C3E50".toColorInt(), "#4CA1AF".toColorInt()), positions = listOf(0f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#24243E".toColorInt(), "#302B63".toColorInt(), "#0F0C29".toColorInt()), positions = listOf(0f, 0.5f, 1f), angle = 135f, type = GradientType.LINEAR),

        // ── Navy & Deep Ocean ─────────────────────────────────────────────────
        GradientItem(colors = listOf("#005C97".toColorInt(), "#363795".toColorInt()), positions = listOf(0f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#1CB5E0".toColorInt(), "#000046".toColorInt()), positions = listOf(0f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#2E3192".toColorInt(), "#1BFFFF".toColorInt()), positions = listOf(0f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#0052D4".toColorInt(), "#4364F7".toColorInt(), "#6FB1FC".toColorInt()), positions = listOf(0f, 0.5f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#4CA1AF".toColorInt(), "#C4E0E5".toColorInt()), positions = listOf(0f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#003973".toColorInt(), "#E5E5BE".toColorInt()), positions = listOf(0f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#1A3C5E".toColorInt(), "#2980B9".toColorInt(), "#6DD5FA".toColorInt()), positions = listOf(0f, 0.5f, 1f), angle = 135f, type = GradientType.LINEAR),

        // ── Cool Blue & Cyan ──────────────────────────────────────────────────
        GradientItem(colors = listOf("#00D2FF".toColorInt(), "#3A7BD5".toColorInt()), positions = listOf(0f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#4FACFE".toColorInt(), "#00F2FE".toColorInt()), positions = listOf(0f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#48C6EF".toColorInt(), "#6F86D6".toColorInt()), positions = listOf(0f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#89F7FE".toColorInt(), "#66A6FF".toColorInt()), positions = listOf(0f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#A1C4FD".toColorInt(), "#C2E9FB".toColorInt()), positions = listOf(0f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#6DD5FA".toColorInt(), "#2980B9".toColorInt(), "#1A3C5E".toColorInt()), positions = listOf(0f, 0.5f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#00C9FF".toColorInt(), "#92FE9D".toColorInt()), positions = listOf(0f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#0BD3D3".toColorInt(), "#0099F7".toColorInt()), positions = listOf(0f, 1f), angle = 135f, type = GradientType.LINEAR),

        // ── Purple & Violet ───────────────────────────────────────────────────
        GradientItem(colors = listOf("#DA22FF".toColorInt(), "#9733EE".toColorInt()), positions = listOf(0f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#7B2FF7".toColorInt(), "#F107A3".toColorInt()), positions = listOf(0f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#667EEA".toColorInt(), "#764BA2".toColorInt()), positions = listOf(0f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#5F0A87".toColorInt(), "#A4508B".toColorInt(), "#F6D365".toColorInt()), positions = listOf(0f, 0.5f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#8E2DE2".toColorInt(), "#4A00E0".toColorInt()), positions = listOf(0f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#4A00E0".toColorInt(), "#8E2DE2".toColorInt(), "#DA22FF".toColorInt()), positions = listOf(0f, 0.5f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#B721FF".toColorInt(), "#21D4FD".toColorInt()), positions = listOf(0f, 1f), angle = 135f, type = GradientType.LINEAR),

        // ── Pink & Magenta ────────────────────────────────────────────────────
        GradientItem(colors = listOf("#F093FB".toColorInt(), "#F5576C".toColorInt()), positions = listOf(0f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#FF6CAB".toColorInt(), "#7366FF".toColorInt()), positions = listOf(0f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#FD79A8".toColorInt(), "#A29BFE".toColorInt()), positions = listOf(0f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#FF0099".toColorInt(), "#493240".toColorInt()), positions = listOf(0f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#F953C6".toColorInt(), "#B91D73".toColorInt()), positions = listOf(0f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#FF61D2".toColorInt(), "#FE9090".toColorInt(), "#FFD700".toColorInt()), positions = listOf(0f, 0.5f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#E96443".toColorInt(), "#904E95".toColorInt()), positions = listOf(0f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#A18CD1".toColorInt(), "#FBC2EB".toColorInt()), positions = listOf(0f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#C471ED".toColorInt(), "#F64F59".toColorInt(), "#12C2E9".toColorInt()), positions = listOf(0f, 0.5f, 1f), angle = 135f, type = GradientType.LINEAR),

        // ── Red & Orange Fire ─────────────────────────────────────────────────
        GradientItem(colors = listOf("#FF512F".toColorInt(), "#DD2476".toColorInt()), positions = listOf(0f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#F83600".toColorInt(), "#F9D423".toColorInt()), positions = listOf(0f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#FC4A1A".toColorInt(), "#F7B733".toColorInt()), positions = listOf(0f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#FF416C".toColorInt(), "#FF4B2B".toColorInt()), positions = listOf(0f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#CB2D3E".toColorInt(), "#EF473A".toColorInt()), positions = listOf(0f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#FF0000".toColorInt(), "#FF6600".toColorInt(), "#FF9900".toColorInt()), positions = listOf(0f, 0.5f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#ED213A".toColorInt(), "#93291E".toColorInt()), positions = listOf(0f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#FF5F6D".toColorInt(), "#FFC371".toColorInt()), positions = listOf(0f, 1f), angle = 135f, type = GradientType.LINEAR),

        // ── Warm Amber & Yellow ───────────────────────────────────────────────
        GradientItem(colors = listOf("#F7971E".toColorInt(), "#FFD200".toColorInt()), positions = listOf(0f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#FFBE76".toColorInt(), "#FF7043".toColorInt()), positions = listOf(0f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#F6D365".toColorInt(), "#FDA085".toColorInt()), positions = listOf(0f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#FFD700".toColorInt(), "#FF8C00".toColorInt(), "#FF4500".toColorInt()), positions = listOf(0f, 0.5f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#F7BB97".toColorInt(), "#DD5E89".toColorInt()), positions = listOf(0f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#FCFF9E".toColorInt(), "#C67700".toColorInt()), positions = listOf(0f, 1f), angle = 135f, type = GradientType.LINEAR),

        // ── Green & Lime ──────────────────────────────────────────────────────
        GradientItem(colors = listOf("#11998E".toColorInt(), "#38EF7D".toColorInt()), positions = listOf(0f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#56AB2F".toColorInt(), "#A8E063".toColorInt()), positions = listOf(0f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#1D976C".toColorInt(), "#93F9B9".toColorInt()), positions = listOf(0f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#00B09B".toColorInt(), "#96C93D".toColorInt()), positions = listOf(0f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#43E97B".toColorInt(), "#38F9D7".toColorInt()), positions = listOf(0f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#008000".toColorInt(), "#00FF00".toColorInt(), "#ADFF2F".toColorInt()), positions = listOf(0f, 0.5f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#76B852".toColorInt(), "#8DC26F".toColorInt()), positions = listOf(0f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#134E5E".toColorInt(), "#71B280".toColorInt()), positions = listOf(0f, 1f), angle = 135f, type = GradientType.LINEAR),

        // ── Teal & Mint ───────────────────────────────────────────────────────
        GradientItem(colors = listOf("#00CDAC".toColorInt(), "#8DDAD5".toColorInt()), positions = listOf(0f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#96FBC4".toColorInt(), "#F9F586".toColorInt()), positions = listOf(0f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#0BD3D3".toColorInt(), "#F8FF00".toColorInt()), positions = listOf(0f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#00F260".toColorInt(), "#0575E6".toColorInt()), positions = listOf(0f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#43C6AC".toColorInt(), "#191654".toColorInt()), positions = listOf(0f, 1f), angle = 135f, type = GradientType.LINEAR),

        // ── Rainbow & Spectrum ────────────────────────────────────────────────
        GradientItem(colors = listOf("#FF0000".toColorInt(), "#FF7700".toColorInt(), "#FFFF00".toColorInt(), "#00FF00".toColorInt(), "#0000FF".toColorInt(), "#8B00FF".toColorInt()), positions = listOf(0f, 0.2f, 0.4f, 0.6f, 0.8f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#FF6B6B".toColorInt(), "#FEC89A".toColorInt(), "#FFE66D".toColorInt(), "#A8E6CF".toColorInt(), "#88D8FF".toColorInt()), positions = listOf(0f, 0.25f, 0.5f, 0.75f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#F64F59".toColorInt(), "#C471ED".toColorInt(), "#12C2E9".toColorInt()), positions = listOf(0f, 0.5f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#FC466B".toColorInt(), "#3F5EFB".toColorInt()), positions = listOf(0f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#00F5A0".toColorInt(), "#00D9F5".toColorInt()), positions = listOf(0f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#FBAB7E".toColorInt(), "#F7CE68".toColorInt()), positions = listOf(0f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#85FFBD".toColorInt(), "#FFFB7D".toColorInt()), positions = listOf(0f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#FF9A8B".toColorInt(), "#FF6A88".toColorInt(), "#FF99AC".toColorInt()), positions = listOf(0f, 0.5f, 1f), angle = 135f, type = GradientType.LINEAR),

        // ── Sunset & Dusk ─────────────────────────────────────────────────────
        GradientItem(colors = listOf("#FF9A9E".toColorInt(), "#FAD0C4".toColorInt()), positions = listOf(0f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#FFAFBD".toColorInt(), "#FFC3A0".toColorInt()), positions = listOf(0f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#FF6E7F".toColorInt(), "#BFE9FF".toColorInt()), positions = listOf(0f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#FDC830".toColorInt(), "#F37335".toColorInt()), positions = listOf(0f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#FC5C7D".toColorInt(), "#6A3093".toColorInt()), positions = listOf(0f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#F7971E".toColorInt(), "#FFD200".toColorInt(), "#FF6B6B".toColorInt()), positions = listOf(0f, 0.5f, 1f), angle = 135f, type = GradientType.LINEAR),

        // ── Soft & Pastel ─────────────────────────────────────────────────────
        GradientItem(colors = listOf("#FFECD2".toColorInt(), "#FCB69F".toColorInt()), positions = listOf(0f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#FFF1EB".toColorInt(), "#ACE0F9".toColorInt()), positions = listOf(0f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#FDDB92".toColorInt(), "#D1FDFF".toColorInt()), positions = listOf(0f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#FCCB90".toColorInt(), "#D57EEB".toColorInt()), positions = listOf(0f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#E0C3FC".toColorInt(), "#8EC5FC".toColorInt()), positions = listOf(0f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#D4FC79".toColorInt(), "#96E6A1".toColorInt()), positions = listOf(0f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#FFDEE9".toColorInt(), "#B5FFFC".toColorInt()), positions = listOf(0f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#E0F7FA".toColorInt(), "#B2EBF2".toColorInt()), positions = listOf(0f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#FAD0C4".toColorInt(), "#FFD1FF".toColorInt()), positions = listOf(0f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#A1FFCE".toColorInt(), "#FAFFD1".toColorInt()), positions = listOf(0f, 1f), angle = 135f, type = GradientType.LINEAR),

        // ── Metals — 6-stop ramps with a highlight band ───────────────────────
        GradientItem(colors = listOf("#613F00".toColorInt(), "#FFD86B".toColorInt(), "#FFF0C7".toColorInt(), "#FFD86B".toColorInt(), "#8A5A00".toColorInt(), "#AB8847".toColorInt()), positions = listOf(0f, 0.3f, 0.47f, 0.56f, 0.8f, 1f), angle = 90f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#6D402B".toColorInt(), "#F8C9B4".toColorInt(), "#FCEAE3".toColorInt(), "#F8C9B4".toColorInt(), "#9B5B3E".toColorInt(), "#B78974".toColorInt()), positions = listOf(0f, 0.3f, 0.47f, 0.56f, 0.8f, 1f), angle = 90f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#625B45".toColorInt(), "#F4F0E2".toColorInt(), "#FBF9F4".toColorInt(), "#F4F0E2".toColorInt(), "#8C8262".toColorInt(), "#ACA58E".toColorInt()), positions = listOf(0f, 0.3f, 0.47f, 0.56f, 0.8f, 1f), angle = 90f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#42300F".toColorInt(), "#D9B44A".toColorInt(), "#F1E3BA".toColorInt(), "#D9B44A".toColorInt(), "#5E4415".toColorInt(), "#8B7857".toColorInt()), positions = listOf(0f, 0.3f, 0.47f, 0.56f, 0.8f, 1f), angle = 90f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#766235".toColorInt(), "#F7E6BC".toColorInt(), "#FCF6E6".toColorInt(), "#F7E6BC".toColorInt(), "#A98C4B".toColorInt(), "#C1AC7D".toColorInt()), positions = listOf(0f, 0.3f, 0.47f, 0.56f, 0.8f, 1f), angle = 90f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#4B240D".toColorInt(), "#E8944F".toColorInt(), "#F6D6BC".toColorInt(), "#E8944F".toColorInt(), "#6B3312".toColorInt(), "#946C54".toColorInt()), positions = listOf(0f, 0.3f, 0.47f, 0.56f, 0.8f, 1f), angle = 90f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#341D08".toColorInt(), "#C58A3E".toColorInt(), "#E9D3B6".toColorInt(), "#C58A3E".toColorInt(), "#4A2A0C".toColorInt(), "#7D6650".toColorInt()), positions = listOf(0f, 0.3f, 0.47f, 0.56f, 0.8f, 1f), angle = 90f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#4D555B".toColorInt(), "#EDF1F4".toColorInt(), "#F8FAFB".toColorInt(), "#EDF1F4".toColorInt(), "#6E7A82".toColorInt(), "#979FA5".toColorInt()), positions = listOf(0f, 0.3f, 0.47f, 0.56f, 0.8f, 1f), angle = 90f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#181D20".toColorInt(), "#9AA5AD".toColorInt(), "#D9DDE0".toColorInt(), "#9AA5AD".toColorInt(), "#22292E".toColorInt(), "#606569".toColorInt()), positions = listOf(0f, 0.3f, 0.47f, 0.56f, 0.8f, 1f), angle = 90f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#6C7276".toColorInt(), "#F2F4F5".toColorInt(), "#FAFBFB".toColorInt(), "#F2F4F5".toColorInt(), "#9AA3A8".toColorInt(), "#B6BDC0".toColorInt()), positions = listOf(0f, 0.3f, 0.47f, 0.56f, 0.8f, 1f), angle = 90f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#613F00".toColorInt(), "#FFD86B".toColorInt(), "#FFF0C7".toColorInt(), "#FFD86B".toColorInt(), "#8A5A00".toColorInt(), "#AB8847".toColorInt()), positions = listOf(0f, 0.3f, 0.47f, 0.56f, 0.8f, 1f), angle = 0f, type = GradientType.SWEEP, sweepStartAngle = 200f),
        GradientItem(colors = listOf("#6D402B".toColorInt(), "#F8C9B4".toColorInt(), "#FCEAE3".toColorInt(), "#F8C9B4".toColorInt(), "#9B5B3E".toColorInt(), "#B78974".toColorInt()), positions = listOf(0f, 0.3f, 0.47f, 0.56f, 0.8f, 1f), angle = 0f, type = GradientType.SWEEP, sweepStartAngle = 200f),
        GradientItem(colors = listOf("#625B45".toColorInt(), "#F4F0E2".toColorInt(), "#FBF9F4".toColorInt(), "#F4F0E2".toColorInt(), "#8C8262".toColorInt(), "#ACA58E".toColorInt()), positions = listOf(0f, 0.3f, 0.47f, 0.56f, 0.8f, 1f), angle = 0f, type = GradientType.SWEEP, sweepStartAngle = 200f),
        GradientItem(colors = listOf("#42300F".toColorInt(), "#D9B44A".toColorInt(), "#F1E3BA".toColorInt(), "#D9B44A".toColorInt(), "#5E4415".toColorInt(), "#8B7857".toColorInt()), positions = listOf(0f, 0.3f, 0.47f, 0.56f, 0.8f, 1f), angle = 0f, type = GradientType.SWEEP, sweepStartAngle = 200f),
        GradientItem(colors = listOf("#766235".toColorInt(), "#F7E6BC".toColorInt(), "#FCF6E6".toColorInt(), "#F7E6BC".toColorInt(), "#A98C4B".toColorInt(), "#C1AC7D".toColorInt()), positions = listOf(0f, 0.3f, 0.47f, 0.56f, 0.8f, 1f), angle = 0f, type = GradientType.SWEEP, sweepStartAngle = 200f),
        GradientItem(colors = listOf("#4B240D".toColorInt(), "#E8944F".toColorInt(), "#F6D6BC".toColorInt(), "#E8944F".toColorInt(), "#6B3312".toColorInt(), "#946C54".toColorInt()), positions = listOf(0f, 0.3f, 0.47f, 0.56f, 0.8f, 1f), angle = 0f, type = GradientType.SWEEP, sweepStartAngle = 200f),

        // ── Radial glows — light coming from inside the letter ────────────────
        GradientItem(colors = listOf("#FFF3C4".toColorInt(), "#F2C765".toColorInt(), "#E8A317".toColorInt()), positions = listOf(0f, 0.55f, 1f), angle = 0f, type = GradientType.RADIAL, radialRadiusFactor = 0.62f),
        GradientItem(colors = listOf("#FFFFFF".toColorInt(), "#73CEF5".toColorInt(), "#00A6ED".toColorInt()), positions = listOf(0f, 0.55f, 1f), angle = 0f, type = GradientType.RADIAL, radialRadiusFactor = 0.62f),
        GradientItem(colors = listOf("#FFE3F1".toColorInt(), "#EE78B8".toColorInt(), "#E0218A".toColorInt()), positions = listOf(0f, 0.55f, 1f), angle = 0f, type = GradientType.RADIAL, radialRadiusFactor = 0.62f),
        GradientItem(colors = listOf("#EAFFD0".toColorInt(), "#8BC86D".toColorInt(), "#3E9B1C".toColorInt()), positions = listOf(0f, 0.55f, 1f), angle = 0f, type = GradientType.RADIAL, radialRadiusFactor = 0.62f),
        GradientItem(colors = listOf("#FFE9D6".toColorInt(), "#EF9971".toColorInt(), "#E2571E".toColorInt()), positions = listOf(0f, 0.55f, 1f), angle = 0f, type = GradientType.RADIAL, radialRadiusFactor = 0.62f),
        GradientItem(colors = listOf("#E6F7FF".toColorInt(), "#6EA1CE".toColorInt(), "#0B5AA6".toColorInt()), positions = listOf(0f, 0.55f, 1f), angle = 0f, type = GradientType.RADIAL, radialRadiusFactor = 0.62f),
        GradientItem(colors = listOf("#FFF0F0".toColorInt(), "#D46C7E".toColorInt(), "#B00020".toColorInt()), positions = listOf(0f, 0.55f, 1f), angle = 0f, type = GradientType.RADIAL, radialRadiusFactor = 0.62f),
        GradientItem(colors = listOf("#F3E9FF".toColorInt(), "#A883D6".toColorInt(), "#6A2FB5".toColorInt()), positions = listOf(0f, 0.55f, 1f), angle = 0f, type = GradientType.RADIAL, radialRadiusFactor = 0.62f),

        // ── Occasion — Ramadan, Eid, Pakistan, weddings ───────────────────────
        GradientItem(colors = listOf("#03301C".toColorInt(), "#0B6B3A".toColorInt(), "#1FA463".toColorInt()), positions = listOf(0f, 0.5f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#0B3B2E".toColorInt(), "#14795A".toColorInt(), "#E4C36A".toColorInt()), positions = listOf(0f, 0.5f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#0A2E1B".toColorInt(), "#E4C36A".toColorInt(), "#FFF3C4".toColorInt()), positions = listOf(0f, 0.5f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#F7E9C9".toColorInt(), "#E8C877".toColorInt(), "#B98A2E".toColorInt()), positions = listOf(0f, 0.5f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#FFE6EE".toColorInt(), "#F7B8CE".toColorInt(), "#C86B8E".toColorInt()), positions = listOf(0f, 0.5f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#E8F6EF".toColorInt(), "#B9E3D0".toColorInt(), "#6FBF9B".toColorInt()), positions = listOf(0f, 0.5f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#01411C".toColorInt(), "#0B6B3A".toColorInt(), "#FFFFFF".toColorInt()), positions = listOf(0f, 0.5f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#FFFFFF".toColorInt(), "#D8EADF".toColorInt(), "#01411C".toColorInt()), positions = listOf(0f, 0.5f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#4A0E1C".toColorInt(), "#8C1C2E".toColorInt(), "#D4A03C".toColorInt()), positions = listOf(0f, 0.5f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#5B1230".toColorInt(), "#9B2352".toColorInt(), "#F0C1D4".toColorInt()), positions = listOf(0f, 0.5f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#2A1B4A".toColorInt(), "#5B3FA0".toColorInt(), "#C9B6F5".toColorInt()), positions = listOf(0f, 0.5f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#123D2A".toColorInt(), "#2E7D52".toColorInt(), "#CDE8B5".toColorInt()), positions = listOf(0f, 0.5f, 1f), angle = 135f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#03301C".toColorInt(), "#0B6B3A".toColorInt(), "#1FA463".toColorInt()), positions = listOf(0f, 0.5f, 1f), angle = 45f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#0B3B2E".toColorInt(), "#14795A".toColorInt(), "#E4C36A".toColorInt()), positions = listOf(0f, 0.5f, 1f), angle = 45f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#0A2E1B".toColorInt(), "#E4C36A".toColorInt(), "#FFF3C4".toColorInt()), positions = listOf(0f, 0.5f, 1f), angle = 45f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#F7E9C9".toColorInt(), "#E8C877".toColorInt(), "#B98A2E".toColorInt()), positions = listOf(0f, 0.5f, 1f), angle = 45f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#FFE6EE".toColorInt(), "#F7B8CE".toColorInt(), "#C86B8E".toColorInt()), positions = listOf(0f, 0.5f, 1f), angle = 45f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#E8F6EF".toColorInt(), "#B9E3D0".toColorInt(), "#6FBF9B".toColorInt()), positions = listOf(0f, 0.5f, 1f), angle = 45f, type = GradientType.LINEAR),

        // ── Duotone — the same pair read differently by angle ─────────────────
        GradientItem(colors = listOf("#FF4D2E".toColorInt(), "#FF953C".toColorInt(), "#FFB400".toColorInt()), positions = listOf(0f, 0.52f, 1f), angle = 0f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#1B6DF0".toColorInt(), "#69B8F9".toColorInt(), "#7CE7FF".toColorInt()), positions = listOf(0f, 0.52f, 1f), angle = 45f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#12B886".toColorInt(), "#8BDFA2".toColorInt(), "#D8F999".toColorInt()), positions = listOf(0f, 0.52f, 1f), angle = 90f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#7A3FF2".toColorInt(), "#C872E2".toColorInt(), "#FF6EC7".toColorInt()), positions = listOf(0f, 0.52f, 1f), angle = 180f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#FF5A7A".toColorInt(), "#FFA197".toColorInt(), "#FFC48C".toColorInt()), positions = listOf(0f, 0.52f, 1f), angle = 270f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#0E7490".toColorInt(), "#75C0BD".toColorInt(), "#A7F3D0".toColorInt()), positions = listOf(0f, 0.52f, 1f), angle = 315f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#8E24AA".toColorInt(), "#D0729B".toColorInt(), "#FF8A65".toColorInt()), positions = listOf(0f, 0.52f, 1f), angle = 0f, type = GradientType.LINEAR),
        GradientItem(colors = listOf("#00695C".toColorInt(), "#74B3AC".toColorInt(), "#B2DFDB".toColorInt()), positions = listOf(0f, 0.52f, 1f), angle = 45f, type = GradientType.LINEAR),

        // ── Sweep — for chrome and holographic fills ──────────────────────────
        GradientItem(colors = listOf("#FF0080".toColorInt(), "#FFB300".toColorInt(), "#00E676".toColorInt(), "#00B0FF".toColorInt(), "#7C4DFF".toColorInt(), "#FF0080".toColorInt()), positions = listOf(0f, 0.2f, 0.4f, 0.6f, 0.8f, 1f), angle = 0f, type = GradientType.SWEEP),
        GradientItem(colors = listOf("#EDF1F4".toColorInt(), "#8E9BA5".toColorInt(), "#FFFFFF".toColorInt(), "#5A6670".toColorInt(), "#DCE3E8".toColorInt(), "#EDF1F4".toColorInt()), positions = listOf(0f, 0.2f, 0.42f, 0.66f, 0.85f, 1f), angle = 0f, type = GradientType.SWEEP),
        GradientItem(colors = listOf("#2B2B2B".toColorInt(), "#9E9E9E".toColorInt(), "#FFFFFF".toColorInt(), "#6E6E6E".toColorInt(), "#141414".toColorInt(), "#2B2B2B".toColorInt()), positions = listOf(0f, 0.22f, 0.45f, 0.68f, 0.88f, 1f), angle = 0f, type = GradientType.SWEEP),
    )
}