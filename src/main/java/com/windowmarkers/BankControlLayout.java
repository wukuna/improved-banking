package com.windowmarkers;

import java.awt.Rectangle;
import java.util.*;

final class BankControlLayout
{
	static final int DOCK_WIDTH = 48, DOCK_X = 42, TOP = 54, PITCH = 38, MIN_DOCK_WIDTH = 526;

	static final class Entry
	{
		final BankControl control;
		int position;
		boolean left;

		Entry(BankControl control, int position, boolean left)
		{
			this.control = control;
			this.position = position;
			this.left = left;
		}

		@Override
		public String toString()
		{
			return control.label + (left ? "  — left dock" : "");
		}
	}

	static final class Plan
	{
		final Map<BankControl, Rectangle> bounds = new EnumMap<>(BankControl.class);
		final Set<BankControl> docked = EnumSet.noneOf(BankControl.class);
		int inset;
	}

	final List<Entry> entries = new ArrayList<>();

	static BankControlLayout read(String value)
	{
		BankControlLayout layout = new BankControlLayout();
		Set<BankControl> seen = EnumSet.noneOf(BankControl.class);
		if (value != null && value.startsWith("v1;") && value.length() < 2048)
			for (String part : value.substring(3).split(";"))
			{
				String[] fields = part.split(":");
				if (fields.length != 3)
					continue;
				try
				{
					BankControl control = BankControl.valueOf(fields[0]);
					int position = Integer.parseInt(fields[1]);
					if ((!fields[2].equals("L") && !fields[2].equals("B")) || !seen.add(control))
						continue;
					layout.entries
							.add(new Entry(control, Math.max(0, Math.min(1000, position)), fields[2].equals("L")));
				}
				catch (IllegalArgumentException ignored)
				{
				}
			}
		for (BankControl control : BankControl.values())
			if (!seen.contains(control))
				layout.entries
						.add(new Entry(control, control.ordinal() * 1000 / (BankControl.values().length - 1), false));
		layout.sort();
		return layout;
	}

	void sort()
	{
		entries.sort(Comparator.comparingInt(e -> e.position));
	}

	String write()
	{
		StringBuilder out = new StringBuilder("v1;");
		for (Entry entry : entries)
			out.append(entry.control.name()).append(':').append(entry.position).append(':')
					.append(entry.left ? 'L' : 'B').append(';');
		return out.toString();
	}

	void move(Entry entry, int direction)
	{
		int index = entries.indexOf(entry), other = index + direction;
		if (other < 0 || other >= entries.size())
			return;
		Entry neighbour = entries.get(other);
		int position = entry.position;
		entry.position = neighbour.position;
		neighbour.position = position;
		Collections.swap(entries, index, other);
	}

	Plan plan(int width, int height, Set<BankControl> visible)
	{
		Plan plan = new Plan();
		List<Entry> row = new ArrayList<>();
		int capacity = width >= MIN_DOCK_WIDTH ? Math.max(0, (height - 38 - TOP) / PITCH) : 0;
		for (Entry entry : entries)
		{
			if (!visible.contains(entry.control))
				continue;
			if (entry.left && plan.docked.size() < capacity)
			{
				plan.bounds.put(entry.control,
						new Rectangle(DOCK_X, TOP + plan.docked.size() * PITCH, entry.control.width, 36));
				plan.docked.add(entry.control);
			}
			else
				row.add(entry);
		}
		plan.inset = plan.docked.isEmpty() ? 0 : DOCK_WIDTH;
		int remaining = row.stream().mapToInt(e -> e.control.width + 2).sum(), cursor = 2;
		for (Entry entry : row)
		{
			int size = entry.control.width;
			remaining -= size + 2;
			int desired = 2 + (int) ((long) entry.position * Math.max(0, width - 4 - size) / 1000);
			int x = Math.max(cursor, Math.min(desired, width - 2 - size - remaining));
			plan.bounds.put(entry.control, new Rectangle(x, height - 37, size, 36));
			cursor = x + size + 2;
		}
		return plan;
	}
}
