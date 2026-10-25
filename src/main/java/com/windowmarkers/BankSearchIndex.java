package com.windowmarkers;

import java.util.*;

/** Immutable bank-only search documents, built off the client thread. */
final class BankSearchIndex
{
    private final Map<String,Set<Integer>> documents=new LinkedHashMap<>();
    private final Set<String> activities=new HashSet<>(),ordinaryNames=new HashSet<>();
    BankSearchIndex(List<AutoCategories.Item> owned,List<Marker> markers,CustomPresets presets)
    {
        Set<Integer> ids=new HashSet<>();
        for(AutoCategories.Item item:owned) {
            ids.add(item.id); add(item.name,item.id); ordinaryNames.add(SearchText.normalize(item.name));
            if(!ItemVariantState.identityNeedsReview(item)) for(WikiCatalogue.Entry entry:WikiCatalogue.entries(item))
                add(ItemVariantState.annotate(entry.group,item),item.id);
        }
        for(AutoCategories.Preset preset:AutoCategories.Preset.values()) {
            if(SetupItemLayout.activityPreset(preset)) activity(preset.toString());
            AutoCategories.suggest(preset,owned).forEach((name,items) -> {
                if(name.startsWith("Other")) return;
                for(AutoCategories.Item item:items) { add(name,item.id); add(preset.toString(),item.id); }
            });
        }
        for(ActivitySetups.Setup setup:ActivitySetups.all()) {
            activity(setup.activity); activity(setup.activity+" — "+setup.label);
            for(int id:setup.itemIds) if(ids.contains(id)) add(setup.activity+" — "+setup.label,id);
        }
        for(Marker marker:markers) if(marker.linked()) for(int id:marker.linkedItemIds)
            if(ids.contains(id)) add(marker.labelText(),id);
        for(CustomPresets.Preset preset:presets.presets) for(Marker marker:preset.groups)
            if(marker.linked()) for(int id:marker.linkedItemIds) if(ids.contains(id)) { add(preset.name,id); add(marker.labelText(),id); }
        for(CustomPresets.Preset preset:presets.presets) if(preset.groupBySlot) activity(preset.name);
        ordinaryNames.addAll(Arrays.asList("food","potions","weapons","armour","armor","equipment","resources","tools","capes","head","farming","fishing","mining","woodcutting"));
        for(net.runelite.api.Skill skill:net.runelite.api.Skill.values()) ordinaryNames.add(SearchText.normalize(skill.getName()));
    }
    private void activity(String name) {
        activities.add(SearchText.normalize(name));
        String alias=BankCatalogue.aliases(name); if(!alias.isEmpty()) activities.add(alias);
    }
    boolean groupBySlot(String query) {
        String normalized=SearchText.normalize(query);
        if(normalized.length()<3 || ordinaryNames.contains(normalized) || !plainQuery(query)) return false;
        // A general category or individual item must not inherit layout merely
        // because it also occurs in a boss setup's long document.
        return activities.stream().anyMatch(name -> name.equals(normalized) || name.startsWith(normalized+" "));
    }
    private void add(String text,int id)
    {
        String key=SearchText.normalize(text+" "+BankCatalogue.aliases(text));
        if(!key.isEmpty()) documents.computeIfAbsent(key,k -> new HashSet<>()).add(id);
    }
    static boolean plainQuery(String query)
    { return query!=null && !query.trim().isEmpty() && !query.matches(".*[:<>=!|&].*"); }
    Set<Integer> find(String query)
    {
        Set<Integer> result=new HashSet<>();
        if(!plainQuery(query)) return result;
        String normalized=SearchText.normalize(query);
        if(normalized.isEmpty()) return result;
        for(Map.Entry<String,Set<Integer>> entry:documents.entrySet())
            if(entry.getKey().contains(normalized) || (normalized.length()>=3 && SearchText.normalizedScore(entry.getKey(),normalized)!=Integer.MAX_VALUE))
                result.addAll(entry.getValue());
        return result;
    }
}
