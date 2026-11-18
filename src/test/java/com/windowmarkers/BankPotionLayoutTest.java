package com.windowmarkers;

import java.util.ArrayList;
import java.util.List;
import net.runelite.api.ScriptID;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetType;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class BankPotionLayoutTest
{
	@Test
	public void emptyRightCellsResizeWithRowsWithoutWaitingForARebuild()
	{
		Fixture f = new Fixture();
		BankControlsTest.Cell filler = f.cell(205, 72, 204, 36, WidgetType.GRAPHIC);
		when(filler.widget.getSpriteId()).thenReturn(net.runelite.api.SpriteID.RESIZEABLE_MODE_SIDE_PANEL_BACKGROUND);
		f.items.children.add(filler.widget);
		f.layout.capture(f.items.widget);
		for (int width : new int[] { 601, 702, 409, 473 })
		{
			f.items.widget.setOriginalWidth(width);
			f.layout.apply(f.items.widget);
			assertEquals((width + 1) / 2, filler.widget.getOriginalX());
			assertEquals(width / 2, filler.widget.getOriginalWidth());
			assertEquals(width, filler.widget.getOriginalX() + filler.widget.getOriginalWidth());
			assertEquals(72, filler.widget.getOriginalY());
			assertEquals(36, filler.widget.getOriginalHeight());
			f.assertRow(1, (width + 1) / 2, 36, width / 2);
		}
		clearInvocations(filler.widget);
		f.layout.apply(f.items.widget);
		verify(filler.widget, never()).revalidate();
		f.items.children.set(f.items.children.size() - 1, mock(Widget.class));
		f.items.widget.setOriginalWidth(800);
		f.layout.apply(f.items.widget);
		assertEquals(236, filler.widget.getOriginalWidth());
	}

	@Test
	public void dragReordersWholeRowsAndAnOldCallbackCannotMoveRebuiltRows()
	{
		Fixture f = new Fixture();
		f.layout.capture(f.items.widget);
		BankPotionViews views = new BankPotionViews();
		views.config = mock(WindowMarkersConfig.class);
		when(views.config.moveStoredPotions()).thenReturn(true);
		views.settings = new BankPotionSettings();
		views.stock = new BankPotionStock();
		views.settings.configs = mock(net.runelite.client.config.ConfigManager.class);
		BankPotionViewsTest.stock(views.stock,
				java.util.Arrays.asList(
						new BankPotionStock.Potion(1000, 0, 100, 2, false, java.util.Collections.singletonList(100)),
						new BankPotionStock.Potion(1001, 1, 101, 3, false, java.util.Collections.singletonList(101))));
		when(f.items.widget.getChild(anyInt())).thenAnswer(call -> f.items.children.get(call.getArgument(0)));
		net.runelite.api.Client client = mock(net.runelite.api.Client.class);
		when(client.getMouseCanvasPosition()).thenReturn(new net.runelite.api.Point(220, 45));
		f.layout.enableMovement(client, views);
		org.mockito.ArgumentCaptor<Object[]> listenerCapture = org.mockito.ArgumentCaptor.forClass(Object[].class);
		verify(f.parts.get(0).widget).setOnDragCompleteListener(listenerCapture.capture());
		Object[] listener = listenerCapture.getValue();
		net.runelite.api.widgets.JavaScriptCallback drag = (net.runelite.api.widgets.JavaScriptCallback) listener[0];
		drag.run(mock(net.runelite.api.ScriptEvent.class));
		f.assertRow(0, 205, 36, 204);
		f.assertRow(1, 0, 36, 204);
		verify(views.settings.configs).setRSProfileConfiguration("windowmarkers", "storedPotion.order.1", "1001,1000");
		f.layout.clear();
		clearInvocations(views.settings.configs);
		drag.run(mock(net.runelite.api.ScriptEvent.class));
		verifyNoInteractions(views.settings.configs);
		verify(client, never()).runScript(any(Object[].class));
	}

	@Test
	public void widthChangeUpdatesNativeScrollWidthWithoutChangingScrollPosition()
	{
		Fixture f = new Fixture();
		f.layout.capture(f.items.widget);
		f.items.widget.setScrollWidth(409);
		f.items.widget.setScrollY(90);
		f.items.widget.setOriginalWidth(601);
		f.layout.apply(f.items.widget);
		assertEquals(601, f.items.widget.getScrollWidth());
		assertEquals(90, f.items.widget.getScrollY());
		verify(f.items.widget, times(1)).setScrollY(90);
	}

	@Test
	public void nativeColumnsResizeTogetherWithoutChangingActionsOrderOrScroll()
	{
		Fixture f = new Fixture();
		f.layout.capture(f.items.widget);
		for (int width : new int[] { 473, 474, 409, 701, 409 })
		{
			f.items.widget.setOriginalWidth(width);
			f.layout.apply(f.items.widget);
			f.assertRow(0, 0, 36, width / 2);
			f.assertRow(1, (width + 1) / 2, 36, width / 2);
			assertEquals(width / 2, f.divider.widget.getOriginalX());
			verify(f.items.widget, never()).setScrollY(anyInt());
			for (BankControlsTest.Cell cell : f.parts)
			{
				verify(cell.widget, never()).setHidden(anyBoolean());
				verify(cell.widget, never()).setOnOpListener(any(Object[].class));
				verify(cell.widget, never()).clearActions();
				verify(cell.widget, never()).setItemId(anyInt());
			}
		}
		clearInvocations(f.parts.stream().map(c -> c.widget).toArray());
		f.layout.apply(f.items.widget);
		for (BankControlsTest.Cell part : f.parts)
			verify(part.widget, never()).revalidate();
	}

	@Test
	public void customizerSavedCoordinatesAndPotionBarsRemainWithTheirPotions()
	{
		Fixture f = new Fixture();
		f.items.widget.setOriginalWidth(473);
		f.setRow(0, 0, 36, 236);
		f.setRow(1, 237, 36, 236);
		f.divider.widget.setOriginalX(236);
		f.layout.capture(f.items.widget);
		f.setRow(0, 205, 72, 204);
		f.setRow(1, 0, 36, 204);
		List<BankControlsTest.Cell> bars = f.bars();
		f.parts.get(3).widget.setHidden(true);
		for (int width : new int[] { 473, 409, 601, 409 })
		{
			f.items.widget.setOriginalWidth(width);
			f.layout.apply(f.items.widget);
			f.assertRow(0, (width + 1) / 2, 72, width / 2);
			f.assertRow(1, 0, 36, width / 2);
			for (int row = 0; row < 2; row++)
			{
				Widget dose = f.parts.get(row * 5 + 3).widget;
				assertEquals(dose.getOriginalX(), bars.get(row * 3).widget.getOriginalX());
				assertEquals(dose.getOriginalY(), bars.get(row * 3).widget.getOriginalY());
				assertEquals(dose.getOriginalX() + 2, bars.get(row * 3 + 2).widget.getOriginalX());
				assertEquals(145, bars.get(row * 3).widget.getOriginalWidth());
				assertEquals(80, bars.get(row * 3 + 1).widget.getOriginalWidth());
			}
		}
		verify(f.parts.get(3).widget, never()).setHidden(false);
		f.setRow(0, 0, 36, 204);
		f.setRow(1, 205, 72, 204);
		f.layout.apply(f.items.widget);
		f.assertRow(0, 0, 36, 204);
		f.assertRow(1, 205, 72, 204);
	}

	@Test
	public void unrelatedExtrasAndReplacedOrHiddenInterfacesAreNotRewritten()
	{
		Fixture f = new Fixture();
		f.layout.capture(f.items.widget);
		BankControlsTest.Cell unrelated = f.cell(6, 9, 80, 13, WidgetType.TEXT);
		f.items.children.add(unrelated.widget);
		f.items.widget.setOriginalWidth(601);
		f.layout.apply(f.items.widget);
		verify(unrelated.widget, never()).setOriginalX(anyInt());
		when(f.items.widget.isHidden()).thenReturn(true);
		clearInvocations(f.parts.stream().map(c -> c.widget).toArray());
		f.layout.apply(f.items.widget);
		for (BankControlsTest.Cell part : f.parts)
			verifyNoInteractions(part.widget);
		when(f.items.widget.isHidden()).thenReturn(false);
		f.items.children.set(1, mock(Widget.class));
		f.layout.apply(f.items.widget);
		for (BankControlsTest.Cell part : f.parts)
			verifyNoInteractions(part.widget);
		f.layout.clear();
		f.layout.apply(f.items.widget);
	}

	@Test
	public void controllerCapturesNativeGeometryBeforeOtherPluginsRestoreCoordinates()
	{
		BankControlLayoutTest.Fixture owner = new BankControlLayoutTest.Fixture();
		Fixture f = new Fixture();
		when(owner.controller.client.getWidget(InterfaceID.Bankmain.POTIONSTORE_ITEMS)).thenReturn(f.items.widget);
		owner.bus.register(ScriptPostFired.class, event ->
		{
			if (event.getScriptId() == ScriptID.POTIONSTORE_BUILD)
			{
				f.setRow(0, 205, 72, 204);
				f.setRow(1, 0, 36, 204);
				f.bars();
			}
		}, -1);
		f.items.widget.setOriginalWidth(473);
		f.setRow(0, 0, 36, 236);
		f.setRow(1, 237, 36, 236);
		owner.bus.post(new ScriptPostFired(ScriptID.POTIONSTORE_BUILD));
		f.assertRow(0, 237, 72, 236);
		f.assertRow(1, 0, 36, 236);
		verify(owner.controller.client, never()).runScript(any(Object[].class));
	}

	private static final class Fixture
	{
		final BankPotionLayout layout = new BankPotionLayout();
		final BankControlsTest.Cell items = new BankControlsTest.Cell(InterfaceID.Bankmain.POTIONSTORE_ITEMS, null, 0,
				0, 409, 240, 0, 0, 0, 0);
		final List<BankControlsTest.Cell> parts = new ArrayList<>();
		final BankControlsTest.Cell divider;

		Fixture()
		{
			for (int row = 0; row < 2; row++)
			{
				int x = row * 205;
				parts.add(cell(x, 36, 204, 36, WidgetType.GRAPHIC));
				parts.add(cell(x + 1, 38, 36, 32, WidgetType.GRAPHIC));
				parts.add(cell(x + 38, 38, 204, 36, WidgetType.TEXT));
				parts.add(cell(x + 38, 57, 204, 36, WidgetType.TEXT));
				parts.add(cell(x + 188, 57, 13, 12, WidgetType.GRAPHIC));
				when(parts.get(row * 5 + 1).widget.getItemId()).thenReturn(100 + row);
			}
			for (BankControlsTest.Cell part : parts)
				items.children.add(part.widget);
			divider = cell(204, 36, 1, 72, WidgetType.RECTANGLE);
			items.children.add(divider.widget);
		}

		BankControlsTest.Cell cell(int x, int y, int w, int h, int type)
		{
			BankControlsTest.Cell c = new BankControlsTest.Cell(items.id, items, x, y, w, h, 0, 0, 0, 0);
			when(c.widget.getType()).thenReturn(type);
			when(c.widget.getItemId()).thenReturn(-1);
			return c;
		}

		void setRow(int row, int x, int y, int width)
		{
			int[] xs = { x, x + 1, x + 38, x + 38, x + width - 16 }, ys = { y, y + 2, y + 2, y + 21, y + 21 };
			for (int i = 0; i < 5; i++)
			{
				Widget w = parts.get(row * 5 + i).widget;
				w.setOriginalX(xs[i]);
				w.setOriginalY(ys[i]);
				if (i == 0 || i == 2 || i == 3)
					w.setOriginalWidth(width);
			}
		}

		List<BankControlsTest.Cell> bars()
		{
			List<BankControlsTest.Cell> result = new ArrayList<>();
			for (int row = 0; row < 2; row++)
			{
				Widget dose = parts.get(row * 5 + 3).widget;
				result.add(cell(dose.getOriginalX(), dose.getOriginalY(), 145, 13, WidgetType.RECTANGLE));
				result.add(cell(dose.getOriginalX(), dose.getOriginalY(), 80, 13, WidgetType.RECTANGLE));
				result.add(cell(dose.getOriginalX() + 2, dose.getOriginalY() + 1, 204, 36, WidgetType.TEXT));
			}
			for (BankControlsTest.Cell extra : result)
				items.children.add(extra.widget);
			return result;
		}

		void assertRow(int row, int x, int y, int width)
		{
			assertEquals(x, parts.get(row * 5).widget.getOriginalX());
			assertEquals(y, parts.get(row * 5).widget.getOriginalY());
			assertEquals(x + 1, parts.get(row * 5 + 1).widget.getOriginalX());
			assertEquals(x + 38, parts.get(row * 5 + 2).widget.getOriginalX());
			assertEquals(x + 38, parts.get(row * 5 + 3).widget.getOriginalX());
			assertEquals(x + width - 16, parts.get(row * 5 + 4).widget.getOriginalX());
			assertEquals(width, parts.get(row * 5).widget.getOriginalWidth());
		}
	}
}
