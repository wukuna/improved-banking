package com.windowmarkers;

import java.util.*;

/** Owned combat switches, separate from teleport/charge requirements and guide ranks. */
final class InventoryGearAlternatives
{
    private static final Map<Integer,AutoCategories.Item> IDENTITIES=new HashMap<>();
    private static final Map<String,List<Integer>> FAMILIES=new HashMap<>();
    static {
        for(String[] row:ItemTaxonomy.readRows("ge-item-slots.tsv",3)) {
            int id=Integer.parseInt(row[0]),slot=Integer.parseInt(row[1]);
            IDENTITIES.put(id,new AutoCategories.Item(id,row[2],true,null,0,0,0,slot));
        }
        for(AutoCategories.Item item:IDENTITIES.values())
            FAMILIES.computeIfAbsent(family(item),key->new ArrayList<>()).add(item.id);
        FAMILIES.values().forEach(Collections::sort);
    }
    private InventoryGearAlternatives() { }

    static String family(AutoCategories.Item item) {
        return ItemTaxonomy.baseName(CombatEquivalence.familyName(item));
    }
    static boolean sameCombatItem(int expected,AutoCategories.Item held) {
        AutoCategories.Item example=IDENTITIES.get(expected),identity=IDENTITIES.get(held.id);
        if(example==null || identity==null || !identity.name.equals(held.name) || example.slot!=identity.slot) return false;
        // Rings, weapons and ammo can represent teleports, specific specials,
        // gathering effects or ammunition requirements rather than a gear swap.
        if(!Arrays.asList(0,1,2,4,5,7,9,10).contains(example.slot)) return false;
        return family(example).equals(family(held)) && ItemVariantState.canRepresentOwnedEquivalent(held,example);
    }
    static List<Integer> equivalents(List<Integer> exact,Map<Integer,ItemCatalogueIndex.Item> owned) {
        List<Integer> result=new ArrayList<>(exact);
        for(int expected:exact) {
            AutoCategories.Item example=IDENTITIES.get(expected);
            if(example==null) continue;
            for(int id:FAMILIES.getOrDefault(family(example),Collections.emptyList())) {
                ItemCatalogueIndex.Item item=owned.get(id);
                if(item!=null && !result.contains(id) && sameCombatItem(expected,item.fact)) result.add(id);
            }
        }
        return result;
    }
    static boolean rangedAccessory(int id) {
        AutoCategories.Item item=IDENTITIES.get(id);
        if(item==null) return false;
        return Arrays.asList("dizana's quiver","ava's assembler","ava's accumulator",
            "necklace of anguish","necklace of rupture","twisted buckler","dragonfire ward",
            "odium ward","aquanite hopper","book of law","antler guard").contains(family(item));
    }
    static boolean sameCombatItem(int expected,int candidate) {
        AutoCategories.Item item=IDENTITIES.get(candidate);
        return item!=null && (expected==candidate || sameCombatItem(expected,item));
    }
}
