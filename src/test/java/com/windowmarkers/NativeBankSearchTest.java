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
        NativeBankSearch search=root.createChildInjector(exported,
            binder -> binder.bind(NativeBankSearch.class)).getInstance(NativeBankSearch.class);
        assertSame(existingBankTags,search.bankTags);
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
}
