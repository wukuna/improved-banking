package com.windowmarkers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.ScriptID;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetType;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.EventBus;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class BankPotionInsertionTest
{
	private BankPotionViewsTest.Fixture bank(int count)
	{
		BankPotionViewsTest.Fixture f = new BankPotionViewsTest.Fixture();
		f.items.children.clear();
		Item[] nativeItems = new Item[count];
		for (int i = 0; i < count; i++)
		{
			Widget cell = new BankControlsTest.Cell(f.items.id, f.items, 51 + i % 8 * 48, i / 8 * 36,
					36, 32, 0, 0, 0, 0).widget;
			cell.setItemId(500 + i);
			when(cell.getType()).thenReturn(WidgetType.GRAPHIC);
			when(cell.getIndex()).thenReturn(i);
			f.items.children.add(cell);
			nativeItems[i] = new Item(500 + i, 1);
		}
		ItemContainer bank = f.views.client.getItemContainer(InventoryID.BANK);
		when(bank.size()).thenReturn(count);
		when(bank.getItems()).thenReturn(nativeItems);
		when(f.views.configs.getRSProfileConfiguration("windowmarkers", "storedPotion.1000.tab")).thenReturn("0");
		return f;
	}

	@Test
	public void insertedPotionShiftsItemsOneCellAndWrapsOnlyAtRowEnd()
	{
		for (int count : new int[] { 0, 1, 7, 8, 9, 15, 16 })
			for (int position : new int[] { -1, 0, 3, 8 })
			{
				BankPotionViewsTest.Fixture f = bank(count);
				when(f.views.configs.getRSProfileConfiguration("windowmarkers", "storedPotion.1000.position"))
						.thenReturn(Integer.toString(position));
				f.views.append();
				int inserted = position < 0 ? count : Math.min(count, position);
				assertCell(f.items.children.get(count), inserted, 8, 48, 0);
				for (int i = 0; i < count; i++)
				{
					Widget original = f.items.children.get(i);
					assertCell(original, i < inserted ? i : i + 1, 8, 48, 0);
					assertEquals(i, original.getIndex());
					assertEquals(500 + i, original.getItemId());
				}
				verify(f.views.configs, never()).setRSProfileConfiguration(anyString(), anyString(), any());
			}
	}

	@Test
	public void multipleStoredPotionsFillRemainingSlotsBeforeAddingRows()
	{
		for (int count : new int[] { 6, 7, 8 })
			for (int stored : new int[] { 2, 9 })
			{
				BankPotionViewsTest.Fixture f = bank(count);
				List<BankPotionStock.Potion> stock = new ArrayList<>();
				net.runelite.api.ItemComposition definition = f.views.client.getItemDefinition(104);
				for (int i = 0; i < stored; i++)
				{
					int id = 104 + i, enumId = 1000 + i;
					stock.add(new BankPotionStock.Potion(enumId, i, id, 20, false,
							java.util.Collections.singletonList(id)));
					when(f.views.client.getItemDefinition(id)).thenReturn(definition);
					when(f.views.configs.getRSProfileConfiguration("windowmarkers", "storedPotion." + enumId + ".tab"))
							.thenReturn("0");
					when(f.views.configs.getRSProfileConfiguration("windowmarkers", "storedPotion." + enumId + ".position"))
							.thenReturn(Integer.toString(i));
				}
				BankPotionViewsTest.stock(f.views.stock, stock);
				f.views.append();
				for (int i = 0; i < stored; i++)
					assertCell(f.items.children.get(count + i), i, 8, 48, 0);
				for (int i = 0; i < count; i++)
					assertCell(f.items.children.get(i), stored + i, 8, 48, 0);
			}
	}

	@Test
	public void fillingAPartialTabDoesNotPushTheNextTabDownButOverflowDoes()
	{
		for (int firstTab : new int[] { 7, 8 })
		{
			BankPotionViewsTest.Fixture f = bank(firstTab + 1);
			Widget nextTab = f.items.children.get(firstTab);
			nextTab.setOriginalX(51);
			nextTab.setOriginalY(60);
			when(f.views.client.getVarbitValue(VarbitID.BANK_TAB_1)).thenReturn(firstTab);
			when(f.views.client.getVarbitValue(VarbitID.BANK_TAB_2)).thenReturn(1);
			when(f.views.configs.getRSProfileConfiguration("windowmarkers", "storedPotion.1000.tab")).thenReturn("1");
			Widget separator = new BankControlsTest.Cell(f.items.id, f.items, 51, 48, 360, 1, 0, 0, 0, 0).widget;
			separator.setItemId(-1);
			f.items.children.add(separator);
			f.views.append();
			int addedHeight = firstTab == 8 ? 36 : 0;
			assertEquals(60 + addedHeight, nextTab.getOriginalY());
			assertEquals(48 + addedHeight, separator.getOriginalY());
			assertCell(f.items.children.get(firstTab + 2), firstTab, 8, 48, 0);
		}
	}

	@Test
	public void mainSectionIsDrawnAboveNumberedTabsDespiteHavingLaterNativeIndices()
	{
		for (int mainCount : new int[] { 0, 2, 7, 8 })
			for (int position : new int[] { -1, 0 })
			{
				BankPotionViewsTest.Fixture f = bank(6 + mainCount);
				when(f.views.client.getVarbitValue(VarbitID.BANK_TAB_1)).thenReturn(6);
				when(f.views.configs.getRSProfileConfiguration("windowmarkers", "storedPotion.1000.position"))
						.thenReturn(Integer.toString(position));
				int tabTop = mainCount == 0 ? 0 : 48;
				for (int i = 0; i < 6; i++)
				{
					f.items.children.get(i).setOriginalX(51 + i * 48);
					f.items.children.get(i).setOriginalY(tabTop);
				}
				for (int i = 0; i < mainCount; i++)
				{
					f.items.children.get(6 + i).setOriginalX(51 + i * 48);
					f.items.children.get(6 + i).setOriginalY(0);
				}
				f.views.append();
				int inserted = position < 0 ? mainCount : position;
				assertCell(f.items.children.get(6 + mainCount), inserted, 8, 48, 0);
				for (int i = 0; i < mainCount; i++)
					assertCell(f.items.children.get(6 + i), i < inserted ? i : i + 1, 8, 48, 0);
				for (int i = 0; i < 6; i++)
					assertCell(f.items.children.get(i), i, 8, 48, tabTop + (mainCount % 8 == 0 ? 36 : 0));
			}
	}

	@Test
	public void customColumnCountsKeepTheInsertedPotionAndFollowingItemsInOneGrid()
	{
		for (int columns : new int[] { 4, 8, 12 })
		{
			BankPotionViewsTest.Fixture f = bank(9);
			f.items.widget.setOriginalWidth(51 + 35 + columns * 36 + (columns - 1) * 12);
			when(f.views.configs.getRSProfileConfiguration("windowmarkers", "storedPotion.1000.position")).thenReturn("0");
			f.views.append();
			Widget potion = f.items.children.get(9);
			when(potion.getType()).thenReturn(WidgetType.GRAPHIC);
			BankLayoutController controller = new BankLayoutController();
			controller.client = f.views.client;
			controller.config = mock(WindowMarkersConfig.class);
			controller.clientThread = mock(ClientThread.class);
			controller.eventBus = mock(EventBus.class);
			when(controller.config.bankLayoutEnabled()).thenReturn(true);
			when(controller.config.columns()).thenReturn(columns);
			controller.startUp();
			controller.onScriptPreFired(new ScriptPreFired(ScriptID.BANKMAIN_FINISHBUILDING));
			List<Widget> ordered = new ArrayList<>(f.items.children);
			ordered.sort(Comparator.comparingInt(Widget::getOriginalY).thenComparingInt(Widget::getOriginalX));
			assertSame(potion, ordered.get(0));
			for (int i = 0; i < ordered.size(); i++)
				assertCell(ordered.get(i), i, columns, 48, 4);
		}
	}

	private void assertCell(Widget widget, int slot, int columns, int pitch, int top)
	{
		assertEquals(51 + slot % columns * pitch, widget.getOriginalX());
		assertEquals(top + slot / columns * 36, widget.getOriginalY());
	}
}
