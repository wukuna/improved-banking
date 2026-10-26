package com.windowmarkers;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class GeVariantAuditTest
{
    private ItemCatalogueIndex.Item item(int id,String name,int slot) { return GeSearchIndexTest.item(id,name,slot); }
    private Set<Integer> ids(ItemCatalogueIndex.Result result) {
        assertNotNull(result); Set<Integer> ids=new HashSet<>(); result.items.forEach(i->ids.add(i.id)); return ids;
    }
    @Test public void geCanBuyTheChargedBowRecommendationsWithoutChangingBankSetups()
    {
        Map<String,Set<Integer>> before=new HashMap<>();
        ActivitySetups.all().forEach(s->before.put(s.id,new HashSet<>(s.itemIds)));
        ItemCatalogueIndex index=new ItemCatalogueIndex(Arrays.asList(item(22547,"Craw's bow (u)",3),item(27652,"Webweaver bow (u)",3)));
        ItemCatalogueIndex.Result result=index.find("whisperer weapon","390886:2");
        assertEquals(new HashSet<>(Arrays.asList(22547,27652)),ids(result));
        for(int id:ids(result)) {
            assertEquals(0,result.ranks.get(id).tier);
            assertTrue(result.ranks.get(id).explanation.contains("activation ether"));
        }
        ActivitySetups.all().forEach(s->assertEquals(before.get(s.id),s.itemIds));
    }
    @Test public void enhancedCapacityIsDiscoverableButDoesNotPretendToIncludeCharges()
    {
        ItemCatalogueIndex index=new ItemCatalogueIndex(Arrays.asList(item(11905,"Trident of the Seas (full)",3),
            item(11908,"Uncharged trident",3),item(22290,"Uncharged trident (e)",3),item(12900,"Uncharged toxic trident",3),item(22294,"Uncharged toxic trident (e)",3)));
        ItemCatalogueIndex.Result result=index.find("fossil island wyverns weapon","");
        assertTrue(ids(result).contains(22294)); assertEquals(0,result.ranks.get(22294).tier);
        assertTrue(result.ranks.get(22294).explanation.contains("20,000"));
        result=index.find("toa magic weapon","");
        assertTrue(ids(result).containsAll(Arrays.asList(11905,11908,22290,12900,22294)));
        assertEquals(4,result.ranks.get(11905).tier);
        assertEquals(0,result.ranks.get(11908).tier);
    }
    @Test public void emptySkillingPurchaseNeedsFillingAndJewelleryKeepsChargeDistinctions()
    {
        ItemCatalogueIndex index=new ItemCatalogueIndex(Arrays.asList(item(5331,"Watering can",-1),
            item(11118,"Combat bracelet(4)",9),item(11972,"Combat bracelet(6)",9),item(11126,"Combat bracelet",9)));
        ItemCatalogueIndex.Result farm=index.find("tithe farm","");
        assertTrue(ids(farm).contains(5331)); assertEquals(0,farm.ranks.get(5331).tier);
        assertTrue(farm.ranks.get(5331).explanation.contains("Fill it before watering"));
        ItemCatalogueIndex.Result barrows=index.find("barrows","27247:1");
        assertTrue(ids(barrows).containsAll(Arrays.asList(11118,11972,11126)));
        assertTrue(barrows.ranks.get(11126).explanation.contains("no teleports"));
    }
    @Test public void brokenDegradedAndDamagedEquipmentCannotAcquireReadyGearColours()
    {
        ItemCatalogueIndex index=new ItemCatalogueIndex(Arrays.asList(item(4708,"Ahrim's hood",0),item(4860,"Ahrim's hood 0",0),
            item(26382,"Torva full helm",0),item(26376,"Torva full helm (damaged)",0)));
        ItemCatalogueIndex.Result magic=index.find("toa magic head","");
        assertEquals(3,magic.ranks.get(4708).tier); assertEquals(0,magic.ranks.get(4860).tier);
        ItemCatalogueIndex.Result melee=index.find("toa melee head","");
        assertTrue(ids(melee).contains(26376)); assertEquals(0,melee.ranks.get(26376).tier);
    }
    @Test public void fallbackSlotsCoverWeaponsHandsAndUnusableVersions()
    {
        assertEquals(3,ItemCatalogueIndex.fallbackSlot(11908,"Uncharged trident"));
        assertEquals(3,ItemCatalogueIndex.fallbackSlot(22481,"Sanguinesti staff (uncharged)"));
        assertEquals(9,ItemCatalogueIndex.fallbackSlot(26235,"Zaryte vambraces"));
        assertEquals(3,ItemCatalogueIndex.fallbackSlot(4866,"Ahrim's staff 0"));
        assertEquals(-1,ItemCatalogueIndex.fallbackSlot(11908,"Different future item"));
    }
    @Test public void functionalSlotsDoNotConfuseAmmunitionOrTwoHandedShieldsWithOtherRoles()
    {
        ItemCatalogueIndex index=new ItemCatalogueIndex(Arrays.asList(item(21015,"Dinh's bulwark",3),
            item(11230,"Dragon dart",3),item(25849,"Amethyst dart",3)));
        ItemCatalogueIndex.Result shields=index.find("sulliuscep shield","104707:1");
        assertTrue(ids(shields).contains(21015)); assertEquals(2,shields.ranks.get(21015).tier);
        assertTrue(shields.ranks.get(21015).explanation.contains("both hands"));
        assertEquals(0,index.find("sulliuscep weapon","104707:1").ranks.get(21015).tier);
        ItemCatalogueIndex.Result ammo=index.find("fight cave ammo","28467:1");
        assertTrue(ids(ammo).containsAll(Arrays.asList(11230,25849)));
        assertEquals(1,ammo.ranks.get(11230).tier); assertEquals(2,ammo.ranks.get(25849).tier);
        assertTrue(ammo.ranks.get(11230).explanation.contains("blowpipe"));
    }
    @Test public void changedPurchaseIdentityIsNotSubstitutedAndNoOffhandMeansNoShieldRank()
    {
        ItemCatalogueIndex index=new ItemCatalogueIndex(Collections.singletonList(item(22547,"Different future item",3)));
        assertFalse(ids(index.find("whisperer weapon","")).contains(22547));
        boolean actualShieldRemains=false;
        for(String[] row:ItemTaxonomy.readRows("activity-setup-ranks.tsv",5)) {
            assertFalse(row[0].equals("111819:1") && row[2].equals("shield") && row[3].equals("1"));
            if(row[0].equals("111819:1") && row[1].equals("21000") && row[2].equals("shield")) actualShieldRemains=true;
        }
        assertTrue(actualShieldRemains);
    }
    @Test public void completeWordTypoDoesNotPickUpUnrelatedMinigameTools()
    {
        ItemCatalogueIndex index=new ItemCatalogueIndex(Arrays.asList(item(11920,"Dragon pickaxe",3),
            item(590,"Tinderbox",-1),item(3678,"Flamtaer hammer",-1)));
        Set<Integer> expected=ids(index.find("mining tools",""));
        assertTrue(expected.contains(11920)); assertFalse(expected.contains(3678));
        assertEquals(expected,ids(index.find("minign tools","")));
    }
}
