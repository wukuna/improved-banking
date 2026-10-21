package com.windowmarkers;

import java.util.*;

/** Per-role substitutions only; never removes an item from its other raid uses. */
final class RaidGearFilter
{
    private static final List<String[]> RANKS=ItemTaxonomy.readRows("raid-ranks.tsv",5);
    private static final List<String[]> REQUIREMENTS=ItemTaxonomy.readRows("raid-wear-requirements.tsv",8);
    static void filter(AutoCategories.Preset preset,Map<String,List<AutoCategories.Item>> groups,int[] levels,Map<Integer,String> exclusions)
    { filter(preset,groups,levels,false,exclusions); }
    static void filter(AutoCategories.Preset preset,Map<String,List<AutoCategories.Item>> groups,int[] levels,boolean ancientRingsUnlocked,Map<Integer,String> exclusions)
    {
        if(preset!=AutoCategories.Preset.CHAMBERS_OF_XERIC && preset!=AutoCategories.Preset.THEATRE_OF_BLOOD && preset!=AutoCategories.Preset.TOMBS_OF_AMASCUT) { return; }
        String raid=RaidRecommendations.raid(preset);
        for(Map.Entry<String,List<AutoCategories.Item>> group:groups.entrySet())
        {
            String style=group.getKey().contains("Magic") ? "Magic" : group.getKey().contains("Ranged") ? "Ranged" : group.getKey().endsWith("Melee") ? "Melee" : "";
            if(style.isEmpty()) { continue; }
            List<AutoCategories.Item> candidates=new ArrayList<>(group.getValue());
            group.getValue().removeIf(item -> {
                if(protectedUse(item)) { return false; }
                for(String[] lower:RANKS)
                {
                    if(!lower[0].equals(raid) || !lower[1].equals(style) || !ItemTaxonomy.matches(item.name,lower[4])) { continue; }
                    for(String[] higher:RANKS)
                    {
                        if(!higher[0].equals(raid) || !higher[1].equals(style) || !higher[2].equals(lower[2]) || Integer.parseInt(higher[3])>=Integer.parseInt(lower[3])) { continue; }
                        for(AutoCategories.Item upgrade:candidates)
                        {
                            if(upgrade.id==item.id || protectedUse(upgrade) || !ItemTaxonomy.matches(upgrade.name,higher[4]) || !sameRole(lower[2],item,upgrade) || !canWear(upgrade,levels,ancientRingsUnlocked)) { continue; }
                            String reason=group.getKey()+": "+item.name+" → "+upgrade.name+" (Wiki "+lower[2].toLowerCase(Locale.ROOT)+" ranking)";
                            exclusions.merge(item.id,reason,(a,b) -> a+"; "+b); return true;
                        }
                    }
                }
                return false;
            });
        }
        groups.values().removeIf(List::isEmpty);
    }
    private static boolean canWear(AutoCategories.Item item,int[] levels,boolean ancientRingsUnlocked)
    {
        if(ItemTaxonomy.matchesAny(item.name,"ultor ring","magus ring","venator ring","bellator ring")) { return ancientRingsUnlocked; }
        for(String[] row:REQUIREMENTS)
        {
            if(!ItemTaxonomy.matches(item.name,row[0])) { continue; }
            for(int i=0;i<7;i++) { if((i<levels.length ? levels[i] : 1)<Integer.parseInt(row[i+1])) { return false; } }
            return true;
        }
        return false;
    }
    private static boolean sameRole(String slot,AutoCategories.Item lower,AutoCategories.Item higher)
    {
        String n=ItemTaxonomy.baseName(lower.name);
        if(slot.contains("weapon"))
        {
            // Explicit substitutions, not a blanket comparison of a row containing multiple loadout roles.
            if(n.equals("abyssal whip")) { return ItemTaxonomy.matchesAny(higher.name,"abyssal tentacle","blade of saeldor"); }
            if(n.equals("trident of the seas")) { return ItemTaxonomy.matchesAny(higher.name,"trident of the swamp","sanguinesti staff"); }
            if(n.equals("warped sceptre")) { return ItemTaxonomy.matchesAny(higher.name,"trident of the seas","trident of the swamp","sanguinesti staff"); }
            if(n.equals("rune crossbow")) { return ItemTaxonomy.matchesAny(higher.name,"dragon crossbow","armadyl crossbow","zaryte crossbow","dragon hunter crossbow"); }
            return false;
        }
        if(slot.equals("Ring"))
        {
            return (n.equals("berserker ring (i)") && ItemTaxonomy.matches(higher.name,"ultor ring"))
                || (n.equals("seers ring (i)") && ItemTaxonomy.matches(higher.name,"magus ring"));
        }
        if(slot.equals("Shield"))
        {
            return (n.equals("dragon defender") && ItemTaxonomy.matches(higher.name,"avernic defender"))
                || (n.equals("elidinis' ward") && ItemTaxonomy.matches(higher.name,"elidinis' ward (f)"));
        }
        return true;
    }
    private static boolean protectedUse(AutoCategories.Item item)
    {
        String n=ItemTaxonomy.baseName(item.name);
        return ItemTaxonomy.needsRepair(item.name) || ItemTaxonomy.needsCharge(item.name)
            || ItemTaxonomy.equipmentSet(n)!=null || n.startsWith("virtus ") || n.startsWith("bloodbark ")
            || n.startsWith("oathplate ") || n.equals("serpentine helm") || n.contains("blood fury")
            || n.startsWith("mixed hide ");
    }
}
