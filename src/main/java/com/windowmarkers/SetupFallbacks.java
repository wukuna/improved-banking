package com.windowmarkers;

import java.util.*;

final class SetupFallbacks
{
	private static final List<String[]> ALTERNATIVES = ItemTaxonomy.readRows("setup-fallbacks.tsv", 3);

	static void fill(Map<Integer, List<Integer>> gear, ActivitySetups.Setup setup, SetupLoadout.Table table,
			Map<Integer, ItemCatalogueIndex.Item> owned, Collection<ItemCatalogueIndex.Item> wearing,
			Map<Integer, String> notes)
	{
		String style = SetupCompletion.style(table.style);
		if (style.isEmpty())
			return;
		String purpose = SetupActivityRules.purpose(setup.activity, style);
		Set<Integer> missing = new HashSet<>();
		for (SetupLoadout.Field field : table.fields)
			if (!field.ids.isEmpty() && !gear.containsKey(field.slot())
					&& SetupActivityRules.fallbackSlot(setup.activity, field.slot()))
				missing.add(field.slot());
		ItemCatalogueIndex.Item weapon = gear.containsKey(3) ? owned.get(gear.get(3).get(0)) : null;
		for (String[] row : ALTERNATIVES)
		{
			int slot = Integer.parseInt(row[1]);
			if (!missing.contains(slot) || !row[0].equals(purpose) && !row[0].equals("all"))
				continue;
			owned.values().stream().filter(item -> item.slot == slot && CombatEquivalence.matches(item.fact, row[2]))
					.sorted(Comparator.comparingInt(item -> item.id)).forEach(item ->
					{
						if (!SetupLoadout.equipmentReady(setup, item)
								|| !SetupEquipmentChoice.allowed(item, "", weapon, wearing))
							return;
						List<Integer> choices = gear.computeIfAbsent(slot, key -> new ArrayList<>());
						if (!choices.contains(item.id))
						{
							choices.add(item.id);
							notes.put(item.id, "Owned " + purpose
									+ " fallback for a missing guide slot; no Wiki tier assigned. Check method requirements.");
						}
					});
		}
	}

	private SetupFallbacks()
	{
	}
}
