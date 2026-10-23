package com.windowmarkers;

import java.util.*;
import javax.swing.SwingUtilities;
import org.junit.Test;
import static org.junit.Assert.*;

public class LookupSearchTest
{
    @Test public void lookupOffersSeparateFilterAndHighlightActionsForOneOrTwoItems() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ItemLookupPanel one=new ItemLookupPanel(Collections.singletonList("Shark"),Collections.singletonList(Collections.singleton("Category: Food — general use")));
            assertFalse(one.send.isEnabled()); one.list.setSelectedIndex(0); one.send.doClick();
            assertEquals("Food — general use",one.filter); assertFalse(one.highlightRequested);
            ItemLookupPanel two=new ItemLookupPanel(Arrays.asList("Fire cape","Fire max cape"),Arrays.asList(
                new TreeSet<>(Arrays.asList("Category: Capes","Preset: Chambers of Xeric / CoX Melee")),
                new TreeSet<>(Arrays.asList("Category: Capes","Category: Runecraft — pouch protection"))));
            assertEquals(3,two.list.getModel().getSize());
            assertEquals("Both items",two.list.getModel().getElementAt(0).owner);
            two.highlight.doClick(); assertTrue(two.highlightRequested); assertNull(two.filter);
            ItemLookupPanel unknown=new ItemLookupPanel(Collections.singletonList("Unknown"),Collections.singletonList(Collections.emptySet()));
            assertFalse(unknown.send.isEnabled()); assertTrue(unknown.highlight.isEnabled());
        });
    }
    @Test public void membershipFiltersKeepCategoryAndSavedGroupNamesIntact() {
        assertEquals("CoX Melee",ItemLookupPanel.filterText("Preset: Chambers of Xeric / CoX Melee"));
        assertEquals("Vorkath — Example inventory",ItemLookupPanel.filterText("Wiki setup: Vorkath / Example inventory — state needs review"));
        assertEquals("My food / potions",ItemLookupPanel.filterText("Saved group: My food / potions"));
    }
    @Test public void bankSearchFindsNamesCategoriesAndCustomGroupsButDoesNotAbsorbNativeSyntax() {
        List<AutoCategories.Item> bank=Arrays.asList(
            new AutoCategories.Item(6570,"Fire cape",true,null,0,0,0),
            new AutoCategories.Item(385,"Shark",false,new String[]{"Eat"},0,0,0),
            new AutoCategories.Item(5331,"Watering can",false,null,0,0,0));
        Marker food=new Marker(0,0,48,36); food.label="My Food"; food.linkedItemIds=new int[]{385,3144};
        BankSearchIndex index=new BankSearchIndex(bank,Collections.singletonList(food),new CustomPresets());
        assertEquals(Collections.singleton(385),index.find("shark"));
        assertEquals(Collections.singleton(385),index.find("My Food"));
        assertTrue(index.find("farming").contains(5331));
        assertTrue(index.find("cox").contains(6570));
        assertFalse(index.find("cox").contains(5331));
        for(String query:new String[]{""," ","tag:food",">100k","qty>=10","ha:1000"}) assertTrue(query,index.find(query).isEmpty());
    }
}
