package com.windowmarkers;

import java.util.*;

/** Search the full bundled catalogue; membership remains distinct from activity readiness. */
final class BankCatalogue
{
    enum Kind { CATEGORY, PRESET, WIKI_SETUP, CUSTOM_PRESET, SAVED_GROUP }
    static final class Choice
    {
        final Kind kind;
        final String name, search;
        final Object value;
        Choice(Kind kind,String name,Object value)
        {
            this.kind=kind; this.name=name; this.value=value;
            search=SearchText.normalize(name+" "+aliases(name));
        }
        @Override public String toString() { return name+"  · "+kind.toString().toLowerCase(Locale.ROOT).replace('_',' '); }
    }
    static String aliases(String name)
    {
        return name.contains("Chambers of Xeric") ? "cox" : name.contains("Theatre of Blood") ? "tob"
            : name.contains("Tombs of Amascut") ? "toa" : name.contains("The Fractured Archive") ? "tfa" : "";
    }
    static List<Choice> choices(List<AutoCategories.Item> owned,List<Marker> markers,CustomPresets presets)
    {
        Set<String> categories=new TreeSet<>(WikiCatalogue.groups());
        categories.addAll(ItemTaxonomy.groups()); categories.addAll(RaidRecommendations.groups());
        categories.addAll(Arrays.asList("Hybrid equipment","Magic equipment","Ranged equipment","Melee equipment","Equipment",
            "Defensive or utility equipment — review","Health-lowering utility","Restricted-use supplies — blighted",
            "Specialist food — purple sweets","Runes","Food — general, not activity advice","Potions — general, not activity advice",
            "Drinks and brews — review","Resources","Other — review","Other — equipment role needs review","Raid mega-rare weapons"));
        // All preset and setup names are indexed independently of ownership. Only
        // broad groups need the current bank to discover state-specific labels.
        categories.addAll(AutoCategories.suggest(AutoCategories.Preset.ALL_CATEGORIES,owned).keySet());
        List<Choice> result=new ArrayList<>();
        for(String category:categories) result.add(new Choice(Kind.CATEGORY,category,category));
        for(AutoCategories.Preset preset:AutoCategories.Preset.values()) result.add(new Choice(Kind.PRESET,preset.toString(),preset));
        for(ActivitySetups.Setup setup:ActivitySetups.all()) result.add(new Choice(Kind.WIKI_SETUP,setup.activity+" — "+setup.label,setup));
        for(CustomPresets.Preset preset:presets.presets) result.add(new Choice(Kind.CUSTOM_PRESET,preset.name,preset));
        for(Marker marker:markers) result.add(new Choice(Kind.SAVED_GROUP,marker.labelText().isEmpty()?"Unnamed group":marker.labelText(),marker));
        return result;
    }
    static List<Choice> search(List<Choice> choices,String query,int limit)
    {
        String needle=SearchText.normalize(query);
        Map<Choice,Integer> scores=new IdentityHashMap<>();
        for(Choice choice:choices)
        {
            int score=SearchText.normalizedScore(choice.search,needle);
            if(score!=Integer.MAX_VALUE) scores.put(choice,score);
        }
        List<Choice> result=new ArrayList<>(scores.keySet());
        result.sort(Comparator.comparingInt((Choice c)->scores.get(c)).thenComparing(c->c.name).thenComparing(c->c.kind));
        return new ArrayList<>(result.subList(0,Math.min(limit,result.size())));
    }
    static Map<String,List<AutoCategories.Item>> categoryGroups(String category,List<AutoCategories.Item> items)
    {
        Map<String,List<AutoCategories.Item>> groups=new LinkedHashMap<>();
        Set<Integer> found=new HashSet<>();
        for(AutoCategories.Item item:items)
            if(!ItemVariantState.identityNeedsReview(item) && WikiCatalogue.entries(item).stream().anyMatch(e -> e.group.equals(category)) && found.add(item.id))
                groups.computeIfAbsent(ItemVariantState.annotate(category,item),k -> new ArrayList<>()).add(item);
        List<AutoCategories.Preset> matching=new ArrayList<>();
        if(category.startsWith("CoX ")) matching.add(AutoCategories.Preset.CHAMBERS_OF_XERIC);
        else if(category.startsWith("ToB ")) matching.add(AutoCategories.Preset.THEATRE_OF_BLOOD);
        else if(category.startsWith("ToA ")) matching.add(AutoCategories.Preset.TOMBS_OF_AMASCUT);
        else if(category.startsWith("TFA ")) matching.add(AutoCategories.Preset.FRACTURED_ARCHIVE);
        else matching.addAll(Arrays.asList(AutoCategories.Preset.ALL_CATEGORIES,AutoCategories.Preset.COMBAT_GROUPS,AutoCategories.Preset.RAID_MEGA_RARES));
        for(AutoCategories.Preset preset:matching)
            for(AutoCategories.Item item:AutoCategories.suggest(preset,items).getOrDefault(category,Collections.emptyList()))
                // Suggested labels already contain their state or review note.
                if(found.add(item.id)) groups.computeIfAbsent(category,k -> new ArrayList<>()).add(item);
        return groups;
    }
    static SortedSet<String> memberships(AutoCategories.Item item,List<AutoCategories.Item> owned,List<Marker> markers,CustomPresets custom)
    {
        SortedSet<String> result=new TreeSet<>();
        if(ItemVariantState.identityNeedsReview(item)) result.add("Review: "+ItemVariantState.reviewGroup(item));
        else for(WikiCatalogue.Entry entry:WikiCatalogue.entries(item)) result.add("Category: "+ItemVariantState.annotate(entry.group,item));
        // Only fellow set pieces affect this item's group. Evaluating the entire
        // bank once per preset would repeatedly classify hundreds of unrelated IDs.
        String set=ItemTaxonomy.equipmentSet(ItemVariantState.familyName(item));
        List<AutoCategories.Item> context=new ArrayList<>();
        if(set!=null) for(AutoCategories.Item candidate:owned)
            if(set.equals(ItemTaxonomy.equipmentSet(ItemVariantState.familyName(candidate)))) context.add(candidate);
        if(context.stream().noneMatch(i -> i.id==item.id)) context.add(item);
        for(AutoCategories.Preset preset:AutoCategories.Preset.values())
            AutoCategories.suggest(preset,context).forEach((group,items) -> {
                if(items.stream().anyMatch(i -> i.id==item.id)) result.add("Preset: "+preset+" / "+group);
            });
        for(ActivitySetups.Setup setup:ActivitySetups.all()) if(setup.itemIds.contains(item.id))
            result.add("Wiki setup: "+setup.activity+" / "+setup.label+(ItemVariantState.needsActivityReview(item)?" — state needs review":""));
        for(Marker marker:markers) if(marker.linked() && Arrays.stream(marker.linkedItemIds).anyMatch(id -> id==item.id))
            result.add("Saved group: "+marker.labelText());
        for(CustomPresets.Preset preset:custom.presets) for(Marker group:preset.groups)
            if(group.linked() && Arrays.stream(group.linkedItemIds).anyMatch(id -> id==item.id)) result.add("Custom preset: "+preset.name+" / "+group.labelText());
        return result;
    }
    static String compare(String left,Set<String> a,String right,Set<String> b)
    {
        Set<String> shared=new TreeSet<>(a); shared.retainAll(b);
        Set<String> onlyA=new TreeSet<>(a); onlyA.removeAll(b);
        Set<String> onlyB=new TreeSet<>(b); onlyB.removeAll(a);
        return section("Shared",shared)+section("Only "+left,onlyA)+section("Only "+right,onlyB);
    }
    private static String section(String name,Set<String> entries)
    { return name+" ("+entries.size()+")\n"+(entries.isEmpty()?"None":String.join("\n",entries))+"\n\n"; }
}
