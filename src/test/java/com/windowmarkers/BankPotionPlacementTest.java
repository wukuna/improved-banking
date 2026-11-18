package com.windowmarkers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class BankPotionPlacementTest
{
	private static final class Fixture
	{
		final BankPotionPlacement placement = new BankPotionPlacement();
		final BankPotionSettings settings = new BankPotionSettings();
		final Map<String, String> saved = new HashMap<>();
		final Map<Widget, BankPotionStock.Potion> potions = new IdentityHashMap<>();
		final Client client = mock(Client.class);
		final BankControlsTest.Cell parent = new BankControlsTest.Cell(1, null, 70, 90, 450, 400, 0, 0, 0, 0);
		final Widget first = cell(500), second = cell(501), potion = cell(104);

		Fixture()
		{
			settings.configs = mock(ConfigManager.class);
			when(settings.configs.getRSProfileConfiguration(eq("windowmarkers"), anyString()))
					.thenAnswer(call -> saved.get(call.getArgument(1)));
			doAnswer(call ->
			{
				saved.put(call.getArgument(1), String.valueOf((Object) call.getArgument(2)));
				return null;
			}).when(settings.configs).setRSProfileConfiguration(eq("windowmarkers"), anyString(), any());
			potions.put(potion, new BankPotionStock.Potion(1000, 3, 104, 20, false, Collections.singletonList(104)));
			when(parent.widget.getChild(anyInt())).thenAnswer(call -> parent.children.get(call.getArgument(0)));
			when(client.getDraggedWidget()).thenReturn(potion);
		}

		Widget cell(int id)
		{
			int index = parent.children.size();
			Widget cell = new BankControlsTest.Cell(1, parent, 51 + index * 48, 4, 36, 32, 0, 0, 0, 0).widget;
			cell.setItemId(id);
			when(cell.getIndex()).thenReturn(index);
			parent.children.add(cell);
			return cell;
		}

		void capture()
		{
			placement.capture(client, 0, new ArrayList<>(parent.children), potions, settings);
		}

		Runnable drag(Widget target, java.util.function.BooleanSupplier current)
		{
			java.awt.Rectangle bounds = BankWidgetGeometry.bounds(target);
			when(client.getMouseCanvasPosition()).thenReturn(new Point(bounds.x + 10, bounds.y + 10));
			when(client.getDraggedOnWidget()).thenReturn(target);
			when(client.getMouseCurrentButton()).thenReturn(1);
			when(client.isDraggingWidget()).thenReturn(true);
			placement.drag(client, parent.widget, potions, settings, current);
			return () ->
			{
				when(client.getMouseCurrentButton()).thenReturn(0);
				placement.drag(client, parent.widget, potions, settings, current);
			};
		}
	}

	@Test
	public void draggingOnlySavesLocalPositionAndRebuildRetainsNativeWidgetIdentities()
	{
		Fixture f = new Fixture();
		f.capture();
		verify(f.settings.configs, never()).setRSProfileConfiguration(anyString(), anyString(), any());
		f.drag(f.first, () -> true).run();
		assertEquals("0", f.saved.get("storedPotion.1000.position"));
		f.placement.clear();
		f.capture();
		assertEquals(51, f.potion.getOriginalX());
		assertEquals(99, f.first.getOriginalX());
		assertEquals(147, f.second.getOriginalX());
		assertSame(f.first, f.parent.children.get(0));
		assertSame(f.potion, f.parent.children.get(2));
		verify(f.client, never()).runScript(any(Object[].class));
	}

	@Test
	public void forwardDragUsesTheTargetSlotAndOtherPotionsKeepTheirOrder()
	{
		Fixture f = new Fixture();
		Widget other = f.cell(204);
		f.potions.put(other, new BankPotionStock.Potion(2000, 4, 204, 10, false, Collections.singletonList(204)));
		f.saved.put("storedPotion.1000.position", "0");
		f.saved.put("storedPotion.2000.position", "1");
		f.capture();
		f.drag(other, () -> true).run();
		assertEquals("1", f.saved.get("storedPotion.1000.position"));
		assertEquals("0", f.saved.get("storedPotion.2000.position"));
	}

	@Test
	public void dropOnAnotherNativeTabUpdatesPotionTabWithoutReorderingNativeItems()
	{
		Fixture f = new Fixture();
		f.placement.capture(f.client, 2, new ArrayList<>(Collections.singletonList(f.first)), f.potions, f.settings);
		f.placement.capture(f.client, 0, new ArrayList<>(Arrays.asList(f.second, f.potion)), f.potions, f.settings);
		f.drag(f.first, () -> true).run();
		assertEquals("2", f.saved.get("storedPotion.1000.tab"));
		assertEquals("0", f.saved.get("storedPotion.1000.position"));
		assertSame(f.first, f.parent.children.get(0));
	}

	@Test
	public void profileChangeRebuildOrPointerOutsideViewportCancelsPendingDrop()
	{
		for (int cancel = 0; cancel < 3; cancel++)
		{
			Fixture f = new Fixture();
			f.capture();
			final boolean current = cancel != 0;
			Runnable drop = f.drag(f.first, () -> current);
			if (cancel == 1)
				f.placement.clear();
			if (cancel == 2)
				when(f.client.getMouseCanvasPosition()).thenReturn(new Point(-1, -1));
			drop.run();
			assertTrue(f.saved.isEmpty());
		}
	}

	@Test
	public void nativeItemOntoPotionInsertsLocallyAndSurvivesRebuild()
	{
		Fixture f = new Fixture();
		f.saved.put("storedPotion.1000.position", "0");
		f.capture();
		when(f.client.getDraggedWidget()).thenReturn(f.second);
		Runnable drop = f.drag(f.potion, () -> true);
		drop.run();
		Map<String, String> saved = new HashMap<>(f.saved);
		drop.run();
		assertEquals(saved, f.saved);
		assertEquals("1", f.saved.get("storedPotion.1000.position"));
		assertEquals("500,501|501,500", f.saved.get("storedPotion.bankItems.0"));
		verify(f.client, atLeastOnce()).setDraggedOnWidget(null);
		verify(f.second, never()).setOnDragCompleteListener(any(Object[].class));
		f.placement.clear();
		f.capture();
		assertEquals(51, f.second.getOriginalX());
		assertEquals(99, f.potion.getOriginalX());
		assertEquals(147, f.first.getOriginalX());
		assertSame(f.first, f.parent.children.get(0));
		assertSame(f.second, f.parent.children.get(1));
	}

	@Test
	public void nativeDragAfterPotionInsertionKeepsLocalOrderAndNativeTarget()
	{
		Fixture f = new Fixture();
		f.saved.put("storedPotion.1000.position", "0");
		f.capture();
		when(f.client.getDraggedWidget()).thenReturn(f.second);
		f.drag(f.potion, () -> true).run();
		f.placement.clear();
		f.capture();
		clearInvocations(f.client);
		when(f.client.getDraggedWidget()).thenReturn(f.first);
		f.drag(f.second, () -> true).run();
		verify(f.client, never()).setDraggedOnWidget(any());
		assertEquals("501,500|500,501", f.saved.get("storedPotion.bankItems.0"));
		assertEquals("1", f.saved.get("storedPotion.1000.position"));
	}

	@Test
	public void ordinaryNativeDragsWithNoStoredStacksDoNotCreateLocalOrder()
	{
		Fixture f = new Fixture();
		f.parent.children.remove(f.potion);
		f.potions.clear();
		f.capture();
		when(f.client.getDraggedWidget()).thenReturn(f.first);
		f.drag(f.second, () -> true).run();
		assertTrue(f.saved.isEmpty());
		verify(f.client, never()).setDraggedOnWidget(any());
	}

	@Test
	public void nativeItemDroppedOnPotionInAnotherTabUsesTheNativeTabTarget()
	{
		Fixture f = new Fixture();
		f.placement.capture(f.client, 2, new ArrayList<>(Arrays.asList(f.second, f.potion)), f.potions, f.settings);
		f.placement.capture(f.client, 0, new ArrayList<>(Collections.singletonList(f.first)), f.potions, f.settings);
		Widget tabs = mock(Widget.class), button = mock(Widget.class);
		when(f.client.getWidget(net.runelite.api.gameval.InterfaceID.Bankmain.TABS)).thenReturn(tabs);
		when(tabs.getChild(12)).thenReturn(button);
		when(f.client.getDraggedWidget()).thenReturn(f.first);
		f.drag(f.potion, () -> true).run();
		verify(f.client).setDraggedOnWidget(button);
		assertTrue(f.saved.isEmpty());
	}

	@Test
	public void localItemOrderSurvivesPlaceholderReplacement()
	{
		Fixture f = new Fixture();
		f.saved.put("storedPotion.bankItems.0", "500,501|501,500");
		f.saved.put("storedPotion.1000.position", "1");
		net.runelite.api.ItemComposition placeholder = mock(net.runelite.api.ItemComposition.class);
		when(placeholder.getPlaceholderTemplateId()).thenReturn(14401);
		when(placeholder.getPlaceholderId()).thenReturn(501);
		when(f.client.getItemDefinition(1501)).thenReturn(placeholder);
		f.second.setItemId(1501);
		f.capture();
		assertEquals(51, f.second.getOriginalX());
		assertEquals(99, f.potion.getOriginalX());
		assertEquals(147, f.first.getOriginalX());
		verify(f.settings.configs, never()).setRSProfileConfiguration(anyString(), anyString(), any());
	}

	@Test
	public void aClickOrChangedItemCannotCommitAnInsertion()
	{
		for (boolean changedItem : new boolean[] { false, true })
		{
			Fixture f = new Fixture();
			f.capture();
			Runnable drop = f.drag(f.first, () -> true);
			if (changedItem)
				f.potion.setItemId(999);
			else
				when(f.client.isDraggingWidget()).thenReturn(false);
			drop.run();
			assertTrue(f.saved.isEmpty());
		}
	}

	@Test
	public void malformedAndOutOfRangePositionsFallBackWithoutRewritingSettings()
	{
		for (String value : new String[] { "broken", "-3", "4001" })
		{
			Fixture f = new Fixture();
			f.saved.put("storedPotion.1000.position", value);
			f.capture();
			assertEquals(147, f.potion.getOriginalX());
			verify(f.settings.configs, never()).setRSProfileConfiguration(anyString(), anyString(), any());
		}
	}
}
