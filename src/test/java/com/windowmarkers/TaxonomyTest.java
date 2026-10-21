package com.windowmarkers;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class TaxonomyTest
{
    private static AutoCategories.Item item(int id,String name)
    { return new AutoCategories.Item(id,name,true,new String[]{"Eat","Drink"},200,0,0); }
    private static final AutoCategories.Preset[] RAIDS={AutoCategories.Preset.CHAMBERS_OF_XERIC,AutoCategories.Preset.THEATRE_OF_BLOOD,AutoCategories.Preset.TOMBS_OF_AMASCUT};
    private static boolean marked(Map<String,List<AutoCategories.Item>> groups,int id)
    { return groups.values().stream().flatMap(Collection::stream).anyMatch(i -> i.id==id); }

    @Test public void weaponRolesBeatMisleadingStatsAndItemNames()
    {
        for (String name : Arrays.asList("Ancient sceptre","Ice ancient sceptre","Twinflame staff","Toktz-mej-tal","Void knight mace","Tumeken's shadow","Trident of the swamp","Warped sceptre"))
        { assertEquals(name,"Magic weapons",AutoCategories.category(item(1,name),true)); }
        assertEquals("Ranged weapons",ItemTaxonomy.weaponRole("eclipse atlatl"));
        assertEquals("Hybrid weapons",ItemTaxonomy.weaponRole("black salamander"));
        assertEquals("Melee weapons",ItemTaxonomy.weaponRole("ancient mace"));
        assertEquals("Tools",ItemTaxonomy.weaponRole("dragon pickaxe"));
        assertEquals("Melee weapons",ItemTaxonomy.weaponRole("dragon battleaxe"));
    }

    @Test public void everyBarrowsAndMoonWeaponBelongsWithItsArmour()
    {
        String[][] pairs={{"Ahrim's robetop","Ahrim's staff"},{"Dharok's platebody","Dharok's greataxe"},{"Guthan's chainskirt","Guthan's warspear"},{"Karil's leathertop","Karil's crossbow"},{"Torag's platelegs","Torag's hammers"},{"Verac's brassard","Verac's flail"},{"Blood moon chestplate","Dual macuahuitl"},{"Blue moon tassets","Blue moon spear"},{"Eclipse moon helm","Eclipse atlatl"}};
        for (String[] pair : pairs)
        {
            assertNotNull(pair[0],ItemTaxonomy.equipmentSet(pair[0]));
            assertEquals(pair[1],ItemTaxonomy.equipmentSet(pair[0]),ItemTaxonomy.equipmentSet(pair[1]));
        }
        for (int condition : new int[]{100,75,50,25,0})
        { assertEquals(ItemTaxonomy.equipmentSet("guthan's warspear"),ItemTaxonomy.equipmentSet("Guthan's warspear "+condition)); }
        assertNull(ItemTaxonomy.equipmentSet("Guthan's armour set"));
        assertNull(ItemTaxonomy.equipmentSet("Void knight mace"));
        assertNull(ItemTaxonomy.equipmentSet("Crystal pickaxe"));
        assertNull(ItemTaxonomy.equipmentSet("Atlatl dart"));
        assertNull(ItemTaxonomy.equipmentSet("Toktz-mej-tal"));
    }

    @Test public void unrelatedFoodAndPotionsNeverLeakIntoAnyRaid()
    {
        String[] excluded={"Dwarven rock cake","Rock cake","Blighted anglerfish","Blighted manta ray","Blighted super restore(4)","Purple sweets","Sailing brew","Fisherman's brew","Zamorak brew(4)","Attack potion(4)","Fishing potion(4)","Agility potion(4)","Hunter potion(4)","Compost potion(4)","Super antifire potion(4)","Antifire potion(4)","Combat potion(4)","Energy potion(4)","Weapon poison(++)","Beer","Basket","Saradomin brew(0)","Super combat potion mix(2)","Mind rune","Body rune","Blood moon armour set","Tumeken's shadow ornament kit"};
        List<AutoCategories.Item> items=new ArrayList<>();
        for (int i=0;i<excluded.length;i++) { items.add(item(i,excluded[i])); }
        for (AutoCategories.Preset raid : RAIDS)
        { assertTrue(raid.toString(),AutoCategories.suggest(raid,items).isEmpty()); }
    }

    @Test public void armourAccessoriesAndWeaponsAreIncludedForEveryRaid()
    {
        List<AutoCategories.Item> bank=Arrays.asList(item(1,"Torva platebody"),item(2,"Bandos tassets"),item(3,"Virtus robe top"),item(4,"Ancestral robe bottom"),item(5,"Occult necklace"),item(6,"Primordial boots"),item(7,"Super restore(2)"),item(8,"Saradomin brew(4)"));
        for (AutoCategories.Preset raid : RAIDS)
        {
            Map<String,List<AutoCategories.Item>> groups=AutoCategories.suggest(raid,bank);
            for (AutoCategories.Item i : bank) { assertTrue(raid+" missing "+i.name,marked(groups,i.id)); }
        }
        assertTrue(marked(AutoCategories.suggest(AutoCategories.Preset.TOMBS_OF_AMASCUT,Arrays.asList(item(10,"Karil's leathertop 75"),item(11,"Masori body (f)"))),10));
        assertFalse(marked(AutoCategories.suggest(AutoCategories.Preset.THEATRE_OF_BLOOD,Arrays.asList(item(10,"Karil's leathertop 75"))),10));
    }

    @Test public void incompleteSetsAndBrokenGearAreNotDefaultRecommendations()
    {
        List<AutoCategories.Item> bank=new ArrayList<>(Arrays.asList(item(1,"Elite void top"),item(2,"Elite void robe"),item(3,"Void ranger helm"),item(4,"Ancestral robe top (broken)")));
        Map<String,List<AutoCategories.Item>> groups=AutoCategories.suggest(AutoCategories.Preset.THEATRE_OF_BLOOD,bank);
        assertTrue(groups.containsKey("ToB Incomplete Void set — review"));
        assertFalse(RaidRecommendations.defaultSelected("ToB Incomplete Void set — review"));
        assertFalse(RaidRecommendations.defaultSelected("ToB Needs repair/charge — review"));
        bank.add(item(5,"Void knight gloves"));
        assertEquals(4,AutoCategories.suggest(AutoCategories.Preset.THEATRE_OF_BLOOD,bank).get("ToB Elite Void switches — choose helm for role").size());
    }

    @Test public void variantsAreConservativeAndPotionsRetainTheirIdentity()
    {
        assertTrue(ItemTaxonomy.matches("Holy sanguinesti staff","Sanguinesti staff"));
        assertTrue(ItemTaxonomy.matches("Bow of faerdhinen (c)","Bow of faerdhinen"));
        assertTrue(ItemTaxonomy.matches("Twisted ancestral robe top","Ancestral robe top"));
        assertTrue(ItemTaxonomy.matches("Imbued zamorak cape","God capes"));
        assertTrue(ItemTaxonomy.matches("Saradomin d'hide body","Blessed body"));
        assertFalse(ItemTaxonomy.matches("Berserker ring","Berserker ring (i)"));
        assertFalse(ItemTaxonomy.matches("Ancient sceptre ornament kit","Ancient sceptre"));
        assertEquals("saradomin brew",ItemTaxonomy.baseName("Saradomin brew(3)"));
        assertEquals("blighted super restore",ItemTaxonomy.baseName("Blighted super restore(4)"));
        assertNull(RaidRecommendations.supplyGroup("tob","Sanfew serum(4)"));
        assertNotNull(RaidRecommendations.supplyGroup("cox","Sanfew serum(4)"));
        assertNull(RaidRecommendations.supplyGroup("toa","Stamina potion(4)"));
    }

    @Test public void sharedAccessoriesDoNotBecomeMeleeBecauseItsScoreIsHigher()
    {
        AutoCategories.Item gloves=new AutoCategories.Item(-1,"Barrows gloves",true,null,24,12,6);
        assertTrue(AutoCategories.category(gloves,true).startsWith("Hybrid equipment"));
        AutoCategories.Item drink=new AutoCategories.Item(-2,"Fisherman's brew",false,new String[]{"Drink"},0,0,0);
        assertFalse(AutoCategories.category(drink,false).startsWith("Combat"));
    }
}
