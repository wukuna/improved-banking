package com.windowmarkers;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class RaidGearFilterTest
{
    private AutoCategories.Item item(int id,String name) { return new AutoCategories.Item(id,name,true,null,0,0,0); }
    private Map<String,List<AutoCategories.Item>> filtered(AutoCategories.Preset preset,int[] levels,AutoCategories.Item... items)
    {
        Map<String,List<AutoCategories.Item>> groups=AutoCategories.suggest(preset,Arrays.asList(items));
        RaidGearFilter.filter(preset,groups,levels,new LinkedHashMap<>()); return groups;
    }
    private boolean has(Map<String,List<AutoCategories.Item>> groups,int id)
    { return groups.values().stream().flatMap(Collection::stream).anyMatch(i -> i.id==id); }
    @Test public void coxMagicRemovesLowerOwnedHatButNotOtherSlotsOrSpecialists()
    {
        Map<String,List<AutoCategories.Item>> result=filtered(AutoCategories.Preset.CHAMBERS_OF_XERIC,new int[]{99,99,99,99,99,99},
            item(-1,"Ancestral hat"),item(-2,"Mystic hat (dark)"),item(-3,"Mystic robe top"),item(-4,"Virtus mask"),item(-5,"Ahrim's hood"));
        assertFalse(has(result,-2)); for(int id:new int[]{-1,-3,-4,-5}) { assertTrue(has(result,id)); }
    }
    @Test public void replacementMustMeetActualLevelsAndBeUsable()
    {
        assertTrue(has(filtered(AutoCategories.Preset.CHAMBERS_OF_XERIC,new int[]{99,99,64,99,99,99},item(-1,"Ancestral hat"),item(-2,"Mystic hat")),-2));
        assertTrue(has(filtered(AutoCategories.Preset.CHAMBERS_OF_XERIC,new int[]{99,99,99,99,74,99},item(-1,"Ancestral hat"),item(-2,"Mystic hat")),-2));
        assertTrue(has(filtered(AutoCategories.Preset.CHAMBERS_OF_XERIC,new int[]{99,99,99,99,99,99},item(-1,"Ancestral hat (broken)"),item(-2,"Mystic hat")),-2));
    }
    @Test public void allRaidsCompareSameSlotArmourAcrossCombatStyles()
    {
        for(AutoCategories.Preset preset:new AutoCategories.Preset[]{AutoCategories.Preset.CHAMBERS_OF_XERIC,AutoCategories.Preset.THEATRE_OF_BLOOD,AutoCategories.Preset.TOMBS_OF_AMASCUT})
        {
            Map<String,List<AutoCategories.Item>> result=filtered(preset,new int[]{99,99,99,99,99,99},item(-1,"Torva platebody"),item(-2,"Bandos chestplate"),item(-3,"Bandos tassets"));
            assertFalse(preset.toString(),has(result,-2)); assertTrue(has(result,-3));
        }
        Map<String,List<AutoCategories.Item>> ranged=filtered(AutoCategories.Preset.CHAMBERS_OF_XERIC,new int[]{99,99,99,99,99,99},item(-1,"Masori body (f)"),item(-2,"Armadyl chestplate"));
        assertFalse(has(ranged,-2));
    }
    @Test public void generalClassificationDoesNotPretendToBeAnActivityRanking()
    {
        assertTrue(has(filtered(AutoCategories.Preset.ALL_CATEGORIES,new int[]{99,99,99,99,99,99},item(-1,"Ancestral hat"),item(-2,"Mystic hat")),-2));
    }
    @Test public void weaponsAreComparedOnlyWithinInterchangeableRoles()
    {
        int[] max={99,99,99,99,99,99,99};
        for(AutoCategories.Preset preset:new AutoCategories.Preset[]{AutoCategories.Preset.CHAMBERS_OF_XERIC,AutoCategories.Preset.THEATRE_OF_BLOOD})
        {
            Map<String,List<AutoCategories.Item>> result=filtered(preset,max,item(-1,"Abyssal tentacle"),item(-2,"Abyssal whip"));
            assertFalse(has(result,-2)); assertTrue(has(result,-1));
        }
        for(AutoCategories.Preset preset:new AutoCategories.Preset[]{AutoCategories.Preset.CHAMBERS_OF_XERIC,AutoCategories.Preset.TOMBS_OF_AMASCUT})
        {
            Map<String,List<AutoCategories.Item>> result=filtered(preset,max,item(-1,"Trident of the swamp"),item(-2,"Trident of the seas"),item(-3,"Dragon crossbow"),item(-4,"Rune crossbow"));
            assertFalse(has(result,-2)); assertFalse(has(result,-4));
        }
    }
    @Test public void differentWeaponEffectsAndSpecialAttackRolesStayAvailable()
    {
        int[] max={99,99,99,99,99,99,99};
        Map<String,List<AutoCategories.Item>> result=filtered(AutoCategories.Preset.CHAMBERS_OF_XERIC,max,item(-1,"Tumeken's shadow"),item(-2,"Sanguinesti staff"),item(-3,"Twisted bow"),item(-4,"Toxic blowpipe"),item(-5,"Dragon hunter lance"),item(-6,"Scythe of vitur"));
        for(int id=-1;id>=-6;id--) { assertTrue("Missing separate role "+id,has(result,id)); }
        result=filtered(AutoCategories.Preset.THEATRE_OF_BLOOD,max,item(-1,"Eye of Ayak"),item(-2,"Kodai wand"),item(-3,"Ancient sceptre"),item(-4,"Dragon warhammer"));
        for(int id=-1;id>=-4;id--) { assertTrue(has(result,id)); }
    }
    @Test public void accessoryUpgradesCheckHitpointsAndKeepBloodFury()
    {
        for(AutoCategories.Preset preset:new AutoCategories.Preset[]{AutoCategories.Preset.CHAMBERS_OF_XERIC,AutoCategories.Preset.THEATRE_OF_BLOOD,AutoCategories.Preset.TOMBS_OF_AMASCUT})
        {
            AutoCategories.Item[] bank={item(-1,"Amulet of rancour"),item(-2,"Amulet of torture"),item(-3,"Amulet of blood fury"),item(-4,"Necklace of rupture"),item(-5,"Necklace of anguish"),item(-6,"Avernic defender"),item(-7,"Dragon defender")};
            Map<String,List<AutoCategories.Item>> result=filtered(preset,new int[]{99,99,99,99,99,99,99},bank);
            assertFalse(has(result,-2)); assertTrue(has(result,-3)); assertFalse(has(result,-5)); assertFalse(has(result,-7));
            result=filtered(preset,new int[]{99,99,99,99,99,99,89},bank);
            assertTrue(has(result,-2)); assertTrue(has(result,-5));
        }
    }
    @Test public void ringUnlocksAreRequiredAndLightbearerNeverReplacesDamageRing()
    {
        List<AutoCategories.Item> bank=Arrays.asList(item(-1,"Ultor ring"),item(-2,"Berserker ring (i)"),item(-3,"Lightbearer"));
        Map<String,List<AutoCategories.Item>> groups=AutoCategories.suggest(AutoCategories.Preset.CHAMBERS_OF_XERIC,bank);
        RaidGearFilter.filter(AutoCategories.Preset.CHAMBERS_OF_XERIC,groups,new int[]{99,99,99,99,99,99,99},false,new HashMap<>());
        assertTrue(has(groups,-2));
        RaidGearFilter.filter(AutoCategories.Preset.CHAMBERS_OF_XERIC,groups,new int[]{99,99,99,99,99,99,99},true,new HashMap<>());
        assertFalse(has(groups,-2)); assertTrue(has(groups,-3));
    }
    @Test public void weaponAndOffhandRequirementsAndSeparateRolesAreRespected()
    {
        assertTrue(has(filtered(AutoCategories.Preset.CHAMBERS_OF_XERIC,new int[]{99,99,99,99,77,99,99},item(-1,"Trident of the swamp"),item(-2,"Trident of the seas")),-2));
        assertTrue(has(filtered(AutoCategories.Preset.TOMBS_OF_AMASCUT,new int[]{99,99,99,99,99,79,99},item(-1,"Elidinis' ward (f)"),item(-2,"Elidinis' ward")),-2));
        Map<String,List<AutoCategories.Item>> groups=filtered(AutoCategories.Preset.CHAMBERS_OF_XERIC,new int[]{99,99,99,99,99,99,99},item(-1,"Occult necklace"),item(-2,"Amulet of fury"));
        assertFalse(groups.get("CoX Magic").stream().anyMatch(i -> i.id==-2));
        assertTrue(groups.get("CoX Melee").stream().anyMatch(i -> i.id==-2));
        assertTrue(groups.get("CoX Ranged").stream().anyMatch(i -> i.id==-2));
    }
}
