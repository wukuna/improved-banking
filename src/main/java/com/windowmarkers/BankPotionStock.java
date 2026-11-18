package com.windowmarkers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.EnumComposition;
import net.runelite.api.ScriptID;
import net.runelite.api.EnumID;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.widgets.Widget;

@Singleton
final class BankPotionStock
{
	static final class Potion
	{
		final int enumId;
		final int index;
		final int itemId;
		final int quantity;
		final boolean unfinished;
		final List<Integer> variants;

		Potion(int enumId, int index, int itemId, int quantity, boolean unfinished, List<Integer> variants)
		{
			this.enumId = enumId;
			this.index = index;
			this.itemId = itemId;
			this.quantity = quantity;
			this.unfinished = unfinished;
			this.variants = Collections.unmodifiableList(variants);
		}
	}

	@Inject
	Client client;
	private List<Potion> potions = Collections.emptyList();
	private final Set<Integer> triggers = new HashSet<>();
	private boolean dirty = true;

	List<Potion> potions()
	{
		return potions;
	}

	void clear()
	{
		potions = Collections.emptyList();
		triggers.clear();
		dirty = true;
	}

	void changed(int varp)
	{
		if (triggers.contains(varp) || varp == VarPlayerID.POTIONSTORE_BASE_VAR_1
				|| varp == VarPlayerID.POTIONSTORE_BASE_VAR_2 || varp == VarPlayerID.POTIONSTORE_BASE_VAR_3
				|| varp == VarPlayerID.POTIONSTORE_BASE_VAR_4 || varp == VarPlayerID.POTIONSTORE_BASE_VAR_5
				|| varp == VarPlayerID.POTIONSTORE_BASE_VAR_6)
		{
			dirty = true;
		}
	}

	boolean refresh()
	{
		if (!dirty)
		{
			return false;
		}
		Widget items = client.getWidget(InterfaceID.Bankmain.POTIONSTORE_ITEMS);
		if (items == null)
		{
			return false;
		}
		EnumComposition finished = client.getEnum(EnumID.POTIONSTORE_POTIONS);
		EnumComposition unfinished = client.getEnum(EnumID.POTIONSTORE_UNFINISHED_POTIONS);
		if (finished == null || unfinished == null)
		{
			return false;
		}
		List<Potion> next = new ArrayList<>();
		int index = 0;
		for (EnumComposition list : new EnumComposition[] { finished, unfinished })
		{
			for (int enumId : list.getIntVals())
			{
				EnumComposition definition = client.getEnum(enumId);
				int row = index++;
				if (definition == null)
				{
					continue;
				}
				client.runScript(ScriptID.POTIONSTORE_DOSES, enumId);
				int doses = client.getIntStack()[0];
				client.runScript(ScriptID.POTIONSTORE_WITHDRAW_DOSES, enumId);
				int withdraw = client.getIntStack()[0];
				if (withdraw <= 0 || doses < withdraw)
				{
					continue;
				}
				int itemId = definition.getIntValue(withdraw);
				if (itemId <= 0)
				{
					continue;
				}
				List<Integer> variants = new ArrayList<>();
				for (int dose = 1; dose <= 4; dose++)
				{
					int variant = definition.getIntValue(dose);
					if (variant > 0)
					{
						variants.add(variant);
					}
				}
				next.add(new Potion(enumId, row, itemId, doses / withdraw, list == unfinished, variants));
			}
		}
		int[] vars = items.getVarTransmitTrigger();
		if (vars != null)
		{
			for (int varp : vars)
			{
				triggers.add(varp);
			}
		}
		dirty = false;
		boolean changed = next.size() != potions.size();
		for (int i = 0; !changed && i < next.size(); i++)
		{
			Potion before = potions.get(i);
			Potion after = next.get(i);
			changed = before.enumId != after.enumId || before.itemId != after.itemId
					|| before.quantity != after.quantity;
		}
		potions = Collections.unmodifiableList(next);
		return changed;
	}
}
