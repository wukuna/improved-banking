package com.windowmarkers;

import java.util.*;

/** Exact owned-item membership in one explicitly selected Wiki example. */
final class ActivitySetups
{
    static final class Setup
    {
        final String id, activity, label, source, notes;
        final Set<Integer> itemIds=new HashSet<>();
        Setup(String[] row)
        { id=row[0]; activity=row[1]; label=row[2]; source=row[3]; notes=row[4]; }
        @Override public String toString() { return label; }

        Map<String,List<AutoCategories.Item>> suggest(List<AutoCategories.Item> owned)
        {
            Map<String,List<AutoCategories.Item>> result=new LinkedHashMap<>();
            for (AutoCategories.Item item:owned)
            {
                if (!itemIds.contains(item.id)) { continue; }
                String group=ItemVariantState.needsActivityReview(item)
                    ? ItemVariantState.reviewGroup(item) : label+" — setup review";
                AutoCategories.add(result,activity+" — "+ItemVariantState.annotate(group,item),item);
            }
            return result;
        }
    }
    private static final Map<String,Setup> SETUPS=new LinkedHashMap<>();
    static
    {
        for (String[] row:ItemTaxonomy.readRows("activity-setups.tsv",5))
        {
            if (SETUPS.put(row[0],new Setup(row))!=null) { throw new IllegalStateException("Duplicate activity setup"); }
        }
        for (String[] row:ItemTaxonomy.readRows("activity-setup-items.tsv",2))
        {
            Setup setup=SETUPS.get(row[0]);
            if (setup==null) { throw new IllegalStateException("Missing activity setup"); }
            setup.itemIds.add(Integer.parseInt(row[1]));
        }
    }
    static List<String> activities(String search)
    {
        String needle=search.trim().toLowerCase(Locale.ROOT);
        Set<String> result=new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (Setup setup:SETUPS.values())
        { if (setup.activity.toLowerCase(Locale.ROOT).contains(needle)) { result.add(setup.activity); } }
        return new ArrayList<>(result);
    }
    static List<Setup> all() { return new ArrayList<>(SETUPS.values()); }
    static List<Setup> forActivity(String activity)
    {
        List<Setup> result=new ArrayList<>();
        for (Setup setup:SETUPS.values()) { if (setup.activity.equals(activity)) { result.add(setup); } }
        return result;
    }
}
