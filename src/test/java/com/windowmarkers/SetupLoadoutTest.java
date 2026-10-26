package com.windowmarkers;
import java.util.*;
import org.junit.*;
import static org.junit.Assert.*;

public class SetupLoadoutTest {
    static ItemCatalogueIndex catalogue() {
        return new ItemCatalogueIndex(Arrays.asList(
            GeSearchIndexTest.item(21018,"Ancestral hat",0),GeSearchIndexTest.item(4708,"Ahrim's hood",0),
            GeSearchIndexTest.item(4860,"Ahrim's hood 0",0),GeSearchIndexTest.item(4712,"Ahrim's robetop",4),
            GeSearchIndexTest.item(4714,"Ahrim's robeskirt",7),GeSearchIndexTest.item(27275,"Tumeken's shadow",3),
            GeSearchIndexTest.item(12924,"Toxic blowpipe (empty)",3),GeSearchIndexTest.item(11907,"Trident of the seas",3),
            GeSearchIndexTest.item(6685,"Saradomin brew(4)",-1),GeSearchIndexTest.item(3024,"Super restore(4)",-1),
            GeSearchIndexTest.item(385,"Shark",-1),GeSearchIndexTest.item(2452,"Antifire potion(4)",-1)),false);
    }
    @Test public void buildsOneEquipmentGuideAndOneExactInventoryWithOwnedReadyVersions() {
        var result=catalogue().find("toa magic","364045:3","364045:9");
        SetupLoadout plan=result.loadout;assertNotNull(plan);
        assertTrue(plan.equipmentName.contains("Magic"));assertTrue(plan.inventoryName.contains("Minimum"));
        assertEquals(Integer.valueOf(21018),plan.equipment.get(0).get(0));
        assertFalse(plan.equipment.values().stream().anyMatch(ids->ids.contains(4860)));
        assertFalse(plan.inventory.containsValue(12924));assertFalse(plan.inventory.containsValue(2452));
        assertFalse(plan.inventory.isEmpty());
        assertEquals(1,result.ranks.get(21018).tier);assertTrue(result.ranks.get(4712).tier>1);
        assertTrue(result.items.stream().anyMatch(i->plan.inventory.containsValue(i.id)));
    }
    @Test public void slotTierAndOrdinaryQueriesDoNotProduceWholeLoadouts() {
        var index=catalogue();assertNull(index.find("toa head","").loadout);
        assertNull(index.find("toa magic 1st","").loadout);
        assertNull(index.find("food","").loadout);
    }
    @Test public void allLayoutResourcesReferenceAcceptedExactMembers() {
        Map<String,ActivitySetups.Setup> setups=new HashMap<>();ActivitySetups.all().forEach(s->setups.put(s.id,s));
        Set<String> tables=new HashSet<>();
        for(String[] row:ItemTaxonomy.readRows("setup-layout-tables.tsv",4)) { assertTrue(row[0],setups.containsKey(row[0]));tables.add(row[0]); }
        for(String[] row:ItemTaxonomy.readRows("setup-layout-fields.tsv",4)) {
            assertTrue(row[0],tables.contains(row[0]));
            for(String value:row[2].split(",")) if(!value.isEmpty()) assertTrue(row[0]+":"+value,setups.get(row[0]).itemIds.contains(Integer.parseInt(value)));
        }
    }
}
