package com.windowmarkers;

import java.util.*;

/** An activity contains separate source loadouts; inventories are never unioned. */
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
    private static final Map<String,String> SWITCH_NOTES=new HashMap<>();
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
        for(String[] row:ItemTaxonomy.readRows("setup-switch-notes.tsv",2)) SWITCH_NOTES.put(row[0],row[1]);
    }
    final String equipmentName,inventoryName;
    final String activity;
    final Map<Integer,List<Integer>> equipment;
    final Map<Integer,Integer> inventory;
    final Map<Integer,Field> inventoryExample;
    final Map<Integer,String> equipmentNotes=new HashMap<>();
    String equipmentNote="";
    String sectionName="";
    private List<SetupLoadout> sections=Collections.emptyList();
    List<SetupLoadout> sections() { return sections.isEmpty()?Collections.singletonList(this):sections; }
    int sectionCount() { return sections.isEmpty()?1:sections.size(); }
    Set<Integer> itemIds() {
        Set<Integer> result=new LinkedHashSet<>();
        for(SetupLoadout section:sections()) { section.equipment.values().forEach(result::addAll);result.addAll(section.inventory.values()); }
        return result;
    }
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
        result.equipmentNote=equipmentNote;
        result.sectionName=sectionName;
        if(!sections.isEmpty()) {
            List<SetupLoadout> kept=new ArrayList<>();
            for(SetupLoadout section:sections) kept.add(section.retainItems(allowed));
            result.sections=Collections.unmodifiableList(kept);
        }
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
        // Explicit source selections and saved inventory choices remain stable.
        if(matched.size()<2) return buildOne(matched,owned,visible,inventoryContext);
        Set<String> activities=new HashSet<>();matched.forEach(s->activities.add(s.activity));
        Set<String> families=new HashSet<>();activities.forEach(a->families.add(a.split("/",2)[0]));
        if(families.size()!=1) return buildOne(matched,owned,visible,inventoryContext);
        String activityName=families.iterator().next();
        Map<String,List<ActivitySetups.Setup>> groups=new LinkedHashMap<>();
        boolean recommended=matched.stream().anyMatch(s->TABLES.containsKey(s.id) && TABLES.get(s.id).kind.equals("recommended equipment") && layoutTable(TABLES.get(s.id)));
        for(ActivitySetups.Setup setup:matched) {
            Table table=TABLES.get(setup.id);
            if(table==null || !table.kind.contains("equipment") || !layoutTable(table) || recommended && !table.kind.equals("recommended equipment")) continue;
            String phase=table.context.toLowerCase(Locale.ROOT).contains("phase")?table.context.replaceFirst("^Equipment / ",""):"";
            String style=SetupCompletion.style(table.style);
            String name=phase.isEmpty()?table.style:phase+" / "+(style.isEmpty()?table.style:style.substring(0,1).toUpperCase(Locale.ROOT)+style.substring(1));
            if(activities.size()>1 && !setup.activity.equals(activityName)) name=setup.activity.substring(activityName.length()+1)+" / "+name;
            groups.computeIfAbsent(name,k->new ArrayList<>()).add(setup);
        }
        if(groups.size()<2) return buildOne(matched,owned,visible,inventoryContext);
        List<SetupLoadout> parts=new ArrayList<>();
        groups.forEach((name,setups)-> {
            SetupLoadout part=buildOne(setups,owned,visible,inventoryContext);
            if(part!=null) { part.sectionName=name;parts.add(part); }
        });
        if(parts.isEmpty()) return buildOne(matched,owned,visible,inventoryContext);
        SetupLoadout first=parts.get(0);
        SetupLoadout result=new SetupLoadout(activityName,first.equipmentName,first.inventoryName,first.equipment,first.inventory,first.inventoryExample);
        result.equipmentNotes.putAll(first.equipmentNotes);result.equipmentNote=first.equipmentNote;
        result.sections=Collections.unmodifiableList(parts);
        return result;
    }
    private static int slotCount(Table table) {
        Set<Integer> slots=new HashSet<>();for(Field field:table.fields) if(field.slot()>=0) slots.add(field.slot());return slots.size();
    }
    private static boolean layoutTable(Table table) {
        return slotCount(table)>=3 || slotCount(table)>0 && (table.context+" "+table.style).toLowerCase(Locale.ROOT).contains("phase");
    }
    private static SetupLoadout buildOne(List<ActivitySetups.Setup> matched,Map<Integer,ItemCatalogueIndex.Item> owned,Set<Integer> visible,String inventoryContext)
    {
        ActivitySetups.Setup gear=null; int best=-1;
        for(ActivitySetups.Setup setup:matched) {
            Table table=TABLES.get(setup.id); if(table==null || !table.kind.contains("equipment")) continue;
            Set<Integer> slots=new HashSet<>(),available=new HashSet<>();
            for(Field field:table.fields) if(field.slot()>=0) {
                slots.add(field.slot());
                if(field.ids.stream().anyMatch(id->visible.contains(id) && equipmentReady(setup,owned.get(id)) && owned.get(id).slot==field.slot())) available.add(field.slot());
            }
            // Phase-only switches are completed from the parent activity.
            // Ordinary room/utility lists remain in their own alternatives group.
            if(!layoutTable(table)) continue;
            int score=available.size()*100+(table.kind.equals("recommended equipment")?20:0)+slots.size();
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
            if(gear!=null) completeEquipment(selected,gear,table,owned,requirements,equipmentNotes);
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
        result.equipmentNote=gear==null?"":SWITCH_NOTES.getOrDefault(gear.id,"");
        return result;
    }
    /** Fill omissions without promoting a different activity's rankings to this boss's BIS. */
    private static void completeEquipment(Map<Integer,List<Integer>> equipment,ActivitySetups.Setup gear,Table table,
        Map<Integer,ItemCatalogueIndex.Item> owned,Collection<ItemCatalogueIndex.Item> requirements,Map<Integer,String> notes)
    {
        String style=SetupCompletion.style(table.style);
        if(style.isEmpty() && equipment.containsKey(3)) {
            ItemCatalogueIndex.Item weapon=owned.get(equipment.get(3).get(0));
            String role=ItemTaxonomy.weaponRole(ItemVariantState.familyName(weapon.fact));
            style=role==null?"":SetupCompletion.style(role);
        }
        if(style.isEmpty()) return; // skilling methods retain their actual equipment requirements
        Set<Integer> protectedSlots=SetupCompletion.protectedSlots(gear.activity);
        // A listed weapon defines the method. Do not quietly turn an unavailable
        // scorching-bow method into a melee method, or replace a required weapon.
        if(table.fields.stream().anyMatch(f->f.slot()==3)) {
            protectedSlots=new HashSet<>(protectedSlots);protectedSlots.add(3);
        }
        List<ActivitySetups.Setup> donors=new ArrayList<>();
        for(ActivitySetups.Setup candidate:ActivitySetups.forActivity(gear.activity)) {
            Table source=TABLES.get(candidate.id);
            if(source!=null && source.kind.equals("recommended equipment") && SetupCompletion.style(source.style).equals(style)) donors.add(candidate);
        }
        String fallback=style.equals("magic")?"364045:3":style.equals("ranged")?"364045:2":"364045:1";
        ActivitySetups.Setup general=ActivitySetups.byId(fallback);if(general!=null) donors.add(general);
        for(ActivitySetups.Setup donor:donors) {
            List<Field> fields=new ArrayList<>(TABLES.get(donor.id).fields);
            fields.sort(Comparator.comparingInt((Field f)->f.slot()==3?0:1).thenComparingInt(Field::order));
            Set<Integer> missing=new HashSet<>(Arrays.asList(0,1,2,3,4,5,7,9,10,12,13));missing.removeAll(equipment.keySet());
            missing.removeAll(protectedSlots);
            if(!donor.activity.equals(gear.activity)) missing.remove(3);
            ItemCatalogueIndex.Item weapon=equipment.containsKey(3)?owned.get(equipment.get(3).get(0)):null;
            for(Field field:fields) if(missing.contains(field.slot())) for(int id:field.ids) {
                ItemCatalogueIndex.Item item=owned.get(id);String note=field.itemNotes.getOrDefault(id,field.note);
                if(!equipmentReady(donor,item) || item.slot!=field.slot() || !SetupEquipmentChoice.allowed(item,note,weapon,requirements)) continue;
                List<Integer> options=equipment.computeIfAbsent(field.slot(),k->new ArrayList<>());
                if(!options.contains(id)) {
                    options.add(id);notes.put(id,"Owned "+style+" alternative for a missing guide slot; not a new boss-specific ranking. "+note);
                }
                if(field.slot()==3 && weapon==null) weapon=item;
            }
        }
        SetupCompletion.fill(equipment,owned,style,notes,protectedSlots);
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
