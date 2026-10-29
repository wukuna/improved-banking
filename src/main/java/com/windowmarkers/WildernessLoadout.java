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
    // Display-only preferences inside the existing budget policy and the exact
    // source's equipment fields. They never enter owned selection or filtering.
    private static final List<Integer> EXAMPLES=Arrays.asList(3749,10499,1704,2503,2497,2491,6328,12788,892,
        4089,2412,4091,4093,7460,4097,28585,11907,1434,3105,3842,12612);
    private static ItemCatalogueIndex.Item exampleItem(int id) {
        String[] row=BUDGET.get(id);if(row==null) return null;
        int slot=Integer.parseInt(row[2]);
        return new ItemCatalogueIndex.Item(new AutoCategories.Item(id,row[1],true,new String[]{"Wear"},0,0,0,slot),row[1],slot);
    }
    private static void recommendMissing(SetupLoadout part) {
        SetupLoadout.Table table=part.equipmentTable();if(table==null) return;
        List<SetupLoadout.Field> fields=new ArrayList<>(table.fields);
        fields.sort(Comparator.comparingInt((SetupLoadout.Field f)->f.slot()==3?0:1).thenComparingInt(SetupLoadout.Field::order));
        Map<Integer,List<Integer>> candidates=new LinkedHashMap<>();Map<Integer,String> notes=new HashMap<>();
        for(var field:fields) if(field.slot()>=0) for(int id:field.ids) {
            var item=exampleItem(id);
            if(item==null || item.slot!=field.slot() || wildernessWeapon(id) || ItemVariantState.needsActivityReview(item.fact)
                || !SetupItemRequirements.reviewReason(part.equipmentSource,item.fact).isEmpty()) continue;
            candidates.computeIfAbsent(item.slot,k->new ArrayList<>()).add(id);
            notes.put(id,field.itemNotes.getOrDefault(id,field.note));
        }
        ItemCatalogueIndex.Item weapon=part.equipment.containsKey(3)?exampleItem(part.equipment.get(3).get(0)):null;
        for(var entry:candidates.entrySet()) {
            List<Integer> existing=part.equipment.getOrDefault(entry.getKey(),Collections.emptyList());
            if(!existing.isEmpty() && (entry.getKey()!=13 || weapon==null
                || SetupEquipmentChoice.allowed(exampleItem(existing.get(0)),"",weapon,Collections.emptyList()))) continue;
            entry.getValue().sort(Comparator.comparingInt(id->EXAMPLES.contains(id)?EXAMPLES.indexOf(id):Integer.MAX_VALUE));
            for(int id:entry.getValue()) {
                var item=exampleItem(id);
                if(!SetupEquipmentChoice.allowed(item,notes.get(id),weapon,Collections.emptyList())) continue;
                part.equipmentExample.put(entry.getKey(),id);
                if(entry.getKey()==3) weapon=item;
                break;
            }
        }
    }
    static void finish(SetupLoadout plan) {
        plan.wildernessBudget=true;
        for(SetupLoadout part:plan.sections()) {
            part.wildernessBudget=true;
            recommendMissing(part);
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
