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
//
// `pool` says which categories may draw on a layout, and defaults to "core". The ten
// original categories name no pools, so they see only the core layouts and their output
// is frozen — see `layoutsFor` in the generator for why that matters more than it looks.
//
// ── Two rules every multi-line layout here obeys ──
//
// 1. Adjacent centres are never closer than about 0.18 of the box. A layer's size is
//    solved from its *width* alone (measure the line, scale it to widthPct), and width
//    says nothing about height, so a short word given a wide target wants to be
//    enormous. TextPreset.verticalRoom() catches that by capping a layer's height at the
//    distance to its nearest neighbour's centre — two neighbours of equal height then
//    exactly meet and never overlap. The cap only works if the gap it is derived from is
//    big enough to be worth having, which is what this rule is.
//
// 2. Within one layout the widths stay in a band — roughly 0.45 to 0.80 for three lines,
//    0.40 to 0.75 for four. The generator pairs any phrase set with any layout of the
//    right line count, so a layout that puts 0.82 next to 0.28 only reads well when the
//    phrase happens to be long/short in the same order. `three-kicker` is the one
//    deliberate exception, kept for the short-word-first sets ("اہلاً / رمضان / مبارک").

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

  // ── Extended pool ─────────────────────────────────────────────────────────
  //
  // Seven more three-line geometries and six four-line ones. The core pool has two
  // three-line layouts and no four-line ones at all, which is why the first catalogue
  // came out 235 lockups of one line, 118 of two and 12 of three: a phrase set can only
  // be cut into a geometry that wants exactly its number of lines, and past two lines
  // there was almost nowhere to put it.
  //
  // The three-line set is deliberately more even than `three-stack` and `three-arch`.
  // Those two were written alongside phrase sets shaped short/long/short and lean hard
  // on that; the sets below are mostly three phrases of comparable length, and a 0.82
  // beside a 0.34 makes one of them shout at the others.

  {
    id: 'three-even',
    lines: 3,
    pool: 'extended',
    layers: [
      { xPct: 0.5, yPct: 0.28, widthPct: 0.64 },
      { xPct: 0.5, yPct: 0.5, widthPct: 0.7 },
      { xPct: 0.5, yPct: 0.72, widthPct: 0.6 },
    ],
  },
  {
    id: 'three-lead',
    lines: 3,
    pool: 'extended',
    // Weight on the opening line: the phrase that names the occasion, then its tail.
    layers: [
      { xPct: 0.5, yPct: 0.29, widthPct: 0.78 },
      { xPct: 0.5, yPct: 0.52, widthPct: 0.56 },
      { xPct: 0.5, yPct: 0.73, widthPct: 0.46 },
    ],
  },
  {
    id: 'three-tail',
    lines: 3,
    pool: 'extended',
    // The mirror of three-lead, for sets that build to their last word.
    layers: [
      { xPct: 0.5, yPct: 0.27, widthPct: 0.46 },
      { xPct: 0.5, yPct: 0.48, widthPct: 0.56 },
      { xPct: 0.5, yPct: 0.71, widthPct: 0.78 },
    ],
  },
  {
    id: 'three-ladder',
    lines: 3,
    pool: 'extended',
    // Stepped to the right down the card. The x drift is small on purpose — a bigger one
    // pushes the widest line off the box once its style draws a stroke past the glyphs.
    layers: [
      { xPct: 0.42, yPct: 0.28, widthPct: 0.54, rotation: -4 },
      { xPct: 0.52, yPct: 0.5, widthPct: 0.6, rotation: -4 },
      { xPct: 0.6, yPct: 0.72, widthPct: 0.52, rotation: -4 },
    ],
  },
  {
    id: 'three-banner',
    lines: 3,
    pool: 'extended',
    // Narrow middle line reads as a rule between two headings.
    layers: [
      { xPct: 0.5, yPct: 0.26, widthPct: 0.74 },
      { xPct: 0.5, yPct: 0.49, widthPct: 0.42 },
      { xPct: 0.5, yPct: 0.73, widthPct: 0.66 },
    ],
  },
  {
    id: 'three-kicker',
    lines: 3,
    pool: 'extended',
    // The exception to the width-band rule, for short/long/short sets.
    layers: [
      { xPct: 0.5, yPct: 0.24, widthPct: 0.34 },
      { xPct: 0.5, yPct: 0.47, widthPct: 0.76 },
      { xPct: 0.5, yPct: 0.71, widthPct: 0.5 },
    ],
  },
  {
    id: 'three-tilted',
    lines: 3,
    pool: 'extended',
    layers: [
      { xPct: 0.5, yPct: 0.29, widthPct: 0.6, rotation: -5 },
      { xPct: 0.5, yPct: 0.51, widthPct: 0.7, rotation: -5 },
      { xPct: 0.5, yPct: 0.73, widthPct: 0.52, rotation: -5 },
    ],
  },

  // Four lines. Centres at roughly 0.20 / 0.40 / 0.60 / 0.80 give every layer 0.18–0.20
  // of vertical room, which is the tightest these go. The widths stay below 0.76: four
  // lines already fill the card, and one of them running wide is what makes a four-line
  // lockup read as a wall of text rather than a composition.
  {
    id: 'four-stack',
    lines: 4,
    pool: 'extended',
    layers: [
      { xPct: 0.5, yPct: 0.2, widthPct: 0.58 },
      { xPct: 0.5, yPct: 0.4, widthPct: 0.66 },
      { xPct: 0.5, yPct: 0.6, widthPct: 0.66 },
      { xPct: 0.5, yPct: 0.8, widthPct: 0.54 },
    ],
  },
  {
    id: 'four-lead',
    lines: 4,
    pool: 'extended',
    layers: [
      { xPct: 0.5, yPct: 0.21, widthPct: 0.74 },
      { xPct: 0.5, yPct: 0.41, widthPct: 0.54 },
      { xPct: 0.5, yPct: 0.61, widthPct: 0.54 },
      { xPct: 0.5, yPct: 0.8, widthPct: 0.46 },
    ],
  },
  {
    id: 'four-tail',
    lines: 4,
    pool: 'extended',
    layers: [
      { xPct: 0.5, yPct: 0.2, widthPct: 0.46 },
      { xPct: 0.5, yPct: 0.39, widthPct: 0.54 },
      { xPct: 0.5, yPct: 0.59, widthPct: 0.54 },
      { xPct: 0.5, yPct: 0.79, widthPct: 0.72 },
    ],
  },
  {
    id: 'four-even',
    lines: 4,
    pool: 'extended',
    layers: [
      { xPct: 0.5, yPct: 0.22, widthPct: 0.6 },
      { xPct: 0.5, yPct: 0.41, widthPct: 0.6 },
      { xPct: 0.5, yPct: 0.59, widthPct: 0.6 },
      { xPct: 0.5, yPct: 0.78, widthPct: 0.6 },
    ],
  },
  {
    id: 'four-ladder',
    lines: 4,
    pool: 'extended',
    layers: [
      { xPct: 0.44, yPct: 0.2, widthPct: 0.5, rotation: -4 },
      { xPct: 0.5, yPct: 0.4, widthPct: 0.56, rotation: -4 },
      { xPct: 0.56, yPct: 0.6, widthPct: 0.56, rotation: -4 },
      { xPct: 0.6, yPct: 0.8, widthPct: 0.48, rotation: -4 },
    ],
  },
  {
    id: 'four-pyramid',
    lines: 4,
    pool: 'extended',
    // Widening down the card, for sets that end on the payoff line.
    layers: [
      { xPct: 0.5, yPct: 0.2, widthPct: 0.42 },
      { xPct: 0.5, yPct: 0.4, widthPct: 0.54 },
      { xPct: 0.5, yPct: 0.6, widthPct: 0.64 },
      { xPct: 0.5, yPct: 0.8, widthPct: 0.74 },
    ],
  },
];

// ── Phrases ─────────────────────────────────────────────────────────────────
//
// Each entry is the lines of one lockup, in reading order. Sets of one to four lines are
// all here; the generator only pairs a set with a layout that wants that many lines, so a
// two-line phrase never gets squeezed into a single-line geometry.
//
// The number of lockups a category can reach is its phrase count times MAX_PER_PHRASE,
// and how many of them are three- or four-layer is decided *here*, not by the layouts —
// a category with no four-line sets gets no four-line lockups however many four-line
// geometries exist. The categories below are written roughly half multi-line for that
// reason; the original ten average one and a half lines and that is what the catalogue
// looked like.
//
// On the Urdu: these are written to be posted, not translated. Where Pakistani users
// genuinely post in Roman Urdu or English — motivational content, shop and sale copy —
// that is what is here, and it is mixed rather than segregated because that is how the
// feed looks. Everywhere else it is Nastaleeq Urdu.

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

  // ── Added in the universal pass ───────────────────────────────────────────
  //
  // Everything below is new, and all of it draws on the extended layout pool, so these
  // shelves carry the three- and four-layer lockups the original ten could not.

  // The only shelf with no season at all. "صبح بخیر" goes out every morning of the year.
  greetings: [
    ['صبح بخیر'],
    ['شب بخیر'],
    ['السلام علیکم'],
    ['خوش آمدید'],
    ['شکریہ'],
    ['صبح', 'بخیر'],
    ['شب', 'بخیر'],
    ['السلام', 'علیکم'],
    ['بہت بہت', 'شکریہ'],
    ['اللہ', 'حافظ'],
    ['آپ کا دن', 'اچھا گزرے'],
    ['ہمیشہ', 'خوش رہیں'],
    ['السلام', 'علیکم', 'ورحمۃ اللہ'],
    ['صبح بخیر', 'خوش رہیں', 'آباد رہیں'],
    ['نیا دن', 'نئی امید', 'نیا حوصلہ'],
    ['دعا ہے', 'آپ کا دن', 'اچھا گزرے'],
    ['شب بخیر', 'میٹھے خواب', 'اللہ حافظ'],
    ['مسکرائیں', 'خوش رہیں', 'دعا کریں'],
    ['اللہ', 'آپ کو', 'خوش رکھے'],
    ['خوش آمدید', 'تشریف لائیے', 'شکریہ'],
    ['السلام علیکم', 'صبح بخیر', 'اللہ آپ کو', 'خوش رکھے'],
    ['نیا دن', 'نیا آغاز', 'نئی امید', 'نیا حوصلہ'],
    ['خوش رہیں', 'مسکراتے رہیں', 'دعاؤں میں', 'یاد رکھیں'],
    ['صبح بخیر', 'دن اچھا گزرے', 'شام خوشگوار ہو', 'شب بخیر'],
    ['سلامتی', 'خوشی', 'صحت', 'سکون'],
  ],

  // 12 Rabi-ul-Awwal. In Pakistan this is the single biggest poster occasion outside the
  // two Eids, and it was sharing a shelf with "سبحان اللہ" and Muharram.
  milad: [
    ['میلادُ النبی ﷺ'],
    ['ربیع الاول مبارک'],
    ['مرحبا یا مصطفیٰ ﷺ'],
    ['درود شریف'],
    ['۱۲ ربیع الاول'],
    ['ربیع الاول', 'مبارک'],
    ['جشنِ عید', 'میلاد النبی ﷺ'],
    ['مرحبا', 'یا مصطفیٰ ﷺ'],
    ['یا نبی', 'سلام علیک'],
    ['ماہِ', 'ربیع الاول'],
    ['آمد', 'مرحبا'],
    ['نعت', 'شریف'],
    ['یا نبی', 'سلام', 'علیک'],
    ['جشنِ', 'عید میلاد', 'النبی ﷺ'],
    ['ربیع الاول', 'کا چاند', 'مبارک'],
    ['صلی اللہ', 'علیہ وآلہ', 'وسلم'],
    ['مرحبا', 'یا مصطفیٰ', 'ﷺ'],
    ['آمنہ کے', 'لال کی', 'آمد مرحبا'],
    ['سرکار ﷺ کی', 'آمد کا', 'مہینہ'],
    ['درود', 'و', 'سلام'],
    ['یا رسول اللہ', 'ﷺ', 'سلام علیک'],
    ['یا نبی', 'سلام علیک', 'یا رسول', 'سلام علیک'],
    ['جشنِ', 'عید میلاد', 'النبی ﷺ', 'مبارک ہو'],
    ['اللھم', 'صلِّ علیٰ', 'سیدنا', 'محمد ﷺ'],
    ['ربیع الاول', 'آمد مرحبا', 'مرحبا', 'یا مصطفیٰ ﷺ'],
    ['درود', 'سلام', 'عقیدت', 'محبت'],
  ],

  // Supplication, and the everyday blessings that are not tied to a date: get well,
  // safe travel, remember me in your prayers.
  dua: [
    ['دعا'],
    ['آمین'],
    ['دعاؤں میں یاد رکھیں'],
    ['یا اللہ رحم فرما'],
    ['دعاؤں میں', 'یاد رکھیں'],
    ['یا اللہ', 'رحم فرما'],
    ['اللہ', 'آسانی فرمائے'],
    ['جلد', 'صحتیاب ہوں'],
    ['سفر', 'بخیر'],
    ['اللہ', 'خیر کرے'],
    ['یا اللہ', 'ہمارے حال پر', 'رحم فرما'],
    ['اللہ', 'آپ کو', 'خوش رکھے'],
    ['دعا ہے', 'اللہ', 'آسانی فرمائے'],
    ['اللہ پاک', 'ہم سب کو', 'ہدایت دے'],
    ['صحت و', 'تندرستی', 'عطا فرما'],
    ['یا اللہ', 'ہر گھر', 'آباد رکھ'],
    ['اللہ', 'آپ کی', 'حفاظت فرمائے'],
    ['رب کا', 'شکر', 'ہر حال میں'],
    ['یا اللہ', 'ہمیں', 'سیدھا راستہ', 'دکھا'],
    ['دعا ہے', 'آپ کی زندگی', 'خوشیوں سے', 'بھری رہے'],
    ['اللہ', 'رزق میں', 'برکت', 'عطا فرمائے'],
    ['صحت', 'سکون', 'رزق', 'ہدایت'],
  ],

  // Kept off the Eid and Islamic shelves on purpose: Muharram is mourning, and a card
  // that styles it like a celebration is the kind of mistake a user does not forgive.
  // The style pools for this category are dark and quiet for the same reason.
  muharram: [
    ['محرم الحرام'],
    ['یا حسین'],
    ['یومِ عاشورہ'],
    ['کربلا'],
    ['محرم', 'الحرام'],
    ['یومِ', 'عاشورہ'],
    ['سلام', 'یا حسین'],
    ['شہیدانِ', 'کربلا'],
    ['نواسۂ', 'رسول ﷺ'],
    ['امام', 'حسین'],
    ['یا', 'حسین', 'ابنِ علی'],
    ['سلام', 'شہیدانِ', 'کربلا'],
    ['کربلا', 'کے شہیدوں', 'کو سلام'],
    ['محرم', 'الحرام', 'کی آمد'],
    ['نواسۂ', 'رسول ﷺ', 'کو سلام'],
    ['حق', 'اور', 'صداقت'],
    ['۱۰', 'محرم', 'الحرام'],
    ['محرم الحرام', 'یومِ عاشورہ', 'سلام', 'یا حسین'],
    ['کربلا', 'کا پیغام', 'حق کے لیے', 'ڈٹ جانا'],
    ['صبر', 'قربانی', 'ایثار', 'حسینیت'],
    ['سلام', 'یا حسین', 'سلام', 'شہیدانِ کربلا'],
  ],

  // The everyday milestone shelf: exam results, a new job, a new house, a new baby.
  congrats: [
    ['مبارک ہو'],
    ['کامیابی مبارک'],
    ['شاباش'],
    ['ڈھیروں مبارکباد'],
    ['کامیابی', 'مبارک'],
    ['بہت بہت', 'مبارک ہو'],
    ['نئی نوکری', 'مبارک'],
    ['نیا گھر', 'مبارک'],
    ['ننھے مہمان', 'کی آمد'],
    ['امتحان میں', 'کامیابی'],
    ['آپ کو', 'کامیابی', 'مبارک ہو'],
    ['بہت بہت', 'مبارک ہو', 'شاباش'],
    ['نئی منزل', 'نیا سفر', 'مبارک ہو'],
    ['ننھے مہمان', 'کی آمد', 'مبارک ہو'],
    ['محنت', 'رنگ لائی', 'مبارک ہو'],
    ['نیا گھر', 'نئی خوشیاں', 'مبارک ہو'],
    ['نئی نوکری', 'نیا آغاز', 'مبارک ہو'],
    ['آپ کو', 'اس کامیابی', 'پر ڈھیروں', 'مبارکباد'],
    ['نیا سفر', 'نئی منزل', 'نئی کامیابی', 'مبارک ہو'],
    ['محنت', 'لگن', 'کامیابی', 'مبارک ہو'],
    ['خوشیاں', 'کامیابیاں', 'ترقیاں', 'مبارک ہوں'],
  ],

  love: [
    ['محبت'],
    ['میری جان'],
    ['عشق'],
    ['تم سے محبت ہے'],
    ['تم سے', 'محبت ہے'],
    ['میری', 'جان'],
    ['ہمیشہ', 'ساتھ'],
    ['دل سے', 'دل تک'],
    ['تیرے', 'بغیر'],
    ['تم میری', 'دنیا ہو'],
    ['تم ہو', 'تو سب', 'کچھ ہے'],
    ['تم سے', 'محبت ہے', 'بے پناہ'],
    ['ہر خوشی', 'تمہارے', 'نام'],
    ['میری جان', 'میری دنیا', 'میرا سکون'],
    ['ہمیشہ', 'ساتھ', 'رہنا'],
    ['دل', 'دھڑکن', 'تم'],
    ['تم', 'میری', 'کائنات ہو'],
    ['تم ہو', 'تو دنیا', 'خوبصورت', 'لگتی ہے'],
    ['محبت', 'اعتبار', 'وفا', 'ہمیشہ'],
    ['میری جان', 'میری دنیا', 'میرا سکون', 'تم ہی ہو'],
    ['دل', 'جان', 'عشق', 'وفا'],
  ],

  // Where Roman Urdu and English are not a compromise — this is how the genre is posted.
  motivation: [
    ['ہمت نہ ہارو'],
    ['محنت رنگ لائے گی'],
    ['یقین رکھو'],
    ['Never Give Up'],
    ['Dream Big'],
    ['ہمت', 'نہ ہارو'],
    ['محنت', 'رنگ لائے گی'],
    ['خواب دیکھو', 'پورے کرو'],
    ['Never', 'Give Up'],
    ['Dream', 'Big'],
    ['یقین', 'رکھو'],
    ['Keep', 'Going'],
    ['ہمت', 'نہ ہارو', 'منزل قریب ہے'],
    ['محنت کرو', 'یقین رکھو', 'کامیابی ملے گی'],
    ['خواب', 'محنت', 'کامیابی'],
    ['Dream', 'Believe', 'Achieve'],
    ['ارادہ', 'محنت', 'کامیابی'],
    ['Work Hard', 'Stay Humble', 'Keep Going'],
    ['آج', 'ابھی', 'شروع کرو'],
    ['Mehnat', 'Karo', 'Rang Layegi'],
    ['خواب دیکھو', 'محنت کرو', 'یقین رکھو', 'کامیاب ہو جاؤ'],
    ['Dream', 'Believe', 'Work', 'Achieve'],
    ['ہمت', 'حوصلہ', 'محنت', 'کامیابی'],
    ['اٹھو', 'چلو', 'بڑھو', 'جیتو'],
    ['Wake Up', 'Work Hard', 'Stay Focused', 'Repeat'],
  ],

  family: [
    ['ماں'],
    ['ماں کی دعا'],
    ['والدین'],
    ['دوستی'],
    ['ماں', 'کی دعا'],
    ['یومِ', 'مادر'],
    ['بہن', 'بھائی'],
    ['سچی', 'دوستی'],
    ['والدین کا', 'سایہ'],
    ['یومِ', 'والد'],
    ['ماں کے', 'قدموں تلے', 'جنت ہے'],
    ['ماں باپ', 'کی خدمت', 'عبادت ہے'],
    ['یومِ', 'والدہ', 'مبارک'],
    ['بہن', 'بھائی', 'کا پیار'],
    ['سچے دوست', 'قسمت والوں', 'کو ملتے ہیں'],
    ['ماں', 'باپ', 'اولاد'],
    ['گھر', 'کی', 'رونق'],
    ['ماں کی دعا', 'باپ کا سایہ', 'اللہ سب کو', 'نصیب کرے'],
    ['گھر', 'خاندان', 'محبت', 'سکون'],
    ['ماں', 'باپ', 'بہن', 'بھائی'],
    ['دوستی', 'خلوص', 'اعتبار', 'ساتھ'],
  ],

  // Dhaba, home chef, bakery. Separate from Business because the copy is different —
  // a restaurant posts a dish, not a service.
  food: [
    ['بریانی'],
    ['چائے'],
    ['دیسی کھانے'],
    ['آج ہی آرڈر کریں'],
    ['مزیدار', 'بریانی'],
    ['گرم گرم', 'چائے'],
    ['آج ہی', 'آرڈر کریں'],
    ['فریش', 'اور لذیذ'],
    ['ہوم', 'میڈ'],
    ['فری', 'ڈیلیوری'],
    ['مزیدار', 'بریانی', 'آرڈر کریں'],
    ['گرم گرم', 'چائے', 'اور پراٹھا'],
    ['دیسی کھانے', 'گھر جیسا', 'ذائقہ'],
    ['فریش', 'لذیذ', 'سستا'],
    ['آج کا', 'خصوصی', 'مینو'],
    ['نہاری', 'حلیم', 'قورمہ'],
    ['ہوم میڈ', 'فریش', 'روزانہ'],
    ['مزیدار کھانا', 'مناسب قیمت', 'فری ڈیلیوری', 'آج ہی آرڈر کریں'],
    ['ناشتہ', 'دوپہر', 'رات', 'ہر وقت حاضر'],
    ['بریانی', 'نہاری', 'حلیم', 'قورمہ'],
    ['فریش', 'لذیذ', 'سستا', 'گھر تک'],
  ],

  // Tuition academies and schools. Admission-open season runs twice a year and the
  // posters are everywhere.
  education: [
    ['داخلے جاری ہیں'],
    ['ٹیوشن سنٹر'],
    ['علم روشنی ہے'],
    ['Admission Open'],
    ['داخلے', 'جاری ہیں'],
    ['Admission', 'Open'],
    ['ٹیوشن', 'سنٹر'],
    ['علم', 'روشنی ہے'],
    ['بہترین', 'نتائج'],
    ['تجربہ کار', 'اساتذہ'],
    ['داخلے', 'جاری ہیں', 'آج ہی رابطہ کریں'],
    ['تجربہ کار', 'اساتذہ', 'بہترین نتائج'],
    ['علم', 'شعور', 'کامیابی'],
    ['Admission', 'Open', 'Apply Now'],
    ['میٹرک', 'انٹر', 'بی اے'],
    ['پڑھو', 'سمجھو', 'آگے بڑھو'],
    ['داخلے جاری ہیں', 'تجربہ کار اساتذہ', 'بہترین نتائج', 'آج ہی رابطہ کریں'],
    ['علم', 'محنت', 'شعور', 'کامیابی'],
    ['Admission', 'Open', 'Limited Seats', 'Apply Now'],
  ],

  // Non-religious and year-round: New Year, the first rain, the turn of the seasons.
  seasons: [
    ['نیا سال مبارک'],
    ['بارش'],
    ['موسمِ بہار'],
    ['Happy New Year'],
    ['نیا سال', 'مبارک'],
    ['موسمِ', 'بہار'],
    ['بارش', 'کا موسم'],
    ['سردیوں کی', 'آمد'],
    ['Happy', 'New Year'],
    ['نیا سال', 'نئی امیدیں', 'مبارک ہو'],
    ['بارش', 'چائے', 'اور پکوڑے'],
    ['بہار', 'پھول', 'خوشبو'],
    ['سردی', 'چائے', 'اور رضائی'],
    ['پہلی', 'بارش', 'کا موسم'],
    ['ساون', 'برسات', 'ٹھنڈی ہوا'],
    ['نیا سال', 'نیا آغاز', 'نئی امیدیں', 'مبارک ہو'],
    ['بہار', 'گرمی', 'ساون', 'سردی'],
    ['بارش', 'چائے', 'پکوڑے', 'اور آپ'],
  ],

  travel: [
    ['سفر'],
    ['ناران کاغان'],
    ['شمالی علاقہ جات'],
    ['ٹور پیکیج'],
    ['ٹور', 'پیکیج'],
    ['شمالی', 'علاقہ جات'],
    ['سفر', 'بخیر'],
    ['ناران', 'کاغان'],
    ['نئی', 'منزلیں'],
    ['مری', 'اور گلیات'],
    ['ناران', 'کاغان', 'ٹور پیکیج'],
    ['پہاڑ', 'جھیلیں', 'وادیاں'],
    ['سفر کریں', 'دنیا دیکھیں', 'یادیں بنائیں'],
    ['شمالی', 'علاقہ جات', 'کی سیر'],
    ['سوات', 'کالام', 'مالم جبہ'],
    ['نئی', 'منزلیں', 'نئے سفر'],
    ['ناران', 'کاغان', 'سوات', 'ٹور پیکیج'],
    ['سفر کریں', 'دنیا دیکھیں', 'یادیں بنائیں', 'خوش رہیں'],
    ['پہاڑ', 'جھیلیں', 'وادیاں', 'صحرا'],
  ],

  hajj: [
    ['حج مبارک'],
    ['عمرہ مبارک'],
    ['لبیک اللھم لبیک'],
    ['حج', 'مبارک'],
    ['عمرہ', 'مبارک'],
    ['لبیک', 'اللھم لبیک'],
    ['مکہ', 'مدینہ'],
    ['زیارتِ', 'حرمین'],
    ['لبیک', 'اللھم', 'لبیک'],
    ['حج', 'مبارک', 'حجِ مبرور'],
    ['خانہ کعبہ', 'مدینہ منورہ', 'زیارت مبارک'],
    ['حج', 'عمرہ', 'زیارت'],
    ['مکہ', 'مدینہ', 'حرمین شریفین'],
    ['لبیک', 'اللھم', 'لبیک', 'لا شریک لک'],
    ['حج', 'عمرہ', 'زیارت', 'مبارک ہو'],
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

  // ── Added in the universal pass ───────────────────────────────────────────
  //
  // Appended rather than interleaved: shelf order is the enum's job, and the ten above
  // are left exactly where they were so their diff stays empty.
  //
  // All of these name `layouts: ['core', 'extended']`. The ten above name nothing and
  // therefore keep the core pool and their existing geometry — see `layoutsFor`.
  //
  // A target above phrases × 3 is wasted: MAX_PER_PHRASE caps a phrase at three
  // appearances, and a fourth would have to repeat a geometry the phrase has already
  // worn. Each target below is about twice its phrase count, which leaves the generator
  // enough slack to skip a collision without coming up short.

  greetings: {
    target: 40,
    layouts: ['core', 'extended'],
    primary: ['minimal', 'calligraphy', 'gold'],
    secondary: ['minimal', 'modern', 'calligraphy'],
    fonts: ['Nastaleeq', 'Handwriting', 'Decorated'],
  },
  milad: {
    target: 40,
    layouts: ['core', 'extended'],
    primary: ['gold', 'calligraphy', 'emboss'],
    secondary: ['gold', 'calligraphy', 'minimal'],
    fonts: ['Nastaleeq', 'Quran', 'Decorated'],
  },
  dua: {
    target: 30,
    layouts: ['core', 'extended'],
    primary: ['calligraphy', 'minimal', 'gold'],
    secondary: ['minimal', 'calligraphy', 'emboss'],
    fonts: ['Nastaleeq', 'Quran', 'Handwriting'],
  },
  muharram: {
    target: 30,
    layouts: ['core', 'extended'],
    // Dark and quiet throughout, primary and secondary alike. Muharram is mourning; a
    // neon or a ribbon on it is not a style mismatch, it is an offence.
    primary: ['dark', 'minimal', 'emboss'],
    secondary: ['dark', 'minimal', 'emboss'],
    fonts: ['Nastaleeq', 'Quran', 'Regular'],
  },
  congrats: {
    target: 30,
    layouts: ['core', 'extended'],
    primary: ['gold', 'threed', 'layers'],
    secondary: ['modern', 'minimal', 'gold'],
    fonts: ['Nastaleeq', 'Decorated', 'Round', 'Bold'],
  },
  love: {
    target: 30,
    layouts: ['core', 'extended'],
    primary: ['neon', 'calligraphy', 'layers'],
    secondary: ['minimal', 'calligraphy', 'modern'],
    fonts: ['Nastaleeq', 'Handwriting', 'Decorated'],
  },
  motivation: {
    target: 35,
    layouts: ['core', 'extended'],
    primary: ['threed', 'modern', 'neon'],
    secondary: ['minimal', 'modern', 'dark'],
    fonts: ['Bold', 'Modern', 'Nastaleeq', 'Condensed'],
  },
  family: {
    target: 30,
    layouts: ['core', 'extended'],
    primary: ['calligraphy', 'gold', 'minimal'],
    secondary: ['minimal', 'calligraphy', 'modern'],
    fonts: ['Nastaleeq', 'Handwriting', 'Decorated'],
  },
  food: {
    target: 30,
    layouts: ['core', 'extended'],
    primary: ['badges', 'threed', 'ribbons'],
    secondary: ['modern', 'minimal', 'badges'],
    fonts: ['Bold', 'Round', 'Decorated', 'Wide'],
  },
  education: {
    target: 25,
    layouts: ['core', 'extended'],
    primary: ['modern', 'minimal', 'emboss'],
    secondary: ['minimal', 'modern', 'dark'],
    fonts: ['Bold', 'Modern', 'Condensed', 'Regular'],
  },
  seasons: {
    target: 25,
    layouts: ['core', 'extended'],
    primary: ['layers', 'neon', 'modern'],
    secondary: ['minimal', 'modern', 'layers'],
    fonts: ['Round', 'Handwriting', 'Nastaleeq', 'Decorated'],
  },
  travel: {
    target: 25,
    layouts: ['core', 'extended'],
    // The only two categories using the Perspective and Ribbons shelves, which had no
    // lockups at all in the first run.
    primary: ['perspective', 'modern', 'layers'],
    secondary: ['minimal', 'modern', 'emboss'],
    fonts: ['Wide', 'Bold', 'Modern', 'Nastaleeq'],
  },
  hajj: {
    target: 20,
    layouts: ['core', 'extended'],
    primary: ['gold', 'calligraphy', 'emboss'],
    secondary: ['gold', 'minimal', 'calligraphy'],
    fonts: ['Nastaleeq', 'Quran', 'Decorated'],
  },
};

module.exports = { LAYOUTS, PHRASES, CATEGORIES };
