# Improved Banking

![Improved Banking](icon.png)

Find and organize bank items with a searchable legend, Wiki categories and activity setups, custom item groups and presets, flexible layouts and full-tab screenshots.

**Beta — rapidly updated.** Improved Banking is being rapidly updated with up-to-date Old School RuneScape Wiki information and item categorizations. Categories and recommendations are still being refined. All requests, corrections and information are considered and appreciated: please [open an issue](https://github.com/wukuna/improved-banking/issues).

The Wiki-based catalogue ships with the plugin; it does not fetch updates during gameplay or send your bank contents to the Wiki. New information arrives through plugin updates. General classifications are organizational groups, not a claim that every item is suitable for every activity or that a group forms a complete best-in-slot loadout.

[Features](#features) · [Getting started](#getting-started) · [Usage](#usage) · [Saved settings](#saved-settings) · [Compatibility](#compatibility) · [Related plugins and sources](#related-plugins-and-sources) · [Development](#development) · [Support](#support) · [License and attribution](#license-and-attribution)

## Features

- **Searchable Legend:** find categories, presets and saved groups as you type; filter by preset, drag to reorder, and edit groups in one place.
- **Item lookup and comparison:** inspect one or two items, send a classification to Legend, or temporarily highlight the selected items without creating a group.
- **Custom groups and presets:** choose item icons from a searchable palette, combine disconnected bank selections, and save reusable groups with their labels and appearance.
- **Wiki-based organization:** broad categories, skilling, utility and raid presets, plus separately reviewed activity setups. Charged, empty, broken and equivalent versions retain their documented distinctions.
- **Optional native bank search:** enable category, preset and custom-group matches from the checkbox in the bank search prompt, with live item highlighting.
- **Bank presentation:** item-linked and area markers, readable labels, optional hover highlights, flexible dimensions, tab alignment and local full-tab screenshots.
- **Saved layouts:** separate settings for each character and RuneLite profile, multi-selection and Undo/Redo.

**Availability:** The [initial Plugin Hub submission](https://github.com/runelite/plugin-hub/pull/19100) has merged. This README follows the current source, which can include updates still awaiting Hub review.

## Getting started

Open your bank and the **Improved Banking** square-frame sidebar icon.

- **Legend:** search saved groups, categories and presets with suggestions and approximate spelling. By default, groups follow the current bank tab; choose **All saved groups** to manage absent items too. Filter by preset, drag rows to reorder the legend, edit labels/appearance or item membership, and delete selected groups or clear the layout. Shift + arrows and Ctrl-click select several groups. Undo restores group edits and ordering.
- **Custom:** create item groups with a searchable item-icon palette, draw linked groups or ordinary area markers, combine selected groups, and save/apply custom presets.
- **Tools:** bank size and tab alignment, screenshots, default border style and copies of older marker layouts.

The camera button immediately left of the bank's **X** saves the complete current tab or search results. **Tools → Save bank screenshot** offers the same capture. Select the main bank tab to capture the whole bank. Images are saved locally under RuneLite's `screenshots/<character>/bank-markers` folder, with native item icons, quantities, marker artwork and a separate legend. Nothing is uploaded or copied to the clipboard. This is a content export, not a reproduction of every third-party widget.

## Usage

### Markers and labels

Choose **Custom → Draw item group** or **Draw area marker**, then drag inside the bank. Right-click or **Cancel drawing** cancels. Outside drawing mode, markers are click-through.

Item groups follow exact item identities between bank tabs and across width/column changes. Their regions use the item grid and leave unrelated cells unmarked. To combine disconnected selections, draw and name the first group, select it in Legend, then choose **Custom → Add selection to group** and draw around another set of items. **Merge selected groups** combines existing linked groups under one name and retains the first selected group's appearance. Matching colours alone do not merge groups.

**Snap areas to item grid** applies independently to ordinary area markers. Area markers scroll with bank contents but do not follow item identities or reflow. Move and resize area markers from Legend.

Select one marker in Legend and choose **Label / appearance** to edit its label, color, font, font size, border, background and opacity. Item icons draw above marker backgrounds. Borders support **Plain**, **Vanilla OSRS** and **Active theme**. Active theme uses loaded equipment-frame sprite overrides from Resource Packs or Interface Styles, falling back to vanilla; it does not download or redistribute packs.

Mini-labels show short names once per group, only where they fit without covering item cells. Full names remain in the legend. Plugin settings offer **Bank labels: MINI / FULL / OFF**, **Subtle marker backgrounds** and **Highlight on hover**. Turning off hover highlighting retains click-to-pin and hover names. Saved labels and colors are not rewritten by these display options.

### Search, item lookup and custom presets

**Legend → Categories / presets** searches the bundled Wiki categories, equipment groups, raid presets, Wiki activity setups and saved custom presets. Results update as you type and tolerate approximate spelling; CoX, ToB and ToA abbreviations work. Choosing a category or preset opens a review before adding any groups. Search finds available categories even when they are not yet in your legend.

**Item lookup / compare** opens a searchable item-icon palette. Choose one item to list its recorded categories, presets, Wiki setups and custom memberships, or two to see shared and unique memberships. Same-name item variants retain separate IDs. This compares membership, not item power or activity readiness. Use **Only items in this bank** to limit results to owned items; uncheck it to search the item catalogue.

Select linked groups in Legend, then use **Custom → Save selected as preset**. Custom presets store their item IDs, labels and appearance separately for each character and RuneLite profile, and can be applied across that combination's named layouts. Applying a preset copies groups, retains unowned item links for later and skips already-grouped items by default. Select a custom preset in **Legend → Categories / presets** to delete it; existing groups stay intact. Up to 50 custom presets and 4,000 item types per custom group are supported.

### Item lookup actions

After choosing one or two items and pressing **OK**, select a classification and choose **Send filter to Legend** (or double-click the row). This opens the matching category or setup for review, then adds colored, item-linked groups to Legend. Category searches include secondary uses and preserve empty/broken state notes. Existing identical groups are selected without duplicating them or resetting their appearance. Saved-group rows focus their existing group; custom-preset rows select the matching group to apply. **Highlight this item(s)** temporarily outlines both selected item identities in the current bank view, including items without a saved group. Use **Legend → Unpin** to clear the lookup highlight. Changing account, profile or named layout also clears it.

### Native bank search

Click the bank's normal search button and check **Enable Improved Banking search** in the chatbox search prompt. This starts off and is also available in plugin settings. Plain-text searches can match item names, Wiki categories, presets and custom groups, with matching items outlined as you type. Longer words tolerate small spelling mistakes. Search does not make a gear recommendation: item-state and activity conditions still apply.

Normal native matches are retained, active Bank Tags tab membership is respected, and special searches such as `tag:food` or value comparisons keep their existing behavior. Clearing or closing search clears its highlights. Turning the option off restores ordinary search. The plugin only adds its own checkbox to the active bank-search prompt; other chatbox inputs are unchanged.

### Auto-categorize

Choose a broad classification, skilling, outfit, combat, supply, utility or raid preset. Review the proposed groups before applying them. Bulk presets skip already-linked items by default. **Include items already in other groups** keeps multiple uses of the same item; it starts checked when adding an individual category. Alternatively, check an old marker for replacement. Area markers remain. A batch is one Undo step.

The bundled catalogue includes skilling outfits and tools, production ingredients, equipment sets, general combat, supplies, travel, storage and quest/clue items. General categories use Wiki item classifications; activity choices use the relevant guide's equipment and inventory lists. Partial set ownership does not imply an active set bonus. A set weapon without matching armour remains a standalone weapon category. Unknown items remain in an unchecked review group.

The CoX, ToB and ToA presets retain their separate gear, supply and specialist roles. **Choose a Wiki activity and setup** opens a searchable selection of boss, skilling and activity guides. Select one equipment table or example inventory, read its conditions and review the matching items before applying markers. These choices begin unchecked and do not combine alternative setups into one loadout. The Fractured Archive guide is marked provisional while the Wiki develops its recommendations.

Equivalent items are recognized only for the effect they share. Fire and Infernal max capes can fill their corresponding combat-cape roles; they do not acquire every ordinary Max cape perk. Skilling outfit recolours and upgrades retain their documented XP or movement benefits, without copying extra effects, clue requirements or quest identities. Guide references preserve specific variants, including lantern fuel, cape trim and poison. Generic Barrows armour alternatives stay in their equipment slots; ornamented Sanguinesti staffs and Scythes retain their combat roles without transferring god protection. Charged, empty, damaged and broken versions keep separate state notes. An item in your bank does not prove that it has sufficient charges, that you meet every requirement or that a complete set is equipped.

**Exclude worse items** conservatively compares supported alternatives using owned replacements, real skill levels and verified unlocks. It preserves separate roles, special attacks, set effects and useful alternatives. It does not claim a universal gear ranking. [Coverage details](src/main/resources/com/windowmarkers/CATALOGUE-COVERAGE.md), [Wiki attribution](src/main/resources/com/windowmarkers/TAXONOMY-SOURCES.md) and [raid comparison sources](src/main/resources/com/windowmarkers/RAID-FILTER-SOURCES.md) describe the implemented scope.

### Bank layout

**Tools → Bank size and tabs** controls bank width, height, columns, tab alignment, spacing below the tabs and deposit-button placement. Item icons retain their native size. Bank Tags custom layouts retain their designed grid. Linked markers follow reflow; area markers keep their saved coordinates.

## Saved settings

Markers are separate for each RuneScape character **and** RuneLite settings profile. Renaming a profile preserves its markers. Each combination supports up to 20 named layouts and 250 markers per layout. Switching characters or profiles cancels drawing and loads the matching layout.

**New**, **Copy** and **Rename** manage layouts. **Undo / Redo** keeps up to 50 marker changes per layout during the current session; restarting clears history, not saved markers. Layout-management actions themselves are not undoable.

Existing Window Markers beta settings and saved layouts are preserved under their original storage keys. Tools can copy earlier account-only or shared marker layouts into an empty current layout without changing the originals.

## Compatibility

Improved Banking replaces the separate **Flexible Bank** and **Bank Screenshot** plugins and declares those conflicts. Existing Flexible Bank size preferences are copied once when no corresponding Improved Banking preference exists. Original settings remain stored. Disable Improved Banking before re-enabling the separate plugins. The integrated bank-layout option can also be disabled in settings.

The screenshot button can be disabled independently in **Screenshots** settings. Bank controls, menus and character actions remain under your control. This plugin does not automatically withdraw, equip, sort or move bank items.

## Related plugins and sources

| Project | Relationship | Relevant features and limits |
| --- | --- | --- |
| [Flexible Bank](https://github.com/TheLeon0/flexible-bank) | Adapted source and overlapping alternative | Bank dimensions and layout. Improved Banking combines this with markers, a legend and screenshots; the plugins declare a conflict. |
| Bank Screenshot | Overlapping alternative | Standalone bank captures. Improved Banking provides its own current-tab export with marker artwork and a legend; the plugins declare a conflict. |
| Bank Tags | Companion | Tab organization and custom layouts. Improved Banking retains custom layouts' designed grid rather than replacing their organization. |
| Resource Packs / Interface Styles | Companion | Active-theme marker borders can use already-loaded sprite overrides, with vanilla fallback. Pack artwork is not bundled. |
| [Old School RuneScape Wiki](https://oldschool.runescape.wiki/) | Category and recommendation data source | The bundled catalogue uses attributed Wiki information. It does not fetch updates during gameplay or upload bank contents. |

Feature overlap does not imply every option of another plugin is reproduced. Java code attribution and Wiki-data licensing are documented below.

## Development

Use Java 11 and run `./gradlew test jar` (Windows: `gradlew.bat test jar`). Published source uses the normal plugin name and RuneLite activation defaults. Local testing builds display **Improved Banking TEST** and retain the same feature-storage keys.

## Support

[Report an issue, correction or feature request](https://github.com/wukuna/improved-banking/issues). For category problems, include the item names, selected preset and whether Exclude worse items was enabled. For layout problems, include the current bank tab, RuneLite profile context, overlapping bank plugins and a screenshot with private details removed. All requests and information are considered and appreciated.

## License and attribution

Original Java code is BSD 2-Clause; see [LICENSE](LICENSE). The integrated bank-layout controller adapts [TheLeon0/Flexible Bank](https://github.com/TheLeon0/flexible-bank/tree/0ea1d3bf2c7bc9534323d4d6939599681c4d40fa), with its BSD licence and attribution included in source and resources.

The bundled Wiki-derived data files are distributed under **CC BY-NC-SA 3.0**, separately from the Java code. Their source revisions, attribution and modifications are documented in [TAXONOMY-SOURCES.md](src/main/resources/com/windowmarkers/TAXONOMY-SOURCES.md). No resource-pack artwork is redistributed.
