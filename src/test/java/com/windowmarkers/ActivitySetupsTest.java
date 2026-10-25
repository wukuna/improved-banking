package com.windowmarkers;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class ActivitySetupsTest
{
    @Test public void ordinaryWikiLinksRestoreAlternativesWithoutIgnoringTheirStates() {
        for(String key:Arrays.asList("110044:5","305955:8","635395:3")) {
            ActivitySetups.Setup s=setup(key);
            assertTrue(s.itemIds.containsAll(Arrays.asList(12899,12900,33314,33316)));
            Map<String,List<AutoCategories.Item>> result=s.suggest(Arrays.asList(item(12899,"Trident of the Swamp"),item(12900,"Uncharged toxic trident")));
            assertTrue(result.entrySet().stream().anyMatch(e -> e.getKey().contains("Needs repair/charge") && e.getValue().stream().anyMatch(i -> i.id==12900)));
        }
        assertTrue(setup("80876:1").itemIds.contains(9757));
        assertFalse(setup("80876:1").itemIds.contains(9756));
        assertTrue(setup("82921:1").itemIds.containsAll(Arrays.asList(13121,13122,13123,13124)));
    }

    @Test public void requiredOrnateMaulDoesNotSelectOrdinaryMaulOrLooseUpgrade() {
        for(String key:Arrays.asList("43753:9","43753:11")) {
            ActivitySetups.Setup s=setup(key);
            assertTrue(s.itemIds.containsAll(Arrays.asList(24225,24227)));
            for(int id:new int[]{4153,12848,24229}) assertFalse(s.itemIds.contains(id));
            assertTrue(s.notes.contains("must already be installed"));
        }
    }

    @Test public void valeTotemOfferingVariableContainsProductsAndRetainsCostChoice() {
        ActivitySetups.Setup s=setup("596472:3");
        assertTrue(s.itemIds.containsAll(Arrays.asList(70,72,859,861,21952,22263,1513)));
        assertFalse(s.itemIds.contains(4734)); // Another wooden-looking weapon is not a listed offering.
        assertTrue(s.notes.contains("not more magic logs"));
        assertTrue(s.notes.contains("price and required quantity are not inferred"));
    }

    @Test public void namedFootnotesAndSkillRequirementsAreNotDropped() {
        assertTrue(setup("44881:5").notes.contains("With earth spells"));
        assertTrue(setup("44881:5").notes.contains("Requires super antifire potion"));
        assertTrue(setup("102334:2").notes.contains("Defence 1"));
        assertTrue(setup("364045:1").notes.contains("Ranged section"));
        assertTrue(setup("204107:1").notes.contains("ammunition dependency"));
    }

    private ActivitySetups.Setup setup(String id) {
        return ActivitySetups.all().stream().filter(s -> s.id.equals(id)).findFirst().get();
    }

    @Test public void trimmedCapeAlternativesKeepPrayerBonusWithoutTransferringPerks() {
        ActivitySetups.Setup drakes=setup("267357:1");
        assertTrue(drakes.itemIds.containsAll(Arrays.asList(9751,19476,13069,13068,13280)));
        assertFalse(drakes.itemIds.contains(9750)); // Untrimmed Strength has no prayer bonus.
        assertFalse(drakes.itemIds.contains(9813)); // Untrimmed quest cape also has none.
        assertTrue(drakes.notes.contains("Individual teleports and skill perks are not transferred"));
    }

    @Test public void genericBarrowsSlotsDoNotAddUnrelatedWeaponsOrMagicArmour() {
        ActivitySetups.Setup vorkath=setup("108966:2");
        assertTrue(vorkath.itemIds.containsAll(Arrays.asList(4745,4955,4956,4749,4751)));
        assertFalse(vorkath.itemIds.contains(4708)); // Ahrim's hood is not a tank helm.
        assertFalse(vorkath.itemIds.contains(4718)); // Dharok's greataxe is not an armour slot.
        Map<String,List<AutoCategories.Item>> groups=vorkath.suggest(Arrays.asList(
            item(4955,"Torag's helm 25"),item(4956,"Torag's helm 0")));
        assertTrue(groups.entrySet().stream().anyMatch(e -> e.getKey().contains("Needs repair/charge")
            && e.getValue().stream().anyMatch(i -> i.id==4956)));
        assertTrue(groups.entrySet().stream().filter(e -> e.getValue().stream().anyMatch(i -> i.id==4955))
            .noneMatch(e -> e.getKey().contains("Needs repair/charge")));
        assertTrue(setup("25401:2").notes.contains("complete Guthan set"));
    }

    @Test public void explicitLanternFuelAndTabletImageResolveOnlyTheirDocumentedVersions() {
        ActivitySetups.Setup gotr=setup("351891:1");
        assertTrue(gotr.itemIds.containsAll(Arrays.asList(26842,26844,26846,26848)));
        for(int id:new int[]{26822,26824,26826,26836,26838,26840}) assertFalse(gotr.itemIds.contains(id));
        ActivitySetups.Setup attacker=setup("414593:2");
        assertTrue(attacker.itemIds.contains(27522));
        for(int id:new int[]{27519,27520,27521}) assertFalse(attacker.itemIds.contains(id));
    }

    @Test public void poweredStaffAlternativesKeepCosmeticAndChargeStatesWithoutRestrictedModes() {
        ActivitySetups.Setup wardens=setup("364045:25");
        assertTrue(wardens.itemIds.containsAll(Arrays.asList(22323,22481,25731,25733,12899,12900,33318,33320)));
        for(int id:new int[]{22335,22516,27679,27788}) assertFalse(wardens.itemIds.contains(id));
        Map<String,List<AutoCategories.Item>> groups=wardens.suggest(Arrays.asList(
            item(25731,"Holy sanguinesti staff"),item(25733,"Holy sanguinesti staff (uncharged)")));
        assertTrue(groups.entrySet().stream().anyMatch(e -> e.getKey().contains("Needs repair/charge")
            && e.getValue().stream().anyMatch(i -> i.id==25733)));
        assertTrue(groups.keySet().stream().noneMatch(RaidRecommendations::defaultSelected));
    }

    @Test public void specialistAmmunitionAndGrapplesDoNotCrossWeaponTypes() {
        ActivitySetups.Setup sire=setup("74025:1");
        assertTrue(sire.itemIds.containsAll(Arrays.asList(882,892,11212,21326)));
        assertFalse(sire.itemIds.contains(9706)); assertFalse(sire.itemIds.contains(2866));
        ActivitySetups.Setup sepulchre=setup("267008:2");
        assertTrue(sepulchre.itemIds.contains(9185));
        assertFalse(sepulchre.itemIds.contains(11785)); // Explicit Armadyl exclusion.
        assertFalse(sepulchre.itemIds.contains(19481)); // Ballista cannot grapple.
        assertTrue(sepulchre.notes.contains("Zaryte"));
    }

    @Test public void refreshedKrakenSustainAndDpsSetupsStaySeparateAndKeepRequiredShield() {
        ActivitySetups.Setup sustain=ActivitySetups.forActivity("Kraken").stream()
            .filter(s -> s.label.equals("Mage (AFK, Sustain) — Equipment alternatives")).findFirst().get();
        ActivitySetups.Setup dps=ActivitySetups.forActivity("Kraken").stream()
            .filter(s -> s.label.equals("Magic (DPS) — Equipment alternatives")).findFirst().get();
        assertNotEquals(sustain.id,dps.id);
        assertTrue(sustain.notes.contains("requires a Rondache"));
        assertFalse(dps.notes.contains("requires a Rondache"));
        assertTrue(sustain.source.contains("oldid="));
    }

    @Test public void reviewedGenericReferencesHaveExactAlternativesWithoutMixingCompletedAndDamagedBooks() {
        boolean book=false,halo=false,unimbued=false;
        for(ActivitySetups.Setup s:ActivitySetups.all()) {
            if(s.notes.contains("These are completed god books")) {
                assertTrue(s.itemIds.contains(3840)); // Holy book
                book=true;
            }
            if(s.notes.contains("Halo alternatives retain")) { assertTrue(s.itemIds.contains(12637)); halo=true; }
            if(s.notes.contains("Unimbued god capes retain")) { assertTrue(s.itemIds.contains(2412)); unimbued=true; }
        }
        assertTrue(book && halo && unimbued);
        ActivitySetups.Setup broken=new ActivitySetups.Setup(new String[]{"test","Test","Example","source",""});
        broken.itemIds.add(4860);
        assertTrue(broken.suggest(Collections.singletonList(item(4860,"Ahrim's hood 0"))).keySet().stream().noneMatch(RaidRecommendations::defaultSelected));
    }
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
    @Test public void specialistWeaponGroupsStayInTheirGuideRoleAndRespectEmptyStates() {
        ActivitySetups.Setup souls=ActivitySetups.forActivity("The Whisperer").stream().filter(s -> s.label.startsWith("Lost Souls")).findFirst().get();
        assertTrue(souls.itemIds.containsAll(Arrays.asList(12926,31575,861,22804,22810)));
        Map<String,List<AutoCategories.Item>> empty=souls.suggest(Collections.singletonList(item(12924,"Toxic blowpipe (empty)")));
        assertTrue(contains(empty,12924));
        assertTrue(empty.keySet().stream().noneMatch(RaidRecommendations::defaultSelected));
        ActivitySetups.Setup clone=ActivitySetups.forActivity("Moons of Peril").stream().filter(s -> s.label.startsWith("Eclipse Clone phase")).findFirst().get();
        assertTrue(clone.itemIds.containsAll(Arrays.asList(11802,11804,11806,11808,26233)));
        assertFalse(clone.itemIds.contains(11798)); // A godsword blade is a component, not a weapon.
        assertTrue(clone.notes.contains("special attacks are not interchangeable"));
        ActivitySetups.Setup shield=ActivitySetups.forActivity("Spindel").stream().filter(s -> s.label.startsWith("Ranged")).findFirst().get();
        assertTrue(shield.itemIds.contains(22284)); assertFalse(shield.itemIds.contains(22266));
        assertTrue(shield.notes.contains("one-handed"));
    }
}
