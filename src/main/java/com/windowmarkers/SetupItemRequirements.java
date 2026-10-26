package com.windowmarkers;

import java.util.*;

/** Role-specific state restrictions; these do not remove an item's other uses. */
final class SetupItemRequirements
{
    private static final Map<String,Map<Integer,String[]>> REQUIREMENTS=new HashMap<>();
    static
    {
        for (String[] row:ItemTaxonomy.readRows("setup-item-requirements.tsv",5))
            REQUIREMENTS.computeIfAbsent(row[0],key->new HashMap<>()).put(Integer.parseInt(row[1]),row);
    }

    static String reviewReason(String setupId,AutoCategories.Item item)
    {
        String[] row=REQUIREMENTS.getOrDefault(setupId,Collections.emptyMap()).get(item.id);
        return row!=null && row[2].equalsIgnoreCase(item.name) ? row[3] : "";
    }
}
