package com.windowmarkers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;

final class BankPotionPlacement
{
	private final List<Section> sections = new ArrayList<>();
	private final Map<Widget, Integer> itemKeys = new IdentityHashMap<>();
	private long revision;
	private Widget held;
	private long heldRevision;
	private boolean released;
	private BooleanSupplier heldCurrent;
	private int heldItem;

	void clear()
	{
		sections.clear();
		itemKeys.clear();
		revision++;
	}

	void capture(Client client, int tab, List<Widget> cells, Map<Widget, BankPotionStock.Potion> potions,
			BankPotionSettings settings)
	{
		cells.sort(Comparator.comparingInt(Widget::getOriginalY).thenComparingInt(Widget::getOriginalX));
		List<Widget> ordered = new ArrayList<>(cells);
		List<Widget> nativeCells = cells.stream().filter(cell -> !potions.containsKey(cell))
				.sorted(Comparator.comparingInt(Widget::getIndex)).collect(Collectors.toList());
		for (Widget cell : nativeCells)
		{
			net.runelite.api.ItemComposition item = client.getItemDefinition(cell.getItemId());
			itemKeys.put(cell, item != null && item.getPlaceholderTemplateId() > 0
					? item.getPlaceholderId() : cell.getItemId());
		}
		List<Integer> nativeOrder = ids(nativeCells, potions);
		List<Integer> displayOrder = BankPotionItemOrder.read(settings, tab, nativeOrder);
		if (!displayOrder.equals(nativeOrder))
		{
			Map<Integer, Integer> ranks = new HashMap<>();
			for (int i = 0; i < displayOrder.size(); i++)
				ranks.put(displayOrder.get(i), i);
			nativeCells.sort(Comparator.comparingInt(cell -> ranks.get(itemKeys.get(cell))));
			int next = 0;
			for (int i = 0; i < ordered.size(); i++)
				if (!potions.containsKey(ordered.get(i)))
					ordered.set(i, nativeCells.get(next++));
		}
		List<Widget> saved = new ArrayList<>();
		for (Widget cell : cells)
			if (potions.containsKey(cell) && settings.bankPosition(potions.get(cell).enumId) >= 0)
				saved.add(cell);
		saved.sort(Comparator.comparingInt(cell -> settings.bankPosition(potions.get(cell).enumId)));
		ordered.removeAll(saved);
		for (Widget cell : saved)
			ordered.add(Math.min(ordered.size(), settings.bankPosition(potions.get(cell).enumId)), cell);
		int[][] slots = new int[cells.size()][2];
		for (int i = 0; i < cells.size(); i++)
			slots[i] = new int[] { cells.get(i).getOriginalX(), cells.get(i).getOriginalY() };
		for (int i = 0; i < ordered.size(); i++)
		{
			Widget cell = ordered.get(i);
			cell.setOriginalX(slots[i][0]);
			cell.setOriginalY(slots[i][1]);
			cell.revalidate();
		}
		sections.add(new Section(tab, ordered, nativeOrder));
	}

	void drag(Client client, Widget parent, Map<Widget, BankPotionStock.Potion> potions,
			BankPotionSettings settings, BooleanSupplier current)
	{
		Widget dragged = client.getDraggedWidget();
		Section source = section(dragged);
		Widget target = client.getDraggedOnWidget();
		Section destination = section(target);
		boolean stored = potions.containsKey(dragged), ontoStored = potions.containsKey(target);
		if (stored || ontoStored)
			client.setDraggedOnWidget(null);
		if (client.getMouseCurrentButton() != 0)
		{
			if (held != dragged || released)
			{
				held = dragged;
				heldRevision = revision;
				heldCurrent = current;
				heldItem = dragged == null ? -1 : dragged.getItemId();
				released = false;
			}
			return;
		}
		if (released || held != dragged)
			return;
		released = true;
		if (heldRevision != revision || heldCurrent == null || !heldCurrent.getAsBoolean()
				|| !current.getAsBoolean() || !client.isDraggingWidget()
				|| parent.isHidden() || source == null || destination == null || dragged == target
				|| dragged.getItemId() != heldItem
				|| parent.getChild(dragged.getIndex()) != dragged || parent.getChild(target.getIndex()) != target
				|| dragged.isSelfHidden() || target.isSelfHidden())
			return;
		Point mouse = client.getMouseCanvasPosition();
		if (mouse == null || !BankWidgetGeometry.bounds(parent).contains(mouse.getX(), mouse.getY()))
			return;
		if (stored || ontoStored)
		{
			if (source != destination && !stored)
			{
				Widget tabs = client.getWidget(net.runelite.api.gameval.InterfaceID.Bankmain.TABS);
				Widget tab = tabs == null ? null : tabs.getChild(10 + destination.tab);
				if (tab != null)
					client.setDraggedOnWidget(tab);
				return;
			}
			move(dragged, target, destination, potions, settings, true);
		}
		else if (source == destination)
		{
			boolean insert = client.getVarbitValue(VarbitID.BANK_INSERTMODE) == 1;
			reorder(source.nativeOrder, itemKeys.get(dragged), itemKeys.get(target), insert);
			move(dragged, target, destination, potions, settings, insert);
		}
	}

	private Section section(Widget widget)
	{
		for (Section section : sections)
			if (section.cells.contains(widget))
				return section;
		return null;
	}

	private void move(Widget dragged, Widget target, Section destination,
			Map<Widget, BankPotionStock.Potion> potions, BankPotionSettings settings, boolean insert)
	{
		for (Section section : sections)
		{
			List<Widget> ordered = new ArrayList<>(section.cells);
			boolean removed = ordered.contains(dragged);
			if (section == destination && removed)
				reorder(ordered, dragged, target, insert);
			else
			{
				ordered.remove(dragged);
				if (section == destination)
					ordered.add(ordered.indexOf(target), dragged);
			}
			if (removed || section == destination)
			{
				List<Integer> displayOrder = ids(ordered, potions);
				if (!displayOrder.equals(section.nativeOrder)
						|| settings.configs.getRSProfileConfiguration("windowmarkers", BankPotionSettings.PREFIX + "bankItems." + section.tab) != null)
					BankPotionItemOrder.save(settings, section.tab, section.nativeOrder, displayOrder);
				for (int i = 0; i < ordered.size(); i++)
				{
					BankPotionStock.Potion potion = potions.get(ordered.get(i));
					if (potion != null)
					{
						settings.setBankTab(potion.enumId, section.tab);
						settings.setBankPosition(potion.enumId, i);
					}
				}
			}
		}
		revision++;
	}

	private static <T> void reorder(List<T> order, T source, T target, boolean insert)
	{
		int from = order.indexOf(source), to = order.indexOf(target);
		if (from < 0 || to < 0)
			return;
		if (insert)
		{
			order.remove(from);
			order.add(to, source);
		}
		else
			Collections.swap(order, from, to);
	}

	private List<Integer> ids(List<Widget> cells, Map<Widget, BankPotionStock.Potion> potions)
	{
		return cells.stream().filter(cell -> !potions.containsKey(cell)).map(itemKeys::get).collect(Collectors.toList());
	}

	private static final class Section
	{
		final int tab;
		final List<Widget> cells;
		final List<Integer> nativeOrder;

		Section(int tab, List<Widget> cells, List<Integer> nativeOrder)
		{
			this.tab = tab;
			this.cells = cells;
			this.nativeOrder = nativeOrder;
		}
	}
}
