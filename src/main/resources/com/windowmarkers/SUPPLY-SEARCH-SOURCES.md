# Supply search evidence

Reviewed 2026-10-26. Data attribution: Old School RuneScape Wiki contributors, CC BY-NC-SA 3.0. Compiled data is bundled; no runtime Wiki requests or player-data uploads.

## Food

Food identities use exact Wiki item IDs, with name guards on healing facts. `food-search-facts.tsv` records the item-page revision for each identity. Healing and bite counts come from [Food/All food, revision 15318406](https://oldschool.runescape.wiki/w/Food/All_food?oldid=15318406), cross-checked against [Food, revision 15373716](https://oldschool.runescape.wiki/w/Food?oldid=15373716) and individual pages for conditional healing. Only edible identities are included; potion/drink, raw, unfinished, burnt and poisonous variants are not inherited from a shared name. Whole melons/pineapples that need preparation are not ready-to-eat matches.

[Fast-food mechanics, revision 15368024](https://oldschool.runescape.wiki/w/Food/Fast_foods?oldid=15368024) and the [fast-food table](https://oldschool.runescape.wiki/w/Template:Fast_foods_table) distinguish non-standard eating/attack delays from same-tick combo eating. A final cake slice loses the earlier slices' fast-eat flag. A half pie remains fast but is no longer a multi-bite item. The final giant-crab-meat portion is likewise not multi-bite.

[Anglerfish, revision 15360516](https://oldschool.runescape.wiki/w/Anglerfish?oldid=15360516) uses the documented base-Hitpoints formula, including its 93-HP boundary. Strawberries, watermelon slices and cooked sweetcorn use their documented base-level formulas. Variable-range foods use the minimum; unknown and random healing are excluded from thresholds. Varlamorian kebab's average is not treated as guaranteed healing. One bite of summer pie is 11 HP, not 22. Hunter-food totals include immediate plus delayed healing; the separate instant-food filter excludes delayed foods. [Moonlight antelope, revision 15374170](https://oldschool.runescape.wiki/w/Cooked_moonlight_antelope?oldid=15374170) documents replacement of pending hunter-food healing. No healing is inferred merely from an Eat action.

Blighted food remains explicitly area-restricted, including the documented PvP-world and safe-area exceptions; it is not universally usable food. Overhealing has its own PvP restrictions. Activity presets continue to use their own guide supply lists, not these broad groups. Unbankable food may have a factual catalogue entry but cannot become an owned-bank or tradeable-GE result.

## Potion effects

[Potions, revision 15351958](https://oldschool.runescape.wiki/w/Potions?oldid=15351958), individual potion pages and their exact dose IDs establish effects. Unfinished potions and empty containers are excluded. Restore and super restore differ on Prayer; regeneration differs from immediate restoration; poison and venom cures/immunity differ; anti-venom and antidote++'s short venom immunity must not be confused with anti-venom+'s longer immunity. The venom-cure group describes a complete one-dose cure. Healing mixes retain their original potion effects. Blighted restores carry area restrictions. Strength, duration, side effects and dose requirements are not ranked as interchangeable.

## Ammunition

[Ammunition, revision 15374969](https://oldschool.runescape.wiki/w/Ammunition?oldid=15374969), [bows, revision 15309525](https://oldschool.runescape.wiki/w/Bow?oldid=15309525), crossbow tables and weapon-specific pages establish the supported identities and tier limits. Only complete ammunition enters a compatibility group. Poisoned darts cannot be loaded into the [toxic blowpipe](https://oldschool.runescape.wiki/w/Toxic_blowpipe?oldid=15376390). [Dorgeshuun crossbow](https://oldschool.runescape.wiki/w/Dorgeshuun_crossbow) permits bone bolts and metal bolts through iron, excluding silver and gem-tipped bolts. [Hunters' sunlight crossbow](https://oldschool.runescape.wiki/w/Hunters%27_sunlight_crossbow?oldid=15212660) uses antler bolts, not kebbit bolts. Karil's uses bolt racks. Ogre bow and comp ogre bow retain different brutal-arrow support. Cannon matches steel/granite cannonballs; salamanders retain their specific tar. Charge-based bows receive no interchangeable-ammo inference.

These are compatibility groups, not accuracy or damage rankings. The reviewed set does not claim every unusual quest arrow, cosmetic weapon alias or situational ammunition is covered. Unreviewed compatibility stays unassigned. No Fractured Archive presets or new content-specific groups are included.
