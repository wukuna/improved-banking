# Improved Banking

![Improved Banking](icon.png)

Find and organize bank items with a searchable legend and bank-window picker, Wiki categories and activity setups, saveable equipment-and-inventory views, custom groups, flexible layouts and full-tab screenshots. Search food by healing and eating behaviour, potions by effect, and ammunition by compatible weapon. Optional Grand Exchange search finds purchasable items using the same categories, with matching guide colours and 1st/2nd/3rd tags across bank and GE searches.

**Beta — rapidly updated.** Improved Banking is being rapidly updated with up-to-date Old School RuneScape Wiki information and item categorizations. Categories and recommendations are still being refined. All requests, corrections and information are considered and appreciated: please [open an issue](https://github.com/wukuna/improved-banking/issues).

The Wiki-based catalogue ships with the plugin; it does not fetch updates during gameplay or send your bank contents to the Wiki. New information arrives through plugin updates. General classifications are organizational groups, not a claim that every item is suitable for every activity or that a group forms a complete best-in-slot loadout.

[Features](#features) · [Getting started](#getting-started) · [Usage](#usage) · [Saved settings](#saved-settings) · [Compatibility](#compatibility) · [Related plugins and sources](#related-plugins-and-sources) · [Development](#development) · [Support](#support) · [License and attribution](#license-and-attribution)

## Features

- **Searchable Legend:** find categories, presets and saved groups as you type; filter by preset, drag to reorder, and edit groups in one place.
- **Item lookup and comparison:** inspect one or two items, send a classification to Legend, or temporarily highlight the selected items without creating a group.
- **Custom groups and presets:** choose item icons from a searchable palette, combine disconnected bank selections, and save reusable groups with their labels and appearance.
- **Wiki-based organization:** broad categories, skilling, utility and raid presets, plus separately reviewed activity setups. Charged, empty, broken and equivalent versions retain their documented distinctions.
- **Optional native bank search:** enable category, preset and custom-group matches from the checkbox in the bank search prompt, with live item highlighting.
- **Bank category picker:** search the same catalogue from the dropdown beside the bank camera button, open a reviewed view, save it as a custom preset or return to your bank.
- **Equipment and inventory views:** an eight-column equipment diagram and one Wiki inventory example, with owned gear alternatives and extra supplies below. General item/category searches retain their normal order.
- **Bank Tags integration:** add a real tag to selected items through an icon palette for use with Bank Tags layouts. Existing tags are retained.
- **Bank presentation:** item-linked and area markers, readable labels, optional hover highlights, flexible dimensions, tab alignment and local full-tab screenshots.
- **Saved layouts:** separate settings for each character and RuneLite profile, multi-selection and Undo/Redo.
- **Optional GE discovery:** search activities and categories such as `toa head`, `cox cape`, `vorkath ranged` or `mining tools`. Shared purple/1st, blue/2nd and green/3rd labels identify the first three Wiki equipment columns in both bank and GE searches where matching setups agree.
- **GE shopping filters:** find owned activity gear and supported next guide upgrades, narrow skill supplies with search tips, and optionally show guide prices, affordability colours and known unmet requirements.

**Availability:** The [initial Plugin Hub submission](https://github.com/runelite/plugin-hub/pull/19100) has merged. This README follows the current source, which can include updates still awaiting Hub review.

## Getting started

Open your bank and the **Improved Banking** square-frame sidebar icon.

- **Views:** search and open bank views, save and reopen them, add their colours to Legend, return to all bank items, or tag items for Bank Tags. Expand **Guide choices** to choose equipment and inventory examples.
- **Legend:** search saved groups, categories and presets with suggestions and approximate spelling. By default, groups follow the current bank tab; choose **All saved groups** to manage absent items too. Filter by preset, drag rows to reorder the legend, edit labels/appearance or item membership, and delete selected groups or clear the layout. Shift + arrows and Ctrl-click select several groups. Undo restores group edits and ordering.
- **Custom:** create item groups with a searchable item-icon palette, draw linked groups or ordinary area markers, combine selected groups, and save selected groups as custom presets.
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

Select linked groups in Legend, then use **Custom → Save selected groups**. Custom presets store their item IDs, labels and appearance separately for each character and RuneLite profile, and can be applied across that combination's named layouts. Applying a preset copies groups, retains unowned item links for later and skips already-grouped items by default. Select a custom preset in **Legend → Categories / presets** to delete it; existing groups stay intact. Up to 50 custom presets and 4,000 item types per custom group are supported.

### Item lookup actions

After choosing one or two items and pressing **OK**, select a classification and choose **Send filter to Legend** (or double-click the row). This opens the matching category or setup for review, then adds colored, item-linked groups to Legend. Category searches include secondary uses and preserve empty/broken state notes. Existing identical groups are selected without duplicating them or resetting their appearance. Saved-group rows focus their existing group; custom-preset rows select the matching group to apply. **Highlight this item(s)** temporarily outlines both selected item identities in the current bank view, including items without a saved group. Use **Legend → Unpin** to clear the lookup highlight. Changing account, profile or named layout also clears it.

### Native bank search

Click the bank's normal search button and check **Enable Improved Banking search** in the chatbox search prompt. This starts off and is also available in plugin settings. Plain-text searches can match item names, Wiki categories, presets and custom groups, with matching items outlined as you type. Longer words tolerate small spelling mistakes. Activity searches such as `toa magic head` use the same guide columns and conditions as GE search; general categories and ordinary item-name matches do not receive invented rankings.

Normal native matches are retained for general searches. Explicit supply filters below use their verified membership so a similarly named item cannot bypass a healing threshold or ammunition restriction. Active Bank Tags tab membership is respected, and special searches such as `tag:food` or value comparisons keep their existing behavior. Clearing or closing search clears its highlights. Turning the option off restores ordinary search. If the category index cannot refresh, ordinary search remains available and the previous category results are cleared. The plugin only adds its own checkbox to the active bank-search prompt; other chatbox inputs are unchanged.

### Food, potion and ammunition searches

These searches work in the bank and in the **in-game GE Buy-item search** when their respective Improved Banking search options are enabled. GE results include only tradeable items; bank results include only owned items. Their categories are also available in Legend and item lookup.

| Search | Results |
| --- | --- |
| `food`, `cooked food` | Ready-to-eat food, including edible fruit; excludes raw fish/meat, unfinished cooking inputs and drinks. |
| `raw food` | Raw fish/meat and uncooked cooking inputs. |
| `food 20` | Food healing at least 20 HP in one bite. Uses base Hitpoints for level-dependent healing; never adds together separate pie/cake bites. |
| `instant food 20`, `delayed food 20` | Separate immediate-only foods from hunter foods whose per-bite total includes a delayed heal. |
| `fast foods`, `combo food` | Fast food has a non-standard attack or eating delay; combo food can follow normal food in the same tick. These are separate groups. |
| `multi bite food`, `overhealing food` | Multi-bite foods and food that can heal above base Hitpoints. |
| `blighted food`, `unrestricted food` | Blighted food with area restrictions, or food excluding blighted variants. Other item-specific restrictions still apply. |
| `prayer potions`, `stat restore potions`, `stamina potions` | Potions by documented effect, retaining every usable dose. Prayer restoration and regeneration also have separate categories. |
| `poison cure potions`, `venom immunity potions`, `antifire potions` | Cure/protection effects; durations and side effects differ. `venom cure potions` identifies a complete cure in one dose. |
| `attack potions`, `healing potions`, `special attack potions` | Combat/skill boosts, healing or special-attack restoration. |
| `blowpipe ammo`, `rcb ammo`, `tbow ammo`, `sunlight crossbow ammo` | Compatible complete ammunition, excluding unfinished parts and weapon-incompatible variants. |

`food 20` includes delayed healing; use `instant food 20` when you need the full heal immediately. Hover GE results to read healing and restriction notes. Random foods such as kebabs do not qualify for numeric healing thresholds; variable-range foods use their minimum heal. Potions remain separate from food. A food group is not a recommendation for every boss or raid.

Ammo groups cover conventional bows/crossbows, blowpipes, ballistae, hunter crossbows, Karil's crossbow, ogre bows, the eclipse atlatl, cannon and salamanders. Unpoisoned darts are required for blowpipes; Dorgeshuun crossbows exclude gem-tipped bolts; hunter antler bolts stay separate from kebbit bolts. Charge-based bows do not receive a guessed arrow group. Weapon, Slayer and area requirements still apply. [Supply sources and scope](src/main/resources/com/windowmarkers/SUPPLY-SEARCH-SOURCES.md).

The core Grand Exchange plugin's fuzzy search remains available for queries Improved Banking does not recognize. Recognized supply and activity queries take priority, including an intentionally empty result. The catalogue loads in the background after login/enabling; if you searched while it was loading, edit or re-enter the query once ready. This does not extend the Grand Exchange side panel's price-lookup search.

The **v** dropdown beside the bank camera button searches categories and presets. **Views** in the sidebar provides the same entry point, plus a plain-text search for queries such as `toa top` or `toa magic head 1st`. Opening a view filters the bank immediately. **Add view to Legend** is the separate action that saves its coloured groups. **Show all bank items** clears the current view/search. Opening a saved view does not add duplicate Legend groups.

**Equipment and inventory search layout** places equipment on the left and one recommended Wiki inventory example on the right, inside the first eight columns at normal item spacing, even in a widened bank. The equipment diagram takes the highest owned ready alternative from one guide table for each slot, keeping its actual tier label. Conditional choices stay among the alternatives below. Incomplete Void selections and a shield paired with a two-handed weapon are kept out of the equipment diagram. Guide requirements, remaining charges and set effects still need your review.

The inventory preserves every populated item position in one Wiki example, including repeated supplies and equipment switches. Raid equipment examples keep their matching role or progression inventory; an explicit choice under **Guide choices** takes priority. Owning more items from another example does not replace the matching inventory. Repeated displays point to the same bank stack and retain its normal withdrawal choices; they do not change quantities or withdraw anything automatically.

Missing or unready inventory items appear as dimmed guide previews with no withdrawal action. Unbankable supplies, such as Moonlight potions and Tithe Farm seeds, also appear as guide previews with acquisition notes. They are not added to bank or GE search results. Inventory-only activities do not need an equipment table. Hover to see why a preview or alternative is shown. Intentionally empty source positions and placeholder-only slots stay empty. When the Wiki lists price-dependent food or potion alternatives, an owned eligible choice is shown; live prices are not compared. See [inventory source notes](src/main/resources/com/windowmarkers/INVENTORY-LAYOUT-SOURCES.md).

Other gear follows below by slot or documented raid room, followed by food, potions, runes and utilities. Room alternatives cover Chambers of Xeric, Theatre of Blood and Tombs of Amascut; a shared item can appear in multiple rooms. Guardian pickaxes have their own group, including verified Dragon pickaxe cosmetics. Hover explanations retain the guide's conditions. The view shows the chosen guides. Expand **Views → Guide choices** to choose another equipment table or inventory example. Ordinary item searches and categories keep their order, and an existing Bank Tags layout takes precedence.

Use **Views → Save current view…**, then select it under **Saved views and presets** and click **Open saved view** to return. Saved views preserve exact item IDs, colours, guide and inventory context; they do not automatically expand to future Wiki recommendations. They remain separate for each character and RuneLite profile. Temporary views do not create saved Bank Tags tabs or replace their layouts.

**Views → Add tag to item…** opens the item palette and a tag-name field. Confirm to add that tag through Bank Tags, preserving each item's existing tags. The optional variations checkbox uses Bank Tags' own variation grouping; it starts off. These are real Bank Tags tags, shared according to Bank Tags' normal RuneLite profile storage rather than Improved Banking's character-specific marker storage. Use the tag in a Bank Tags tab and its layout tools. No items are withdrawn or moved in the underlying bank.

### Auto-categorize

Choose a broad classification, skilling, outfit, combat, supply, utility or raid preset. Review the proposed groups before applying them. Bulk presets skip already-linked items by default. **Include items already in other groups** keeps multiple uses of the same item; it starts checked when adding an individual category. Alternatively, check an old marker for replacement. Area markers remain. A batch is one Undo step.

The bundled catalogue includes skilling outfits and tools, production ingredients, equipment sets, general combat, supplies, travel, storage and quest/clue items. General categories use Wiki item classifications; activity choices use the relevant guide's equipment and inventory lists. Partial set ownership does not imply an active set bonus. A set weapon without matching armour remains a standalone weapon category. Unknown items remain in an unchecked review group.

The CoX, ToB and ToA presets retain their separate gear, supply and specialist roles. **Choose a Wiki activity and setup** opens a searchable selection of boss, skilling and activity guides. Select one equipment table or example inventory, read its conditions and review the matching items before applying markers. These choices begin unchecked and do not combine alternative setups into one loadout. 

Equivalent items are recognized only for the effect they share. Fire and Infernal max capes can fill their corresponding combat-cape roles; they do not acquire every ordinary Max cape perk. Skilling outfit recolours and upgrades retain their documented XP or movement benefits, without copying extra effects, clue requirements or quest identities. Guide references preserve specific variants, including lantern fuel, cape trim and poison. Generic Barrows armour alternatives stay in their equipment slots; ornamented Sanguinesti staffs and Scythes retain their combat roles without transferring god protection. Charged, empty, damaged and broken versions keep separate state notes. Verified Echo Barrows, blazing blowpipe, mutagen helm, coloured crystal armour and infernal-tool cosmetics retain their functional alternatives. Depleted tools can remain useful without inheriting a rank for a charge-dependent effect. Teleport roles require a charged version, including an equipped glory for Abyss trips; uncharged jewellery stays available for its separate combat or passive uses. Inventory examples follow the selected method and team size, even when guides reuse the same setup name. Hover notes identify reduced-capacity items such as damaged essence pouches. An item in your bank does not prove that it has sufficient charges, that you meet every requirement or that a complete set is equipped.

Guide alternatives also include ordinary linked items and named footnotes. For example, an ornate-handle granite maul requirement selects the upgraded weapon, while Vale Totems offering examples include the guide's magic-wood products. Links explaining a required spell, weapon or complete set remain conditions; they are not automatically added as interchangeable equipment. Follow the pinned guide when a setup refers to another phase or ranged section.

**Exclude worse items** conservatively compares supported alternatives using owned replacements, real skill levels and verified unlocks. It preserves separate roles, special attacks, set effects and useful alternatives. It does not claim a universal gear ranking. [Coverage details](src/main/resources/com/windowmarkers/CATALOGUE-COVERAGE.md), [Wiki attribution](src/main/resources/com/windowmarkers/TAXONOMY-SOURCES.md) and [raid comparison sources](src/main/resources/com/windowmarkers/RAID-FILTER-SOURCES.md) describe the implemented scope.

### Grand Exchange search

Check **Grand Exchange search → Enable Grand Exchange search** in the plugin settings. It defaults to off. Allow a moment for the local catalogue to load, then use the ordinary GE buy-item search. Examples include `toa head`, `tombs magic head`, `tob weapon`, `cox cape`, `vorkath ranged`, `zulrah weapon`, `mining tools`, `woodcutting tools` and `herblore ingredients`. Slot qualifiers include head/helm/hat/hood, body/chest/top, legs/bottom, hands/gloves, feet/boots, neck/amulet, cape, weapon, offhand/shield/defender, ammo and ring. Add `1`, `1st` or `bis` to show only first-tier items; `2`/`2nd` and `3`/`3rd` work the same way. For example, `toa magic head 2nd` shows second-tier matching headgear. Multiple tier tokens select their union. Unranked items are excluded from a tier-filtered search; ordinary numbered item names remain native searches. These keywords work in both bank and GE searches. The search covers the shared classifications and all audited activity guides, not only raids. Only GE-tradeable items appear; a broad search exceeding the native 250-result limit must be narrowed.

For shopping, `tools` means tools, not a guide's energy potions or other inventory supplies. `farming` selects directly classified Farming supplies and equipment. Narrow it with `farming seeds`, `farming trees` (tree seeds and saplings), `farming saplings`, `farming tools` or `farming compost`. Hover **Search tips [?]** in the chatbox for a short list of more specific queries, then type the one you want. These shopping refinements apply only to the GE; the bank retains its broader organizational categories.

**Activity items** starts at **All items**. Choose **Owned**, **Not owned** or **Next guide upgrade**, or append `owned`, `unowned` or `next upgrade` to an activity query, such as `toa magic top next upgrade`. Open your bank once after login so the plugin can include bank, inventory and equipped items. Before that, ownership filters leave results unfiltered and show a notice. This snapshot stays in memory, clears across logins and profiles, and is never uploaded. Placeholders do not count as owned. Verified usable equivalents can count as an **Owned variant**; an empty or broken counterpart does not prove usable ownership.

**Next guide upgrade** compares supported roles within the same Wiki equipment table and selects the nearest available better column above owned ready equipment. It is not a DPS, budget or value-for-money recommendation. Conditional, special-attack, set-dependent and unsupported roles may have no upgrade result; use **All items** to see those alternatives and items needing charging or repair. **Hide worse than owned** starts off and hides an item only when verified usable owned gear outranks it in every compared role. It retains specialist, conditional, set and unranked alternatives. **Owned** mode still shows all owned matches. The optional **Mark owned activity items** adds ownership labels without filtering.

**Show guide prices** starts off. Prices use RuneLite's cached price source and show the estimated cost of one item, with the full amount on hover. **Colour prices by funds** uses green when observed coins and platinum cover that amount, red when a known balance falls short, and neutral when the price or balance is unknown. It includes inventory and the last observed bank balance; a GE offer change invalidates that bank balance until the bank is observed again. Uncollected offers and the resale value of equipment do not count. Guide prices are not guaranteed offers, and account restrictions and buy limits still apply.

**Mark unmet requirements** also starts off. It adds a red **[Locked]** label and a hover explanation for reviewed unmet skill or unlock requirements. Coverage is partial: an item without a tag is not guaranteed usable, fully charged or part of a complete set. This does not disable purchases or alter item actions. Price, ownership and requirement labels are independent of the guide-tier colours.

**Gear guide tiers → Show Wiki gear tiers** adds shared configurable colours and `[1st]`, `[2nd]`, `[3rd]` to the first three columns of a matching guide's recommended-equipment table. The defaults are purple for 1st, blue for 2nd and green for 3rd. The bank uses matching borders and small corner tags on ranked items; the GE adds the tag beside each item name. First choice is BIS **for that setup and role**, subject to its requirements. Hover the item name for context and conditions. General categories, example inventories, conflicting rankings, provisional recommendations and items needing preparation stay neutral. A missing or untradeable first choice does not turn second choice into first. Purchasing a listed variant does not include its charges, upgrades or set partners.

Verified purchase alternatives can appear alongside a guide's exact items, such as an uncharged bow or enhanced-capacity trident. Hover explanations identify charging, repair, filling and capacity differences; these alternatives do not inherit a ready-to-use gear colour. Role searches also recognize two-handed shields and supported blowpipe ammunition without confusing those roles with ordinary weapon recommendations.

Use **Views → Guide choices → Choose bank / GE guide** to narrow an activity to one source table and read its conditions. **Clear bank / GE guide** restores all matching setups. This same context applies to live bank searches; saved setup groups retain the guide they were created from. Search again at the GE after changing the guide or settings. The context is saved per RuneLite profile; other activities remain searchable. Ordinary item-name searches and earlier-handled searches such as Bank Tags retain their existing behavior. This feature does not buy items or change offers, prices or quantities. [Tier data and scope](src/main/resources/com/windowmarkers/GE-SEARCH-SOURCES.md).

### Bank layout

**Tools → Bank size and tabs** controls bank width, height, columns, tab alignment, spacing below the tabs and deposit-button placement. Item icons retain their native size. Bank Tags custom layouts retain their designed grid. Linked markers follow reflow; area markers keep their saved coordinates.

## Saved settings

Markers are separate for each RuneScape character **and** RuneLite settings profile. Renaming a profile preserves its markers. Each combination supports up to 20 named layouts and 250 markers per layout. Switching characters or profiles cancels drawing and loads the matching layout.

**New**, **Copy** and **Rename** manage layouts. **Undo / Redo** keeps up to 50 marker changes per layout during the current session; restarting clears history, not saved markers. Layout-management actions themselves are not undoable.

Existing Window Markers beta settings and saved layouts are preserved under their original storage keys. Tools can copy earlier account-only or shared marker layouts into an empty current layout without changing the originals.

## Compatibility

Improved Banking replaces the separate **Flexible Bank** and **Bank Screenshot** plugins and declares those conflicts. Existing Flexible Bank size preferences are copied once when no corresponding Improved Banking preference exists. Original settings remain stored. Disable Improved Banking before re-enabling the separate plugins. The integrated bank-layout option can also be disabled in settings.

The screenshot button can be disabled independently in **Screenshots** settings. Bank controls, menus and character actions remain under your control. Slot grouping changes the displayed search layout only; it never rearranges the stored bank, withdraws or equips items.

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
