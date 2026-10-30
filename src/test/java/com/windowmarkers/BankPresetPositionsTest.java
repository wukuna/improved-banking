package com.windowmarkers;

import com.google.gson.Gson;
import net.runelite.client.config.ConfigManager;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class BankPresetPositionsTest
{
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
