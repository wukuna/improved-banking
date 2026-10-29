# Wilderness defaults and inventory supplies

Reviewed 29 October 2026. These are bank-planning choices; no character actions, live combat guidance, prices or death-protection calculations are added.

## Wilderness budget defaults

`wilderness-budget-items.tsv` is a curated eligibility list of 309 exact equipment identities, with names and slots checked against the existing reviewed equipment and state tables. It retains ordinary budget alternatives and Wilderness weapons. It does not assign new Wiki ranks or claim that every eligible item is suitable for every activity. The selected source table, combat style, ammunition, charge state and other existing requirements still apply. Empty/broken variants in the identity list remain subject to their state guards.

The default combined bank views for Callisto, Artio, Venenatis, Spindel, Vet'ion, Calvar'ion, Scorpia, Chaos Fanatic, Chaos Elemental, Revenants, Krystilia, King Black Dragon, Green dragon, Chaos Temple (church), Abyss and Chest (Rogues' Castle) use that eligibility list before choosing equipment or completing omitted slots. KBD and Abyss are included for the Wilderness journey. Non-Wilderness activities and explicit individual source choices retain the original guide selection.

The player-requested policy is an owned, ready Wilderness weapon with budget armour/accessories. For example, expensive armour, jewellery, boots and general mega-rare weapons cannot enter via shared completion tables. Items are not selected by current GE value, and neither a fixed GP cap nor protected-item status is implied. Ordinary black d'hide, mystic, proselyte and comparable source-listed choices remain eligible. Source-listed low-risk inventories are preferred when no inventory is explicitly chosen. High-risk, BIS-smuggling and gravestone methods are omitted from the combined default; exact guide choices remain available. Expensive equipment switches are excluded from default inventory previews as well as owned results. Source inventory positions remain fixed.

Primary guide evidence remains the revisions pinned in `activity-setups.tsv`, with per-field revisions in `setup-field-revisions.tsv`. Current search-indexed guide content was also checked on the review date (direct Wiki reads were unavailable):

- [Callisto strategies](https://oldschool.runescape.wiki/w/Callisto/Strategies): budget ranged armour and accessories, Wilderness weapons, magic alternatives and supply roles. Bundled guide revision 15368267.
- [Spindel strategies](https://oldschool.runescape.wiki/w/Spindel/Strategies): budget melee/ranged alternatives and specialist Wilderness weapons. Bundled revision 15345527.
- [Calvar'ion strategies](https://oldschool.runescape.wiki/w/Calvar%27ion/Strategies): separate low-risk equipment/inventory example, retained with its original positions. Bundled revision 15379084.
- [Scorpia strategies](https://oldschool.runescape.wiki/w/Scorpia/Strategies): inexpensive magic alternatives and blighted/self-damaging supplies. Bundled revision 15317574.

## Inventory readiness and potion doses

The organizational facet `Self-damaging items — deliberate use` is retained. When a reviewed inventory lists the exact supply, that warning alone no longer makes the item unavailable. This covers divine potions, locator orbs, Zamorak brews and araxyte venom sacs where explicitly listed. Identity guards, actual charge/repair states, role requirements, other area restrictions and unbankable guide previews remain independent checks. Tooltips retain a deliberate-use note.

- [Divine potions](https://oldschool.runescape.wiki/w/Divine_potion) and [divine super combat potion](https://oldschool.runescape.wiki/w/Divine_super_combat_potion): self-damage is part of ordinary use, rather than a broken or unowned state. Current indexed pages checked on the review date.
- [Potions](https://oldschool.runescape.wiki/w/Potions) and [Wiki GE IDs](https://oldschool.runescape.wiki/w/Module:GEIDs/data.json): distinct potion types and dose identities.

`inventory-potion-doses.tsv` contains 152 exact identities across 38 ordinary potion families. Names, Drink actions and ordinary/non-note/non-placeholder identities were checked against the 26 October cache snapshot used by the existing catalogue audit. Ordinary GE-tradeable identities were retained, plus the exact three tradeable lower-dose blighted super restores (24601, 24603, 24605). Each family has doses 1–4. Unknown, minigame, mix, unfinished, same-name test and placeholder identities are not inferred by name at runtime.

The exact source dose is preferred. If absent, an owned bottle of the same listed potion can fill the display, preferring more remaining doses. A tooltip states its dose count and that the guide's total doses are not guaranteed. Normal/divine, regular/blighted and distinct anti-venom or antifire effects are separate families. A chargeable weapon, teleport, sack or container never gains a potion equivalence.

## Attribution

The two adapted data resources retain the catalogue's attribution to Old School RuneScape Wiki contributors and CC BY-NC-SA 3.0 licence, as recorded in [TAXONOMY-SOURCES.md](TAXONOMY-SOURCES.md). Source recommendations are narrowed for the requested budget policy; that policy is not a new Wiki ranking. Original Java code remains BSD 2-Clause. No existing guide tables or deferred-content exclusions are regenerated by this change.

## Final ownership and presentation audit

29 October 2026: retained the exact 309-identity Wilderness eligibility resource and every equipment-selection rule. Missing core gear (head, weapon, body or legs) is labelled in the affected section and persistent view title. Muted gear suggestions are drawn only from that section's source fields intersected with the existing budget eligibility and readiness/requirement checks. Suggestions never enter owned result membership, tiers or saved custom selections. Ordinary inexpensive weapons are preferred for unowned suggestions; specialist Wilderness weapons remain preferred when owned. Two-handed examples retain off-hand exclusion and compatible ammunition. These are suggestions, not verified account unlocks or a complete risk calculation.

The inventory corpus's CHECK/CONTENTS states requiring ownership-only presentation are ash sanctifier (25781), bonecrusher (13116), soul bearer (19634) and master scroll book (21389). Each uses one identity for variable charges or contents. An exact reviewed inventory match may show the owned bank stack, while its tooltip explicitly says the ID does not establish remaining charges, contents or toggle state. General item-state review, quest-stage copies, known empty/broken variants and unlisted use remain guarded. No charge value is inferred or persisted.

Evidence: existing exact state records and the [ash sanctifier](https://oldschool.runescape.wiki/w/Ash_sanctifier), [container items](https://oldschool.runescape.wiki/w/Container_items), [storage](https://oldschool.runescape.wiki/w/Storage), and [master scroll book](https://oldschool.runescape.wiki/w/Master_scroll_book) pages. Current indexed Wiki evidence checked on the audit date. The [Callisto guide](https://oldschool.runescape.wiki/w/Callisto/Strategies) confirms budget black d'hide, archer helm, rune gloves/mystic alternatives; no guide resources were regenerated.

All supported potion identities were exercised across the reviewed inventory corpus, including Antidote++ dose matching. A different potion effect is still not substituted. Owned preset highlights now use the same yellow treatment as native search; dimmed previews remain excluded from marker cells and actions. Matched activity runes are placed beneath the first gear diagram, before later phases, with a reminder to choose the relevant spells/pouch contents rather than bringing the entire list. Narrow layouts reserve rune space before stacking the inventory.
