package com.windowmarkers;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public class BankActionLayoutTest
{
	@Test
	public void legacyOffsetsPreserveGapsAndCenterOnTheStripAxis()
	{
		BankActionLayout layout = BankActionLayout.read("v2;A:R:0:1:300;B:R:1:9:50;C:B:0:300:10;D:B:1:50:0;");
		Map<String, Dimension> sizes = sizes("A", "B", "C", "D");
		BankActionLayout.Plan plan = layout.plan(718, 650, sizes, true);
		assertEquals(104, plan.bounds.get("B").y);
		assertEquals(354, plan.bounds.get("A").y);
		assertEquals(693, plan.bounds.get("A").getCenterX(), 0.5);
		assertEquals(plan.bounds.get("A").getCenterX(), plan.bounds.get("B").getCenterX(), 0.5);
		assertEquals(52, plan.bounds.get("D").x);
		assertEquals(302, plan.bounds.get("C").x);
		assertEquals(plan.bounds.get("C").getCenterY(), plan.bounds.get("D").getCenterY(), 0.5);
		assertEquals(plan.bounds, BankActionLayout.read(layout.write()).plan(718, 650, sizes, true).bounds);
	}

	@Test
	public void shrinkingSideStripsFillsThemBeforeOverflowingInOrder()
	{
		BankActionLayout layout = layout('R', "A", "B", "C", "D", "E", "F");
		Map<String, Dimension> sizes = sizes("A", "B", "C", "D", "E", "F");
		String saved = layout.write();
		Map<String, Rectangle> large = layout.plan(718, 650, sizes, true).bounds;
		BankActionLayout.Plan small = layout.plan(718, 240, sizes, true);
		assertEquals(Arrays.asList("A", "B", "C"), on(small, 'R'));
		assertEquals(Arrays.asList("D", "E", "F"), on(small, 'B'));
		assertEquals(36, small.bottomHeight);
		assertFits(small, sizes, 718, 240);
		assertEquals(saved, layout.write());
		assertEquals(large, layout.plan(718, 650, sizes, true).bounds);
	}

	@Test
	public void overflowUsesNearbyAvailableSideBeforeBottom()
	{
		BankActionLayout layout = layout('T', "A", "B", "C", "D");
		BankActionLayout.Plan plan = layout.plan(718, 240, sizes("A", "B", "C", "D"), true);
		assertEquals(Arrays.asList("A"), on(plan, 'T'));
		assertEquals(Arrays.asList("B", "C", "D"), on(plan, 'L'));
		assertTrue(on(plan, 'B').isEmpty());
	}

	@Test
	public void shrinkingBottomStripMovesTheTailToNearestSide()
	{
		String[] keys = { "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N" };
		Map<String, Dimension> sizes = sizes(keys);
		BankActionLayout layout = layout('B', keys);
		BankActionLayout.Plan plan = layout.plan(478, 400, sizes, false);
		assertEquals(12, on(plan, 'B').size());
		assertEquals(Arrays.asList("M", "N"), on(plan, 'R'));
		assertEquals(36, plan.bottomHeight);
		assertFits(plan, sizes, 478, 400);
		assertEquals(14, on(layout.plan(718, 400, sizes, false), 'B').size());
	}

	@Test
	public void existingMembersKeepTheirOrderBeforeOverflowArrives()
	{
		BankActionLayout layout = layout('T', "A", "B", "C", "D");
		layout.move("X", 'L', 0);
		layout.move("Y", 'L', 1);
		Map<String, Dimension> sizes = sizes("A", "B", "C", "D", "X", "Y");
		BankActionLayout.Plan plan = layout.plan(718, 240, sizes, true);
		assertEquals(Arrays.asList("X", "Y", "B"), on(plan, 'L'));
		assertEquals(Arrays.asList("C", "D"), on(plan, 'B'));
		assertFits(plan, sizes, 718, 240);
	}

	@Test
	public void unavailableLeftStripDoesNotChangeSavedDestination()
	{
		BankActionLayout layout = layout('L', "A", "B", "C", "D");
		String saved = layout.write();
		Map<String, Dimension> sizes = sizes("A", "B", "C", "D");
		for (boolean tags : new boolean[] { false, true })
		{
			BankActionLayout.Plan plan = layout.plan(478, 240, sizes, tags);
			assertTrue(on(plan, 'L').isEmpty());
			assertFits(plan, sizes, 478, 240);
		}
		assertEquals(saved, layout.write());
		assertEquals(4, on(layout.plan(718, 650, sizes, true), 'L').size());
	}

	@Test
	public void mixedButtonsStayVisibleAlignedAndOrderedThroughRepeatedResizes()
	{
		Map<String, Dimension> sizes = new LinkedHashMap<>();
		BankActionLayout layout = BankActionLayout.read("");
		char[] docks = { 'R', 'L', 'T', 'B' };
		for (int i = 0; i < 18; i++)
		{
			String key = "BUTTON_" + (char) ('A' + i);
			sizes.put(key,
					new Dimension(i % 6 == 0 ? 96 : i % 3 == 0 ? 46 : 36, i % 6 == 0 ? 22 : i % 4 == 0 ? 30 : 36));
			layout.move(key, docks[i % 4], i);
			layout.entries.get(key).offset = i * 57;
		}
		String saved = layout.write();
		Map<String, Rectangle> initial = layout.plan(1000, 650, sizes, true).bounds;
		for (int width : new int[] { 1000, 718, 536, 510, 478, 718, 1000 })
			for (int height : new int[] { 650, 330, 240, 160, 240, 650 })
				for (boolean tags : new boolean[] { false, true })
				{
					BankActionLayout.Plan plan = layout.plan(width, height, sizes, tags);
					assertFits(plan, sizes, width, height);
					for (char dock : docks)
					{
						List<String> own = new ArrayList<>();
						for (String key : on(plan, dock))
							if (layout.entries.get(key).dock == dock)
								own.add(key);
						for (int i = 1; i < own.size(); i++)
							assertTrue(layout.entries.get(own.get(i - 1)).order < layout.entries.get(own.get(i)).order);
					}
				}
		assertEquals(saved, layout.write());
		assertEquals(initial, layout.plan(1000, 650, sizes, true).bounds);
	}

	@Test
	public void spacedButtonsCloseGapsBeforeOverflowAndRecoverOnExpansion()
	{
		BankActionLayout layout = BankActionLayout.read("v2;A:R:0:0:40;B:R:1:0:200;C:R:2:0:400;D:R:3:0:500;");
		Map<String, Dimension> sizes = sizes("A", "B", "C", "D");
		String saved = layout.write();
		BankActionLayout.Plan original = layout.plan(718, 650, sizes, true);
		BankActionLayout.Plan medium = layout.plan(718, 300, sizes, true);
		assertEquals(Arrays.asList("A", "B", "C", "D"), on(medium, 'R'));
		assertFits(medium, sizes, 718, 300);
		BankActionLayout.Plan small = layout.plan(718, 240, sizes, true);
		assertEquals(Arrays.asList("A", "B", "C"), on(small, 'R'));
		assertEquals(Arrays.asList("D"), on(small, 'B'));
		assertFits(small, sizes, 718, 240);
		assertEquals(saved, layout.write());
		assertEquals(original.bounds, layout.plan(718, 650, sizes, true).bounds);
	}

	@Test
	public void slidingIntoTheMiddleRetainsOrderAndOffsetAfterReload()
	{
		BankActionLayout layout = layout('R', "A", "B", "C", "D");
		Map<String, Dimension> sizes = sizes("A", "B", "C", "D");
		BankActionLayout.Plan initial = layout.plan(718, 650, sizes, false);
		Rectangle rail = BankActionLayout.rail('R', 718, 650, initial.bottomHeight);
		layout.insert("D", 'R', new java.awt.Point(rail.x, 110), rail, initial.bounds, initial.docks);
		BankActionLayout.Plan result = layout.plan(718, 650, sizes, false);
		assertEquals(Arrays.asList("A", "B", "D", "C"), on(result, 'R'));
		assertEquals(result.bounds, BankActionLayout.read(layout.write()).plan(718, 650, sizes, false).bounds);
	}

	private static BankActionLayout layout(char dock, String... keys)
	{
		BankActionLayout layout = BankActionLayout.read("");
		for (int i = 0; i < keys.length; i++)
			layout.move(keys[i], dock, i);
		return layout;
	}

	private static Map<String, Dimension> sizes(String... keys)
	{
		Map<String, Dimension> sizes = new LinkedHashMap<>();
		for (String key : keys)
			sizes.put(key, new Dimension(36, 36));
		return sizes;
	}

	private static List<String> on(BankActionLayout.Plan plan, char dock)
	{
		List<String> keys = new ArrayList<>();
		plan.docks.forEach((key, value) ->
		{
			if (value == dock)
				keys.add(key);
		});
		keys.sort((a, b) ->
		{
			Rectangle first = plan.bounds.get(a), second = plan.bounds.get(b);
			int row = Double.compare(first.getCenterY(), second.getCenterY());
			return row != 0 ? row : Integer.compare(first.x, second.x);
		});
		return keys;
	}

	private static void assertFits(BankActionLayout.Plan plan, Map<String, Dimension> sizes, int width, int height)
	{
		assertEquals(width + "x" + height, sizes.keySet(), plan.bounds.keySet());
		plan.bounds.forEach((key, r) ->
		{
			assertEquals(sizes.get(key), r.getSize());
			assertTrue(new Rectangle(0, 0, width, height).contains(r));
			plan.bounds.forEach((other, b) ->
			{
				if (!key.equals(other))
					assertFalse(key + " overlaps " + other, r.intersects(b));
			});
			char dock = plan.docks.get(key);
			if (dock != 'B')
				assertEquals(BankActionLayout.rail(dock, width, height, plan.bottomHeight, plan.tagTop).getCenterX(),
						r.getCenterX(), 0.5);
		});
	}
}
