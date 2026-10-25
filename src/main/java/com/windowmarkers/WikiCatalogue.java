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
    private static final Map<String,List<Entry>> CATEGORY_SUPPLEMENT=new HashMap<>();
    private static final Map<String,List<Entry>> ROLE_EXCLUSIONS=new HashMap<>();
    private static final Set<Integer> AMBIGUOUS_NAMES=new HashSet<>();
    private static final Set<Integer> EXCLUDED_IDENTITIES=new HashSet<>();
    static
    {
        for (String[] row : ItemTaxonomy.readRows("item-catalogue.tsv",3))
        { INDEX.computeIfAbsent(row[0],k -> new ArrayList<>()).add(new Entry(row[1],row[2])); }
        for (String[] row:ItemTaxonomy.readRows("ambiguous-item-names.tsv",1))
        { AMBIGUOUS_NAMES.add(Integer.parseInt(row[0])); }
        for (String[] row:ItemTaxonomy.readRows("catalogue-identity-exclusions.tsv",1))
        { EXCLUDED_IDENTITIES.add(Integer.parseInt(row[0])); }
        // Exact-ID Wiki category evidence supplements organizational roles only.
        // Raid recommendations and ranked alternatives use their own reviewed resources.
        for (String resource : new String[]{"wiki-category-supplement.tsv","combat-equivalence-categories.tsv","wiki-recipe-roles.tsv","wiki-role-updates.tsv"})
        for (String[] row : ItemTaxonomy.readRows(resource,3))
        {
            if (!row[0].matches("I:[0-9]+")) { throw new IllegalStateException("Wiki category requires an exact item ID"); }
            List<Entry> entries=CATEGORY_SUPPLEMENT.computeIfAbsent(row[0],k -> new ArrayList<>());
            if (entries.stream().noneMatch(e -> e.facet.equals(row[1]) && e.group.equals(row[2])))
            { entries.add(new Entry(row[1],row[2])); }
        }
        for (String[] row : ItemTaxonomy.readRows("catalogue-role-exclusions.tsv",3))
        {
            if (!row[0].matches("I:[0-9]+")) { throw new IllegalStateException("Role correction requires an exact item ID"); }
            ROLE_EXCLUSIONS.computeIfAbsent(row[0],k -> new ArrayList<>()).add(new Entry(row[1],row[2]));
        }
    }
    static List<Entry> entries(AutoCategories.Item item)
    {
        if (EXCLUDED_IDENTITIES.contains(item.id)) { return Collections.emptyList(); }
        List<Entry> result=INDEX.get("I:"+item.id);
        boolean nameSafe=!AMBIGUOUS_NAMES.contains(item.id);
        if (result==null && nameSafe) { result=INDEX.get("N:"+item.name); }
        if (result==null && nameSafe) { result=INDEX.get("N:"+ItemTaxonomy.baseName(item.name)); }
        if (result==null && nameSafe && item.name.matches(".*\\s*\\([1-8]\\)$"))
        {
            List<Entry> charged=INDEX.get("N:"+item.name.replaceFirst("\\s*\\([1-8]\\)$",""));
            if (charged!=null && charged.stream().anyMatch(e -> e.facet.equals("travel"))) { result=charged; }
        }
        List<Entry> supplement=CATEGORY_SUPPLEMENT.get("I:"+item.id);
        List<Entry> exclusions=ROLE_EXCLUSIONS.get("I:"+item.id);
        if (supplement==null && exclusions==null) { return result==null ? Collections.emptyList() : result; }
        // Preserve reviewed name aliases when adding an exact-ID category record.
        List<Entry> combined=result==null ? new ArrayList<>() : new ArrayList<>(result);
        if (supplement!=null) for (Entry entry:supplement)
        {
            if (combined.stream().noneMatch(e -> e.facet.equals(entry.facet) && e.group.equals(entry.group)))
            { combined.add(entry); }
        }
        // Correct only the reviewed identity and role, including a name fallback
        // from a different item with the same display name. Shared indexes stay intact.
        if (exclusions!=null)
        { combined.removeIf(e -> exclusions.stream().anyMatch(x -> x.facet.equals(e.facet) && x.group.equals(e.group))); }
        return combined;
    }
    static boolean hasFacet(AutoCategories.Item item,String facet)
    { return entries(item).stream().anyMatch(e -> e.facet.equals(facet)); }

    static Set<String> groups()
    {
        Set<String> groups=new TreeSet<>();
        for(Map<String,List<Entry>> index:Arrays.asList(INDEX,CATEGORY_SUPPLEMENT))
            for(List<Entry> entries:index.values()) for(Entry entry:entries) groups.add(entry.group);
        return groups;
    }

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
            case "skilling": return e.group.contains("materials") || e.group.contains("ingredients") || e.group.contains("products")
                || e.group.endsWith("recipe inputs") || e.group.endsWith("guild access") || e.group.endsWith("pouch protection") || e.group.endsWith("warm clothing") ? 9 : 3;
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
