package com.windowmarkers;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.banktags.*;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class BankSearchViewTest
{
    private static final class ReopenFixture {
        final BankSearchView view=new BankSearchView();
        final AtomicReference<String> active=new AtomicReference<>();
        final AtomicReference<BankTag> registered=new AtomicReference<>();
        final List<Runnable> queued=new ArrayList<>();
        final net.runelite.api.widgets.Widget bank=mock(net.runelite.api.widgets.Widget.class);
        final net.runelite.api.widgets.Widget title=mock(net.runelite.api.widgets.Widget.class);
        final String own;
        ReopenFixture() {
            view.client=mock(net.runelite.api.Client.class);view.clientThread=mock(net.runelite.client.callback.ClientThread.class);
            view.tags=mock(TagManager.class);view.bankTags=mock(BankTagsService.class);view.items=mock(ItemManager.class);
            when(view.client.getGameState()).thenReturn(net.runelite.api.GameState.LOGGED_IN);
            when(view.client.getWidget(net.runelite.api.gameval.InterfaceID.Bankmain.UNIVERSE)).thenReturn(bank);
            when(view.client.getWidget(net.runelite.api.gameval.InterfaceID.Bankmain.TITLE)).thenReturn(title);
            when(view.bankTags.getActiveTag()).thenAnswer(i->active.get());
            when(view.items.canonicalize(anyInt())).thenAnswer(i->i.getArgument(0));
            doAnswer(i->{registered.set(i.getArgument(1));return null;}).when(view.tags).registerTag(anyString(),any());
            doAnswer(i->{registered.set(null);return null;}).when(view.tags).unregisterTag(anyString());
            doAnswer(i->{active.set(i.getArgument(0));return null;}).when(view.bankTags).openBankTag(anyString(),anyInt());
            doAnswer(i->{active.set(null);return null;}).when(view.bankTags).closeBankTag();
            doAnswer(i->{queued.add(i.getArgument(0));return null;}).when(view.clientThread).invokeLater(any(Runnable.class));
            Marker item=new Marker(0,0,48,36);item.linkedItemIds=new int[]{25731};
            view.show("Sire",Collections.singletonList(item),true);own=active.get();
        }
        void close() { view.bankClosed();active.set(null);when(bank.isHidden()).thenReturn(true);view.update(); }
        void open(boolean remember) { when(bank.isHidden()).thenReturn(false);active.set(remember?own:null);view.update(); }
        void run() { new ArrayList<>(queued).forEach(Runnable::run);queued.clear(); }
    }
    @Test public void closingAndReopeningRetainsContentsAndRestoresSafeDisplayOptions() {
        for(boolean remember:new boolean[]{false,true}) {
            ReopenFixture f=new ReopenFixture();BankTag contents=f.registered.get();
            f.close();assertSame(contents,f.registered.get());assertTrue(contents.contains(25731));assertFalse(contents.contains(25733));
            f.open(remember);f.view.update();assertEquals(1,f.queued.size());
            verify(f.view.bankTags,times(1)).openBankTag(anyString(),anyInt());
            f.run();f.view.update();assertTrue(f.view.isActive());assertSame(contents,f.registered.get());
            verify(f.view.bankTags,times(2)).openBankTag(eq(f.own),eq(BankTagsService.OPTION_NO_LAYOUT|BankTagsService.OPTION_HIDE_TAG_NAME));
            verify(f.title).setText("Bank view: Sire");
            f.active.set(null);f.view.update();assertNull(f.registered.get());assertFalse(f.view.isActive());
        }
    }
    @Test public void pendingReopenNeverOverridesAnotherTabOrSearch() {
        for(int change=0;change<3;change++) {
            ReopenFixture f=new ReopenFixture();f.close();f.open(true);
            if(change==0) f.active.set("Other plugin tab");
            if(change==1) { f.active.set(null);when(f.view.client.getVarbitValue(net.runelite.api.gameval.VarbitID.BANK_CURRENTTAB)).thenReturn(2); }
            if(change==2) { f.active.set(null);when(f.view.client.getVarcIntValue(net.runelite.api.gameval.VarClientID.MESLAYERMODE)).thenReturn(net.runelite.api.vars.InputType.SEARCH.getType()); }
            f.run();assertNull(f.registered.get());assertFalse(f.view.isActive());
            verify(f.view.bankTags,times(1)).openBankTag(anyString(),anyInt());verify(f.view.bankTags,never()).closeBankTag();
        }
    }
    @Test public void clearingAClosedPresetClearsItsRememberedNameAndCancelsQueuedRestore() {
        ReopenFixture f=new ReopenFixture();f.close();f.open(true);f.close();f.view.clear();f.run();
        assertNull(f.registered.get());assertFalse(f.view.isActive());
        verify(f.view.bankTags).closeBankTag();verify(f.view.bankTags,times(1)).openBankTag(anyString(),anyInt());
    }
    @Test public void closingAgainBeforeDeferredRestoreKeepsContentsForNextOpening() {
        ReopenFixture f=new ReopenFixture();f.close();f.open(true);f.close();f.run();
        assertNotNull(f.registered.get());f.open(true);f.run();assertTrue(f.view.isActive());
        verify(f.view.bankTags,times(2)).openBankTag(anyString(),anyInt());
    }
    @Test public void budgetPreviewsOpenWithoutOwnedMatchesAndHighlightsOnlyContainOwnedIds() {
        BankSearchView view=new BankSearchView();view.client=mock(net.runelite.api.Client.class);
        view.tags=mock(TagManager.class);view.bankTags=mock(BankTagsService.class);view.items=mock(ItemManager.class);
        AtomicReference<String> active=new AtomicReference<>();AtomicReference<BankTag> tag=new AtomicReference<>();
        when(view.bankTags.getActiveTag()).thenAnswer(i->active.get());
        doAnswer(i->{active.set(i.getArgument(0));return null;}).when(view.bankTags).openBankTag(anyString(),anyInt());
        doAnswer(i->{tag.set(i.getArgument(1));return null;}).when(view.tags).registerTag(anyString(),any());
        doAnswer(i->{active.set(null);return null;}).when(view.bankTags).closeBankTag();
        when(view.items.canonicalize(anyInt())).thenAnswer(i->i.getArgument(0));
        var title=mock(net.runelite.api.widgets.Widget.class);
        when(view.client.getWidget(net.runelite.api.gameval.InterfaceID.Bankmain.UNIVERSE)).thenReturn(mock(net.runelite.api.widgets.Widget.class));
        when(view.client.getWidget(net.runelite.api.gameval.InterfaceID.Bankmain.TITLE)).thenReturn(title);
        var plan=SetupLoadout.build(ActivitySetups.forActivity("Callisto"),Collections.emptyMap(),Collections.emptySet(),"");
        view.show("Callisto",Collections.emptyList(),true,plan,Collections.emptyMap());
        assertTrue(view.isActive());assertNotNull(view.loadout());assertNull(view.highlight());
        assertFalse(tag.get().contains(2503));assertFalse(tag.get().contains(27238));
        view.update();verify(title).setText("Bank view: Callisto - not enough gear in bank");
        Marker potion=new Marker(0,0,48,36);potion.linkedItemIds=new int[]{23733};
        view.show("Callisto",Collections.singletonList(potion),true,plan,Collections.emptyMap());
        assertArrayEquals(new int[]{23733},view.highlight().linkedItemIds);
        active.set("Another tab");view.update();assertNull(view.highlight());assertNull(view.loadout());
    }
    @Test public void pendingViewCannotReopenAfterSearchOrTabChanges() {
        BankSearchView view=new BankSearchView();view.client=mock(net.runelite.api.Client.class);view.bankTags=mock(BankTagsService.class);
        var bank=mock(net.runelite.api.widgets.Widget.class);
        when(view.client.getGameState()).thenReturn(net.runelite.api.GameState.LOGGED_IN);
        when(view.client.getWidget(net.runelite.api.gameval.InterfaceID.Bankmain.UNIVERSE)).thenReturn(bank);
        when(view.client.getVarcIntValue(net.runelite.api.gameval.VarClientID.MESLAYERMODE)).thenReturn(11);
        when(view.client.getVarcStrValue(net.runelite.api.gameval.VarClientID.MESLAYERINPUT)).thenReturn("cox");
        BankTag original=id -> true;
        when(view.bankTags.getActiveTag()).thenReturn("raid");when(view.bankTags.getActiveBankTag()).thenReturn(original);
        var unchanged=view.unchangedContext();assertTrue(unchanged.getAsBoolean());
        when(view.client.getVarcStrValue(net.runelite.api.gameval.VarClientID.MESLAYERINPUT)).thenReturn("");assertFalse(unchanged.getAsBoolean());
        when(view.client.getVarcStrValue(net.runelite.api.gameval.VarClientID.MESLAYERINPUT)).thenReturn("cox");
        when(view.client.getVarcIntValue(net.runelite.api.gameval.VarClientID.MESLAYERMODE)).thenReturn(0);assertFalse(unchanged.getAsBoolean());
        when(view.client.getVarcIntValue(net.runelite.api.gameval.VarClientID.MESLAYERMODE)).thenReturn(11);
        when(view.client.getVarbitValue(net.runelite.api.gameval.VarbitID.BANK_CURRENTTAB)).thenReturn(2);assertFalse(unchanged.getAsBoolean());
        when(view.client.getVarbitValue(net.runelite.api.gameval.VarbitID.BANK_CURRENTTAB)).thenReturn(0);
        when(view.bankTags.getActiveTag()).thenReturn("supplies");assertFalse(unchanged.getAsBoolean());
        when(view.bankTags.getActiveTag()).thenReturn("raid");
        when(view.bankTags.getActiveBankTag()).thenReturn(id -> false);assertFalse(unchanged.getAsBoolean());
        when(view.bankTags.getActiveBankTag()).thenReturn(original);assertTrue(unchanged.getAsBoolean());
        when(bank.isHidden()).thenReturn(true);assertFalse(unchanged.getAsBoolean());when(bank.isHidden()).thenReturn(false);
        when(view.client.getWidget(net.runelite.api.gameval.InterfaceID.Bankmain.UNIVERSE)).thenReturn(mock(net.runelite.api.widgets.Widget.class));assertFalse(unchanged.getAsBoolean());
        when(view.client.getWidget(net.runelite.api.gameval.InterfaceID.Bankmain.UNIVERSE)).thenReturn(bank);
        when(view.client.getGameState()).thenReturn(net.runelite.api.GameState.LOGIN_SCREEN);assertFalse(unchanged.getAsBoolean());
    }
    @Test public void noViewCanStartWithoutAnOpenBank() {
        BankSearchView view=new BankSearchView();view.client=mock(net.runelite.api.Client.class);
        assertFalse(view.unchangedContext().getAsBoolean());
        verify(view.client,never()).getVarcIntValue(anyInt());
    }
    @Test public void temporaryViewRetainsExactStatesAndDoesNotCloseAnotherPluginsTab() {
        BankSearchView view=new BankSearchView(); view.tags=mock(TagManager.class); view.bankTags=mock(BankTagsService.class); view.items=mock(ItemManager.class);
        AtomicReference<BankTag> registered=new AtomicReference<>(),active=new AtomicReference<>();
        AtomicReference<String> name=new AtomicReference<>();
        doAnswer(i -> {registered.set(i.getArgument(1));return null;}).when(view.tags).registerTag(anyString(),any());
        doAnswer(i -> { BankTag custom=registered.get(); active.set(id -> custom.contains(id)); name.set(i.getArgument(0)); return null;}).when(view.bankTags).openBankTag(anyString(),anyInt());
        doAnswer(i -> {name.set(null);return null;}).when(view.bankTags).closeBankTag();
        when(view.bankTags.getActiveBankTag()).thenAnswer(i -> active.get());
        when(view.bankTags.getActiveTag()).thenAnswer(i -> name.get());
        when(view.items.canonicalize(anyInt())).thenAnswer(i -> i.getArgument(0));
        Marker charged=new Marker(0,0,48,36); charged.linkedItemIds=new int[]{25731};
        view.show("ToB",Collections.singletonList(charged),true);
        assertTrue(view.isActive()); assertTrue(view.bySlot());
        assertTrue(active.get().contains(25731)); assertFalse(active.get().contains(25733));
        verify(view.bankTags).openBankTag(anyString(),eq(BankTagsService.OPTION_NO_LAYOUT|BankTagsService.OPTION_HIDE_TAG_NAME));
        active.set(mock(BankTag.class)); name.set("Different tab"); view.clear();
        verify(view.bankTags,never()).closeBankTag(); verify(view.tags).unregisterTag(anyString()); assertTrue(view.groups().isEmpty());
        view.show("ToB",Collections.singletonList(charged),true); view.clear(); verify(view.bankTags).closeBankTag();
    }
    @Test public void olderCustomPresetsKeepTheirOriginalBehavior() {
        CustomPresets presets=new com.google.gson.Gson().fromJson("{\"presets\":[{\"name\":\"Food\",\"groups\":[{\"x\":0,\"y\":0,\"width\":48,\"height\":36,\"linkedItemIds\":[385]}]}]}",CustomPresets.class);
        presets.normalize(); assertEquals(1,presets.presets.size());
        assertFalse(presets.presets.get(0).bankView); assertFalse(presets.presets.get(0).groupBySlot);
    }
}
