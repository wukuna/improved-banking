package com.windowmarkers;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class InventoryPairingTest
{
    private static Map<Integer,ItemCatalogueIndex.Item> highTierBank()
    {
        Map<Integer,ItemCatalogueIndex.Item> bank=new HashMap<>();
        for(ItemCatalogueIndex.Item item:Arrays.asList(
            GeSearchIndexTest.item(20997,"Twisted bow",3),
            GeSearchIndexTest.item(27275,"Tumeken's shadow",3),
            GeSearchIndexTest.item(21018,"Ancestral hat",0),
            GeSearchIndexTest.item(21021,"Ancestral robe top",4),
            GeSearchIndexTest.item(21024,"Ancestral robe bottom",7),
            GeSearchIndexTest.item(12002,"Occult necklace",2),
            GeSearchIndexTest.item(27238,"Masori body (f)",4),
            GeSearchIndexTest.item(27241,"Masori chaps (f)",7),
            GeSearchIndexTest.item(22325,"Scythe of Vitur",3),
            GeSearchIndexTest.item(6685,"Saradomin brew(4)",-1),
            GeSearchIndexTest.item(3024,"Super restore(4)",-1))) bank.put(item.id,item);
        return bank;
    }

    @Test public void selectedExampleKeepsItsPairedInventoryEvenWhenOtherGearIsOwned()
    {
        Map<Integer,ItemCatalogueIndex.Item> bank=highTierBank();
        for(String[] pair:new String[][]{
            {"364045:4","Minimum (0-100 Invocation)"},
            {"94147:4","Minimum"},
            {"271073:4","Intermediate"},
            {"117586:9","Learner/Void"},
            {"117586:3","Entry Mode"},
            {"117586:4","Learner/Void"},
            {"117586:5","Melee Role"},
            {"117586:7","Mage/Freezer Role"},
            {"117586:8","Range Role"}})
        {
            SetupLoadout plan=SetupLoadout.build(Collections.singletonList(ActivitySetups.byId(pair[0])),bank,bank.keySet(),"");
            assertNotNull(pair[0],plan);
            assertTrue(plan.inventoryName,plan.inventoryName.startsWith(pair[1]+" "));
        }
    }

    @Test public void pairsStayWithinTheSameActivityAndUseRealInventoryTables()
    {
        for(String[] pair:ItemTaxonomy.readRows("setup-inventory-pairs.tsv",2))
        {
            ActivitySetups.Setup gear=ActivitySetups.byId(pair[0]),inventory=ActivitySetups.byId(pair[1]);
            assertNotNull(pair[0],gear);assertNotNull(pair[1],inventory);
            assertEquals(pair[0],gear.activity,inventory.activity);
            assertFalse(SetupLoadout.inventoryTable(gear));assertTrue(SetupLoadout.inventoryTable(inventory));
        }
    }

    @Test public void explicitInventoryChoiceStillOverridesThePairedExample()
    {
        Map<Integer,ItemCatalogueIndex.Item> bank=highTierBank();
        SetupLoadout plan=SetupLoadout.build(Collections.singletonList(ActivitySetups.byId("364045:4")),bank,bank.keySet(),"364045:13");
        assertTrue(plan.inventoryName,plan.inventoryName.startsWith("Max/Expert+"));
        assertEquals(28,plan.inventoryExample.size());
    }

    @Test public void raidInventoriesRetainWikiEmptySlotsAndRepeatedSupplies()
    {
        for(String[] expected:new String[][]{
            {"117586:3","117586:1","20"},
            {"117586:9","117586:13","28"},
            {"117586:10","117586:15","28"},
            {"117586:11","117586:17","28"},
            {"117586:12","117586:19","28"},
            {"364045:4","364045:9","27"},
            {"364045:5","364045:10","28"},
            {"364045:6","364045:11","28"},
            {"364045:7","364045:12","28"},
            {"364045:8","364045:13","28"},
            {"271073:4","271073:6","28"},
            {"271073:5","271073:7","28"},
            {"94147:4","94147:9","28"},
            {"94147:5","94147:10","28"},
            {"94147:6","94147:11","28"},
            {"94147:7","94147:12","28"},
            {"94147:8","94147:13","28"}})
        {
            SetupLoadout plan=SetupLoadout.build(Collections.singletonList(ActivitySetups.byId(expected[0])),Collections.emptyMap(),Collections.emptySet(),expected[1]);
            assertNotNull(expected[0],plan);
            assertEquals(expected[1],Integer.parseInt(expected[2]),plan.inventoryExample.size());
        }
    }

    @Test public void unbankableGuideItemsFillTheirSlotsWithoutBecomingOwnedOrSearchable()
    {
        for(String[] pair:new String[][]{{"269949:1","269949:2"},{"460329:1","460329:6"},
            {"469947:2","469947:4"},{"469947:2","469947:5"},{"346655:1","346655:1"}})
        {
            Map<Integer,ItemCatalogueIndex.Item> impossibleBank=new HashMap<>();
            for(String[] row:ItemTaxonomy.readRows("setup-inventory-previews.tsv",6))
                impossibleBank.put(Integer.parseInt(row[2]),GeSearchIndexTest.item(Integer.parseInt(row[2]),row[3],-1));
            SetupLoadout plan=SetupLoadout.build(Collections.singletonList(ActivitySetups.byId(pair[0])),impossibleBank,impossibleBank.keySet(),pair[1]);
            assertNotNull(pair[1],plan);
            for(String[] row:ItemTaxonomy.readRows("setup-inventory-previews.tsv",6)) if(row[0].equals(pair[1]))
            {
                int position=Integer.parseInt(row[1])-1,id=Integer.parseInt(row[2]);
                SetupLoadout.Field field=plan.inventoryExample.get(position);
                assertNotNull(row[0]+":"+position,field);assertTrue(field.guideOnly);
                assertTrue(field.note.startsWith("Cannot be banked."));assertEquals(Collections.singletonList(id),field.ids);
                assertFalse(plan.inventory.containsKey(position));
                assertFalse(ActivitySetups.byId(pair[1]).itemIds.contains(id));
            }
        }
    }

    @Test public void inventoryOnlyActivityDoesNotRequireAnEquipmentTable()
    {
        SetupLoadout plan=SetupLoadout.build(ActivitySetups.forActivity("Tithe Farm"),Collections.emptyMap(),Collections.emptySet(),"");
        assertNotNull(plan);assertTrue(plan.equipmentName.isEmpty());assertTrue(plan.equipment.isEmpty());
        assertEquals(11,plan.inventoryExample.size());
        assertTrue(plan.inventoryExample.get(0).guideOnly);
    }
}
