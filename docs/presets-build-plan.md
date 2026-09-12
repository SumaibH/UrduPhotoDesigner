# Dynamic Presets — verified build plan

Written against the code as it stands on `production` @ c24b56e. Every claim below was checked
against a file, and the file:line is given so it can be rechecked when the code moves.

---

## 0. Verdict on the original plan

What was right, and can be kept as-is:

- `ElementType.GROUP` exists and the sentinel pattern is real — sentinel `id` == children's
  `groupId`, sentinel carries no `groupId` of its own. `CanvasViewModel.kt:1842`
  (`selectElementForGrouping`) is the exact shape to copy.
- `CanvasAction.UpdateCanvasElementsOrder` genuinely gives 1-tap undo of a whole insert. Its
  replay calls `restoreWithContext(context)` on every element (`CanvasViewModel.kt:6651`), so
  undo/redo rehydrates correctly rather than restoring inert copies.
- Filtering `ElementType.BACKGROUND` out is correct, and filtering *by type* rather than by index
  is the right call — the loader elsewhere assumes background is `elements[0]`
  (`CanvasViewModel.kt:6914`), which is an assumption worth not inheriting.
- Room + reactive `Flow` + Glide thumbnails + API refresh is exactly how templates already work,
  so the shape of the data layer is familiar and correct.

What is wrong, in descending order of damage:

### 0.1 The migration section would destroy user data — as written it is worse than no plan

`AppDatabase` is built with `.fallbackToDestructiveMigration()` and **registers no migrations at
all** (`data/local/AppDatabase.kt:44`). Writing `MIGRATION_5_6` is not enough: unless it is passed
to `.addMigrations(...)`, bumping the version to 6 takes the destructive path and drops every
table — including `ExportResult`, which is every saved user project. The plan's own stated goal
("avoids wiping user projects") is defeated by a line it never mentions.

Two further traps behind that one:

- `exportSchema` is not configured and there is no `app/schemas/` directory. Room still validates
  the post-migration schema against the compiled one at open time; without exported schemas there
  is no `MigrationTestHelper` test possible, so the first proof the migration works is a user's
  phone.
- The plan's DDL writes `DEFAULT 0` on the boolean/int columns. Room does **not** generate defaults
  for Kotlin non-null properties unless the property carries `@ColumnInfo(defaultValue = "0")`. A
  default present in the hand-written DDL but absent in the generated schema is a validation
  mismatch, and Room throws `IllegalStateException: Migration didn't properly handle` on the next
  open. Either drop the `DEFAULT` clauses or put `@ColumnInfo(defaultValue = ...)` on the entity —
  pick one and make both sides agree.

### 0.2 The name collides with a feature that already exists

`assets/presets/text_styles.json`, `TextStylePreset`, `PresetCategory` and `TextStylesRepository`
already own the word "preset" in this codebase, and they mean a *text style* — colour, stroke,
extrude, bevel. Adding `PresetEntity`, `PresetsRepo`, `PresetDao` and
`assets/presets/default_presets.json` next to them makes every future `grep preset` ambiguous, and
the two things are applied through completely different code paths (`addTextWithStyle` at
`CanvasViewModel.kt:4740` vs. a group insert).

Recommendation: keep **"Presets"** as the user-facing word if that is what tests well in Urdu, but
name the code after what it is — a multi-layer **Layout**. `LayoutEntity`, `LayoutDao`,
`LayoutsRepo`, `ui/editor/panels/layouts/`, `assets/layouts/`. Costs nothing now, saves a rename
later. The rest of this document uses the code name.

### 0.3 The premium gate contradicts how every other premium asset in the app behaves

Nothing in this app paywalls at tap. Premium is carried per-element as `isPremium` / `isSubscribed`
(`CanvasElement.kt:304-306`), rolled up by `hasPremiumAsset` (`CanvasViewModel.kt:7087` —
`list.any { !it.isSubscribed && it.isPremium }`), and gated once, at export, by
`ExportFragment.isPremiumLocked()` (`ExportFragment.kt:1013`), which also honours
`isSessionExportUnlocked` (the rewarded-ad path).

Blocking at tap, as §5.1 proposed, would be the only place in the app that does so — and it is the
weaker funnel: the user has invested nothing yet. Let them insert the premium layout, see it on
their photo, and meet the gate at export where the unlock path already exists.

Concretely: set `isPremium = true` on the inserted children, and
`isSubscribed = billingManager.isSubscribed.value && isPremium` to match the template loader
(`CanvasViewModel.kt:6946`). Do nothing else. The badge on the tile stays — `TemplatesAdapter.kt:92`
is the pattern: `is_premium && !is_subscribed`.

### 0.4 Font collection is incomplete — and there is already a correct helper going unused

The plan (and the existing template loader at `CanvasViewModel.kt:6862`) does:

```kotlin
elements.filter { it.type == ElementType.TEXT }.mapNotNull { it.fontId }
```

`CanvasElement.collectFontIds()` (`CanvasElement.kt:907`) already exists and also covers
`ElementType.TABLE` via `tableData.allFontIds()`. It is currently dead code — zero callers. Any
layout containing a table would download the wrong font set and render in the fallback face. Use
`elements.flatMap { it.collectFontIds() }.distinct()`, and fix the template loader on the same pass.

### 0.5 Hydration is absent from the plan, and without it the elements are inert

A `CanvasElement` straight out of Gson cannot be drawn and cannot even be *measured*: `context` is
`@Transient` (`CanvasElement.kt:30`), bitmaps are still base64 strings, `DRAW` strokes have a null
`Path`, and `paint.typeface` is unset. The template loader does the full sequence at
`CanvasViewModel.kt:6875-6907`:

1. `copy(adjustments = adjustments ?: AdjustmentValues())` — null adjustments crash downstream
2. `copy(context = context)`
3. `drawStrokes?.forEach { it.restorePath() }`
4. `restoreWithContextBackground(context)` — the base64 → Bitmap decode; expensive, must stay off
   the main thread
5. resolve typeface from `_localFonts` by `fontId` with `R.font.default_canvas` fallback
6. on the main thread, `applyTypefaceFromFontList()` per TEXT element

This matters for ordering, not just completeness: **step 5 must happen before any bounds are
measured**, because text bounds come from the paint. Measuring first and hydrating second gives a
bounding box computed against the default face and a layout that lands in the wrong place at the
wrong size.

### 0.6 Scale normalization as described will distort text

`element.scale *= k` alone is wrong twice over. Positions have to scale about the preset's own
bounding-box centre before the group is re-centred, or the layout shears apart; and TEXT carries an
independent per-token `token.scale` (`CanvasElement.kt:522`) that interacts with the ink-bounds
sizing. Apply one uniform factor to `x`, `y` *and* `scale`, measured about the group centre, once,
after hydration — never per-element ad hoc.

### 0.7 zIndex was not mentioned, and the layout will insert underneath the user's work

The preset JSON carries its own `zIndex` values, authored for an empty canvas. Inserted as-is they
interleave with, or sit under, whatever the user already has. Rebase them: take
`maxOf { it.zIndex }` of the **current** canvas and offset the whole preset above it — the sentinel
taking the highest, the way `selectElementForGrouping` does (`CanvasViewModel.kt:1849`).

### 0.8 Analytics will report every insert as a layer reorder

The action mapping sends `UpdateCanvasElementsOrder` to `"layers" to "reorder"`
(`CanvasViewModel.kt:6150`). Reusing that action for undo is right; but without a new branch,
`tool_action_performed` will file every layout insert under the layers panel and the feature will be
invisible in GA4. Add a `CanvasAction.AddLayout` (wrapping the same old/new lists) and map it to
`"layouts" to "apply_layout"`.

### 0.9 The layering skips the project's own conventions

Every fetched feature here is five files, not two:

- `domain/repo/FetchXRepo.kt` — network, returns `Flow<Response<T>>` (`common/sealed/Response.kt`)
- `domain/repo/XRepo.kt` + `data/repository/XRepoImpl.kt` — Room
- `domain/usecase/FetchAPIXUseCase.kt`, `GetXUseCase.kt`, `InsertXUseCase.kt`, `UpdateXUseCase.kt`
- `MainViewModel` consumes only use cases (`MainViewModel.kt:89-92`)

The plan collapses this into one `PresetsRepo` consumed directly. It will work, and it will be the
one feature shaped differently from the other six.

### 0.10 `is_downloading` should not be a database column

`TemplateEntity.is_downloading` is persisted, so a process death mid-download leaves a row stuck
`is_downloading = true` forever with no recovery path. Don't inherit the bug: hold in-flight ids in a
ViewModel `StateFlow<Set<Int>>`, the way `FontsAdapter` holds `downloadingIds`. Persist only
`is_downloaded` and `file_path`.

### 0.11 One thing that is easy to lose

`TemplatesRepoImpl.insertTemplates` merges on insert, preserving `is_downloaded`, `file_path` and
`download_progress` from the existing row (`TemplatesRepoImpl.kt:20-30`). Without the same merge,
every API refresh resets the download state and the user re-downloads everything. Copy it verbatim.

---

## 1. Server contract

Mirror the template endpoints rather than inventing a parallel style.

```
GET /layouts            → LayoutsResponse
GET /layout/json/{id}   → streamed body (same shape as template/json/{id})
```

`TemplatesResponse` today is `data class TemplatesResponse(val templates: List<TemplateEntity>)` —
no `status`, no `message`, no `enabled`. The proposed envelope is new server work; it is worth
doing, but say so out loud.

```json
{
  "status": true,
  "enabled": true,
  "layouts": [
    {
      "id": 101,
      "name": "Jashn-e-Azadi Heading",
      "category": "Poetry",
      "tags": ["azadi", "poetry"],
      "is_premium": false,
      "is_popular": true,
      "thumbnail_url": "layouts/thumbnails/thumb_101.webp",
      "json_url": "layouts/json/layout_101.json",
      "json_size": 48213,
      "canvas_width": 1080,
      "canvas_height": 1080,
      "element_count": 4,
      "created_at": "2026-09-12 10:00:00",
      "updated_at": "2026-09-12 10:00:00"
    }
  ]
}
```

Two additions over the original:

- **`json_size`** — templates already carry it and `DownloadRepo.downloadTemplateById` prefers it
  over `Content-Length` (`DownloadRepo.kt:56`), which is the only way to get honest progress when
  the server streams chunked.
- **`element_count`** — lets the client refuse an absurd layout before downloading it. Pair it with a
  server-side cap: a layout with base64 images embedded can reach tens of MB, and this feature is
  meant to insert *lockups*, not whole designs. Cap at roughly 12 elements and 2 MB, enforced at
  publish time in the dashboard.

Drop `fonts: ["12","45"]` from the response. The client must derive font ids from the JSON anyway
(`collectFontIds`), and a second copy in the listing is a copy that goes stale the moment a designer
re-uploads the JSON without re-saving the row.

**Authoring path worth building:** these JSONs should come out of the app itself — select elements,
group, "Export as layout" behind a debug/admin flag — not be hand-assembled in the dashboard. It is
the only way to guarantee schema compatibility as `CanvasElement` keeps growing, and `CanvasElement`
has grown a lot.

---

## 2. Data layer

```
data/model/LayoutEntity.kt          @Entity(tableName = "layouts")
data/model/LayoutsResponse.kt
data/local/LayoutDao.kt
data/local/AppDatabase.kt           +layoutsDao(), version 6, addMigrations(MIGRATION_5_6)
data/local/Migrations.kt            [NEW] MIGRATION_5_6
data/remote/EndPointsInterface.kt   +getLayouts(), +getLayoutJson()
domain/repo/FetchLayoutsRepo.kt     network → Flow<Response<LayoutsResponse>>
domain/repo/LayoutsRepo.kt          + data/repository/LayoutsRepoImpl.kt
domain/usecase/                     FetchAPILayoutsUseCase, GetLayoutsUseCase,
                                    InsertLayoutsUseCase, UpdateLayoutsUseCase
domain/repo/DownloadRepo.kt         +downloadLayoutById()
```

`LayoutEntity` is `TemplateEntity` minus `download_progress` and `is_downloading`, plus
`element_count`. Do **not** give it a `toExportResultFinal()` — a layout is never a project.

### 2.1 The migration, done properly

`data/local/Migrations.kt`:

```kotlin
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `layouts` (
                `id` INTEGER NOT NULL,
                `name` TEXT NOT NULL,
                `category` TEXT,
                `tags` TEXT NOT NULL,
                `is_premium` INTEGER NOT NULL,
                `is_popular` INTEGER NOT NULL,
                `is_subscribed` INTEGER NOT NULL,
                `thumbnail_url` TEXT NOT NULL,
                `json_url` TEXT NOT NULL,
                `json_size` INTEGER NOT NULL,
                `canvas_width` INTEGER NOT NULL,
                `canvas_height` INTEGER NOT NULL,
                `element_count` INTEGER NOT NULL,
                `file_path` TEXT,
                `is_downloaded` INTEGER NOT NULL,
                `created_at` TEXT,
                `updated_at` TEXT,
                PRIMARY KEY(`id`)
            )
        """.trimIndent())
    }
}
```

No `DEFAULT` clauses, because the entity carries no `@ColumnInfo(defaultValue = ...)`. If you add
one, add the other.

`AppDatabase.buildDatabase`:

```kotlin
Room.databaseBuilder(context, AppDatabase::class.java, "UrduPhotoDesigner.db")
    .addMigrations(MIGRATION_5_6)
    .fallbackToDestructiveMigrationFrom(1, 2, 3, 4)   // see below — explicit, not a catch-all
    .build()
```

### 2.2 Users below v5 have no path, and that is deliberate

The database has gone 2 → 3 → 4 → 5, each bump destructive and none of them with a migration
(`git log -G"version = [0-9]"` on `AppDatabase.kt`: `90f20f4` v2, `658a417` v3, `e33c7a4` v4,
`3508780` v5). So installs that have not been opened since before the v5 bump still hold a v2–v4
database, and there is no 4 → 6 path to write — there is no exported v4 schema to write it against.

Those users get wiped. That is not a regression this change introduces: under the current release
they are wiped by the existing 2/3/4 → 5 destructive fallback the moment they update. The point of
`fallbackToDestructiveMigrationFrom(1, 2, 3, 4)` over a blanket `fallbackToDestructiveMigration()` is
that it names exactly which versions are allowed to be destroyed, so a future 6 → 7 bump that forgets
its migration crashes loudly in QA instead of quietly deleting everyone's projects. Every bump from
here on writes a real migration.

And turn schema export on so this is testable at all:

```kotlin
// app/build.gradle.kts
ksp { arg("room.schemaLocation", "$projectDir/schemas") }
sourceSets["androidTest"].assets.srcDir("$projectDir/schemas")
```

Commit the generated `schemas/…/5.json` and `6.json`. Note that `5.json` has never existed —
generate it by building once at version 5 *before* the bump, on a clean checkout, or the migration
test has nothing to migrate from.

---

## 3. Sync and the kill switch

`enabled` from the response goes into `PreferenceDataStoreAPI` under a new `LAYOUTS_API_ENABLED` key
in `PreferenceDataStoreKeysConstants`.

| State | What the user sees |
|---|---|
| API reachable, `enabled: true` | Fresh list, merged into Room, thumbnails via Glide |
| API reachable, `enabled: false` | Tab hidden entirely — not an empty grid |
| API unreachable | Whatever Room holds, via `Flow`; downloaded layouts still insert |
| Room empty and offline | Empty state with retry, matching `SectionStatus.Failed` elsewhere |

Wire `_layoutsStatus: MutableStateFlow<SectionStatus>` the way `_templatesStatus` is wired
(`MainViewModel.kt:155`) so it participates in the existing home-state aggregation.

**Removed from the original plan:** the bundled `assets/presets/default_presets.json` fallback. It
duplicates the Room cache, which already survives offline; it collides with the existing
`assets/presets/` path used by text styles; and bundled JSON goes stale against a `CanvasElement`
schema that changes every few releases, so it is the most likely thing in this feature to ship broken
and never be noticed. If day-one content before the first sync really matters, bundle it under
`assets/layouts/` and treat it as seed data written into Room on first run — not as a second
rendering path.

Also mirror `updatePremiumEntitlement(subscribed)` on purchase (`MainViewModel.kt:518`) so crown
badges clear the moment someone subscribes.

---

## 4. The insertion pipeline — the part that has to be exactly right

`CanvasViewModel.insertLayout(layout: LayoutEntity, context: Context)`, on `Dispatchers.Default`,
switching to Main only for the LiveData write. The order is not negotiable:

1. **Read and decode.** `ProjectCodec.toPlainJsonFile(sourceFile, tempJson)` first — the existing
   loader auto-detects `.urdc` vs plain JSON by magic bytes (`CanvasViewModel.kt:6833`) and layouts
   must go through the same door, or the day the dashboard starts serving `.urdc` this silently
   breaks. Stream into Gson; never `readText()`.
2. **Drop the background.** `filter { it.type != ElementType.BACKGROUND }`. By type, not index.
3. **Fonts.** `elements.flatMap { it.collectFontIds() }.distinct()` → `fontGate.ensureFonts(ids)`.
   `FontGate` already resolves missing-only, downloads in parallel and waits for the DB to reflect
   readiness (`viewmodels/FontGate.kt:25`). **Add the fallback it currently lacks:** `ensureFonts`
   hangs in `waitUntilFontsReady` forever if a font id in the JSON no longer exists server-side,
   because `first { … }` is never satisfied. Give it a timeout and fall through to
   `R.font.default_canvas`. That is the original plan's §5.2 and it is a genuinely good catch — but
   it belongs inside `FontGate`, benefiting template opens too, not bolted onto layouts.
4. **Hydrate**, in the order at `CanvasViewModel.kt:6875-6907`: adjustments null-guard →
   `copy(context)` → `restorePath()` on draw strokes → `restoreWithContextBackground(context)` →
   typeface from `_localFonts` with the `default_canvas` fallback.
5. **Measure**, now that the paints are real. Union the per-element bounds into one RectF.
6. **Fit and centre.** `k = min(1f, (canvasW * 0.8f) / boundsW, (canvasH * 0.8f) / boundsH)`. For
   each element: scale `x`, `y` about the bounds centre by `k`, multiply `scale` by `k`, then
   translate so the bounds centre lands on `(canvasW / 2, canvasH / 2)`.
7. **Re-identify.** New `UUID` per child; `groupId = newGroupId` on all of them; new sentinel with
   `id = newGroupId`, `customName = layout.name`, `groupId = null`, `isGroupCollapsed = false` —
   copying `selectElementForGrouping` (`CanvasViewModel.kt:1848`).
8. **Rebase zIndex** above the current canvas maximum, sentinel highest.
9. **Premium.** `isPremium = layout.is_premium`,
   `isSubscribed = billingManager.isSubscribed.value && layout.is_premium`.
10. **Apply and record.** On Main: `_canvasElements.value = old + children + sentinel`, then
    `_canvasActions.push(CanvasAction.AddLayout(oldList, newList))` with both lists mapped through
    `copy(context = null)`, `_redoStack.clear()`, `notifyUndoRedoChanged()`. Undo replay reuses the
    `UpdateCanvasElementsOrder` branch's `restoreWithContext` logic (`CanvasViewModel.kt:6651`).
11. **Select the group.** Do not set `isSelected` by hand — group selection resolves through a child
    in places (`CanvasViewModel.kt:3931`). Go through the same entry point the layers panel uses when
    a group row is tapped, so the bounding box and handles behave identically.

---

## 5. UI

`ui/editor/panels/layouts/` — `LayoutsFragment`, `LayoutsAdapter`, and a category row reusing
`TemplateCategoriesAdapter` rather than a new one.

- Grid of thumbnails, Glide, shimmer placeholder — same as `TemplatesAdapter`.
- Crown badge: `is_premium && !is_subscribed` (`TemplatesAdapter.kt:92`).
- Tap → spinner overlay on that tile, ids held in a ViewModel `StateFlow<Set<Int>>`. No percentage
  text in a tile that small; the original plan was right about this.
- If `ensureFonts` reports a stage, surface it through the existing `_loadingStage` pair rather than
  a new dialog — that plumbing already drives the template-open progress UI.

**Removed:** the "One-Tap Ungroup & Edit" prompt from §5.4. Ungroup is already on the selection
toolbar, and `ungroupElements()` already handles selection-of-child and selection-of-sentinel
(`CanvasViewModel.kt:1888`). Selecting the sentinel on insert — step 11 — puts the handles and that
toolbar in front of the user without a coach mark. Add the coach mark only if device testing shows
people don't find it.

**Where the tab lives:** `panels/text/` has subtabs (`fonts`, `styles`, `format`, `threed`, …) and
`styles` there means *text* styles. A layout is not a text property, so it does not belong there. Put
it at the top level of the editor panels beside `objects`, or as a second pager tab on the Templates
screen. Top-level in the editor is the stronger placement: the whole value is inserting a lockup onto
a photo the user already has open.

---

## 6. Analytics

New `CanvasAction.AddLayout` → `"layouts" to "apply_layout"` in the action mapping
(`CanvasViewModel.kt:6114-6158`), and `getActionDetailForAction` returns the layout category so the
report can say *which* layouts earn their place — the same reasoning already written into that
function's comment for colours and gradients.

Impressions: reuse `analytics/impressions/TemplateImpressionTracker` rather than writing a second one.

---

## 7. Build order

Six slices, each its own commit, each independently verifiable:

1. **Migration infrastructure.** Schema export on, `5.json` generated and committed, `layouts` table,
   `MIGRATION_5_6`, `addMigrations`, `MigrationTestHelper` test. Ship this alone and verify no
   project loss before anything else lands.
2. **Data layer.** Entity, DAO, both repos, four use cases, DI wiring, endpoint methods.
3. **Sync.** `MainViewModel` fetch + observe + `SectionStatus`, the `enabled` kill switch in the
   datastore, entitlement mirror on purchase.
4. **`FontGate` hardening.** Timeout + default-face fallback in `waitUntilFontsReady`, and switch the
   template loader to `collectFontIds()`. Standalone fix; benefits templates too.
5. **`insertLayout`.** The pipeline in §4, plus `CanvasAction.AddLayout` and its analytics mapping.
6. **UI.** Fragment, adapter, category row, spinner state, empty/failed states.

---

## 8. Verification

Migration — the one that must not be hand-waved:

- `MigrationTestHelper` 5 → 6: seed `ExportResult` rows at v5, migrate, assert the rows survive and
  `layouts` exists.
- On a device: install v31 from the store build, create two projects, install the new build over it,
  confirm both projects are still in Files. This is the real test; the instrumented one only proves
  the DDL.
- Confirm `.addMigrations` is actually reached — temporarily log inside `migrate()`, because a
  silently-destructive migration passes every "does the app still open" check.

Network and offline:

- `enabled: false` → tab absent, not empty.
- Airplane mode with a previously downloaded layout → inserts with no network.
- Airplane mode with an undownloaded layout → clean failure on the tile, no crash, spinner clears.
- Kill the process mid-download, reopen → tile is tappable again (this is what §0.10 buys).

Insertion:

- A layout whose JSON references a font id deleted from the server → inserts in the default face
  within a few seconds, does not hang.
- A layout containing a TABLE element → correct fonts. This is the `collectFontIds` fix; it will fail
  before slice 4, which is the point of testing it.
- Insert onto a canvas that already has elements → layout lands on top, not underneath.
- Insert a 1080×1080 layout into a 1080×1920 canvas → fits inside 80%, stays centred, text not
  distorted.
- Undo once → entire layout gone, background and existing elements untouched. Redo → returns
  hydrated, text still in the right face. Verify this; don't assume `restoreWithContext` covers it.
- Premium layout, unsubscribed → inserts freely, export shows the premium gate, rewarded-ad unlock
  still works.
- Rotate the device mid-insert.
- LeakCanary across ten inserts and ten undos — this path decodes bitmaps and holds `Context`, which
  is one of the two leak shapes this app keeps producing.
