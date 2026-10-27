package com.windowmarkers;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class CatalogueTest
{
    @Test public void ammunitionAndTemporaryQuestModelsDoNotBecomeWearableGear() {
        for(int id:new int[]{2,21728}) {
            AutoCategories.Item ammo=item(id,id==2?"Steel cannonball":"Granite cannonball");
            assertTrue(WikiCatalogue.entries(ammo).stream().anyMatch(e -> e.group.equals("Ammo — cannon")));
            assertFalse(WikiCatalogue.entries(ammo).stream().anyMatch(e -> e.group.equals("Ranged weapons") || e.group.equals("Equipment — weapons")));
        }
        AutoCategories.Item quest=item(24265,"V's shield");
        assertFalse(WikiCatalogue.entries(quest).stream().anyMatch(e -> e.group.contains("equipment — shield") || e.group.equals("Equipment — shield")));
        assertEquals(-1,ItemCatalogueIndex.fallbackSlot(24265,"V's shield"));
        assertEquals(5,ItemCatalogueIndex.fallbackSlot(24266,"V's shield"));
        String[] pieces={"head","body","legs","tail","gloves","feet"};
        for(int n=0;n<pieces.length;n++) {
            String name="Snow imp costume "+pieces[n];
            assertFalse(WikiCatalogue.entries(item(21841+n,name)).stream().anyMatch(e -> e.group.startsWith("Equipment — ")));
            assertEquals(-1,ItemCatalogueIndex.fallbackSlot(21841+n,name));
            assertTrue(WikiCatalogue.entries(item(21847+2*n,name)).stream().anyMatch(e -> e.group.startsWith("Equipment — ")));
        }
    }

    @Test public void refreshedUtilityRolesPreservePartnersAndDoNotBecomeRaidSupplies() {
        for(int id:new int[]{10890,1718,24725}) {
            AutoCategories.Item i=item(id,"test");
            for(String skill:new String[]{"Agility","Thieving","Woodcutting"})
                assertTrue(WikiCatalogue.entries(i).stream().anyMatch(e -> e.group.equals(skill+" — poison cure (book + symbol + Prayer)")));
            assertFalse(WikiCatalogue.hasFacet(i,"supply"));
        }
        for(int id:new int[]{7534,7535}) assertTrue(WikiCatalogue.entries(item(id,"test")).stream().anyMatch(e -> e.group.equals("Sailing — diving pair (helmet + apparatus)")));
        assertTrue(WikiCatalogue.hasFacet(item(33881,"Wyrmscraig boots"),"cosmetic"));
        assertTrue(WikiCatalogue.entries(item(34658,"Archive key")).isEmpty());
    }
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
    @Test public void wikiSupplementRetainsSharedToolRolesAndMissingMaterials()
    {
        assertTrue(WikiCatalogue.entries(item(2347,"Hammer")).stream().anyMatch(e -> e.group.equals("Smithing — tools")));
        assertTrue(WikiCatalogue.entries(item(2347,"Hammer")).stream().anyMatch(e -> e.group.equals("Construction — tools")));
        assertTrue(WikiCatalogue.entries(item(946,"Knife")).stream().anyMatch(e -> e.group.equals("Cooking — tools")));
        assertNotNull(WikiCatalogue.category(item(1733,"Needle"),AutoCategories.Preset.SKILLING));
        assertEquals("Crafting — materials",WikiCatalogue.category(item(1734,"Thread"),AutoCategories.Preset.SKILLING));
        assertNotNull(WikiCatalogue.category(item(590,"Tinderbox"),AutoCategories.Preset.SKILLING));
    }
    @Test public void categoryMembershipDoesNotMistakeASeedDibberForSeeds()
    {
        assertFalse(WikiCatalogue.entries(item(5343,"Seed dibber")).stream().anyMatch(e -> e.group.equals("Farming — seeds and planting materials")));
        assertTrue(WikiCatalogue.hasFacet(item(5343,"Seed dibber"),"skilling"));
    }
    @Test public void organizationalRolesCannotGrantRaidEligibility()
    {
        List<AutoCategories.Item> tools=Arrays.asList(item(2347,"Hammer"),item(1734,"Thread"),item(5343,"Seed dibber"));
        for(AutoCategories.Preset raid:Arrays.asList(AutoCategories.Preset.CHAMBERS_OF_XERIC,
            AutoCategories.Preset.THEATRE_OF_BLOOD,AutoCategories.Preset.TOMBS_OF_AMASCUT))
        {
            for(AutoCategories.Item tool:tools) { assertNull(WikiCatalogue.category(tool,raid)); }
        }
        assertFalse(AutoCategories.suggest(AutoCategories.Preset.THEATRE_OF_BLOOD,
            Collections.singletonList(item(1734,"Thread"))).values().stream().anyMatch(items -> !items.isEmpty()));
    }
    @Test public void everySupplementRowUsesAnExactIdentityAndNeverBecomesARaidRule()
    {
        Set<String> facets=new HashSet<>(Arrays.asList("skilling","boost","utility","travel","supply","quest","clue","cosmetic","combat","restricted"));
        Set<String> ids=new HashSet<>();
        for(String[] row:ItemTaxonomy.readRows("wiki-category-supplement.tsv",3))
        {
            assertTrue(row[0],row[0].matches("I:[0-9]+"));
            assertTrue(row[1],facets.contains(row[1]));
            if(!ids.add(row[0])) { continue; }
            AutoCategories.Item unknownName=item(Integer.parseInt(row[0].substring(2)),"Unknown item name");
            for(AutoCategories.Preset raid:Arrays.asList(AutoCategories.Preset.CHAMBERS_OF_XERIC,
                AutoCategories.Preset.THEATRE_OF_BLOOD,AutoCategories.Preset.TOMBS_OF_AMASCUT))
            {
                assertNull(row[0],WikiCatalogue.category(unknownName,raid));
                // A separately reviewed variant ID can resolve to an explicit raid-table item.
                // Category membership alone still cannot establish that identity or eligibility.
                if(CombatEquivalence.familyName(unknownName).equals(unknownName.name))
                { assertTrue(row[0],AutoCategories.suggest(raid,Collections.singletonList(unknownName)).isEmpty()); }
            }
        }
    }
    @Test public void nonCombatMaterialsKeepTheirUseWithoutBecomingRaidGear()
    {
        AutoCategories.Item[] materials={item(4821,"Black nails"),item(21338,"Amethyst bolt tips"),
            item(2307,"Bread dough"),item(9463,"Adamant crossbow (u)"),item(9429,"Adamantite limbs"),
            item(5496,"Apple sapling"),item(21469,"Teak seedling"),item(21504,"Giant seaweed"),
            item(21512,"Bird house"),item(6040,"Amulet of Nature")};
        for(AutoCategories.Item material:materials)
        {
            assertNotNull(material.name,WikiCatalogue.category(material,AutoCategories.Preset.SKILLING));
            for(AutoCategories.Preset raid:Arrays.asList(AutoCategories.Preset.CHAMBERS_OF_XERIC,
                AutoCategories.Preset.THEATRE_OF_BLOOD,AutoCategories.Preset.TOMBS_OF_AMASCUT))
            { assertTrue(material.name,AutoCategories.suggest(raid,Collections.singletonList(material)).isEmpty()); }
        }
        assertNull(WikiCatalogue.category(item(9463,"Adamant crossbow (u)"),AutoCategories.Preset.COMBAT_GROUPS));
        assertNull(WikiCatalogue.category(item(9429,"Adamantite limbs"),AutoCategories.Preset.COMBAT_GROUPS));
        assertTrue(WikiCatalogue.entries(item(21504,"Giant seaweed")).stream()
            .anyMatch(e -> e.group.equals("Crafting — glassmaking materials")));
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

    @Test public void ammoComponentsAndUnfinishedGrapplesCannotInheritReadyEquipmentRoles()
    {
        for(AutoCategories.Item component:Arrays.asList(item(21350,"Amethyst arrowtips"),
            item(11876,"Unfinished broad bolts"),item(30998,"Atlatl dart tips"),item(9418,"Mith grapple")))
        {
            assertNotNull(component.name,WikiCatalogue.category(component,AutoCategories.Preset.SKILLING));
            assertNull(component.name,WikiCatalogue.category(component,AutoCategories.Preset.COMBAT_GROUPS));
            assertFalse(component.name,WikiCatalogue.entries(component).stream().anyMatch(e -> e.group.startsWith("Ammunition —")));
        }
        assertFalse(WikiCatalogue.hasFacet(item(9418,"Mith grapple"),"cosmetic"));
        assertTrue(WikiCatalogue.hasFacet(item(9419,"Mith grapple"),"cosmetic"));
        assertTrue(ItemVariantState.needsActivityReview(item(9418,"Mith grapple")));
        assertFalse(ItemVariantState.needsActivityReview(item(9419,"Mith grapple")));
        assertTrue(WikiCatalogue.entries(item(9143,"Adamant bolts")).stream()
            .anyMatch(e -> e.group.equals("Ammunition — bolts (match weapon)")));
    }
    @Test public void deferredWeaponCategoriesAreAbsentAndModeRestrictionsRemain() {
        assertTrue(WikiCatalogue.hasFacet(item(13307,"Blood money"),"restricted"));
        assertTrue(WikiCatalogue.entries(item(34681,"Ascension crossbows")).isEmpty());
        for(int id:new int[]{34715,34717})
            assertTrue(WikiCatalogue.entries(item(id,id==34715?"Obligator":"Obligator (uncharged)")).isEmpty());
        assertTrue(WikiCatalogue.entries(item(34059,"Fairy tale quest lamp")).isEmpty());
    }
}
