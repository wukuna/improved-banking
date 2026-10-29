package com.windowmarkers;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class ConsolidatedActivityTest
{
    private static Map<Integer,ItemCatalogueIndex.Item> bank() {
        Map<Integer,ItemCatalogueIndex.Item> result=new LinkedHashMap<>();
        String[][] rows={{"29589","Emberlight","3"},{"29591","Scorching bow","3"},{"27275","Tumeken's shadow","3"},
            {"12899","Trident of the swamp","3"},{"31097","Avernic treads (max)","10"},{"22947","Rada's blessing 4","13"},
            {"21326","Amethyst arrow","13"},{"11212","Dragon arrow","13"},{"22322","Avernic defender","5"},
            {"21018","Ancestral hat","0"},{"26382","Torva full helm","0"},{"12954","Dragon defender","5"},
            {"6685","Saradomin brew(4)","-1"},{"3024","Super restore(4)","-1"},
            {"4860","Ahrim's hood 0","0"},{"12924","Toxic blowpipe (empty)","3"}};
        for(String[] r:rows) { int id=Integer.parseInt(r[0]);result.put(id,GeSearchIndexTest.item(id,r[1],Integer.parseInt(r[2]))); }
        return result;
    }
    @Test public void everyActivityHasOneBrowseEntryAndArchivedAmmoStillResolves() {
        List<BankCatalogue.Choice> choices=BankCatalogue.choices(Collections.emptyList(),Collections.emptyList(),new CustomPresets());
        for(String activity:ActivitySetups.activities(""))
            assertEquals(activity,1,choices.stream().filter(c->c.kind==BankCatalogue.Kind.ACTIVITY && c.name.equals(activity)).count());
        assertFalse(choices.stream().anyMatch(c->c.kind==BankCatalogue.Kind.WIKI_SETUP || CatalogueArchive.hidden(c.name)));
        assertEquals(37,ItemTaxonomy.readRows("catalogue-archive.tsv",2).size());
        assertTrue(SupplySearch.parse("ammo twisted bow").ids.contains(11212));
        Marker old=new Marker(0,0,48,36);old.label="Ammo — twisted bow";old.linkedItemIds=new int[]{11212};
        assertTrue(BankCatalogue.choices(Collections.emptyList(),Collections.singletonList(old),new CustomPresets()).stream().anyMatch(c->c.value==old));
        assertTrue(BankCatalogue.targets("Activity: Abyssal Sire",choices).stream().anyMatch(c->c.name.equals("Abyssal Sire")));
        assertTrue(BankCatalogue.search(choices,"sire phase 1 magic",10).stream().anyMatch(c->c.name.equals("Abyssal Sire")));
    }
    @Test public void sireCombinesBothPhasesAndKeepsInventoriesSeparate() {
        var bank=bank();var plan=SetupLoadout.build(ActivitySetups.forActivity("Abyssal Sire"),bank,bank.keySet(),"");
        assertNotNull(plan);assertEquals(6,plan.sections().size()); // two phases, three styles; duplicate melee source collapsed
        assertTrue(plan.sections().stream().anyMatch(p->p.sectionName.contains("Phase 1")));
        assertTrue(plan.sections().stream().anyMatch(p->p.sectionName.contains("Phase 2")));
        for(SetupLoadout part:plan.sections()) {
            assertEquals(part.sectionName,Integer.valueOf(31097),part.equipment.get(10).get(0));
            assertTrue(part.inventoryExample.keySet().stream().allMatch(i->i>=0 && i<28));
            assertFalse(part.itemIds().contains(4860));assertFalse(part.itemIds().contains(12924));
            if(part.sectionName.contains("Magic")) assertEquals(Integer.valueOf(22947),part.equipment.get(13).get(0));
            if(part.sectionName.contains("Phase 1") && part.sectionName.contains("Melee")) {
                assertEquals(Integer.valueOf(29591),part.equipment.get(3).get(0));
                assertFalse(part.equipment.get(13).contains(22947));
            }
        }
        var edited=plan.retainItems(Collections.singleton(29589));
        assertEquals(plan.sections().size(),edited.sections().size());
        assertEquals(Collections.singleton(29589),edited.itemIds());
        for(SetupLoadout part:edited.sections()) assertTrue(part.inventory.keySet().containsAll(part.inventoryExample.keySet()));
    }
    @Test public void everyActivityBuildsWithSparseOwnedGearWithoutReintroducingUnreadyStates() {
        var bank=bank();int built=0;
        for(String activity:ActivitySetups.activities("")) {
            assertFalse(activity.toLowerCase(Locale.ROOT).contains("fractured archive"));
            var plan=SetupLoadout.build(ActivitySetups.forActivity(activity),bank,bank.keySet(),"");
            if(plan==null) continue;built++;
            for(SetupLoadout section:plan.sections()) {
                assertTrue(activity,section.inventory.size()<=28);
                assertTrue(activity,bank.keySet().containsAll(section.itemIds()));
                assertFalse(activity,section.itemIds().contains(4860));
                assertFalse(activity,section.itemIds().contains(12924));
                for(var slot:section.equipment.entrySet()) for(int id:slot.getValue()) assertEquals(activity,slot.getKey().intValue(),bank.get(id).slot);
            }
        }
        assertTrue(built>=100);
    }
    @Test public void singleSourceSireMagicHasBootsBlessingAndOwnedHelmetFallback() {
        var bank=bank();var plan=SetupLoadout.build(Collections.singletonList(ActivitySetups.byId("74025:2")),bank,Collections.singleton(27275),"");
        assertEquals(1,plan.sections().size());
        assertEquals(Integer.valueOf(31097),plan.equipment.get(10).get(0));
        assertEquals(Integer.valueOf(22947),plan.equipment.get(13).get(0));
        assertEquals(Integer.valueOf(21018),plan.equipment.get(0).get(0));
        var index=new ItemCatalogueIndex(new ArrayList<>(bank.values()),false);
        var result=index.find("sire magic","74025:2");
        assertTrue(result.items.stream().anyMatch(i->i.id==31097));
        assertTrue(result.items.stream().anyMatch(i->i.id==22947));
    }
    @Test public void ordinaryUpgradesDoNotReplaceRequiredProtectionOrAMethodWeapon() {
        var bank=bank();
        for(String activity:Arrays.asList("Basilisk Knight","Skeletal Wyvern","Fossil Island Wyverns")) {
            var plan=SetupLoadout.build(ActivitySetups.forActivity(activity),bank,bank.keySet(),"");
            assertNotNull(activity,plan);
            for(SetupLoadout part:plan.sections()) {
                assertFalse(activity,part.equipment.getOrDefault(5,Collections.emptyList()).contains(22322));
                assertFalse(activity,part.equipment.getOrDefault(5,Collections.emptyList()).contains(12954));
            }
        }
        var shaman=SetupLoadout.build(ActivitySetups.forActivity("Lizardman shaman"),bank,bank.keySet(),"");
        for(var part:shaman.sections()) assertFalse(part.equipment.getOrDefault(10,Collections.emptyList()).contains(31097));
        bank.remove(29591);
        var phase=SetupLoadout.build(Collections.singletonList(ActivitySetups.byId("74025:1")),bank,bank.keySet(),"");
        assertFalse(phase.equipment.containsKey(3)); // no invented replacement for the scorching-bow phase method
    }
    @Test public void parentActivitySearchKeepsChallengeModeAndMainPhaseContextsDistinct() {
        var result=new ItemCatalogueIndex(new ArrayList<>(bank().values()),false).find("cox","");
        assertNotNull(result.loadout);assertTrue(result.loadout.sectionCount()>1);
        assertEquals("Chambers of Xeric",result.loadout.activity);
        assertTrue(result.loadout.sections().stream().anyMatch(p->p.sectionName.contains("Challenge Mode")));
        assertTrue(result.loadout.sections().stream().anyMatch(p->p.sectionName.contains("Great Olm")));
    }
}
