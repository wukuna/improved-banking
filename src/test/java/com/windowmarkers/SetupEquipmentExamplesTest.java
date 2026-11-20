package com.windowmarkers;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class SetupEquipmentExamplesTest
{
	private SetupLoadout plan(String id, ItemCatalogueIndex.Item... items)
	{
		Map<Integer, ItemCatalogueIndex.Item> owned = new LinkedHashMap<>();
		for (ItemCatalogueIndex.Item item : items)
			owned.put(item.id, item);
		return SetupLoadout.build(List.of(ActivitySetups.byId(id)), owned, owned.keySet(), "");
	}

	@Test
	public void emptySarachnisAndHesporiGearShowsEachRecommendedSlotWithoutInventingStock()
	{
		for (String id : List.of("224363:1", "569290:3"))
		{
			SetupLoadout result = plan(id);
			assertEquals(Set.of(0, 1, 2, 3, 4, 5, 7, 9, 10, 12, 13), result.equipmentExample.keySet());
			assertEquals(Integer.valueOf(10828), result.equipmentExample.get(0));
			assertTrue(result.equipment.isEmpty());
			assertTrue(result.itemIds().isEmpty());
			assertTrue(result.hasExamples());
			assertTrue(result.equipmentNotes.values().stream().allMatch(n -> n.contains("preview only")));
		}
		assertEquals(Integer.valueOf(28810), plan("569290:3").equipmentExample.get(3));
		assertEquals(Integer.valueOf(11840), plan("569290:3").equipmentExample.get(10));
	}

	@Test
	public void ownedMaximumGearIsNotReplacedByMissingRecommendations()
	{
		SetupLoadout result = plan("569290:6", ItemCatalogueIndex.guideItem(30750),
				ItemCatalogueIndex.guideItem(30753), ItemCatalogueIndex.guideItem(30756),
				ItemCatalogueIndex.guideItem(22325));
		for (int slot : new int[] { 0, 3, 4, 7 })
			assertFalse(result.equipmentExample.containsKey(slot));
		assertEquals(List.of(30750), result.equipment.get(0));
		assertEquals(List.of(22325), result.equipment.get(3));
	}

	@Test
	public void ownedFallbackRemainsPreferredOverAnUnownedGuideItem()
	{
		SetupLoadout result = plan("74025:7", ItemCatalogueIndex.guideItem(6918));
		assertEquals(List.of(6918), result.equipment.get(0));
		assertFalse(result.equipmentExample.containsKey(0));
		assertTrue(result.equipmentNotes.get(6918).contains("fallback"));
	}

	@Test
	public void basiliskHelmetRemainsVisibleWithItsTaskCondition()
	{
		SetupLoadout result = plan("256152:8", ItemCatalogueIndex.guideItem(11865));
		assertFalse(result.equipment.containsKey(0));
		assertEquals(Integer.valueOf(11865), result.equipmentExample.get(0));
		assertTrue(result.equipmentNotes.get(11865).contains("Slayer task"));
		assertEquals(Integer.valueOf(24266), result.equipmentExample.get(5));
	}

	@Test
	public void taskConditionsAreStillCheckedForThePreferredPreview()
	{
		SetupPlayerState state = new SetupPlayerState(null, Map.of(), Map.of(), Map.of(), "Sarachnis");
		SetupLoadout result = SetupLoadout.build(List.of(ActivitySetups.byId("224363:1")), Map.of(), Set.of(), "", state);
		assertEquals(Integer.valueOf(11865), result.equipmentExample.get(0));
		assertEquals(Integer.valueOf(10828), plan("224363:1").equipmentExample.get(0));
	}

	@Test
	public void previewOfLockedEquipmentExplainsTheRequirementWithoutMakingItOwned()
	{
		SetupPlayerState state = new SetupPlayerState(new GeRequirements(new int[] {1,1,10,1,20,1,10}, false, false),
				Map.of(), Map.of(), Map.of(), "");
		SetupLoadout result = SetupLoadout.build(List.of(ActivitySetups.byId("569290:3")), Map.of(), Set.of(), "", state);
		assertTrue(result.equipmentNotes.get(10828).contains("Requires:"));
		assertTrue(result.equipment.isEmpty());
	}

	@Test
	public void ibansStaffIsShownWithItsChargeNoteWithoutSuggestingTheBrokenVersion()
	{
		SetupLoadout result = plan("25401:10");
		assertEquals(Integer.valueOf(1409), result.equipmentExample.get(3));
		assertTrue(result.equipmentNotes.get(1409).contains("check spell charges"));
		assertFalse(result.equipment.containsKey(3));
		assertFalse(result.equipmentExample.containsValue(1410));
	}

	@Test
	public void everyEquipmentTableRetainsItsListedSlotsInAnEmptyBank()
	{
		List<String> missing = new ArrayList<>();
		int tables = 0;
		for (ActivitySetups.Setup setup : ActivitySetups.all())
		{
			SetupLoadout result = plan(setup.id);
			if (result == null || result.equipmentTable() == null)
				continue;
			tables++;
			for (SetupLoadout.Field field : result.equipmentTable().fields)
			{
				int slot = field.slot();
				if (slot < 0 || field.ids.isEmpty() || result.equipmentExample.containsKey(slot))
					continue;
				if (slot == 13)
				{
					ItemCatalogueIndex.Item weapon = ItemCatalogueIndex.guideItem(result.equipmentExample.getOrDefault(3, -1));
					boolean compatible = field.ids.stream().map(ItemCatalogueIndex::guideItem)
							.filter(Objects::nonNull).anyMatch(item -> item.slot == slot
									&& SetupLoadout.equipmentReady(setup, item)
									&& SetupEquipmentChoice.allowed(item, "", weapon, List.of()));
					if (!compatible)
						continue;
				}
				missing.add(setup.id + " " + setup.activity + " " + field.name);
			}
		}
		assertTrue("Only checked " + tables + " tables", tables > 300);
		assertEquals(Collections.emptyList(), missing);
	}
}
