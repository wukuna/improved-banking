package com.windowmarkers;
import java.util.*;
import org.junit.*;
import static org.junit.Assert.*;

public class SearchQualifiersTest {
    private static ItemCatalogueIndex ge;
    @BeforeClass public static void catalogue() { ge=GeSearchIndexTest.index(); }
    private Set<Integer> ids(String query) { var result=ge.find(query,"");assertNotNull(query,result);Set<Integer> ids=new HashSet<>();result.items.forEach(i->ids.add(i.id));return ids; }
    @Test public void tierQualifiersPreserveGuideColumnsAndExcludeUnrankedBrokenItems() {
        for(String word:new String[]{"1","1st","bis","first"}) assertEquals(Collections.singleton(21018),ids("toa magic head "+word));
        for(String word:new String[]{"2","2nd","second"}) assertEquals(Collections.singleton(26241),ids("toa magic head "+word));
        for(String word:new String[]{"3","3rd","third"}) assertEquals(Collections.singleton(4708),ids("toa magic head "+word));
        assertEquals(new HashSet<>(Arrays.asList(21018,4708)),ids("toa magic head 1 3"));
        assertTrue(ids("food bis").isEmpty());
    }
    @Test public void aliasesApplyToBothBankAndGe() {
        assertEquals(ids("toa body"),ids("toa top"));assertEquals(ids("toa legs"),ids("toa bottom"));
        assertEquals(ids("toa head"),ids("toa helmet"));assertEquals(ids("cox hands"),ids("cox gloves"));
        List<AutoCategories.Item> items=new ArrayList<>();for(var item:ge.find("toa","").items) items.add(item.fact);
        BankSearchIndex bank=new BankSearchIndex(items,Collections.emptyList(),new CustomPresets());
        assertEquals(ids("toa magic head 2nd"),bank.find("toa magic head 2nd"));
        assertEquals(ids("toa top"),bank.find("toa top"));
    }
    @Test public void numberedItemNamesAndOtherSearchSyntaxRemainNative() {
        var index=new ItemCatalogueIndex(Collections.singletonList(GeSearchIndexTest.item(6691,"Saradomin brew(1)",-1)));
        assertNull(index.find("Saradomin brew(1)",""));
        assertNull(index.find("tag:toa 1st",""));assertNull(index.find("ge>1000",""));
    }
}
