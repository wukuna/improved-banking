package com.windowmarkers;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class RaidRoomLayoutTest
{
    @Test public void allDragonPickaxeCosmeticsMatchGuardiansButBrokenDoesNotBecomeReady()
    {
        List<ItemCatalogueIndex.Item> items=new ArrayList<>();
        for(int id:new int[]{11920,12797,23677,25376,30351})
            items.add(GeSearchIndexTest.item(id,id==11920 || id==12797?"Dragon pickaxe":"Dragon pickaxe (or)",3));
        items.add(GeSearchIndexTest.item(27695,"Dragon pickaxe (broken)",3));
        for(boolean ge:new boolean[]{false,true})
        {
            var result=new ItemCatalogueIndex(items,ge).find("cox guardians","94147:20");assertNotNull(result);
            for(ItemCatalogueIndex.Item item:items) if(item.id!=27695)
            {
                assertTrue("Missing "+item.id,result.items.stream().anyMatch(i->i.id==item.id));
                assertEquals(2,result.ranks.get(item.id).tier);
                assertEquals("Guardians — pickaxes",RaidRecommendations.layoutGroup("Chambers of Xeric",item.fact));
            }
            if(result.ranks.containsKey(27695)) assertEquals(0,result.ranks.get(27695).tier);
        }
        assertTrue(ItemVariantState.needsActivityReview(items.get(5).fact));
    }

    @Test public void depletedPickaxesStillHaveTheirGuardianUseAndOtherRaidsKeepRoomRoles()
    {
        assertEquals("Guardians — pickaxes",RaidRecommendations.layoutGroup("Chambers of Xeric/Challenge Mode",GeSearchIndexTest.item(23682,"Crystal pickaxe (inactive)",3).fact));
        assertEquals("Guardians — pickaxes",RaidRecommendations.layoutGroup("Chambers of Xeric",GeSearchIndexTest.item(13244,"Infernal pickaxe (uncharged)",3).fact));
        assertTrue(RaidRecommendations.layoutGroup("Tombs of Amascut",GeSearchIndexTest.item(25979,"Keris partisan",3).fact).contains("Kephri"));
        assertTrue(RaidRecommendations.layoutGroup("Theatre of Blood",GeSearchIndexTest.item(28997,"Dual macuahuitl",3).fact).contains("Verzik"));
        assertEquals("",RaidRecommendations.layoutGroup("Mining",GeSearchIndexTest.item(11920,"Dragon pickaxe",3).fact));
        assertTrue(RaidRecommendations.layoutGroups("Chambers of Xeric/Challenge Mode",GeSearchIndexTest.item(21003,"Elder maul",3).fact).contains("Guardians — defence-drain specials"));
        Set<String> raids=new HashSet<>();
        for(String[] row:ItemTaxonomy.readRows("raid-layout-rooms.tsv",2)) {
            var setup=ActivitySetups.byId(row[0]);assertNotNull(row[0],setup);
            assertTrue(setup.label.endsWith("Equipment alternatives"));raids.add(setup.activity.split("/")[0]);
        }
        assertEquals(new HashSet<>(Arrays.asList("Chambers of Xeric","Theatre of Blood","Tombs of Amascut")),raids);
    }

    @Test public void inventoryKeepsEveryRepeatedSupplyAndReviewedAlternativePosition()
    {
        var items=Arrays.asList(GeSearchIndexTest.item(2434,"Prayer potion(4)",-1));
        var owned=new HashMap<Integer,ItemCatalogueIndex.Item>();items.forEach(i->owned.put(i.id,i));
        var plan=SetupLoadout.build(ActivitySetups.forActivity("Basilisk Knight"),owned,owned.keySet(),"256152:10");
        // This guide deliberately leaves several slots empty; populated prayer slots must all survive.
        assertNotNull(plan);
        assertEquals(15,plan.inventory.values().stream().filter(id->id==2434).count());
        assertEquals(plan.inventoryExample.size(),plan.inventoryExample.keySet().stream().distinct().count());
        assertFalse(plan.inventoryExample.containsKey(16));
    }

    @Test public void coxMaxInventoryHasAllTwentyEightSourcePositionsIncludingRepeats()
    {
        var plan=SetupLoadout.build(ActivitySetups.forActivity("Chambers of Xeric"),Collections.emptyMap(),Collections.emptySet(),"94147:13");
        assertNotNull(plan);assertEquals(28,plan.inventoryExample.size());
        for(int slot=0;slot<28;slot++) assertTrue("Missing source slot "+slot,plan.inventoryExample.containsKey(slot));
        assertTrue(plan.inventoryExample.values().stream().filter(f->f.ids.contains(6685)).count()>1);
    }
}
