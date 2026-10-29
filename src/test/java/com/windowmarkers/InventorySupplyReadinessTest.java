package com.windowmarkers;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class InventorySupplyReadinessTest
{
    private static Map<Integer,ItemCatalogueIndex.Item> supplies() {
        Map<Integer,ItemCatalogueIndex.Item> bank=new LinkedHashMap<>();
        for(Object[] entry:new Object[][]{{23733,"Divine ranging potion(4)"},{23736,"Divine ranging potion(3)"},
            {23739,"Divine ranging potion(2)"},{23742,"Divine ranging potion(1)"},
            {23685,"Divine super combat potion(4)"},{23688,"Divine super combat potion(3)"},
            {23691,"Divine super combat potion(2)"},{23694,"Divine super combat potion(1)"},
            {23709,"Divine super strength potion(4)"},{24635,"Divine bastion potion(4)"},
            {24638,"Divine bastion potion(3)"},{24641,"Divine bastion potion(2)"},{24644,"Divine bastion potion(1)"},
            {22081,"Locator orb"},{29784,"Araxyte venom sac"},{2450,"Zamorak brew(4)"}}) {
            int id=(int)entry[0];bank.put(id,GeSearchIndexTest.item(id,(String)entry[1],-1));
        }
        return bank;
    }

    @Test public void reviewedInventorySuppliesFillAllTheirOwnedPositionsDespiteSelfDamageWarnings() {
        Map<Integer,ItemCatalogueIndex.Item> bank=supplies();
        int slots=0;Set<String> activities=new HashSet<>();
        for(ActivitySetups.Setup setup:ActivitySetups.all()) {
            if(!SetupLoadout.inventoryTable(setup) || Collections.disjoint(setup.itemIds,bank.keySet())) continue;
            SetupLoadout plan=SetupLoadout.build(Collections.singletonList(setup),bank,bank.keySet(),setup.id);
            assertNotNull(setup.id,plan);
            for(var entry:plan.inventoryExample.entrySet()) {
                if(!entry.getValue().inventoryChoice()) continue;
                for(int id:entry.getValue().ids) if(bank.containsKey(id)) {
                    assertEquals(setup.id+" inventory position "+entry.getKey(),Integer.valueOf(id),plan.inventory.get(entry.getKey()));
                    assertFalse(ItemVariantState.inventoryUseNote(bank.get(id).fact).isEmpty());
                    activities.add(setup.activity);slots++;break;
                }
            }
        }
        assertTrue(activities.toString(),activities.containsAll(Arrays.asList("Alchemical Hydra","Callisto","Vorkath")));
        assertTrue("Exercise repeated supplies across the inventory corpus",slots>100);
    }

    @Test public void unlistedSuppliesAndUnreadyStatesRemainGuarded() {
        var hydra=ActivitySetups.byId("256152:2");
        for(var item:Arrays.asList(GeSearchIndexTest.item(24589,"Blighted manta ray",-1),
            GeSearchIndexTest.item(12924,"Toxic blowpipe (empty)",3),
            GeSearchIndexTest.item(4860,"Ahrim's hood 0",0),
            GeSearchIndexTest.item(20985,"Overload (+)(4)",-1)))
            assertTrue(item.name,ItemVariantState.needsInventoryReview(item.fact,hydra));
        assertTrue("General categories retain the deliberate-use warning",
            ItemVariantState.needsActivityReview(supplies().get(23733).fact));
    }

    @Test public void partialDoseBottlesFillRepeatedSlotsWithoutChangingPotionEffects() {
        Map<Integer,ItemCatalogueIndex.Item> bank=new LinkedHashMap<>();
        for(Object[] row:new Object[][]{{23736,"Divine ranging potion(3)"},{24601,"Blighted super restore(3)"},
            {6689,"Saradomin brew(2)"},{12631,"Stamina potion(1)"}}) {
            int id=(int)row[0];bank.put(id,GeSearchIndexTest.item(id,(String)row[1],-1));
        }
        var result=new ItemCatalogueIndex(new ArrayList<>(bank.values()),false).find("callisto","");
        var ranged=result.loadout.sections().stream().filter(s->s.sectionName.equals("Ranged")).findFirst().orElseThrow();
        assertEquals(Integer.valueOf(23736),ranged.inventory.get(2));
        assertEquals(Integer.valueOf(24601),ranged.inventory.get(1));
        assertEquals(Integer.valueOf(24601),ranged.inventory.get(5));
        assertEquals(Integer.valueOf(6689),ranged.inventory.get(18));
        assertEquals(Integer.valueOf(12631),ranged.inventory.get(7));
        for(int id:bank.keySet()) assertTrue(result.items.stream().anyMatch(i->i.id==id));
        assertTrue(InventoryPotionDoses.note(23736).contains("3-dose"));
        bank.put(23733,GeSearchIndexTest.item(23733,"Divine ranging potion(4)",-1));
        var exact=SetupLoadout.build(Collections.singletonList(ActivitySetups.byId("61964:2")),bank,bank.keySet(),"");
        assertEquals(Integer.valueOf(23733),exact.inventory.get(2));
        bank.remove(23733);bank.remove(23736);
        bank.put(2444,GeSearchIndexTest.item(2444,"Ranging potion(4)",-1));
        var wrongEffect=SetupLoadout.build(Collections.singletonList(ActivitySetups.byId("61964:2")),bank,bank.keySet(),"");
        assertFalse(wrongEffect.inventory.containsKey(2));
    }

    @Test public void doseAlternativesValidateExactIdentityAndDoNotCrossBlightedFamilies() {
        assertFalse(InventoryPotionDoses.listed(Collections.singleton(23733),GeSearchIndexTest.item(23736,"Future reused identity",-1).fact));
        assertFalse(InventoryPotionDoses.alternatives(Collections.singletonList(3024)).contains(24601));
        assertFalse(InventoryPotionDoses.alternatives(Collections.singletonList(24598)).contains(3026));
        assertFalse(InventoryPotionDoses.alternatives(Collections.singletonList(12924)).contains(12926));
        Map<String,Set<String>> families=new HashMap<>();Set<Integer> ids=new HashSet<>();
        for(String[] row:ItemTaxonomy.readRows("inventory-potion-doses.tsv",4)) {
            assertTrue(ids.add(Integer.parseInt(row[0])));
            families.computeIfAbsent(row[2],k->new HashSet<>()).add(row[3]);
        }
        for(var family:families.entrySet()) assertEquals(family.getKey(),new HashSet<>(Arrays.asList("1","2","3","4")),family.getValue());
    }
}
