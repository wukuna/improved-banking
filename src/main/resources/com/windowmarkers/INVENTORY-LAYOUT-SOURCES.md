# Inventory examples and room groups

OSRS Wiki contributors, CC BY-NC-SA 3.0. Reviewed 2026-10-25.

`setup-inventory-pairs.tsv` pairs an equipment example with the inventory under its matching Wiki tab or role. Explicit inventory selection takes priority; ownership coverage only chooses among equally preferred candidates. Cosmetic variants and different charge states of one field do not multiply that field's coverage score. A style match is used for other guides only when the style is nonempty.

- [Chambers of Xeric](https://oldschool.runescape.wiki/w/Chambers_of_Xeric/Strategies?oldid=15373675): minimum, standard, Bowfa, DHCB and max examples retain their corresponding inventories.
- [Challenge Mode](https://oldschool.runescape.wiki/w/Chambers_of_Xeric/Challenge_Mode/Strategies?oldid=15373687): intermediate and maximum examples retain their corresponding inventories.
- [Theatre of Blood](https://oldschool.runescape.wiki/w/Theatre_of_Blood/Strategies?oldid=15372089): Entry Mode, learner/Void, melee, mage/freezer and range roles remain distinct. The advanced melee role uses the melee inventory example, not a union of all role inventories.
- [Tombs of Amascut](https://oldschool.runescape.wiki/w/Tombs_of_Amascut/Strategies?oldid=15373676): minimum through maximum/expert examples retain their corresponding inventories and invocation labels.

All 17 raid inventory examples retain the source positions and repeated items. ToB Entry Mode contains 20 populated item positions and minimum ToA contains 27; the other examples contain 28. Empty positions are not filled with invented recommendations. Inventory diagrams are examples, not quantities owned, withdrawal instructions or guaranteed usable loadouts. Conditional equipment stays subject to its guide requirements.

`setup-inventory-previews.tsv` records 14 positions across seven non-raid inventories whose pictured items cannot be banked. Each row includes an exact cache item ID, expected name, acquisition note and pinned item-page citation. These are dimmed, noninteractive previews only; they never become bank search memberships, GE candidates or withdrawable bank stacks. They cover Mimic, Logavano seeds, Moonlight potions and bloated toads. The two Maniacal monkey inventories deliberately use four placeholder-only positions each; those remain empty because they identify no item.

`raid-layout-rooms.tsv` maps 34 room/phase equipment tables to headings, using the exact memberships and citations in `activity-setups.tsv`. Conditions such as drained defence, invocation levels, required sets and weapon-specific phases remain in the hover notes. An item can appear in several relevant room groups. Pickaxes and defence-draining special weapons have separate Guardian groups. An [inactive crystal pickaxe](https://oldschool.runescape.wiki/w/Crystal_pickaxe?oldid=15366313) has dragon-pickaxe mining performance; the CoX guide says the crystal pickaxe's advantage at Guardians is its lower weight, not improved damage from charges. The Guardian exception does not make it equivalent for ordinary mining.
