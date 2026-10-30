# Improved Banking

![Improved Banking](icon.png)

Find what you need, build a useful bank view and make it your own. Search Wiki categories, activities and item uses; preview gear and inventory setups; then save editable views with named, coloured groups. Add item-linked markers, review junk with explanations, adjust the bank layout and capture full-tab screenshots. Optional Grand Exchange search uses the same categories and guide tiers, with owned-item, upgrade and price information where supported.

**Beta — rapidly updated.** Improved Banking is being rapidly updated with up-to-date Old School RuneScape Wiki information and item categorizations. Categories and recommendations are still being refined. All requests, corrections and information are considered and appreciated: please [open an issue](https://github.com/wukuna/improved-banking/issues).

The Wiki-based catalogue ships with the plugin; it does not fetch updates during gameplay or send your bank contents to the Wiki. New information arrives through plugin updates. General classifications are organizational groups, not a claim that every item is suitable for every activity or that a group forms a complete best-in-slot loadout.

[Features](#features) · [Getting started](#getting-started) · [Usage](#usage) · [Saved settings](#saved-settings) · [Compatibility](#compatibility) · [Inspiration and recipe](#inspiration-and-recipe) · [Development](#development) · [Support](#support) · [License and attribution](#license-and-attribution)

## Features

- **Recent updates:** up to five bundled change notes appear in the chatbox once per update in each RuneLite profile.
- **Searchable Legend:** find activities and saved groups as you type; filter by preset, drag to reorder, and edit groups in one place.
- **Item lookup and comparison:** inspect one or two items, send a classification to Legend, or temporarily highlight the selected items without creating a group.
- **Custom groups and presets:** choose item icons from a searchable palette, combine disconnected bank selections, and save reusable groups with their labels and appearance.
- **Editable saved views:** copy a built-in search into a custom view, then add/remove item icons, rename groups and reorder them in one resizable editor. Save commits the draft; Cancel leaves the saved view unchanged.
- **Cleanup review:** find reviewed burnt cooking results with specific quest, repeatable-task, container and rarity notes. A separate conservative sell-candidate search covers supported ordinary gathering tools.
- **Wiki-based organization:** broad categories, skilling, utility and raid presets, plus separately reviewed activity setups. Charged, empty, broken and equivalent versions retain their documented distinctions.
- **Optional native bank search:** enable category, preset and custom-group matches from the checkbox in the bank search prompt, with live item highlighting.
- **View presets button:** browse 102 activities and your saved views from a native-style button beside the bank camera button, open a reviewed view, save it as a custom preset or return to your bank.
- **Equipment and inventory views:** phase-specific equipment diagrams and Wiki inventories, with matching owned alternatives below. Temporary presets retain their selected item list; unrelated supplies are excluded. General item/category searches retain their normal order.
- **Bank Tags integration:** add a real tag to selected items through an icon palette for use with Bank Tags layouts. Existing tags are retained. Search results and highlights refresh when switching tabs and stay within the active tag's membership.
- **Bank presentation:** item-linked and area markers, readable labels, optional hover highlights, flexible dimensions, tab alignment and local full-tab screenshots.
- **Saved layouts:** separate settings for each character and RuneLite profile, multi-selection and Undo/Redo.
- **Optional GE discovery:** search activities and categories such as `toa head`, `cox cape`, `vorkath ranged` or `mining tools`. Shared purple, blue and green tier labels identify the first three Wiki equipment columns in both bank and GE searches where matching setups agree.
- **GE shopping filters:** find owned activity gear and supported next guide upgrades, narrow skill supplies with search tips, and optionally show guide prices, affordability colours and known unmet requirements.

**Availability:** The [initial Plugin Hub submission](https://github.com/runelite/plugin-hub/pull/19100) has merged. This README follows the current source, which can include updates still awaiting Hub review.

## Getting started

Open your bank and the **Improved Banking** square-frame sidebar icon.

- **Legend:** the main workflow. Type an item, category or activity and choose **Show in bank** (or a suggestion) to preview it. **Add to Legend** saves the coloured group; **Clear bank filter** returns to ordinary bank contents. Saved groups appear below. Select a group to edit its appearance or items, remove it, or drag it into a new order. Shift + arrows and Ctrl-click select several groups. **Clear highlight** ends a pinned or item-lookup highlight. Undo restores group edits and ordering.
- **Custom:** create item groups with the searchable item palette, draw linked groups, or draw ordinary area markers. Management of existing groups stays in Legend.
- **Tools:** bank size and tab alignment, screenshots, Bank Tags tagging, default border style, **Marker layouts** for switching a whole collection of groups, and copies of older layouts.

**Legend → Saved views** holds **Save as custom view**, **Open saved view**, **Edit saved view** and **Delete saved view**, plus guide/inventory choices. **Filter saved groups** chooses the current bank tab, all saved groups or a preset. **More group actions** holds merging, adding another drawn region, saving selected groups as a view, area movement/resizing, bulk auto-grouping and removal of all groups. These secondary controls stay collapsed until needed.

The camera button immediately left of the bank's **X** saves the complete current tab or search results. **Tools → Save bank screenshot** offers the same capture. Select the main bank tab to capture the whole bank. Images are saved locally under RuneLite's `screenshots/<character>/bank-markers` folder, with native item icons, quantities, marker artwork and a separate legend. Nothing is uploaded or copied to the clipboard. The capture waits for item icons to load and keeps the layout as it was when requested. This is a content export, not a reproduction of every third-party widget.

## Usage

### Markers and labels

Choose **Custom → Draw item group** or **Draw area marker**, then drag inside the bank. Right-click or **Cancel drawing** cancels. Outside drawing mode, markers are click-through.

Item groups follow exact item identities between bank tabs and across width/column changes. Their regions use the item grid and leave unrelated cells unmarked. To combine disconnected selections, draw and name the first group, select it in Legend, then choose **Legend → More group actions → Draw more items into group** and draw around another set of items. **Merge selected groups** combines existing linked groups under one name and retains the first selected group's appearance. Matching colours alone do not merge groups.

**Snap areas to item grid** fits the visible item cells, including tab sections with different row offsets. It applies independently to ordinary area markers. Area markers scroll with bank contents but do not follow item identities or reflow. Move and resize area markers from Legend.

Select one marker in Legend and choose **Edit label / appearance** to edit its label, color, font, font size, border, background and opacity. Item icons draw above marker backgrounds. Borders support **Plain**, **Vanilla OSRS** and **Active theme**. Active theme uses loaded equipment-frame sprite overrides from Resource Packs or Interface Styles, falling back to vanilla; it does not download or redistribute packs.

Mini-labels show short category names once per group. Custom groups retain the name you entered, shortened only to fit the available space. Labels first try the group edge, then nearby empty space; item icons stay uncovered. Full names remain in the legend when the bank is too crowded for a label. Under **Labels and highlighting**, settings offer **Bank labels: MINI / FULL / OFF**, **Subtle marker backgrounds**, **Highlight on hover** and **Show category and setup tooltips**. Turn off the tooltip setting to hide group names, category explanations, gear tiers and inventory notes on hover while keeping bank colours, labels and native item actions. Turning off hover highlighting retains click-to-pin. Saved labels and colors are not rewritten by these display options.

### Search, item lookup and custom presets

Drag items inside a preset to swap two displayed cells or move an item into an empty space. Each repeated inventory item moves independently. Positions are saved for the current preset, bank width, character and RuneLite profile; your real bank order and other Bank Tag Layouts tabs stay unchanged. **Saved views → Reset preset item positions** restores the generated arrangement. After adding items in the custom-view editor, press **Add selected items**, then **Save changes**; the new items can be dragged in the open preset too.

**Appearance** has independent switches for preset name tags, section headings, rank labels and item outlines/backgrounds. Floating name tags start hidden; section headings and 1st/2nd/3rd labels start visible. The bank title always identifies the selected preset. These switches also apply to activity searches and preserve saved group labels and colours.

**Legend → Browse activities** and **View presets** list named activities and saved custom views. General categories and broad supply presets are kept out of this menu; their item classifications and native search matching remain available. Barbarian Assault and Fossil Island Wyverns each group their related methods, with original guide conditions and setup IDs retained. The seasonal Echo Hespori entry stays out of ordinary browsing. Results update as you type and tolerate approximate spelling; CoX, ToB and ToA abbreviations work. Choosing a result previews the bank; it does not save a new group until **Add to Legend** is used. Activities appear even when they are not yet in your legend.

**Item lookup / compare** opens a searchable item-icon palette. Choose one item to list its recorded categories, presets, Wiki setups and custom memberships, or two to see shared and unique memberships. Same-name item variants retain separate IDs. This compares membership, not item power or activity readiness. Use **Only items in this bank** to limit results to owned items; uncheck it to search the item catalogue.

Select linked groups in Legend, then use **Legend → More group actions → Save groups as custom view**. Alternatively, open a built-in category or setup and choose **Saved views → Save as custom view**. In the editor, select a group to rename it, choose border/background colours, add item icons from the search palette or remove selected items. Shift + arrows and Ctrl-click select multiple items or groups. Move groups up/down, add new groups or remove them, then choose **Save changes**. Cancel discards the draft. The built-in preset and existing Legend groups are unchanged.

Use **Saved views → Edit saved view** to change that copy later, **Open saved view** to display it, or **Delete saved view** to remove the copy. Custom views store exact item IDs, labels, appearance and optional gear-layout context separately for each character and RuneLite profile. Unowned item links remain available for later; removing an item also removes its reference icon from the custom inventory layout. Up to 50 custom views and 4,000 item types per group are supported. Saved-view edits are committed through Save/Cancel; marker Undo/Redo applies to Legend groups, not the saved-view library.

### Junk and quest-use review

Search **Junk review** (or `junk`) in Legend or enabled native bank search. The initial reviewed set covers burnt cooking results; this is not a complete junk catalogue or an instruction to discard anything. Ordinary review items have a subtle grey outline. Items with niche or uncertain uses have a light amber outline and stay in the same junk results. Hover an item, or open **Item uses and reasons** to see names, icons, the specific purpose and source; this window also searches the reason text.

Burnt chicken retains its **repeatable Keldagrim tasks** note even after **Forgettable Tale...** is completed. Burnt meat's **Witch's Potion** note changes when that quest is completed. Recoverable containers, rare collector items and uncertain uses receive their own explanations. Collector value can remain even after a practical use ends.

`quest active`, `quest future`, `quest completed` and `quest unknown` separate the currently mapped quest uses. Live completion checks initially cover burnt chicken and burnt meat; other quest-related items remain **progress unknown**. Future-quest notes distinguish reviewed prerequisites and boostable skill requirements without claiming complete eligibility. Progress is read locally on bank opening; it is not uploaded. A completed quest use does not imply that every other use is finished.

`sellable` or **Sell candidates** reviews ordinary metal axes and pickaxes only when a better owned, usable replacement is observed. Known clue/quest uses and repeatable task tools remain protected, and members-only replacements do not displace free-world tools. This does not rank combat weapons against each other or mark a specialist weapon redundant because a higher-priced weapon is owned. Nothing is sold, dropped, destroyed or withdrawn automatically. [Cleanup scope and sources](src/main/resources/com/windowmarkers/BANK-CLEANUP-SOURCES.md).

### Item lookup actions

After choosing one or two items and pressing **OK**, select a classification and choose **Send filter to Legend** (or double-click the row). This opens the matching category or setup for review, then adds colored, item-linked groups to Legend. Category searches include secondary uses and preserve empty/broken state notes. Existing identical groups are selected without duplicating them or resetting their appearance. Saved-group rows focus their existing group; custom-preset rows select the matching group to apply. **Highlight this item(s)** temporarily outlines both selected item identities in the current bank view, including items without a saved group. Use **Legend → Clear highlight** to clear the lookup highlight. Changing account, profile or named layout also clears it.

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

The **View presets** button beside the bank camera button is a shortcut to the same category/preset previews used by Legend. It uses native button artwork, including resource-pack replacements, and reserves header space so long bank titles do not overlap it. The sidebar has one search field for queries such as `toa top` or `toa magic head 1st`. **Add to Legend** saves the preview's coloured groups. **Clear bank filter** clears the current view/search. Opening a saved view does not add duplicate Legend groups.

Flexible bank sizes keep the native settings form centred at its original size, with separate checkbox columns and rows. Opening settings temporarily gives a short bank enough height for the full form; returning to items restores the configured height. Native option actions are unchanged.

**Equipment and inventory search layout** keeps each boss or activity in one browse entry. Built-in presets use the same setup calculation, guide choice and layout as native activity search, and keep the selected view open after closing the picker or search input. Phase and method loadouts appear as separate equipment/inventory sections inside that preset. Equipment sits on the left and a recommended Wiki inventory on the right, inside the first eight columns at normal item spacing, even in a widened bank. Narrower banks stack the inventory below the equipment. Duplicate source tables for the same phase and style share one section; other owned choices remain below as alternatives. Repeated supplies and gear reused in later phases remain visible when Bank Tag Layouts is enabled.

Equipment follows the guide's ordered choices and conditions. Missing slots use suitable ready owned alternatives from compatible activity tables, then the shared combat-style lists. Avernic treads use the relevant style upgrades; base treads remain usable alternatives without inheriting upgraded tier labels. Blessings fill a free ammunition slot, including owned Rada's blessing 4, but never replace ammunition required by the selected weapon. Fallbacks have explanatory notes and do not acquire an invented boss-specific rank. Required protection and method-defining weapons are preserved. If no suitable item is available, a labelled slot explains what is missing; Wilderness defaults can instead show a dimmed budget suggestion. These suggestions have no highlight or withdrawal action and do not widen the budget filter. A two-handed weapon shows **2H** in the off-hand slot. Incomplete Void sets are not presented as complete outfits. Hover equipment for its guide note. Native bank-tab headings and divider lines are omitted inside these views; ordinary tabs retain them.

The browser now lists 105 activities rather than 855 separate source tables. The 37 weapon-specific ammunition categories are archived from browsing and item-lookup suggestions. Their compatibility data and explicit searches remain available, and existing saved groups are preserved. Use an activity preset or the broad ammunition category for normal browsing; **Guide choices** still offers individual source tables for an explicit setup.

Each inventory preserves every populated item position in its Wiki example, including repeated supplies and equipment switches. When several phase sections use the same inventory, it is shown once with **Shared inventory above** beside the later sections. Different inventories remain separate. Raid equipment examples keep their matching role or progression inventory; an explicit choice under **Guide choices** takes priority. Owning more items from another example does not replace the matching inventory. Repeated displays point to the same bank stack and retain its normal withdrawal choices; they do not change quantities or withdraw anything automatically. Owned blighted supplies fill the exact slots listing them in reviewed inventories, with a restriction note; they are not treated as missing or broken merely because their use is restricted.

Owned divine potions and other deliberately self-damaging supplies fill the reviewed inventory slots that list them; their use warning does not mean they are unavailable. If the exact dose is missing, supported potions can use an owned bottle of the same potion with a different dose count. Hover to check the doses: repeated slots share one bank stack and do not guarantee enough doses for the trip. Regular/divine and regular/blighted potions remain distinct. Preset activity views use the same yellow owned-item highlighting as native search; dimmed examples stay unhighlighted. Owned ash sanctifiers, bonecrushers, soul bearers and master scroll books fill their reviewed inventory slots with charge/content reminders. Their item identity alone cannot verify charges, stored contents or toggles.

**Wilderness defaults** favour owned Wilderness weapons with budget armour and accessories. When a setup lacks a usable owned head, weapon, body or legs choice, its heading says **not enough gear in bank**. Missing slots can show muted, source-listed budget suggestions such as black d'hide armour; owning expensive gear does not relax the filter. A view can show suggestions even when nothing eligible is owned. Expensive general gear cannot fill gaps in these defaults; source-listed low-risk inventories are preferred where available. Budget choices are curated, not a live GP limit, and the plugin does not assume items are protected on death. Exact individual guide choices remain available through **Guide choices**. [Selection scope and sources](src/main/resources/com/windowmarkers/WILDERNESS-AND-SUPPLY-SOURCES.md).

When a home-teleport tablet is missing, the inventory can use an owned Construction cape or ordinary Max cape. Combat Max capes and redirected tablets are not interchangeable home teleports. This substitution does not imply that your house has the guide's required facilities.

Missing or unready inventory items appear as dimmed guide previews with no withdrawal action. Unbankable supplies, such as Moonlight potions and Tithe Farm seeds, also appear as guide previews with acquisition notes. They are not added to bank or GE search results. Inventory-only activities do not need an equipment table. Hover to see why a preview or alternative is shown. Intentionally empty source positions and placeholder-only slots stay empty. When the Wiki lists price-dependent food or potion alternatives, an owned eligible choice is shown; live prices are not compared. See [inventory source notes](src/main/resources/com/windowmarkers/INVENTORY-LAYOUT-SOURCES.md).

**Suggested runes** sit directly beneath the first equipment setup, before later phase setups. They are the activity's already-matched rune choices, not a requirement to bring every rune; choose those needed for your spellbook, spells and rune pouch. Other gear follows below all phase sections by slot or documented raid room, followed by food, potions and utilities. Room alternatives cover Chambers of Xeric, Theatre of Blood and Tombs of Amascut; a shared item can appear in multiple rooms. Guardian pickaxes have their own group, including verified Dragon pickaxe cosmetics. Hover explanations retain the guide's conditions. Expand **Legend → Saved views → Guide choices** to choose one equipment table or an inventory example. Ordinary item searches and categories keep their order. Improved Banking's preset views retain their contents and layout when the bank closes and reopens. Choosing a regular tab or search clears the preset; existing saved Bank Tags layouts are kept separately.

Use **Legend → Saved views → Save as custom view…**, then select the copy in the same section and click **Open saved view** to return. **Edit saved view…** opens its draft editor; **Save changes** updates that copy. A new copy must have a distinct name. Deleting a saved view keeps existing Legend groups. Saved views preserve exact item IDs, colours, guide and inventory context; they do not automatically expand to future Wiki recommendations. They remain separate for each character and RuneLite profile. Temporary views keep their own display arrangement when Bank Tag Layouts is enabled. Existing saved tag layouts remain separate. Native tab dividers are hidden while a grouped activity layout is shown, including magnifying-glass searches, and return to native control on the next bank build.

**Tools → Add Bank Tags tag…** opens the item palette and a tag-name field. Confirm to add that tag through Bank Tags, preserving each item's existing tags. The optional variations checkbox uses Bank Tags' own variation grouping; it starts off. These are real Bank Tags tags, shared according to Bank Tags' normal RuneLite profile storage rather than Improved Banking's character-specific marker storage. Use the tag in a Bank Tags tab and its layout tools. No items are withdrawn or moved in the underlying bank.

### Auto-categorize

Open **Legend → More group actions → Auto-group bank**. Choose a broad classification, skilling, outfit, combat, supply, utility or raid preset. Review the proposed groups before applying them. Bulk presets skip already-linked items by default. **Include items already in other groups** keeps multiple uses of the same item; it starts checked when adding an individual category. Alternatively, check an old marker for replacement. Area markers remain. A batch is one Undo step.

The bundled catalogue includes skilling outfits and tools, production ingredients, equipment sets, general combat, supplies, travel, storage and quest/clue items. General categories use Wiki item classifications; activity choices use the relevant guide's equipment and inventory lists. Partial set ownership does not imply an active set bonus. A set weapon without matching armour remains a standalone weapon category. Unknown items remain in an unchecked review group.

The CoX, ToB and ToA presets retain their separate gear, supply and specialist roles. **Choose a Wiki activity and setup** opens a searchable selection of boss, skilling and activity guides. Select one equipment table or example inventory, read its conditions and review the matching items before applying markers. These choices begin unchecked and do not combine alternative setups into one loadout. 

Equivalent items are recognized only for the effect they share. Fire and Infernal max capes can fill their corresponding combat-cape roles; they do not acquire every ordinary Max cape perk. Skilling outfit recolours and upgrades retain their documented XP or movement benefits, without copying extra effects, clue requirements or quest identities. Guide references preserve specific variants, including lantern fuel, cape trim and poison. Generic Barrows armour alternatives stay in their equipment slots; ornamented Sanguinesti staffs and Scythes retain their combat roles without transferring god protection. Charged, empty, damaged and broken versions keep separate state notes. Verified Echo Barrows, blazing blowpipe, mutagen helm, coloured crystal armour and infernal-tool cosmetics retain their functional alternatives. Depleted tools can remain useful without inheriting a rank for a charge-dependent effect. Teleport roles require a charged version, including an equipped glory for Abyss trips; uncharged jewellery stays available for its separate combat or passive uses. Inventory examples follow the selected method and team size, even when guides reuse the same setup name. Hover notes identify reduced-capacity items such as damaged essence pouches. An item in your bank does not prove that it has sufficient charges, that you meet every requirement or that a complete set is equipped.

Guide alternatives also include ordinary linked items and named footnotes. For example, an ornate-handle granite maul requirement selects the upgraded weapon, while Vale Totems offering examples include the guide's magic-wood products. Links explaining a required spell, weapon or complete set remain conditions; they are not automatically added as interchangeable equipment. Follow the pinned guide when a setup refers to another phase or ranged section.

**Exclude worse items** conservatively compares supported alternatives using owned replacements, real skill levels and verified unlocks. It preserves separate roles, special attacks, set effects and useful alternatives. It does not claim a universal gear ranking. [Coverage details](src/main/resources/com/windowmarkers/CATALOGUE-COVERAGE.md), [Wiki attribution](src/main/resources/com/windowmarkers/TAXONOMY-SOURCES.md) and [raid comparison sources](src/main/resources/com/windowmarkers/RAID-FILTER-SOURCES.md) describe the implemented scope.

### Grand Exchange search

Check **Grand Exchange search → Enable Grand Exchange search** in the plugin settings. It defaults to off. Allow a moment for the local catalogue to load, then use the ordinary GE buy-item search. Examples include `toa head`, `tombs magic head`, `tob weapon`, `cox cape`, `vorkath ranged`, `zulrah weapon`, `mining tools`, `woodcutting tools` and `herblore ingredients`. Slot qualifiers include head/helm/hat/hood, body/chest/top, legs/bottom, hands/gloves, feet/boots, neck/amulet, cape, weapon, offhand/shield/defender, ammo and ring. Add `1`, `1st` or `bis` to show only first-tier items; `2`/`2nd` and `3`/`3rd` work the same way. For example, `toa magic head 2nd` shows second-tier matching headgear. Multiple tier tokens select their union. Unranked items are excluded from a tier-filtered search; ordinary numbered item names remain native searches. These keywords work in both bank and GE searches. The search covers the shared classifications and all audited activity guides, not only raids. Only GE-tradeable items appear; a broad search exceeding the native 250-result limit must be narrowed.

For shopping, `tools` means tools, not a guide's energy potions or other inventory supplies. `farming` selects directly classified Farming supplies and equipment. Narrow it with `farming seeds`, `farming trees` (tree seeds and saplings), `farming saplings`, `farming tools` or `farming compost`. Hover **Search tips [?]** at the top left of the chatbox for a short list of more specific queries, then type the one you want. These shopping refinements apply only to the GE; the bank retains its broader organizational categories.

**Activity items** starts at **All items**. Choose **Owned**, **Not owned** or **Next guide upgrade**, or append `owned`, `unowned` or `next upgrade` to an activity query, such as `toa magic top next upgrade`. Open your bank once after login so the plugin can include bank, inventory and equipped items. Before that, ownership filters leave results unfiltered and show a notice. This snapshot stays in memory, clears across logins and profiles, and is never uploaded. Placeholders do not count as owned. A depleted item does not imply ownership of its charged benefits, and an ordinary trident does not imply enhanced capacity. The compact **[O]** badge means owned; hover to see whether it is the exact item or a verified usable equivalent; an empty or broken counterpart does not prove usable ownership.

**Next guide upgrade** compares supported roles within the same Wiki equipment table and selects the nearest available better column above owned ready equipment. It is not a DPS, budget or value-for-money recommendation. Conditional, special-attack, set-dependent and unsupported roles may have no upgrade result; use **All items** to see those alternatives and items needing charging or repair. **Hide worse than owned** starts off and hides an item only when verified usable owned gear outranks it in every compared role. It retains specialist, conditional, set and unranked alternatives. **Owned** mode still shows all owned matches. The optional **Mark owned activity items** adds **[O]** badges without filtering. **[N]** marks a next guide upgrade.

GE results keep the item name on the first line and compact badges and price on the second. Long text is shortened to fit the native row; hover for the full item name, badge meanings, requirements and exact guide price in gp. `~?` means the price is unavailable. Narrow layouts may leave details in the tooltip.

**Show guide prices** starts off. Prices use RuneLite's cached price source and show the estimated cost of one item, with the full amount on hover. **Colour prices by funds** uses green when observed coins and platinum cover that amount, red when a known balance falls short, and neutral when the price or balance is unknown. It includes inventory and the last observed bank balance; a GE offer change invalidates that bank balance until the bank is observed again. Uncollected offers and the resale value of equipment do not count. Guide prices are not guaranteed offers, and account restrictions and buy limits still apply.

**Mark unmet requirements** also starts off. It adds a red **[!]** badge and a hover explanation for reviewed unmet skill or unlock requirements. Coverage is partial: an item without a tag is not guaranteed usable, fully charged or part of a complete set. This does not disable purchases or alter item actions. Price, ownership and requirement labels are independent of the guide-tier colours.

**Gear guide tiers → Show Wiki gear tiers** adds shared configurable colours and tier tags to the first three columns of a matching guide's recommended-equipment table. The defaults are purple for 1st, blue for 2nd and green for 3rd. The bank uses matching borders and 1st/2nd/3rd corner tags on ranked items; the GE uses compact `[1]`, `[2]`, `[3]` badges. First choice is BIS **for that setup and role**, subject to its requirements. Hover the item name for context and conditions. General categories, example inventories, conflicting rankings, provisional recommendations and items needing preparation stay neutral. A missing or untradeable first choice does not turn second choice into first. Purchasing a listed variant does not include its charges, upgrades or set partners.

Verified purchase alternatives can appear alongside a guide's exact items, such as an uncharged bow or enhanced-capacity trident. Hover explanations identify charging, repair, filling and capacity differences; these alternatives do not inherit a ready-to-use gear colour. Role searches also recognize two-handed shields and supported blowpipe ammunition without confusing those roles with ordinary weapon recommendations.

Full equipment tables win ties over partial switches when both cover the same owned slots. Sire Phase 1 switch lists are explicitly labelled **Equipment switches**; omitted slots are not invented. Relevant Avernic treads upgrades count for their matching single combat style, without claiming the bonuses of their missing upgrades. [Equipment audit and source details](src/main/resources/com/windowmarkers/EQUIPMENT-SLOT-AUDIT.md).

Use **Legend → Saved views → Guide choices → Choose bank / GE guide** to narrow an activity to one source table and read its conditions. **Clear bank / GE guide** restores all matching setups. This same context applies to live bank searches; saved setup groups retain the guide they were created from. Search again at the GE after changing the guide or settings. The context is saved per RuneLite profile; other activities remain searchable. Ordinary item-name searches and earlier-handled searches such as Bank Tags retain their existing behavior. This feature does not buy items or change offers, prices or quantities. [Tier data and scope](src/main/resources/com/windowmarkers/GE-SEARCH-SOURCES.md).

### Bank layout

**Tools → Bank size and tabs** controls bank width, height, columns, tab alignment, spacing below the tabs and deposit-button placement. Item icons retain their native size. Bank Tags custom layouts retain their designed grid. Linked markers follow reflow; area markers keep their saved coordinates.

**Hold Alt and drag a bank button** to move it along the bottom row, change its order, or place it in the left dock beside Bank Tags. Guides appear while Alt is held. Release to save the arrangement automatically; releasing outside the bank, losing focus, closing the bank or changing profiles cancels an unfinished move. An Alt-click without a drag does not activate or rearrange the button. Normal clicks keep their usual behavior. Ctrl+Alt stays available for RuneLite's overlay controls.

**Tools → Reset bank buttons** restores your previous bank controls. **Custom bank buttons** can also be turned off in settings, and **Alt-drag bank buttons** disables the drag shortcut if desired. Only a completed drag enables a custom arrangement.

The dock needs a bank at least 536 pixels wide. Controls that cannot fit beside a short or narrow bank stay in the bottom row. Buttons keep their native sizes, actions, tooltips and artwork; controls hidden by the game stay hidden. The dock reserves its own space beside Bank Tags and the item grid, including Bank Tag Layouts. The arrangement is saved in your RuneLite profile and takes precedence over the older deposit-button placement while enabled. The built-in bank layout must also be enabled.

The ordinary All tab preserves native tab sections without treating transparent tab drop areas as extra item rows.

## Saved settings

Markers are separate for each RuneScape character **and** RuneLite settings profile. Renaming a profile preserves its markers. Each combination supports up to 20 named layouts and 250 markers per layout. Switching characters or profiles cancels drawing and loads the matching layout.

**New**, **Copy** and **Rename** manage layouts. **Undo / Redo** keeps up to 50 marker changes per layout during the current session; restarting clears history, not saved markers. Layout-management actions themselves are not undoable.

Existing Window Markers beta settings and saved layouts are preserved under their original storage keys. Tools can copy earlier account-only or shared marker layouts into an empty current layout without changing the originals.

## Compatibility

Improved Banking replaces the separate **Flexible Bank** and **Bank Screenshot** plugins and declares those conflicts. Existing Flexible Bank size preferences are copied once when no corresponding Improved Banking preference exists. Original settings remain stored. Disable Improved Banking before re-enabling the separate plugins. The integrated bank-layout option can also be disabled in settings.

The screenshot button can be disabled independently in **Screenshots** settings. Bank controls, menus and character actions remain under your control. Slot grouping changes the displayed search layout only; it never rearranges the stored bank, withdraws or equips items.

## Inspiration and recipe

The recipe is searchable categories, recognisable gear layouts, clear markers and editable personal groups. Flexible Bank supplies adapted layout code. Bank Tags, Bank Tag Layouts, Screen Markers and the other tools below illustrate the focused workflows that this combination brings together or complements.

| Project | Connection | What it contributes or complements |
| --- | --- | --- |
| [Flexible Bank](https://github.com/TheLeon0/flexible-bank) | Adapted foundation | Bank sizing, columns and deposit-button layout; credit to TheLeon0. Declared conflict because both plugins manage the bank geometry. |
| [Bank Screenshot](https://github.com/raiyni/example-plugin/tree/f96b865927366f1c19b9ec46483d9d0f503f57c7) | Feature reference | Bank captures. Improved Banking adds its own marker artwork and legend to current-tab exports. Declared conflict. |
| [Bank Tags (built in)](https://github.com/runelite/runelite/tree/master/runelite-client/src/main/java/net/runelite/client/plugins/banktags) | Integrated companion | Persistent tags and tab layouts. Our Add Bank Tags tool uses real tags and respects the active tag's item membership. |
| [Bank Tag Layouts](https://github.com/geheur/bank-tag-custom-layouts) | Layout inspiration and alternative | Arranged item positions and layout placeholders. Our guide views use an equipment/inventory shape; Bank Tags' core layouts remain a separate workflow. |
| [Inventory Setups](https://github.com/dillydill123/inventory-setups) | Related setup workflow | Records personal gear, inventories and setup details. Our editable Wiki views do not import or replace its saved setups; choose one active bank-layout workflow at a time. |
| [Screen Markers (built in)](https://github.com/runelite/runelite/tree/master/runelite-client/src/main/java/net/runelite/client/plugins/screenmarkers) | Marker inspiration | Visual boxes over the client. Improved Banking's markers instead belong to the bank contents or follow linked items. |
| [Wasted Bank Space](https://github.com/mcgeer/WastedBankSpace) | Complementary cleanup approach | Shows items that can be stored elsewhere. Our initial Junk review explains reviewed niche uses; it does not reproduce the complete storage-location catalogue. |
| [Bank Tag Generation](https://github.com/MitchBarnett/wiki-bank-tag-integration) | Related category workflow | Creates bank tags from Wiki categories or monster drops. Our catalogue is bundled and reviewed, with category search inside the plugin. |
| [Bank Tab Names](https://github.com/Psyda/bank-tab-names) | Optional tab presentation tool | Styled names and icons for bank tabs. Legend group labels identify collections of items, a different layer from tab names. |
| [Grand Exchange (built in)](https://github.com/runelite/runelite/tree/master/runelite-client/src/main/java/net/runelite/client/plugins/grandexchange) | Built-in companion | Price information and ordinary GE search. Improved Banking optionally adds category/activity queries to the in-game Buy-item search; the price-lookup sidebar remains separate. |
| [Resource Packs](https://github.com/melkypie/resource-packs) | Optional appearance tool | Active-theme marker borders can use already-loaded pack sprites. No pack artwork is bundled. |
| [Interface Styles (built in)](https://github.com/runelite/runelite/tree/master/runelite-client/src/main/java/net/runelite/client/plugins/interfacestyles) | Optional appearance tool | Loaded interface sprites can also supply themed marker borders, with a vanilla fallback. |

The [Old School RuneScape Wiki](https://oldschool.runescape.wiki/) supplies attributed category and strategy-guide information. It is credited as a data source, with pinned references and its own license, rather than presented as a plugin dependency. Catalogue updates arrive with plugin releases; bank contents are not uploaded. See [License and attribution](#license-and-attribution) for code and data credits.

## Development

Use Java 11 and run `./gradlew test jar` (Windows: `gradlew.bat test jar`). Published source uses the normal plugin name and RuneLite activation defaults. Local testing builds display **Improved Banking TEST** and retain the same feature-storage keys.

## Support

[Report an issue, correction or feature request](https://github.com/wukuna/improved-banking/issues). For category problems, include the item names, selected preset and whether Exclude worse items was enabled. For layout problems, include the current bank tab, RuneLite profile context, overlapping bank plugins and a screenshot with private details removed. All requests and information are considered and appreciated.

## Inventory switches and item outlines

Inventory gear switches accept owned combat variants, including ornamented jewellery. Missing ranged capes, necklaces and off-hands can use alternatives listed in the same activity's ranged equipment guide. For example, Muspah can use an owned Aquanite hopper for its Twisted buckler inventory slot, or an assembler/accumulator cape in place of Dizana's quiver. Lower-tier alternatives retain their own stats, effects and guide ranks; charge checks, protected equipment and Wilderness budget rules still apply.

Adjacent item highlights with the same colour share a continuous outline, including irregular groups. Different guide colours, empty slots and gaps between setups remain distinct. Tier labels and hover explanations remain available.

Under **Appearance**, **Hide item outlines in presets** removes preset outlines and backgrounds while retaining rank badges. **Hide item labels in presets** removes only the small 1st/2nd/3rd badges. Both start off and also apply to activity search layouts. Setup headings, item icons, hover explanations, item choices and ordinary bank markers remain available. Saved screenshots also respect the outline/background switch.

## License and attribution

Original Java code is BSD 2-Clause; see [LICENSE](LICENSE). The integrated bank-layout controller adapts [TheLeon0/Flexible Bank](https://github.com/TheLeon0/flexible-bank/tree/0ea1d3bf2c7bc9534323d4d6939599681c4d40fa), with its BSD licence and attribution included in source and resources.

The bundled Wiki-derived data files are distributed under **CC BY-NC-SA 3.0**, separately from the Java code. Their source revisions, attribution and modifications are documented in [TAXONOMY-SOURCES.md](src/main/resources/com/windowmarkers/TAXONOMY-SOURCES.md). No resource-pack artwork is redistributed.

Preset item tooltips combine rank and inventory notes into a compact, wrapped summary. Item-state warnings remain visible; long strategy footnotes stay in Guide choices. Hover text is capped at six lines and 300 pixels wide, using your tooltip font.
