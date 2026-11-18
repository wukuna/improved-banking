package com.windowmarkers;

import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.Point;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.JavaScriptCallback;
import net.runelite.api.widgets.Widget;

final class BankViewGeometry
{
	private static final String RETURN_POSITION = "Return to preset position";
	private Widget parent;
	private final List<Position> positions = new ArrayList<>();
	private BankPresetPositions.Layout layout;
	private Runnable save;
	private List<SetupItemLayout.InventoryNote> notes;
	private int height;
	private long dragRevision;

	void cancelDrag()
	{
		dragRevision++;
	}

	void clear()
	{
		cancelDrag();
		parent = null;
		positions.clear();
		layout = null;
		save = null;
		notes = null;
		height = 0;
	}

	void capture(Widget container)
	{
		clear();
		parent = container;
		Widget[] children = container.getDynamicChildren();
		if (children == null)
			return;
		for (Widget child : children)
			if (child != null && !child.isSelfHidden() && child.getItemId() >= 0)
				positions.add(new Position(child));
	}

	int capture(Widget container, BankPresetPositions.Layout layout, Runnable save,
			List<SetupItemLayout.InventoryNote> notes, int height)
	{
		capture(container);
		this.layout = layout;
		this.save = save;
		this.notes = notes;
		this.height = height;

		positions.sort(Comparator.comparingInt((Position p) -> p.y).thenComparingInt(p -> p.x));
		Map<Integer, Integer> occurrences = new HashMap<>();
		for (Position p : positions)
		{
			p.key = p.itemId + ":" + occurrences.merge(p.itemId, 1, Integer::sum);
			for (int i = 0; i < notes.size(); i++)
				if (notes.get(i).x == p.x && notes.get(i).y == p.y)
				{
					p.note = i;
					break;
				}
		}
		for (Position p : positions)
		{
			int[] saved = layout.cells.get(p.key);
			if (saved != null)
				move(p, saved[0], saved[1]);
		}

		List<Position> occupied = new ArrayList<>();
		List<Position> ordered = new ArrayList<>(positions);
		ordered.sort(Comparator.comparing(p -> !layout.cells.containsKey(p.key)));
		List<Position> overflow = new ArrayList<>();
		for (Position p : ordered)
		{
			if (p.x + 36 > container.getWidth() || overlaps(p, occupied))
				overflow.add(p);
			else
				occupied.add(p);
		}
		overflow.sort(Comparator.comparingInt((Position p) -> p.y).thenComparingInt(p -> p.x));
		int bottom = height;
		for (Position p : occupied)
			bottom = Math.max(bottom, p.y + 36);
		int left = Math.min(51, Math.max(0, container.getWidth() - 36));
		int columns = Math.max(1, (container.getWidth() - left - 36) / 48 + 1);
		for (int i = 0; i < overflow.size(); i++)
			move(overflow.get(i), left + i % columns * 48, bottom + 4 + i / columns * 36);
		restore(container);
		return contentHeight();
	}

	private boolean overlaps(Position p, List<Position> others)
	{
		for (Position q : others)
			if (p.x < q.x + 36 && p.x + 36 > q.x && p.y < q.y + 32 && p.y + 32 > q.y)
				return true;
		return false;
	}

	boolean drag(Client client, Widget container, java.util.function.BooleanSupplier current)
	{
		Widget dragged = client.getDraggedWidget();
		Position source = find(dragged);
		if (container != parent || source == null || !valid(source, container))
			return false;

		client.setDraggedOnWidget(null);
		long revision = dragRevision;
		dragged.setOnDragCompleteListener((JavaScriptCallback) event ->
		{
			if (revision != dragRevision || !current.getAsBoolean() || container != parent || find(dragged) != source
					|| !valid(source, container))
				return;
			drop(source, client.getMouseCanvasPosition());
		});
		return true;
	}

	void menuAdded(Client client, MenuEntry entry, java.util.function.BooleanSupplier current)
	{
		if (layout == null || parent == null || parent.isHidden() || entry.getParam1() != InterfaceID.Bankmain.ITEMS
				|| entry.getIdentifier() != 1
				|| entry.getType() != MenuAction.CC_OP && entry.getType() != MenuAction.CC_OP_LOW_PRIORITY)
			return;
		Position source = find(entry.getWidget());
		if (source == null || !valid(source, parent) || source.widget.isSelfHidden()
				|| source.homeX + 36 > parent.getWidth()
				|| source.x == source.homeX && source.y == source.homeY && !layout.cells.containsKey(source.key))
			return;
		MenuEntry[] entries = client.getMenuEntries();
		if (entries != null)
			for (MenuEntry existing : entries)
				if (existing != null && RETURN_POSITION.equals(existing.getOption())
						&& existing.getParam1() == entry.getParam1()
						&& existing.getParam0() == source.widget.getIndex())
					return;
		long revision = dragRevision;
		client.createMenuEntry(0).setOption(RETURN_POSITION).setTarget(entry.getTarget())
				.setType(MenuAction.RUNELITE_LOW_PRIORITY).setParam0(source.widget.getIndex())
				.setParam1(entry.getParam1()).onClick(clicked ->
				{
					if (revision == dragRevision && current.getAsBoolean() && parent != null && !parent.isHidden()
							&& find(source.widget) == source && valid(source, parent) && !source.widget.isSelfHidden())
						returnPosition(source);
				});
	}

	private void returnPosition(Position source)
	{
		for (Position other : positions)
		{
			if (other == source || !valid(other, parent))
				continue;
			int[] preferred = layout.cells.get(other.key);
			if (!intersects(source.homeX, source.homeY, other.x, other.y)
					&& (preferred == null || !intersects(source.homeX, source.homeY, preferred[0], preferred[1])))
				continue;
			int x = source.x, y = source.y;
			if (intersects(x, y, source.homeX, source.homeY))
			{
				x = Math.min(51, Math.max(0, parent.getWidth() - 36));
				y = contentHeight() + 4;
			}
			for (Position occupied : positions)
				if (occupied != source && occupied != other && intersects(x, y, occupied.x, occupied.y))
				{
					x = Math.min(51, Math.max(0, parent.getWidth() - 36));
					y = contentHeight() + 4;
					break;
				}
			move(other, x, y);
			if (other.x == other.homeX && other.y == other.homeY)
				layout.cells.remove(other.key);
			else
				remember(other);
		}
		move(source, source.homeX, source.homeY);
		layout.cells.remove(source.key);
		finishMove();
	}

	private static boolean intersects(int x, int y, int otherX, int otherY)
	{
		return x < otherX + 36 && x + 36 > otherX && y < otherY + 32 && y + 32 > otherY;
	}

	private void drop(Position source, Point mouse)
	{
		Point origin = parent.getCanvasLocation();
		if (origin == null || mouse == null)
			return;
		int x = mouse.getX() - origin.getX(), visibleY = mouse.getY() - origin.getY();
		if (x < 0 || x >= parent.getWidth() || visibleY < 0 || visibleY >= parent.getHeight())
			return;
		int y = visibleY + parent.getScrollY();
		Position target = null;
		for (Position p : positions)
			if (p != source && valid(p, parent) && x >= p.x && x < p.x + 36 && y >= p.y && y < p.y + 32)
			{
				target = p;
				break;
			}
		int nextX, nextY;
		if (target != null)
		{
			nextX = target.x;
			nextY = target.y;
			move(target, source.x, source.y);
			remember(target);
		}
		else
		{
			nextX = Math.max(0, Math.min(parent.getWidth() - 36, 51 + Math.round((x - 69) / 48f) * 48));
			nextY = Math.max(0, Math.min(50000, source.y + Math.round((y - source.y - 16) / 36f) * 36));

			for (Position p : positions)
				if (p != source && nextX < p.x + 36 && nextX + 36 > p.x && nextY < p.y + 32 && nextY + 32 > p.y)
					return;
		}
		move(source, nextX, nextY);
		remember(source);
		finishMove();
	}

	private void finishMove()
	{
		restore(parent);
		parent.setScrollHeight(Math.max(parent.getHeight(), contentHeight() + 8));
		parent.revalidateScroll();
		if (save != null)
			save.run();
	}

	private void remember(Position p)
	{
		if (layout != null && p.key != null && (layout.cells.containsKey(p.key) || layout.cells.size() < 4000))
			layout.cells.put(p.key, new int[] { p.x, p.y });
	}

	private void move(Position p, int x, int y)
	{
		p.x = x;
		p.y = y;
		if (notes != null && p.note >= 0)
			notes.set(p.note, new SetupItemLayout.InventoryNote(notes.get(p.note).text, x, y));
	}

	private int contentHeight()
	{
		int result = height;
		for (Position p : positions)
			result = Math.max(result, p.y + 36);
		return result;
	}

	private Position find(Widget widget)
	{
		for (Position p : positions)
			if (p.widget == widget)
				return p;
		return null;
	}

	boolean occupied(int x, int y)
	{
		for (Position p : positions)
			if (p.x == x && p.y == y)
				return true;
		return false;
	}

	List<SetupItemLayout.InventoryNote> visibleNotes(List<SetupItemLayout.InventoryNote> original)
	{
		if (notes != original)
			return original;
		List<SetupItemLayout.InventoryNote> result = new ArrayList<>();
		for (int i = 0; i < notes.size(); i++)
		{
			SetupItemLayout.InventoryNote note = notes.get(i);
			boolean covered = false;
			for (Position p : positions)
				if (p.x == note.x && p.y == note.y && p.note != i)
				{
					covered = true;
					break;
				}
			if (!covered)
				result.add(note);
		}
		return result;
	}

	private boolean valid(Position p, Widget container)
	{
		if (container == null)
			return false;
		Widget[] children = container.getDynamicChildren();
		int index = p.widget.getIndex();
		return children != null && index >= 0 && index < children.length && children[index] == p.widget
				&& p.widget.getItemId() == p.itemId;
	}

	void restore(Widget container)
	{
		if (parent != container || container == null)
			return;
		Widget[] children = container.getDynamicChildren();
		if (children == null)
			return;
		for (Position p : positions)
		{
			int index = p.widget.getIndex();
			if (index < 0 || index >= children.length || children[index] != p.widget
					|| p.widget.getItemId() != p.itemId)
				continue;

			if (p.widget.isSelfHidden() || p.widget.getOriginalX() != p.x || p.widget.getOriginalY() != p.y)
			{
				p.widget.setHidden(false);
				p.widget.setOriginalX(p.x);
				p.widget.setOriginalY(p.y);
				p.widget.revalidate();
			}
		}
	}

	private static final class Position
	{
		final Widget widget;
		final int itemId, homeX, homeY;
		int x, y, note = -1;
		String key;

		Position(Widget widget)
		{
			this.widget = widget;
			itemId = widget.getItemId();
			x = widget.getOriginalX();
			y = widget.getOriginalY();
			homeX = x;
			homeY = y;
		}
	}
}
