# Brief: in-panel preview for fonts, styles, stickers (and every other asset tile)

Prototype to match: https://claude.ai/code/artifact/183edf24-66ea-444d-997c-12a1115620b2
(two artboards — Fonts in adjustments mode, Stickers in the main panel — click into one to try it;
drag handle flips collapsed/expanded).

## What we're adding

A **preview / detail view** for any asset tile in the editor's bottom panel. It shows the asset big,
its name and details, lets you type your own words (fonts), and offers **Use on canvas / Add to
canvas**, **Share**, **Download**. It must work in **both panel states** and in **both the main panels
and the adjustments panels**, with **one rule everywhere**:

| State      | Gesture to open preview                                   | Untouched                                    |
|------------|-----------------------------------------------------------|----------------------------------------------|
| Collapsed  | **Long-press** the tile                                   | tap = today's behaviour; horizontal scroll   |
| Expanded   | **Tap the small eye button** drawn on the tile            | tap = today's behaviour; long-press = today's multi-select where it exists |

Why this split: tap is taken everywhere, long-press is taken in expanded (multi-select on
images/objects/shapes), vertical drag belongs to the sheet, horizontal drag to the collapsed strip.
Long-press is free in every collapsed panel today (`ImagesListFragment.onLongPress` only acts when
`mainViewModel.isPanelExpanded(PanelType.IMAGES)`; `EmojiAdapter` passes `onLongPress = null` for
collapsed; fonts/styles have no long-press). Do **not** add a double-tap and do **not** add any
delay to the existing tap.

**The preview is not a dialog and not a second bottom sheet.** The app has neither. It is the panel
pushing its own content, exactly like the existing `[← Language]` breadcrumb pattern in
`TextFragment` (`view_panel_tab_breadcrumb.xml`, `bg_breadcrumb_chip.xml`).

## Where it applies

- `ui/editor/panels/text/fonts/FontsAdapter.kt` (+ `TextFragment.kt`, which owns `fontsAdapter`,
  the collapsed/expanded header morph and the `MorphGridLayoutManager`) — fonts, both modes.
- `ui/editor/panels/text/styles/TextStylesGridAdapter.kt` / `TextStylesMainAdapter.kt` — presets/styles.
- `ui/editor/panels/images/ImagesAdapter.kt` (+ `ImagesListFragment.kt`, `ImagesFragment.kt`) — stickers/images.
- `ui/editor/panels/objects/EmojiAdapter.kt` (+ `ObjectsListFragment.kt`) and
  `ui/editor/panels/shape/ShapeAdapter.kt` (+ `ShapesListFragment.kt`) — same rule, same view.
  Where an adapter has no preview-worthy detail, the view still opens with name + big preview + actions.

Item layouts involved: `layout_font_item.xml` / `layout_font_item_expanded.xml`,
`layout_images_item.xml` / `layout_images_item_expanded.xml`, `item_emoji*.xml`, plus the
panel layouts `fragment_text.xml`, `fragment_images.xml`, `fragment_objects.xml`, etc.

## Gesture wiring

- Long-press: use the existing `addPressEffectWithLongClick(onLongClick, onClick)` in
  `common/utils/Utils.kt` (it already uses `ViewConfiguration.getLongPressTimeout()` and suppresses
  the click after a long-press). Route `onLongClick` to **open preview when the panel is collapsed**;
  when expanded, keep whatever the adapter does today (multi-select toggle, or nothing).
- Eye button: a 22dp (font band) / 26dp (image tile) round button, white (`#FFFFFF`, images at 92%
  alpha with a 1dp-ish soft shadow), stroke eye icon `#2B2B2B` at 75% opacity. Put it in the
  expanded item layouts only (`*_expanded.xml`):
  - font: at the end of `bottomContainer` in `layout_font_item_expanded.xml`, after `download`.
  - image/emoji/shape: bottom-end of the tile, margin 8dp.
  Its click must **not** propagate to the tile (`isClickable = true` on the button; consume the click).
- Tap on the tile itself: unchanged code path (`onFontSelected`, `onImageSelected`, …).

## The preview view

Push it inside the panel the same way the breadcrumb tab works; it replaces the grid **and** the
grid's header rows while open. Back is the breadcrumb chip; the panel's own drag/close still works.

**Header row (42dp):** `[← Fonts]` breadcrumb chip (`bg_breadcrumb_chip`, 26dp, `ic_back` 6×10dp
tinted `appColor`, label 12sp `@font/bold` `appColor`; label = the tab you came from — `Fonts`,
`Presets`, `Islamic`, …) · name 14sp `@font/bold` `black`, single line, ellipsize · `PRO` pill
(gradient `ic_button_gradient_wrap`, 8.5sp bold white, letter-spacing 0.06) when `is_premium`.
Hairline `tab_divider` under it.

**Body (padding 10dp 12dp 12dp):**
1. **Well + paper:** a `surface_1` (`#F7F7F7`) rounded well (12dp radius, 10dp padding) holding a
   white "paper" card (8dp radius, elevation ≈ `0 1dp 4dp rgba(0,0,0,.08)`) so the asset reads the
   way it sits on the canvas. The paper fills the remaining height.
   - Font: the sample in that typeface, RTL, centred. Text = the user's typed words, else
     `اردو کینوس`. 32sp collapsed / 48sp expanded. Expanded also shows, under a hairline, the
     alphabet row `ا ب پ ت ٹ ث ج چ ح خ د ڈ ذ ر ڑ ز ژ س ش ص ض ط ظ ع غ ف ق ک گ ل م ن و ہ ھ ی ے`
     and the numerals `۰ ۱ ۲ ۳ ۴ ۵ ۶ ۷ ۸ ۹` in the same face, 15sp, `gray`.
     Render with the real typeface once downloaded; before that use the existing `font_image` /
     `image_url` thumbnail (via Glide/SvgLoader as `FontsAdapter.loadImage` does) — typed text only
     works when the file is local, so disable the field with a hint until then.
   - Image/sticker/emoji/shape: the asset centred, ~130dp collapsed / ~240dp expanded, `fitCenter`.
   - Style/preset: the existing `TextStyleThumbnailRenderer` output, big.
2. **Chips row (24dp, gap 6dp, wrap):** `surface_1` pills, 10.5sp `@font/medium`, `black`:
   fonts → `font_language` · `font_category` · `file_size`; images → category · `PNG`/`SVG` ·
   `W × H` · size. Plus a state chip: `Downloaded` (`state_green_tint` bg, `sub_save_text` text, tiny
   check) or `Not downloaded` (`surface_1`, `gray`).
3. **Type-your-own-words field (fonts only, 40dp):** `bg_search_pill` styling (`surface_1`,
   1dp `surface_stroke`, fully round), 13sp, RTL, hint "Type your own words to try it". Live-updates
   the paper as you type. Persist the typed text for the session so switching fonts keeps it.
4. **Actions row (44dp, gap 10dp):** primary gradient pill (`ic_button_gradient_wrap`, 14sp bold
   white) **Use on canvas** (adjustments: apply to the selected element) / **Add to canvas** (main
   panels: same as today's tap) — then closes the preview; **Share** round `surface_1` 44dp
   (`ic_share`) → `ACTION_SEND` of the local asset file with the right MIME (font/ttf, image/png…),
   like `FilesListFragment.shareItem`; if not downloaded, download first, then share;
   **Download** round 44dp (`ic_download`) → existing `mainViewModel.downloadFont(font)` /
   image download path, with the same progress the tile shows; becomes a `state_green_tint` check
   when `is_downloaded`.

**Sheet height while previewing.** Collapsed panels are ~214dp tall; the preview needs ~340dp
(fonts) / ~330dp (images). Add a third snap height to `PanelSheetBehavior` (or grow the panel the way
`TextFragment` already animates `headerSpace` 42→118dp) so the sheet rises to that height when a
preview opens from the collapsed state and returns on back — the canvas must stay visible above it.
Expanded: the preview simply fills the expanded panel. Dismiss also on the panel's close button and
on back-press. Slide the preview in from the end (≈220ms) and back out.

**Multi-select:** when a grid is in multi-select mode, the eye button still opens the preview for that
tile; long-press keeps toggling selection. Leaving the preview restores the selection state and toolbar.

## Don'ts

- No dialog, no `BottomSheetDialogFragment`, no popup window, no overlay over the sheet.
- No change to tap semantics, no tap delay, no double-tap.
- Don't touch the `MorphGridLayoutManager` morph or the collapsed↔expanded header sync beyond
  hiding/showing them while the preview is open.
- Keep `Nunito` (`@font/regular|medium|bold`), `appColor #005D28`, `surface_1 #F7F7F7`,
  `surface_stroke #ECECEC`, `gray #8E94A2` — nothing new in `colors.xml` unless it's a preview-only
  surface you can't express with the existing tokens.

## Done means

- Every asset grid in the editor opens the preview by long-press (collapsed) and by the eye button
  (expanded), in main and adjustments panels; tap and long-press behave exactly as before otherwise.
- Fonts render the real typeface in the preview once local and re-render as you type.
- Share, Download and Use/Add all work from the preview, including for not-yet-downloaded assets.
- `./gradlew :app:assembleDevDebug` passes (set `JAVA_HOME` to the JBR under Android Studio first —
  see the repo's build notes). Don't claim it works on device unless you ran it on the Pixel 9 Pro
  emulator and looked.
- Commit as separate logical slices: gesture wiring, preview view, per-panel adoption.
