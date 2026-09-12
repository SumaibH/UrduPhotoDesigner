#!/usr/bin/env node
/*
 * Builds the lockup catalogue from the three authored sets in preset-content.js.
 *
 *   node tools/generate-presets.js
 *
 * Writes one file per category into app/src/main/assets/presets/lockups/, then leaves
 * the result to be curated on a device. Generated and curated is how you get volume
 * that does not feel mechanical; generated and shipped is how the style catalogue
 * ended up with 317 duplicates.
 *
 * Deterministic: same inputs, same output, so a re-run after an edit produces a diff
 * you can read rather than a reshuffle.
 *
 * Hand-tuned lockups are preserved. Anything in a category file without a
 * "generated": true marker is kept as authored and the generated ones fill in around
 * it, which is what lets a dozen heroes lead each list.
 */

const fs = require('fs');
const path = require('path');

const { LAYOUTS, PHRASES, CATEGORIES } = require('./preset-content.js');

const ROOT = path.resolve(__dirname, '..');
const LOCKUPS_DIR = path.join(ROOT, 'app/src/main/assets/presets/lockups');
const STYLES_JSON = path.join(ROOT, 'app/src/main/assets/presets/text_styles.json');
const FONTS_JSON = path.join(__dirname, 'font-inventory.json');

const readJson = (f) => JSON.parse(fs.readFileSync(f, 'utf8').replace(/^﻿/, ''));

// ── Pools ───────────────────────────────────────────────────────────────────

const styles = readJson(STYLES_JSON);
const fonts = readJson(FONTS_JSON).fonts;

/** Perceived brightness, matching the renderer's plate rule. */
function luminance(hex) {
  if (!hex) return null;
  const c = hex.replace('#', '');
  const s = c.length === 8 ? c.slice(2) : c;
  if (s.length !== 6) return null;
  const [r, g, b] = [0, 2, 4].map((i) => parseInt(s.slice(i, i + 2), 16));
  return (0.2126 * r + 0.7152 * g + 0.0722 * b) / 255;
}

/*
 * The two "Paper Letterpress White" styles cannot be read on either card plate —
 * near-white fill and near-white emboss over a hairline dark stroke the emboss
 * swallows. Excluded from every pool rather than caught later by the validator.
 */
function unusable(style) {
  if (style.hasOuterGlow && style.outerGlowRadius > 0) return false;
  const mid = style.textGradientColors
    ? style.textGradientColors[Math.floor(style.textGradientColors.length / 2)]
    : null;
  const fill = luminance(mid || style.textColor);
  if (fill === null || fill <= 0.62) return false;
  const contour = style.strokeWidth > 0 ? luminance(style.strokeColor) : null;
  if (contour === null || contour > 0.62) return false;
  if (!style.hasEmboss) return false;
  return (
    luminance(style.embossHighlightColor) > 0.62 &&
    luminance(style.embossShadowColor) > 0.62 &&
    style.embossDepth > style.strokeWidth
  );
}

/** Style ids on a given shelf, e.g. "gold" → gold_001…gold_053. */
function stylePool(prefix) {
  return styles
    .filter((s) => s.id.startsWith(prefix + '_') && !unusable(s))
    .map((s) => s.id);
}

const styleById = new Map(styles.map((s) => [s.id, s]));

/**
 * Which plate a style needs behind it, matching the renderer's rule exactly.
 *
 * A card carries one plate for all its lines, so this is what decides whether two
 * styles can share a lockup: gold on the dark plate and charcoal minimal on the light
 * one are both handsome, and putting them on the same card makes one of them vanish.
 * 42 layers of the first generated run were pale gold on a light plate for exactly
 * this reason — the plate was right for the second line and wrong for the first.
 */
function wantsDarkPlate(id) {
  const s = styleById.get(id);
  if (!s) return false;
  if (s.hasOuterGlow && s.outerGlowRadius > 0) return true;
  const mid = s.textGradientColors
    ? s.textGradientColors[Math.floor(s.textGradientColors.length / 2)]
    : null;
  const fill = luminance(mid || s.textColor);
  if (fill === null || fill <= 0.62) return false;
  const contour = s.strokeWidth > 0 ? luminance(s.strokeColor) : null;
  return contour === null || contour > 0.62;
}

/**
 * A style for the second line that can share a plate with the first.
 *
 * Searches the preferred shelf first, then the category's other shelves, because a
 * whole shelf can disagree: every Minimal style is dark ink, so a gold first line
 * paired against Minimal has nowhere to go within that shelf and the lockup ends up
 * with a light plate its gold cannot be seen on. Falling through to the next shelf
 * costs the preferred pairing and saves the card.
 *
 * Returns the preferred pick unchanged if nothing in the category agrees — a lockup
 * with one awkward line still beats no lockup.
 */
function agreeingStyle(pools, preferred, start, wantDark) {
  const ordered = [pools[preferred], ...pools.filter((_, i) => i !== preferred)];
  for (const pool of ordered) {
    for (let i = 0; i < pool.length; i++) {
      const candidate = pool[(start + i) % pool.length];
      if (wantsDarkPlate(candidate) === wantDark) return candidate;
    }
  }
  return pools[preferred][start % pools[preferred].length];
}

/** Urdu font file names filed under one of [categories]. */
function fontPool(categories) {
  const pool = fonts
    .filter((f) => categories.includes(f.font_category))
    .filter((f) => f.font_language === 'Urdu')
    .map((f) => f.file_name);
  // English display faces cannot set Urdu, so a category whose pools are all English
  // would silently produce lockups in a fallback face. Fall back to Nastaleeq, which
  // every one of these phrases is at home in.
  return pool.length
    ? pool
    : fonts.filter((f) => f.font_category === 'Nastaleeq').map((f) => f.file_name);
}

// ── Generation ──────────────────────────────────────────────────────────────

/**
 * How many times one phrase may appear in a category.
 *
 * The lever that decides whether a shelf reads as a catalogue or as one idea
 * repeated. Three means Ramadan's twenty-one phrase sets can reach fifty while no
 * phrase dominates, and each repeat is a different layout, style and face.
 */
const MAX_PER_PHRASE = 3;

/**
 * The layouts a category is allowed to draw on.
 *
 * Every layout carries a `pool`, defaulting to "core". A category names the pools it
 * wants and gets those layouts in declaration order, so adding a layout to a pool a
 * category does not name cannot move that category's output by a pixel.
 *
 * That is the whole point of the field. Layout choice is `candidates[... % length]`, so
 * appending one 3-line layout to a shared list would re-cut every existing lockup that
 * has three lines — same ids, different geometry, which is worse than a preset
 * disappearing out of somebody's Recents because it disappears quietly. The ten original
 * categories name no pools and therefore stay on "core" exactly as generated.
 */
function layoutsFor(config) {
  const allowed = new Set(config.layouts || ['core']);
  return LAYOUTS.filter((l) => allowed.has(l.pool || 'core'));
}

function generateCategory(name, config, startAt) {
  const phrases = PHRASES[name] || [];
  if (!phrases.length) return [];
  const layouts = layoutsFor(config);

  const primaryPools = config.primary.map(stylePool).filter((p) => p.length);
  const secondaryPools = config.secondary.map(stylePool).filter((p) => p.length);
  const faces = fontPool(config.fonts);
  if (!primaryPools.length || !faces.length) return [];

  const out = [];
  const used = new Set(); // phrase+layout, so a phrase never repeats a geometry
  let n = startAt;

  // Round by round rather than phrase by phrase, so the shelf opens with every
  // phrase once before any of them comes back.
  for (let round = 0; round < MAX_PER_PHRASE && out.length < config.target; round++) {
    for (let p = 0; p < phrases.length && out.length < config.target; p++) {
      const lines = phrases[p];
      const candidates = layouts.filter((l) => l.lines === lines.length);
      if (!candidates.length) continue;

      // Offset by the round so the second appearance of a phrase is a different
      // geometry from the first, and by the phrase index so neighbours differ too.
      const layout = candidates[(p + round * 2 + Math.floor(p / 3)) % candidates.length];
      const key = `${p}:${layout.id}`;
      if (used.has(key)) continue;
      used.add(key);

      // Walk the pools with a stride that is coprime with nothing in particular —
      // it only has to move, and to move differently for the first and second line.
      const primaryPool = primaryPools[(p + round) % primaryPools.length];
      const pools = secondaryPools.length ? secondaryPools : [primaryPool];
      const preferred = (p + round + 1) % pools.length;
      const primaryStyle = primaryPool[(p * 5 + round * 11) % primaryPool.length];
      // The second line has to want the same plate as the first, or one of them is
      // drawn on a ground it cannot be read against.
      const secondaryStyle = agreeingStyle(
        pools,
        preferred,
        p * 7 + round * 13 + 3,
        wantsDarkPlate(primaryStyle)
      );
      const face = faces[(p + round * 3) % faces.length];

      n += 1;
      out.push({
        id: `${name}_${String(n).padStart(3, '0')}`,
        name: `${lines.join(' ')} — ${layout.id}`,
        generated: true,
        aspect: 1.0,
        layers: lines.map((text, i) => {
          const geom = layout.layers[i];
          const layer = {
            text,
            fontId: face,
            // The first line carries the treatment; the rest take the quieter one, so
            // the eye is told what to read first.
            styleId: i === 0 ? primaryStyle : secondaryStyle,
            xPct: geom.xPct,
            yPct: geom.yPct,
            widthPct: geom.widthPct,
            rotation: geom.rotation || 0,
            align: 'CENTER',
          };
          return layer;
        }),
      });
    }
  }
  return out;
}

// ── Write ───────────────────────────────────────────────────────────────────

if (!fs.existsSync(LOCKUPS_DIR)) fs.mkdirSync(LOCKUPS_DIR, { recursive: true });

let total = 0;
const summary = [];

for (const [name, config] of Object.entries(CATEGORIES)) {
  const file = path.join(LOCKUPS_DIR, `${name}.json`);

  // Hand-written lockups lead the list and survive every re-run.
  let handWritten = [];
  if (fs.existsSync(file)) {
    try {
      handWritten = readJson(file).filter((p) => !p.generated);
    } catch (e) {
      console.error(`  ! ${name}.json could not be read, treating as empty: ${e.message}`);
    }
  }

  // Numbering picks up after the hand-written ones rather than colliding with them —
  // colliding candidates used to be dropped, which quietly threw away the best of the
  // first round for any category that had heroes written by hand.
  const highest = handWritten.reduce((max, p) => {
    const suffix = parseInt(String(p.id).split("_").pop(), 10);
    return Number.isFinite(suffix) && suffix > max ? suffix : max;
  }, 0);
  const generated = generateCategory(name, config, highest)
    .slice(0, Math.max(0, config.target - handWritten.length));

  const all = [...handWritten, ...generated];
  fs.writeFileSync(file, JSON.stringify(all, null, 2) + '\n');

  total += all.length;
  summary.push(
    `  ${name.padEnd(12)} ${String(all.length).padStart(3)}  ` +
      `(${handWritten.length} hand-written, ${generated.length} generated)  target ${config.target}`
  );
}

console.log(`Wrote ${total} lockups across ${Object.keys(CATEGORIES).length} categories:`);
summary.forEach((s) => console.log(s));
console.log('\nNow run: node tools/validate-presets.js');
