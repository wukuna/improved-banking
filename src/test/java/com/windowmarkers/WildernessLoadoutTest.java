package com.windowmarkers;

import java.util.*;
import java.util.stream.Collectors;
import org.junit.Test;
import static org.junit.Assert.*;

public class WildernessLoadoutTest
{
    private static Map<Integer,ItemCatalogueIndex.Item> bank() {
        Map<Integer,ItemCatalogueIndex.Item> bank=new LinkedHashMap<>();
        for(String[] row:ItemTaxonomy.readRows("ge-item-slots.tsv",3)) {
            int id=Integer.parseInt(row[0]);bank.put(id,GeSearchIndexTest.item(id,row[2],Integer.parseInt(row[1])));
        }
        for(Object[] row:new Object[][]{{23733,"Divine ranging potion(4)"},{24589,"Blighted manta ray"},
            {24595,"Blighted karambwan"},{24598,"Blighted super restore(4)"}}) {
            int id=(int)row[0];bank.put(id,GeSearchIndexTest.item(id,(String)row[1],-1));
        }
        return bank;
    }
    @Test public void wildernessDefaultsUseOwnedSpecialistWeaponsAndBudgetArmour() {
        var bank=bank();var index=new ItemCatalogueIndex(new ArrayList<>(bank.values()),false);
        for(String activity:Arrays.asList("Callisto","Artio","Venenatis","Spindel","Vet'ion","Calvar'ion",
            "Scorpia","Chaos Fanatic","Chaos Elemental","Revenants","Krystilia","King Black Dragon",
            "Green dragon","Chaos Temple (church)","Abyss","Chest (Rogues' Castle)")) {
            var result=index.find(activity,"");assertNotNull(activity,result);
            assertNotNull(activity,result.loadout);assertTrue(activity,result.loadout.wildernessBudget);
            for(var item:result.items) assertTrue(activity+" included "+item.name,WildernessLoadout.allowed(item));
            for(var part:result.loadout.sections()) {
                for(var ids:part.equipment.values()) for(int id:ids)
                    assertFalse(activity+" unready "+id,ItemVariantState.needsActivityReview(bank.get(id).fact));
                for(var field:part.inventoryExample.values()) for(int id:field.ids)
                    assertTrue(activity+" expensive preview "+id,WildernessLoadout.allowedPreview(id));
                assertFalse(activity,part.sectionName.contains("BIS smuggle"));
            }
        }
        var callisto=index.find("callisto","").loadout;
        var ranged=callisto.sections().stream().filter(s->s.sectionName.equals("Ranged")).findFirst().orElseThrow();
        assertEquals(Integer.valueOf(27655),ranged.equipment.get(3).get(0));
        assertEquals(Integer.valueOf(2503),ranged.equipment.get(4).get(0));
        assertEquals(Integer.valueOf(2497),ranged.equipment.get(7).get(0));
        assertEquals(Integer.valueOf(23733),ranged.inventory.get(2));
        var magic=callisto.sections().stream().filter(s->s.sectionName.equals("Magic")).findFirst().orElseThrow();
        assertEquals(Integer.valueOf(27665),magic.equipment.get(3).get(0));
        assertEquals(Integer.valueOf(4091),magic.equipment.get(4).get(0));
        assertEquals(Integer.valueOf(4093),magic.equipment.get(7).get(0));
        var calvar=index.find("calvar'ion","").loadout;
        assertTrue(calvar.inventoryName,calvar.inventoryName.startsWith("Low-Risk"));
        assertEquals(Integer.valueOf(27660),calvar.equipment.get(3).get(0));
    }
    @Test public void expensiveOnlyBankDoesNotQuietlyBecomeABisWildernessSetup() {
        var bank=bank();bank.keySet().retainAll(new HashSet<>(Arrays.asList(27238,27241,31097,26382,21021,27275,20997)));
        var result=new ItemCatalogueIndex(new ArrayList<>(bank.values()),false).find("callisto","");
        assertTrue(result.items.isEmpty());
        for(var part:result.loadout.sections()) assertTrue(part.equipment.isEmpty());
        assertTrue(result.loadout.notEnoughGear());assertTrue(result.loadout.hasBudgetExamples());
        var ranged=result.loadout.sections().get(0);
        assertEquals(Integer.valueOf(2503),ranged.equipmentExample.get(4));
        assertEquals(Integer.valueOf(2497),ranged.equipmentExample.get(7));
        assertEquals(Integer.valueOf(12788),ranged.equipmentExample.get(3));
        assertEquals(Integer.valueOf(892),ranged.equipmentExample.get(13));
        assertEquals(Integer.valueOf(7460),result.loadout.sections().get(1).equipmentExample.get(9));
        assertTrue(result.loadout.itemIds().isEmpty());
        assertFalse(result.loadout.retainItems(Collections.emptySet()).hasBudgetExamples());
    }
    @Test public void suggestionsNeverBroadenTheBudgetFilterOrReplaceOwnedGear() {
        var bank=bank();var full=new ItemCatalogueIndex(new ArrayList<>(bank.values()),false).find("callisto","").loadout;
        assertFalse(full.notEnoughGear());
        for(var part:full.sections()) assertFalse(part.equipmentExample.containsKey(3));
        var empty=SetupLoadout.build(ActivitySetups.forActivity("Callisto"),Collections.emptyMap(),Collections.emptySet(),"");
        for(var part:empty.sections()) for(int id:part.equipmentExample.values()) {
            assertTrue(WildernessLoadout.allowed(bank.get(id)));
            assertFalse(WildernessLoadout.wildernessWeapon(id));
            assertTrue(ActivitySetups.byId(part.equipmentSource).itemIds.contains(id));
            assertFalse(ItemVariantState.needsActivityReview(bank.get(id).fact));
        }
        bank.keySet().retainAll(Collections.singleton(22947));
        var noWeapon=SetupLoadout.build(ActivitySetups.forActivity("Callisto"),bank,bank.keySet(),"").sections().get(0);
        assertEquals(Integer.valueOf(12788),noWeapon.equipmentExample.get(3));
        assertEquals(Integer.valueOf(892),noWeapon.equipmentExample.get(13));
        assertEquals(Collections.singleton(22947),noWeapon.itemIds());
    }
    @Test public void explicitSourceChoicesAndNonWildernessActivitiesKeepTheirReviewedGear() {
        var bank=bank();var index=new ItemCatalogueIndex(new ArrayList<>(bank.values()),false);
        var exact=index.find("callisto","61964:1");assertFalse(exact.loadout.wildernessBudget);
        assertTrue(exact.items.stream().anyMatch(i->i.id==27238));
        var hydra=index.find("hydra","");assertFalse(hydra.loadout.wildernessBudget);
        assertTrue(hydra.items.stream().anyMatch(i->i.id==27238));
        var custom=index.find("callisto","").loadout.retainItems(Collections.singleton(2503));
        assertTrue(custom.wildernessBudget);assertEquals(Collections.singleton(2503),custom.itemIds());
    }
    @Test public void blightedAndDivineInventoryItemsAreNotLabelledAsUnreadyInTheirGuide() {
        var result=new ItemCatalogueIndex(new ArrayList<>(bank().values()),false).find("callisto","");
        for(int id:new int[]{23733,24589,24595}) {
            assertFalse(result.ranks.get(id).explanation.contains("not a ready-to-use"));
            assertFalse(result.ranks.get(id).explanation.contains("Activity restrictions — review"));
        }
    }
}
