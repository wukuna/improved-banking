package com.windowmarkers;

import java.util.Arrays;
import java.util.Collections;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.banktags.BankTagsService;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class BankPotionViewsTest
{
	@Test
	public void repeatedStoragePotionRemapsAtClickTimeBeforeTheStorageWithdrawalHandler()
	{
		Fixture f = new Fixture();
		f.preset();
		f.views.append();
		Widget potion = f.items.children.get(1);
		when(potion.getIndex()).thenReturn(1);
		SetupInventoryWidgets copies = new SetupInventoryWidgets();
		copies.setClient(f.views.client);
		copies.setVirtualSource(f.views::validSource);
		f.views.setMenuSource(copies::source);
		Widget copy = copies.add(f.items.widget, potion, 104, mock(net.runelite.client.game.ItemManager.class));
		when(copy.getIndex()).thenReturn(2);
		MenuEntry menu = f.menu(copy, copy.getIndex());
		java.util.concurrent.atomic.AtomicInteger slot = new java.util.concurrent.atomic.AtomicInteger(copy.getIndex());
		when(menu.getParam0()).thenAnswer(call -> slot.get());
		doAnswer(call -> { slot.set(call.getArgument(0)); return menu; }).when(menu).setParam0(anyInt());
		copies.menuAdded(menu);
		f.views.onMenuEntryAdded(new MenuEntryAdded(menu));
		verify(menu, never()).setParam0(anyInt());
		MenuOptionClicked click = new MenuOptionClicked(menu);
		copies.clicked(click, f.views.client);
		assertFalse(click.isConsumed());
		assertEquals(potion.getIndex(), menu.getParam0());
		f.views.onMenuOptionClicked(click);
		assertFalse(click.isConsumed());
		verify(menu).setParam1(InterfaceID.Bankmain.POTIONSTORE_ITEMS);
		assertEquals(15, menu.getParam0());
		verify(menu, never()).setIdentifier(anyInt());
	}

	@Test
	public void storedBankEntriesHaveNativeDragThresholdsAndSavedPositions()
	{
		Fixture f = new Fixture();
		when(f.views.configs.getRSProfileConfiguration("windowmarkers", "storedPotion.1000.tab")).thenReturn("0");
		when(f.views.configs.getRSProfileConfiguration("windowmarkers", "storedPotion.1000.position")).thenReturn("0");
		f.views.append();
		Widget potion = f.items.children.get(1);
		assertEquals(51, potion.getOriginalX());
		assertEquals(0, potion.getOriginalY());
		assertEquals(99, f.items.children.get(0).getOriginalX());
		assertEquals(0, f.items.children.get(0).getOriginalY());
		assertTrue((potion.getClickMask() & net.runelite.api.widgets.WidgetConfig.DRAG) != 0);
		assertSame(f.items.widget, potion.getDragParent());
		assertEquals(5, potion.getDragDeadTime());
		verify(f.views.configs, never()).setRSProfileConfiguration(anyString(), anyString(), any());
	}

	@Test
	public void searchesRequirePerPotionOptInAndAMatchingName()
	{
		Fixture f = new Fixture();
		when(f.views.client.getVarcIntValue(net.runelite.api.gameval.VarClientID.MESLAYERMODE))
				.thenReturn(net.runelite.api.vars.InputType.SEARCH.getType());
		when(f.views.client.getVarcStrValue(net.runelite.api.gameval.VarClientID.MESLAYERINPUT)).thenReturn("prayer");
		f.views.append();
		assertEquals(1, f.items.children.size());
		when(f.views.configs.getRSProfileConfiguration("windowmarkers", "storedPotion.1000.search")).thenReturn("true");
		when(f.views.client.getVarcStrValue(net.runelite.api.gameval.VarClientID.MESLAYERINPUT)).thenReturn("stamina");
		f.views.append();
		assertEquals(1, f.items.children.size());
		when(f.views.client.getVarcStrValue(net.runelite.api.gameval.VarClientID.MESLAYERINPUT)).thenReturn("PRAYER");
		f.views.append();
		assertEquals(2, f.items.children.size());
	}

	@Test
	public void existingVisiblePotionIsNotDuplicatedInPresets()
	{
		Fixture f = new Fixture();
		f.preset();
		f.items.children.get(0).setItemId(104);
		f.items.children.get(0).setItemQuantity(10);
		f.views.append();
		assertEquals(1, f.items.children.size());
	}

	@Test
	public void nativeUserOperationsRetainTheirQuantityIdentifiersThroughEventBus()
	{
		Fixture f = new Fixture();
		f.preset();
		f.views.append();
		net.runelite.client.eventbus.EventBus bus = new net.runelite.client.eventbus.EventBus();
		bus.register(f.views);
		for (int operation = 1; operation <= 8; operation++)
		{
			MenuEntry menu = f.menu(f.items.children.get(1), 1);
			when(menu.getIdentifier()).thenReturn(operation);
			bus.post(new MenuEntryAdded(menu));
			MenuOptionClicked clicked = new MenuOptionClicked(menu);
			bus.post(clicked);
			assertFalse(clicked.isConsumed());
			verify(menu).setParam1(InterfaceID.Bankmain.POTIONSTORE_ITEMS);
			verify(menu).setParam0(15);
			verify(menu, never()).setIdentifier(anyInt());
		}
	}

	@Test
	public void presetIncludesStorageOnlyStockAndRedirectsOnlyTheClickedWithdrawal()
	{
		Fixture f = new Fixture();
		f.preset();
		f.views.append();
		assertEquals(2, f.items.children.size());
		Widget potion = f.items.children.get(1);
		assertEquals(104, potion.getItemId());
		assertEquals(20, potion.getItemQuantity());
		assertTrue(f.views.validSource(potion));
		MenuEntry menu = f.menu(potion, 1);
		f.views.onMenuEntryAdded(new MenuEntryAdded(menu));
		MenuOptionClicked click = new MenuOptionClicked(menu);
		f.views.onMenuOptionClicked(click);
		assertFalse(click.isConsumed());
		verify(menu).setParam1(InterfaceID.Bankmain.POTIONSTORE_ITEMS);
		verify(menu).setParam0(15);
		verify(menu, never()).setIdentifier(anyInt());
		verify(f.views.client, never()).runScript(any(Object[].class));
		f.views.stock.clear();
		click = new MenuOptionClicked(menu);
		f.views.onMenuOptionClicked(click);
		assertTrue(click.isConsumed());
	}

	@Test
	public void bankTabChoicesDoNotLeakIntoOtherTabsAndNoSettingIsWrittenDuringBuild()
	{
		Fixture f = new Fixture();
		when(f.views.configs.getRSProfileConfiguration("windowmarkers", "storedPotion.1000.tab")).thenReturn("2");
		when(f.views.client.getVarbitValue(VarbitID.BANK_CURRENTTAB)).thenReturn(1);
		f.views.append();
		assertEquals(1, f.items.children.size());
		when(f.views.client.getVarbitValue(VarbitID.BANK_CURRENTTAB)).thenReturn(2);
		when(f.views.client.getVarbitValue(VarbitID.BANK_TAB_2)).thenReturn(1);
		f.views.append();
		assertEquals(2, f.items.children.size());
		verify(f.views.configs, never()).setRSProfileConfiguration(anyString(), anyString(), any());
	}

	@Test
	public void openMenusRejectChangedProfileAndReplacementWidgets()
	{
		Fixture f = new Fixture();
		when(f.views.configs.getRSProfileConfiguration("windowmarkers", "storedPotion.1000.tab")).thenReturn("0");
		f.views.append();
		Widget potion = f.items.children.get(1);
		MenuEntry menu = f.menu(potion, 1);
		f.views.onMenuEntryAdded(new MenuEntryAdded(menu));
		when(f.views.configs.getRSProfileKey()).thenReturn("other");
		MenuOptionClicked click = new MenuOptionClicked(menu);
		f.views.onMenuOptionClicked(click);
		assertTrue(click.isConsumed());
		when(f.views.configs.getRSProfileKey()).thenReturn(null);
		f.items.children.set(1, mock(Widget.class));
		click = new MenuOptionClicked(menu);
		f.views.onMenuOptionClicked(click);
		assertTrue(click.isConsumed());
		verify(menu, never()).setParam1(anyInt());
	}

	static final class Fixture
	{
		final BankPotionViews views = new BankPotionViews();
		final BankControlsTest.Cell items = new BankControlsTest.Cell(InterfaceID.Bankmain.ITEMS, null, 0, 0, 460, 400,
				0, 0, 0, 0);
		final BankControlsTest.Cell store = new BankControlsTest.Cell(InterfaceID.Bankmain.POTIONSTORE_ITEMS, null, 0,
				0, 409, 400, 0, 0, 0, 0);

		Fixture()
		{
			views.client = mock(Client.class);
			views.configs = mock(ConfigManager.class);
			views.stock = new BankPotionStock();
			views.settings = new BankPotionSettings();
			views.settings.configs = views.configs;
			views.searchView = BankSearchViewTest.availableView();
			views.searchView.bankTags = mock(BankTagsService.class);
			stock(views.stock, Collections.singletonList(
					new BankPotionStock.Potion(1000, 3, 104, 20, false, Arrays.asList(101, 102, 103, 104))));
			when(views.client.getWidget(InterfaceID.Bankmain.ITEMS)).thenReturn(items.widget);
			when(views.client.getWidget(InterfaceID.Bankmain.POTIONSTORE_ITEMS)).thenReturn(store.widget);
			ItemContainer bank = mock(ItemContainer.class);
			when(bank.getItems()).thenReturn(new Item[] { new Item(500, 1) });
			when(bank.size()).thenReturn(1);
			when(views.client.getItemContainer(InventoryID.BANK)).thenReturn(bank);
			BankControlsTest.Cell real = new BankControlsTest.Cell(items.id, items, 51, 0, 36, 32, 0, 0, 0, 0);
			real.widget.setItemId(500);
			items.children.add(real.widget);
			when(items.widget.getChild(anyInt())).thenAnswer(call ->
			{
				int index = call.getArgument(0);
				return index < 0 || index >= items.children.size() ? null : items.children.get(index);
			});
			ItemComposition definition = mock(ItemComposition.class);
			when(definition.getName()).thenReturn("Prayer potion(4)");
			when(views.client.getItemDefinition(104)).thenReturn(definition);
			when(views.client.getIntStack()).thenReturn(new int[20]);
			when(views.client.getIntStackSize()).thenReturn(18);
		}

		MenuEntry menu(Widget potion, int index)
		{
			MenuEntry menu = mock(MenuEntry.class);
			when(menu.getWidget()).thenReturn(potion);
			when(menu.getParam0()).thenReturn(index);
			when(menu.getParam1()).thenReturn(InterfaceID.Bankmain.ITEMS);
			when(menu.getItemId()).thenReturn(104);
			when(menu.getIdentifier()).thenReturn(1);
			when(menu.getType()).thenReturn(MenuAction.CC_OP);
			return menu;
		}

		void preset()
		{
			BankSearchView search = views.searchView;
			search.tags = mock(net.runelite.client.plugins.banktags.TagManager.class);
			search.items = mock(net.runelite.client.game.ItemManager.class);
			when(search.items.canonicalize(anyInt())).thenAnswer(call -> call.getArgument(0));
			doAnswer(call ->
			{
				when(search.bankTags.getActiveTag()).thenReturn(call.getArgument(0));
				when(search.bankTags.getActiveBankTag()).thenReturn(call.getArgument(1));
				return null;
			}).when(search.tags).registerTag(anyString(), any());
			Marker group = new Marker(0, 0, 48, 36);
			group.linkedItemIds = new int[] { 101 };
			assertTrue(search.show("Potions", Collections.singletonList(group), true));
		}
	}

	static void stock(BankPotionStock stock, java.util.List<BankPotionStock.Potion> potions)
	{
		try
		{
			java.lang.reflect.Field field = BankPotionStock.class.getDeclaredField("potions");
			field.setAccessible(true);
			field.set(stock, potions);
		}
		catch (ReflectiveOperationException e)
		{
			throw new AssertionError(e);
		}
	}
}
