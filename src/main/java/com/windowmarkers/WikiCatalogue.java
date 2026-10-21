package com.windowmarkers;

import java.util.*;

/** Bundled, audited Wiki item identities and useful roles. Does not imply activity eligibility. */
final class WikiCatalogue
{
    static final class Entry
    {
        final String facet, group;
        Entry(String facet,String group) { this.facet=facet; this.group=group; }
    }
    private static final Map<String,List<Entry>> INDEX=new HashMap<>();
    static
    {
        for (String[] row : ItemTaxonomy.readRows("item-catalogue.tsv",3))
        { INDEX.computeIfAbsent(row[0],k -> new ArrayList<>()).add(new Entry(row[1],row[2])); }
    }
    static List<Entry> entries(AutoCategories.Item item)
    {
        List<Entry> result=INDEX.get("I:"+item.id);
        if (result==null) { result=INDEX.get("N:"+item.name); }
        if (result==null) { result=INDEX.get("N:"+ItemTaxonomy.baseName(item.name)); }
        if (result==null && item.name.matches(".*\\s*\\([1-8]\\)$"))
        {
            List<Entry> charged=INDEX.get("N:"+item.name.replaceFirst("\\s*\\([1-8]\\)$",""));
            if (charged!=null && charged.stream().anyMatch(e -> e.facet.equals("travel"))) { result=charged; }
        }
        return result==null ? Collections.emptyList() : result;
    }
    static boolean hasFacet(AutoCategories.Item item,String facet)
    { return entries(item).stream().anyMatch(e -> e.facet.equals(facet)); }

    static String category(AutoCategories.Item item,AutoCategories.Preset preset)
    {
        if (preset==AutoCategories.Preset.FOOD_AND_POTIONS && !hasFacet(item,"supply") && !hasFacet(item,"restricted") && !item.action("Eat") && !item.action("Drink")) { return null; }
        Entry best=null;
        for (Entry entry : entries(item))
        {
            if (!accepts(preset,entry)) { continue; }
            if (best==null || priority(entry)<priority(best) || (priority(entry)==priority(best) && entry.group.compareTo(best.group)<0)) { best=entry; }
        }
        return best==null ? null : best.group;
    }
    private static int priority(Entry e)
    {
        switch (e.facet)
        {
            case "outfit": return 0;
            case "restricted": return 1;
            case "boost": return 2;
            case "skilling": return e.group.contains("materials") || e.group.contains("ingredients") || e.group.contains("products") ? 9 : 3;
            case "travel": return 4;
            case "combat": return e.group.startsWith("Prayer") ? 7 : 5;
            case "supply": return 6;
            case "utility": return 8;
            case "quest": return 10;
            case "clue": return 11;
            default: return 12;
        }
    }
    private static boolean accepts(AutoCategories.Preset p,Entry e)
    {
        switch (p)
        {
            case ALL_CATEGORIES: case BROAD_TYPES: return true;
            case SKILLING: return e.facet.equals("outfit") || e.facet.equals("skilling") || (e.facet.equals("boost") && !e.group.startsWith("Combat"));
            case SKILLING_OUTFITS: return e.facet.equals("outfit");
            case COMBAT_GROUPS: return e.facet.equals("combat") || (e.facet.equals("boost") && e.group.startsWith("Combat"));
            case FOOD_AND_POTIONS: return e.facet.equals("supply") || e.facet.equals("boost") || e.facet.equals("restricted");
            case TRAVEL_AND_UTILITY: return e.facet.equals("travel") || e.facet.equals("utility");
            case QUESTS_AND_CLUES: return e.facet.equals("quest") || e.facet.equals("clue");
            case COSMETICS: return e.facet.equals("cosmetic");
            default: return false;
        }
    }
    static String source(AutoCategories.Preset preset)
    {
        switch (preset)
        {
            case SKILLING: case SKILLING_OUTFITS: return "https://oldschool.runescape.wiki/w/Skilling_equipment";
            case FOOD_AND_POTIONS: return "https://oldschool.runescape.wiki/w/Potions";
            case TRAVEL_AND_UTILITY: return "https://oldschool.runescape.wiki/w/Container_items";
            case QUESTS_AND_CLUES: return "https://oldschool.runescape.wiki/w/Quest_items";
            case COSMETICS: return "https://oldschool.runescape.wiki/w/Clothes";
            case ALL_CATEGORIES: case BROAD_TYPES: case COMBAT_GROUPS: return "https://oldschool.runescape.wiki/w/Equipment";
            default: return RaidRecommendations.source(preset);
        }
    }
}
