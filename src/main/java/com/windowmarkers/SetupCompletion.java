package com.windowmarkers;

import java.util.*;

/** Conservative completion for omitted boot and utility-ammo slots. */
final class SetupCompletion
{
    private SetupCompletion() { }
    static String style(String label) {
        String text=label.toLowerCase(Locale.ROOT);
        if(text.contains("magic") || text.contains("mage")) return "magic";
        if(text.contains("ranged") || text.contains("range ") || text.equals("range")) return "ranged";
        if(text.contains("melee") || text.contains("flinching")) return "melee";
        return "";
    }
    static Set<Integer> protectedSlots(String activity) {
        String name=activity.toLowerCase(Locale.ROOT);
        if(name.contains("lizardman shaman")) return new HashSet<>(Arrays.asList(0,4,7,9,10));
        if(name.contains("wyvern") || name.contains("basilisk")) return Collections.singleton(5);
        if(name.contains("smoke devil")) return Collections.singleton(0);
        return Collections.emptySet();
    }
    static void fill(Map<Integer,List<Integer>> gear,Map<Integer,ItemCatalogueIndex.Item> owned,String style,Map<Integer,String> notes,Set<Integer> protectedSlots) {
        // Upgrades affect only their own style. Base/non-matching treads do not
        // inherit the upgraded melee strength, ranged strength or magic damage.
        int[] boots=style.equals("magic")?new int[]{31097,31093,31095,31096,31088,31091,31092,31094,13235}
            :style.equals("ranged")?new int[]{31097,31092,31094,31096,31088,31091,31093,31095,13237}
            :new int[]{31097,31091,31094,31095,13239,31088,31092,31093,31096,11840};
        List<Integer> existing=gear.getOrDefault(10,Collections.emptyList());
        // Preserve specialist footwear (e.g. insulated boots), rather than
        // silently replacing a mechanic's required item with combat boots.
        if(!protectedSlots.contains(10) && (existing.isEmpty() || existing.stream().anyMatch(id->id>=31088 && id<=31097 || id==13235 || id==13237 || id==13239))) {
            List<Integer> choices=new ArrayList<>();
            for(int id:boots) {
                ItemCatalogueIndex.Item item=owned.get(id);
                if(item!=null && item.slot==10 && !ItemVariantState.needsActivityReview(item.fact)) choices.add(id);
            }
            for(int id:existing) if(!choices.contains(id)) choices.add(id);
            if(!choices.isEmpty()) {
                gear.put(10,choices);
                for(int id:choices) if(!existing.contains(id)) notes.put(id,"Owned boots completing this style's equipment. Avernic upgrades apply only to their associated combat style; check equip requirements.");
            }
        }
        if(gear.containsKey(13)) return;
        ItemCatalogueIndex.Item weapon=gear.containsKey(3)?owned.get(gear.get(3).get(0)):null;
        List<Integer> blessings=new ArrayList<>();
        for(int id:new int[]{22947,22945,20220,20223,20226,20229,20232,20235,22943,22941}) {
            ItemCatalogueIndex.Item item=owned.get(id);
            if(item!=null && item.slot==13 && !ItemVariantState.needsActivityReview(item.fact)
                && SetupEquipmentChoice.allowed(item,"",weapon,owned.values())) {
                blessings.add(id);notes.put(id,"Owned prayer-slot alternative. A blessing does not replace ammunition required by the selected weapon.");
            }
        }
        if(!blessings.isEmpty()) gear.put(13,blessings);
    }
}
