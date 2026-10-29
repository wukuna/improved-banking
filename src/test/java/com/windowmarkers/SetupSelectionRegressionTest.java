package com.windowmarkers;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class SetupSelectionRegressionTest {
    @Test public void refreshedSarachnisExampleIncludesOwnedMagicTreads() {
        for(int id:new int[]{31097,31093,31095,31096}) {
            var p=plan("224363:10",item(id,"Avernic treads",10));
            assertEquals(Integer.valueOf(id),p.equipment.get(10).get(0));
        }
    }
    @Test public void refreshedKrillArrowTiersAndElementalSpellAmuletsStayContextual() {
        assertTrue(ActivitySetups.byId("24889:1").itemIds.contains(33589));
        assertTrue(ActivitySetups.byId("82921:5").itemIds.contains(34428));
        assertFalse(ActivitySetups.byId("74025:7").itemIds.contains(34428));
        var kril=plan("24889:1",item(20997,"Twisted bow",3),item(33589,"Seeking amethyst arrow",13),item(11212,"Dragon arrow",13));
        assertEquals(Integer.valueOf(33589),kril.equipment.get(13).get(0));
    }
    @Test public void sireFullMagicGuideSelectsOwnedBlessingAndMaxTreads() {
        var p=plan("74025:7",item(27275,"Tumeken's shadow",3),item(22947,"Rada's blessing 4",13),
            item(31097,"Avernic treads (max)",10),item(13235,"Eternal boots",10));
        assertEquals(Integer.valueOf(22947),p.equipment.get(13).get(0));
        assertEquals(Integer.valueOf(31097),p.equipment.get(10).get(0));
    }
    @Test public void sirePartialSwitchTableExplainsItsIntentionallyOmittedSlots() {
        var p=plan("74025:2",item(27275,"Tumeken's shadow",3),item(22947,"Rada's blessing 4",13),item(31097,"Avernic treads (max)",10));
        assertTrue(p.equipmentNote.contains("omits boots, ammunition and a ring"));
        assertFalse(p.equipment.containsKey(10));assertFalse(p.equipment.containsKey(13));
        assertEquals(p.equipmentNote,p.retainItems(Collections.singleton(27275)).equipmentNote);
    }
    @Test public void magicTreadAlternativesNeedTheEternalUpgradeAndShareTheirGuideRank() {
        for(int id:new int[]{31093,31095,31096}) {
            String name=id==31093?"Avernic treads (et)":id==31095?"Avernic treads (pr)(et)":"Avernic treads (pe)(et)";
            var index=new ItemCatalogueIndex(Arrays.asList(item(27275,"Tumeken's shadow",3),item(22947,"Rada's blessing 4",13),
                item(id,name,10),item(31088,"Avernic treads",10),item(31091,"Avernic treads (pr)",10)),false);
            var result=index.find("sire magic","74025:7");
            assertEquals(Integer.valueOf(id),result.loadout.equipment.get(10).get(0));
            assertEquals(1,result.ranks.get(id).tier);
            assertFalse(result.loadout.equipment.get(10).contains(31088));
            assertFalse(result.loadout.equipment.get(10).contains(31091));
            assertEquals(Integer.valueOf(22947),result.loadout.equipment.get(13).get(0));
        }
    }
    private static ItemCatalogueIndex.Item item(int id,String name,int slot) { return GeSearchIndexTest.item(id,name,slot); }
    private static SetupLoadout plan(String setup,ItemCatalogueIndex.Item... items) {
        Map<Integer,ItemCatalogueIndex.Item> bank=new LinkedHashMap<>();for(var i:items) bank.put(i.id,i);
        return SetupLoadout.build(Collections.singletonList(ActivitySetups.byId(setup)),bank,bank.keySet(),"");
    }
    @Test public void hydraChoosesArrowsForTheOwnedBowInsteadOfBoltsOrBlessings() {
        var p=plan("204107:1",item(20997,"Twisted bow",3),item(11212,"Dragon arrow",13),
            item(21326,"Amethyst arrow",13),item(21944,"Ruby dragon bolts (e)",13),item(22947,"Rada's blessing 4",13));
        assertEquals(Integer.valueOf(11212),p.equipment.get(13).get(0));
        assertFalse(p.equipment.get(13).contains(21944));assertFalse(p.equipment.get(13).contains(22947));
        var best=plan("204107:1",item(20997,"Twisted bow",3),item(33595,"Seeking dragon arrow",13),item(11212,"Dragon arrow",13));
        assertEquals(Integer.valueOf(33595),best.equipment.get(13).get(0));
        assertFalse(SupplySearch.parse("ammo magic bow").ids.contains(33595));
        assertTrue(SupplySearch.parse("ammo magic bow").ids.contains(33589));
    }
    @Test public void sireAttackStyleAdviceDoesNotDemoteEmberlightBelowFang() {
        var p=plan("74025:5",item(29589,"Emberlight",3),item(26219,"Osmumten's fang",3));
        assertEquals(Integer.valueOf(29589),p.equipment.get(3).get(0));
        assertTrue(p.equipmentNotes.get(29589).contains("stab"));
    }
    @Test public void amoxliatlHeadNoteBelongsOnlyToSlayerHelmAndWeaponStyleIsNotARestriction() {
        var p=plan("538484:1",item(11865,"Slayer helmet (i)",0),item(26382,"Torva full helm",0),
            item(22325,"Scythe of vitur",3),item(22486,"Scythe of vitur (uncharged)",3));
        assertEquals(Integer.valueOf(26382),p.equipment.get(0).get(0));
        assertFalse(p.equipment.get(0).contains(11865));
        assertEquals(Collections.singletonList(22325),p.equipment.get(3));
    }
    @Test public void standaloneDualMacuahuitlUsesItsStandaloneColumnWithoutInventingFullSet() {
        var p=plan("538484:1",item(28997,"Dual macuahuitl",3));
        assertEquals(Collections.singletonList(28997),p.equipment.get(3));
        assertTrue(p.equipmentNotes.get(28997).contains("without full"));
    }
    @Test public void bloodMoonBenefitRequiresTheSelectedArmourNotMerelyOwningIt() {
        var weapon=item(28997,"Dual macuahuitl",3);var head=item(29028,"Blood moon helm",0);
        var body=item(29022,"Blood moon chestplate",4);var legs=item(29025,"Blood moon tassets",7);
        var complete=plan("538484:1",weapon,head,body,legs);
        assertTrue(complete.equipmentNotes.get(28997).contains("with full"));
        var mixed=plan("538484:1",weapon,head,body,legs,item(26382,"Torva full helm",0));
        assertEquals(Integer.valueOf(26382),mixed.equipment.get(0).get(0));
        assertTrue(mixed.equipmentNotes.get(28997).contains("without full"));
    }
    @Test public void homeTeleportFallbackAcceptsSkillCapeButNotCombatMaxCape() {
        for(var cape:Arrays.asList(item(9789,"Construct. cape",1),item(9790,"Construct. cape(t)",1),item(13280,"Max cape",1))) {
            var p=plan("538484:1",cape);assertEquals(Integer.valueOf(cape.id),p.inventory.get(27));
        }
        assertFalse(plan("538484:1",item(21285,"Infernal max cape",1)).inventory.containsKey(27));
        var exact=plan("538484:1",item(8013,"Teleport to house",-1),item(13280,"Max cape",1));
        assertEquals(Integer.valueOf(8013),exact.inventory.get(27));
        assertEquals(Collections.singletonList(11747),SetupEquipmentChoice.inventoryAlternatives(Collections.singletonList(11747)));
        assertEquals(Arrays.asList(11866,21268),SetupEquipmentChoice.inventoryAlternatives(Collections.singletonList(11866)));
        assertEquals(Collections.singletonList(21268),SetupEquipmentChoice.inventoryAlternatives(Collections.singletonList(21268)));
    }
    @Test public void everyOptionStaysInsideItsReviewedFieldAndInSourceOrder() {
        Map<String,Set<Integer>> allowed=new HashMap<>();
        for(String[] r:ItemTaxonomy.readRows("setup-layout-fields.tsv",4)) {
            Set<Integer> ids=new HashSet<>();for(String id:r[2].split(",")) if(!id.isEmpty()) ids.add(Integer.parseInt(id));
            allowed.put(r[0]+"/"+r[1],ids);
        }
        Map<String,Integer> order=new HashMap<>();
        for(String[] r:ItemTaxonomy.readRows("setup-layout-options.tsv",5)) {
            String key=r[0]+"/"+r[1];int position=Integer.parseInt(r[3]);
            assertTrue(key+"/"+r[2],allowed.get(key).contains(Integer.parseInt(r[2])));
            assertTrue(key,position>=order.getOrDefault(key,-1));order.put(key,position);
        }
        assertTrue(order.size()>10000);
    }
}
