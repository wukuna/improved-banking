package com.windowmarkers;

import java.util.*;

/** Shopping categories deliberately exclude the rest of an activity's example inventory. */
final class GeCategorySearch
{
    private final Map<String,List<ItemCatalogueIndex.Item>> categories=new LinkedHashMap<>();
    private static final List<String> SKILLS=Arrays.asList("farming","mining","woodcutting","fishing","hunter","crafting","smithing","fletching","construction","herblore","cooking","firemaking","runecraft","agility","thieving");
    GeCategorySearch(Collection<ItemCatalogueIndex.Item> items)
    {
        Set<String> trees=new HashSet<>();
        for(ItemCatalogueIndex.Item item:items) if(group(item,"Farming — saplings")) trees.add(treeName(item.name));
        for(ItemCatalogueIndex.Item item:items) {
            if(ItemVariantState.identityNeedsReview(item.fact)) continue;
            List<WikiCatalogue.Entry> entries=WikiCatalogue.entries(item.fact);
            if(entries.isEmpty()) continue;
            boolean tools=tool(item,entries);
            if(tools) add("tools",item);
            for(String skill:SKILLS) {
                boolean belongs=entries.stream().anyMatch(e->e.group.toLowerCase(Locale.ROOT).startsWith(skill+" — ") && !e.group.endsWith("recipe inputs"));
                if(belongs && tools) add(skill+" tools",item);
                if(!belongs) continue;
                if(entries.stream().anyMatch(e->e.group.toLowerCase(Locale.ROOT).startsWith(skill+" — ") && e.group.endsWith("boosts"))) add(skill+" boosts",item);
                if(entries.stream().anyMatch(e->e.group.toLowerCase(Locale.ROOT).startsWith(skill+" — ") && e.group.endsWith("materials"))) add(skill+" materials",item);
                // The generic skill means direct skill supplies/equipment, not its guide's teleports, food or energy potions.
                if(entries.stream().anyMatch(e->e.group.toLowerCase(Locale.ROOT).startsWith(skill+" — ")
                    && !e.group.endsWith("recipe inputs") && (e.facet.equals("skilling") || e.facet.equals("outfit")))) add(skill,item);
            }
            if(group(item,"Farming — seeds and planting materials") && (item.search.matches(".* seeds?") || item.search.equals("acorn"))) add("farming seeds",item);
            if(group(item,"Farming — saplings")) add("farming saplings",item);
            if(group(item,"Farming — saplings") || group(item,"Farming — seeds and planting materials")
                && (item.search.equals("acorn") || item.search.matches(".* seeds?") && trees.contains(treeName(item.name)))) add("farming trees",item);
            if(entries.stream().anyMatch(e->e.group.startsWith("Farming — ")) && item.search.contains("compost")) add("farming compost",item);
        }
        for(String key:new ArrayList<>(categories.keySet())) categories.get(key).sort(Comparator.comparing(i->i.name,String.CASE_INSENSITIVE_ORDER));
    }
    private static boolean group(ItemCatalogueIndex.Item item,String name)
    { return !ItemVariantState.identityNeedsReview(item.fact) && WikiCatalogue.entries(item.fact).stream().anyMatch(e->e.group.equals(name)); }
    private static String treeName(String name)
    { return SearchText.normalize(name).replaceFirst(" (sapling|seeds?)$","").replaceFirst(" tree$",""); }
    private static boolean tool(ItemCatalogueIndex.Item item,List<WikiCatalogue.Entry> entries)
    {
        if(item.fact.action("Drink") || entries.stream().anyMatch(e->e.group.startsWith("Potions"))) return false;
        if("Tools".equals(ItemTaxonomy.weaponRole(ItemVariantState.familyName(item.fact)))) return true;
        return entries.stream().anyMatch(e->e.facet.equals("skilling") && (e.group.endsWith(" — tools") || e.group.endsWith(" — axes") || e.group.endsWith(" — pickaxes")));
    }
    private void add(String key,ItemCatalogueIndex.Item item)
    { categories.computeIfAbsent(key,k->new ArrayList<>()).add(item); }
    ItemCatalogueIndex.Result find(String input)
    {
        if(input==null || input.length()>100 || !BankSearchIndex.plainQuery(input)) return null;
        String key=SearchText.normalize(input).replaceFirst(" tool$"," tools").replaceFirst(" seed$"," seeds").replaceFirst(" tree$"," trees").replaceFirst(" sapling$"," saplings");
        if(!categories.containsKey(key) && !key.equals("tools") && !key.startsWith("farming ") && !SKILLS.contains(key)) return null;
        if(!categories.containsKey(key) && !Arrays.asList("farming tools","farming seeds","farming saplings","farming trees","farming compost").contains(key) && !SKILLS.contains(key) && !key.equals("tools")) return null;
        List<ItemCatalogueIndex.Item> selected=categories.getOrDefault(key,Collections.emptyList());
        Map<Integer,ItemCatalogueIndex.Rank> notes=new HashMap<>();
        for(ItemCatalogueIndex.Item item:selected) notes.put(item.id,new ItemCatalogueIndex.Rank(0,"Direct "+key+" classification. This is not a complete activity loadout."));
        return new ItemCatalogueIndex.Result(new ArrayList<>(selected),notes,false);
    }
    List<String> suggestions(String input,boolean activity)
    {
        String key=SearchText.normalize(input);
        if(key.equals("farming")) return available("farming seeds","farming trees","farming tools","farming compost");
        if(key.equals("tools")) return available("farming tools","mining tools","woodcutting tools","fishing tools");
        if(SKILLS.contains(key)) return available(key+" tools",key+" materials",key+" boosts");
        if(key.equals("food")) return Arrays.asList("food 20","fast foods","combo food","raw food");
        if(key.equals("potions")) return Arrays.asList("prayer potions","stamina potions","antifire potions","healing potions");
        if(activity) {
            if(Arrays.stream(key.split(" +")).anyMatch(word->ItemCatalogueIndex.slot(word)>=0)) return Arrays.asList(key+" owned",key+" unowned",key+" next upgrade");
            return Arrays.asList(key+" head",key+" top",key+" owned",key+" next upgrade");
        }
        return Collections.emptyList();
    }
    private List<String> available(String... candidates)
    {
        List<String> result=new ArrayList<>();
        for(String candidate:candidates) if(categories.containsKey(candidate)) result.add(candidate);
        return result;
    }
}
