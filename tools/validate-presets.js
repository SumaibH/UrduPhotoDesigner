#!/usr/bin/env node
/*
 * Checks every text preset lockup before it ships.
 *
 * A preset layer names its style and its font by id. Nothing at runtime fails loudly when
 * one of those ids is wrong — by design: a missing style leaves the layer unstyled and a
 * missing font falls back to the default face, because content going out of step with the
 * app should never take somebody's project down with it. The cost of that kindness is
 * that a typo ships silently and shows up as a card that looks subtly wrong. This is
 * where it is supposed to be caught instead.
 *
 *   node tools/validate-presets.js
 *
 * Exits non-zero if anything is broken. Warnings do not fail the run.
 */

const fs = require('fs');
const path = require('path');

const ROOT = path.resolve(__dirname, '..');
const LOCKUPS_DIR = path.join(ROOT, 'app/src/main/assets/presets/lockups');
const STYLES_JSON = path.join(ROOT, 'app/src/main/assets/presets/text_styles.json');
const GRADIENTS_KT = path.join(ROOT, 'app/src/main/java/com/webscare/urducanvas/common/utils/GradientPresets.kt');
const FONTS_JSON = path.join(__dirname, 'font-inventory.json');

const errors = [];
const warnings = [];
const err = (where, msg) => errors.push(`${where}: ${msg}`);
const warn = (where, msg) => warnings.push(`${where}: ${msg}`);

const readJson = (file) =>
  JSON.parse(fs.readFileSync(file, 'utf8').replace(/^\uFEFF/, ''));

// ── What the ids are allowed to point at ────────────────────────────────────

const styles = readJson(STYLES_JSON);
const styleIds = new Set(styles.map((s) => s.id));
const styleById = new Map(styles.map((s) => [s.id, s]));

/** Perceived brightness of a #RRGGBB or #AARRGGBB colour, 0..1. Matches the renderer's. */
function luminance(hex) {
  if (!hex) return null;
  const c = hex.replace('#', '');
  const s = c.length === 8 ? c.slice(2) : c;
  if (s.length !== 6) return null;
  const [r, g, b] = [0, 2, 4].map((i) => parseInt(s.slice(i, i + 2), 16));
  return (0.2126 * r + 0.7152 * g + 0.0722 * b) / 255;
}
const LIGHT = 0.62;

/*
 * A card carries one plate, light or dark, chosen from the styles on it. Two styles in
 * the catalogue cannot be read on either: a near-white fill whose emboss highlight and
 * shadow are also near-white, held together only by a hairline dark stroke that the
 * emboss then buries. On the light plate the fill disappears; on the dark one the stroke
 * does, and the offsets read as doubled glyphs. Both are called "Paper Letterpress
 * White" and both want a mid-tone paper that a preset card does not have.
 *
 * Two of six hundred and fifty, so this names the shape rather than banning a list.
 */
function unreadableOnAnyPlate(style) {
  if (!style) return false;
  if (style.hasOuterGlow && style.outerGlowRadius > 0) return false;
  const mid = style.textGradientColors
    ? style.textGradientColors[Math.floor(style.textGradientColors.length / 2)]
    : null;
  const fill = luminance(mid || style.textColor);
  if (fill === null || fill <= LIGHT) return false;

  const contour = style.strokeWidth > 0 ? luminance(style.strokeColor) : null;
  if (contour === null || contour > LIGHT) return false; // light ink, no dark contour: dark plate handles it

  // Dark contour, so the light plate is chosen — unless a light emboss swamps it.
  if (!style.hasEmboss) return false;
  const hi = luminance(style.embossHighlightColor);
  const lo = luminance(style.embossShadowColor);
  return hi > LIGHT && lo > LIGHT && style.embossDepth > style.strokeWidth;
}

/** The plate a style needs behind it, matching TextStyleThumbnailRenderer.wantsDarkPanel. */
function wantsDarkPlate(style) {
  if (!style) return false;
  if (style.hasOuterGlow && style.outerGlowRadius > 0) return true;
  const mid = style.textGradientColors
    ? style.textGradientColors[Math.floor(style.textGradientColors.length / 2)]
    : null;
  const fill = luminance(mid || style.textColor);
  if (fill === null || fill <= LIGHT) return false;
  const contour = style.strokeWidth > 0 ? luminance(style.strokeColor) : null;
  return contour === null || contour > LIGHT;
}

const PLATE_LIGHT = luminance('#F2F3F0');
const PLATE_DARK = luminance('#1E211F');

/**
 * How much a line stands out from the plate its card will get, 0..1.
 *
 * The card carries one plate for every line on it, so a lockup pairing a gold line
 * with a charcoal one strands whichever of the two the plate is wrong for. Reported
 * per layer, because that is the line the reader loses.
 *
 * Styles carrying their own label plate or an outer glow are skipped — they bring
 * their own ground and are legible on either.
 */
function plateContrast(style, plateLum) {
  if (!style || style.hasLabel) return null;
  if (style.hasOuterGlow && style.outerGlowRadius > 0) return null;
  const mid = style.textGradientColors
    ? style.textGradientColors[Math.floor(style.textGradientColors.length / 2)]
    : null;
  const fill = luminance(mid || style.textColor);
  if (fill === null) return null;
  const contour = style.strokeWidth > 0 ? luminance(style.strokeColor) : null;
  return Math.max(
    Math.abs(fill - plateLum),
    contour === null ? 0 : Math.abs(contour - plateLum)
  );
}

// The gradient catalogue is Kotlin, not data, so it is counted rather than parsed —
// a gradientId is an index into that list and all this check needs is its length.
const gradientCount = (fs.readFileSync(GRADIENTS_KT, 'utf8').match(/GradientItem\(/g) || []).length;

let fontIds = null;
try {
  fontIds = new Set(readJson(FONTS_JSON).fonts.map((f) => f.file_name));
} catch (e) {
  warn('fonts', `no inventory at ${path.relative(ROOT, FONTS_JSON)} — font ids unchecked`);
}

// ── Per-file checks ─────────────────────────────────────────────────────────

if (!fs.existsSync(LOCKUPS_DIR)) {
  console.log('No lockups directory yet — nothing to validate.');
  process.exit(0);
}

const files = fs.readdirSync(LOCKUPS_DIR).filter((f) => f.endsWith('.json') && f !== 'index.json');
const seenIds = new Map();
/*
 * Lockups by what they actually are, ignoring id and name.
 *
 * This is the check the style catalogue did not have: 317 of its 650 entries were
 * byte-identical to another one once you looked past the id, and nobody noticed until
 * somebody counted. A generated catalogue can make that mistake far faster than a
 * hand-written one, so it is checked on every run.
 */
const seenShapes = new Map();
let presetCount = 0;
let layerCount = 0;

const shapeOf = (p) =>
  JSON.stringify(
    (p.layers || []).map((l) => [
      l.text, l.fontId, l.styleId, l.xPct, l.yPct, l.widthPct, l.rotation || 0,
      l.override ? JSON.stringify(l.override) : null,
    ])
  );

for (const file of files) {
  const where = `lockups/${file}`;
  let presets;
  try {
    presets = readJson(path.join(LOCKUPS_DIR, file));
  } catch (e) {
    err(where, `not valid JSON — ${e.message}`);
    continue;
  }
  if (!Array.isArray(presets)) {
    err(where, 'expected a top-level array of presets');
    continue;
  }

  // The file a preset is in decides its category, so the id ought to say the same
  // thing — it is what findPresetById guesses with to avoid parsing all ten files.
  const expectedPrefix = path.basename(file, '.json') + '_';

  presets.forEach((p, i) => {
    presetCount++;
    const at = `${where}[${i}]${p && p.id ? ` ${p.id}` : ''}`;

    if (!p || typeof p !== 'object') return err(at, 'not an object');
    if (!p.id) return err(at, 'has no id');

    if (seenIds.has(p.id)) err(at, `duplicate id, also in ${seenIds.get(p.id)}`);
    else seenIds.set(p.id, where);

    if (!p.id.startsWith(expectedPrefix)) {
      warn(at, `id does not start with "${expectedPrefix}" — findPresetById will fall back to a full scan`);
    }
    if (!p.name) warn(at, 'has no name');
    if (p.aspect !== undefined && !(p.aspect > 0)) err(at, `aspect must be positive, got ${p.aspect}`);
    if (p.background !== undefined) {
      err(at, 'carries a background — presets are text only');
    }

    if (!Array.isArray(p.layers) || p.layers.length === 0) {
      return err(at, 'has no layers, so it would insert nothing');
    }

    const shape = shapeOf(p);
    if (seenShapes.has(shape)) {
      err(at, `is identical to ${seenShapes.get(shape)} apart from its id and name`);
    } else {
      seenShapes.set(shape, p.id);
    }

    // Which plate this lockup's card will get, and therefore what every line on it
    // has to stand out from.
    const layerStyles = p.layers.map((l) => styleById.get(l.styleId)).filter(Boolean);
    const plateLum =
      layerStyles.length && layerStyles.every(wantsDarkPlate) ? PLATE_DARK : PLATE_LIGHT;

    p.layers.forEach((l, j) => {
      layerCount++;
      const la = `${at} layer[${j}]`;
      if (!l || typeof l !== 'object') return err(la, 'not an object');
      if (!l.text || !String(l.text).trim()) err(la, 'has no text');

      if (l.styleId && !styleIds.has(l.styleId)) {
        err(la, `styleId "${l.styleId}" is not in text_styles.json`);
      } else if (l.styleId && unreadableOnAnyPlate(styleById.get(l.styleId))) {
        err(la, `styleId "${l.styleId}" cannot be read on either card plate — pick another`);
      }
      if (!l.styleId) warn(la, 'names no style, so it renders unstyled');

      const contrast = plateContrast(styleById.get(l.styleId), plateLum);
      if (contrast !== null && contrast < 0.25) {
        warn(
          la,
          `barely stands out from the ${plateLum > 0.5 ? 'light' : 'dark'} plate its card gets ` +
            `(${contrast.toFixed(2)}) — pair it with a line that wants the same plate`
        );
      }

      if (l.fontId && fontIds && !fontIds.has(l.fontId)) {
        // A warning, not an error: the inventory is a snapshot of the fonts table
        // taken off a device and is known to be short of the full list, so an id
        // missing from it is suspicious rather than proven wrong.
        warn(la, `fontId "${l.fontId}" is not in the font inventory — check the spelling`);
      }
      if (!l.fontId) warn(la, 'names no font, so it renders in the fallback face');

      for (const k of ['xPct', 'yPct']) {
        if (l[k] === undefined) continue;
        if (!(l[k] >= 0 && l[k] <= 1)) err(la, `${k} must be within 0..1, got ${l[k]}`);
      }
      if (l.widthPct !== undefined && !(l.widthPct > 0 && l.widthPct <= 1)) {
        err(la, `widthPct must be within 0..1, got ${l.widthPct}`);
      }
      if (l.align !== undefined && !['LEFT', 'CENTER', 'RIGHT'].includes(l.align)) {
        err(la, `align "${l.align}" is not LEFT, CENTER or RIGHT`);
      }

      if (l.override) {
        if (typeof l.override !== 'object' || Array.isArray(l.override)) {
          err(la, 'override must be an object of style fields');
        } else if (l.override.gradientId !== undefined) {
          const g = l.override.gradientId;
          if (!Number.isInteger(g) || g < 0 || g >= gradientCount) {
            err(la, `override gradientId ${g} is outside the catalogue of ${gradientCount}`);
          }
        }
      }
    });
  });
}

// Styles may name a gradient too, and that id comes from the same catalogue.
styles.forEach((s) => {
  if (s.gradientId === undefined) return;
  if (!Number.isInteger(s.gradientId) || s.gradientId < 0 || s.gradientId >= gradientCount) {
    err(`text_styles.json ${s.id}`, `gradientId ${s.gradientId} is outside the catalogue of ${gradientCount}`);
  }
});

// ── Report ──────────────────────────────────────────────────────────────────

console.log(
  `Checked ${presetCount} presets (${layerCount} layers) in ${files.length} files ` +
  `against ${styleIds.size} styles, ${gradientCount} gradients` +
  (fontIds ? `, ${fontIds.size} fonts` : '') + '.'
);

if (warnings.length) {
  console.log(`\n${warnings.length} warning(s):`);
  warnings.forEach((w) => console.log('  ! ' + w));
}
if (errors.length) {
  console.log(`\n${errors.length} error(s):`);
  errors.forEach((e) => console.log('  x ' + e));
  process.exit(1);
}
console.log('\nAll ids resolve.');
