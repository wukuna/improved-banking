package com.windowmarkers;

import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import net.runelite.api.Client;
import net.runelite.api.SpriteID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.JavaScriptCallback;
import net.runelite.api.widgets.WidgetConfig;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetType;

final class BankPotionLayout
{
	private Widget parent;
	private Widget[] nativeChildren;
	private final List<Row> rows = new ArrayList<>();
	private final List<Widget> dividers = new ArrayList<>();
	private final List<Widget> fillers = new ArrayList<>();
	private final List<Extra> extras = new ArrayList<>();
	private int childCount;
	private long generation;

	void clear()
	{
		generation++;
		parent = null;
		nativeChildren = null;
		rows.clear();
		dividers.clear();
		fillers.clear();
		extras.clear();
		childCount = 0;
	}

	void capture(Widget items)
	{
		clear();
		if (items == null || items.isHidden() || items.getWidth() < 100)
			return;
		Widget[] children = items.getDynamicChildren();
		if (children == null)
			return;
		parent = items;
		nativeChildren = children.clone();
		childCount = children.length;
		int half = items.getWidth() / 2, lastY = -1;
		for (int i = 0; i + 4 < children.length; i += 5)
		{
			Widget a = children[i], icon = children[i + 1], name = children[i + 2], dose = children[i + 3],
					heart = children[i + 4];
			if (a == null || icon == null || name == null || dose == null || heart == null
					|| a.getType() != WidgetType.GRAPHIC || a.isSelfHidden() || icon.getItemId() < 0
					|| name.getType() != WidgetType.TEXT || dose.getType() != WidgetType.TEXT
					|| heart.getType() != WidgetType.GRAPHIC || a.getOriginalWidth() < 36)
				continue;
			int column = a.getOriginalX() > 0 || lastY == a.getOriginalY() ? 1 : 0;
			rows.add(new Row(i, new Widget[] { a, icon, name, dose, heart }, column));
			lastY = a.getOriginalY();
		}
		int rowEnd = rows.isEmpty() ? 0 : rows.get(rows.size() - 1).index + 5;
		for (int i = 0; i < children.length; i++)
		{
			Widget child = children[i];
			if (child != null && child.getType() == WidgetType.RECTANGLE && child.getOriginalWidth() == 1
					&& child.getOriginalX() == half)
				dividers.add(child);
			if (i >= rowEnd && child != null && child.getType() == WidgetType.GRAPHIC
					&& child.getSpriteId() == SpriteID.RESIZEABLE_MODE_SIDE_PANEL_BACKGROUND
					&& child.getOriginalX() > 0 && child.getOriginalHeight() == 36)
				fillers.add(child);
		}
	}

	void apply(Widget items)
	{
		if (items == null || items != parent || items.isHidden() || items.getWidth() < 100)
			return;
		Widget[] children = items.getDynamicChildren();
		if (children == null || children.length < nativeChildren.length)
			return;
		for (int i = 0; i < nativeChildren.length; i++)
			if (children[i] != nativeChildren[i])
				return;
		if (items.getScrollWidth() != 0 && items.getScrollWidth() != items.getWidth())
		{
			items.setScrollWidth(items.getWidth());
			for (Widget child : children)
				if (child != null)
					child.revalidate();
		}
		if (children.length != childCount)
		{
			extras.clear();
			childCount = children.length;
			for (int i = nativeChildren.length; i + 2 < children.length; i += 3)
			{
				Widget back = children[i], fill = children[i + 1], text = children[i + 2];
				if (back == null || fill == null || text == null || back.getType() != WidgetType.RECTANGLE
						|| fill.getType() != WidgetType.RECTANGLE || text.getType() != WidgetType.TEXT)
					continue;
				for (Row row : rows)
				{
					Widget dose = row.parts[3];
					if (back.getOriginalX() == dose.getOriginalX() && back.getOriginalY() == dose.getOriginalY()
							&& fill.getOriginalX() == back.getOriginalX() && fill.getOriginalY() == back.getOriginalY()
							&& text.getOriginalX() == back.getOriginalX() + 2
							&& text.getOriginalY() == back.getOriginalY() + 1)
					{
						extras.add(new Extra(back, dose, 0, 0, false));
						extras.add(new Extra(fill, dose, 0, 0, false));
						extras.add(new Extra(text, dose, 2, 1, text.getOriginalWidth() == dose.getOriginalWidth()));
						break;
					}
				}
			}
		}
		int half = items.getWidth() / 2;
		for (Row row : rows)
		{
			Widget background = row.parts[0];
			int currentX = background.getOriginalX();
			if (currentX != row.lastX)
				row.column = currentX > 0 ? 1 : 0;
			int x = row.column == 0 ? 0 : (items.getWidth() + 1) / 2, y = background.getOriginalY();
			for (int i = 0; i < 5; i++)
			{
				Widget part = row.parts[i];
				int px = x + (i == 4 ? half - row.heartInset : row.dx[i]);
				int width = i == 0 || i == 2 || i == 3 ? half : part.getOriginalWidth();
				place(part, px, y + row.dy[i], width);
			}
			row.lastX = x;
		}
		for (Extra extra : extras)
			place(extra.widget, extra.anchor.getOriginalX() + extra.x, extra.anchor.getOriginalY() + extra.y,
					extra.widthFollows ? extra.anchor.getOriginalWidth() : extra.widget.getOriginalWidth());
		for (Widget divider : dividers)
			place(divider, half, divider.getOriginalY(), divider.getOriginalWidth());
		for (Widget filler : fillers)
			place(filler, (items.getWidth() + 1) / 2, filler.getOriginalY(), half);
	}

	void enableMovement(Client client, BankPotionViews views)
	{
		if (parent == null || !views.config.moveStoredPotions() || views.externalOrder())
			return;
		Map<Integer, BankPotionStock.Potion> stocked = new HashMap<>();
		for (BankPotionStock.Potion potion : views.stock.potions())
			stocked.put(potion.index * 5, potion);
		Map<Integer, List<Row>> sections = new HashMap<>();
		for (Row row : rows)
		{
			BankPotionStock.Potion potion = stocked.get(row.index);
			if (potion == null)
				continue;
			int section = row.parts[4].getSpriteId() == SpriteID.FAIRY_RING_REMOVE_FAVOURITE ? 0
					: potion.unfinished ? 2 : 1;
			sections.computeIfAbsent(section, key -> new ArrayList<>()).add(row);
		}
		Widget expected = parent;
		long expectedGeneration = generation;
		for (Map.Entry<Integer, List<Row>> entry : sections.entrySet())
		{
			List<Row> section = entry.getValue();
			section.sort(Comparator.comparingInt((Row row) -> row.parts[0].getOriginalY())
					.thenComparingInt(row -> row.parts[0].getOriginalX()));
			int top = section.get(0).parts[0].getOriginalY();
			int height = section.get(0).parts[0].getOriginalHeight();
			List<Integer> saved = views.settings.order(entry.getKey());
			section.sort(Comparator.comparingInt(row ->
			{
				int position = saved.indexOf(stocked.get(row.index).enumId);
				return position < 0 ? Integer.MAX_VALUE : position;
			}));
			positionRows(section, top, height);
			for (Row row : section)
			{
				Widget background = row.parts[0];
				background.setDragParent(parent);
				background.setClickMask(background.getClickMask() | WidgetConfig.DRAG | WidgetConfig.DRAG_ON);
				background.setDragDeadTime(Math.max(5, background.getDragDeadTime()));
				background.setHasListener(true);
				background.setOnDragCompleteListener((JavaScriptCallback) event ->
				{
					if (generation != expectedGeneration || parent != expected || parent.isHidden()
							|| views.externalOrder() || !views.config.moveStoredPotions()
							|| parent.getChild(row.index) != background)
						return;
					net.runelite.api.Point mouse = client.getMouseCanvasPosition();
					if (mouse == null)
						return;
					Row target = null;
					for (Row other : section)
					{
						if (other.parts[0].getBounds().contains(mouse.getX(), mouse.getY()))
							target = other;
					}
					if (target == null || target == row)
						return;
					int from = section.indexOf(row);
					int to = section.indexOf(target);
					if (client.getVarbitValue(VarbitID.BANK_INSERTMODE) == 1)
					{
						section.remove(from);
						section.add(to, row);
					}
					else
						java.util.Collections.swap(section, from, to);
					positionRows(section, top, height);
					apply(parent);
					List<Integer> order = new ArrayList<>();
					for (Row placed : section)
						order.add(stocked.get(placed.index).enumId);
					views.settings.setOrder(entry.getKey(), order);
				});
			}
		}
		apply(parent);
	}

	private void positionRows(List<Row> section, int top, int height)
	{
		for (int i = 0; i < section.size(); i++)
		{
			Row row = section.get(i);
			row.column = i % 2;
			row.lastX = row.column == 0 ? 0 : (parent.getWidth() + 1) / 2;
			row.parts[0].setOriginalX(row.lastX);
			row.parts[0].setOriginalY(top + i / 2 * height);
		}
	}

	private static void place(Widget widget, int x, int y, int width)
	{
		if (widget.getOriginalX() == x && widget.getOriginalY() == y && widget.getOriginalWidth() == width)
			return;
		widget.setOriginalX(x);
		widget.setOriginalY(y);
		widget.setOriginalWidth(width);
		widget.revalidate();
	}

	private static final class Row
	{
		final int index, heartInset;
		final Widget[] parts;
		final int[] dx = new int[5], dy = new int[5];
		int column, lastX;

		Row(int index, Widget[] parts, int column)
		{
			this.index = index;
			this.parts = parts;
			this.column = column;
			lastX = parts[0].getOriginalX();
			for (int i = 0; i < 5; i++)
			{
				dx[i] = parts[i].getOriginalX() - lastX;
				dy[i] = parts[i].getOriginalY() - parts[0].getOriginalY();
			}
			heartInset = parts[0].getOriginalWidth() - dx[4];
		}
	}

	private static final class Extra
	{
		final Widget widget, anchor;
		final int x, y;
		final boolean widthFollows;

		Extra(Widget widget, Widget anchor, int x, int y, boolean widthFollows)
		{
			this.widget = widget;
			this.anchor = anchor;
			this.x = x;
			this.y = y;
			this.widthFollows = widthFollows;
		}
	}
}
