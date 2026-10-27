package com.windowmarkers;

import java.util.*;

/** One source equipment table and one source inventory, never a union of inventories. */
final class SetupLoadout
{
    static final class Field {
        final String name, note; final List<Integer> ids=new ArrayList<>();
        final boolean guideOnly;
        final Map<Integer,String> itemNotes=new HashMap<>();
        Field(String[] row) { this(row,false); }
        Field(String[] row,boolean guideOnly) { name=row[1]; note=row[3];this.guideOnly=guideOnly; for(String id:row[2].split(",")) if(!id.isEmpty()) ids.add(Integer.parseInt(id)); }
        int slot() { return name.matches("2h[0-9]*")?3:ItemCatalogueIndex.slot(name.replaceAll("[0-9]+$","")); }
        int order() { String number=name.replaceFirst("^2h","weapon").replaceAll("[^0-9]",""); return number.isEmpty()?0:Integer.parseInt(number); }
        boolean inventoryChoice() { return !guideOnly && (note.isEmpty() || note.startsWith("The Wiki chooses ") || note.startsWith("The Vale Totems magiclog variable ")); }
    }
    static final class Table {
        final String kind,context,style; final List<Field> fields=new ArrayList<>();
        Table(String[] row) { kind=row[1];context=row[2];style=row[3]; }
    }
    private static final Map<String,Table> TABLES=new LinkedHashMap<>();
    private static final Map<String,String> INVENTORY_PAIRS=new HashMap<>();
    static {
        for(String[] row:ItemTaxonomy.readRows("setup-layout-tables.tsv",4)) TABLES.put(row[0],new Table(row));
        for(String[] row:ItemTaxonomy.readRows("setup-layout-fields.tsv",4)) {
            Table table=TABLES.get(row[0]); if(table!=null) table.fields.add(new Field(row));
        }
        Map<String,List<String[]>> choices=new LinkedHashMap<>();
        for(String[] row:ItemTaxonomy.readRows("setup-layout-options.tsv",5))
            choices.computeIfAbsent(row[0]+"/"+row[1],key->new ArrayList<>()).add(row);
        for(Map.Entry<String,Table> entry:TABLES.entrySet()) for(Field field:entry.getValue().fields) {
            List<String[]> rows=choices.get(entry.getKey()+"/"+field.name);
            if(rows==null) continue;
            field.ids.clear();
            for(String[] row:rows) { int id=Integer.parseInt(row[2]);field.ids.add(id);field.itemNotes.put(id,row[4]); }
        }
        for(String[] row:ItemTaxonomy.readRows("setup-inventory-previews.tsv",6)) {
            Table table=TABLES.get(row[0]);
            if(table!=null && table.kind.equals("inventory")) table.fields.add(new Field(new String[]{row[0],row[1],row[2],row[4]},true));
        }
        for(String[] row:ItemTaxonomy.readRows("setup-inventory-pairs.tsv",2)) INVENTORY_PAIRS.put(row[0],row[1]);
    }
    final String equipmentName,inventoryName;
    final String activity;
    final Map<Integer,List<Integer>> equipment;
    final Map<Integer,Integer> inventory;
    final Map<Integer,Field> inventoryExample;
    final Map<Integer,String> equipmentNotes=new HashMap<>();
    private SetupLoadout(String activity,String equipmentName,String inventoryName,Map<Integer,List<Integer>> equipment,Map<Integer,Integer> inventory,Map<Integer,Field> inventoryExample) {
        this.activity=activity;
        this.equipmentName=equipmentName;this.inventoryName=inventoryName;
        this.equipment=equipment;this.inventory=inventory;this.inventoryExample=inventoryExample;
    }
    static boolean inventoryTable(ActivitySetups.Setup setup) { Table table=TABLES.get(setup.id);return table!=null && table.kind.equals("inventory"); }
    /** A custom view is an explicit item selection; removed items must not return as example icons. */
    SetupLoadout retainItems(Set<Integer> allowed)
    {
        Map<Integer,List<Integer>> gear=new LinkedHashMap<>();
        equipment.forEach((slot,ids)-> { List<Integer> kept=new ArrayList<>();for(int id:ids) if(allowed.contains(id)) kept.add(id);if(!kept.isEmpty()) gear.put(slot,kept); });
        Map<Integer,Integer> bag=new LinkedHashMap<>();inventory.forEach((slot,id)-> { if(allowed.contains(id)) bag.put(slot,id); });
        Map<Integer,Field> example=new LinkedHashMap<>();inventoryExample.forEach((slot,field)-> { if(bag.containsKey(slot)) example.put(slot,field); });
        SetupLoadout result=new SetupLoadout(activity,equipmentName,inventoryName,gear,bag,example);
        equipmentNotes.forEach((id,note)-> { if(allowed.contains(id)) result.equipmentNotes.put(id,note); });
        return result;
    }
    private static boolean ready(ItemCatalogueIndex.Item item) { return item!=null && !ItemVariantState.needsActivityReview(item.fact); }
    private static boolean inventoryReady(ActivitySetups.Setup setup,ItemCatalogueIndex.Item item) {
        return ready(item) && SetupItemRequirements.reviewReason(setup.id,item.fact).isEmpty();
    }
    private static boolean equipmentReady(ActivitySetups.Setup setup,ItemCatalogueIndex.Item item) {
        return inventoryReady(setup,item) && !ItemVariantState.needsRankReview(item.fact,setup.activity,setup.label);
    }
    static SetupLoadout build(List<ActivitySetups.Setup> matched,Map<Integer,ItemCatalogueIndex.Item> owned,Set<Integer> visible,String inventoryContext)
    {
        ActivitySetups.Setup gear=null; int best=-1;
        for(ActivitySetups.Setup setup:matched) {
            Table table=TABLES.get(setup.id); if(table==null || !table.kind.contains("equipment")) continue;
            Set<Integer> slots=new HashSet<>(),available=new HashSet<>();
            for(Field field:table.fields) if(field.slot()>=0) {
                slots.add(field.slot());
                if(field.ids.stream().anyMatch(id->visible.contains(id) && equipmentReady(setup,owned.get(id)) && owned.get(id).slot==field.slot())) available.add(field.slot());
            }
            // A boss-phase weapon list is not a complete equipment guide.
            if(slots.size()<3) continue;
            int score=available.size()*100+(table.kind.equals("recommended equipment")?10:0);
            if(score>best) { gear=setup;best=score; }
        }
        ActivitySetups.Setup anchor=gear;
        if(anchor==null) for(ActivitySetups.Setup setup:matched) if(inventoryTable(setup)) { anchor=setup;break; }
        if(anchor==null) return null;
        Table table=TABLES.get(anchor.id);
        Map<Integer,List<Integer>> equipment=new LinkedHashMap<>();
        List<Field> fields=gear==null?new ArrayList<>():new ArrayList<>(table.fields); fields.sort(Comparator.comparingInt(Field::order));
        // Resolve the main weapon first, then select compatible ammunition and
        // dependent equipment using that exact guide choice.
        fields.sort(Comparator.comparingInt((Field f)->f.slot()==3?0:1).thenComparingInt(Field::order));
        Map<Integer,String> equipmentNotes=new HashMap<>();
        Collection<ItemCatalogueIndex.Item> requirements=owned.values();
        for(int pass=0;pass<4;pass++) {
            Map<Integer,List<Integer>> selected=chooseEquipment(fields,gear,owned,visible,requirements,equipmentNotes);
            if(selected.equals(equipment)) break;
            equipment=selected;
            List<ItemCatalogueIndex.Item> wearing=new ArrayList<>();
            for(List<Integer> options:equipment.values()) if(!options.isEmpty()) wearing.add(owned.get(options.get(0)));
            requirements=wearing;
        }
        ActivitySetups.Setup inventory=null;best=-1;int bestPreference=-1;
        for(ActivitySetups.Setup setup:ActivitySetups.forActivity(anchor.activity)) {
            if(!inventoryTable(setup)) continue;
            Table candidate=TABLES.get(setup.id);
            int score=0;Set<List<Integer>> distinct=new HashSet<>();
            for(Field field:candidate.fields) if(field.inventoryChoice() && distinct.add(field.ids)) {
                // Owning several colours/charge states of one switch must not
                // make an unrelated example appear more complete.
                int available=0;
                for(int id:field.ids) if(inventoryReady(setup,owned.get(id)))
                    available=Math.max(available,owned.get(id).slot>=0?20:1);
                score+=available;
            }
            boolean paired=gear!=null && setup.id.equals(INVENTORY_PAIRS.get(gear.id));
            boolean sameStyle=gear!=null && !table.style.isEmpty() && candidate.style.equalsIgnoreCase(table.style);
            boolean sameContext=sameStyle && !table.context.isEmpty() && candidate.context.equalsIgnoreCase(table.context);
            // Different methods can reuse labels such as "Best in slot".
            // Prefer the source pairing/context before comparing owned items.
            int preference=setup.id.equals(inventoryContext)?5:paired?4:sameContext?3:sameStyle?2:matched.contains(setup)?1:0;
            if(preference>bestPreference || preference==bestPreference && score>best)
            { inventory=setup;best=score;bestPreference=preference; }
        }
        Map<Integer,Integer> inventoryItems=new TreeMap<>();
        Map<Integer,Field> inventoryExample=new TreeMap<>();
        if(inventory!=null) for(Field field:TABLES.get(inventory.id).fields) {
            int position=field.order()-1;
            if(!(field.name.matches("[0-9]+") || field.name.startsWith("inventory")) || position<0 || position>=28 || field.ids.isEmpty()) continue;
            inventoryExample.put(position,field);
            if(!field.inventoryChoice()) continue;
            for(int id:SetupEquipmentChoice.inventoryAlternatives(field.ids))
                if(inventoryReady(inventory,owned.get(id))) { inventoryItems.put(position,id);break; }
        }
        SetupLoadout result=new SetupLoadout(anchor.activity,gear==null?"":gear.activity+" / "+gear.label,inventory==null?"No inventory example":inventory.label,equipment,inventoryItems,inventoryExample);
        result.equipmentNotes.putAll(equipmentNotes);
        return result;
    }
    private static Map<Integer,List<Integer>> chooseEquipment(List<Field> fields,ActivitySetups.Setup gear,
        Map<Integer,ItemCatalogueIndex.Item> owned,Set<Integer> visible,Collection<ItemCatalogueIndex.Item> requirements,
        Map<Integer,String> equipmentNotes)
    {
        Map<Integer,List<Integer>> equipment=new LinkedHashMap<>();equipmentNotes.clear();
        ItemCatalogueIndex.Item weapon=null;
        for(Field field:fields) {
            int slot=field.slot();if(slot<0) continue;
            for(int id:field.ids) {
                ItemCatalogueIndex.Item item=owned.get(id);
                if(!visible.contains(id) || !equipmentReady(gear,item) || item.slot!=slot) continue;
                String note=field.itemNotes.getOrDefault(id,field.note);
                if(!SetupEquipmentChoice.allowed(item,note,weapon,requirements)) continue;
                List<Integer> options=equipment.computeIfAbsent(slot,k->new ArrayList<>());
                if(!options.contains(id)) { options.add(id);equipmentNotes.put(id,note); }
                if(slot==3 && weapon==null) weapon=item;
            }
        }
        return equipment;
    }

}
