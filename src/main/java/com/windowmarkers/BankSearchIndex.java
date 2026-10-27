package com.windowmarkers;

import java.util.*;

/** Immutable bank-only search documents, built off the client thread. */
final class BankSearchIndex
{
    private final Map<String,Set<Integer>> documents=new LinkedHashMap<>();
    private final Map<String,Set<Integer>> customDocuments=new LinkedHashMap<>();
    private final ItemCatalogueIndex catalogue;
    private final int hitpoints;
    private final BankCleanup cleanup;
    private final Map<String,Map<Integer,ItemCatalogueIndex.Rank>> markerRanks=new HashMap<>();
    private List<String> lastKeys=Collections.emptyList();
    private List<int[]> lastMembers=Collections.emptyList();
    private Map<Integer,ItemCatalogueIndex.Rank> lastRanks=Collections.emptyMap();
    private final Set<String> activities=new HashSet<>(),ordinaryNames=new HashSet<>();
    BankSearchIndex(List<AutoCategories.Item> owned,List<Marker> markers,CustomPresets presets)
    { this(owned,markers,presets,99); }
    BankSearchIndex(List<AutoCategories.Item> owned,List<Marker> markers,CustomPresets presets,int hitpoints)
    { this(owned,markers,presets,hitpoints,Collections.emptySet(),BankCleanup.Progress.unknown()); }
    BankSearchIndex(List<AutoCategories.Item> owned,List<Marker> markers,CustomPresets presets,int hitpoints,Set<Integer> tradeable,BankCleanup.Progress progress)
    {
        this.hitpoints=Math.max(1,Math.min(99,hitpoints));
        cleanup=new BankCleanup(owned,tradeable,progress);
        List<ItemCatalogueIndex.Item> facts=new ArrayList<>();
        for(AutoCategories.Item item:owned) facts.add(new ItemCatalogueIndex.Item(item,item.name,
            item.slot==-2?ItemCatalogueIndex.fallbackSlot(item.id,item.name):item.slot));
        catalogue=new ItemCatalogueIndex(facts,false);
        Set<Integer> ids=new HashSet<>();
        for(AutoCategories.Item item:owned) {
            ids.add(item.id); add(item.name,item.id); ordinaryNames.add(SearchText.normalize(item.name));
            if(!ItemVariantState.identityNeedsReview(item)) for(WikiCatalogue.Entry entry:WikiCatalogue.entries(item))
                add(ItemVariantState.annotate(entry.group,item),item.id);
        }
        for(AutoCategories.Preset preset:AutoCategories.Preset.values()) {
            if(SetupItemLayout.activityPreset(preset)) activity(preset.toString());
            for(int id:catalogue.presetIds(preset)) add(preset.toString(),id);
        }
        catalogue.categoryDocuments().forEach((name,items)-> { for(int id:items) add(name,id); });
        for(ActivitySetups.Setup setup:ActivitySetups.all()) {
            activity(setup.activity); activity(setup.activity+" — "+setup.label);
            for(int id:setup.itemIds) if(ids.contains(id)) add(setup.activity+" — "+setup.label,id);
        }
        for(Marker marker:markers) if(marker.linked()) for(int id:marker.linkedItemIds)
            if(ids.contains(id)) addCustom(marker.labelText(),id);
        for(CustomPresets.Preset preset:presets.presets) for(Marker marker:preset.groups)
            if(marker.linked()) for(int id:marker.linkedItemIds) if(ids.contains(id)) { addCustom(preset.name,id); addCustom(marker.labelText(),id); }
        for(CustomPresets.Preset preset:presets.presets) if(preset.groupBySlot) activity(preset.name);
        ordinaryNames.addAll(Arrays.asList("food","potions","weapons","armour","armor","equipment","resources","tools","capes","head","farming","fishing","mining","woodcutting"));
        for(net.runelite.api.Skill skill:net.runelite.api.Skill.values()) ordinaryNames.add(SearchText.normalize(skill.getName()));
        for(Marker marker:markers) indexRank(marker);
        for(CustomPresets.Preset preset:presets.presets) for(Marker marker:preset.groups) indexRank(marker);
    }
    private void addCustom(String text,int id) {
        add(text,id); customDocuments.computeIfAbsent(SearchText.normalize(text),k->new HashSet<>()).add(id);
    }
    private void indexRank(Marker marker) {
        if(marker.guideQuery!=null && !marker.guideQuery.isEmpty()) markerRanks.computeIfAbsent(marker.rankKey(),
            key->BankCleanup.isQuery(marker.guideQuery)?cleanup.search(marker.guideQuery).ranks:catalogue.guideRanks(marker.guideQuery,marker.guideContext==null?"":marker.guideContext,hitpoints));
    }
    Map<Integer,ItemCatalogueIndex.Rank> ranks(List<Marker> groups) {
        boolean same=groups.size()==lastKeys.size();
        for(int i=0;same && i<groups.size();i++) same=groups.get(i).rankKey().equals(lastKeys.get(i)) && groups.get(i).linkedItemIds==lastMembers.get(i);
        if(same) return lastRanks;
        Map<Integer,ItemCatalogueIndex.Rank> result=new HashMap<>();
        for(Marker marker:groups) if(marker.linked()) {
            // Cleanup maps are already built from this snapshot; new preview groups need their reasons too.
            if(BankCleanup.isQuery(marker.guideQuery)) indexRank(marker);
            Map<Integer,ItemCatalogueIndex.Rank> ranks=markerRanks.getOrDefault(marker.rankKey(),Collections.emptyMap());
            for(int id:marker.linkedItemIds) if(ranks.containsKey(id)) result.merge(id,ranks.get(id),TierPresentation::merge);
        }
        lastKeys=new ArrayList<>();lastMembers=new ArrayList<>();
        for(Marker group:groups) { lastKeys.add(group.rankKey());lastMembers.add(group.linkedItemIds); }
        lastRanks=Collections.unmodifiableMap(result);
        return lastRanks;
    }
    static final class Match {
        final Set<Integer> ids; final Map<Integer,ItemCatalogueIndex.Rank> ranks; final boolean slots;
        final SetupLoadout loadout;
        Match(Set<Integer> ids,Map<Integer,ItemCatalogueIndex.Rank> ranks,boolean slots) { this(ids,ranks,slots,null); }
        Match(Set<Integer> ids,Map<Integer,ItemCatalogueIndex.Rank> ranks,boolean slots,SetupLoadout loadout) { this.ids=ids;this.ranks=ranks;this.slots=slots;this.loadout=loadout; }
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
    { return search(query,"").ids; }
    Match search(String query,String context)
    { return search(query,context,""); }
    Match search(String query,String context,String inventoryContext)
    { return search(query,context,inventoryContext,hitpoints); }
    Match search(String query,String context,String inventoryContext,int hitpoints)
    {
        Set<Integer> result=new HashSet<>();
        if(!plainQuery(query)) return new Match(result,Collections.emptyMap(),false);
        Match review=cleanup.search(query);if(review!=null) return review;
        String normalized=SearchText.normalize(query);
        ItemCatalogueIndex.Result common=catalogue.find(query,context,inventoryContext,hitpoints);
        if(common!=null) for(ItemCatalogueIndex.Item item:common.items) result.add(item.id);
        // Purpose/threshold queries have exact membership. Saved groups with a
        // matching name must not reintroduce raw food or incompatible ammo.
        if(common!=null && SupplySearch.parse(query)!=null) return new Match(result,common.ranks,false);
        Map<String,Set<Integer>> extra=common==null?documents:customDocuments;
        boolean exact=extra.keySet().stream().anyMatch(t->t.contains(normalized));
        for(Map.Entry<String,Set<Integer>> entry:extra.entrySet())
            if(entry.getKey().contains(normalized) || (!exact && normalized.length()>=3 && SearchText.normalizedScore(entry.getKey(),normalized)!=Integer.MAX_VALUE))
                result.addAll(entry.getValue());
        SearchQuery qualifiers=SearchQuery.parse(query);
        if(common!=null && !qualifiers.tiers.isEmpty()) result.removeIf(id->!qualifiers.accepts(common.ranks.get(id)));
        return new Match(result,common==null?Collections.emptyMap():common.ranks,common!=null && common.activity || groupBySlot(query),common==null?null:common.loadout);
    }
}
