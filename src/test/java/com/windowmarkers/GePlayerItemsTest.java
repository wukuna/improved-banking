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
    @Test public void reducedEquipmentDoesNotProveOwnershipOfItsChargedBenefits() {
        int[][] pairs={{1704,1712,2},{19550,20655,12},{25539,25541,12},{13244,13243,3},{23682,23680,3}};
        String[][] names={{"Amulet of glory","Amulet of glory(4)"},{"Ring of suffering","Ring of suffering (r)"},
            {"Celestial ring (uncharged)","Celestial ring"},{"Infernal pickaxe (uncharged)","Infernal pickaxe"},
            {"Crystal pickaxe (inactive)","Crystal pickaxe"}};
        for(int n=0;n<pairs.length;n++) {
            var reduced=GeSearchIndexTest.item(pairs[n][0],names[n][0],pairs[n][2]);
            var charged=GeSearchIndexTest.item(pairs[n][1],names[n][1],pairs[n][2]);
            var owned=new GePlayerItems.Snapshot(true,Collections.singletonList(reduced),0,true);
            assertSame("Exact ownership is still factual",reduced,owned.owned(reduced));
            assertNull("Missing benefit: "+charged.name,owned.owned(charged));
        }
    }
    @Test public void healthyCosmeticsAndPartialDurabilityStillCountAsUsableEquivalents() {
        var partial=GeSearchIndexTest.item(4859,"Ahrim's hood 25",0);
        var cosmetic=GeSearchIndexTest.item(24666,"Twisted ancestral robe top",4);
        var owned=new GePlayerItems.Snapshot(true,Arrays.asList(partial,cosmetic),0,true);
        assertSame(partial,owned.owned(GeSearchIndexTest.item(4708,"Ahrim's hood",0)));
        assertSame(cosmetic,owned.owned(GeSearchIndexTest.item(21021,"Ancestral robe top",4)));
        assertNull(new GePlayerItems.Snapshot(true,Collections.singletonList(
            GeSearchIndexTest.item(4860,"Ahrim's hood 0",0)),0,true).owned(GeSearchIndexTest.item(4708,"Ahrim's hood",0)));
    }
    @Test public void ordinaryTridentDoesNotHideTheEnhancedCapacityPurchase() {
        var basic=GeSearchIndexTest.item(12899,"Trident of the Swamp",3);
        var enhanced=GeSearchIndexTest.item(22292,"Trident of the Swamp (e)",3);
        var purchase=GeSearchIndexTest.item(22294,"Uncharged toxic trident (e)",3);
        assertNull(new GePlayerItems.Snapshot(true,Collections.singletonList(basic),0,true).owned(purchase));
        assertSame(enhanced,new GePlayerItems.Snapshot(true,Collections.singletonList(enhanced),0,true).owned(purchase));
        assertSame(enhanced,new GePlayerItems.Snapshot(true,Collections.singletonList(enhanced),0,true).owned(basic));
    }
}
