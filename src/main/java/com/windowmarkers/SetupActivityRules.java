package com.windowmarkers;

import java.util.*;

final class SetupActivityRules
{
	private static final Map<String, String[]> RULES = new HashMap<>();
	static
	{
		for (String[] row : ItemTaxonomy.readRows("setup-activity-rules.tsv", 4))
			RULES.put(row[0], row);
	}

	static boolean fallbackSlot(String activity, int slot)
	{
		String[] rule = RULES.get(activity);
		return rule != null && !rule[1].equals("guide") && slot != 3 && slot != 13
				&& !Arrays.asList(rule[2].split(",")).contains(Integer.toString(slot));
	}

	static String purpose(String activity, String style)
	{
		String[] rule = RULES.get(activity);
		return rule != null && rule[1].equals("defence") ? style + " defence" : style;
	}

	static String note(String activity)
	{
		String[] rule = RULES.get(activity);
		return rule == null ? "Check this method's equipment and supply requirements." : rule[3];
	}

	static boolean sameMethod(SetupLoadout.Table a, SetupLoadout.Table b)
	{
		return a.context.equalsIgnoreCase(b.context) && a.style.equalsIgnoreCase(b.style);
	}

	static boolean inventoryCompatible(String activity, SetupLoadout.Table gear, SetupLoadout.Table inventory)
	{
		if (activity.equals("Abyssal Sire"))
			return true;
		String a = method(gear.context), b = method(inventory.context);
		return a.isEmpty() || b.isEmpty() || a.equals(b);
	}

	private static String method(String context)
	{
		return context.toLowerCase(Locale.ROOT).replaceAll("\\b(?:equipment|inventory|setup|setups|recommended)\\b", "")
				.replaceAll("[ /]+", " ").trim();
	}

	private SetupActivityRules()
	{
	}
}
