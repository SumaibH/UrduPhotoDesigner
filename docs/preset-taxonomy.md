# Preset taxonomy — the universal catalogue

What each shelf of the text-preset panel is for, why it earned one, and how many lockups it
can carry. Written when the catalogue went from ten categories and 365 lockups to
twenty-three and 755.

The list is the reviewable part. The content generated from it can be re-cut in a minute
(`node tools/generate-presets.js`); the decision about which shelves exist is the one that
is expensive to change later, because a shelf's name is its asset file name and the prefix
on every id inside it, and an id that moves reads to a user as a preset that vanished out
of their Recents.

---

## The problem this was fixing

The first catalogue was ten categories, and four of them — Ramadan, Eid, Islamic and
Condolence — were 170 of the 365 lockups. Ramadan alone was 50. That is a catalogue for
roughly six weeks of the Islamic calendar plus funerals, and for the other forty-six weeks
of the year a user opening the panel found Birthday, Wedding, Business, Quotes, Sale and
Pakistan and not much reason to stay.

The second problem was shape rather than subject. 235 of the 365 lockups were a single line
of text, 118 were two, and **twelve** had three. A one-line lockup is a font and a colour;
it is not really a *design*, and a shelf of them does not look like something worth
scrolling. The cause was upstream of the layouts: the generator can only cut a phrase set
into a geometry wanting exactly that many lines, and the authored phrase sets averaged one
and a half lines each.

Both are fixed below, and neither fix touches the ten existing files — see
"What did not change".

---

## Shelf order

Declaration order in `TextPresetCategory` is shelf order, and it runs by expected demand.
The ordering argument, shelf by shelf:

1. **Greetings** leads because it is the only shelf with no season at all. "صبح بخیر" is
   posted every morning of the year; Ramadan is posted for thirty days of it.
2. Ramadan, Eid, Milad un Nabi, Islamic, Dua, Muharram — the Islamic block, kept together
   and near the front because that is where this app's users start.
3. Birthday, Wedding, Congratulations, Love, Motivation, Quotes, Family — personal
   occasions and everyday feelings, the year-round middle of the catalogue.
4. Pakistan — national days, three or four spikes a year and very large ones.
5. Business, Sale, Food, Education — the commercial shelves, in order of how many
   Pakistani small businesses post on each.
6. Seasons, Travel, Hajj & Umrah — real but narrow.
7. **Condolence** last, deliberately. Nobody browses to it; they arrive needing it.

Reordering this list later is safe. Nothing persists an ordinal — the Recents shelf stores
preset ids — so the enum can be resequenced when the analytics say something different.

---

## The thirteen new categories

Each line: why it deserves a shelf, and what it can carry. "Supports" is phrase sets × 3,
which is the real ceiling — `MAX_PER_PHRASE` in the generator caps a phrase at three
appearances and each has to wear a different geometry.

| Shelf | Why it earned one | Sets | Supports | Shipping |
|---|---|---|---|---|
| **Greetings** | The highest-frequency post type there is and the catalogue had nothing for it. صبح بخیر, شب بخیر, السلام علیکم, شکریہ, خوش آمدید — every morning, all year, no occasion required. | 25 | 75 | 40 |
| **Milad un Nabi** | 12 Rabi-ul-Awwal is arguably the biggest single poster occasion in Pakistan outside the two Eids, and it was sharing a shelf with سبحان اللہ and Muharram. Naat and salam phrasing is its own register and deserves its own gold-and-calligraphy pool. | 26 | 78 | 40 |
| **Dua** | Supplication and the blessings with no date attached: دعاؤں میں یاد رکھیں, جلد صحتیاب ہوں, سفر بخیر. "Remember me in your prayers" is posted more often than any single Eid greeting. Also the home for get-well and safe-travel, which had nowhere to go. | 22 | 66 | 30 |
| **Muharram** | The other big missing Islamic occasion, and the one that most needs separating: Muharram is mourning. On a shared shelf it would be styled like Eid, and a card that celebrates Ashura is the sort of mistake a user does not forgive. Its style pools are dark and quiet throughout. | 21 | 63 | 30 |
| **Congratulations** | The everyday milestone shelf — exam results, a new job, a new house, a new baby. Birthday and Wedding cover two milestones; this covers the rest, and they happen far more often. | 21 | 63 | 30 |
| **Love** | Romantic lockups were reachable only through Quotes, which is a literary shelf. Distinct audience, distinct phrasing, and a large one. | 21 | 63 | 30 |
| **Motivation** | Split deliberately from Quotes: Quotes is اقوالِ زریں and شاعری, this is hustle content, and the two do not share a voice or a typeface. This is also the shelf where Roman Urdu and English are not a compromise — "Mehnat Rang Layegi" and "Never Give Up" are how the genre is actually posted. | 25 | 75 | 35 |
| **Family** | ماں کی دعا, والدین, بہن بھائی, Mother's and Father's Day, and دوستی. Year-round, and the single most shared category of Urdu image post after greetings. | 21 | 63 | 30 |
| **Food** | Dhaba, home chef, bakery, home delivery. Split from Business because the copy is a different thing — a restaurant posts a dish, not a service, and the styles that suit بریانی do not suit خدمت میں حاضر. | 21 | 63 | 30 |
| **Education** | Tuition academies and schools. Admission-open season runs twice a year and the posters are on every wall in the country; "Admission Open" in English beside داخلے جاری ہیں is exactly how they are printed. | 19 | 57 | 25 |
| **Seasons** | The non-religious calendar: نیا سال, the first rain, بہار, سردی. New Year alone could not carry a shelf; the turn of the seasons with it can, and بارش چائے اور پکوڑے is posted every monsoon. | 18 | 54 | 25 |
| **Travel** | Tour operators and the northern-areas season — ناران کاغان, سوات, ٹور پیکیج. Narrower than the shelves above but a real commercial use with its own vocabulary. | 19 | 57 | 25 |
| **Hajj & Umrah** | The remaining missing Islamic occasion. Kept separate from Eid because حج مبارک and لبیک اللھم لبیک are not Eid-ul-Adha greetings even though they share a fortnight. **The thinnest shelf here** — see Doubts. | 15 | 45 | 20 |

Totals: 13 shelves, 274 phrase sets, 390 lockups. Catalogue-wide: 23 shelves, 755 lockups.

### Considered and rejected

- **Jumma Mubarak** — weekly and enormous, but it is one phrase with variations. It cannot
  reach twenty genuinely distinct lockups, so it stays inside Islamic and Dua.
- **Friendship** — big in Pakistan, but it overlaps Love and Quotes so heavily that three
  shelves would be competing for the same posts. Folded into Family.
- **Shab-e-Barat / Shab-e-Meraj** — real occasions, not twenty lockups each. They stay in
  Islamic.
- **New Year** as its own shelf — one day. Folded into Seasons.

---

## More layers

The count of three- and four-layer lockups is decided by the **phrase sets**, not the
layouts. A category with no four-line sets gets no four-line lockups however many four-line
geometries exist, which is why the first run produced twelve three-layer lockups out of 365
— there were five three-line phrase sets in the entire authored content.

So both ends were changed:

- **Thirteen new layouts** in `tools/preset-content.js` — seven more three-line geometries
  and six four-line ones, where the core pool had two three-line layouts and no four-line
  ones at all.
- **The new phrase sets are written multi-line first.** Roughly 4 one-line, 6 two-line,
  7 three-line and 4 four-line per category, against the original ten's average of one and
  a half lines.

Result across the whole catalogue:

| Layers | Before | After |
|---|---|---|
| 1 | 235 | 343 |
| 2 | 118 | 254 |
| 3 | 12 | 108 |
| 4 | 0 | 50 |

158 lockups of three or more layers, up from twelve. Within the new shelves alone, 37% are
three-layer or four-layer.

### Why the new layouts are shaped the way they are

`TextPreset.verticalRoom()` documents a real failure: a layer's size is solved from its
**width** alone — measure the line, scale it to `widthPct` — and width says nothing about
height, so a short word given a wide target wants to be enormous and lands on the line
below. "ماہِ صیام / مبارک" rendered as a pile. The fix in the model caps a layer's height at
the distance to its nearest neighbour's centre, so two neighbours of equal height exactly
meet and never overlap.

That cap only works if the gap it is derived from is worth having, so every new multi-line
layout obeys two rules, both written into the file:

1. **Adjacent centres are never closer than about 0.18 of the box.** Four-line layouts sit
   at roughly 0.20 / 0.40 / 0.60 / 0.80, which gives every layer 0.18–0.20 of vertical room.
2. **Widths stay in a band within one layout** — about 0.45 to 0.80 for three lines, 0.40 to
   0.75 for four. The generator pairs any phrase set with any layout of the right line
   count, so a layout putting 0.82 next to 0.28 only reads well when the phrase happens to
   be long/short in that same order. `three-kicker` is the one deliberate exception, kept
   for the short-word-first sets.

---

## What did not change

The ten original category files are **byte-identical** to what shipped. Verified with
`git diff --exit-code` after the regeneration run.

That is not a happy accident, and it needed a change to the generator. Layout choice is
`candidates[… % candidates.length]`, so appending one three-line layout to a shared list
would silently re-cut every existing three-line lockup — same id, different geometry. A
preset that changes under an id is worse than one that disappears, because it disappears
quietly.

So layouts now carry a `pool`, defaulting to `core`, and a category names the pools it
draws on. The ten original categories name none, get `core`, and are frozen. The thirteen
new ones name `['core', 'extended']`.

### If you want the old shelves re-cut too

Add `layouts: ['core', 'extended']` to any of the ten in `CATEGORIES` and re-run. Ramadan,
Eid, Islamic and Wedding would all gain three-layer versions of phrases they already carry.

**This is not free and it is not done here.** It changes what existing ids render — the ids
survive, the compositions under them do not — so someone who saved `ramadan_014` to their
Recents gets a different card next time. Worth doing at a content release, with the id churn
accepted on purpose; not worth doing quietly.

---

## Doubts — where a native speaker should look first

Ordered by how much a mistake would cost.

1. **Muharram, all of it.** Religiously sensitive and contested between sects, and phrases
   like سلام یا حسین, نواسۂ رسول ﷺ and حسینیت sit differently for different readers. Every
   line was written to be respectful and factual, but "respectful enough" is not a call to
   make from outside the audience. Read this shelf before it ships.
2. **Milad — the honorifics and the salam.** ﷺ is used as a glyph inline; whether it renders
   correctly in every Nastaleeq face in the pool has not been checked on a device. The four
   line 'یا نبی / سلام علیک / یا رسول / سلام علیک' is the famous salam and should read as
   that, not as a repetition — worth eyes on.
   Also 'اللھم / صلِّ علیٰ / سیدنا / محمد ﷺ' carries a diacritic on صلِّ which may or may not
   set well in these faces.
3. **Hajj & Umrah is the thinnest new shelf.** Fifteen phrase sets against twenty lockups
   means most phrases appear twice; the ceiling is 45 but the honest supply is about 20.
   It ships because the occasion was genuinely missing, not because the content is deep. If
   any shelf here should be cut or merged, it is this one.
4. **Motivation's Roman Urdu.** 'Mehnat Rang Layegi' and 'Mehnat / Karo / Rang Layegi' are
   spelled the way they are usually typed, not by a transliteration standard. If the usual
   spelling is different, it will look wrong to exactly the audience that posts it.
5. **Four-line sets that are lists rather than sentences** — 'صحت / سکون / رزق / ہدایت',
   'دل / جان / عشق / وفا', 'صبر / قربانی / ایثار / حسینیت'. These work as designs and they
   work as captions, but a native reader will know immediately whether they read as
   deliberate or as four words stacked up to fill a layout. Three or four of them are
   probably keepers and the rest are probably filler; I cannot tell which.
6. **Family.** 'ماں کے قدموں تلے جنت ہے' is the well-known saying and should be safe;
   'ماں باپ کی خدمت عبادت ہے' is a paraphrase and may want different wording.
7. **Travel place names.** مالم جبہ, کالام, گلیات are spelled as commonly written but were
   not checked against a gazetteer.

### One thing that is not a content doubt but will show

The style catalogue has **no Urdu fonts at all** under `Elegant`, `Script` or `Rounded` —
`tools/font-inventory.json` files fifteen Elegant and eleven Script faces and every one is
non-Urdu. `fontPool()` silently falls back to Nastaleeq when a category's pools come up
empty, so Wedding (which names Elegant and Script) has been drawing on Nastaleeq and
Decorated only since the first run. That is not new and nothing is broken, but a wedding
shelf that was designed to have script faces does not have any.
