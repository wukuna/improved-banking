package com.windowmarkers;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class FinalSupplyAuditTest
{
    @Test public void chargedUtilityOwnershipFillsEveryReviewedInventoryPosition() {
        Map<Integer,ItemCatalogueIndex.Item> bank=new LinkedHashMap<>();
        bank.put(25781,GeSearchIndexTest.item(25781,"Ash sanctifier",-1));
        bank.put(13116,GeSearchIndexTest.item(13116,"Bonecrusher",-1));
        bank.put(19634,GeSearchIndexTest.item(19634,"Soul bearer",-1));
        bank.put(21389,GeSearchIndexTest.item(21389,"Master scroll book",-1));
        Set<Integer> exercised=new HashSet<>();
        for(var setup:ActivitySetups.all()) {
            if(!SetupLoadout.inventoryTable(setup) || Collections.disjoint(setup.itemIds,bank.keySet())) continue;
            var plan=SetupLoadout.build(Collections.singletonList(setup),bank,bank.keySet(),setup.id);
            for(var entry:plan.inventoryExample.entrySet()) if(entry.getValue().inventoryChoice())
                for(int id:entry.getValue().ids) if(bank.containsKey(id)) {
                    assertEquals(setup.activity+" / "+entry.getKey(),Integer.valueOf(id),plan.inventory.get(entry.getKey()));
                    assertTrue(ItemVariantState.inventoryUseNote(bank.get(id).fact).contains("does not establish"));
                    exercised.add(id);break;
                }
        }
        assertEquals(bank.keySet(),exercised);
        var sire=new ItemCatalogueIndex(new ArrayList<>(bank.values()),false).find("sire","");
        assertTrue(sire.loadout.sections().stream().anyMatch(s->s.inventory.containsValue(25781)));
        assertFalse(sire.ranks.get(25781).explanation.contains("not a ready-to-use"));
        assertTrue(sire.ranks.get(25781).explanation.contains("check death-rune charges"));
    }

    @Test public void knownEmptyBrokenAndUnlistedUtilityItemsStayGuarded() {
        var sire=ActivitySetups.forActivity("Abyssal Sire").stream().filter(SetupLoadout::inventoryTable).findFirst().orElseThrow();
        for(Object[] row:new Object[][]{{19636,"Damaged soul bearer"},{12924,"Toxic blowpipe (empty)"},
            {4860,"Ahrim's hood 0"},{25781,"Future reused identity"},{19634,"Soul bearer"}}) {
            var item=GeSearchIndexTest.item((int)row[0],(String)row[1],-1).fact;
            assertTrue((String)row[1],ItemVariantState.needsInventoryReview(item,sire));
        }
        assertTrue(ItemVariantState.needsActivityReview(GeSearchIndexTest.item(25781,"Ash sanctifier",-1).fact));
    }

    @Test public void everySupportedPotionDoseFillsItsReviewedSlotsAndAbsentPotionsAreNotInvented() {
        Map<Integer,ItemCatalogueIndex.Item> bank=new HashMap<>();
        for(String[] row:ItemTaxonomy.readRows("inventory-potion-doses.tsv",4))
            bank.put(Integer.parseInt(row[0]),GeSearchIndexTest.item(Integer.parseInt(row[0]),row[1],-1));
        int checked=0;
        for(var setup:ActivitySetups.all()) if(SetupLoadout.inventoryTable(setup)) {
            var plan=SetupLoadout.build(Collections.singletonList(setup),bank,bank.keySet(),setup.id);
            for(var entry:plan.inventoryExample.entrySet()) if(entry.getValue().inventoryChoice())
                for(int id:entry.getValue().ids) if(bank.containsKey(id)) {
                    assertEquals(setup.id+" / "+entry.getKey(),Integer.valueOf(id),plan.inventory.get(entry.getKey()));checked++;break;
                }
        }
        assertTrue(checked>500);
        var antidote=GeSearchIndexTest.item(5954,"Antidote++(3)",-1);
        var owned=Collections.singletonMap(5954,antidote);
        var plan=SetupLoadout.build(ActivitySetups.forActivity("Callisto"),owned,owned.keySet(),"");
        assertEquals(Integer.valueOf(5954),plan.sections().get(0).inventory.get(3));
        var empty=SetupLoadout.build(ActivitySetups.forActivity("Callisto"),Collections.emptyMap(),Collections.emptySet(),"");
        assertFalse(empty.sections().get(0).inventory.containsKey(3));
    }
}
