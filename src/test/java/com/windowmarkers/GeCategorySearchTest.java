package com.windowmarkers;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class GeCategorySearchTest
{
    private List<ItemCatalogueIndex.Item> items() { return Arrays.asList(
        GeSearchIndexTest.item(5341,"Rake",-1),GeSearchIndexTest.item(5343,"Seed dibber",-1),GeSearchIndexTest.item(5295,"Ranarr seed",-1),
        GeSearchIndexTest.item(5315,"Yew seed",-1),GeSearchIndexTest.item(5373,"Yew sapling",-1),GeSearchIndexTest.item(5321,"Watermelon seed",-1),
        GeSearchIndexTest.item(3008,"Energy potion(4)",-1),GeSearchIndexTest.item(3016,"Super energy(4)",-1),
        GeSearchIndexTest.item(11920,"Dragon pickaxe",3),GeSearchIndexTest.item(5325,"Gardening trowel",-1)); }
    private Set<Integer> ids(ItemCatalogueIndex.Result result) { assertNotNull(result);Set<Integer> ids=new HashSet<>();result.items.forEach(i->ids.add(i.id));return ids; }
    @Test public void toolsAreNotACompleteEquipmentOrSupplyLoadout() {
        GeCategorySearch search=new GeCategorySearch(items());Set<Integer> tools=ids(search.find("tools"));
        assertTrue(tools.contains(5341));assertTrue(tools.contains(11920));assertFalse(tools.contains(3008));assertFalse(tools.contains(3016));assertFalse(tools.contains(5295));
        assertFalse(ids(search.find("farming tools")).contains(11920));assertTrue(ids(search.find("farming tools")).contains(5343));
    }
    @Test public void seedsTreesAndToolsHaveDifferentMembership() {
        GeCategorySearch search=new GeCategorySearch(items());
        assertTrue(ids(search.find("farming seeds")).containsAll(Arrays.asList(5295,5315,5321)));
        assertFalse(ids(search.find("farming seeds")).contains(5373));
        assertEquals(new HashSet<>(Arrays.asList(5315,5373)),ids(search.find("farming trees")));
        assertFalse(ids(search.find("farming")).contains(3008));assertFalse(ids(search.find("farming")).contains(11920));
    }
    @Test public void hintsStaySmallAndNeverDuplicateSlots() {
        GeCategorySearch search=new GeCategorySearch(items());
        assertEquals(Arrays.asList("farming seeds","farming trees","farming tools"),search.suggestions("farming",false));
        assertTrue(search.suggestions("farming",false).stream().allMatch(s->!search.find(s).items.isEmpty()));
        assertTrue(search.suggestions("toa head",true).stream().noneMatch(s->s.contains("head head")));
        assertNull(search.find("toa"));assertNull(search.find("farming potion"));
    }
    @Test public void bankMatchingRemainsIndependentOfShoppingCategories() {
        ItemCatalogueIndex bank=new ItemCatalogueIndex(items(),false);
        var before=bank.find("farming","");new GeCategorySearch(items()).find("farming tools");
        assertEquals(ids(before),ids(bank.find("farming","")));
    }
}
