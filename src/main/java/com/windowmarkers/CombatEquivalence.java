package com.windowmarkers;

import java.util.HashMap;
import java.util.Map;

/** Reviewed combat roles only; never a universal alias for utility, quests or clues. */
final class CombatEquivalence
{
    private static final Map<Integer,String> FAMILIES=new HashMap<>();
    private static final Map<Integer,Boolean> MAX_REQUIREMENTS=new HashMap<>();
    static
    {
        for (String resource:new String[]{"combat-equivalences.tsv","cosmetic-combat-equivalences.tsv"})
        for (String[] row:ItemTaxonomy.readRows(resource,3))
        {
            int id=Integer.parseInt(row[0]);
            if (FAMILIES.put(id,row[1])!=null || (!row[2].equals("true") && !row[2].equals("false")))
            { throw new IllegalStateException("Invalid combat equivalence: "+id); }
            MAX_REQUIREMENTS.put(id,Boolean.parseBoolean(row[2]));
        }
    }
    static String familyName(AutoCategories.Item item)
    { return FAMILIES.getOrDefault(item.id,ItemVariantState.familyName(item)); }
    static boolean requiresMaxedSkills(AutoCategories.Item item)
    { return MAX_REQUIREMENTS.getOrDefault(item.id,false); }
    static boolean matches(AutoCategories.Item item,String expected)
    {
        String family=familyName(item);
        if (family.equals("imbued ancient cape") && (expected.equals("god capes") || expected.equals("imbued god cape")))
        { return true; }
        return ItemTaxonomy.matches(family,expected);
    }
    static boolean matchesAny(AutoCategories.Item item,String... expected)
    { for (String name:expected) { if (matches(item,name)) { return true; } } return false; }
}
