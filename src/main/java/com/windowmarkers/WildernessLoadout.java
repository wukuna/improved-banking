package com.windowmarkers;

import java.util.*;

/** Budget defaults for bank planning, not a price or death-protection calculation. */
final class WildernessLoadout
{
    private static final Set<String> ACTIVITIES=new HashSet<>(Arrays.asList("Callisto","Artio","Venenatis","Spindel",
        "Vet'ion","Calvar'ion","Scorpia","Chaos Fanatic","Chaos Elemental","Revenants","Krystilia",
        "King Black Dragon","Green dragon","Chaos Temple (church)","Abyss","Chest (Rogues' Castle)"));
    private static final Map<Integer,String[]> BUDGET=new HashMap<>();
    private static final Set<Integer> EQUIPMENT=new HashSet<>();
    static {
        for(String[] row:ItemTaxonomy.readRows("wilderness-budget-items.tsv",4)) BUDGET.put(Integer.parseInt(row[0]),row);
        for(String[] row:ItemTaxonomy.readRows("ge-item-slots.tsv",3))
            if(Integer.parseInt(row[1])>=0) EQUIPMENT.add(Integer.parseInt(row[0]));
    }
    private WildernessLoadout() { }
    static boolean applies(List<ActivitySetups.Setup> matched) {
        return matched.size()>1 && ACTIVITIES.contains(matched.get(0).activity)
            && matched.stream().allMatch(s->s.activity.equals(matched.get(0).activity));
    }
    static boolean allowed(ItemCatalogueIndex.Item item) {
        if(item.slot<0 && !EQUIPMENT.contains(item.id)) return true;
        String[] row=BUDGET.get(item.id);
        return row!=null && row[1].equalsIgnoreCase(item.name) && Integer.parseInt(row[2])==item.slot;
    }
    static boolean allowedPreview(int id) { return !EQUIPMENT.contains(id) || BUDGET.containsKey(id); }
    static boolean wildernessWeapon(int id) { return BUDGET.containsKey(id) && BUDGET.get(id)[3].equals("wilderness weapon"); }
    static List<ActivitySetups.Setup> defaults(List<ActivitySetups.Setup> matched) {
        List<ActivitySetups.Setup> result=new ArrayList<>();
        for(ActivitySetups.Setup setup:matched) {
            String label=setup.label.toLowerCase(Locale.ROOT);
            if(!label.contains("high-risk") && !label.contains("bis smuggle") && !label.contains("gravestone")) result.add(setup);
        }
        return result;
    }
    static void finish(SetupLoadout plan) {
        plan.wildernessBudget=true;
        for(SetupLoadout part:plan.sections()) {
            part.wildernessBudget=true;
            // Keep source positions, supplies and meaningful empty slots. A
            // costly equipment switch must not survive as a grey recommendation.
            for(var iterator=part.inventoryExample.entrySet().iterator();iterator.hasNext();) {
                var entry=iterator.next();var field=entry.getValue();
                List<Integer> kept=new ArrayList<>();for(int id:field.ids) if(allowedPreview(id)) kept.add(id);
                if(kept.isEmpty()) { iterator.remove();part.inventory.remove(entry.getKey()); }
                else if(!kept.equals(field.ids)) {
                    SetupLoadout.Field copy=new SetupLoadout.Field(new String[]{"",field.name,"",field.note},field.guideOnly);
                    copy.ids.addAll(kept);copy.itemNotes.putAll(field.itemNotes);entry.setValue(copy);
                }
            }
            part.equipment.forEach((slot,ids)-> {
                for(int id:ids) part.equipmentNotes.put(id,(wildernessWeapon(id)
                    ?"Owned Wilderness weapon; charges and ether add risk. "
                    :"Owned budget choice for Wilderness travel/content. ")
                    +"Check items kept on death; no protection is assumed. "+part.equipmentNotes.getOrDefault(id,""));
            });
        }
    }
}
