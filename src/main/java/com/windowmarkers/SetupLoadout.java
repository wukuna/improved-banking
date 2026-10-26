package com.windowmarkers;

import java.util.*;

/** One source equipment table and one source inventory, never a union of inventories. */
final class SetupLoadout
{
    static final class Field {
        final String name, note; final List<Integer> ids=new ArrayList<>();
        final boolean guideOnly;
        Field(String[] row) { this(row,false); }
        Field(String[] row,boolean guideOnly) { name=row[1]; note=row[3];this.guideOnly=guideOnly; for(String id:row[2].split(",")) if(!id.isEmpty()) ids.add(Integer.parseInt(id)); }
        int slot() { return name.matches("2h[0-9]*")?3:ItemCatalogueIndex.slot(name.replaceAll("[0-9]+$","")); }
        int order() { String number=name.replaceFirst("^2h","weapon").replaceAll("[^0-9]",""); return number.isEmpty()?0:Integer.parseInt(number); }
        boolean unconditional() { return note.isEmpty() || note.equals("Can function as hybrid armour, saving inventory space"); }
        boolean inventoryChoice() { return !guideOnly && (note.isEmpty() || note.startsWith("The Wiki chooses ") || note.startsWith("The Vale Totems magiclog variable ")); }
    }
    static final class Table {
        final String kind,style; final List<Field> fields=new ArrayList<>();
        Table(String[] row) { kind=row[1];style=row[3]; }
    }
    private static final Map<String,Table> TABLES=new LinkedHashMap<>();
    private static final Map<String,String> INVENTORY_PAIRS=new HashMap<>();
    static {
        for(String[] row:ItemTaxonomy.readRows("setup-layout-tables.tsv",4)) TABLES.put(row[0],new Table(row));
        for(String[] row:ItemTaxonomy.readRows("setup-layout-fields.tsv",4)) {
            Table table=TABLES.get(row[0]); if(table!=null) table.fields.add(new Field(row));
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
    private SetupLoadout(String activity,String equipmentName,String inventoryName,Map<Integer,List<Integer>> equipment,Map<Integer,Integer> inventory,Map<Integer,Field> inventoryExample) {
        this.activity=activity;
        this.equipmentName=equipmentName;this.inventoryName=inventoryName;
        this.equipment=equipment;this.inventory=inventory;this.inventoryExample=inventoryExample;
    }
    static boolean inventoryTable(ActivitySetups.Setup setup) { Table table=TABLES.get(setup.id);return table!=null && table.kind.equals("inventory"); }
    private static boolean ready(ItemCatalogueIndex.Item item) { return item!=null && !ItemVariantState.needsActivityReview(item.fact); }
    private static boolean inventoryReady(ActivitySetups.Setup setup,ItemCatalogueIndex.Item item) {
        return ready(item) && SetupItemRequirements.reviewReason(setup.id,item.fact).isEmpty();
    }
    static SetupLoadout build(List<ActivitySetups.Setup> matched,Map<Integer,ItemCatalogueIndex.Item> owned,Set<Integer> visible,String inventoryContext)
    {
        ActivitySetups.Setup gear=null; int best=-1;
        for(ActivitySetups.Setup setup:matched) {
            Table table=TABLES.get(setup.id); if(table==null || !table.kind.contains("equipment")) continue;
            Set<Integer> slots=new HashSet<>(),available=new HashSet<>();
            for(Field field:table.fields) if(field.slot()>=0) {
                slots.add(field.slot());
                if(field.ids.stream().anyMatch(id->visible.contains(id) && ready(owned.get(id)) && owned.get(id).slot==field.slot())) available.add(field.slot());
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
        for(Field field:fields) {
            int slot=field.slot(); if(slot<0) continue;
            // Conditional alternatives remain below the equipment diagram.
            if(!field.unconditional()) continue;
            for(int id:field.ids) {
                ItemCatalogueIndex.Item item=owned.get(id);
                if(!visible.contains(id) || !ready(item) || item.slot!=slot
                    || ItemVariantState.needsRankReview(item.fact,gear.activity,gear.label)) continue;
                List<Integer> options=equipment.computeIfAbsent(slot,k->new ArrayList<>());
                if(!options.contains(id)) options.add(id);
            }
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
            boolean paired=gear!=null && (setup.id.equals(INVENTORY_PAIRS.get(gear.id))
                || !table.style.isEmpty() && candidate.style.equalsIgnoreCase(table.style));
            int preference=setup.id.equals(inventoryContext)?3:paired?2:matched.contains(setup)?1:0;
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
            for(int id:field.ids) if(inventoryReady(inventory,owned.get(id))) { inventoryItems.put(position,id);break; }
        }
        return new SetupLoadout(anchor.activity,gear==null?"":gear.activity+" / "+gear.label,inventory==null?"No inventory example":inventory.label,equipment,inventoryItems,inventoryExample);
    }
}
