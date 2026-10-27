# Bank cleanup review — 2026-10-27

This is a bank-only, opt-in search over observed owned items. It does not alter bank contents or declare all matched items useless. No runtime Wiki requests or bank-data uploads are made. The exact-ID/name rules in `bank-cleanup-items.tsv` include 62 burnt cooking identities, each with a pinned page revision. These reviewed examples are an initial set, not exhaustive cleanup coverage.

## Sources and interpretation

- [Burnt food](https://oldschool.runescape.wiki/w/Special:Redirect/page/23901?oldid=15167268): general cooking results and collector context. Neither a burnt name nor a lack of food healing proves no other use.
- [Burnt chicken](https://oldschool.runescape.wiki/w/Special:Redirect/page/22058?oldid=15303862) and [Keldagrim tasks](https://oldschool.runescape.wiki/w/Special:Redirect/page/30237?oldid=15336437): possible Forgettable Tale request and repeatable Gunslik/rowdy-dwarf requests. Completion of the quest or one task does not remove repeatable usefulness.
- [Burnt meat](https://oldschool.runescape.wiki/w/Special:Redirect/page/13528?oldid=15371141) and [Witch's Potion](https://oldschool.runescape.wiki/w/Special:Redirect/page/2148?oldid=15367062): one-time quest ingredient. In-progress state does not establish whether the item has already been handed in; the explanation preserves that uncertainty.
- [Forgettable Tale...](https://oldschool.runescape.wiki/w/Special:Redirect/page/8725?oldid=15374902): possible random item request, prerequisite quests and boostable skill requirements. The plugin does not infer the player's assigned random item or complete quest eligibility.
- Individual burnt-item sources identify recoverable containers, rare items and uncertain uses. Burnt pitta bread's normal-source availability guard remains in ordinary classifications; an exact, owned reviewed copy may still receive a rare-collector cleanup note. This does not grant equipment, food or activity capabilities.

## Conservative sell candidates

Only explicitly listed ordinary metal pickaxes/axes are compared using the existing reviewed `LowerTierFilter` and current base gathering levels. Known clue and quest facets remain protected. Iron pickaxe, iron axe and steel axe are additionally protected for repeatable Keldagrim task uses. Dragon replacements require a members world. No comparison here covers combat gear, charged/inactive tools, felling/infernal/crystal effects, ornaments or unreviewed equivalents. Lack of a sell result is not proof that an item must be kept; a result is not an instruction to sell it.

## Progress and interface

The official `Quest.getState(Client)` API is read on the client thread in deferred work between events, including on bank opening; it invokes the supported quest-status script indirectly. No quest-state script runs in a rendering or script callback. An unavailable state remains unknown. Snapshot results are scoped to the existing account/profile lifecycle. Only the four explicitly required quest identities are queried; unrelated content is not enumerated.

Light amber means a niche or unresolved purpose; grey means ordinary review. The item-reasons window provides the exact explanation. Quest filters describe mapped quest uses, not global item redundancy. An unmapped quest-related item remains unknown. Custom views keep explicit item selections while their review explanations use the current account snapshot.

Derived Wiki text/data: Old School RuneScape Wiki contributors, CC BY-NC-SA 3.0. Explanations are condensed and qualified; pinned source links preserve the reviewed evidence. Corrections are welcome.
