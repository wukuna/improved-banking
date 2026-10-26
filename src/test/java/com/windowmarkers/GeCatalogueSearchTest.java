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
    @Test public void bothCoreFuzzyModesAndRegistrationOrdersPreserveSemanticQueries() throws Exception {
        for(var mode:Arrays.asList(net.runelite.client.plugins.grandexchange.GrandExchangeSearchMode.FUZZY_ONLY,
            net.runelite.client.plugins.grandexchange.GrandExchangeSearchMode.FUZZY_FALLBACK))
        for(boolean reverse:Arrays.asList(false,true)) {
            GeCatalogueSearch ours=ready();
            var core=new net.runelite.client.plugins.grandexchange.GrandExchangePlugin();
            var settings=mock(net.runelite.client.plugins.grandexchange.GrandExchangeConfig.class);
            when(settings.geSearchMode()).thenReturn(mode);
            var manager=mock(net.runelite.client.game.ItemManager.class);
            set(core,"client",ours.client);set(core,"config",settings);set(core,"itemManager",manager);
            var bus=new net.runelite.client.eventbus.EventBus();
            if(reverse) {bus.register(core);bus.register(ours);} else {bus.register(ours);bus.register(core);}
            for(String query:Arrays.asList("toa magic head","food 20","food 999","raw food","fast foods","rcb ammo")) {
                when(ours.client.getVarcStrValue(VarClientID.MESLAYERINPUT)).thenReturn(query);
                GrandExchangeSearched event=new GrandExchangeSearched();bus.post(event);assertTrue(event.isConsumed());
            }
            // Core disable/re-enable must not change handler precedence.
            bus.unregister(core);bus.register(core);bus.post(new GrandExchangeSearched());
            verifyNoInteractions(manager);verify(ours.client,times(7)).setGeSearchResultIds(any());
        }
    }
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
    @Test public void loadingDoesNotLeaveCoreFuzzyResultsInControlOnceTheCatalogueIsReady() throws Exception {
        GeCatalogueSearch ours=ready();set(ours,"index",null);set(ours,"preparing",true);
        var core=new net.runelite.client.plugins.grandexchange.GrandExchangePlugin();
        var settings=mock(net.runelite.client.plugins.grandexchange.GrandExchangeConfig.class);
        when(settings.geSearchMode()).thenReturn(net.runelite.client.plugins.grandexchange.GrandExchangeSearchMode.FUZZY_ONLY);
        var manager=mock(net.runelite.client.game.ItemManager.class);
        set(core,"client",ours.client);set(core,"config",settings);set(core,"itemManager",manager);
        set(core,"fuzzySearchScorer",new net.runelite.client.plugins.grandexchange.FuzzySearchScorer());
        var bus=new net.runelite.client.eventbus.EventBus();bus.register(core);bus.register(ours);
        bus.post(new GrandExchangeSearched());verify(ours.client).setGeSearchResultCount(0);
        set(ours,"index",GeSearchIndexTest.index());set(ours,"preparing",false);clearInvocations(ours.client);
        bus.post(new GrandExchangeSearched());verify(ours.client).setGeSearchResultCount(4);
        verifyNoInteractions(manager);
    }
    private static void set(Object target,String name,Object value) throws Exception
    { Field field=target.getClass().getDeclaredField(name); field.setAccessible(true); field.set(target,value); }
    private GeCatalogueSearch ready() throws Exception
    {
        GeCatalogueSearch search=new GeCatalogueSearch();
        search.client=mock(Client.class);
        search.config=WindowMarkersConfigProxyTest.proxy(Collections.singletonMap("geCatalogueSearch","true"));
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
    @Test public void pricesAndLockedTagsAreIndependentOfGearColoursAndRestoreCleanly() throws Exception
    {
        GeCatalogueSearch search=ready();search.config=new WindowMarkersConfig() {
            public boolean geCatalogueSearch() {return true;}public boolean geTierColours() {return false;}
            public boolean geShowPrices() {return true;}public boolean geMarkLocked() {return true;}
        };
        search.itemManager=mock(net.runelite.client.game.ItemManager.class);search.playerItems=owned(search,new Item(995,100));
        when(search.itemManager.getItemPrice(21018)).thenReturn(1_000_000L);
        search.onGrandExchangeSearched(new GrandExchangeSearched());
        Widget container=mock(Widget.class),icon=mock(Widget.class),label=mock(Widget.class);
        when(icon.getItemId()).thenReturn(21018);when(label.getText()).thenReturn("Ancestral hat");when(label.getTextColor()).thenReturn(123);
        when(container.getDynamicChildren()).thenReturn(new Widget[]{icon,label,null});when(search.client.getWidget(InterfaceID.Chatbox.MES_LAYER_SCROLLCONTENTS)).thenReturn(container);
        search.onScriptPostFired(new ScriptPostFired(ScriptID.GE_ITEM_SEARCH));
        String annotated="<col=ff7777>[Locked]</col> Ancestral hat <col=ff7777>~1m gp</col>";
        verify(label).setText(annotated);verify(label,never()).setTextColor(anyInt());
        when(label.getText()).thenReturn(annotated);clearInvocations(label);search.clear();verify(label).setText("Ancestral hat");
        verify(search.client,never()).setVarcStrValue(anyInt(),anyString());
    }
    @Test public void ownershipSuffixUsesTheRegularPluginWithoutWritingNativeInput() throws Exception
    {
        GeCatalogueSearch search=ready();search.playerItems=owned(search,new Item(21018,1));
        when(search.client.getVarcStrValue(VarClientID.MESLAYERINPUT)).thenReturn("toa magic head owned");
        GrandExchangeSearched event=new GrandExchangeSearched();search.onGrandExchangeSearched(event);assertTrue(event.isConsumed());
        verify(search.client).setGeSearchResultIds(new short[]{21018});verify(search.client,never()).setVarcStrValue(anyInt(),anyString());
    }
    @Test public void requirementChecksNeverRunNestedScriptsInsideGeCallbacks() throws Exception
    {
        GeCatalogueSearch search=ready();search.config=new WindowMarkersConfig() {
            public boolean geCatalogueSearch() {return true;}public boolean geMarkLocked() {return true;}
            public WindowMarkersConfig.ActivityItems geActivityFilter() {return WindowMarkersConfig.ActivityItems.UNOWNED;}
        };
        search.playerItems=owned(search,new Item(21018,1));
        search.onGrandExchangeSearched(new GrandExchangeSearched());
        Widget container=mock(Widget.class),icon=mock(Widget.class),label=mock(Widget.class);
        when(icon.getItemId()).thenReturn(4708);when(label.getText()).thenReturn("Ahrim's hood");
        when(container.getDynamicChildren()).thenReturn(new Widget[]{icon,label,null});
        when(search.client.getWidget(InterfaceID.Chatbox.MES_LAYER_SCROLLCONTENTS)).thenReturn(container);
        search.onScriptPostFired(new ScriptPostFired(ScriptID.GE_ITEM_SEARCH));
        verify(search.client,never()).runScript(any(Object[].class));
    }
    @Test public void hintsReuseOneOwnedWidgetAndDisappearOnClose() throws Exception
    {
        GeCatalogueSearch search=ready();set(search,"shopping",new GeCategorySearch(Collections.emptyList()));
        Widget parent=mock(Widget.class),hint=mock(Widget.class);
        when(parent.getWidth()).thenReturn(500);when(parent.getHeight()).thenReturn(150);when(parent.getChildren()).thenReturn(new Widget[]{hint});
        when(parent.createChild(eq(-1),anyInt())).thenReturn(hint);when(search.client.getWidget(InterfaceID.Chatbox.MES_LAYER)).thenReturn(parent);
        for(String query:Arrays.asList("toa","toa head","toa top","toa")) {
            when(search.client.getVarcStrValue(VarClientID.MESLAYERINPUT)).thenReturn(query);
            search.onGrandExchangeSearched(new GrandExchangeSearched());search.onBeforeRender(new BeforeRender());
        }
        verify(parent,times(1)).createChild(eq(-1),anyInt());
        clearInvocations(hint);search.onWidgetClosed(new WidgetClosed(InterfaceID.GE_OFFERS,0,true));verify(hint).setHidden(true);
        search.onBeforeRender(new BeforeRender());verify(hint,never()).setHidden(false);
    }
    @Test public void specialSearchSyntaxNeverGetsNormalizedIntoAShoppingQuery() throws Exception
    {
        GeCatalogueSearch search=ready();set(search,"shopping",new GeCategorySearch(Collections.emptyList()));
        for(String query:Arrays.asList("tag:tools","tools > 1","toa:owned")) {
            when(search.client.getVarcStrValue(VarClientID.MESLAYERINPUT)).thenReturn(query);GrandExchangeSearched event=new GrandExchangeSearched();
            search.onGrandExchangeSearched(event);assertFalse(event.isConsumed());
        }
    }
    private GePlayerItems owned(GeCatalogueSearch search,Item... items)
    {
        GePlayerItems tracker=new GePlayerItems();tracker.client=search.client;tracker.itemManager=mock(net.runelite.client.game.ItemManager.class);
        when(search.client.getAccountHash()).thenReturn(77L);
        ItemContainer empty=mock(ItemContainer.class);when(empty.getItems()).thenReturn(new Item[0]);
        when(search.client.getItemContainer(net.runelite.api.gameval.InventoryID.INV)).thenReturn(empty);
        when(search.client.getItemContainer(net.runelite.api.gameval.InventoryID.WORN)).thenReturn(empty);
        for(Item item:items) {
            ItemComposition def=mock(ItemComposition.class);when(def.getNote()).thenReturn(-1);when(def.getPlaceholderTemplateId()).thenReturn(-1);
            when(def.getName()).thenReturn(item.getId()==995?"Coins":"Ancestral hat");when(tracker.itemManager.getItemComposition(item.getId())).thenReturn(def);
        }
        ItemContainer bank=mock(ItemContainer.class);when(bank.getItems()).thenReturn(items);tracker.bankChanged(bank);return tracker;
    }
}
