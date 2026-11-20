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
	@Test
	public void hideButtonHasItsOwnActionAndOldCallbacksStopAfterCleanup()
	{
		Widget parent = mock(Widget.class), button = mock(Widget.class);
		when(parent.getWidth()).thenReturn(478);
		when(parent.createChild(anyInt(), anyInt())).thenReturn(button);
		when(parent.getChildren()).thenReturn(new Widget[] { button });
		SetupInventoryWidgets widgets = new SetupInventoryWidgets();
		Runnable hide = mock(Runnable.class);
		widgets.addHideButton(parent, new SetupItemLayout.Heading("Low level", 51, 16, false, "224363:1"), hide);
		verify(button).setItemId(-1);
		verify(button).setAction(0, "Hide this section");
		verify(button).setClickMask(0);
		verify(button).setOriginalX(new SetupItemLayout.Heading("Low level", 51, 16, false, "224363:1").buttonX(478));
		verify(button).setOriginalY(4);
		var callback = org.mockito.ArgumentCaptor.forClass(net.runelite.api.widgets.JavaScriptCallback.class);
		verify(button).setOnOpListener(callback.capture());
		callback.getValue().run(mock(ScriptEvent.class));
		verify(hide).run();
		widgets.clear();
		callback.getValue().run(mock(ScriptEvent.class));
		verifyNoMoreInteractions(hide);
		verify(button).setHidden(true);
	}

	@Test
	public void restoreControlIsAvailableInTheViewAndCannotRunAfterCleanup()
	{
		Widget parent = mock(Widget.class), button = mock(Widget.class);
		when(parent.createChild(anyInt(), anyInt())).thenReturn(button);
		when(parent.getChildren()).thenReturn(new Widget[] { button });
		SetupInventoryWidgets widgets = new SetupInventoryWidgets();
		Runnable restore = mock(Runnable.class);
		widgets.addRestoreButton(parent, 2, restore);
		verify(button).setText("Hidden sections (2)");
		verify(button).setItemId(-1);
		verify(button).setClickMask(0);
		verify(button).setOriginalY(4);
		var callback = org.mockito.ArgumentCaptor.forClass(net.runelite.api.widgets.JavaScriptCallback.class);
		verify(button).setOnOpListener(callback.capture());
		callback.getValue().run(mock(ScriptEvent.class));
		verify(restore).run();
		widgets.clear();
		callback.getValue().run(mock(ScriptEvent.class));
		verifyNoMoreInteractions(restore);
	}

	@Test
	public void hiddenRowCleanupDoesNotUnhideReplacedOrChangedBankItems()
	{
		Widget parent = mock(Widget.class), first = mock(Widget.class), second = mock(Widget.class);
		when(first.getIndex()).thenReturn(0);
		when(second.getIndex()).thenReturn(1);
		when(first.getItemId()).thenReturn(100);
		when(second.getItemId()).thenReturn(200);
		when(parent.getChild(0)).thenReturn(first);
		when(parent.getChild(1)).thenReturn(second);
		SetupInventoryWidgets widgets = new SetupInventoryWidgets();
		widgets.hide(parent, first);
		widgets.hide(parent, second);
		when(parent.getChild(0)).thenReturn(mock(Widget.class));
		when(second.getItemId()).thenReturn(201);
		widgets.clear();
		verify(first, never()).setHidden(false);
		verify(second, never()).setHidden(false);
	}

	@Test
	public void repeatedDisplayUsesExistingActionAndRejectsChangedBankStack()
	{
		Widget parent = mock(Widget.class), source = mock(Widget.class), copy = mock(Widget.class);
		when(parent.createChild(anyInt(), anyInt())).thenReturn(copy);
		when(source.getItemId()).thenReturn(6685);
		when(source.getIndex()).thenReturn(7);
		when(source.getItemQuantity()).thenReturn(40);
		when(source.getActions()).thenReturn(new String[] { "Withdraw-1", "Withdraw-5" });
		when(parent.getChild(7)).thenReturn(source);
		Client client = mock(Client.class);
		WidgetConfigNode config = mock(WidgetConfigNode.class);
		when(client.getWidgetConfig(source)).thenReturn(config);
		when(config.getClickMask()).thenReturn(WidgetConfig.transmitAction(0) | WidgetConfig.DRAG);
		SetupInventoryWidgets widgets = new SetupInventoryWidgets();
		widgets.setClient(client);
		widgets.add(parent, source, 6685, mock(ItemManager.class));
		verify(copy).setClickMask(WidgetConfig.transmitAction(0));
		when(copy.getClickMask()).thenReturn(WidgetConfig.transmitAction(0));
		widgets.allowPresetDragging();
		verify(copy).setClickMask(WidgetConfig.transmitAction(0) | WidgetConfig.DRAG);
		verify(copy).setHasListener(true);
		MenuEntry menu = mock(MenuEntry.class);
		when(menu.getWidget()).thenReturn(copy);
		when(menu.getParam1()).thenReturn(InterfaceID.Bankmain.ITEMS);
		when(menu.getType()).thenReturn(MenuAction.CC_OP);
		widgets.menuAdded(menu);
		verify(menu, never()).setParam0(anyInt());
		assertSame(source, widgets.source(copy));
		verify(copy).setAction(0, "Withdraw-1");
		verify(copy).setItemQuantity(40);
		ItemContainer bank = mock(ItemContainer.class);
		when(client.getWidget(InterfaceID.Bankmain.ITEMS)).thenReturn(parent);
		when(client.getItemContainer(InventoryID.BANK)).thenReturn(bank);
		when(bank.getItem(7)).thenReturn(new Item(6685, 40));
		MenuOptionClicked click = new MenuOptionClicked(menu);
		widgets.clicked(click, client);
		assertFalse(click.isConsumed());
		verify(menu).setParam0(7);
		when(bank.getItem(7)).thenReturn(new Item(385, 40));
		click = new MenuOptionClicked(menu);
		widgets.clicked(click, client);
		assertTrue(click.isConsumed());
		when(bank.getItem(7)).thenReturn(null);
		widgets.setVirtualSource(widget -> widget == source);
		click = new MenuOptionClicked(menu);
		widgets.clicked(click, client);
		assertFalse(click.isConsumed());
		when(source.getItemId()).thenReturn(385);
		click = new MenuOptionClicked(menu);
		widgets.clicked(click, client);
		assertTrue(click.isConsumed());
		when(source.getItemId()).thenReturn(6685);
		widgets.clear();
		click = new MenuOptionClicked(menu);
		widgets.clicked(click, client);
		assertTrue(click.isConsumed());
	}

	@Test
	public void everyFoodCopyKeepsItsOwnMouseTargetUntilTheNativeClickIsDispatched()
	{
		Client client = mock(Client.class);
		Widget parent = mock(Widget.class), source = mock(Widget.class);
		when(source.getIndex()).thenReturn(7);
		when(source.getItemId()).thenReturn(385);
		when(source.getActions()).thenReturn(new String[] { "Withdraw-1", "Withdraw-5", "Withdraw-10", "Withdraw-X", "Withdraw-All" });
		when(client.getWidget(InterfaceID.Bankmain.ITEMS)).thenReturn(parent);
		when(parent.getChild(7)).thenReturn(source);
		ItemContainer bank = mock(ItemContainer.class);
		when(client.getItemContainer(InventoryID.BANK)).thenReturn(bank);
		when(bank.getItem(7)).thenReturn(new Item(385, 1000));
		SetupInventoryWidgets widgets = new SetupInventoryWidgets();
		widgets.setClient(client);
		for (int i = 0; i < 20; i++)
		{
			Widget copy = mock(Widget.class);
			when(parent.createChild(anyInt(), anyInt())).thenReturn(copy);
			widgets.add(parent, source, 385, mock(ItemManager.class));
			for (MenuAction action : new MenuAction[] { MenuAction.CC_OP, MenuAction.CC_OP_LOW_PRIORITY })
			{
				MenuEntry menu = mock(MenuEntry.class);
				java.util.concurrent.atomic.AtomicInteger slot = new java.util.concurrent.atomic.AtomicInteger(100 + i);
				when(menu.getParam0()).thenAnswer(call -> slot.get());
				doAnswer(call -> { slot.set(call.getArgument(0)); return menu; }).when(menu).setParam0(anyInt());
				when(menu.getWidget()).thenAnswer(call -> slot.get() == 7 ? source : copy);
				when(menu.getParam1()).thenReturn(InterfaceID.Bankmain.ITEMS);
				when(menu.getType()).thenReturn(action);
				widgets.menuAdded(menu);
				assertSame(copy, menu.getWidget());
				var click = new MenuOptionClicked(menu);
				widgets.clicked(click, client);
				assertFalse(click.isConsumed());
				assertSame(source, menu.getWidget());
				verify(menu, never()).setIdentifier(anyInt());
			}
		}
		verify(client, never()).runScript(any(Object[].class));
	}

	@Test
	public void cleanupPreservesOtherPluginWidgetsAndMissingItemsHaveNoActions()
	{
		Widget parent = mock(Widget.class), original = mock(Widget.class), copy = mock(Widget.class),
				other = mock(Widget.class);
		when(parent.createChild(anyInt(), anyInt())).thenReturn(copy);
		SetupInventoryWidgets widgets = new SetupInventoryWidgets();
		widgets.add(parent, null, 385, mock(ItemManager.class));
		assertTrue(widgets.preview(copy));
		verify(copy, never()).setAction(anyInt(), anyString());
		verify(copy).setClickMask(0);
		widgets.allowPresetDragging();
		verify(copy, never()).setClickMask(WidgetConfig.DRAG);
		AtomicReference<Widget[]> children = new AtomicReference<>(new Widget[] { original, copy, other });
		when(parent.getChildren()).thenAnswer(i -> children.get());
		doAnswer(i ->
		{
			children.set(i.getArgument(0));
			return null;
		}).when(parent).setChildren(any(Widget[].class));
		widgets.clear();
		assertArrayEquals(new Widget[] { original, null, other }, children.get());
		widgets.clear();
		assertArrayEquals(new Widget[] { original, null, other }, children.get());
		verify(original, never()).setHidden(anyBoolean());
		verify(other, never()).setHidden(anyBoolean());
	}

	@Test
	public void trailingCopiesAreRemovedAndReplacedWidgetsAreLeftAlone()
	{
		Widget parent = mock(Widget.class), original = mock(Widget.class), copy = mock(Widget.class),
				replacement = mock(Widget.class);
		when(parent.createChild(anyInt(), anyInt())).thenReturn(copy);
		SetupInventoryWidgets widgets = new SetupInventoryWidgets();
		widgets.add(parent, null, 385, mock(ItemManager.class));
		when(parent.getChildren()).thenReturn(new Widget[] { original, copy });
		widgets.clear();
		verify(parent).setChildren(new Widget[] { original });
		widgets.add(parent, null, 385, mock(ItemManager.class));
		when(parent.getChildren()).thenReturn(new Widget[] { original, replacement });
		widgets.clear();
		verify(parent).setChildren(new Widget[] { original, replacement });
		verify(replacement, never()).setHidden(anyBoolean());
	}
}
