package com.windowmarkers;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class CatalogueTest
{
    private static AutoCategories.Item item(int id,String name)
    { return new AutoCategories.Item(id,name,false,null,0,0,0); }
    private static String category(String name,AutoCategories.Preset preset)
    { return WikiCatalogue.category(item(-1,name),preset); }

    @Test public void toolsCoverDistinctSkillsAndAreNotSupplies()
    {
        String[][] cases={{"Cooking gauntlets","Cooking"},{"Magic secateurs","Farming"},{"Box trap","Hunter"},
            {"Lobster pot","Fishing"},{"Bronze pickaxe","Mining"},{"Bones","Prayer"},{"Oak plank","Construction"},
            {"Coal bag","Mining"},{"Small pouch","Runecraft"}};
        for(String[] c:cases)
        {
            String group=category(c[0],AutoCategories.Preset.SKILLING);
            assertNotNull(c[0],group); assertTrue(c[0]+": "+group,group.startsWith(c[1]));
            assertNull(c[0],category(c[0],AutoCategories.Preset.FOOD_AND_POTIONS));
        }
        assertNotNull(category("Raw shark",AutoCategories.Preset.SKILLING));
        assertNull(category("Iron helm",AutoCategories.Preset.SKILLING));
    }
    @Test public void outfitsDoNotAdmitAdjacentRewardsOrUpgradeMaterials()
    {
        for(String n:Arrays.asList("Warm gloves","Abyssal red dye","Rogue kit"))
        { assertNull(n,category(n,AutoCategories.Preset.SKILLING_OUTFITS)); }
        assertNotNull(category("Smiths gloves (i)",AutoCategories.Preset.SKILLING_OUTFITS));
    }
    @Test public void everyOutfitIncludesAllPiecesAndAlternativeFarmerBodies()
    {
        String[] families={
            "graceful hood|graceful top|graceful legs|graceful gloves|graceful boots|graceful cape",
            "carpenter's helmet|carpenter's shirt|carpenter's trousers|carpenter's boots",
            "farmer's strawhat|farmer's jacket|farmer's shirt|farmer's boro trousers|farmer's boots",
            "pyromancer hood|pyromancer garb|pyromancer robe|pyromancer boots",
            "angler hat|angler top|angler waders|angler boots",
            "spirit angler headband|spirit angler top|spirit angler waders|spirit angler boots",
            "guild hunter headwear|guild hunter top|guild hunter legs|guild hunter boots",
            "prospector helmet|prospector jacket|prospector legs|prospector boots",
            "golden prospector helmet|golden prospector jacket|golden prospector legs|golden prospector boots",
            "zealot's helm|zealot's robe top|zealot's robe bottom|zealot's boots",
            "hat of the eye|robe top of the eye|robe bottoms of the eye|boots of the eye",
            "smiths tunic|smiths trousers|smiths gloves|smiths boots",
            "rogue mask|rogue top|rogue trousers|rogue gloves|rogue boots",
            "lumberjack hat|lumberjack top|lumberjack legs|lumberjack boots",
            "forestry hat|forestry top|forestry legs|forestry boots",
            "prescription goggles|alchemist labcoat|alchemist pants|alchemist gloves"};
        for(String family:families)
        {
            String first=null;
            for(String piece:family.split("\\|"))
            {
                String group=category(piece,AutoCategories.Preset.SKILLING_OUTFITS);
                assertNotNull(piece,group);
                if(first==null) { first=group; } else { assertEquals(piece,first,group); }
            }
        }
    }
    @Test public void utilitiesAndClueItemsRemainDiscoverable()
    {
        assertNotNull(category("Ring of dueling(8)",AutoCategories.Preset.TRAVEL_AND_UTILITY));
        assertNotNull(category("Coal bag",AutoCategories.Preset.TRAVEL_AND_UTILITY));
        assertNotNull(category("Clue scroll (master)",AutoCategories.Preset.QUESTS_AND_CLUES));
        assertNotNull(category("Iron pickaxe",AutoCategories.Preset.QUESTS_AND_CLUES));
        assertNull(category("Imaginary item",AutoCategories.Preset.SKILLING));
        assertEquals("Other — review",AutoCategories.category(item(-1,"Imaginary item"),true));
    }
    @Test public void actualItemIdsResolveAndOutfitNamesDoNotRequireStats()
    {
        assertTrue(WikiCatalogue.hasFacet(item(1351,"Unknown localized name"),"skilling"));
        assertTrue(category("Graceful hood",AutoCategories.Preset.SKILLING).startsWith("Agility"));
    }
    @Test public void worseToolsRequireOwnedUsableUpgradeAndRetainDistinctUses()
    {
        List<AutoCategories.Item> bank=Arrays.asList(item(-1,"Bronze pickaxe"),item(-2,"Dragon pickaxe"),
            item(-3,"Black pickaxe"),item(-4,"Iron pickaxe"),item(-5,"Infernal pickaxe"),item(-6,"Dharok's greataxe"));
        assertTrue(LowerTierFilter.exclusions(bank.subList(0,2),1,99,99).isEmpty());
        Map<Integer,String> hidden=LowerTierFilter.exclusions(bank,61,99,99);
        assertTrue(hidden.containsKey(-1));
        for(int id:new int[]{-2,-3,-4,-5,-6}) { assertFalse(hidden.containsKey(id)); }
        assertTrue(LowerTierFilter.exclusions(Collections.singletonList(item(-1,"Bronze pickaxe")),99,99,99).isEmpty());
    }
}
