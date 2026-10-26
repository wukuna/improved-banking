package com.windowmarkers;

import java.util.*;
import net.runelite.api.*;
import net.runelite.api.gameval.InventoryID;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class GePlayerItemsTest
{
    private GePlayerItems tracker() {
        GePlayerItems tracker=new GePlayerItems();tracker.client=mock(Client.class);tracker.itemManager=mock(net.runelite.client.game.ItemManager.class);
        when(tracker.client.getGameState()).thenReturn(GameState.LOGGED_IN);when(tracker.client.getAccountHash()).thenReturn(77L);
        definition(tracker,995,"Coins",-1,-1,-1);definition(tracker,13204,"Platinum token",-1,-1,-1);definition(tracker,385,"Shark",-1,-1,-1);
        ItemContainer inventory=container(new Item(995,10));when(tracker.client.getItemContainer(InventoryID.INV)).thenReturn(inventory);
        ItemContainer worn=container();when(tracker.client.getItemContainer(InventoryID.WORN)).thenReturn(worn);
        return tracker;
    }
    private void definition(GePlayerItems tracker,int id,String name,int note,int linked,int placeholder) {
        ItemComposition def=mock(ItemComposition.class);when(def.getName()).thenReturn(name);when(def.getNote()).thenReturn(note);
        when(def.getLinkedNoteId()).thenReturn(linked);when(def.getPlaceholderTemplateId()).thenReturn(placeholder);
        when(tracker.itemManager.getItemComposition(id)).thenReturn(def);
    }
    private ItemContainer container(Item... items) { ItemContainer c=mock(ItemContainer.class);when(c.getItems()).thenReturn(items);return c; }
    @Test public void bankNotesBecomeItemsButPlaceholdersAndZeroQuantityDoNot() {
        GePlayerItems t=tracker();definition(t,386,"Shark",799,385,-1);definition(t,40000,"Shark",-1,-1,14401);
        t.bankChanged(container(new Item(386,5),new Item(40000,1),new Item(13204,0)));
        var snapshot=t.snapshot();assertTrue(snapshot.complete);
        assertNotNull(snapshot.owned(GeSearchIndexTest.item(385,"Shark",-1)));
        assertNull(snapshot.owned(GeSearchIndexTest.item(13204,"Platinum token",-1)));
        assertEquals(2,snapshot.items.size());
    }
    @Test public void coinsAndPlatinumUseLongArithmeticAndExcludeUncollectedOffers() {
        GePlayerItems t=tracker();t.bankChanged(container(new Item(995,Integer.MAX_VALUE),new Item(13204,Integer.MAX_VALUE)));
        var snapshot=t.snapshot();assertEquals(2_149_631_130_657L,snapshot.funds);assertTrue(snapshot.fundsComplete);
        assertEquals("55dd88",GeCatalogueSearch.priceColour(3_000_000_000L,snapshot));
        verify(t.client,never()).getGrandExchangeOffers();
    }
    @Test public void offerUpdatesInvalidateBankMoneyUntilObservedAgain() {
        GePlayerItems t=tracker();t.bankChanged(container(new Item(995,10000)));assertTrue(t.snapshot().fundsComplete);
        t.offersChanged();var snapshot=t.snapshot();assertTrue(snapshot.complete);assertFalse(snapshot.fundsComplete);assertEquals(10,snapshot.funds);
        assertEquals("c0c0c0",GeCatalogueSearch.priceColour(100,snapshot));assertEquals("55dd88",GeCatalogueSearch.priceColour(5,snapshot));
        t.bankChanged(container(new Item(995,0)));assertEquals("ff7777",GeCatalogueSearch.priceColour(100,t.snapshot()));
    }
    @Test public void accountChangesAndExplicitResetCannotReuseAnotherBank() {
        GePlayerItems t=tracker();t.bankChanged(container(new Item(385,50)));assertTrue(t.snapshot().complete);
        when(t.client.getAccountHash()).thenReturn(88L);assertFalse(t.snapshot().complete);assertNull(t.snapshot().owned(GeSearchIndexTest.item(385,"Shark",-1)));
        t.bankChanged(container(new Item(385,1)));t.clear();assertFalse(t.snapshot().complete);
    }
    @Test public void pricesAreNeutralWhenMissingAndPreserveLargeAmounts() {
        var s=new GePlayerItems.Snapshot(true,Collections.emptyList(),0,true);
        assertEquals("c0c0c0",GeCatalogueSearch.priceColour(0,s));assertEquals("ff7777",GeCatalogueSearch.priceColour(1,s));
        assertEquals("3b",GeCatalogueSearch.compactPrice(3_000_000_000L));assertEquals("2.1t",GeCatalogueSearch.compactPrice(2_149_631_130_647L));
    }
}
