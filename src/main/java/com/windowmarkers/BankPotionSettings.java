package com.windowmarkers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.config.ConfigManager;

@Singleton
final class BankPotionSettings
{
	static final String PREFIX = "storedPotion.";
	@Inject
	ConfigManager configs;

	int bankTab(int potion)
	{
		String value = configs.getRSProfileConfiguration("windowmarkers", PREFIX + potion + ".tab");
		try
		{
			int tab = Integer.parseInt(value);
			return tab >= 0 && tab <= 9 ? tab : -1;
		}
		catch (NumberFormatException ignored)
		{
			return -1;
		}
	}

	boolean search(int potion)
	{
		return Boolean.parseBoolean(configs.getRSProfileConfiguration("windowmarkers", PREFIX + potion + ".search"));
	}

	int bankPosition(int potion)
	{
		String value = configs.getRSProfileConfiguration("windowmarkers", PREFIX + potion + ".position");
		try
		{
			int position = Integer.parseInt(value);
			return position >= 0 && position <= 4000 ? position : -1;
		}
		catch (NumberFormatException ignored)
		{
			return -1;
		}
	}

	void setBankPosition(int potion, int position)
	{
		configs.setRSProfileConfiguration("windowmarkers", PREFIX + potion + ".position", position);
	}

	void setBankTab(int potion, int tab)
	{
		configs.setRSProfileConfiguration("windowmarkers", PREFIX + potion + ".tab", Math.max(-1, Math.min(9, tab)));
	}

	void setSearch(int potion, boolean shown)
	{
		configs.setRSProfileConfiguration("windowmarkers", PREFIX + potion + ".search", shown);
	}

	List<Integer> order(int section)
	{
		String value = configs.getRSProfileConfiguration("windowmarkers", PREFIX + "order." + section);
		if (value == null || value.isEmpty() || value.length() > 4096)
		{
			return Collections.emptyList();
		}
		List<Integer> result = new ArrayList<>();
		for (String part : value.split(","))
		{
			try
			{
				int id = Integer.parseInt(part);
				if (id > 0 && !result.contains(id))
				{
					result.add(id);
				}
			}
			catch (NumberFormatException ignored)
			{
				return Collections.emptyList();
			}
		}
		return result;
	}

	void setOrder(int section, List<Integer> order)
	{
		List<String> values = new ArrayList<>();
		for (int id : order)
		{
			values.add(Integer.toString(id));
		}
		configs.setRSProfileConfiguration("windowmarkers", PREFIX + "order." + section, String.join(",", values));
	}
}
