# Presets API — backend brief

**For:** the dashboard/backend developer
**App:** Urdu Photo Designer (UrduCanvas), Android
**Dashboard:** `https://dashboard.urdufonts.com`
**Status:** app-side client not yet built — see §11 for how this sequences

This brief is self-contained. You do not need the Android source to build against it.

---

## 1. What you are building, in one paragraph

The app ships with 365 **presets** — finished Urdu text lockups ("رمضان / مبارک" arranged,
styled and positioned) that a user taps to drop onto their photo. Today all 365 are baked into
the APK as JSON files, so adding a preset for Muharram or a sale season means shipping an app
update and waiting on Play review. You are building the API that lets those presets be added,
edited and retired from the dashboard instead, so content ships the same day it is designed.

A preset is **not** an image and **not** a design file. It is a small JSON object describing two
or three text layers: what each says, which font and style it wears, and where it sits as a
fraction of its box. They are tiny — a few hundred bytes each. There is nothing to upload to a
CDN, no thumbnails to generate, no binaries to serve.

---

## 2. Hard constraints — the contract the app depends on

These are not preferences. The app's parser is already written and shipped; deviating breaks it.

1. **The category payload must be a bare JSON array**, not an object with a wrapper. One parser
   serves both the bundled files and your endpoint, and the bundled files are bare arrays.
2. **A preset entry must NOT carry a `category` field.** The endpoint path is the authority for
   which category a preset belongs to, exactly as the filename is today. An entry that claims a
   category is ignored at best and confusing at worst.
3. **`id` is authored, string, and permanent.** Format `{category}_{nnn}` — `ramadan_006`,
   `wedding_014`. The app stores these ids in the user's "Recents" shelf, so a renamed id reads
   to the user as a preset that vanished. Never reassign or renumber. Never use an auto-increment
   integer.
4. **`fontId` and `styleId` are opaque strings that must match existing app assets exactly.**
   See §8 — this is where a well-meaning backend will break everything.
5. **The ten categories are fixed** for now: `ramadan`, `eid`, `islamic`, `wedding`, `birthday`,
   `pakistan`, `business`, `quotes`, `sale`, `condolence`. Lowercase, in the URL path. The app
   cannot display an eleventh category without an app update, so the dashboard must not offer one.

---

## 3. Current content, for sizing

| Category | Presets today |
|---|---|
| ramadan | 50 |
| eid | 50 |
| islamic | 50 |
| pakistan | 40 |
| wedding | 40 |
| birthday | 30 |
| business | 30 |
| quotes | 30 |
| sale | 25 |
| condolence | 20 |
| **Total** | **365** |

Expect the full catalogue to sit in the low thousands eventually. A category payload is a few
hundred KB at most. No pagination needed.

---

## 4. Authentication

Identical to the existing endpoints (`/fonts`, `/templates`, `/canvas/sizes`):

```
Header: X-API-KEY: <the existing app API key>
Base:   https://dashboard.urdufonts.com/api/
```

No user auth. No per-user content. Same key the app already sends everywhere else.

---

## 5. Endpoint 1 — manifest

```
GET /api/presets
```

Cheap, called on app start. Tells the app whether the feature is on and which categories changed
since last sync, so it only downloads what moved.

```json
{
  "status": true,
  "message": "Presets fetched successfully",
  "enabled": true,
  "style_catalogue_version": 1,
  "categories": [
    { "category": "ramadan",    "version": 7, "count": 52, "updated_at": "2026-09-14 10:00:00" },
    { "category": "eid",        "version": 3, "count": 50, "updated_at": "2026-09-13 18:22:00" },
    { "category": "islamic",    "version": 1, "count": 50, "updated_at": "2026-09-12 09:00:00" },
    { "category": "wedding",    "version": 1, "count": 40, "updated_at": "2026-09-12 09:00:00" },
    { "category": "birthday",   "version": 1, "count": 30, "updated_at": "2026-09-12 09:00:00" },
    { "category": "pakistan",   "version": 1, "count": 40, "updated_at": "2026-09-12 09:00:00" },
    { "category": "business",   "version": 1, "count": 30, "updated_at": "2026-09-12 09:00:00" },
    { "category": "quotes",     "version": 1, "count": 30, "updated_at": "2026-09-12 09:00:00" },
    { "category": "sale",       "version": 1, "count": 25, "updated_at": "2026-09-12 09:00:00" },
    { "category": "condolence", "version": 1, "count": 20, "updated_at": "2026-09-12 09:00:00" }
  ]
}
```

Field rules:

- **`enabled`** — the kill switch. See §10. When `false` the app ignores the API entirely and
  shows its bundled content. Must be readable from a single dashboard setting, no deploy.
- **`version`** — an integer per category, **incremented on every publish, unpublish or edit of
  any preset in that category**. This is the only thing the app compares. If you forget to bump
  it, clients never see the change. Bump it in the same transaction as the write.
- **`count`** — published presets in that category. Informational; the app does not rely on it.
- **`style_catalogue_version`** — see §9. Ship `1` and leave it until told otherwise.
- Always return **all ten categories**, even at version 1 with zero changes. The app diffs the
  full list.

---

## 6. Endpoint 2 — category payload

```
GET /api/presets/{category}        e.g. /api/presets/ramadan
```

Returns a **bare JSON array** of published presets in that category. This is the exact format of
the files already in the APK — copy it literally.

```json
[
  {
    "id": "ramadan_001",
    "name": "Ramadan Mubarak — Stacked Gold",
    "aspect": 1,
    "layers": [
      {
        "text": "رمضان",
        "fontId": "Jameel Noori Nastaleeq .ttf",
        "styleId": "gold_001",
        "xPct": 0.5,
        "yPct": 0.38,
        "widthPct": 0.72,
        "rotation": 0,
        "align": "CENTER"
      },
      {
        "text": "مبارک",
        "fontId": "Jameel Noori Nastaleeq .ttf",
        "styleId": "gold_001",
        "override": {
          "textGradientColors": ["#FFF3C4", "#E8C877", "#B98A2E"]
        },
        "xPct": 0.5,
        "yPct": 0.64,
        "widthPct": 0.5,
        "rotation": 0,
        "align": "CENTER"
      }
    ]
  }
]
```

### Field reference

**Preset object**

| Field | Type | Required | Notes |
|---|---|---|---|
| `id` | string | yes | `{category}_{nnn}`. Permanent. Unique across the whole catalogue, not just the category. |
| `name` | string | yes | Internal/admin label. Not currently shown to users. |
| `aspect` | number | no, default `1` | Width ÷ height of the box the lockup was composed in. Must be > 0. |
| `layers` | array | yes | 1–5 objects. A preset with zero layers is dropped by the app as malformed. |

**Layer object**

| Field | Type | Required | Notes |
|---|---|---|---|
| `text` | string | yes | The Urdu or English the layer renders. A layer without it is dropped. |
| `fontId` | string | no | Exact `file_name` from your `fonts` table. See §8. |
| `styleId` | string | no | An id from the app's style catalogue. See §8. |
| `override` | object | no | Free-form style field overrides. **Store and serve verbatim** — do not validate its keys, do not normalize, do not reformat. New style fields get added over time and a strict schema here would reject valid content. |
| `xPct` | number | no, default `0.5` | Horizontal centre, fraction of box. App clamps to 0–1. |
| `yPct` | number | no, default `0.5` | Vertical centre, fraction of box. App clamps to 0–1. |
| `widthPct` | number | no, default `0.8` | How wide the line measures, fraction of box. App clamps to 0.05–1. |
| `rotation` | number | no, default `0` | Degrees. |
| `align` | string | no, default `"CENTER"` | One of `LEFT`, `CENTER`, `RIGHT`. |

### Response rules

- Unpublished / draft presets must **not** appear.
- An unknown category returns `[]` with HTTP 200, not a 404. The app treats a category as
  legitimately empty rather than as an error.
- Order is preserved as the array is served — serve them in the admin's chosen display order.

---

## 7. Database schema (suggested)

```sql
CREATE TABLE presets (
  id                VARCHAR(64) PRIMARY KEY,      -- 'ramadan_006', authored, never reassigned
  category          VARCHAR(32) NOT NULL,         -- one of the ten
  name              VARCHAR(255) NOT NULL,
  aspect            DECIMAL(6,3) NOT NULL DEFAULT 1.000,
  layers            JSON NOT NULL,                -- the layers array, stored whole
  is_published      BOOLEAN NOT NULL DEFAULT 0,
  sort_order        INT NOT NULL DEFAULT 0,
  created_at        TIMESTAMP,
  updated_at        TIMESTAMP,
  INDEX idx_cat_pub (category, is_published, sort_order)
);

CREATE TABLE preset_categories (
  category          VARCHAR(32) PRIMARY KEY,      -- seeded with the ten, not user-creatable
  version           INT NOT NULL DEFAULT 1,       -- bump on ANY change to this category
  updated_at        TIMESTAMP
);

CREATE TABLE preset_settings (
  id                        TINYINT PRIMARY KEY DEFAULT 1,
  enabled                   BOOLEAN NOT NULL DEFAULT 0,
  style_catalogue_version   INT NOT NULL DEFAULT 1
);
```

**Store `layers` as a JSON column, whole.** Do not explode it into a `preset_layers` table. The
`override` field is deliberately schema-free and can name style properties that do not exist yet;
a relational shape would have to be migrated every time the app's style format grows. One column,
served back verbatim.

---

## 8. Publish-time validation — read this twice

This is the section most likely to be got wrong, and the failure is silent: a bad `fontId` or
`styleId` does not crash the app or show an error. The layer just renders in the wrong font or
with no styling at all, and the preset quietly looks broken on the user's phone.

### `fontId` must exactly match `fonts.file_name`

These are literal filenames, with extensions, mixed case, spaces, and vendor suffixes. Real
examples from shipped content:

```
"Jameel Noori Nastaleeq .ttf"                              ← note the space before .ttf
"AA Sameer Rafiya Unicode Regular - [UrduFonts.com].ttf"
"Al Qalam Alvi Nastaleeq.TTF"                              ← uppercase extension
"Abdo Line.otf"
```

**Do not slugify, trim, lowercase, URL-encode, or "clean" these strings.** Validate with an exact,
case-sensitive string comparison against `fonts.file_name`. Reject the publish if it does not
match, and show the admin which one failed.

### `styleId` must exist in the app's style catalogue

Style ids look like `gold_001`, `threed_014`, `neon_007` — roughly 650 of them, currently bundled
in the app, not in your database. Until they are served by the API too (not in scope here), you
cannot validate these against a table.

**What to do instead:** the person authoring presets will give you a flat list of valid style ids
exported from the app. Store it as a reference table (`preset_style_ids`) or a config file, and
validate against it at publish. Ask for a refreshed list whenever the app ships new styles.

### Other publish checks

- `id` matches `^[a-z]+_[0-9]{3,}$` and its prefix equals its category.
- `id` is unique across the entire `presets` table.
- `layers` is a non-empty array of at most 5 objects.
- Every layer has a non-empty `text`.
- `aspect` > 0.
- `xPct`, `yPct` within 0–1; `widthPct` within 0.05–1.
- `align` is one of `LEFT`, `CENTER`, `RIGHT`.
- `override`, if present, is a JSON object (not an array, not a string).

Reject the publish with a clear per-field message. Never auto-correct — a silently "fixed" preset
is one nobody checks.

---

## 9. `style_catalogue_version`

Presets are designed inside a debug build of the app, so the style ids they reference are always
real. The one gap: the designer's build can be **ahead of** the version users have installed. A
preset using a style added last week renders unstyled on an older phone.

The mechanic: each app release knows which style-catalogue version it ships with, and skips
content authored above it.

**For Monday: return `1` and do nothing else.** Bump it only when you are told the app has shipped
a new style set, and add a `min_style_version` column to `presets` at that point. Noting it now so
the field exists in the contract from day one rather than being a breaking addition later.

---

## 10. The `enabled` kill switch, and how Monday actually goes

`enabled` is a single boolean in `preset_settings`, flippable from the dashboard with no deploy.

- `enabled: true` → app uses the API.
- `enabled: false` → app ignores the API completely and uses its bundled 365 presets.

The app's fallback order is **API → its own cache → bundled content**, so there is no state in
which the presets panel is empty or broken. If your endpoint 500s, times out, or returns
nonsense, the user sees the bundled catalogue and notices nothing.

**Ship Monday with `enabled: false`.** Get the endpoints live, get content loaded, verify with
curl. Flip it to `true` only once the app release carrying the client has landed — see §11.

---

## 11. Sequencing — please read, it affects what "done" means

One thing needs to be said plainly: **the app cannot start using this API on Monday.** The
client-side code that calls `/api/presets` has not been written yet — it is planned, not built.
The version of the app on users' phones today has no knowledge of these endpoints, so no amount
of backend work makes it fetch them.

What Monday *can* deliver, and what makes it worth doing now:

1. **Backend live with `enabled: false`.** Endpoints real, content loaded, dashboard usable.
2. **The app team builds against a real API** instead of a stub, which is the main reason to do
   the backend first.
3. **When the app release ships**, you flip `enabled` to `true` and it starts working — no
   coordinated deploy, no second app update.

And from that point on the property actually wanted is delivered: **every new preset, every edit,
every retirement goes live without an app update.** The one-time cost is the app release that
carries the client. Everything after it is instant.

So the Monday deliverable is "backend ready and dark", not "feature live". If anyone is expecting
presets to start updating on phones Monday, correct that expectation now rather than after.

---

## 12. Dashboard admin requirements

- **Import a preset from pasted JSON.** Presets are authored in the app and exported as JSON;
  the admin pastes it, picks the category, and it validates per §8 before saving as a draft.
- **Bulk import** a whole category array — the ten existing files will be seeded this way.
- **Publish / unpublish** per preset. Unpublish must not delete; the user's Recents shelf handles
  a preset disappearing gracefully, but you want it recoverable.
- **Reorder** within a category (`sort_order`) — display order is editorial, e.g. the strongest
  Ramadan lockups first.
- **Version bump is automatic** on publish, unpublish, edit and reorder. Never a manual button;
  a human will forget and clients will silently stall on stale content.
- **A global `enabled` toggle**, clearly labelled as affecting all users immediately.
- **Preview is optional and low priority** — the dashboard cannot faithfully render Urdu Nastaleeq
  with the app's styling engine, so a half-accurate preview is worse than none. Authoring happens
  in the app, where the preview is real.

---

## 13. What NOT to build

Saves time and prevents contract mismatch:

- **No thumbnail generation or image storage.** The app renders preset thumbnails on-device from
  the layers. There is no `thumbnail_url`.
- **No `is_premium` flag on presets.** The app computes whether a preset is premium from the fonts
  and styles it uses, against flags that already live on the `fonts` table. A preset-level flag
  would fight it.
- **No file uploads, no CDN, no `json_url`.** Presets are inline JSON in the array response.
- **No pagination, no search, no filtering endpoints.** The app holds the catalogue locally and
  does its own filtering.
- **No per-user or per-device content.** Same catalogue for everyone.
- **No `category` field inside preset entries** (§2.2).

---

## 14. Self-test checklist

```bash
# 1. Manifest returns all ten categories and enabled:false
curl -H "X-API-KEY: <key>" https://dashboard.urdufonts.com/api/presets

# 2. A category returns a BARE ARRAY — output must start with '[' not '{'
curl -H "X-API-KEY: <key>" https://dashboard.urdufonts.com/api/presets/ramadan

# 3. No entry carries a "category" field
curl -s -H "X-API-KEY: <key>" .../api/presets/ramadan | grep -c '"category"'   # must be 0

# 4. Unknown category is an empty array with HTTP 200, not a 404
curl -i -H "X-API-KEY: <key>" .../api/presets/muharram

# 5. Drafts are excluded — unpublish one, confirm it leaves the array

# 6. Editing any preset bumps that category's version in the manifest, and only that one

# 7. Missing/invalid API key is rejected the same way /fonts rejects it
```

Round-trip check worth doing once: take one of the ten bundled files from the app team, import it,
serve it back, and diff against the original. Ignoring key order and whitespace, it should be
identical. If it is not, the difference is a bug in your import or serialization — most likely
`override` being reformatted, or font filenames being normalized.

---

## 15. Questions to send back

1. Can you get a flat list of the ~650 valid `styleId` values and the `fonts.file_name` list for
   the validator, or should the validator start as a warning rather than a hard reject?
2. Confirm the API key and base URL are the same ones `/fonts` and `/templates` already use.
3. Any dashboard framework constraints that change the schema in §7?
