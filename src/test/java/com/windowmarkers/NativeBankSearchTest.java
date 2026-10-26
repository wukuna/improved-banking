package com.windowmarkers;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.ScriptCallbackEvent;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarClientID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.game.ItemManager;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.bank.BankSearch;
import net.runelite.client.plugins.banktags.BankTag;
import net.runelite.client.plugins.banktags.BankTagsPlugin;
import net.runelite.client.plugins.banktags.BankTagsService;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class NativeBankSearchTest
{
    @Test public void initializesUsingTheActualBankTagsPublicModule() throws Exception {
        BankTagsPlugin existingBankTags=new BankTagsPlugin();
        net.runelite.client.plugins.banktags.TagManager tagManager=mock(net.runelite.client.plugins.banktags.TagManager.class);
        set(existingBankTags,"tagManager",tagManager);
        Method publicModule=BankTagsPlugin.class.getDeclaredMethod("getPublicModule");
        publicModule.setAccessible(true);
        Module exported=(Module)publicModule.invoke(existingBankTags);
        Injector root=Guice.createInjector(binder -> {
            binder.bind(Client.class).toInstance(mock(Client.class));
            binder.bind(ClientThread.class).toInstance(mock(ClientThread.class));
            binder.bind(ConfigManager.class).toInstance(mock(ConfigManager.class));
            binder.bind(WindowMarkersConfig.class).toInstance(mock(WindowMarkersConfig.class));
            binder.bind(ItemManager.class).toInstance(mock(ItemManager.class));
            binder.bind(BankSearch.class).toInstance(mock(BankSearch.class));
        });
        Injector child=root.createChildInjector(exported,
            binder -> { binder.bind(NativeBankSearch.class); binder.bind(BankSearchView.class); });
        NativeBankSearch search=child.getInstance(NativeBankSearch.class);
        assertSame(existingBankTags,search.bankTags);
        assertSame(search,child.getInstance(NativeBankSearch.class));
        BankSearchView view=child.getInstance(BankSearchView.class);
        assertSame(existingBankTags,view.bankTags); assertSame(tagManager,view.tags);
    }

    private NativeBankSearch ready() throws Exception {
        NativeBankSearch search=new NativeBankSearch(); search.client=mock(Client.class);
        search.config=mock(WindowMarkersConfig.class); search.bankTags=mock(BankTagsService.class); search.itemManager=mock(ItemManager.class);
        when(search.client.getGameState()).thenReturn(GameState.LOGGED_IN);
        when(search.client.getWidget(InterfaceID.Bankmain.UNIVERSE)).thenReturn(mock(Widget.class));
        when(search.client.getVarcIntValue(VarClientID.MESLAYERMODE)).thenReturn(11);
        when(search.config.improvedBankSearch()).thenReturn(true);
        when(search.client.getIntStackSize()).thenReturn(2); when(search.client.getObjectStackSize()).thenReturn(1);
        when(search.client.getObjectStack()).thenReturn(new Object[]{"farming"});
        when(search.client.getIntStack()).thenReturn(new int[]{0,5331});
        when(search.itemManager.canonicalize(5331)).thenReturn(5331);
        set(search,"query","farming"); set(search,"matches",new HashSet<>(Collections.singleton(5331))); search.start(); return search;
    }
    private void set(Object target,String name,Object value) throws Exception { Field f=target.getClass().getDeclaredField(name); f.setAccessible(true); f.set(target,value); }
    private void filter(NativeBankSearch search) { ScriptCallbackEvent event=new ScriptCallbackEvent(); event.setEventName("bankSearchFilter"); search.filter(event); }
    @Test public void optionalMatchesNeverHideNativeResultsOrEscapeActiveTags() throws Exception {
        NativeBankSearch search=ready(); filter(search); assertEquals(1,search.client.getIntStack()[0]);
        search.client.getIntStack()[0]=0; BankTag tag=mock(BankTag.class); when(search.bankTags.getActiveBankTag()).thenReturn(tag);
        filter(search); assertEquals(0,search.client.getIntStack()[0]);
        when(tag.contains(5331)).thenReturn(true); filter(search); assertEquals(1,search.client.getIntStack()[0]);
        when(search.client.getIntStack()).thenReturn(new int[]{1,385}); filter(search); assertEquals(1,search.client.getIntStack()[0]);
    }
    @Test public void toggleOffClosedBankChangedQueryAndResetPreventStaleMatches() throws Exception {
        NativeBankSearch search=ready(); when(search.config.improvedBankSearch()).thenReturn(false); filter(search); assertEquals(0,search.client.getIntStack()[0]);
        when(search.config.improvedBankSearch()).thenReturn(true); when(search.client.getWidget(InterfaceID.Bankmain.UNIVERSE)).thenReturn(null);
        filter(search); assertEquals(0,search.client.getIntStack()[0]);
        when(search.client.getWidget(InterfaceID.Bankmain.UNIVERSE)).thenReturn(mock(Widget.class));
        when(search.client.getObjectStack()).thenReturn(new Object[]{"food"}); filter(search); assertEquals(0,search.client.getIntStack()[0]);
        when(search.client.getObjectStack()).thenReturn(new Object[]{"farming"}); search.reset(); filter(search); assertEquals(0,search.client.getIntStack()[0]);
    }
    @Test public void savingSearchCannotCaptureThePreviousQueryResults() throws Exception {
        NativeBankSearch search=ready();
        when(search.client.getVarcStrValue(VarClientID.MESLAYERINPUT)).thenReturn("farming");
        assertEquals(Collections.singleton(5331),search.matches());
        when(search.client.getVarcStrValue(VarClientID.MESLAYERINPUT)).thenReturn("toa");
        assertTrue(search.matches().isEmpty());
    }

    @Test public void ranksDisappearWhenGuideQueryToggleBankOrProfileChanges() throws Exception {
        NativeBankSearch search=ready();
        set(search,"highlight",new Marker(0,0,48,36));
        set(search,"ranks",Collections.singletonMap(5331,new ItemCatalogueIndex.Rank(1,"test guide")));
        when(search.client.getVarcStrValue(VarClientID.MESLAYERINPUT)).thenReturn("farming");
        assertEquals(1,search.ranks().size());
        when(search.config.geGuideContext()).thenReturn("new guide");assertTrue(search.ranks().isEmpty());
        filter(search); assertEquals(0,search.client.getIntStack()[0]);
        when(search.config.geGuideContext()).thenReturn("");assertEquals(1,search.ranks().size());
        when(search.config.improvedBankSearch()).thenReturn(false);assertTrue(search.ranks().isEmpty());
        when(search.config.improvedBankSearch()).thenReturn(true);
        when(search.client.getVarcStrValue(VarClientID.MESLAYERINPUT)).thenReturn("toa");assertTrue(search.ranks().isEmpty());
        when(search.client.getVarcStrValue(VarClientID.MESLAYERINPUT)).thenReturn("farming");
        when(search.client.getWidget(InterfaceID.Bankmain.UNIVERSE)).thenReturn(null);assertTrue(search.ranks().isEmpty());
        when(search.client.getWidget(InterfaceID.Bankmain.UNIVERSE)).thenReturn(mock(Widget.class));
        search.reset();assertTrue(search.ranks().isEmpty());
    }

    @Test public void disabledOrChangedSearchCannotKeepOldHighlightAndResetRemovesListener() throws Exception {
        NativeBankSearch search=ready(); Marker marker=new Marker(0,0,48,36);
        set(search,"highlight",marker);
        when(search.client.getVarcStrValue(VarClientID.MESLAYERINPUT)).thenReturn("farming");
        assertSame(marker,search.highlight());
        when(search.config.improvedBankSearch()).thenReturn(false); assertNull(search.highlight());
        when(search.config.improvedBankSearch()).thenReturn(true);
        when(search.client.getVarcStrValue(VarClientID.MESLAYERINPUT)).thenReturn("toa"); assertNull(search.highlight());
        Widget checkbox=mock(Widget.class); set(search,"checkbox",checkbox); search.reset();
        verify(checkbox).setHidden(true); verify(checkbox).clearActions();
        verify(checkbox).setOnOpListener((Object[])null); verify(checkbox).setHasListener(false);
    }

    @Test public void switchingOffInPluginSettingsRebuildsResultsOnlyOnce() throws Exception {
        NativeBankSearch search=ready(); search.bankSearch=mock(BankSearch.class);
        when(search.config.improvedBankSearch()).thenReturn(false);
        when(search.client.getVarcStrValue(VarClientID.MESLAYERINPUT)).thenReturn("farming");
        search.update(); search.update();
        verify(search.bankSearch,times(1)).layoutBank(); assertTrue(search.matches().isEmpty());
    }

    @Test public void changedBankContentsClearHighlightEvenWhenQueryIsUnchanged() throws Exception {
        NativeBankSearch search=ready(); search.bankSearch=mock(BankSearch.class);
        search.clientThread=mock(ClientThread.class);
        java.util.concurrent.CountDownLatch applied=new java.util.concurrent.CountDownLatch(2);
        doAnswer(call -> { ((Runnable)call.getArgument(0)).run(); applied.countDown(); return null; })
            .when(search.clientThread).invokeLater(any(Runnable.class));
        set(search,"highlight",new Marker(0,0,48,36));
        when(search.client.getVarcStrValue(VarClientID.MESLAYERINPUT)).thenReturn("farming");
        search.index(new BankSearchIndex(Collections.emptyList(),Collections.emptyList(),new CustomPresets()));
        assertTrue(applied.await(10,java.util.concurrent.TimeUnit.SECONDS));
        assertTrue(search.matches().isEmpty()); assertNull(search.highlight());
        verify(search.bankSearch).layoutBank();
    }

    @Test public void newQueryWithSameMatchesStillRestoresResultsAfterNativeSearchRebuild() throws Exception {
        NativeBankSearch search=ready(); search.bankSearch=mock(BankSearch.class);
        search.clientThread=mock(ClientThread.class);
        java.util.concurrent.CountDownLatch applied=new java.util.concurrent.CountDownLatch(1);
        doAnswer(call -> { ((Runnable)call.getArgument(0)).run(); applied.countDown(); return null; })
            .when(search.clientThread).invokeLater(any(Runnable.class));
        BankSearchIndex index=new BankSearchIndex(Collections.singletonList(new AutoCategories.Item(5331,"Watering can",false,null,0,0,0)),Collections.emptyList(),new CustomPresets());
        set(search,"index",index);
        Set<Integer> expected=index.find("farmng"); assertEquals(Collections.singleton(5331),expected);
        set(search,"matches",expected);
        when(search.client.getVarcStrValue(VarClientID.MESLAYERINPUT)).thenReturn("farmng");
        search.update(); assertTrue(applied.await(10,java.util.concurrent.TimeUnit.SECONDS));
        assertEquals(expected,search.matches()); verify(search.bankSearch).layoutBank();
        clearInvocations(search.bankSearch);
        search.index(index); // A refresh of identical data must not cause repeated bank-tab rebuilds.
        javax.swing.SwingUtilities.invokeAndWait(() -> {});
        verify(search.bankSearch,after(200).never()).layoutBank();
    }

    @Test public void failedRefreshDropsOldOptionalResultsAndRestoresNativeSearch() throws Exception {
        NativeBankSearch search=ready(); search.bankSearch=mock(BankSearch.class);
        search.clientThread=mock(ClientThread.class);
        java.util.concurrent.CountDownLatch applied=new java.util.concurrent.CountDownLatch(2);
        doAnswer(call -> { ((Runnable)call.getArgument(0)).run(); applied.countDown(); return null; })
            .when(search.clientThread).invokeLater(any(Runnable.class));
        set(search,"highlight",new Marker(0,0,48,36)); set(search,"slots",true);
        when(search.client.getVarcStrValue(VarClientID.MESLAYERINPUT)).thenReturn("farming");
        BankSearchIndex broken=new BankSearchIndex(Collections.emptyList(),Collections.emptyList(),new CustomPresets());
        // Simulate an index failure after a previous successful query.
        set(broken,"catalogue",null);
        search.index(broken);
        assertTrue(applied.await(10,java.util.concurrent.TimeUnit.SECONDS));
        assertTrue(search.matches().isEmpty()); assertNull(search.highlight()); assertFalse(search.groupBySlot());
        filter(search); assertEquals(0,search.client.getIntStack()[0]);
        verify(search.bankSearch).layoutBank();
    }
}
