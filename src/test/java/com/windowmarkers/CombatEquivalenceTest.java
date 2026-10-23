package com.windowmarkers;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class CombatEquivalenceTest
{
    private AutoCategories.Item item(int id,String name)
    { return new AutoCategories.Item(id,name,true,null,0,0,0); }
    private boolean has(Map<String,List<AutoCategories.Item>> groups,int id)
    { return groups.values().stream().flatMap(Collection::stream).anyMatch(i -> i.id==id); }
    private final AutoCategories.Preset[] raids={AutoCategories.Preset.CHAMBERS_OF_XERIC,
        AutoCategories.Preset.THEATRE_OF_BLOOD,AutoCategories.Preset.TOMBS_OF_AMASCUT};

    @Test public void combatEquivalentsAreIncludedAcrossRaidPresets()
    {
        AutoCategories.Item[] items={item(13329,"Fire max cape"),item(21285,"Infernal max cape"),
            item(27374,"Masori assembler"),item(27363,"Masori assembler max cape"),
            item(21898,"Assembler max cape"),item(28902,"Dizana's max cape"),
            item(21776,"Imbued Saradomin max cape"),item(34633,"Imbued ancient cape"),item(34625,"Imbued ancient max cape")};
        for (AutoCategories.Preset raid:raids)
        {
            Map<String,List<AutoCategories.Item>> groups=AutoCategories.suggest(raid,Arrays.asList(items));
            for(AutoCategories.Item i:items) { assertTrue(raid+": "+i.name,has(groups,i.id)); }
        }
    }
    @Test public void equalityDoesNotTransferCluesGodProtectionOrAllMaxPerks()
    {
        assertFalse(ItemTaxonomy.matches(item(13329,"Fire max cape"),"fire cape"));
        assertFalse(CombatEquivalence.matches(item(13329,"Fire max cape"),"max cape"));
        assertFalse(CombatEquivalence.matches(item(34633,"Imbued ancient cape"),"imbued saradomin cape"));
        assertFalse(CombatEquivalence.matches(item(21186,"Fire max cape"),"fire cape")); // animation ID
        List<WikiCatalogue.Entry> roles=WikiCatalogue.entries(item(13329,"Fire max cape"));
        assertTrue(roles.stream().anyMatch(e -> e.group.equals("Runecraft — pouch protection")));
        assertFalse(roles.stream().anyMatch(e -> e.group.contains("Crafting Guild teleport")));
        assertFalse(CombatEquivalence.matches(item(13280,"Max cape"),"ava's assembler")); // account unlock not inferred
    }
    @Test public void ornamentsResolveToTheirActualCombatVersion()
    {
        assertTrue(CombatEquivalence.matches(item(27253,"Elidinis' ward (or)"),"elidinis' ward (f)"));
        assertFalse(CombatEquivalence.matches(item(27253,"Elidinis' ward (or)"),"elidinis' ward"));
        assertTrue(CombatEquivalence.matches(item(29804,"Amulet of rancour (s)"),"amulet of rancour"));
        assertTrue(CombatEquivalence.matches(item(28025,"Dragon dagger (p++)(cr)"),"dragon dagger(p++)"));
        assertFalse(CombatEquivalence.matches(item(28025,"Dragon dagger (p++)(cr)"),"dragon dagger"));
        assertTrue(ItemVariantState.needsActivityReview(item(33332,"Iban's staff (u) (o)")));
        assertTrue(ItemVariantState.needsActivityReview(item(33316,"Uncharged toxic trident (o)")));
    }
    @Test public void productionInputsDoNotOverruleEquipmentOrSupplyRoles()
    {
        assertTrue(WikiCatalogue.entries(item(1933,"Pot of flour")).stream()
            .anyMatch(e -> e.facet.equals("skilling") && e.group.equals("Cooking — recipe inputs")));
        assertTrue(WikiCatalogue.entries(item(946,"Knife")).stream()
            .anyMatch(e -> e.facet.equals("skilling") && e.group.endsWith(" — tools")));
        assertTrue(WikiCatalogue.entries(item(227,"Vial of water")).stream()
            .anyMatch(e -> e.group.equals("Herblore — recipe inputs")));
        assertFalse(WikiCatalogue.category(item(6570,"Fire cape"),AutoCategories.Preset.BROAD_TYPES).contains("recipe inputs"));
    }
    @Test public void usableOrnamentedUpgradeParticipatesWithoutCollapsingEqualVariants()
    {
        AutoCategories.Preset raid=AutoCategories.Preset.CHAMBERS_OF_XERIC;
        Map<String,List<AutoCategories.Item>> groups=AutoCategories.suggest(raid,Arrays.asList(
            item(25985,"Elidinis' ward"),item(27251,"Elidinis' ward (f)"),item(27253,"Elidinis' ward (or)")));
        RaidGearFilter.filter(raid,groups,new int[]{99,99,99,99,99,99,99},true,true,new HashMap<>());
        assertFalse(has(groups,25985));assertTrue(has(groups,27251));assertTrue(has(groups,27253));
        groups=AutoCategories.suggest(raid,Arrays.asList(item(25985,"Elidinis' ward"),item(27253,"Elidinis' ward (or)")));
        RaidGearFilter.filter(raid,groups,new int[]{99,99,99,99,99,99,99},true,true,new HashMap<>());
        assertFalse(has(groups,25985));assertTrue(has(groups,27253));
    }
    @Test public void containersAndCountersKeepTheirDifferentFunctions()
    {
        assertTrue(ItemVariantState.needsActivityReview(item(1831,"Waterskin(0)")));
        assertFalse(ItemVariantState.needsActivityReview(item(1823,"Waterskin(4)")));
        assertFalse(ItemVariantState.needsActivityReview(item(33386,"Open gem pouch")));
        assertFalse(ItemVariantState.needsActivityReview(item(25584,"Open fish barrel")));
        assertTrue(ItemVariantState.needsActivityReview(item(33218,"Knapsack")));
        assertTrue(ItemVariantState.needsActivityReview(item(33431,"Trinket of vengeance (1)")));
        assertTrue(ItemVariantState.needsActivityReview(item(20784,"Dragon claws"))); // LMS copy shares a real weapon's name
        assertFalse(ItemVariantState.needsActivityReview(item(13652,"Dragon claws")));
    }
    @Test public void gloryStatesSeparateCombatMiningAndClueUses()
    {
        assertFalse(WikiCatalogue.entries(item(1704,"Amulet of glory")).stream().anyMatch(e -> e.group.startsWith("Mining")));
        assertTrue(WikiCatalogue.entries(item(1712,"Amulet of glory(4)")).stream().anyMatch(e -> e.group.equals("Mining — glory gem benefit")));
        assertTrue(WikiCatalogue.entries(item(10354,"Amulet of glory (t4)")).stream().anyMatch(e -> e.group.equals("Mining — glory gem benefit")));
        assertTrue(WikiCatalogue.entries(item(19707,"Amulet of eternal glory")).stream().anyMatch(e -> e.group.equals("Mining — glory gem benefit")));
        assertFalse(ItemVariantState.needsActivityReview(item(1704,"Amulet of glory")));
        assertTrue(CombatEquivalence.matches(item(19707,"Amulet of eternal glory"),"amulet of glory"));
        assertFalse(ItemTaxonomy.matches(item(10362,"Amulet of glory (t)"),"amulet of glory"));
    }
    @Test public void sharedDisplayNamesDoNotShareUnrelatedWikiRoles()
    {
        assertTrue(WikiCatalogue.entries(item(25615,"Large water container")).isEmpty());
        assertTrue(WikiCatalogue.entries(item(21539,"Heat-proof vessel")).isEmpty());
        assertFalse(WikiCatalogue.entries(item(617,"Coins")).stream().anyMatch(e -> e.group.toLowerCase(Locale.ROOT).contains("currenc")));
        assertTrue(WikiCatalogue.entries(item(617,"Coins")).stream().anyMatch(e -> e.group.contains("Shilo")));
        assertTrue(CombatEquivalence.matches(item(19641,"Black slayer helmet (i)"),"slayer helmet (i)"));
        assertFalse(CombatEquivalence.matches(item(19641,"Black slayer helmet (i)"),"slayer helmet"));
        assertFalse(CombatEquivalence.matches(item(2603,"Adamant kiteshield (t)"),"adamant kiteshield"));
    }
    @Test public void unusableVariantsRemainUncheckedAndCannotReplaceUsableGear()
    {
        for (AutoCategories.Preset raid:raids)
        {
            Map<String,List<AutoCategories.Item>> groups=AutoCategories.suggest(raid,Arrays.asList(
                item(20447,"Fire max cape (broken)"),item(21289,"Infernal max cape (broken)"),item(6570,"Fire cape")));
            assertTrue(groups.entrySet().stream().filter(e -> has(Collections.singletonMap(e.getKey(),e.getValue()),21289))
                .allMatch(e -> !RaidRecommendations.defaultSelected(e.getKey())));
            RaidGearFilter.filter(raid,groups,new int[]{99,99,99,99,99,99,99},true,true,new HashMap<>());
            assertTrue(has(groups,6570));
        }
    }
    @Test public void equalVariantsStayAndMaxReplacementNeedsAllSkills()
    {
        for (AutoCategories.Preset raid:raids)
        {
            List<AutoCategories.Item> capes=Arrays.asList(item(6570,"Fire cape"),item(13329,"Fire max cape"),
                item(21285,"Infernal max cape"));
            Map<String,List<AutoCategories.Item>> groups=AutoCategories.suggest(raid,capes);
            RaidGearFilter.filter(raid,groups,new int[]{99,99,99,99,99,99,99},true,false,new HashMap<>());
            assertTrue(has(groups,6570)); assertTrue(has(groups,13329));
            RaidGearFilter.filter(raid,groups,new int[]{99,99,99,99,99,99,99},true,true,new HashMap<>());
            assertFalse(has(groups,6570)); assertFalse(has(groups,13329)); assertTrue(has(groups,21285));
            groups=AutoCategories.suggest(raid,Arrays.asList(item(21295,"Infernal cape"),item(21285,"Infernal max cape")));
            RaidGearFilter.filter(raid,groups,new int[]{99,99,99,99,99,99,99},true,true,new HashMap<>());
            assertTrue(has(groups,21295)); assertTrue(has(groups,21285));
        }
    }
}
