package com.windowmarkers;

import java.util.HashMap;
import java.util.Map;

/** Exact identity state is separate from category membership and activity suitability. */
final class ItemVariantState
{
    private enum State { AVAILABLE, REPAIR, CHARGE, EMPTY, REDUCED, CHECK, PREPARE, CONTENTS, RESTRICTED }
    private static final Map<Integer,ItemVariantState> STATES=new HashMap<>();
    private static final Map<Integer,String> UNVERIFIED_IDENTITIES=new HashMap<>();
    static
    {
        for (String[] row:ItemTaxonomy.readRows("identity-review-required.tsv",2))
        { UNVERIFIED_IDENTITIES.put(Integer.parseInt(row[0]),row[1]); }
        for (String[] row : ItemTaxonomy.readRows("item-variant-states.tsv",5))
        {
            int id=Integer.parseInt(row[0]);
            if (!row[3].equals("true") && !row[3].equals("false"))
            { throw new IllegalStateException("Invalid variant review policy"); }
            if (STATES.put(id,new ItemVariantState(row[1],State.valueOf(row[2]),Boolean.parseBoolean(row[3]),row[4]))!=null)
            { throw new IllegalStateException("Duplicate variant ID: "+id); }
        }
    }
    private final String family;
    private final State state;
    private final boolean activityReview;
    private final String note;

    private ItemVariantState(String family,State state,boolean activityReview,String note)
    { this.family=family; this.state=state; this.activityReview=activityReview; this.note=note; }

    static String familyName(AutoCategories.Item item)
    {
        ItemVariantState known=STATES.get(item.id);
        return known==null ? item.name : known.family;
    }

    static boolean needsActivityReview(AutoCategories.Item item)
    {
        if (identityNeedsReview(item)) { return true; }
        if (WikiCatalogue.hasFacet(item,"restricted")) { return true; }
        ItemVariantState known=STATES.get(item.id);
        return known==null ? ItemTaxonomy.needsRepair(item.name) || ItemTaxonomy.needsCharge(item.name) : known.activityReview;
    }

    static boolean needsActivityReview(AutoCategories.Item item,String role)
    {
        // The inactive Arclight keeps Darklight's special attack. This exception
        // does not grant it the charged weapon's demon-damage role or ranking.
        if (item.id==30305 && role.startsWith("Special attacks")) { return false; }
        return needsActivityReview(item);
    }

    static String reviewGroup(AutoCategories.Item item)
    {
        if (identityNeedsReview(item)) { return "Unverified item identity — review"; }
        if (WikiCatalogue.hasFacet(item,"restricted")) { return "Activity restrictions — review"; }
        ItemVariantState known=STATES.get(item.id);
        if (known!=null && known.state==State.CHECK)
        { return known.note.contains("charge") ? "Check charges — review" : "Check item use — review"; }
        if (known!=null && known.state==State.REDUCED) { return "Reduced function — review"; }
        if (known!=null && known.state==State.PREPARE) { return "Preparation needed — review"; }
        if (known!=null && known.state==State.CONTENTS) { return "Check contents — review"; }
        if (known!=null && known.state==State.EMPTY) { return "Empty — review"; }
        if (known!=null && known.state==State.RESTRICTED) { return "Activity restrictions — review"; }
        return "Needs repair/charge — review";
    }

    static String annotate(String group,AutoCategories.Item item)
    {
        if (group==null) { return null; }
        ItemVariantState known=STATES.get(item.id);
        // Keep the item's factual category, but do not mix depleted and usable states.
        if (known!=null && !known.note.isEmpty()) { return group+" ("+known.note+")"; }
        return group;
    }

    static boolean identityNeedsReview(AutoCategories.Item item)
    { return item.name.equals(UNVERIFIED_IDENTITIES.get(item.id)); }
}
