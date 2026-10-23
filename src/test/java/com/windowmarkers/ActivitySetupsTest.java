package com.windowmarkers;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class ActivitySetupsTest
{
    private AutoCategories.Item item(int id,String name)
    { return new AutoCategories.Item(id,name,true,null,0,0,0); }
    private boolean contains(Map<String,List<AutoCategories.Item>> groups,int id)
    { return groups.values().stream().flatMap(Collection::stream).anyMatch(i -> i.id==id); }

    @Test public void combatCapeEquivalenceDoesNotGrantSkillingMaxCapeBenefits()
    {
        List<AutoCategories.Item> capes=Arrays.asList(item(6570,"Fire cape"),item(13329,"Fire max cape"),item(13280,"Max cape"));
        ActivitySetups.Setup vorkath=ActivitySetups.forActivity("Vorkath").stream()
            .filter(s -> s.label.equals("Melee — Equipment alternatives")).findFirst().get();
        assertTrue(contains(vorkath.suggest(capes),6570));
        assertTrue(contains(vorkath.suggest(capes),13329));
        ActivitySetups.Setup wintertodt=ActivitySetups.forActivity("Wintertodt").stream()
            .filter(s -> s.label.equals("Solo — Equipment alternatives")).findFirst().get();
        // Fire/max fire capes may qualify as explicit warm clothing, but must
        // not be included solely by a combat-only alias to the normal max cape.
        assertTrue(wintertodt.itemIds.contains(13280));
        assertFalse(CombatEquivalence.matches(item(13329,"Fire max cape"),"max cape"));
    }

    @Test public void selectedExamplesKeepConditionsAndRequireReview()
    {
        ActivitySetups.Setup setup=ActivitySetups.forActivity("Vorkath").stream()
            .filter(s -> s.label.equals("Ranged (crossbow) — Equipment alternatives")).findFirst().get();
        assertTrue(setup.notes.contains("Void"));
        assertTrue(setup.notes.contains("250-300"));
        Map<String,List<AutoCategories.Item>> groups=setup.suggest(Arrays.asList(item(9185,"Rune crossbow"),item(999999,"Rune crossbow")));
        assertTrue(contains(groups,9185)); assertFalse(contains(groups,999999));
        assertTrue(groups.keySet().stream().noneMatch(RaidRecommendations::defaultSelected));
        assertTrue(setup.source.contains("oldid="));
    }

    @Test public void skillingAlternativesDoNotTransferExtraEffectsOrQuestIdentities()
    {
        ActivitySetups.Setup movement=ActivitySetups.forActivity("Hallowed Sepulchre").stream()
            .filter(s -> s.id.equals("267008:2")).findFirst().get();
        assertTrue(movement.itemIds.contains(11852));
        assertTrue(movement.itemIds.contains(13581));
        assertTrue(movement.itemIds.contains(9771));
        assertTrue(movement.itemIds.contains(13280));
        assertFalse(movement.itemIds.contains(13329));
        ActivitySetups.Setup spirit=ActivitySetups.forActivity("Tempoross").stream()
            .filter(s -> s.id.equals("306436:8")).findFirst().get();
        assertTrue(spirit.itemIds.contains(25592));
        assertFalse(spirit.itemIds.contains(13258));
        assertFalse(spirit.itemIds.contains(31252)); // Tortugan quest object shares the headband's name.
        for (String activity:ActivitySetups.activities(""))
        {
            List<ActivitySetups.Setup> setups=ActivitySetups.forActivity(activity);
            assertEquals(setups.size(),setups.stream().map(s -> s.label).distinct().count());
        }
    }

    @Test public void undocumentedCacheCopyCannotBecomeNormalRaidFood()
    {
        Map<String,List<AutoCategories.Item>> groups=AutoCategories.suggest(AutoCategories.Preset.THEATRE_OF_BLOOD,
            Arrays.asList(item(34623,"Anglerfish"),item(13441,"Anglerfish")));
        assertTrue(groups.entrySet().stream().anyMatch(e -> e.getValue().stream().anyMatch(i -> i.id==13441)
            && RaidRecommendations.defaultSelected(e.getKey())));
        assertTrue(groups.entrySet().stream().filter(e -> e.getValue().stream().anyMatch(i -> i.id==34623))
            .allMatch(e -> !RaidRecommendations.defaultSelected(e.getKey())));
        assertTrue(ItemVariantState.identityNeedsReview(item(34623,"Anglerfish")));
    }

    @Test public void examplesNeverFillFoodGapsUsingEatActionsOrSimilarNames()
    {
        List<AutoCategories.Item> unrelated=Arrays.asList(item(13441,"Anglerfish"),item(24592,"Blighted anglerfish"),
            item(7509,"Dwarven rock cake"),item(10476,"Purple sweets"));
        boolean foundRealFood=false;
        for (ActivitySetups.Setup setup:ActivitySetups.forActivity("Vorkath"))
        {
            Map<String,List<AutoCategories.Item>> groups=setup.suggest(unrelated);
            foundRealFood|=contains(groups,13441);
            assertFalse(contains(groups,24592)); assertFalse(contains(groups,7509)); assertFalse(contains(groups,10476));
        }
        assertTrue(foundRealFood);
    }

    @Test public void provisionalRaidDoesNotBorrowOtherRaidsSuppliesOrRankings()
    {
        AutoCategories.Preset raid=AutoCategories.Preset.FRACTURED_ARCHIVE;
        List<AutoCategories.Item> bank=Arrays.asList(item(34715,"Obligator"),item(13329,"Fire max cape"),
            item(6685,"Saradomin brew(4)"),item(24592,"Blighted anglerfish"),item(564,"Cosmic rune"));
        Map<String,List<AutoCategories.Item>> groups=AutoCategories.suggest(raid,bank);
        assertTrue(contains(groups,34715)); assertTrue(contains(groups,13329)); assertTrue(contains(groups,6685));
        assertFalse(contains(groups,24592)); assertFalse(contains(groups,564));
        assertTrue(groups.keySet().stream().noneMatch(RaidRecommendations::defaultSelected));
        assertEquals("tfa",RaidRecommendations.raid(raid));
        assertTrue(ActivitySetups.activities("fractured").contains("The Fractured Archive"));
    }

    @Test public void specialistAndEntryGearRetainTheirSeparateRoles()
    {
        Map<String,List<AutoCategories.Item>> tob=AutoCategories.suggest(AutoCategories.Preset.THEATRE_OF_BLOOD,
            Arrays.asList(item(28997,"Dual macuahuitl"),item(4587,"Dragon scimitar")));
        assertTrue(tob.keySet().stream().anyMatch(k -> k.contains("Verzik P1")));
        assertTrue(tob.entrySet().stream().filter(e -> e.getValue().stream().anyMatch(i -> i.id==4587))
            .allMatch(e -> !RaidRecommendations.defaultSelected(e.getKey())));
    }
}
