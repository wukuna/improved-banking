package com.windowmarkers;

import java.util.*;
import java.util.stream.Collectors;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class SetupAuditRecommendationsTest
{
	private ItemCatalogueIndex.Item item(int id, String name, int slot)
	{
		return GeSearchIndexTest.item(id, name, slot);
	}

	private SetupLoadout plan(String id, ItemCatalogueIndex.Item... items)
	{
		Map<Integer, ItemCatalogueIndex.Item> bank = new LinkedHashMap<>();
		for (var item : items)
			bank.put(item.id, item);
		return SetupLoadout.build(Collections.singletonList(ActivitySetups.byId(id)), bank, bank.keySet(), "");
	}

	@Test
	public void ownedLowerMagicHeadCompletesSireWithoutInventingGuideRank()
	{
		var result = new ItemCatalogueIndex(Collections.singletonList(item(6918, "Infinity hat", 0)), false)
				.find("sire magic", "74025:7");
		assertEquals(Collections.singletonList(6918), result.loadout.equipment.get(0));
		assertEquals(0, result.ranks.get(6918).tier);
		assertTrue(result.loadout.equipmentNotes.get(6918).contains("fallback"));
	}

	@Test
	public void toaDoesNotAddAnUnlistedHeadSwitch()
	{
		assertFalse(plan("364045:3", item(6918, "Infinity hat", 0)).equipment.containsKey(0));
	}

	@Test
	public void protectedSlotsAndUtilityActivitiesDoNotReceiveGenericCombatGear()
	{
		assertFalse(SetupActivityRules.fallbackSlot("Kraken", 0));
		assertFalse(SetupActivityRules.fallbackSlot("Basilisk Knight", 5));
		assertFalse(SetupActivityRules.fallbackSlot("Skeletal Wyvern", 5));
		assertFalse(SetupActivityRules.fallbackSlot("Lizardman shaman", 4));
		assertFalse(SetupActivityRules.fallbackSlot("Alchemical Hydra", 10));
		assertFalse(SetupActivityRules.fallbackSlot("Guardians of the Rift", 0));
		assertFalse(SetupActivityRules.fallbackSlot("The Gauntlet", 3));
		assertEquals("melee defence", SetupActivityRules.purpose("Moons of Peril", "melee"));
	}

	@Test
	public void everyActivityHasAnExplicitFallbackPolicy()
	{
		Set<String> rules = ItemTaxonomy.readRows("setup-activity-rules.tsv", 4).stream().map(r -> r[0])
				.collect(Collectors.toSet());
		for (var setup : ActivitySetups.all())
			assertTrue(setup.activity, rules.contains(setup.activity));
	}

	@Test
	public void mixedCombatStylesAndDifferentMethodsStaySeparate()
	{
		assertEquals("", SetupCompletion.style("Magic and ranged"));
		assertEquals("", SetupCompletion.style("Melee and magic"));
		var gear = new SetupLoadout.Table(new String[] { "x", "equipment", "Solo / Quick pool", "Melee" });
		var inventory = new SetupLoadout.Table(new String[] { "y", "inventory", "Team melee", "Melee" });
		assertFalse(SetupActivityRules.inventoryCompatible("Corporeal Beast", gear, inventory));
	}

	@Test
	public void newKrakenBranchHasItsOwnInventoryAndKeepsOriginalIdentity()
	{
		var setups = ActivitySetups.forActivity("Kraken");
		assertEquals(6, setups.size());
		var afk = setups.stream().filter(s -> s.label.contains("AFK") && s.label.contains("Equipment")).findFirst().get();
		assertTrue(plan(afk.id).inventoryName.contains("AFK"));
		assertFalse(plan(setups.get(0).id).inventoryName.contains("AFK"));
	}

	@Test
	public void taskMatchingDoesNotMistakeAnyAssignmentForTheSelectedBoss()
	{
		var state = new SetupPlayerState(null, Map.of(), Map.of(), Map.of(), "Abyssal demons");
		assertTrue(state.onTask("Abyssal Sire"));
		assertFalse(state.onTask("Kraken"));
		var helmet = item(11865, "Slayer helmet (i)", 0);
		assertTrue(SetupEquipmentChoice.allowed(helmet, "Only on a Slayer task", null, List.of(), true));
		assertFalse(SetupEquipmentChoice.allowed(helmet, "Only on a Slayer task", null, List.of(), false));
		assertFalse(SetupEquipmentChoice.allowed(helmet, "on task", null, List.of(), false));
		assertTrue(SetupEquipmentChoice.allowed(helmet, "on task", null, List.of(), true));
		assertFalse(SetupEquipmentChoice.allowed(helmet, "Slayer task, after completing the diary", null, List.of(), true));
	}

	@Test
	public void refreshedFightCaveHelmetRetainsItsInlineTaskCondition()
	{
		var helmet = item(11865, "Slayer helmet (i)", 0);
		var result = plan("28467:1", helmet);
		assertFalse(result.equipment.getOrDefault(0, List.of()).contains(helmet.id));
		assertTrue(ItemTaxonomy.readRows("setup-layout-options.tsv", 5).stream()
				.anyMatch(row -> row[0].equals("28467:1") && row[1].equals("head1")
						&& row[2].equals("11865") && row[4].contains("on task")));
	}

	@Test
	public void lowLevelPlayersDoNotReceiveUnwearableOwnedFallbacks()
	{
		var levels = new GeRequirements(new int[] { 1, 1, 10, 1, 20, 1, 10 }, false, false);
		var state = new SetupPlayerState(levels, Map.of(), Map.of(), Map.of(), "");
		var hat = item(6918, "Infinity hat", 0);
		var xerician = item(13385, "Xerician hat", 0);
		var usable = state.usable(Map.of(hat.id, hat, xerician.id, xerician));
		assertFalse(usable.containsKey(hat.id));
		assertTrue(usable.containsKey(xerician.id));
		assertTrue(state.locked(hat).contains("50 Magic"));
	}

	@Test
	public void repeatedPotionSlotsDoNotInventBankStock()
	{
		var potion = item(3024, "Super restore(4)", -1);
		var plan = plan("364045:3", potion);
		assertTrue(plan.inventory.values().stream().filter(id -> id == 3024).count() > 1);
		var state = new SetupPlayerState(new GeRequirements(new int[] {99,99,99,99,99,99,99}, true, true),
				Map.of(3024, 1), Map.of(3024, 2), Map.of(), "");
		state.annotate(plan);
		long positions = plan.inventory.values().stream().filter(id -> id == 3024).count();
		assertEquals(positions - 1, plan.unavailableInventory.size());
		assertTrue(plan.inventoryNotes.values().stream().anyMatch(n -> n.contains("Carried: 2") && n.contains("doses of this potion: 4")));
	}

	@Test
	public void missingInventoryPreviewRecognizesCarriedSuppliesWithoutBankActions()
	{
		var plan = plan("538484:1");
		var state = new SetupPlayerState(new GeRequirements(new int[] {99,99,99,99,99,99,99}, true, true),
				Map.of(), Map.of(8013, 1), Map.of(), "");
		state.annotate(plan);
		assertTrue(plan.inventoryNotes.get(27).contains("Already carried"));
		assertFalse(plan.inventory.containsKey(27));
	}

	@Test
	public void twoAdditionalWildernessActivitiesUseTheBudgetPolicy()
	{
		assertTrue(WildernessLoadout.applies(ActivitySetups.forActivity("Crazy archaeologist")));
		assertTrue(WildernessLoadout.applies(ActivitySetups.forActivity("Lava dragon")));
	}

	@Test
	public void questShieldIsNotWearableAndSlayerRequirementsAreChecked()
	{
		assertTrue(ItemTaxonomy.readRows("setup-layout-options.tsv", 5).stream()
				.noneMatch(row -> row[2].equals("24265")));
		var client = mock(net.runelite.api.Client.class);
		when(client.getGameState()).thenReturn(net.runelite.api.GameState.LOGGED_IN);
		when(client.getRealSkillLevel(any(net.runelite.api.Skill.class))).thenReturn(99);
		when(client.getRealSkillLevel(net.runelite.api.Skill.SLAYER)).thenReturn(1);
		when(client.getIntStack()).thenReturn(new int[] { 2 });
		var state = SetupPlayerState.capture(client, mock(net.runelite.client.game.ItemManager.class), List.of(), id -> null);
		assertEquals("60 Slayer", state.locked(item(24266, "V's shield", 5)));
		assertEquals("25 Slayer", state.locked(item(4156, "Mirror shield", 5)));
	}

	@Test
	public void equippedGearIsRecognizedWithoutBecomingWithdrawableStock()
	{
		var client = mock(net.runelite.api.Client.class);
		var manager = mock(net.runelite.client.game.ItemManager.class);
		when(client.getGameState()).thenReturn(net.runelite.api.GameState.LOGGED_IN);
		when(client.getRealSkillLevel(any(net.runelite.api.Skill.class))).thenReturn(99);
		when(client.getIntStack()).thenReturn(new int[] { 2 });
		var equipment = mock(net.runelite.api.ItemContainer.class);
		when(equipment.getItems()).thenReturn(new net.runelite.api.Item[] { new net.runelite.api.Item(6918, 1) });
		when(client.getItemContainer(net.runelite.api.gameval.InventoryID.WORN)).thenReturn(equipment);
		when(manager.canonicalize(6918)).thenReturn(6918);
		var hat = item(6918, "Infinity hat", 0);
		var state = SetupPlayerState.capture(client, manager, List.of(), id -> hat.fact);
		var result = new ItemCatalogueIndex(List.of(), false, state).find("sire magic", "74025:7");
		assertNotNull(result.loadout);
		assertEquals(Integer.valueOf(6918), result.loadout.equipmentExample.get(0));
		assertTrue(result.loadout.heldEquipment.contains(0));
		assertTrue(result.loadout.equipmentNotes.get(6918).contains("Already equipped"));
		assertTrue(result.items.isEmpty());
	}
}
