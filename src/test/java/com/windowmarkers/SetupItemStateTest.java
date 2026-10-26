package com.windowmarkers;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class SetupItemStateTest
{
    private SetupLoadout plan(String activity, String inventory, ItemCatalogueIndex.Item... items)
    {
        Map<Integer,ItemCatalogueIndex.Item> owned=new HashMap<>();
        for (ItemCatalogueIndex.Item item:items) owned.put(item.id,item);
        SetupLoadout plan=SetupLoadout.build(ActivitySetups.forActivity(activity),owned,owned.keySet(),inventory);
        assertNotNull(activity,plan);
        return plan;
    }

    @Test public void moleTeleportSlotUsesChargedWealthInsteadOfFirstListedEmptyVersion()
    {
        var empty=GeSearchIndexTest.item(2572,"Ring of wealth",12);
        var charged=GeSearchIndexTest.item(11980,"Ring of wealth (5)",12);
        for (String setup:Arrays.asList("108241:2","108241:8","108241:11"))
        {
            assertFalse(plan("Giant Mole",setup,empty).inventory.containsValue(empty.id));
            var both=plan("Giant Mole",setup,empty,charged);
            assertTrue(both.inventory.containsValue(charged.id));
            assertFalse(both.inventory.containsValue(empty.id));
        }
    }

    @Test public void shadowTeleportSlotsDoNotAcceptTheUnchargedCombatRing()
    {
        var empty=GeSearchIndexTest.item(28329,"Ring of shadows",12);
        var charged=GeSearchIndexTest.item(28327,"Ring of shadows",12);
        Map<String,String> examples=new LinkedHashMap<>();
        examples.put("373815:4","Phantom Muspah");examples.put("373815:6","Phantom Muspah");
        examples.put("373815:8","Phantom Muspah");examples.put("390886:3","The Whisperer");
        examples.put("391169:3","The Leviathan");examples.put("391177:2","Duke Sucellus");
        examples.put("391188:2","Vardorvis");examples.put("391188:4","Vardorvis");
        for (Map.Entry<String,String> example:examples.entrySet())
        {
            assertFalse(example.getKey(),plan(example.getValue(),example.getKey(),empty).inventory.containsValue(empty.id));
            assertTrue(example.getKey(),plan(example.getValue(),example.getKey(),charged).inventory.containsValue(charged.id));
        }
    }

    @Test public void unchargedItemsKeepTheirOtherUsefulRoles()
    {
        var cowbell=GeSearchIndexTest.item(33103,"Cowbell amulet",2);
        var wealth=GeSearchIndexTest.item(12785,"Ring of wealth (i)",12);
        var shadows=GeSearchIndexTest.item(28329,"Ring of shadows",12);
        assertTrue(plan("Brutus","646739:2",cowbell).inventory.containsValue(cowbell.id));
        assertTrue(plan("Calvar'ion","384116:7",wealth).inventory.containsValue(wealth.id));
        assertTrue(plan("The Leviathan","391169:3",shadows).equipment.get(12).contains(shadows.id));
    }
}
