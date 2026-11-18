package com.windowmarkers;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarClientID;
import net.runelite.api.vars.InputType;
import net.runelite.api.widgets.Widget;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.plugins.PluginManager;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class BankPotionMenuTest
{
	@Test
	public void addingAStoredPotionEnablesNormalBankAndSearchWithoutChangingTheNativeAction()
	{
		Fixture f = new Fixture();
		Consumer<MenuEntry> add = f.open();
		verify(f.added).setOption("Add potion to bank window");
		verify(f.added).setType(MenuAction.RUNELITE_LOW_PRIORITY);
		verify(f.views.configs, never()).setRSProfileConfiguration(anyString(), anyString(), any());
		add.accept(f.added);
		assertEquals(0, f.views.settings.bankTab(1000));
		assertTrue(f.views.settings.search(1000));
		verify(f.nativeEntry, never()).setType(any());
		verify(f.views.client, never()).runScript(any(Object[].class));
		when(f.views.client.getVarcIntValue(VarClientID.MESLAYERMODE)).thenReturn(InputType.SEARCH.getType());
		when(f.views.client.getVarcStrValue(VarClientID.MESLAYERINPUT)).thenReturn("prayer");
		f.views.append();
		assertEquals(2, f.base.items.children.size());
	}

	@Test
	public void readdingARemovedPotionStartsAtTheEndInsteadOfReusingAnOldSlot()
	{
		Fixture f = new Fixture();
		f.values.put("storedPotion.1000.tab", "-1");
		f.values.put("storedPotion.1000.position", "66");
		f.open().accept(f.added);
		assertEquals(-1, f.views.settings.bankPosition(1000));
		assertEquals(0, f.views.settings.bankTab(1000));
		assertTrue(f.views.settings.search(1000));
	}

	@Test
	public void addKeepsAnExistingDestinationAndRemoveClearsOnlyThatPotionsChoices()
	{
		Fixture f = new Fixture();
		f.values.put("storedPotion.1000.tab", "2");
		f.values.put("storedPotion.2000.search", "true");
		f.open().accept(f.added);
		assertEquals(2, f.views.settings.bankTab(1000));
		assertTrue(f.views.settings.search(1000));
		clearInvocations(f.added);
		Consumer<MenuEntry> remove = f.open();
		verify(f.added).setOption("Remove potion from bank window");
		remove.accept(f.added);
		assertEquals(-1, f.views.settings.bankTab(1000));
		assertFalse(f.views.settings.search(1000));
		assertTrue(f.views.settings.search(2000));
	}

	@Test
	public void staleProfileOrRebuiltStorageCannotChangeSavedChoices()
	{
		Fixture f = new Fixture();
		Consumer<MenuEntry> add = f.open();
		when(f.views.configs.getRSProfileKey()).thenReturn("different-character");
		add.accept(f.added);
		assertTrue(f.values.isEmpty());
		when(f.views.configs.getRSProfileKey()).thenReturn(null);
		f.base.store.children.set(15, mock(Widget.class));
		add.accept(f.added);
		assertTrue(f.values.isEmpty());
	}

	@Test
	public void stockChangesAndOtherProvidersKeepMenuActionsOutOfStaleContexts() throws Exception
	{
		Fixture f = new Fixture();
		Consumer<MenuEntry> add = f.open();
		f.views.stock.clear();
		add.accept(f.added);
		assertTrue(f.values.isEmpty());
		Fixture external = new Fixture();
		java.lang.reflect.Field field = BankPotionViews.class.getDeclaredField("externalBank");
		field.setAccessible(true);
		field.setBoolean(external.views, true);
		external.views.onMenuEntryAdded(new MenuEntryAdded(external.nativeEntry));
		verify(external.views.client, never()).createMenuEntry(anyInt());
	}

	@Test
	public void repeatedNativeEntriesDoNotDuplicateTheShortcut()
	{
		Fixture f = new Fixture();
		f.open();
		when(f.added.getOption()).thenReturn("Add potion to bank window");
		when(f.added.getParam0()).thenReturn(15);
		when(f.added.getParam1()).thenReturn(InterfaceID.Bankmain.POTIONSTORE_ITEMS);
		when(f.views.client.getMenuEntries()).thenReturn(new MenuEntry[] { f.added, f.nativeEntry });
		f.views.onMenuEntryAdded(new MenuEntryAdded(f.nativeEntry));
		verify(f.views.client).createMenuEntry(0);
	}

	private static final class Fixture
	{
		final BankPotionViewsTest.Fixture base = new BankPotionViewsTest.Fixture();
		final BankPotionViews views = base.views;
		final Map<String, String> values = new HashMap<>();
		final MenuEntry nativeEntry = mock(MenuEntry.class, RETURNS_SELF);
		final MenuEntry added = mock(MenuEntry.class, RETURNS_SELF);

		Fixture()
		{
			views.events = new EventBus();
			views.plugins = mock(PluginManager.class);
			when(views.plugins.getPlugins()).thenReturn(Collections.emptyList());
			when(views.configs.getRSProfileConfiguration(eq("windowmarkers"), anyString()))
					.thenAnswer(call -> values.get(call.getArgument(1)));
			doAnswer(call ->
			{
				values.put(call.getArgument(1), String.valueOf((Object) call.getArgument(2)));
				return null;
			}).when(views.configs).setRSProfileConfiguration(eq("windowmarkers"), anyString(), any());
			for (int i = 0; i < 17; i++)
				base.store.children.add(new BankControlsTest.Cell(base.store.id, base.store, 0, 0, 200, 36,
						0, 0, 0, 0).widget);
			base.store.children.get(16).setItemId(104);
			when(base.store.widget.getChild(anyInt())).thenAnswer(call ->
			{
				int index = call.getArgument(0);
				return index < 0 || index >= base.store.children.size() ? null : base.store.children.get(index);
			});
			when(nativeEntry.getParam0()).thenReturn(15);
			when(nativeEntry.getParam1()).thenReturn(InterfaceID.Bankmain.POTIONSTORE_ITEMS);
			when(nativeEntry.getIdentifier()).thenReturn(1);
			when(nativeEntry.getType()).thenReturn(MenuAction.CC_OP);
			when(nativeEntry.getTarget()).thenReturn("Prayer potion(4)");
			when(views.client.getMenuEntries()).thenReturn(new MenuEntry[] { nativeEntry });
			when(views.client.createMenuEntry(0)).thenReturn(added);
			views.start(() -> {});
		}

		Consumer<MenuEntry> open()
		{
			views.events.post(new MenuEntryAdded(nativeEntry));
			ArgumentCaptor<Consumer<MenuEntry>> action = ArgumentCaptor.forClass(Consumer.class);
			verify(added, atLeastOnce()).onClick(action.capture());
			return action.getValue();
		}
	}
}
