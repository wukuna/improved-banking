# Improved Banking item taxonomy and activity data

Original tables reviewed October 20, 2026; full snapshot reconciliation and scoped equivalence audit updated October 23, 2026. The plugin uses bundled local lookups; it sends no bank data to the Wiki and does not fetch recommendations during play.

## Attribution and data licence

The exact-ID organizational supplement, `wiki-category-supplement.tsv`, also adapts OSRS Wiki contributor data under CC BY-NC-SA 3.0. [WIKI-CATEGORY-SOURCES.md](WIKI-CATEGORY-SOURCES.md) records its revisions and scope. It adds broad roles without granting activity eligibility or lower-tier rankings. [CATALOGUE-COVERAGE.md](CATALOGUE-COVERAGE.md) records the completed snapshot reconciliation and the identities or functional uses that remain explicitly unverified.

New resources retain that same data licence. Separate source records describe [variant states](VARIANT-STATE-SOURCES.md), [cape combat equivalence](COMBAT-EQUIVALENCE-SOURCES.md), [cosmetic combat equivalence](COSMETIC-EQUIVALENCE-SOURCES.md), [skilling effect alternatives](SKILLING-EQUIVALENCE-SOURCES.md), [recipe inputs and tools](RECIPE-ROLE-SOURCES.md), [source-specific activity setups](ACTIVITY-SETUP-SOURCES.md) and [additional raid contexts](RAID-CONTEXT-SOURCES.md). Exact-ID exclusions and review guards preserve source conflicts instead of guessing at their meaning.

`weapon-roles.tsv`, `raid-equipment.tsv`, `item-catalogue.tsv`, `raid-ranks.tsv` and `raid-wear-requirements.tsv` adapt factual lists by **Old School RuneScape Wiki contributors**, available under [Creative Commons Attribution-NonCommercial-ShareAlike 3.0](https://creativecommons.org/licenses/by-nc-sa/3.0/). These adapted data files are distributed under that same licence. Original Java implementation remains under the repository's BSD 2-Clause licence. No DPS-calculator source code or data was incorporated.

Changes to the Wiki lists: lowercased names; extracted item membership; retained ranking separately for eligible same-slot raid comparisons; removed images and article prose; combined duplicate membership rows; grouped some weapon categories into broader roles; distinguished tools. This attribution file must accompany those resources.

| Source | Retrieved revision | Use |
| --- | --- | --- |
| [Weapons/Types](https://oldschool.runescape.wiki/w/Weapons/Types?oldid=15378242) | 15378242 | 1,024 named weapon variants and their roles |
| [Chambers of Xeric/Strategies](https://oldschool.runescape.wiki/w/Chambers_of_Xeric/Strategies?oldid=15373675) | 15373675 | Main melee, ranged and magic equipment lists; Guardians tools; supplies and conditions |
| [Theatre of Blood/Strategies](https://oldschool.runescape.wiki/w/Theatre_of_Blood/Strategies?oldid=15372089) | 15372089 | Learner, melee, magic and ranged lists; role-specific notes and supplies |
| [Tombs of Amascut/Strategies](https://oldschool.runescape.wiki/w/Tombs_of_Amascut/Strategies?oldid=15373676) | 15373676 | Three combat-style lists; prepot supplies and invocation restrictions |

The raid resource contains 442 activity/group/item membership entries, including duplicates of an item used in several groups. This is not 442 unique items. Source revisions identify the retrieved snapshots, not a promise that the Wiki never changes. Direct page retrieval succeeded after the search index exposed older excerpts; the bundled equipment membership comes from the direct snapshots.

Additional mechanic references: [Barrows equipment](https://oldschool.runescape.wiki/w/Barrows_equipment), [Void Knight equipment](https://oldschool.runescape.wiki/w/Void_Knight_equipment), [Crystal equipment](https://oldschool.runescape.wiki/w/Crystal_equipment), [Inquisitor armour](https://oldschool.runescape.wiki/w/Inquisitor_armour), [Obsidian armour](https://oldschool.runescape.wiki/w/Obsidian_armour), [Moons of Peril](https://oldschool.runescape.wiki/w/Moons_of_Peril), [Blue moon armour](https://oldschool.runescape.wiki/w/Blue_moon_armour), [Eclipse atlatl](https://oldschool.runescape.wiki/w/Eclipse_atlatl).

## Findings and implemented rules

* Weapon identity takes precedence over stat comparisons. Ancient sceptres and Twinflame staff are casting weapons despite their melee bonuses. Salamanders have multiple combat styles. The Eclipse atlatl is ranged despite Strength-based damage. Adding unlike bonuses together cannot determine these roles.
* All six Barrows families include their weapons: Ahrim's staff, Dharok's greataxe, Guthan's warspear, Karil's crossbow, Torag's hammers and Verac's flail. Degraded variants retain membership; broken pieces are distinguished from usable pieces. Family ownership is not proof that a set effect is active.
* Moons families include Dual macuahuitl, Blue moon spear and Eclipse atlatl. Ammunition is separate. The armour can have useful individual stats without its full set effect.
* Void needs its helm, top, robe and gloves. The mace and seal are not required set components. The raid tables in this version use elite Void; incomplete combinations start unchecked.
* Crystal armour has bow-specific synergy, not a generic synergy with crystal tools, halberds or the Blade of Saeldor. The raid Bowfa group starts unchecked unless the bank contains Bowfa and all three usable armour pieces. This conservative raid rule does not imply partial crystal armour has no benefit.
* Inquisitor armour benefits crush attacks without making the mace mandatory. Justiciar requires no particular weapon. Obsidian's melee synergy does not include its staff, ranged weapon, cape or shield as required pieces.
* Equipment can serve several roles. Hybrid accessories are not forced into whichever style produces the largest arbitrary sum. Unresolved defensive/utility items remain explicitly reviewable.
* General food and potion classification is separate from activity suitability. Eat/Drink never admits an item to a raid recommendation. Positive supply lists exclude unrelated potions, blighted variants, damaging rock cakes, sailing drinks, purple sweets and nonmatching potion mixes.
* Supplies carry context: ToB freezer spells, CoX shamans/solo Olm, and ToA prepot paths/On a Diet/Dehydration. The plugin does not read or validate active invocations, spellbook, team role, or actual charges. Context labels describe conditions, not an automatic eligibility verdict.
* Match names exactly after narrow, documented variant normalization. Do not use arbitrary substring matches that admit ornament kits, armour-set boxes, unrelated weapons or restricted supplies. Imbued and fortified variants retain their identity.

## Catalogue coverage

The full category suite and measured source coverage are documented in [CATALOGUE-COVERAGE.md](CATALOGUE-COVERAGE.md). The broader catalogue adds skill tools, complete outfit families, materials, boosts, combat slots, supplies, travel, storage, quest/clue roles and cosmetic/utility equipment. Item IDs and exact variant names come from individual Wiki item infoboxes where available; unresolved fetches retain only list-backed names. Source classifications are adapted into display groups and can overlap.

This remains a classification aid, not a complete raid inventory planner or DPS/BIS optimizer. Specialist encounter tables, Entry/Hard Mode and all spell/ammunition combinations are not exhaustively represented. General categories do not qualify items for activity recommendations. Linked IDs remain exact as described in README; classification aliases do not merge every dose/charge variant into one persistent link. More boss-specific recommendation lists still require separate verification.

## Regression coverage

Tests cover the reported staves, every Barrows/Moons set weapon, degraded/broken pieces, Void/crystal completeness, hybrid accessories, armour/accessory recognition in all three raids, cosmetic variants, imbued distinction, and a cross-raid matrix of unrelated foods, potions, rune types and misleading names. Runtime resources are exercised from the same classpath used by the plugin. In-client appearance and navigation remain user-tested separately.

## Ranked raid alternatives
The optional filter uses the main melee/ranged/magic tables from the three raid strategy revisions above. It compares weapons, armour and accessories within one role and slot, never globally removes an item from other uses, and checks reviewed wearing requirements. Set-dependent and specialist items, supplies, unknown eligibility and equal-ranked alternatives remain. Individual requirement-page revisions are recorded in RAID-FILTER-SOURCES.md. This is not a full loadout optimizer.

