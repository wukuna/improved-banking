package com.windowmarkers;

import java.util.*;

/** Immutable bank-only search documents, built off the client thread. */
final class BankSearchIndex
{
    private final Map<String,Set<Integer>> documents=new LinkedHashMap<>();
    BankSearchIndex(List<AutoCategories.Item> owned,List<Marker> markers,CustomPresets presets)
    {
        Set<Integer> ids=new HashSet<>();
        for(AutoCategories.Item item:owned) {
            ids.add(item.id); add(item.name,item.id);
            if(!ItemVariantState.identityNeedsReview(item)) for(WikiCatalogue.Entry entry:WikiCatalogue.entries(item))
                add(ItemVariantState.annotate(entry.group,item),item.id);
        }
        for(AutoCategories.Preset preset:AutoCategories.Preset.values())
            AutoCategories.suggest(preset,owned).forEach((name,items) -> {
                if(name.startsWith("Other")) return;
                for(AutoCategories.Item item:items) { add(name,item.id); add(preset.toString(),item.id); }
            });
        for(ActivitySetups.Setup setup:ActivitySetups.all()) for(int id:setup.itemIds)
            if(ids.contains(id)) add(setup.activity+" — "+setup.label,id);
        for(Marker marker:markers) if(marker.linked()) for(int id:marker.linkedItemIds)
            if(ids.contains(id)) add(marker.labelText(),id);
        for(CustomPresets.Preset preset:presets.presets) for(Marker marker:preset.groups)
            if(marker.linked()) for(int id:marker.linkedItemIds) if(ids.contains(id)) { add(preset.name,id); add(marker.labelText(),id); }
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
