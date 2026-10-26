package com.windowmarkers;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class GeActivityFilterTest
{
    private final GeRequirements maxed=new GeRequirements(new int[]{99,99,99,99,99,99,99},true,true);
    private ItemCatalogueIndex.Item item(int id,String name,int slot) { return GeSearchIndexTest.item(id,name,slot); }
    private GePlayerItems.Snapshot bank(ItemCatalogueIndex.Item... items) { return new GePlayerItems.Snapshot(true,Arrays.asList(items),0,true); }
    private Set<Integer> ids(GeActivityFilter.Outcome result) { Set<Integer> ids=new HashSet<>();result.result.items.forEach(i->ids.add(i.id));return ids; }
    private ItemCatalogueIndex index() { return new ItemCatalogueIndex(Arrays.asList(item(21021,"Ancestral robe top",4),item(26243,"Virtus robe top",4),
        item(4712,"Ahrim's robetop",4),item(4872,"Ahrim's robetop 0",4),item(4091,"Mystic robe top",4))); }
    @Test public void nearestGuideChoiceDoesNotJumpStraightToBis() {
        var result=index().find("toa magic top","");
        var next=GeActivityFilter.apply(result,bank(item(4091,"Mystic robe top",4)),maxed,WindowMarkersConfig.ActivityItems.NEXT_UPGRADE,false);
        assertEquals(Collections.singleton(4712),ids(next));
        assertEquals(3,next.result.ranks.get(4712).tier);
        assertEquals(Collections.singleton(26243),ids(GeActivityFilter.apply(result,bank(item(4712,"Ahrim's robetop",4)),maxed,WindowMarkersConfig.ActivityItems.NEXT_UPGRADE,false)));
    }
    @Test public void worseFilterPreservesSetAndSpecialistAlternatives() {
        var result=index().find("toa magic top","");
        Set<Integer> filtered=ids(GeActivityFilter.apply(result,bank(item(21021,"Ancestral robe top",4)),maxed,WindowMarkersConfig.ActivityItems.ALL,true));
        assertFalse(filtered.contains(4091));assertTrue(filtered.contains(26243));assertTrue(filtered.contains(4712));assertTrue(filtered.contains(4872));
    }
    @Test public void brokenGearIsNotAnUpgradeBaseline() {
        var result=index().find("toa magic top","");
        var broken=bank(item(4872,"Ahrim's robetop 0",4));
        assertNull(broken.owned(item(4712,"Ahrim's robetop",4)));
        assertEquals(result.items.size(),GeActivityFilter.apply(result,broken,maxed,WindowMarkersConfig.ActivityItems.ALL,true).result.items.size());
        assertTrue(ids(GeActivityFilter.apply(result,broken,maxed,WindowMarkersConfig.ActivityItems.NEXT_UPGRADE,false)).isEmpty());
    }
    @Test public void untradeableCosmeticEquivalentCountsWithoutChangingThePurchaseId() {
        var owned=bank(item(24666,"Twisted ancestral robe top",4));
        assertNotNull(owned.owned(item(21021,"Ancestral robe top",4)));
        assertEquals(Collections.singleton(21021),ids(GeActivityFilter.apply(index().find("toa magic top",""),owned,maxed,WindowMarkersConfig.ActivityItems.OWNED,false)));
    }
    @Test public void unknownBankLeavesResultsIntactAndExplainsWhy() {
        var result=index().find("toa magic top","");
        var next=GeActivityFilter.apply(result,GePlayerItems.Snapshot.unknown(),maxed,WindowMarkersConfig.ActivityItems.UNOWNED,true);
        assertSame(result,next.result);assertTrue(next.notice.contains("Open your bank"));
    }
    @Test public void ownershipFiltersDoNotChangeSupplySearches() {
        var result=new ItemCatalogueIndex(Collections.singletonList(item(385,"Shark",-1))).find("food","");
        assertSame(result,GeActivityFilter.apply(result,bank(),maxed,WindowMarkersConfig.ActivityItems.NEXT_UPGRADE,true).result);
    }
    @Test public void lockedEquipmentDoesNotSuppressOrBecomeANextUpgrade() {
        GeRequirements low=new GeRequirements(new int[]{1,1,1,1,1,1,10},false,false);
        var result=index().find("toa magic top","");
        assertTrue(GeActivityFilter.apply(result,bank(item(4091,"Mystic robe top",4)),low,WindowMarkersConfig.ActivityItems.NEXT_UPGRADE,false).result.items.isEmpty());
        assertEquals(result.items.size(),GeActivityFilter.apply(result,bank(item(21021,"Ancestral robe top",4)),low,WindowMarkersConfig.ActivityItems.ALL,true).result.items.size());
        assertTrue(low.locked(item(21021,"Ancestral robe top",4)).contains("75 Magic"));
        assertTrue(low.locked(item(21021,"Ancestral robe top",4)).contains("65 Defence"));
    }
    @Test public void suffixesAreExplicitAndKeepTierTokens() {
        var parsed=GeActivityFilter.Query.parse("toa top 2nd next upgrade",WindowMarkersConfig.ActivityItems.ALL);
        assertEquals("toa top 2nd",parsed.text);assertEquals(WindowMarkersConfig.ActivityItems.NEXT_UPGRADE,parsed.mode);
        assertEquals(WindowMarkersConfig.ActivityItems.UNOWNED,GeActivityFilter.Query.parse("toa not owned",WindowMarkersConfig.ActivityItems.ALL).mode);
        assertEquals("Shark",GeActivityFilter.Query.parse("Shark",WindowMarkersConfig.ActivityItems.ALL).text);
    }
}
