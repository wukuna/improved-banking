package com.windowmarkers;

import java.util.*;

final class SetupEquipmentExamples
{
	static void fill(SetupLoadout plan, Map<Integer, ItemCatalogueIndex.Item> owned, SetupPlayerState state)
	{
		if (plan == null)
			return;
		for (SetupLoadout section : plan.sections())
		{
			SetupLoadout.Table table = section.equipmentTable();
			if (table == null)
				continue;
			List<SetupLoadout.Field> fields = new ArrayList<>(table.fields);
			fields.sort(Comparator.comparingInt((SetupLoadout.Field f) -> f.slot() == 3 ? 0 : 1)
					.thenComparingInt(SetupLoadout.Field::order));
			Set<Integer> slots = new LinkedHashSet<>();
			for (SetupLoadout.Field field : fields)
				if (field.slot() >= 0)
					slots.add(field.slot());
			List<ItemCatalogueIndex.Item> wearing = new ArrayList<>();
			for (List<Integer> ids : section.equipment.values())
				if (!ids.isEmpty())
					wearing.add(owned.get(ids.get(0)));
			ItemCatalogueIndex.Item weapon = section.equipment.containsKey(3)
					? owned.get(section.equipment.get(3).get(0)) : null;
			for (int slot : slots)
			{
				if (section.equipment.containsKey(slot))
					continue;
				ItemCatalogueIndex.Item example = choose(section, fields, slot, weapon, wearing, state);
				if (example != null)
				{
					section.equipmentExample.put(slot, example.id);
					wearing.add(example);
					if (slot == 3)
						weapon = example;
				}
			}
		}
	}

	private static ItemCatalogueIndex.Item choose(SetupLoadout section, List<SetupLoadout.Field> fields, int slot,
			ItemCatalogueIndex.Item weapon, List<ItemCatalogueIndex.Item> wearing, SetupPlayerState state)
	{
		ActivitySetups.Setup setup = ActivitySetups.byId(section.equipmentSource);
		for (boolean checkConditions : new boolean[] { true, false })
			for (SetupLoadout.Field field : fields)
				if (field.slot() == slot)
					for (int id : field.ids)
					{
						ItemCatalogueIndex.Item item = ItemCatalogueIndex.guideItem(id);
						if (item == null || item.slot != slot || !ItemVariantState.canPreviewEquipment(item.fact)
								|| !SetupItemRequirements.reviewReason(setup.id, item.fact).isEmpty()
								|| ItemVariantState.needsRankReview(item.fact, setup.activity, setup.label))
							continue;
						String note = field.itemNotes.getOrDefault(id, field.note);
						if (!SetupEquipmentChoice.allowed(item, checkConditions ? note : "", weapon, wearing,
								state.onTask(section.activity)))
							continue;
						String locked = state.locked(item);
						section.equipmentNotes.put(id,
								("Wiki equipment suggestion. No suitable owned item selected; preview only. "
										+ ItemVariantState.annotate(note, item.fact)
										+ (locked.isEmpty() ? "" : " Requires: " + locked + ".")).trim());
						return item;
					}
		return null;
	}

	private SetupEquipmentExamples()
	{
	}
}
