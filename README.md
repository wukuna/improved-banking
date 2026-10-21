# Improved Banking

![Improved Banking](icon.png)

Organize your bank with item-linked markers, a tab-aware group legend, flexible bank layouts and full-tab screenshots.

**Beta — rapidly updated.** Improved Banking is being rapidly updated with up-to-date Old School RuneScape Wiki information and item categorizations. Categories and recommendations are still being refined. All requests, corrections and information are considered and appreciated: please [open an issue](https://github.com/wukuna/improved-banking/issues).

The Wiki-based catalogue ships with the plugin; it does not fetch updates during gameplay or send your bank contents to the Wiki. New information arrives through plugin updates. General classifications are organizational groups, not a claim that every item is suitable for every activity or that a group forms a complete best-in-slot loadout.

## Getting started

Open your bank and the **Improved Banking** square-frame sidebar icon.

- **Legend:** groups with matching items in the current tab or search results. Hover a row or a marked bank item to highlight its group. Click a legend entry to pin its highlight; use **Clear highlight** to release it. Counts show distinct matching item types, including items below the scroll area, not stack quantities.
- **Markers:** draw, auto-categorize, select and edit markers; manage named layouts; Undo or Redo changes. Shift + Up/Down, Shift-click and Ctrl-click support multi-selection. Delete selected markers or clear the current layout; either is one Undo step.
- **Tools:** bank size and tab alignment, screenshots, default border style and copies of older marker layouts.

The camera button immediately left of the bank's **X** saves the complete current tab or search results. **Tools → Save bank screenshot** offers the same capture. Select the main bank tab to capture the whole bank. Images are saved locally under RuneLite's `screenshots/<character>/bank-markers` folder, with native item icons, quantities, marker artwork and a separate legend. Nothing is uploaded or copied to the clipboard. This is a content export, not a reproduction of every third-party widget.

## Markers and labels

Choose **Draw marker**, then drag inside the bank. Right-click or **Cancel drawing** cancels. Outside drawing mode, markers are click-through.

**Link new markers to enclosed items** makes markers follow those item identities between bank tabs and across width/column changes. Linked regions use the item grid and leave unrelated cells unmarked. **Snap areas to item grid** independently applies to ordinary area markers. Area markers scroll with bank contents but do not follow item identities or reflow. You can move or resize an area marker; unlink a linked marker before editing its stored area.

Select one marker to edit its label, color, font, font size, border, background and opacity. Item icons draw above marker backgrounds. Borders support **Plain**, **Vanilla OSRS** and **Active theme**. Active theme uses loaded equipment-frame sprite overrides from Resource Packs or Interface Styles, falling back to vanilla; it does not download or redistribute packs.

Mini-labels show short names once per group, only where they fit without covering item cells. Full names remain in the legend. Plugin settings offer **Bank labels: MINI / FULL / OFF**, **Subtle marker backgrounds** and **Highlight on hover**. Turning off hover highlighting retains click-to-pin and hover names. Saved labels and colors are not rewritten by these display options.

## Accounts, profiles and layouts

Markers are separate for each RuneScape character **and** RuneLite settings profile. Renaming a profile preserves its markers. Each combination supports up to 20 named layouts and 250 markers per layout. Switching characters or profiles cancels drawing and loads the matching layout.

**New**, **Copy** and **Rename** manage layouts. **Undo / Redo** keeps up to 50 marker changes per layout during the current session; restarting clears history, not saved markers. Layout-management actions themselves are not undoable.

Existing Window Markers beta settings and saved layouts are preserved under their original storage keys. Tools can copy earlier account-only or shared marker layouts into an empty current layout without changing the originals.

## Auto-categorize

Choose a broad classification, skilling, outfit, combat, supply, utility or raid preset. Review the proposed groups before applying them. Existing linked items are skipped unless their old markers are checked for replacement; area markers remain. A batch is one Undo step.

The bundled catalogue includes skilling outfits and tools, equipment sets, general combat, supplies, travel, storage, quest/clue items and raid recommendations for Chambers of Xeric, Theatre of Blood and Tombs of Amascut. Partial set ownership does not imply an active set bonus. A set weapon without matching armour remains a standalone weapon category. Unknown items remain in an unchecked review group.

**Exclude worse items** conservatively compares supported alternatives using owned replacements, real skill levels and verified unlocks. It preserves separate roles, special attacks, set effects and useful alternatives. It does not claim a universal gear ranking. [Coverage details](src/main/resources/com/windowmarkers/CATALOGUE-COVERAGE.md), [Wiki attribution](src/main/resources/com/windowmarkers/TAXONOMY-SOURCES.md) and [raid comparison sources](src/main/resources/com/windowmarkers/RAID-FILTER-SOURCES.md) describe the implemented scope.

## Bank layout and compatibility

**Tools → Bank size and tabs** controls bank width, height, columns, tab alignment, spacing below the tabs and deposit-button placement. Item icons retain their native size. Bank Tags custom layouts retain their designed grid. Linked markers follow reflow; area markers keep their saved coordinates.

Improved Banking replaces the separate **Flexible Bank** and **Bank Screenshot** plugins and declares those conflicts. Existing Flexible Bank size preferences are copied once when no corresponding Improved Banking preference exists. Original settings remain stored. Disable Improved Banking before re-enabling the separate plugins. The integrated bank-layout option can also be disabled in settings.

The screenshot button can be disabled independently in **Screenshots** settings. Bank controls, menus and character actions remain under your control. This plugin does not automatically withdraw, equip, sort or move bank items.

## Building

Use Java 11 and run `./gradlew test jar` (Windows: `gradlew.bat test jar`). Published source uses the normal name and starts disabled by default. Local testing builds display **Improved Banking TEST** and retain the same feature-storage keys.

## Licences and attribution

Original Java code is BSD 2-Clause; see [LICENSE](LICENSE). The integrated bank-layout controller adapts [TheLeon0/Flexible Bank](https://github.com/TheLeon0/flexible-bank/tree/0ea1d3bf2c7bc9534323d4d6939599681c4d40fa), with its BSD licence and attribution included in source and resources.

The bundled Wiki-derived data files are distributed under **CC BY-NC-SA 3.0**, separately from the Java code. Their source revisions, attribution and modifications are documented in [TAXONOMY-SOURCES.md](src/main/resources/com/windowmarkers/TAXONOMY-SOURCES.md). No resource-pack artwork is redistributed.
