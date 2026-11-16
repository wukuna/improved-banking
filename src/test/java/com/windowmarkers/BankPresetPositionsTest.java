package com.windowmarkers;

import com.google.gson.Gson;
import net.runelite.client.config.ConfigManager;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class BankPresetPositionsTest
{
    @Test public void resetClearsTheSharedLayoutInsteadOfOnlyOneWidth() {
        BankLayoutController c=new BankControlLayoutTest.Fixture().controller;
        c.searchView=BankSearchViewTest.availableView();
        c.searchView.bankTags=mock(net.runelite.client.plugins.banktags.BankTagsService.class);
        c.searchView.tags=mock(net.runelite.client.plugins.banktags.TagManager.class);
        c.searchView.items=mock(net.runelite.client.game.ItemManager.class);
        doAnswer(call->{when(c.searchView.bankTags.getActiveTag()).thenReturn(call.getArgument(0));return null;})
            .when(c.searchView.bankTags).openBankTag(anyString(),anyInt());
        Marker item=new Marker(0,0,48,36);item.linkedItemIds=new int[]{385};
        assertTrue(c.searchView.show("Meals",java.util.Collections.singletonList(item),false,null,java.util.Collections.emptyMap(),"saved:Meals"));
        c.configs=mock(ConfigManager.class,RETURNS_DEEP_STUBS);when(c.configs.getRSProfileKey()).thenReturn("account-a");
        when(c.configs.getProfile().getId()).thenReturn(10L);
        c.presetPositions=new BankPresetPositions();c.presetPositions.configs=c.configs;c.presetPositions.gson=new Gson();
        String shared=c.presetPositions.key("saved:Meals:false");
        c.resetPresetPositions();
        verify(c.configs).setConfiguration(eq("windowmarkers"),eq("account-a"),eq(shared),eq("{\"cells\":{}}"));
    }

    @Test public void sharedLayoutTakesPrecedenceAcrossWidthsWithoutDeletingLegacyLayouts() {
        BankPresetPositions store=new BankPresetPositions();store.gson=new Gson();
        store.configs=mock(ConfigManager.class,RETURNS_DEEP_STUBS);
        when(store.configs.getProfile().getId()).thenReturn(10L);
        when(store.configs.getRSProfileKey()).thenReturn("account-a");
        String legacy=store.key("saved:Meals",478),shared=store.key("saved:Meals");
        String data="{\"cells\":{\"385:1\":[387,40]}}";
        when(store.configs.getConfiguration("windowmarkers","account-a",legacy)).thenReturn(data);
        assertArrayEquals(new int[]{387,40},store.read("account-a","saved:Meals",478).cells.get("385:1"));
        verify(store.configs,never()).setConfiguration(anyString(),anyString(),anyString(),any());
        when(store.configs.getConfiguration("windowmarkers","account-a",shared)).thenReturn(data);
        assertArrayEquals(new int[]{387,40},store.read("account-a","saved:Meals",180).cells.get("385:1"));
        assertNotEquals(shared,store.key("saved:Weapons"));
        when(store.configs.getProfile().getId()).thenReturn(20L);
        assertNotEquals(shared,store.key("saved:Meals"));
    }

    @Test public void storageSeparatesAccountsProfilesViewsAndWidthsAndRejectsBadCoordinates() {
        BankPresetPositions store=new BankPresetPositions();store.gson=new Gson();
        store.configs=mock(ConfigManager.class,RETURNS_DEEP_STUBS);
        when(store.configs.getProfile().getId()).thenReturn(10L);
        when(store.configs.getRSProfileKey()).thenReturn("account-a");
        String key=store.key("saved:Meals",478);
        assertNotEquals(key,store.key("saved:Weapons",478));assertNotEquals(key,store.key("saved:Meals",600));
        when(store.configs.getProfile().getId()).thenReturn(20L);assertNotEquals(key,store.key("saved:Meals",478));
        when(store.configs.getConfiguration("windowmarkers","account-a",key))
            .thenReturn("{\"cells\":{\"385:1\":[51,40],\"bad\":[-3,10],\"null\":null,\"long\":[1,2,3]}}");
        BankPresetPositions.Layout layout=store.read("account-a",key);assertEquals(1,layout.cells.size());
        assertArrayEquals(new int[]{51,40},layout.cells.get("385:1"));assertTrue(store.read("account-b",key).cells.isEmpty());
        store.save("account-b",key,layout);verify(store.configs,never()).setConfiguration(anyString(),anyString(),anyString(),any());
        store.save("account-a",key,layout);verify(store.configs,never()).setConfiguration(anyString(),anyString(),anyString(),any());
        when(store.configs.getProfile().getId()).thenReturn(10L);
        store.save("account-a",key,layout);verify(store.configs).setConfiguration(eq("windowmarkers"),eq("account-a"),eq(key),any());
        when(store.configs.getConfiguration("windowmarkers","account-a",key)).thenReturn("broken json");
        assertTrue(store.read("account-a",key).cells.isEmpty());
    }
}
