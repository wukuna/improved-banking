package com.windowmarkers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

final class BankPotionItemOrder
{
	static List<Integer> read(BankPotionSettings settings, int tab, List<Integer> nativeOrder)
	{
		String value = settings.configs.getRSProfileConfiguration("windowmarkers", key(tab));
		if (value == null || value.length() > 64000)
			return nativeOrder;
		try
		{
			String[] parts = value.split("\\|", -1);
			if (parts.length != 2)
				return nativeOrder;
			List<Integer> previous = parse(parts[0]), saved = parse(parts[1]);
			Set<Integer> liveIds = new HashSet<>(nativeOrder), previousIds = new HashSet<>(previous);
			List<Integer> surviving = previous.stream().filter(liveIds::contains).collect(Collectors.toList());
			List<Integer> current = nativeOrder.stream().filter(previousIds::contains).collect(Collectors.toList());
			if (!surviving.equals(current))
				return nativeOrder;
			List<Integer> result = saved.stream().filter(liveIds::contains).collect(Collectors.toList());
			Set<Integer> included = new HashSet<>(result);
			for (int i = nativeOrder.size() - 1; i >= 0; i--)
				if (included.add(nativeOrder.get(i)))
				{
					int before = i + 1 < nativeOrder.size() ? result.indexOf(nativeOrder.get(i + 1)) : result.size();
					result.add(before, nativeOrder.get(i));
				}
			return result;
		}
		catch (IllegalArgumentException ignored)
		{
			return nativeOrder;
		}
	}

	static void save(BankPotionSettings settings, int tab, List<Integer> nativeOrder, List<Integer> displayOrder)
	{
		settings.configs.setRSProfileConfiguration("windowmarkers", key(tab), join(nativeOrder) + "|" + join(displayOrder));
	}

	private static String key(int tab)
	{
		return BankPotionSettings.PREFIX + "bankItems." + tab;
	}

	private static List<Integer> parse(String value)
	{
		if (value.isEmpty())
			return new ArrayList<>();
		List<Integer> result = Arrays.stream(value.split(",")).map(Integer::parseInt).collect(Collectors.toList());
		if (result.size() > 2000 || result.stream().anyMatch(id -> id < 0)
				|| result.stream().distinct().count() != result.size())
			throw new IllegalArgumentException();
		return result;
	}

	private static String join(List<Integer> values)
	{
		return values.stream().map(String::valueOf).collect(Collectors.joining(","));
	}
}
