/*
 * The three authored sets the lockup catalogue is generated from.
 *
 * Phrases, layouts and style pairings are kept apart on purpose. Each is small enough
 * to read and argue with, and the catalogue is their product — which is how you get
 * three hundred and sixty-five lockups without writing three hundred and sixty-five
 * of them by hand, and without them all looking like each other either.
 *
 * The rule that keeps it from feeling mechanical lives in the generator rather than
 * here: a phrase may be used only a few times, and never twice in the same layout.
 */

// ── Layout templates ────────────────────────────────────────────────────────
//
// Pure geometry. No text, no colour, no font — those come from the other two sets.
// Positions are fractions of the preset's own box, so they mean the same thing on a
// square post and on a story. `lines` is how many phrases the layout needs.

const LAYOUTS = [
  {
    id: 'single',
    lines: 1,
    layers: [{ xPct: 0.5, yPct: 0.5, widthPct: 0.8 }],
  },
  {
    id: 'single-wide',
    lines: 1,
    // Not 0.9: measureText sizes the glyphs, and a style with extrusion or a stroke
    // draws past them. At 0.9 the widest phrases ran off the edge of the card.
    layers: [{ xPct: 0.5, yPct: 0.5, widthPct: 0.84 }],
  },
  {
    id: 'single-tilted',
    lines: 1,
    layers: [{ xPct: 0.5, yPct: 0.5, widthPct: 0.82, rotation: -5 }],
  },
  {
    id: 'stacked-centre',
    lines: 2,
    layers: [
      { xPct: 0.5, yPct: 0.38, widthPct: 0.72 },
      { xPct: 0.5, yPct: 0.63, widthPct: 0.52 },
    ],
  },
  {
    id: 'big-over-small',
    lines: 2,
    layers: [
      { xPct: 0.5, yPct: 0.43, widthPct: 0.82 },
      { xPct: 0.5, yPct: 0.67, widthPct: 0.38 },
    ],
  },
  {
    id: 'small-over-big',
    lines: 2,
    layers: [
      { xPct: 0.5, yPct: 0.32, widthPct: 0.34 },
      { xPct: 0.5, yPct: 0.58, widthPct: 0.80 },
    ],
  },
  {
    id: 'offset-diagonal',
    lines: 2,
    layers: [
      { xPct: 0.38, yPct: 0.34, widthPct: 0.44, rotation: -6 },
      { xPct: 0.6, yPct: 0.63, widthPct: 0.56, rotation: -6 },
    ],
  },
  {
    id: 'baseline-shifted',
    lines: 2,
    layers: [
      { xPct: 0.42, yPct: 0.42, widthPct: 0.52 },
      { xPct: 0.6, yPct: 0.62, widthPct: 0.46 },
    ],
  },
  {
    id: 'wide-with-kicker',
    lines: 2,
    layers: [
      { xPct: 0.5, yPct: 0.45, widthPct: 0.84 },
      { xPct: 0.5, yPct: 0.68, widthPct: 0.32 },
    ],
  },
  {
    id: 'three-stack',
    lines: 3,
    layers: [
      { xPct: 0.5, yPct: 0.28, widthPct: 0.44 },
      { xPct: 0.5, yPct: 0.5, widthPct: 0.82 },
      { xPct: 0.5, yPct: 0.72, widthPct: 0.36 },
    ],
  },
  {
    id: 'three-arch',
    lines: 3,
    layers: [
      { xPct: 0.5, yPct: 0.26, widthPct: 0.34 },
      { xPct: 0.5, yPct: 0.49, widthPct: 0.82 },
      { xPct: 0.5, yPct: 0.73, widthPct: 0.5 },
    ],
  },
];

// ── Phrases ─────────────────────────────────────────────────────────────────
//
// Each entry is the lines of one lockup, in reading order. Sets of one, two and three
// lines are all here; the generator only pairs a set with a layout that wants that
// many lines, so a two-line phrase never gets squeezed into a single-line geometry.

const PHRASES = {
  ramadan: [
    ['رمضان', 'مبارک'],
    ['رمضان', 'کریم'],
    ['ماہِ رمضان', 'مبارک'],
    ['رمضان المبارک'],
    ['خوش آمدید', 'ماہِ رمضان'],
    ['شبِ قدر', 'مبارک'],
    ['الوداع', 'ماہِ رمضان'],
    ['رمضان مبارک'],
    ['رمضان کریم'],
    ['ماہِ صیام', 'مبارک'],
    ['سحری و افطاری'],
    ['رحمتوں کا مہینہ'],
    ['مغفرت کا مہینہ'],
    ['پہلا عشرہ', 'رحمت'],
    ['دوسرا عشرہ', 'مغفرت'],
    ['تیسرا عشرہ', 'نجات'],
    ['افطار', 'مبارک'],
    ['روزہ', 'مبارک'],
    ['جمعۃ الوداع'],
    ['اہلاً', 'رمضان', 'مبارک'],
    ['ماہِ', 'رمضان', 'المبارک'],
  ],
  eid: [
    ['عید', 'مبارک'],
    ['عید الفطر', 'مبارک'],
    ['عید الاضحیٰ', 'مبارک'],
    ['عید سعید'],
    ['چاند رات', 'مبارک'],
    ['بقر عید', 'مبارک'],
    ['عید مبارک'],
    ['عیدِ قرباں', 'مبارک'],
    ['میٹھی عید', 'مبارک'],
    ['نمازِ عید', 'مبارک'],
    ['عید کی خوشیاں'],
    ['خوشیوں بھری عید'],
    ['عیدی', 'مبارک'],
    ['سب کو عید مبارک'],
    ['عید آئی'],
    ['تقبل اللہ منا و منکم'],
    ['عید', 'الفطر', 'مبارک'],
    ['عید', 'الاضحیٰ', 'مبارک'],
    ['چاند', 'رات', 'مبارک'],
  ],
  islamic: [
    ['اللہ اکبر'],
    ['بسم اللہ الرحمٰن الرحیم'],
    ['ماشاء اللہ'],
    ['سبحان اللہ'],
    ['الحمد للہ'],
    ['جمعہ', 'مبارک'],
    ['درود شریف'],
    ['میلادُ النبی ﷺ'],
    ['محرم الحرام'],
    ['شبِ معراج'],
    ['شبِ برات'],
    ['لا الہ الا اللہ'],
    ['یا اللہ'],
    ['استغفر اللہ'],
    ['اسلام علیکم'],
    ['نمازِ جمعہ', 'مبارک'],
    ['ربیع الاول', 'مبارک'],
    ['عاشورہ', 'محرم'],
    ['اللہ ہی کافی ہے'],
    ['یا', 'رسول اللہ', 'ﷺ'],
    ['بسم اللہ', 'الرحمٰن', 'الرحیم'],
  ],
  wedding: [
    ['شادی', 'مبارک'],
    ['نکاح', 'مبارک'],
    ['مبارک ہو'],
    ['مہندی'],
    ['بارات'],
    ['ولیمہ'],
    ['رشتۂ ازدواج'],
    ['شادی مبارک'],
    ['دعوتِ ولیمہ'],
    ['تقریبِ نکاح'],
    ['خوشیوں کا دن'],
    ['سالگرہِ شادی', 'مبارک'],
    ['ہمارا', 'نکاح'],
    ['مہندی', 'کی رات'],
    ['بارات', 'مبارک'],
    ['شادی', 'کی', 'مبارکباد'],
  ],
  pakistan: [
    ['جشنِ آزادی', 'مبارک'],
    ['پاکستان', 'زندہ باد'],
    ['یومِ آزادی'],
    ['۱۴ اگست'],
    ['یومِ پاکستان'],
    ['یومِ دفاع'],
    ['قائدِ اعظم'],
    ['پاکستان زندہ باد'],
    ['سبز ہلالی پرچم'],
    ['میرا پاکستان'],
    ['آزادی', 'مبارک'],
    ['۲۳ مارچ'],
    ['۶ ستمبر'],
    ['ہم پاکستانی'],
    ['جشنِ', 'آزادی', 'مبارک'],
    ['پاکستان', 'زندہ', 'باد'],
  ],
  birthday: [
    ['سالگرہ', 'مبارک'],
    ['جنم دن', 'مبارک'],
    ['ہیپی برتھ ڈے'],
    ['سالگرہ کی مبارکباد'],
    ['سالگرہ مبارک'],
    ['جنم دن مبارک ہو'],
    ['خوش رہو'],
    ['سال گرہ', 'مبارک ہو'],
    ['سالگرہ', 'مبارک', 'ہو'],
    ['ڈھیروں', 'مبارکباد'],
    ['مبارک ہو', 'سالگرہ'],
    ['آپ کو', 'سالگرہ', 'مبارک'],
  ],
  business: [
    ['افتتاح'],
    ['خصوصی پیشکش'],
    ['نئی برانچ'],
    ['رابطہ کریں'],
    ['خدمت میں حاضر'],
    ['گرینڈ اوپننگ'],
    ['ہماری خدمات'],
    ['آج ہی رابطہ کریں'],
    ['نیا کاروبار'],
    ['کھل گیا'],
    ['افتتاح', 'مبارک'],
    ['خصوصی', 'پیشکش'],
  ],
  quotes: [
    ['اقوالِ زریں'],
    ['شعر و شاعری'],
    ['غزل'],
    ['نصیحت'],
    ['کہاوت'],
    ['سوچ بدلو'],
    ['زندگی'],
    ['محبت'],
    ['امید'],
    ['حوصلہ'],
    ['اقوالِ', 'زریں'],
    ['شعر و', 'شاعری'],
  ],
  sale: [
    ['سیل'],
    ['بڑی سیل'],
    ['خصوصی رعایت'],
    ['۵۰٪ رعایت'],
    ['آخری موقع'],
    ['مفت ڈیلیوری'],
    ['کلیئرنس سیل'],
    ['آدھی قیمت'],
    ['سیل', 'شروع'],
    ['بڑی', 'سیل'],
  ],
  condolence: [
    ['اِنّا لِلہ وَاِنّا اِلَیہِ رَاجِعُون'],
    ['تعزیت'],
    ['دعائے مغفرت'],
    ['ایصالِ ثواب'],
    ['قرآن خوانی'],
    ['رسمِ قل'],
    ['چہلم'],
    ['اللہ مغفرت فرمائے'],
  ],
};

// ── Style pairings ──────────────────────────────────────────────────────────
//
// Which shelves of the style catalogue each occasion draws on, and how many lockups
// the category is worth. The generator walks the pools so consecutive lockups in a
// category do not wear the same style family.
//
// `secondary` is deliberately quieter than `primary` where they differ: a lockup in
// which both lines shout reads as two lockups that happen to be on the same card.
//
// Fonts are named by the category the font table files them under, not one by one,
// so the pools stay right when the font list changes. Ramadan, Eid and Islamic all
// lean on the six Nastaleeq faces and will look related — that is accepted, and the
// layouts and styles are what separate them.

const CATEGORIES = {
  ramadan: {
    target: 50,
    primary: ['gold', 'calligraphy', 'emboss'],
    secondary: ['gold', 'calligraphy', 'minimal'],
    fonts: ['Nastaleeq', 'Quran', 'Decorated'],
  },
  eid: {
    target: 50,
    primary: ['gold', 'calligraphy', 'threed'],
    secondary: ['gold', 'minimal', 'calligraphy'],
    fonts: ['Nastaleeq', 'Decorated', 'Elegant'],
  },
  islamic: {
    target: 50,
    primary: ['calligraphy', 'gold', 'emboss'],
    secondary: ['calligraphy', 'minimal', 'gold'],
    fonts: ['Nastaleeq', 'Quran', 'Decorated'],
  },
  wedding: {
    target: 40,
    primary: ['gold', 'calligraphy', 'layers'],
    secondary: ['minimal', 'gold', 'calligraphy'],
    fonts: ['Nastaleeq', 'Elegant', 'Script', 'Decorated'],
  },
  pakistan: {
    target: 40,
    primary: ['pakistan', 'threed', 'emboss'],
    secondary: ['pakistan', 'minimal', 'modern'],
    fonts: ['Bold', 'Decorated', 'Nastaleeq'],
  },
  birthday: {
    target: 30,
    primary: ['neon', 'threed', 'layers'],
    secondary: ['modern', 'minimal', 'neon'],
    fonts: ['Handwriting', 'Round', 'Bold', 'Decorated'],
  },
  business: {
    target: 30,
    primary: ['modern', 'minimal', 'emboss'],
    secondary: ['minimal', 'modern', 'dark'],
    fonts: ['Regular', 'Modern', 'Condensed', 'Bold'],
  },
  quotes: {
    target: 30,
    primary: ['calligraphy', 'minimal', 'emboss'],
    secondary: ['minimal', 'calligraphy', 'modern'],
    fonts: ['Nastaleeq', 'Handwriting', 'Elegant'],
  },
  sale: {
    target: 25,
    primary: ['badges', 'ribbons', 'threed', 'neon'],
    secondary: ['modern', 'minimal', 'badges'],
    fonts: ['Bold', 'Wide', 'Modern', 'Round'],
  },
  condolence: {
    target: 20,
    primary: ['minimal', 'emboss', 'dark'],
    secondary: ['minimal', 'emboss', 'dark'],
    fonts: ['Nastaleeq', 'Regular', 'Quran'],
  },
};

module.exports = { LAYOUTS, PHRASES, CATEGORIES };
