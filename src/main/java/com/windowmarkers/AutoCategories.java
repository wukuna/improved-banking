package com.windowmarkers;

import java.util.*;

/** Local, reviewable facts. Activity suitability is never inferred from Eat/Drink or stat totals. */
final class AutoCategories
{
    enum Preset
    {
        ALL_CATEGORIES("All classifications"), BROAD_TYPES("Broad item types"),
        SKILLING("Skilling — outfits, tools, boosts and materials"), SKILLING_OUTFITS("Skilling outfits only"),
        COMBAT_GROUPS("General combat and equipment sets"), FOOD_AND_POTIONS("Food, potions and restricted supplies"),
        TRAVEL_AND_UTILITY("Travel, storage and utility"), QUESTS_AND_CLUES("Quest and clue items"), COSMETICS("Cosmetic and utility equipment"),
        RAID_MEGA_RARES("Raid mega-rare weapons"),
        CHAMBERS_OF_XERIC("Chambers of Xeric — Wiki gear and supplies"), THEATRE_OF_BLOOD("Theatre of Blood — Wiki gear and supplies"), TOMBS_OF_AMASCUT("Tombs of Amascut — Wiki gear and supplies"),
        FRACTURED_ARCHIVE("The Fractured Archive — provisional Wiki setups"),
        WIKI_ACTIVITY_SETUP("Choose a Wiki activity and setup…");
        private final String label;
        Preset(String label) { this.label=label; }
        @Override public String toString() { return label; }
    }
    static final class Item
    {
        final int id; final String name; final boolean equipment; final String[] actions;
        final int melee, ranged, magic;
        Item(int id,String name,boolean equipment,String[] actions,int melee,int ranged,int magic)
        { this.id=id; this.name=name.toLowerCase(Locale.ROOT); this.equipment=equipment; this.actions=actions; this.melee=melee; this.ranged=ranged; this.magic=magic; }
        boolean action(String action) { return actions!=null && Arrays.stream(actions).anyMatch(action::equalsIgnoreCase); }
    }
    static Map<String,List<Item>> suggest(Preset preset,List<Item> items)
    {
        Map<String,List<Item>> groups=new LinkedHashMap<>();
        if (preset==Preset.WIKI_ACTIVITY_SETUP) { return groups; }
        if (preset!=Preset.RAID_MEGA_RARES && preset!=Preset.CHAMBERS_OF_XERIC && preset!=Preset.THEATRE_OF_BLOOD && preset!=Preset.TOMBS_OF_AMASCUT && preset!=Preset.FRACTURED_ARCHIVE)
        {
            for (Item item : items)
            {
                if (ItemVariantState.identityNeedsReview(item))
                {
                    if (preset==Preset.BROAD_TYPES || preset==Preset.ALL_CATEGORIES)
                    { add(groups,ItemVariantState.reviewGroup(item),item); }
                    continue;
                }
                boolean standalone=standaloneSetWeapon(item,items);
                String group;
                if (preset==Preset.BROAD_TYPES || preset==Preset.ALL_CATEGORIES) { group=standalone ? ItemTaxonomy.weaponRole(ItemVariantState.familyName(item)) : category(item,true); }
                else if (preset==Preset.COMBAT_GROUPS)
                {
                    group=WikiCatalogue.category(item,preset);
                    String set=ItemTaxonomy.equipmentSet(ItemVariantState.familyName(item)), role=ItemTaxonomy.weaponRole(ItemVariantState.familyName(item));
                    if (set!=null && !standalone) { group=set; }
                    else if (role!=null && !role.equals("Tools")) { group=role; }
                    else if ("Tools".equals(role)) { group=null; }
                    else if (group==null && item.equipment && !WikiCatalogue.hasFacet(item,"outfit") && !WikiCatalogue.hasFacet(item,"cosmetic") && !WikiCatalogue.hasFacet(item,"skilling"))
                    { group="Other — equipment role needs review"; }
                }
                else { group=WikiCatalogue.category(item,preset); }
                if (group!=null) { add(groups,ItemVariantState.annotate(group,item),item); }
            }
            return groups;
        }
        if (preset==Preset.RAID_MEGA_RARES)
        {
            for (Item item : items) if (ItemTaxonomy.matchesAny(item,"twisted bow","scythe of vitur","tumeken's shadow"))
            { add(groups,ItemVariantState.needsActivityReview(item) ? ItemVariantState.reviewGroup(item) : "Raid mega-rare weapons",item); }
            return groups;
        }
        return RaidRecommendations.suggest(preset,items);
    }
    private static boolean standaloneSetWeapon(Item item,List<Item> bank)
    {
        String set=ItemTaxonomy.equipmentSet(ItemVariantState.familyName(item)),role=ItemTaxonomy.weaponRole(ItemVariantState.familyName(item));
        if(set==null || role==null || role.equals("Tools")) { return false; }
        return bank.stream().noneMatch(other -> set.equals(ItemTaxonomy.equipmentSet(ItemVariantState.familyName(other)))
            && ItemTaxonomy.weaponRole(ItemVariantState.familyName(other))==null
            && !ItemTaxonomy.baseName(ItemVariantState.familyName(other)).equals("berserker necklace"));
    }
    static String category(Item item,boolean combat)
    {
        String n=ItemTaxonomy.baseName(ItemVariantState.familyName(item));
        String set=ItemTaxonomy.equipmentSet(n);
        if (set!=null) { return set+(ItemTaxonomy.needsRepair(item.name) ? " — repair needed" : ""); }
        if (n.contains("rock cake") || n.equals("locator orb")) { return "Health-lowering utility"; }
        if (n.startsWith("blighted ")) { return "Restricted-use supplies — blighted"; }
        if (n.equals("purple sweets")) { return "Specialist food — purple sweets"; }
        String catalogue=WikiCatalogue.category(item,Preset.ALL_CATEGORIES);
        String weapon=ItemTaxonomy.weaponRole(n);
        if (weapon!=null && !weapon.equals("Tools")) { return combat ? weapon : "Equipment"; }
        if (catalogue!=null && !catalogue.startsWith("Equipment —") && !catalogue.startsWith("Cosmetic/utility equipment")) { return catalogue; }
        String role=ItemTaxonomy.weaponRole(n);
        if (role!=null) { return combat || role.equals("Tools") ? role : "Equipment"; }
        if (catalogue!=null) { return catalogue; }
        if (item.equipment)
        {
            if (!combat) { return "Equipment"; }
            // Different styles use different units; never compare their summed bonuses.
            int styles=(item.magic>0 ? 1 : 0)+(item.ranged>0 ? 1 : 0)+(item.melee>0 ? 1 : 0);
            if (styles>1) { return "Hybrid equipment"; }
            if (item.magic>0) { return "Magic equipment"; }
            if (item.ranged>0) { return "Ranged equipment"; }
            if (item.melee>0) { return "Melee equipment"; }
            return "Defensive or utility equipment — review";
        }
        if (n.endsWith(" rune")) { return "Runes"; }
        if (item.action("Eat")) { return "Food — general, not activity advice"; }
        if (item.action("Drink"))
        {
            if (n.matches(".*(?:potion|serum|antifire|antidote|antipoison|antivenom|anti-venom).*") || n.equals("saradomin brew") || n.equals("zamorak brew") || n.equals("super restore"))
            { return "Potions — general, not activity advice"; }
            return "Drinks and brews — review";
        }
        if (n.endsWith(" ore") || n.endsWith(" logs") || n.equals("logs") || n.endsWith(" seed") || n.startsWith("grimy ") || n.endsWith(" bar") || n.equals("coal") || n.equals("pure essence") || n.equals("rune essence")) { return "Resources"; }
        return "Other — review";
    }
    static void add(Map<String,List<Item>> groups,String group,Item item)
    {
        List<Item> list=groups.computeIfAbsent(group,k -> new ArrayList<>());
        if (list.stream().noneMatch(i -> i.id==item.id)) { list.add(item); }
    }
}
