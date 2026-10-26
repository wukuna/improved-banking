package com.windowmarkers;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class NonRaidLoadoutAuditTest {
    @Test public void abyssTeleportRequiresChargesButCombatGloryRemainsUsable() {
        var empty=GeSearchIndexTest.item(1704,"Amulet of glory",2);
        var charged=GeSearchIndexTest.item(11978,"Amulet of glory(6)",2);
        var index=new ItemCatalogueIndex(Arrays.asList(empty,charged),false);
        var result=index.find("abyss graceful neck","102334:1");
        assertEquals(0,result.ranks.get(1704).tier);
        assertTrue(result.ranks.get(1704).explanation.contains("Edgeville"));
        assertEquals(1,result.ranks.get(11978).tier);
        Map<Integer,ItemCatalogueIndex.Item> bank=new HashMap<>();bank.put(1704,empty);bank.put(11978,charged);
        for(String setup:new String[]{"102334:1","102334:2"}) {
            var plan=SetupLoadout.build(Collections.singletonList(ActivitySetups.byId(setup)),bank,bank.keySet(),"");
            assertEquals(Collections.singletonList(11978),plan.equipment.get(2));
            var depleted=SetupLoadout.build(Collections.singletonList(ActivitySetups.byId(setup)),Collections.singletonMap(1704,empty),Collections.singleton(1704),"");
            assertFalse(depleted.equipment.containsKey(2));
        }
        assertFalse(ItemVariantState.needsActivityReview(empty.fact));
        assertTrue(index.find("toa neck","364045:1").ranks.get(1704).tier>0);
    }
    @Test public void corpTeamAndMassInventoriesDoNotCrossDespiteIdenticalStyleNames() {
        Map<Integer,ItemCatalogueIndex.Item> bank=new HashMap<>();
        for(var item:Arrays.asList(GeSearchIndexTest.item(21003,"Elder maul",3),
            GeSearchIndexTest.item(24419,"Bonecrusher necklace",2),GeSearchIndexTest.item(22114,"Dragon crossbow",3),
            GeSearchIndexTest.item(12791,"Rune pouch",-1))) bank.put(item.id,item);
        for(String gear:new String[]{"41977:21","41977:23"}) {
            var plan=SetupLoadout.build(Collections.singletonList(ActivitySetups.byId(gear)),bank,bank.keySet(),"");
            assertEquals(Collections.singletonList(23685),plan.inventoryExample.get(0).ids);
            assertFalse(plan.inventory.containsValue(21003));
        }
        for(String gear:new String[]{"41977:17","41977:20"}) {
            var plan=SetupLoadout.build(Collections.singletonList(ActivitySetups.byId(gear)),bank,bank.keySet(),"");
            assertTrue(plan.inventoryExample.get(0).ids.contains(21003));
        }
        var explicit=SetupLoadout.build(Collections.singletonList(ActivitySetups.byId("41977:21")),bank,bank.keySet(),"41977:18");
        assertTrue(explicit.inventoryExample.get(0).ids.contains(21003));
    }
}
