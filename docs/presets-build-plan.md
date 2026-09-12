# Dynamic Presets — build plan

Status: **planning only, nothing implemented.**

Written against `production` @ `7f22691` ("merge: the Urdu text presets shelf"). Every claim is
checked against a file and cited, so it can be rechecked when the code moves.

---

## 0. What this plan is now, and what it replaced

The first draft of this document was written against `c24b56e`, before the presets shelf merged.
It described building the feature from scratch: `PresetEntity` holding an array of
`CanvasElement`s, pixel coordinates to normalize, thumbnails to download, a group-insertion
pipeline to write. **All of that is obsolete.** The shelf landed mid-planning as `7f22691` — four
commits off `claude/confident-kapitsa-66851d`, including 365 lockups across ten occasions — and
the shipped model is a different and better shape than either draft assumed.

What is left to build is much smaller than it looked: an API, a Room cache, a kill switch, and a
three-tier fallback. This document covers only that.

### 0.1 What already exists on production — do not re-plan it

| Piece | Where |
|---|---|
| The model — a lockup of fractionally-positioned text layers | `data/model/TextPreset.kt` |
| Ten occasion categories | `data/model/TextPresetCategory.kt` |
| Catalogue loading from bundled assets, lazily per category | `data/repository/TextPresetsRepository.kt` |
| 365 lockups | `assets/presets/lockups/{ramadan,eid,…}.json` |
| Style resolution + per-layer override merge | `TextStylesRepository.resolveStyle()` |
| Insertion: fonts on tap, one group, one undo step | `CanvasViewModel.addTextPreset()` @ 4767 |
| Multi-layer thumbnails, rendered on device | `TextStyleThumbnailRenderer` |
| The panel — All / Styles / Presets, with per-occasion shelves | `panels/text/TextFragment.kt` |
| Recents shelf, id-based | `data/repository/RecentsStore.kt` |

### 0.2 The model, stated plainly so nobody plans against the wrong one again

A `TextPreset` is **not** a serialized canvas. It is:

```kotlin
TextPreset(id, name, category, aspect, layers: List<PresetLayer>)
PresetLayer(text, fontId, styleId, override: JsonObject?, xPct, yPct, widthPct, rotation, align)
```

Three consequences that delete whole sections of the old plan:

- **Positions are fractions of the preset's own box**, not pixels (`TextPreset.kt:28`). Canvas-size
  independence is already solved. There is no bounding box to compute, no 80% fit rule, no
  re-centring — the old plan's entire §4 steps 5–6.
- **Thumbnails are rendered, not fetched.** `TextStyleThumbnailRenderer` draws the layers. There is
  no `thumbnail_url`, no Glide, no shimmer tile to build.
- **Premium is computed, not stored.** `isPremium(premiumFontIds, premiumStyleIds)`
  (`TextPreset.kt:66`) derives it from the fonts and styles a lockup wears, so a preset re-prices
  itself when a font flag flips on the dashboard. There is no `is_premium` column to sync, and the
  export-time gate (`ExportFragment.isPremiumLocked()`) already covers it. **Confirmed keeping.**

A preset is also tiny — a JSON object with two or three layers. There is nothing to stream, so no
`json_url`, no `DownloadRepo`, no `ProjectCodec`, no `file_path`, no `is_downloading`.

---

## 1. Scope

**In:** remote catalogue, Room cache, `enabled` kill switch, three-tier fallback, the async
refactor that fallback forces on `TextFragment`.

**Out:** the model, the renderer, the applier, insertion, premium, recents, the panel's structure.
All shipped.

### 1.1 The fallback chain

Confirmed with you, and it is the spine of the feature:

```
API enabled and reachable  →  serve remote, write through to Room
API unreachable            →  serve Room (populated by the last successful sync)
Room empty                 →  serve bundled assets/presets/lockups/*.json
```

The bundled tier is not a stale duplicate to be argued away — it is 365 authored lockups and the
reason a fresh install has content before its first sync. It stays.

---

## 2. The client change that is actually hard

Everything above is routine. This is not.

`TextFragment` builds its shelves by calling the repository **synchronously, on the main thread**:

```kotlin
stylesDrill == StylesDrill.NONE -> TextPresetsRepository.getAllPresets(ctx)      // :1749
…findPresetsByIds(ctx, RecentsStore.ids(ctx, Kind.PRESET))                       // :1753
…getPresetsByCategory(ctx, it)                                                   // :1755
```

`TextPresetsRepository` is an `object` with a synchronized in-memory cache over `assets.open()` —
fine for bundled files, impossible for Room and the network. Making the catalogue dynamic means
those three call sites stop returning a `List` and start being observed.

This is the single largest piece of client work in the feature, and it was absent from both earlier
drafts. Plan it explicitly:

- `TextPresetsRepository` keeps its shape but gains a source behind it, and its reads become
  `suspend` / `Flow`.
- `TextFragment` observes a `StateFlow<List<TextPreset>>` per shelf instead of pulling. The shelf
  must render from the bundled tier immediately and re-render if a sync lands mid-session, without
  losing scroll position or the user's drill state (`StylesDrill`).
- Do **not** make the first paint wait on Room or the network. Bundled-first, then swap.

---

## 3. Server contract

Mirror the existing endpoints' style (`Constants.BASE_URL`, `X-API-KEY` header).

```
GET /presets                 → manifest: enabled flag + per-category versions
GET /presets/{category}      → the lockups for one occasion
```

Per-category, not one payload, because the shipped repository already loads lazily per category
(`TextPresetsRepository.kt:20` — "ten categories of fifty lockups is a lot of JSON to parse on a
panel the user may never scroll past the first tab of"). Keep that property.

Manifest:

```json
{
  "status": true,
  "enabled": true,
  "style_catalogue_version": 3,
  "categories": [
    { "category": "ramadan", "version": 7, "count": 52, "updated_at": "2026-09-12 10:00:00" }
  ]
}
```

Category payload — **byte-identical to the bundled file format**, so one parser serves both tiers:

```json
[
  {
    "id": "ramadan_006",
    "name": "Mah-e-Siyam Mubarak",
    "aspect": 1.0,
    "layers": [
      { "text": "ماہِ صیام", "fontId": "NastaleeqBold", "styleId": "gold_emboss_02",
        "xPct": 0.5, "yPct": 0.36, "widthPct": 0.78, "rotation": 0, "align": "CENTER" }
    ]
  }
]
```

Three notes on the shape:

- `category` is **not** a field on the entry. The bundled parser takes category from the file it is
  in — "the file is the authority, so an entry cannot claim to be somewhere it is not filed"
  (`TextPresetsRepository.kt:113`). The endpoint path plays the role of the filename. Keep the rule.
- `version` per category is what makes sync cheap: fetch the manifest, compare against what Room
  holds, pull only changed categories.
- `style_catalogue_version` is the guard described in §5.

---

## 4. Data layer

Follow the project's own layering — the shipped features are five files, not two
(`domain/repo/FetchXRepo` → `Flow<Response<T>>`, `domain/repo/XRepo` + `data/repository/XRepoImpl`
for Room, four use cases in `domain/usecase`, `MainViewModel` consuming only use cases,
`MainViewModel.kt:89-92`).

```
data/model/TextPresetEntity.kt        @Entity(tableName = "text_presets")
data/model/PresetsManifestResponse.kt
data/local/TextPresetDao.kt
data/local/AppDatabase.kt             +textPresetDao(), version 6, addMigrations(MIGRATION_5_6)
data/local/Migrations.kt              [NEW]
data/remote/EndPointsInterface.kt     +getPresetsManifest(), +getPresetsByCategory(category)
domain/repo/FetchPresetsRepo.kt       + data/repository/FetchPresetsRepoImpl.kt
domain/repo/PresetsRepo.kt            + data/repository/PresetsRepoImpl.kt
domain/usecase/                       FetchAPIPresetsUseCase, GetPresetsUseCase,
                                      InsertPresetsUseCase, UpdatePresetsUseCase
```

`TextPresetEntity` is the storage shadow of `TextPreset`, not a second model:

| Column | Note |
|---|---|
| `id` TEXT PK | `ramadan_006` — authored, stable, category-prefixed by convention |
| `category` TEXT | the enum name; the row's filing authority, from the endpoint path |
| `name` TEXT | |
| `aspect` REAL | |
| `layers_json` TEXT | the layers array stored raw |
| `catalogue_version` INTEGER | the category version this row arrived with |
| `updated_at` TEXT | |

Store `layers` as raw JSON rather than exploding it into a child table. `PresetLayer.override` is a
deliberately untyped `JsonObject` that "can name any field the style format has — including fields
added to the format later" (`TextPreset.kt:96`). A typed child table would have to be taught every
new style field and would defeat that design. Keep one parser, fed by three sources.

No `is_premium`, no `is_downloaded`, no `is_downloading`, no `file_path`, no `thumbnail_url`.
Premium is computed (§0.2) and there is nothing to download.

### 4.1 The migration — the highest-risk item in this feature

This section is unchanged from the first draft, because the risk is unchanged and it is the one
thing here that can destroy user data.

`AppDatabase` is built with `.fallbackToDestructiveMigration()` and **registers no migrations at
all** (`data/local/AppDatabase.kt:44`). Writing `MIGRATION_5_6` is not enough: unless it is passed
to `.addMigrations(...)`, bumping to version 6 takes the destructive path and drops every table —
including `ExportResult`, which is every saved user project.

Two traps behind that one:

- `exportSchema` is not configured and there is no `app/schemas/` directory. Room still validates
  the post-migration schema at open time; without exported schemas no `MigrationTestHelper` test is
  possible, so the first proof the migration works is a user's phone.
- Do not write `DEFAULT` clauses in the hand-written DDL. Room generates no defaults for Kotlin
  non-null properties unless they carry `@ColumnInfo(defaultValue = "…")`. A default on one side
  and not the other is a validation mismatch and Room throws
  `IllegalStateException: Migration didn't properly handle` on the next open. Pick one and make
  both sides agree.

```kotlin
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `text_presets` (
                `id` TEXT NOT NULL,
                `category` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `aspect` REAL NOT NULL,
                `layers_json` TEXT NOT NULL,
                `catalogue_version` INTEGER NOT NULL,
                `updated_at` TEXT,
                PRIMARY KEY(`id`)
            )
        """.trimIndent())
    }
}
```

```kotlin
Room.databaseBuilder(context, AppDatabase::class.java, "UrduPhotoDesigner.db")
    .addMigrations(MIGRATION_5_6)
    .fallbackToDestructiveMigrationFrom(1, 2, 3, 4)   // explicit, not a catch-all
    .build()
```

Turn schema export on so any of this is testable:

```kotlin
// app/build.gradle.kts
ksp { arg("room.schemaLocation", "$projectDir/schemas") }
sourceSets["androidTest"].assets.srcDir("$projectDir/schemas")
```

`5.json` has never existed — generate it by building once at version 5 *before* the bump, on a
clean checkout, or the migration test has nothing to migrate from. Commit `5.json` and `6.json`.

### 4.2 Installs below v5 have no path, deliberately

The database has gone 2 → 3 → 4 → 5, each bump destructive and none with a migration
(`git log -G"version = [0-9]"` on `AppDatabase.kt`: `90f20f4` v2, `658a417` v3, `e33c7a4` v4,
`3508780` v5). Installs not opened since before the v5 bump still hold a v2–v4 database and there
is no v4 schema to write a 4 → 6 migration against.

Those users get wiped — but they are wiped by the *current* release too, the moment they update.
No regression. The point of `fallbackToDestructiveMigrationFrom(1, 2, 3, 4)` over a blanket
fallback is that it names exactly which versions may be destroyed, so a future 6 → 7 bump that
forgets its migration crashes loudly in QA instead of quietly deleting everyone's projects. Every
bump from here writes a real migration.

---

## 5. Authoring, and the one guard it still needs

Confirmed: **presets are designed in the debug build of the app**, so `styleId` and `fontId` values
are always real ids the app itself produced, carried into the preset JSON. That removes the failure
mode I was worried about — a dashboard-authored preset naming a style that does not exist — and it
is the right call for the same reason the export-a-layout idea was: the app is the only thing that
can guarantee schema compatibility as the style format grows.

One gap it does **not** close. The designer's debug build can be ahead of what users have
installed. A preset authored against a style added last week resolves fine on the authoring device
and renders unstyled on a phone running the released APK — `resolveStyle` returns null for an
unknown id, and a null "means leave this layer unstyled and never fail the insertion"
(`TextPresetsRepository.kt:78`). The layer keeps its text and font and silently loses its colour,
stroke and extrude. Nobody gets an error; the preset just looks wrong.

The guard is cheap: the app publishes the style-catalogue version it ships with, the manifest
carries `style_catalogue_version`, and the client skips (or flags for fallback) any category
authored above its own. Add a validator run to the authoring flow that lists every `styleId` and
`fontId` a category uses, so a preset referencing something unreleased is caught at publish rather
than on a user's screen. `tools/validate-presets.js` already exists and is the natural home.

---

## 6. The category enum is a ceiling on dynamic content

`TextPresetCategory` is a fixed enum of ten occasions, and it is load-bearing three ways: it is the
bundled filename, the shelf tab title, and the authority for a preset's category
(`TextPresetCategory.kt`, `TextPresetsRepository.kt:113`, `TextFragment.kt:1665`).

So the dashboard can add lockups freely but **cannot add an eleventh occasion** without an app
release. If "Muharram" or "Nikah" needs to appear mid-season, the enum has to become data — a
category list in the manifest, with the enum kept only as the bundled-tier fallback.

This is a real product decision and it is not settled. Doing it later is a second refactor of the
same three call sites in `TextFragment`; doing it now costs little while that code is already being
made async in §2. Flagging rather than deciding — it is your call whether dynamic occasions matter
inside the next two seasons.

---

## 6A. Phase 0 — content, and why it comes first

Everything above makes the catalogue *updatable*. None of it makes the catalogue *better*. The
content work is separable, ships sooner, and has to land in the APK anyway because bundled
content is tier 3 of the fallback — it is what every user sees on install, before any sync.

Three workstreams, running in parallel:

### 6A.1 The Doodle style category

A new `PresetCategory.DOODLE`: thick, glossy, rounded 3D "tube" strokes in candy colours — fat
soft-plastic marker strokes with a specular sheen. Built from the existing ~50-field style
vocabulary: a thick `underStroke` as the tube body, a multi-stop `textGradient` or `hasBevel`
for the roundness, a low-radius white `hasInnerGlow` for the sheen, a soft offset `shadow` for
lift.

**This needs a renderer change first, and it is the only code in Phase 0.** Text strokes are
drawn with Paint defaults — `Join.MITER`, `Cap.BUTT`. Confirmed at
`CanvasView.kt:5364` (the under-stroke block) and its immediate successor (the primary stroke
block), plus a mirrored pair around `:6413`. `strokeCap` and `strokeJoin` appear nowhere in text
rendering in this repo; they exist only in the brush engine (`BrushProfile.kt`,
`BrushRenderUtils.kt`). At the stroke widths a tube look needs, MITER joins grow long spikes at
sharp corners, and Nastaleeq is full of sharp corners.

The fix is `strokeJoin = ROUND` / `strokeCap = ROUND` on those paints. The open question is
whether it applies globally or behind an opt-in flag: round joins change how every one of the
~650 existing stroked styles renders. Global is simpler and probably an improvement; opt-in via
a `roundStroke` field on `TextStylePreset` mapped through `TextStyleApplier` is the safe version.
**Decide on screenshot evidence, not reasoning** — sample the existing categories both ways.

Constraint that governs the content: a stroke heavy enough to read as a tube can swallow
Nastaleeq's counters and connecting strokes and make the word unreadable. Err thinner.

### 6A.2 Universal occasion coverage

The ten categories lean hard on the Islamic calendar — ramadan, eid and islamic are 150 of the
365 lockups. The catalogue needs to work year-round: personal milestones, social greetings,
commercial and shop use, motivational content, seasonal non-religious events, and the Islamic
occasions that are *not* Ramadan/Eid.

Taxonomy lands in `docs/preset-taxonomy.md` for review before the content is committed to. The
bar for a new shelf: it must carry 20+ genuinely distinct lockups. Declaration order in
`TextPresetCategory` is shelf order and should run by expected demand.

### 6A.3 Depth — 3 and 4 layer lockups

Most lockups today are one or two layers, which caps how designed they can look. `LAYOUTS` in
`tools/preset-content.js` is pure fractional geometry, so new multi-line templates are cheap —
but `TextPreset.verticalRoom()` documents the failure they must avoid: a short word given a wide
target becomes enormous and lands on the line below it. Three- and four-layer templates have to
respect that, and it is the thing to check first on a device.

### 6A.4 What guards this content

- `tools/generate-presets.js` is deterministic and preserves any lockup lacking a
  `"generated": true` marker, so hand-tuned heroes survive a re-run.
- Its `unusable()` filter already catches one real failure mode — near-white fill plus near-white
  emboss over a hairline dark stroke, invisible on both card plates. New styles must not trip it.
- `styleId` and `fontId` must resolve or the layer renders unstyled with no error. Font ids are
  literal filenames including odd spacing and mixed-case extensions
  (`"Jameel Noori Nastaleeq .ttf"`, `"Al Qalam Alvi Nastaleeq.TTF"`) — never normalized.
- The catalogue has been burned by volume-without-variety before: 317 near-duplicate styles were
  removed once, and Minimal still reads as one flat fill recoloured. Distinct *constructions*,
  then colour families across them — not one construction recoloured forty times.

---

## 7. Build order

**Phase 0 first** (§6A) — the renderer fix, the Doodle set, the taxonomy and the deeper layouts.
It ships in the APK independently of everything below, and the app release that eventually
carries the API client should carry this content too.

Then six slices, each its own commit, each independently verifiable:

1. **Migration infrastructure.** Schema export on, `5.json` generated and committed, `text_presets`
   table, `MIGRATION_5_6`, `addMigrations`, `MigrationTestHelper` test. Ship and verify no project
   loss before anything else lands.
2. **Data layer.** Entity, DAO, both repos, four use cases, DI wiring, endpoint methods.
3. **Repository source switch.** `TextPresetsRepository` gains the three-tier resolution behind its
   existing API; reads become `suspend`/`Flow`. Bundled tier still the only populated one, so
   behaviour is unchanged and the slice is provable on its own.
4. **`TextFragment` async.** The three synchronous call sites become observation. Still no network.
   This is the slice most likely to regress the shipped panel — treat it as a refactor with a device
   pass, not a plumbing change.
5. **Sync.** Manifest fetch, per-category version comparison, write-through to Room, the `enabled`
   kill switch in `PreferenceDataStoreKeysConstants`, `SectionStatus` wiring to match
   `_templatesStatus` (`MainViewModel.kt:155`).
6. **Authoring guard.** `style_catalogue_version` check, `tools/validate-presets.js` extended to
   report unreleased style and font ids.

---

## 8. Verification

**Before slice 1 — re-verify the merged shelf.** It came from a parallel agent session and was
merged the same day. My own notes on that work flag the Minimal style category as reading flat and
Calligraphy as never checked. Confirm the Presets tab builds, inserts, undoes and downloads fonts on
a device *before* layering anything on it, so a later failure is attributable.

Migration, which must not be hand-waved:

- `MigrationTestHelper` 5 → 6: seed `ExportResult` rows at v5, migrate, assert rows survive and
  `text_presets` exists.
- On a device: install the current store build, create two projects, install the new build over it,
  confirm both projects are still in Files. The instrumented test only proves the DDL.
- Confirm `.addMigrations` is actually reached — log inside `migrate()` temporarily, because a
  silently destructive migration passes every "does the app still open" check.

Fallback chain — the feature's whole point, so test all three tiers and the transitions:

- Fresh install, no network → bundled 365 lockups, all ten shelves, correct thumbnails.
- Fresh install, network, `enabled: true` → remote catalogue replaces bundled, no flicker, scroll
  position and drill state preserved if a sync lands while the panel is open.
- `enabled: false` → bundled content, exactly as today. The tab must not disappear or empty.
- Sync once, go offline, restart → Room tier serves, still ten shelves.
- Room populated, then a category's `version` bumps → only that category re-fetches.
- Remote preset whose `styleId` is not in the installed APK → renders text and font, unstyled, no
  crash. This is §5's failure mode; confirm it degrades rather than breaks.
- Recents shelf holding an id the remote catalogue has dropped → that entry disappears from the
  shelf, the others stay (`RecentsStore` resolves through the repository by design).
- A malformed remote category payload → that category falls back, the other nine unaffected.

Insertion, unchanged code but worth a regression pass after slice 4:

- Insert a preset → one group, one undo step, fonts downloaded first.
- Premium preset, unsubscribed → inserts freely, gate appears at export, rewarded-ad unlock works.
- LeakCanary across ten inserts and ten panel open/closes — this app keeps producing two leak
  shapes and slice 4 changes lifecycle-sensitive code.

---

## 9. Open items

1. **Dynamic categories** (§6) — decide before slice 4, since it touches the same code.
2. **Sync trigger and cadence** — on app start alongside templates, on panel open, or both.
   Templates fetch from `MainViewModel` init (`MainViewModel.kt:493`); matching that is the
   low-surprise default, but the presets panel is deeper in the editor than Home is.
3. **Whether remote presets can remove bundled ones.** If `ramadan_006` ships in the APK and the
   dashboard deletes it, does it vanish or does the bundled tier resurrect it? Recommend: remote is
   authoritative when Room is populated, and a tombstone list in the manifest suppresses bundled ids.
   Not decided.
