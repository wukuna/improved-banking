package com.windowmarkers;

import java.util.*;

/** Per-guide column comparisons. Never treats an item's market price as its strength. */
final class GeActivityFilter
{
    enum Mode
    {
        ALL("All items"), OWNED("Owned"), UNOWNED("Not owned"), NEXT_UPGRADE("Next guide upgrade");
        private final String label;
        Mode(String label) { this.label=label; }
        @Override public String toString() { return label; }
    }
    static final class Query
    {
        final String text;final Mode mode;
        Query(String text,Mode mode) { this.text=text;this.mode=mode; }
        static Query parse(String input,Mode fallback)
        {
            String clean=SearchText.normalize(input);
            String[][] suffixes={{" next upgrades","NEXT_UPGRADE"},{" next upgrade","NEXT_UPGRADE"},{" upgrades","NEXT_UPGRADE"},
                {" not owned","UNOWNED"},{" unowned","UNOWNED"},{" owned","OWNED"}};
            for(String[] suffix:suffixes) if(clean.endsWith(suffix[0])) return new Query(clean.substring(0,clean.length()-suffix[0].length()),Mode.valueOf(suffix[1]));
            return new Query(input,fallback==null?Mode.ALL:fallback);
        }
    }
    private static final class Choice
    {
        final String setup,slot,note;final int tier;
        Choice(String[] row) { setup=row[0];slot=row[2];tier=Integer.parseInt(row[3]);note=row[4]; }
        int slotId() { return slot.equals("2h") || slot.equals("special")?3:ItemCatalogueIndex.slot(slot); }
    }
    private static final Map<Integer,List<Choice>> RANKS=new HashMap<>();
    static { for(String[] row:ItemTaxonomy.readRows("activity-setup-ranks.tsv",5)) RANKS.computeIfAbsent(Integer.parseInt(row[1]),k->new ArrayList<>()).add(new Choice(row)); }
    static void prepare() { /* initialize bundled data on the catalogue worker */ }
    static final class Outcome
    {
        final ItemCatalogueIndex.Result result;final Map<Integer,String> upgrades;final String notice;
        Outcome(ItemCatalogueIndex.Result result,Map<Integer,String> upgrades,String notice) { this.result=result;this.upgrades=upgrades;this.notice=notice; }
    }
    private static String role(ItemCatalogueIndex.Item item,Choice choice)
    {
        if(choice.slot.equals("special") || choice.slot.equals("ammo")) return "";
        String name=ItemTaxonomy.baseName(CombatEquivalence.familyName(item.fact));
        if(item.slot==3) {
            if(Arrays.asList("abyssal whip","abyssal tentacle","blade of saeldor").contains(name)) return "slash weapon";
            if(Arrays.asList("warped sceptre","trident of the seas","trident of the swamp","sanguinesti staff").contains(name)) return "powered staff";
            if(Arrays.asList("rune crossbow","dragon crossbow","armadyl crossbow","zaryte crossbow").contains(name)) return "crossbow";
            return ""; // Dragon-hunter, spec, multi-target and other weapons have separate roles.
        }
        if(item.slot==12 || item.slot==5) return "";
        String set=ItemTaxonomy.equipmentSet(name);
        if(set!=null && !name.startsWith("ahrim's ") && !name.startsWith("karil's ") && !name.startsWith("torag's ")
            && !name.startsWith("verac's ") && !name.startsWith("guthan's ") && !name.startsWith("dharok's ")) return "";
        if(name.equals("serpentine helm") || name.contains("blood fury") || name.startsWith("mixed hide ") || name.startsWith("oathplate ")) return "";
        return choice.slot;
    }
    private static boolean ready(ItemCatalogueIndex.Item item,Choice choice)
    {
        ActivitySetups.Setup setup=ActivitySetups.byId(choice.setup);
        return setup!=null && item.slot>=0 && item.slot==choice.slotId() && choice.tier>0
            && !ItemVariantState.needsActivityReview(item.fact)
            && !ItemVariantState.needsRankReview(item.fact,setup.activity,setup.label)
            && SetupItemRequirements.reviewReason(choice.setup,item.fact).isEmpty();
    }
    private static String key(ItemCatalogueIndex.Item item,Choice choice)
    { String role=role(item,choice);return role.isEmpty()?"":choice.setup+":"+role; }
    static Outcome apply(ItemCatalogueIndex.Result input,GePlayerItems.Snapshot owned,GeRequirements requirements,Mode mode,boolean hideWorse)
    {
        if(!input.activity || mode==Mode.ALL && !hideWorse) return new Outcome(input,Collections.emptyMap(),"");
        if(!owned.complete) return new Outcome(input,Collections.emptyMap(),"Open your bank once this login. Ownership filters are not applied yet.");
        Set<String> setups=new HashSet<>();for(ActivitySetups.Setup setup:input.setups) setups.add(setup.id);
        Map<String,Integer> best=new HashMap<>();Map<String,String> bestNames=new HashMap<>();
        Map<String,Integer> unconditionalBest=new HashMap<>();
        for(ItemCatalogueIndex.Item item:owned.items) for(Choice choice:RANKS.getOrDefault(item.id,Collections.emptyList())) {
            String key=key(item,choice);
            if(!setups.contains(choice.setup) || key.isEmpty() || !ready(item,choice) || !requirements.locked(item).isEmpty()) continue;
            if(choice.tier<best.getOrDefault(key,Integer.MAX_VALUE)) { best.put(key,choice.tier);bestNames.put(key,item.name); }
            if(choice.note.isEmpty() && requirements.reviewed(item)) unconditionalBest.merge(key,choice.tier,Math::min);
        }
        Map<Integer,Map<String,Integer>> candidates=new HashMap<>();Map<String,Integer> next=new HashMap<>();
        for(ItemCatalogueIndex.Item item:input.items) {
            if(owned.owned(item)!=null || !requirements.locked(item).isEmpty()) continue;
            for(Choice choice:RANKS.getOrDefault(item.id,Collections.emptyList())) {
                String key=key(item,choice);
                if(!setups.contains(choice.setup) || key.isEmpty() || !ready(item,choice) || !best.containsKey(key) || choice.tier>=best.get(key)) continue;
                candidates.computeIfAbsent(item.id,k->new HashMap<>()).put(key,choice.tier);
                next.merge(key,choice.tier,Math::max); // Closest available better column, not automatically BIS.
            }
        }
        Map<Integer,String> upgrades=new HashMap<>();
        for(Map.Entry<Integer,Map<String,Integer>> item:candidates.entrySet()) for(Map.Entry<String,Integer> candidate:item.getValue().entrySet())
            if(candidate.getValue().equals(next.get(candidate.getKey()))) {
                upgrades.put(item.getKey(),"Next available Wiki column above "+bestNames.get(candidate.getKey())+" for a matching guide/role. Not a DPS or value-for-money calculation; guide conditions still apply.");break;
            }
        List<ItemCatalogueIndex.Item> retained=new ArrayList<>();
        for(ItemCatalogueIndex.Item item:input.items) {
            boolean has=owned.owned(item)!=null;
            if(mode==Mode.OWNED && !has || mode==Mode.UNOWNED && has || mode==Mode.NEXT_UPGRADE && !upgrades.containsKey(item.id)) continue;
            if(hideWorse && mode!=Mode.OWNED && worseInEveryRole(item,setups,unconditionalBest)) continue;
            retained.add(item);
        }
        Map<Integer,ItemCatalogueIndex.Rank> ranks=new HashMap<>();
        for(ItemCatalogueIndex.Item item:retained) if(input.ranks.containsKey(item.id)) ranks.put(item.id,input.ranks.get(item.id));
        String notice=mode==Mode.NEXT_UPGRADE && retained.isEmpty()?"No verified next upgrade in this search. Try a specific guide/slot or All items for conditional, unranked and preparation-needed alternatives.":"";
        return new Outcome(new ItemCatalogueIndex.Result(retained,ranks,input.activity,input.loadout,input.setups),upgrades,notice);
    }
    private static boolean worseInEveryRole(ItemCatalogueIndex.Item item,Set<String> setups,Map<String,Integer> best)
    {
        String name=ItemTaxonomy.baseName(CombatEquivalence.familyName(item.fact));
        if(ItemTaxonomy.equipmentSet(name)!=null || name.startsWith("virtus ") || name.startsWith("bloodbark ")) return false;
        boolean compared=false;
        for(Choice choice:RANKS.getOrDefault(item.id,Collections.emptyList())) if(setups.contains(choice.setup)) {
            String key=key(item,choice);
            if(key.isEmpty() || !ready(item,choice) || !choice.note.isEmpty() || best.getOrDefault(key,Integer.MAX_VALUE)>=choice.tier) return false;
            compared=true;
        }
        return compared;
    }
}
