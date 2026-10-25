package com.windowmarkers;

import java.util.*;
import net.runelite.api.widgets.Widget;
import net.runelite.client.game.ItemManager;

/** A display-only arrangement. Widget item IDs, bank indexes and actions stay native. */
final class SetupItemLayout
{
    private static final int[] SLOTS={0,1,2,3,4,5,7,9,10,12,13};

    static boolean activityPreset(AutoCategories.Preset preset)
    {
        return preset==AutoCategories.Preset.SKILLING || preset==AutoCategories.Preset.SKILLING_OUTFITS
            || preset==AutoCategories.Preset.COMBAT_GROUPS || preset==AutoCategories.Preset.CHAMBERS_OF_XERIC
            || preset==AutoCategories.Preset.THEATRE_OF_BLOOD || preset==AutoCategories.Preset.TOMBS_OF_AMASCUT
            || preset==AutoCategories.Preset.FRACTURED_ARCHIVE || preset==AutoCategories.Preset.WIKI_ACTIVITY_SETUP;
    }

    static int bucket(int id,ItemManager manager)
    {
        var definition=manager.getItemComposition(id);
        if(definition==null) return 19;
        var stats=manager.getItemStats(id);
        String name=definition.getName();
        AutoCategories.Item item=new AutoCategories.Item(id,name,stats!=null && stats.isEquipable(),definition.getInventoryActions(),0,0,0);
        if(ItemVariantState.needsActivityReview(item)) return 19;
        if(stats!=null && stats.getEquipment()!=null) {
            int slot=stats.getEquipment().getSlot();
            for(int i=0;i<SLOTS.length;i++) if(SLOTS[i]==slot) return i;
        }
        if("Tools".equals(ItemTaxonomy.weaponRole(ItemVariantState.familyName(item)))
            || WikiCatalogue.entries(item).stream().anyMatch(e -> e.group.equals("Shared tools") || e.group.endsWith("— tools"))) return 11;
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
