/*
 * Copyright (c) 2017, Tyler <https://github.com/tylerthardy>
 * Copyright (c) 2018, Shaun Dreclin <shaundreclin@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.windowmarkers;

import java.util.*;
import net.runelite.api.*;
import net.runelite.api.gameval.DBTableID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.game.ItemManager;

final class SetupPlayerState
{
	private static final java.util.List<String[]> TASKS = ItemTaxonomy.readRows("setup-slayer-targets.tsv", 2);
	private final GeRequirements requirements;
	private final Map<Integer, Integer> bank, carried, worn;
	private final String task;
	private final Map<Integer, ItemCatalogueIndex.Item> held = new HashMap<>();
	private final Map<String, String> additionalLocks = new HashMap<>();

	SetupPlayerState(GeRequirements requirements, Map<Integer, Integer> bank, Map<Integer, Integer> carried,
			Map<Integer, Integer> worn, String task)
	{
		this.requirements = requirements;
		this.bank = new HashMap<>(bank);
		this.carried = new HashMap<>(carried);
		this.worn = new HashMap<>(worn);
		this.task = task;
	}

	static SetupPlayerState unknown()
	{
		return new SetupPlayerState(null, Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), "");
	}

	static SetupPlayerState capture(Client client, ItemManager manager, List<BankPotionStock.Potion> potions,
			java.util.function.IntFunction<AutoCategories.Item> facts)
	{
		if (client.getGameState() != GameState.LOGGED_IN || client.getRealSkillLevel(Skill.HITPOINTS) < 1)
			return unknown();
		Map<Integer, Integer> bank = counts(client, manager, InventoryID.BANK);
		for (BankPotionStock.Potion potion : potions)
			bank.merge(manager.canonicalize(potion.itemId), potion.quantity, SetupPlayerState::sum);
		GeRequirements requirements = GeRequirements.capture(client,
				Quest.DESERT_TREASURE_II__THE_FALLEN_EMPIRE.getState(client) == QuestState.FINISHED);
		SetupPlayerState result = new SetupPlayerState(requirements, bank, counts(client, manager, InventoryID.INV),
				counts(client, manager, InventoryID.WORN), task(client));
		Set<Integer> ids = new HashSet<>(result.carried.keySet());
		ids.addAll(result.worn.keySet());
		for (int id : ids)
		{
			AutoCategories.Item fact = facts.apply(id);
			result.held.put(id, new ItemCatalogueIndex.Item(fact, fact.name, fact.slot));
		}
		Map<Quest, Boolean> quests = new EnumMap<>(Quest.class);
		for (String[] rule : ItemTaxonomy.readRows("setup-quest-requirements.tsv", 2))
		{
			Quest quest = Quest.valueOf(rule[1]);
			if (!quests.computeIfAbsent(quest, q -> q.getState(client) == QuestState.FINISHED))
				result.additionalLocks.put(rule[0], quest.getName());
		}
		for (String[] rule : ItemTaxonomy.readRows("setup-other-skill-requirements.tsv", 3))
		{
			Skill skill = Skill.valueOf(rule[1]);
			if (client.getRealSkillLevel(skill) < Integer.parseInt(rule[2]))
				result.additionalLocks.merge(rule[0], rule[2] + " " + skill.getName(), (a, b) -> a + ", " + b);
		}
		return result;
	}

	private static int sum(int a, int b)
	{
		return (int) Math.min(Integer.MAX_VALUE, (long) a + b);
	}

	private static Map<Integer, Integer> counts(Client client, ItemManager manager, int containerId)
	{
		Map<Integer, Integer> result = new HashMap<>();
		ItemContainer container = client.getItemContainer(containerId);
		if (container != null)
			for (Item item : container.getItems())
				if (item.getId() >= 0 && item.getQuantity() > 0)
					result.merge(manager.canonicalize(item.getId()), item.getQuantity(), SetupPlayerState::sum);
		return result;
	}

	private static String task(Client client)
	{
		if (client.getVarpValue(VarPlayerID.SLAYER_COUNT) <= 0)
			return "";
		int id = client.getVarpValue(VarPlayerID.SLAYER_TARGET);
		List<Integer> rows = id == 98
				? client.getDBRowsByValue(DBTableID.SlayerTaskSublist.ID, DBTableID.SlayerTaskSublist.COL_TASK_SUBTABLE_ID,
						0, client.getVarbitValue(VarbitID.SLAYER_TARGET_BOSSID))
				: client.getDBRowsByValue(DBTableID.SlayerTask.ID, DBTableID.SlayerTask.COL_ID, 0, id);
		if (rows == null || rows.isEmpty())
			return "";
		int row = id == 98 ? (Integer) client.getDBTableField(rows.get(0), DBTableID.SlayerTaskSublist.COL_TASK, 0)[0]
				: rows.get(0);
		return (String) client.getDBTableField(row, DBTableID.SlayerTask.COL_NAME_UPPERCASE, 0)[0];
	}

	boolean onTask(String activity)
	{
		String current = SearchText.normalize(task).replaceFirst("^the ", "");
		String target = SearchText.normalize(activity).replaceFirst("^slayer task ", "");
		if (current.isEmpty())
			return false;
		if (current.equals(target) || current.equals(target + "s"))
			return true;
		for (String[] row : TASKS)
			if (row[0].equals(activity) && Arrays.asList(row[1].split(",")).contains(current))
				return true;
		return false;
	}

	Map<Integer, ItemCatalogueIndex.Item> usable(Map<Integer, ItemCatalogueIndex.Item> items)
	{
		if (requirements == null)
			return items;
		Map<Integer, ItemCatalogueIndex.Item> result = new HashMap<>();
		Map<Integer, ItemCatalogueIndex.Item> available = new HashMap<>(held);
		available.putAll(items);
		available.forEach((id, item) ->
		{
			if (locked(item).isEmpty())
				result.put(id, item);
		});
		return result;
	}

	String locked(ItemCatalogueIndex.Item item)
	{
		if (requirements == null || item.slot < 0)
			return "";
		String levels = requirements.locked(item);
		for (Map.Entry<String, String> rule : additionalLocks.entrySet())
			if (CombatEquivalence.matches(item.fact, rule.getKey()))
				return levels.isEmpty() ? rule.getValue() : levels + ", " + rule.getValue();
		return levels;
	}

	Set<Integer> visible(Set<Integer> bankItems)
	{
		Set<Integer> result = new HashSet<>(bankItems);
		result.addAll(held.keySet());
		return result;
	}

	void annotate(SetupLoadout plan)
	{
		if (plan == null || requirements == null)
			return;
		for (SetupLoadout section : plan.sections())
		{
			section.stockKnown = true;
			for (Map.Entry<Integer, List<Integer>> entry : section.equipment.entrySet())
			{
				int id = entry.getValue().get(0);
				if (!bank.containsKey(id) && held.containsKey(id))
				{
					section.heldEquipment.add(entry.getKey());
					section.equipmentExample.put(entry.getKey(), id);
					section.equipmentNotes.put(id, (worn.containsKey(id) ? "Already equipped." : "Already carried.")
							+ " This preview has no bank action.");
				}
			}
			Map<Integer, Integer> used = new HashMap<>();
			for (Map.Entry<Integer, Integer> entry : section.inventory.entrySet())
			{
				int id = entry.getValue(), position = entry.getKey();
				if (used.getOrDefault(id, 0) >= bank.getOrDefault(id, 0) && InventoryPotionDoses.known(id))
					for (int alternative : InventoryPotionDoses.alternatives(Collections.singletonList(id)))
						if (used.getOrDefault(alternative, 0) < bank.getOrDefault(alternative, 0))
						{
							id = alternative;
							entry.setValue(id);
							break;
						}
				int required = used.merge(id, 1, Integer::sum);
				int available = bank.getOrDefault(id, 0);
				String note = "Bank / storage: " + available + ". Carried: " + carried.getOrDefault(id, 0)
						+ ". Equipped: " + worn.getOrDefault(id, 0) + ".";
				if (required > available)
				{
					section.unavailableInventory.add(position);
					note += " No additional bank item for this example slot.";
				}
				if (InventoryPotionDoses.known(id))
				{
					long doses = 0;
					int wanted = 0;
					List<Integer> variants = InventoryPotionDoses.alternatives(Collections.singletonList(id));
					for (int variant : variants)
						doses += (long) InventoryPotionDoses.doses(variant) * bank.getOrDefault(variant, 0);
					for (SetupLoadout.Field field : section.inventoryExample.values())
						for (int example : field.ids)
							if (variants.contains(example))
							{
								wanted += InventoryPotionDoses.doses(example);
								break;
							}
					note += " Withdrawable bank/storage doses of this potion: " + doses + "; example doses: " + wanted + ".";
				}
				section.inventoryNotes.put(position, note);
			}
			for (Map.Entry<Integer, SetupLoadout.Field> entry : section.inventoryExample.entrySet())
				if (!section.inventory.containsKey(entry.getKey()))
					for (int id : entry.getValue().ids)
						if (carried.containsKey(id) || worn.containsKey(id))
						{
							section.inventoryNotes.put(entry.getKey(), "Already carried: " + carried.getOrDefault(id, 0)
									+ "; equipped: " + worn.getOrDefault(id, 0) + ". This preview has no bank action.");
							break;
						}
		}
	}
}
