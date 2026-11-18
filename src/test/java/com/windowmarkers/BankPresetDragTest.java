package com.windowmarkers;

import com.google.gson.Gson;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.Point;
import net.runelite.api.ScriptEvent;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.JavaScriptCallback;
import net.runelite.api.widgets.Widget;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class BankPresetDragTest
{
	@Test
	public void returnRemembersTheDisplayedCopyAfterItsWithdrawalEntryIsRemapped()
	{
		Fixture f = new Fixture();
		f.geometry.drag(f.client, f.parent, () -> true);
		f.drop(405, 56);
		MenuFixture menu = new MenuFixture(f, f.a);
		AtomicInteger nativeSlot = new AtomicInteger();
		when(menu.nativeEntry.getWidget()).thenAnswer(call -> nativeSlot.get() == 0 ? f.a : f.b);
		doAnswer(call ->
		{
			nativeSlot.set(call.getArgument(0));
			return menu.nativeEntry;
		}).when(menu.nativeEntry).setParam0(anyInt());
		f.geometry.menuAdded(f.client, menu.nativeEntry, () -> true);
		menu.nativeEntry.setParam0(7);
		menu.click();
		assertEquals(51, f.a.getOriginalX());
		assertEquals(99, f.b.getOriginalX());
		assertTrue(f.saved.cells.isEmpty());
		verify(menu.added).setParam0(0);
	}

	@Test
	public void returnMenuRestoresExactGeneratedSlotAndRemovesOnlySelectedOverride()
	{
		Fixture f = new Fixture();
		f.geometry.drag(f.client, f.parent, () -> true);
		f.drop(405, 92);
		f.saved.cells.put("385:2", new int[] { 435, 40 });
		MenuFixture menu = new MenuFixture(f, f.a);
		f.geometry.menuAdded(f.client, menu.nativeEntry, () -> true);
		verify(menu.added).setOption("Return to preset position");
		verify(menu.added).setType(MenuAction.RUNELITE_LOW_PRIORITY);
		menu.click();
		assertEquals(51, f.a.getOriginalX());
		assertEquals(40, f.a.getOriginalY());
		assertEquals(51, f.notes.get(0).x);
		assertEquals(40, f.notes.get(0).y);
		assertFalse(f.saved.cells.containsKey("385:1"));
		assertArrayEquals(new int[] { 435, 40 }, f.saved.cells.get("385:2"));
		assertEquals(2, f.writes.get());
		verify(menu.nativeEntry, never()).setType(any());
		verify(menu.nativeEntry, never()).setParam0(anyInt());
		verify(f.client, never()).runScript(any(Object[].class));
		Widget first = cell(0, 385, 51, 40), second = cell(1, 385, 99, 40);
		when(f.parent.getDynamicChildren()).thenReturn(new Widget[] { first, second });
		f.geometry.capture(f.parent, f.saved, () ->
		{
		}, new ArrayList<>(), 80);
		assertEquals(51, first.getOriginalX());
		assertEquals(40, first.getOriginalY());
		assertEquals(435, second.getOriginalX());
	}

	@Test
	public void returnToOccupiedPresetSlotSwapsAndClearsRedundantOverrides()
	{
		Fixture f = new Fixture();
		f.geometry.drag(f.client, f.parent, () -> true);
		f.drop(110, 50);
		MenuFixture menu = new MenuFixture(f, f.a);
		f.geometry.menuAdded(f.client, menu.nativeEntry, () -> true);
		menu.click();
		assertEquals(51, f.a.getOriginalX());
		assertEquals(99, f.b.getOriginalX());
		assertEquals(147, f.c.getOriginalX());
		assertTrue(f.saved.cells.isEmpty());
		assertEquals(2, f.writes.get());
	}

	@Test
	public void unchangedItemsAndRepeatedNativeEntriesDoNotAddExtraReturnOptions()
	{
		Fixture f = new Fixture();
		MenuFixture menu = new MenuFixture(f, f.a);
		f.geometry.menuAdded(f.client, menu.nativeEntry, () -> true);
		verify(f.client, never()).createMenuEntry(anyInt());
		f.geometry.drag(f.client, f.parent, () -> true);
		f.drop(405, 56);
		f.geometry.menuAdded(f.client, menu.nativeEntry, () -> true);
		when(menu.added.getOption()).thenReturn("Return to preset position");
		when(menu.added.getParam1()).thenReturn(InterfaceID.Bankmain.ITEMS);
		when(menu.added.getParam0()).thenReturn(0);
		when(f.client.getMenuEntries()).thenReturn(new MenuEntry[] { menu.added, menu.nativeEntry });
		f.geometry.menuAdded(f.client, menu.nativeEntry, () -> true);
		verify(f.client, times(1)).createMenuEntry(0);
		when(menu.nativeEntry.getType()).thenReturn(MenuAction.ITEM_FIFTH_OPTION);
		f.geometry.menuAdded(f.client, menu.nativeEntry, () -> true);
		verify(f.client, times(1)).createMenuEntry(0);
	}

	@Test
	public void staleReturnMenusCannotChangeAnotherViewOrReplacementItem()
	{
		for (int mode = 0; mode < 5; mode++)
		{
			Fixture f = new Fixture();
			f.geometry.drag(f.client, f.parent, () -> true);
			f.drop(405, 56);
			MenuFixture menu = new MenuFixture(f, f.a);
			boolean current = mode != 0;
			f.geometry.menuAdded(f.client, menu.nativeEntry, () -> current);
			if (mode == 1)
				f.geometry.clear();
			if (mode == 2)
				when(f.a.getItemId()).thenReturn(999);
			if (mode == 3)
				when(f.parent.isHidden()).thenReturn(true);
			if (mode == 4)
				f.geometry.cancelDrag();
			menu.click();
			assertEquals(387, f.a.getOriginalX());
			assertEquals(1, f.writes.get());
		}
	}

	@Test
	public void returnFromTemporaryOverflowCancelsTheWidePositionPermanently()
	{
		Fixture f = new Fixture();
		f.saved.cells.put("385:1", new int[] { 387, 40 });
		when(f.parent.getWidth()).thenReturn(180);
		f.geometry.capture(f.parent, f.saved, f.writes::incrementAndGet, f.notes, 80);
		assertTrue(f.a.getOriginalY() >= 80);
		MenuFixture menu = new MenuFixture(f, f.a);
		f.geometry.menuAdded(f.client, menu.nativeEntry, () -> true);
		menu.click();
		assertEquals(51, f.a.getOriginalX());
		assertEquals(40, f.a.getOriginalY());
		assertTrue(f.saved.cells.isEmpty());
		when(f.parent.getWidth()).thenReturn(478);
		f.geometry.capture(f.parent, f.saved, f.writes::incrementAndGet, f.notes, 80);
		assertEquals(51, f.a.getOriginalX());
		assertEquals(40, f.a.getOriginalY());
	}

	private static final class MenuFixture
	{
		final MenuEntry nativeEntry = mock(MenuEntry.class), added = mock(MenuEntry.class, RETURNS_SELF);
		final AtomicReference<java.util.function.Consumer<MenuEntry>> callback = new AtomicReference<>();

		MenuFixture(Fixture f, Widget item)
		{
			when(nativeEntry.getWidget()).thenReturn(item);
			when(nativeEntry.getParam1()).thenReturn(InterfaceID.Bankmain.ITEMS);
			when(nativeEntry.getIdentifier()).thenReturn(1);
			when(nativeEntry.getType()).thenReturn(MenuAction.CC_OP);
			when(nativeEntry.getTarget()).thenReturn("<col=ff9040>Ammunition</col>");
			when(f.client.createMenuEntry(0)).thenReturn(added);
			doAnswer(call ->
			{
				callback.set(call.getArgument(0));
				return added;
			}).when(added).onClick(any(java.util.function.Consumer.class));
		}

		void click()
		{
			assertNotNull(callback.get());
			callback.get().accept(added);
		}
	}

	@Test
	public void rightSidePlacementOverflowsBelowOnShrinkAndReturnsWhenExpandedWithoutSaving()
	{
		Fixture f = new Fixture();
		f.geometry.drag(f.client, f.parent, () -> true);
		f.drop(405, 56);
		assertEquals(387, f.a.getOriginalX());
		assertEquals(40, f.a.getOriginalY());
		assertEquals(1, f.writes.get());
		f.saved.cells.put("385:2", new int[] { 435, 40 });
		f.saved.cells.put("3144:1", new int[] { 387, 76 });
		String preferred = new Gson().toJson(f.saved);
		for (int width : new int[] { 180, 120, 478, 180, 478 })
		{
			when(f.parent.getWidth()).thenReturn(width);
			f.a.setOriginalX(51);
			f.a.setOriginalY(40);
			f.b.setOriginalX(99);
			f.b.setOriginalY(40);
			f.c.setOriginalX(147);
			f.c.setOriginalY(40);
			int height = f.geometry.capture(f.parent, f.saved, f.writes::incrementAndGet, new ArrayList<>(), 80);
			if (width < 478)
			{
				assertEquals(51, f.a.getOriginalX());
				assertEquals(84, f.a.getOriginalY());
				assertTrue(f.b.getOriginalY() >= 84);
				assertTrue(f.c.getOriginalY() >= 84);
				assertTrue(height >= f.c.getOriginalY() + 36);
			}
			else
			{
				assertEquals(387, f.a.getOriginalX());
				assertEquals(435, f.b.getOriginalX());
				assertEquals(387, f.c.getOriginalX());
				assertEquals(76, f.c.getOriginalY());
			}
			Widget[] cells = { f.a, f.b, f.c };
			for (int i = 0; i < cells.length; i++)
			{
				Widget a = cells[i];
				assertTrue(a.getOriginalX() + 36 <= width);
				java.awt.Rectangle bounds = new java.awt.Rectangle(a.getOriginalX(), a.getOriginalY(), 36, 32);
				for (int j = i + 1; j < cells.length; j++)
				{
					Widget b = cells[j];
					assertFalse(bounds.intersects(new java.awt.Rectangle(b.getOriginalX(), b.getOriginalY(), 36, 32)));
				}
			}
			assertEquals(preferred, new Gson().toJson(f.saved));
			assertEquals(1, f.writes.get());
		}
	}

	private static Widget cell(int index, int id, int x, int y)
	{
		Widget cell = mock(Widget.class);
		AtomicInteger px = new AtomicInteger(x), py = new AtomicInteger(y);
		when(cell.getIndex()).thenReturn(index);
		when(cell.getItemId()).thenReturn(id);
		when(cell.getOriginalX()).thenAnswer(i -> px.get());
		when(cell.getOriginalY()).thenAnswer(i -> py.get());
		doAnswer(i ->
		{
			px.set(i.getArgument(0));
			return cell;
		}).when(cell).setOriginalX(anyInt());
		doAnswer(i ->
		{
			py.set(i.getArgument(0));
			return cell;
		}).when(cell).setOriginalY(anyInt());
		return cell;
	}

	private static final class Fixture
	{
		final Widget parent = mock(Widget.class), a = cell(0, 385, 51, 40), b = cell(1, 385, 99, 40),
				c = cell(2, 3144, 147, 40);
		final Client client = mock(Client.class);
		final BankViewGeometry geometry = new BankViewGeometry();
		final BankPresetPositions.Layout saved = new BankPresetPositions.Layout();
		final List<SetupItemLayout.InventoryNote> notes = new ArrayList<>(
				Arrays.asList(new SetupItemLayout.InventoryNote("First slot", 51, 40),
						new SetupItemLayout.InventoryNote("Second slot", 99, 40)));
		final AtomicInteger writes = new AtomicInteger();
		final AtomicReference<JavaScriptCallback> callback = new AtomicReference<>();

		Fixture()
		{
			when(parent.getDynamicChildren()).thenReturn(new Widget[] { a, b, c });
			when(parent.getWidth()).thenReturn(478);
			when(parent.getHeight()).thenReturn(300);
			when(parent.getCanvasLocation()).thenReturn(new Point(100, 200));
			when(client.getDraggedWidget()).thenReturn(a);
			doAnswer(i ->
			{
				callback.set(i.getArgument(0));
				return null;
			}).when(a).setOnDragCompleteListener(any(JavaScriptCallback.class));
			geometry.capture(parent, saved, writes::incrementAndGet, notes, 80);
		}

		void drop(int x, int y)
		{
			when(client.getMouseCanvasPosition()).thenReturn(new Point(100 + x, 200 + y));
			callback.get().run(mock(ScriptEvent.class));
		}
	}

	@Test
	public void dropOverridesTagCallbackSwapsOnlyChosenOccurrenceAndSurvivesRebuild()
	{
		Fixture f = new Fixture();
		AtomicInteger foreign = new AtomicInteger();
		f.a.setOnDragCompleteListener((JavaScriptCallback) e -> foreign.incrementAndGet());
		assertTrue(f.geometry.drag(f.client, f.parent, () -> true));
		verify(f.client).setDraggedOnWidget(null);
		f.drop(110, 50);
		assertEquals(0, foreign.get());
		assertEquals(1, f.writes.get());
		assertEquals(99, f.a.getOriginalX());
		assertEquals(51, f.b.getOriginalX());
		assertEquals(147, f.c.getOriginalX());
		assertEquals(99, f.notes.get(0).x);
		assertEquals(51, f.notes.get(1).x);
		assertArrayEquals(new int[] { 99, 40 }, f.saved.cells.get("385:1"));
		BankPresetPositions.Layout reloaded = new Gson().fromJson(new Gson().toJson(f.saved),
				BankPresetPositions.Layout.class);

		Widget first = cell(1, 385, 51, 40), second = cell(0, 385, 99, 40);
		when(f.parent.getDynamicChildren()).thenReturn(new Widget[] { second, first });
		f.geometry.capture(f.parent, reloaded, () ->
		{
		}, new ArrayList<>(), 80);
		assertEquals(99, first.getOriginalX());
		assertEquals(51, second.getOriginalX());
		verify(f.client, never()).runScript(any(Object[].class));
	}

	@Test
	public void emptyCellDropUsesScrolledCoordinatesAndDoesNotCollapseOtherItems()
	{
		Fixture f = new Fixture();
		when(f.parent.getScrollY()).thenReturn(36);
		f.geometry.drag(f.client, f.parent, () -> true);
		f.drop(69, 100);
		assertEquals(51, f.a.getOriginalX());
		assertEquals(112, f.a.getOriginalY());
		assertEquals(99, f.b.getOriginalX());
		assertEquals(40, f.b.getOriginalY());
		f.b.setOriginalY(800);
		f.geometry.restore(f.parent);
		assertEquals(40, f.b.getOriginalY());
	}

	@Test
	public void cancelOutsideBankAndStaleCallbacksNeverSaveOrMove()
	{
		Fixture f = new Fixture();
		f.geometry.drag(f.client, f.parent, () -> true);
		f.drop(800, 50);
		assertEquals(0, f.writes.get());
		assertEquals(51, f.a.getOriginalX());
		f.geometry.drag(f.client, f.parent, () -> false);
		f.drop(110, 50);
		assertEquals(0, f.writes.get());
		f.geometry.drag(f.client, f.parent, () -> true);
		f.geometry.cancelDrag();
		f.drop(110, 50);
		assertEquals(0, f.writes.get());
		f.geometry.drag(f.client, f.parent, () -> true);
		f.geometry.clear();
		f.drop(110, 50);
		assertEquals(0, f.writes.get());
		assertFalse(f.geometry.drag(f.client, f.parent, () -> true));
	}

	@Test
	public void addingItemCannotHideItUnderAnExistingSavedPosition()
	{
		Fixture f = new Fixture();
		f.saved.cells.put("385:1", new int[] { 147, 40 });
		f.geometry.capture(f.parent, f.saved, () ->
		{
		}, f.notes, 80);
		assertEquals(147, f.a.getOriginalX());
		assertTrue(f.c.getOriginalY() >= 80);
		assertEquals(51, f.c.getOriginalX());
		assertEquals(147, f.notes.get(0).x);
		assertEquals(99, f.notes.get(1).x);
	}
}
