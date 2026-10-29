package com.windowmarkers;

import java.util.*;

/** Same potion, different remaining doses; never substitute a different effect. */
final class InventoryPotionDoses
{
    private static final Map<Integer,String[]> DOSES=new HashMap<>();
    private static final Map<String,List<Integer>> FAMILIES=new HashMap<>();
    static {
        for(String[] row:ItemTaxonomy.readRows("inventory-potion-doses.tsv",4)) {
            int id=Integer.parseInt(row[0]);DOSES.put(id,row);
            FAMILIES.computeIfAbsent(row[2],k->new ArrayList<>()).add(id);
        }
        FAMILIES.values().forEach(ids->ids.sort(Comparator.comparingInt(id->-Integer.parseInt(DOSES.get(id)[3]))));
    }
    private InventoryPotionDoses() { }
    static boolean listed(Collection<Integer> exact,AutoCategories.Item item) {
        String[] row=DOSES.get(item.id);
        return row!=null && row[1].equalsIgnoreCase(item.name)
            && exact.stream().anyMatch(id->DOSES.containsKey(id) && DOSES.get(id)[2].equals(row[2]));
    }
    static List<Integer> alternatives(List<Integer> exact) {
        List<Integer> result=new ArrayList<>(exact);
        for(int id:exact) if(DOSES.containsKey(id)) for(int other:FAMILIES.get(DOSES.get(id)[2]))
            if(!result.contains(other)) result.add(other);
        return result;
    }
    static boolean known(int id) { return DOSES.containsKey(id); }
    static String note(int id) {
        return "Owned "+DOSES.get(id)[3]+"-dose alternative of the same potion. The guide's total doses are not guaranteed; repeated slots share your bank stack.";
    }
}
