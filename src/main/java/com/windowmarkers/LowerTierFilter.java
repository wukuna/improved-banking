package com.windowmarkers;

import java.util.*;

final class LowerTierFilter
{
    private static final String[] METALS={"bronze","iron","steel","black","mithril","adamant","rune","dragon"};
    private static final int[] LEVELS={1,1,6,11,21,31,41,61};

    static Map<Integer,String> exclusions(List<AutoCategories.Item> bank,int mining,int woodcutting,int fishing)
    {
        Map<Integer,String> result=new LinkedHashMap<>();
        tools(bank,result,"pickaxe",mining);
        tools(bank,result,"axe",woodcutting);

        return result;
    }
    private static void tools(List<AutoCategories.Item> bank,Map<Integer,String> result,String type,int skill)
    {
        AutoCategories.Item better=null; int best=-1;
        for (AutoCategories.Item i : bank)
        {
            int tier=tier(i.name,type);
            if (tier>best && LEVELS[tier]<=skill && usable(i.name) && !ItemVariantState.needsActivityReview(i)) { best=tier; better=i; }
        }
        if (better==null) { return; }
        for (AutoCategories.Item i : bank)
        {
            int tier=tier(i.name,type);
            if (tier<0 || tier>=best || !usable(i.name) || ItemVariantState.needsActivityReview(i)) { continue; }

            if (i.name.equals("black pickaxe")) { continue; }

            if (WikiCatalogue.hasFacet(i,"clue") || WikiCatalogue.hasFacet(i,"quest")) { continue; }
            result.put(i.id,i.name+" → "+better.name+" (usable "+type+" tier; special-use items retained)");
        }
    }
    private static boolean usable(String name)
    { return !ItemTaxonomy.needsRepair(name) && !ItemTaxonomy.needsCharge(name); }
    private static int tier(String name,String type)
    {

        for (int i=0;i<METALS.length;i++) { if (name.equals(METALS[i]+" "+type)) { return i; } }
        return -1;
    }
}
