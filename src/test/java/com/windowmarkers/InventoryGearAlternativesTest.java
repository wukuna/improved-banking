package com.windowmarkers;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class InventoryGearAlternativesTest
{
    private static ItemCatalogueIndex.Item item(int id,String name,int slot) { return GeSearchIndexTest.item(id,name,slot); }
    private SetupLoadout muspah(ItemCatalogueIndex.Item... items) {
        Map<Integer,ItemCatalogueIndex.Item> owned=new LinkedHashMap<>();for(var item:items) owned.put(item.id,item);
        return SetupLoadout.build(ActivitySetups.forActivity("Phantom Muspah"),owned,owned.keySet(),"373815:4");
    }
    @Test public void muspahInventoryUsesOwnedRangedAccessoriesAcrossEveryPhase() {
        SetupLoadout plan=muspah(item(32879,"Aquanite hopper",5),item(19547,"Necklace of anguish",2),item(13337,"Accumulator max cape",1));
        for(SetupLoadout section:plan.sections()) {
            assertEquals(Integer.valueOf(13337),section.inventory.get(4));
            assertEquals(Integer.valueOf(19547),section.inventory.get(5));
            assertEquals(Integer.valueOf(32879),section.inventory.get(9));
        }
        assertTrue(plan.itemIds().containsAll(Arrays.asList(32879,19547,13337)));
    }
    @Test public void exactAndReviewedHigherChoicesWinBeforeDowngrades() {
        for(SetupLoadout p:muspah(item(21000,"Twisted buckler",5),item(32879,"Aquanite hopper",5),
            item(28902,"Dizana's max cape",1),item(13337,"Accumulator max cape",1),
            item(33639,"Necklace of rupture",2),item(19547,"Necklace of anguish",2)).sections()) {
            assertEquals(Integer.valueOf(21000),p.inventory.get(9));
            assertEquals(Integer.valueOf(28902),p.inventory.get(4));
            assertEquals(Integer.valueOf(33639),p.inventory.get(5));
        }
        for(SetupLoadout p:muspah(item(22109,"Ava's assembler",1),item(10499,"Ava's accumulator",1)).sections())
            assertEquals(Integer.valueOf(22109),p.inventory.get(4));
        for(SetupLoadout p:muspah(item(10499,"Ava's accumulator",1)).sections()) assertEquals(Integer.valueOf(10499),p.inventory.get(4));
    }
    @Test public void ornamentsWorkBothWaysButRestrictedCopiesAndBrokenCapesDoNot() {
        assertTrue(InventoryGearAlternatives.sameCombatItem(22249,item(19547,"Necklace of anguish",2).fact));
        assertTrue(InventoryGearAlternatives.sameCombatItem(19547,item(22249,"Necklace of anguish (or)",2).fact));
        assertFalse(InventoryGearAlternatives.sameCombatItem(19547,item(27172,"Necklace of anguish",2).fact));
        assertFalse(InventoryGearAlternatives.sameCombatItem(28902,item(28830,"Dizana's max cape (broken)",1).fact));
        assertFalse(InventoryGearAlternatives.sameCombatItem(28327,item(28329,"Ring of shadows (uncharged)",12).fact));
        assertFalse(InventoryGearAlternatives.sameCombatItem(22249,item(19547,"Unreviewed impostor",2).fact));
        for(SetupLoadout p:muspah(item(28830,"Dizana's max cape (broken)",1),item(28947,"Dizana's quiver (uncharged)",1),
            item(27172,"Necklace of anguish",2),item(12825,"Arcane spirit shield",5)).sections()) {
            assertFalse(p.inventory.containsKey(4));assertFalse(p.inventory.containsKey(5));assertFalse(p.inventory.containsKey(9));
        }
    }
    @Test public void allInventoryAnguishExamplesAcceptTheOtherCosmeticVersion() {
        int checked=0;
        for(ActivitySetups.Setup setup:ActivitySetups.all()) if(SetupLoadout.inventoryTable(setup)
            && (setup.itemIds.contains(22249) || setup.itemIds.contains(19547))) {
            int wanted=setup.itemIds.contains(22249)?22249:19547,held=wanted==22249?19547:22249;
            var owned=item(held,held==19547?"Necklace of anguish":"Necklace of anguish (or)",2);
            SetupLoadout plan=SetupLoadout.build(Collections.singletonList(setup),Collections.singletonMap(held,owned),Collections.singleton(held),setup.id);
            if(plan==null || plan.wildernessBudget) continue;
            for(var entry:plan.inventoryExample.entrySet()) if(entry.getValue().inventoryChoice() && entry.getValue().ids.contains(wanted)) {
                assertEquals(setup.id,Integer.valueOf(held),plan.inventory.get(entry.getKey()));checked++;
            }
        }
        assertEquals(10,checked);
    }
    @Test public void sharedSearchIncludesTheReplacementWithoutGivingItTheExamplesRank() {
        var index=new ItemCatalogueIndex(Arrays.asList(item(32879,"Aquanite hopper",5),item(19547,"Necklace of anguish",2),item(13337,"Accumulator max cape",1)),false);
        var result=index.find("muspah","","373815:4");assertNotNull(result.loadout);
        Set<Integer> shown=new HashSet<>();result.items.forEach(i->shown.add(i.id));
        assertTrue(shown.containsAll(Arrays.asList(32879,19547,13337)));
        assertNotEquals(1,result.ranks.get(32879).tier);
        assertNotEquals(1,result.ranks.get(19547).tier);
        assertFalse(result.loadout.retainItems(Collections.singleton(19547)).itemIds().contains(32879));
    }
}
