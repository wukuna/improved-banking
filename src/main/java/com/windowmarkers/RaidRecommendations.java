package com.windowmarkers;

import java.util.*;

/** Explicit per-activity membership from the cited Wiki tables, plus reviewed supply lists. */
final class RaidRecommendations
{
    private static final List<String[]> EQUIPMENT=new ArrayList<>();
    static
    {
        EQUIPMENT.addAll(ItemTaxonomy.readRows("raid-equipment.tsv",3));
        EQUIPMENT.addAll(ItemTaxonomy.readRows("raid-context-equipment.tsv",3));
    }

    static Map<String,List<AutoCategories.Item>> suggest(AutoCategories.Preset preset,List<AutoCategories.Item> items)
    {
        String raid=raid(preset), prefix=raid.equals("cox") ? "CoX" : raid.equals("tob") ? "ToB" : "ToA";
        Map<String,List<AutoCategories.Item>> groups=new LinkedHashMap<>();
        for (String[] row : EQUIPMENT)
        {
            if (!row[0].equals(raid)) { continue; }
            for (AutoCategories.Item item : items)
            {
                if (!CombatEquivalence.matches(item,row[2])) { continue; }
                // Learner alternatives repeated in a main-role table use that role's group and filtering.
                if(row[1].startsWith("Learner") && EQUIPMENT.stream().anyMatch(other -> other[0].equals(raid)
                    && !other[1].startsWith("Learner") && CombatEquivalence.matches(item,other[2]))) { continue; }
                String group=row[1];
                if (ItemVariantState.needsActivityReview(item,group))
                { group=ItemVariantState.reviewGroup(item); }
                else if (isVoid(ItemVariantState.familyName(item)))
                { group=completeVoid(items) ? "Elite Void switches — choose helm for role" : "Incomplete Void set — review"; }
                else if (isCrystal(ItemVariantState.familyName(item)))
                { group=completeCrystal(items) ? "Crystal bow setup" : "Incomplete crystal setup — review"; }
                else if (raid.equals("tob") && group.equals("Magic")) { group="Magic switches — freezer role"; }
                else if (raid.equals("tob") && group.equals("Ranged")) { group="Ranged switches — ranger/Tbow use"; }
                // Keep combat role keys stable for the same-role upgrade filter.
                if (group.startsWith("Guardians tools") || group.startsWith("Special attacks"))
                { group=ItemVariantState.annotate(group,item); }
                AutoCategories.add(groups,prefix+" "+group,item);
            }
        }
        for (AutoCategories.Item item : items)
        {
            String supply=supplyGroup(raid,ItemVariantState.familyName(item));
            if (supply!=null)
            {
                if (ItemVariantState.needsActivityReview(item)) { supply=ItemVariantState.reviewGroup(item); }
                AutoCategories.add(groups,prefix+" "+supply,item);
            }
        }
        return groups;
    }

    static String raid(AutoCategories.Preset preset)
    { return preset==AutoCategories.Preset.CHAMBERS_OF_XERIC ? "cox" : preset==AutoCategories.Preset.THEATRE_OF_BLOOD ? "tob" : "toa"; }

    /** Discoverable names even when none of the relevant items are currently owned. */
    static Set<String> groups()
    {
        Set<String> result=new TreeSet<>();
        for(String raid:Arrays.asList("cox","tob","toa")) {
            String prefix=raid.equals("cox")?"CoX":raid.equals("tob")?"ToB":"ToA";
            Set<String> roles=new TreeSet<>();
            for(String[] row:EQUIPMENT) if(row[0].equals(raid)) {
                String role=row[1];
                if(raid.equals("tob") && role.equals("Magic")) role="Magic switches — freezer role";
                if(raid.equals("tob") && role.equals("Ranged")) role="Ranged switches — ranger/Tbow use";
                roles.add(role);
            }
            roles.addAll(Arrays.asList("Elite Void switches — choose helm for role","Incomplete Void set — review","Crystal bow setup","Incomplete crystal setup — review"));
            roles.addAll(Arrays.asList("Needs repair/charge — review","Check charges — review","Unverified item identity — review",
                "Activity restrictions — review","Check item use — review","Reduced function — review","Preparation needed — review","Check contents — review","Empty — review"));
            for(String name:Arrays.asList("saradomin brew","super restore","prayer potion","stamina potion","sanfew serum",
                "super combat potion","ranging potion","anglerfish","cooked karambwan","saturated heart","salve amulet (e)","salve amulet(ei)",
                "water rune","blood rune","air rune","antidote++","keris partisan of the sun","rune pouch")) {
                String group=supplyGroup(raid,name); if(group!=null) roles.add(group);
            }
            for(String role:roles) result.add(prefix+" "+role);
        }
        return result;
    }

    static String source(AutoCategories.Preset preset)
    {
        switch (preset)
        {
            case CHAMBERS_OF_XERIC: return "https://oldschool.runescape.wiki/w/Chambers_of_Xeric/Strategies";
            case THEATRE_OF_BLOOD: return "https://oldschool.runescape.wiki/w/Theatre_of_Blood/Strategies";
            case TOMBS_OF_AMASCUT: return "https://oldschool.runescape.wiki/w/Tombs_of_Amascut/Strategies";
            default: return "https://oldschool.runescape.wiki/w/Weapons/Types";
        }
    }

    static boolean defaultSelected(String group)
    { return !group.startsWith("Other") && !group.contains("review") && !group.contains("Incomplete") && !group.contains("Needs repair"); }

    private static boolean owns(List<AutoCategories.Item> items,String... names)
    { return items.stream().anyMatch(i -> !ItemVariantState.needsActivityReview(i) && ItemTaxonomy.matchesAny(i,names)); }

    private static boolean completeVoid(List<AutoCategories.Item> items)
    {
        return owns(items,"elite void top") && owns(items,"elite void robe") && owns(items,"void knight gloves")
            && owns(items,"void melee helm","void ranger helm","void mage helm");
    }

    private static boolean completeCrystal(List<AutoCategories.Item> items)
    { return owns(items,"crystal helm") && owns(items,"crystal body") && owns(items,"crystal legs") && owns(items,"bow of faerdhinen"); }

    private static boolean isVoid(String name)
    { String set=ItemTaxonomy.equipmentSet(name); return set!=null && set.startsWith("Void —"); }

    private static boolean isCrystal(String name)
    { String set=ItemTaxonomy.equipmentSet(name); return set!=null && set.startsWith("Crystal —"); }

    static String supplyGroup(String raid,String name)
    {
        String n=ItemTaxonomy.baseName(name);
        // Intentionally positive lists: no Drink/Eat inference, substring matches, or blanket potion/rune groups.
        if (raid.equals("tob"))
        {
            if (oneOf(n,"saradomin brew","super restore","prayer potion","stamina potion")) { return "Restoration supplies"; }
            if (oneOf(n,"super combat potion","divine super combat potion","ranging potion")) { return "Combat boosts — role/prepot"; }
            if (oneOf(n,"anglerfish")) { return "Food — overheal/Maiden"; }
            if (oneOf(n,"cooked karambwan")) { return "Food — combo eating"; }
            if (oneOf(n,"saturated heart","imbued heart")) { return "Magic boost — freezer role"; }
            if (oneOf(n,"salve amulet (e)","salve amulet(ei)","salve amulet (ei)")) { return "Bloat — undead melee"; }
            if (oneOf(n,"water rune","death rune","blood rune")) { return "Ice Barrage runes — Ancients/freezer"; }
        }
        else if (raid.equals("cox"))
        {
            if (oneOf(n,"saradomin brew","super restore","prayer potion")) { return "Restoration supplies"; }
            if (oneOf(n,"stamina potion")) { return "Stamina — solo Olm/running"; }
            if (oneOf(n,"sanfew serum")) { return "Poison protection — shamans"; }
            if (oneOf(n,"super combat potion","ranging potion")) { return "Combat boosts — layout/prepot"; }
            if (oneOf(n,"anglerfish")) { return "Food — pre-raid overheal"; }
            if (oneOf(n,"salve amulet(ei)","salve amulet (ei)")) { return "Mystics — no Shadow setup"; }
        }
        else if (raid.equals("toa"))
        {
            if (oneOf(n,"saradomin brew")) { return "Brews — excludes Dehydration"; }
            if (oneOf(n,"super restore","prayer potion","sanfew serum")) { return "Prayer/restoration supplies"; }
            if (oneOf(n,"super combat potion","divine super combat potion","ranging potion","divine ranging potion")) { return "Combat boosts — path/prepot"; }
            if (oneOf(n,"antidote++")) { return "Poison protection — path dependent"; }
            if (oneOf(n,"anglerfish")) { return "Food — pre-raid (On a Diet restricts eating)"; }
            if (oneOf(n,"saturated heart","imbued heart")) { return "Magic boost — pre-raid"; }
            if (oneOf(n,"keris partisan of the sun")) { return "Healing utility — needs special energy"; }
        }
        if (oneOf(n,"rune pouch","divine rune pouch","book of the dead")) { return "Spell supplies — choose spellbook"; }
        if (oneOf(n,"air rune","cosmic rune","blood rune")) { return "Thrall runes — Arceuus only"; }
        return null;
    }
    private static boolean oneOf(String name,String... names) { return Arrays.asList(names).contains(name); }
}
