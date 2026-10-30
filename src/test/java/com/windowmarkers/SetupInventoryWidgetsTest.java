package com.windowmarkers;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import net.runelite.api.*;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetConfigNode;
import net.runelite.api.widgets.WidgetConfig;
import net.runelite.client.game.ItemManager;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class SetupInventoryWidgetsTest
{
    @Test public void repeatedDisplayUsesExistingActionAndRejectsChangedBankStack()
    {
        Widget parent=mock(Widget.class),source=mock(Widget.class),copy=mock(Widget.class);
        when(parent.createChild(anyInt(),anyInt())).thenReturn(copy);
        when(source.getItemId()).thenReturn(6685);when(source.getIndex()).thenReturn(7);
        when(source.getItemQuantity()).thenReturn(40);when(source.getActions()).thenReturn(new String[]{"Withdraw-1","Withdraw-5"});
        when(parent.getChild(7)).thenReturn(source);
        Client client=mock(Client.class);WidgetConfigNode config=mock(WidgetConfigNode.class);
        when(client.getWidgetConfig(source)).thenReturn(config);
        when(config.getClickMask()).thenReturn(WidgetConfig.transmitAction(0)|WidgetConfig.DRAG);
        SetupInventoryWidgets widgets=new SetupInventoryWidgets();widgets.setClient(client);widgets.add(parent,source,6685,mock(ItemManager.class));
        verify(copy).setClickMask(WidgetConfig.transmitAction(0));
        when(copy.getClickMask()).thenReturn(WidgetConfig.transmitAction(0));
        widgets.allowPresetDragging();
        verify(copy).setClickMask(WidgetConfig.transmitAction(0)|WidgetConfig.DRAG);
        verify(copy).setHasListener(true);
        MenuEntry menu=mock(MenuEntry.class);when(menu.getWidget()).thenReturn(copy);
        when(menu.getParam1()).thenReturn(InterfaceID.Bankmain.ITEMS);when(menu.getType()).thenReturn(MenuAction.CC_OP);
        widgets.menuAdded(menu);verify(menu).setParam0(7);verify(copy).setAction(0,"Withdraw-1");verify(copy).setItemQuantity(40);
        ItemContainer bank=mock(ItemContainer.class);
        when(client.getWidget(InterfaceID.Bankmain.ITEMS)).thenReturn(parent);when(client.getItemContainer(InventoryID.BANK)).thenReturn(bank);
        when(bank.getItem(7)).thenReturn(new Item(6685,40));
        MenuOptionClicked click=new MenuOptionClicked(menu);widgets.clicked(click,client);assertFalse(click.isConsumed());
        when(bank.getItem(7)).thenReturn(new Item(385,40));
        click=new MenuOptionClicked(menu);widgets.clicked(click,client);assertTrue(click.isConsumed());
    }

    @Test public void cleanupPreservesOtherPluginWidgetsAndMissingItemsHaveNoActions()
    {
        Widget parent=mock(Widget.class),original=mock(Widget.class),copy=mock(Widget.class),other=mock(Widget.class);
        when(parent.createChild(anyInt(),anyInt())).thenReturn(copy);
        SetupInventoryWidgets widgets=new SetupInventoryWidgets();widgets.add(parent,null,385,mock(ItemManager.class));
        assertTrue(widgets.preview(copy));verify(copy,never()).setAction(anyInt(),anyString());verify(copy).setClickMask(0);
        widgets.allowPresetDragging();verify(copy,never()).setClickMask(WidgetConfig.DRAG);
        AtomicReference<Widget[]> children=new AtomicReference<>(new Widget[]{original,copy,other});
        when(parent.getChildren()).thenAnswer(i->children.get());
        doAnswer(i->{children.set(i.getArgument(0));return null;}).when(parent).setChildren(any(Widget[].class));
        widgets.clear();assertArrayEquals(new Widget[]{original,null,other},children.get());
        widgets.clear();assertArrayEquals(new Widget[]{original,null,other},children.get());
        verify(original,never()).setHidden(anyBoolean());verify(other,never()).setHidden(anyBoolean());
    }

    @Test public void trailingCopiesAreRemovedAndReplacedWidgetsAreLeftAlone()
    {
        Widget parent=mock(Widget.class),original=mock(Widget.class),copy=mock(Widget.class),replacement=mock(Widget.class);
        when(parent.createChild(anyInt(),anyInt())).thenReturn(copy);
        SetupInventoryWidgets widgets=new SetupInventoryWidgets();widgets.add(parent,null,385,mock(ItemManager.class));
        when(parent.getChildren()).thenReturn(new Widget[]{original,copy});widgets.clear();
        verify(parent).setChildren(new Widget[]{original});
        widgets.add(parent,null,385,mock(ItemManager.class));when(parent.getChildren()).thenReturn(new Widget[]{original,replacement});widgets.clear();
        verify(parent).setChildren(new Widget[]{original,replacement});verify(replacement,never()).setHidden(anyBoolean());
    }
}
