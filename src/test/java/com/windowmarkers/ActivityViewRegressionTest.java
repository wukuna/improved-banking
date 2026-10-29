package com.windowmarkers;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class ActivityViewRegressionTest
{
    @Test public void fullPresetNamesDoNotAddUnrelatedItemsBeyondShortBossSearches() {
        List<AutoCategories.Item> bank=new ArrayList<>();
        for(String[] row:ItemTaxonomy.readRows("ge-item-slots.tsv",3)) {
            int slot=Integer.parseInt(row[1]);
            bank.add(new AutoCategories.Item(Integer.parseInt(row[0]),row[2],slot>=0,null,0,0,0,slot));
        }
        for(Object[] item:new Object[][]{{385,"Shark"},{3024,"Super restore(4)"},{6685,"Saradomin brew(4)"},
            {5331,"Watering can"},{1511,"Logs"},{2347,"Hammer"},{590,"Tinderbox"}})
            bank.add(new AutoCategories.Item((int)item[0],(String)item[1],false,null,0,0,0,-1));
        BankSearchIndex index=new BankSearchIndex(bank,Collections.emptyList(),new CustomPresets());
        for(String[] pair:new String[][]{{"Alchemical Hydra","hydra"},{"Abyssal Sire","sire"},{"Callisto","callisto"}})
        for(String guide:new String[]{"",ActivitySetups.forActivity(pair[0]).get(0).id}) {
            BankSearchIndex.Match preset=index.search(pair[0],guide),search=index.search(pair[1],guide);
            Set<Integer> extras=new HashSet<>(preset.ids);extras.removeAll(search.ids);
            assertTrue(pair[0]+" unexpected preset additions: "+extras,extras.isEmpty());
            assertNotNull(preset.loadout);assertNotNull(search.loadout);
            assertEquals(search.loadout.sectionCount(),preset.loadout.sectionCount());
            for(int i=0;i<search.loadout.sectionCount();i++) {
                var a=search.loadout.sections().get(i);var b=preset.loadout.sections().get(i);
                assertEquals(pair[0]+" section "+i+" gear",a.equipment,b.equipment);
                assertEquals(pair[0]+" section "+i+" inventory",a.inventory,b.inventory);
                assertEquals(pair[0]+" section "+i+" guide positions",a.inventoryExample.keySet(),b.inventoryExample.keySet());
            }
            // Short input also keeps literal name matches (e.g. Hydra slayer helmet).
            assertFalse(preset.ids.contains(5331));assertFalse(preset.ids.contains(1511));assertFalse(preset.ids.contains(2347));
        }
    }

    @Test public void callistoUsesOwnedBlightedFoodInEveryRecommendedInventorySlot() {
        Map<Integer,ItemCatalogueIndex.Item> bank=new LinkedHashMap<>();
        bank.put(24589,GeSearchIndexTest.item(24589,"Blighted manta ray",-1));
        bank.put(24595,GeSearchIndexTest.item(24595,"Blighted karambwan",-1));
        bank.put(24598,GeSearchIndexTest.item(24598,"Blighted super restore(4)",-1));
        SetupLoadout plan=SetupLoadout.build(ActivitySetups.forActivity("Callisto"),bank,bank.keySet(),"");
        assertNotNull(plan);int checked=0;
        for(SetupLoadout section:plan.sections()) for(var slot:section.inventoryExample.entrySet())
            for(int id:slot.getValue().ids) if(bank.containsKey(id)) {
                assertEquals(section.sectionName+" inventory slot "+slot.getKey(),Integer.valueOf(id),section.inventory.get(slot.getKey()));checked++;
            }
        assertTrue("Repeated food slots must all be checked",checked>=10);
        assertTrue(ItemVariantState.needsActivityReview(bank.get(24589).fact));
        assertTrue(ItemVariantState.needsActivityReview(bank.get(24595).fact));
        assertTrue(ItemVariantState.needsInventoryReview(bank.get(24589).fact,ActivitySetups.byId("74025:2")));
        assertTrue(ItemVariantState.needsInventoryReview(GeSearchIndexTest.item(12924,"Toxic blowpipe (empty)",3).fact,ActivitySetups.byId("61964:2")));
    }
    @Test public void everyReviewedInventoryCanUseItsExactOwnedBlightedSupplies() {
        Map<Integer,ItemCatalogueIndex.Item> bank=new LinkedHashMap<>();
        bank.put(24589,GeSearchIndexTest.item(24589,"Blighted manta ray",-1));
        bank.put(24595,GeSearchIndexTest.item(24595,"Blighted karambwan",-1));
        bank.put(24598,GeSearchIndexTest.item(24598,"Blighted super restore(4)",-1));
        Set<String> activities=new HashSet<>();int slots=0;
        for(var setup:ActivitySetups.all()) {
            if(!SetupLoadout.inventoryTable(setup) || Collections.disjoint(setup.itemIds,bank.keySet())) continue;
            var plan=SetupLoadout.build(Collections.singletonList(setup),bank,bank.keySet(),setup.id);
            assertNotNull(setup.id,plan);
            for(var entry:plan.inventoryExample.entrySet()) {
                if(!entry.getValue().inventoryChoice()) continue;
                for(int id:entry.getValue().ids) if(bank.containsKey(id)) {
                    assertEquals(setup.id+" slot "+entry.getKey(),Integer.valueOf(id),plan.inventory.get(entry.getKey()));
                    activities.add(setup.activity);slots++;break;
                }
            }
        }
        assertTrue(activities.toString(),activities.containsAll(Arrays.asList("Callisto","Venenatis","Vet'ion")));
        assertTrue("Check repeated supply positions across Wilderness guides",slots>50);
    }
}
