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
