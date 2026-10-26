package com.windowmarkers;

import java.lang.reflect.Field;
import java.util.*;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarClientID;
import net.runelite.api.widgets.Widget;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class GeCatalogueSearchTest
{
    @Test public void coreFuzzyFallbackRunsOnlyForUnclaimedQueries() throws Exception {
        GeCatalogueSearch ours=ready();
        var core=new net.runelite.client.plugins.grandexchange.GrandExchangePlugin();
        var config=mock(net.runelite.client.plugins.grandexchange.GrandExchangeConfig.class);
        when(config.geSearchMode()).thenReturn(net.runelite.client.plugins.grandexchange.GrandExchangeSearchMode.FUZZY_FALLBACK);
        var manager=mock(net.runelite.client.game.ItemManager.class);
        set(core,"client",ours.client);set(core,"config",config);set(core,"itemManager",manager);
        var bus=new net.runelite.client.eventbus.EventBus();bus.register(core);bus.register(ours);
        // Registration order is intentionally reversed; the actual event priorities must decide.
        bus.post(new GrandExchangeSearched());verify(ours.client,times(1)).setGeSearchResultIds(any());
        verifyNoInteractions(manager);
        clearInvocations(ours.client);
        when(ours.client.getVarcStrValue(VarClientID.MESLAYERINPUT)).thenReturn("shark");
        when(ours.client.getItemCount()).thenReturn(1);
        ItemComposition shark=mock(ItemComposition.class);when(shark.getName()).thenReturn("Shark");
        when(shark.getId()).thenReturn(385);when(shark.getNote()).thenReturn(-1);when(shark.isGeTradeable()).thenReturn(true);
        when(manager.getItemComposition(0)).thenReturn(shark);
        bus.post(new GrandExchangeSearched());verify(manager).getItemComposition(0);
        verify(ours.client).setGeSearchResultIds(new short[]{385});
        verify(ours.client,never()).setVarcStrValue(anyInt(),anyString());
        set(core,"fuzzySearchScorer",new net.runelite.client.plugins.grandexchange.FuzzySearchScorer());
        clearInvocations(ours.client,manager);
        when(ours.client.getVarcStrValue(VarClientID.MESLAYERINPUT)).thenReturn("shrk");
        bus.post(new GrandExchangeSearched());
        verify(manager,times(2)).getItemComposition(0); // exact fallback, then fuzzy scoring
        verify(ours.client).setGeSearchResultIds(new short[]{385});
    }
    private static void set(Object target,String name,Object value) throws Exception
    { Field field=target.getClass().getDeclaredField(name); field.setAccessible(true); field.set(target,value); }
    private GeCatalogueSearch ready() throws Exception
    {
        GeCatalogueSearch search=new GeCatalogueSearch();
        search.client=mock(Client.class); search.config=new WindowMarkersConfig() {
            public boolean geCatalogueSearch() { return true; }
        };
        when(search.client.getGameState()).thenReturn(GameState.LOGGED_IN);
        when(search.client.getVarcStrValue(VarClientID.MESLAYERINPUT)).thenReturn("toa magic head");
        set(search,"active",true); set(search,"index",GeSearchIndexTest.index()); return search;
    }
    @Test public void publicSearchHookIsOptionalAndRespectsEarlierConsumers() throws Exception
    {
        GeCatalogueSearch search=ready(); GrandExchangeSearched event=new GrandExchangeSearched(); event.consume();
        search.onGrandExchangeSearched(event); verify(search.client,never()).setGeSearchResultCount(anyInt());
        search.config=new WindowMarkersConfig(){}; search.onGrandExchangeSearched(new GrandExchangeSearched());
        verify(search.client,never()).setGeSearchResultIds(any());
    }
    @Test public void resultsContainOnlyMatchingSlotsWithoutChangingOffers() throws Exception
    {
        GeCatalogueSearch search=ready(); GrandExchangeSearched event=new GrandExchangeSearched();
        search.onGrandExchangeSearched(event); assertTrue(event.isConsumed());
        verify(search.client).setGeSearchResultIndex(0); verify(search.client).setGeSearchResultCount(4);
        verify(search.client,never()).setVarcStrValue(anyInt(),anyString());
    }
    @Test public void resultLabelDecorationRestoresOnlyOurOwnChanges() throws Exception
    {
        GeCatalogueSearch search=ready(); search.onGrandExchangeSearched(new GrandExchangeSearched());
        Widget container=mock(Widget.class),background=mock(Widget.class),icon=mock(Widget.class),label=mock(Widget.class);
        when(background.getItemId()).thenReturn(-1); when(icon.getItemId()).thenReturn(21018);
        when(label.getText()).thenReturn("<u>Ancestral</u> hat"); when(label.getTextColor()).thenReturn(123);
        when(container.getDynamicChildren()).thenReturn(new Widget[]{background,label,icon});
        when(search.client.getWidget(InterfaceID.Chatbox.MES_LAYER_SCROLLCONTENTS)).thenReturn(container);
        ScriptPostFired script=new ScriptPostFired(ScriptID.GE_ITEM_SEARCH);
        search.onScriptPostFired(script); verify(label).setText("[1st] <u>Ancestral</u> hat");
        when(label.getText()).thenReturn("another plugin"); when(label.getTextColor()).thenReturn(456);
        clearInvocations(label); search.clear(); verify(label,never()).setText(anyString()); verify(label,never()).setTextColor(anyInt());
    }
    @Test public void changedInputDoesNotColourUnrelatedChatboxWidgets() throws Exception
    {
        GeCatalogueSearch search=ready(); search.onGrandExchangeSearched(new GrandExchangeSearched());
        when(search.client.getVarcStrValue(VarClientID.MESLAYERINPUT)).thenReturn("shark");
        ScriptPostFired script=new ScriptPostFired(ScriptID.GE_ITEM_SEARCH);
        search.onScriptPostFired(script); verify(search.client,never()).getWidget(InterfaceID.Chatbox.MES_LAYER_SCROLLCONTENTS);
    }
    @Test public void closingGeRestoresTheOriginalLabelAndColour() throws Exception
    {
        GeCatalogueSearch search=ready(); search.onGrandExchangeSearched(new GrandExchangeSearched());
        Widget container=mock(Widget.class),icon=mock(Widget.class),label=mock(Widget.class);
        when(icon.getItemId()).thenReturn(21018); when(label.getText()).thenReturn("Ancestral hat"); when(label.getTextColor()).thenReturn(123);
        when(container.getDynamicChildren()).thenReturn(new Widget[]{icon,label,null});
        when(search.client.getWidget(InterfaceID.Chatbox.MES_LAYER_SCROLLCONTENTS)).thenReturn(container);
        search.onScriptPostFired(new ScriptPostFired(ScriptID.GE_ITEM_SEARCH));
        when(label.getText()).thenReturn("[1st] Ancestral hat"); when(label.getTextColor()).thenReturn(search.config.geFirstColour().getRGB() & 0xffffff);
        clearInvocations(label); search.onWidgetClosed(new WidgetClosed(InterfaceID.GE_OFFERS,0,true));
        verify(label).setText("Ancestral hat"); verify(label).setTextColor(123);
    }
}
