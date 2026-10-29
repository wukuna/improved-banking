package com.windowmarkers;

import java.util.*;
import net.runelite.api.widgets.Widget;
import net.runelite.client.game.ItemManager;

/** A display-only arrangement. Widget item IDs, bank indexes and actions stay native. */
final class SetupItemLayout
{
    private static final int[] SLOTS={0,1,2,3,4,5,7,9,10,12,13};
    static final class Heading {
        final String text; final int x,y;
        final boolean emptySlot;
        Heading(String text,int x,int y) { this(text,x,y,false); }
        Heading(String text,int x,int y,boolean emptySlot) { this.text=text;this.x=x;this.y=y;this.emptySlot=emptySlot; }
    }
    static final class InventoryNote {
        final String text;final int x,y;
        InventoryNote(String text,int x,int y) { this.text=text;this.x=x;this.y=y; }
    }
    private static final int[][] EQUIPMENT={{0,1,0},{1,0,1},{2,1,1},{13,2,1},{3,0,2},{4,1,2},{5,2,2},{7,1,3},{9,0,4},{10,1,4},{12,2,4}};
    private static final String[] BUCKET_NAMES={"Head alternatives","Cape alternatives","Neck alternatives","Weapon alternatives","Body alternatives","Off-hand alternatives","Leg alternatives","Glove alternatives","Boot alternatives","Ring alternatives","Ammunition","Tools","Food","Potions","Runes","Other supplies","Other","Other","Other","Needs repair / charging / review"};

    static int[] apply(Widget container,ItemManager manager,int columns,int pitch,int padding,SetupLoadout plan,
        Map<Integer,ItemCatalogueIndex.Rank> ranks,List<Heading> headings)
    { return apply(container,manager,columns,pitch,padding,plan,ranks,headings,null,new ArrayList<>()); }

    static int[] apply(Widget container,ItemManager manager,int columns,int pitch,int padding,SetupLoadout plan,
        Map<Integer,ItemCatalogueIndex.Rank> ranks,List<Heading> headings,SetupInventoryWidgets copies,List<InventoryNote> notes)
    {
        headings.clear();notes.clear();
        if(plan==null) return apply(container,manager,columns,pitch,padding);
        Widget[] children=container.getDynamicChildren(); if(children==null) return null;
        List<Widget> cells=new ArrayList<>(); Map<Integer,Widget> byId=new HashMap<>(); int oldEnd=0;
        for(Widget cell:children) if(cell!=null && !cell.isSelfHidden() && cell.getItemId()>=0 && cell.getItemId()!=6512
            && cell.getOriginalWidth()==36 && cell.getOriginalHeight()==32) {
            cells.add(cell);byId.putIfAbsent(manager.canonicalize(cell.getItemId()),cell);oldEnd=Math.max(oldEnd,cell.getOriginalY()+36);
        }
        if(cells.isEmpty() && !plan.hasBudgetExamples()) return null;
        List<Widget> runes=new ArrayList<>();
        for(Widget cell:cells) if(bucket(manager.canonicalize(cell.getItemId()),manager,plan.activity)==14) runes.add(cell);
        runes.sort(Comparator.comparingInt(Widget::getItemId));
        Set<Widget> placed=Collections.newSetFromMap(new IdentityHashMap<>());
        Set<Map<Integer,SetupLoadout.Field>> shownInventories=new HashSet<>();
        int y=padding;
        for(SetupLoadout section:plan.sections()) {
            if(plan.sectionCount()>1 || section.notEnoughGear()) {
                String title=section.sectionName.isEmpty()?section.activity:section.sectionName;
                headings.add(new Heading(title+(section.notEnoughGear()?" - not enough gear in bank":""),51,y+12));y+=18;
            }
            y=placeSection(container,manager,columns,pitch,y,section,byId,placed,headings,copies,notes,shownInventories.add(section.inventoryExample),runes);
            runes=Collections.emptyList();
        }
        Map<String,List<Widget>> extras=new TreeMap<>();
        for(Widget cell:cells) if(!placed.contains(cell)) {
            int id=manager.canonicalize(cell.getItemId()),bucket=bucket(id,manager,plan.activity);
            var definition=manager.getItemComposition(id);
            Set<String> roles=definition==null?Collections.emptySet():RaidRecommendations.layoutGroups(plan.activity,
                new AutoCategories.Item(id,definition.getName(),bucket<11,definition.getInventoryActions(),0,0,0));
            if(roles.isEmpty()) extras.computeIfAbsent(String.format(Locale.ROOT,"%02d:%s",bucket,BUCKET_NAMES[Math.min(19,bucket)]),k->new ArrayList<>()).add(cell);
            else for(String role:roles) extras.computeIfAbsent("11:"+role,k->new ArrayList<>()).add(cell);
        }
        for(var entry:extras.entrySet()) {
            List<Widget> group=entry.getValue();
            group.sort(Comparator.comparingInt((Widget cell)-> {
                var rank=ranks.get(manager.canonicalize(cell.getItemId()));return rank==null || rank.tier<=0?Integer.MAX_VALUE:rank.tier;
            }).thenComparingInt(Widget::getItemId));
            String heading=entry.getKey().substring(3);
            headings.add(new Heading(heading,51,y+12));y+=18;
            String roomNote=RaidRecommendations.roomNote(plan.activity,heading);
            for(int i=0;i<group.size();i++) {
                Widget cell=group.get(i);
                if(!placed.add(cell)) {
                    if(copies==null) continue;
                    cell=copies.add(container,cell,cell.getItemId(),manager);
                }
                place(cell,51+(i%columns)*pitch,y+(i/columns)*36);
                if(!roomNote.isEmpty()) notes.add(new InventoryNote(roomNote,51+(i%columns)*pitch,y+(i/columns)*36));
            }
            y+=((group.size()+columns-1)/columns)*36+8;
        }
        return new int[]{oldEnd,y};
    }
    private static int placeSection(Widget container,ItemManager manager,int columns,int pitch,int padding,SetupLoadout plan,
        Map<Integer,Widget> byId,Set<Widget> placed,List<Heading> headings,SetupInventoryWidgets copies,List<InventoryNote> notes,boolean showInventory,List<Widget> runes)
    {
        Map<Integer,Widget> chosen=new HashMap<>();
        for(var entry:plan.equipment.entrySet()) for(int id:entry.getValue()) if(byId.containsKey(id)) { chosen.put(entry.getKey(),byId.get(id));break; }
        // A missing weapon's budget example may need different ammunition than
        // the prayer-slot filler chosen while there was no owned weapon.
        if(plan.equipmentExample.containsKey(13)) {
            chosen.remove(13);
            Widget ammo=byId.get(plan.equipmentExample.get(13));if(ammo!=null) chosen.put(13,ammo);
        }
        // Void's benefit requires the whole outfit. Do not mix a partial Void
        // selection into an otherwise ordinary equipment diagram.
        boolean fullVoid=true;
        for(int slot:new int[]{0,4,7,9}) if(!isVoid(chosen.get(slot),manager)) fullVoid=false;
        if(!fullVoid) for(int slot:new int[]{0,4,7,9}) if(isVoid(chosen.get(slot),manager)) {
            chosen.remove(slot);
            for(int id:plan.equipment.getOrDefault(slot,Collections.emptyList())) if(byId.containsKey(id) && !isVoid(byId.get(id),manager)) { chosen.put(slot,byId.get(id));break; }
        }
        Widget weapon=chosen.get(3);
        boolean twoHanded=false;
        Integer exampleWeapon=plan.equipmentExample.get(3);
        if(weapon!=null || exampleWeapon!=null) {
            var stats=manager.getItemStats(weapon==null?exampleWeapon:manager.canonicalize(weapon.getItemId()));
            if(stats!=null && stats.getEquipment()!=null && stats.getEquipment().isTwoHanded()) { chosen.remove(5);twoHanded=true; }
        }
        boolean showEquipment=!plan.equipmentName.isEmpty();
        int gearY=padding+18,gearEnd=showEquipment?gearY+5*36:gearY;
        int runeColumns=Math.min(columns,3),runeY=gearEnd+18;
        if(!runes.isEmpty()) gearEnd=runeY+((runes.size()+runeColumns-1)/runeColumns)*36;
        int inventoryY=showEquipment && columns<8 || !showEquipment && !runes.isEmpty()?gearEnd+18:gearY;
        int inventoryX=51+(showEquipment && columns>=8?4*pitch:0);
        if(showEquipment) headings.add(new Heading("Equipment",51,padding+12));
        headings.add(new Heading(plan.inventoryExample.isEmpty()?"No Wiki inventory example":showInventory?"Recommended inventory":"Shared inventory above",inventoryX,inventoryY-6));
        for(int[] position:EQUIPMENT) {
            Widget cell=chosen.get(position[0]);
            int col=position[1]%columns,row=position[2]+position[1]/columns;
            int x=51+col*pitch,y=gearY+row*36;
            if(cell!=null) {
                String note=plan.equipmentNotes.getOrDefault(manager.canonicalize(cell.getItemId()),"");
                if(!placed.add(cell)) {
                    if(copies==null) continue;
                    cell=copies.add(container,cell,cell.getItemId(),manager);
                }
                place(cell,x,y);
                if(!note.isEmpty()) notes.add(new InventoryNote(note,x,y));
            } else if(showEquipment) {
                boolean offhand=position[0]==5 && twoHanded;
                Integer example=offhand?null:plan.equipmentExample.get(position[0]);
                String label=offhand?"2H":slotLabel(position[0]);
                if(example!=null && copies!=null) {
                    place(copies.add(container,null,example,manager),x,y);
                    notes.add(new InventoryNote("Budget gear suggestion: no matching usable item selected from your bank. Check equip requirements; this is not owned stock or a new guide rank.",x,y));
                } else {
                    headings.add(new Heading(label,x,y,true));
                    notes.add(new InventoryNote(offhand?"Off-hand unavailable with the selected two-handed weapon.":"No suitable ready owned item for "+label.toLowerCase(Locale.ROOT)+". Check ownership, charges and setup requirements.",x,y));
                }
            }
        }
        if(!runes.isEmpty()) {
            headings.add(new Heading("Suggested runes",51,runeY-6));
            for(int i=0;i<runes.size();i++) {
                Widget rune=runes.get(i);
                if(!placed.add(rune)) { if(copies==null) continue;rune=copies.add(container,rune,rune.getItemId(),manager); }
                int x=51+i%runeColumns*pitch,y=runeY+i/runeColumns*36;
                place(rune,x,y);
                notes.add(new InventoryNote("Runes listed for this activity. Choose those needed for your spellbook, spell and rune pouch; this is not a required combined rune loadout.",x,y));
            }
        }
        int inventoryColumns=Math.min(4,columns),inventoryEnd=inventoryY;
        for(var entry:plan.inventoryExample.entrySet()) {
            if(!showInventory) break;
            Integer id=plan.inventory.get(entry.getKey());
            Widget source=id==null?null:byId.get(id),cell=source;
            if(source==null || !placed.add(source)) {
                if(copies==null) continue;
                cell=copies.add(container,source,entry.getValue().ids.get(0),manager);
            }
            int position=entry.getKey(),x=inventoryX+(position%inventoryColumns)*pitch;
            int y=inventoryY+(position/inventoryColumns)*36;
            place(cell,x,y);inventoryEnd=Math.max(inventoryEnd,y+36);
            String note=entry.getValue().note;
            if(id!=null && !entry.getValue().ids.contains(id))
                note=InventoryPotionDoses.known(id)?InventoryPotionDoses.note(id):SetupEquipmentChoice.inventoryAlternativeNote(entry.getValue().ids);
            if(source==null) {
                if(!entry.getValue().guideOnly) note="Guide example: no ready bank item for this slot. Check ownership, charges and requirements."+(note.isEmpty()?"":" "+note);
            }
            else if(!note.isEmpty()) note="Showing an owned alternative; live prices are not compared. "+note;
            if(source!=null) {
                var definition=manager.getItemComposition(id);
                if(definition!=null) {
                    String state=ItemVariantState.annotate("Item state",new AutoCategories.Item(id,definition.getName(),false,
                        definition.getInventoryActions(),0,0,0));
                    if(!state.equals("Item state")) note+=(note.isEmpty()?"":" ")+state;
                    String use=ItemVariantState.inventoryUseNote(new AutoCategories.Item(id,definition.getName(),false,
                        definition.getInventoryActions(),0,0,0));
                    if(!use.isEmpty()) note+=(note.isEmpty()?"":" ")+use;
                }
            }
            if(!note.isEmpty()) notes.add(new InventoryNote(note,x,y));
        }
        return Math.max(gearEnd,inventoryEnd)+8;
    }
    private static String slotLabel(int slot) {
        switch(slot) {
            case 0:return "Head";case 1:return "Cape";case 2:return "Neck";case 3:return "Main";
            case 4:return "Body";case 5:return "Off";case 7:return "Legs";case 9:return "Hands";
            case 10:return "Feet";case 12:return "Ring";default:return "Ammo";
        }
    }
    private static boolean isVoid(Widget widget,ItemManager manager) {
        if(widget==null) return false;
        var definition=manager.getItemComposition(manager.canonicalize(widget.getItemId()));
        return definition!=null && definition.getName()!=null && definition.getName().toLowerCase(Locale.ROOT).contains("void");
    }
    private static void place(Widget cell,int x,int y) { cell.setOriginalX(x);cell.setOriginalY(y);cell.revalidate(); }

    static boolean activityPreset(AutoCategories.Preset preset)
    {
        return preset==AutoCategories.Preset.SKILLING || preset==AutoCategories.Preset.SKILLING_OUTFITS
            || preset==AutoCategories.Preset.COMBAT_GROUPS || preset==AutoCategories.Preset.CHAMBERS_OF_XERIC
            || preset==AutoCategories.Preset.THEATRE_OF_BLOOD || preset==AutoCategories.Preset.TOMBS_OF_AMASCUT
            || preset==AutoCategories.Preset.WIKI_ACTIVITY_SETUP;
    }

    static int bucket(int id,ItemManager manager)
    { return bucket(id,manager,""); }
    private static int bucket(int id,ItemManager manager,String activity)
    {
        var definition=manager.getItemComposition(id);
        if(definition==null) return 19;
        var stats=manager.getItemStats(id);
        String name=definition.getName();
        AutoCategories.Item item=new AutoCategories.Item(id,name,stats!=null && stats.isEquipable(),definition.getInventoryActions(),0,0,0);
        boolean reviewedSupply=!activity.isEmpty() && ActivitySetups.forActivity(activity).stream().anyMatch(s->
            SetupLoadout.inventoryTable(s) && (s.itemIds.contains(id) || InventoryPotionDoses.listed(s.itemIds,item))
                && !ItemVariantState.needsInventoryReview(item,s) && SetupItemRequirements.reviewReason(s.id,item).isEmpty());
        if(ItemVariantState.needsActivityReview(item) && !reviewedSupply) return 19;
        if("Tools".equals(ItemTaxonomy.weaponRole(ItemVariantState.familyName(item)))
            || WikiCatalogue.entries(item).stream().anyMatch(e -> e.group.equals("Shared tools") || e.group.endsWith("— tools"))) return 11;
        if(stats!=null && stats.getEquipment()!=null) {
            int slot=stats.getEquipment().getSlot();
            for(int i=0;i<SLOTS.length;i++) if(SLOTS[i]==slot) return i;
        }
        if(item.action("Eat")) return 12;
        if(item.action("Drink")) return 13;
        if(item.name.endsWith(" rune")) return 14;
        return 15;
    }

    static int[] apply(Widget container,ItemManager manager,int columns,int pitch,int padding)
    {
        Widget[] children=container.getDynamicChildren();
        if(children==null || manager==null) return null;
        List<Widget> cells=new ArrayList<>();
        for(Widget child:children) if(child!=null && !child.isSelfHidden() && child.getItemId()>=0
            && child.getItemId()!=6512 && child.getOriginalWidth()==36 && child.getOriginalHeight()==32) cells.add(child);
        if(cells.isEmpty()) return null;
        cells.sort(Comparator.comparingInt(Widget::getOriginalY).thenComparingInt(Widget::getOriginalX));
        Map<Integer,List<Widget>> groups=new TreeMap<>();
        int oldEnd=0;
        Map<Integer,Integer> buckets=new HashMap<>();
        for(Widget cell:cells) {
            oldEnd=Math.max(oldEnd,cell.getOriginalY()+36);
            int id=manager.canonicalize(cell.getItemId());
            groups.computeIfAbsent(buckets.computeIfAbsent(id,k -> bucket(k,manager)),k -> new ArrayList<>()).add(cell);
        }
        int y=padding;
        for(List<Widget> group:groups.values()) {
            for(int i=0;i<group.size();i++) {
                Widget cell=group.get(i);
                cell.setOriginalX(51+(i%columns)*pitch);
                cell.setOriginalY(y+(i/columns)*36);
                cell.revalidate();
            }
            y+=((group.size()+columns-1)/columns)*36;
        }
        return new int[]{oldEnd,y};
    }
}
