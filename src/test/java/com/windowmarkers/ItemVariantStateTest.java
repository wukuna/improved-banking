package com.windowmarkers;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class ItemVariantStateTest
{
    private AutoCategories.Item item(int id,String name)
    { return new AutoCategories.Item(id,name,true,null,0,0,0); }
    private Map<String,List<AutoCategories.Item>> raid(AutoCategories.Item... items)
    { return AutoCategories.suggest(AutoCategories.Preset.CHAMBERS_OF_XERIC,Arrays.asList(items)); }
    private boolean selected(Map<String,List<AutoCategories.Item>> groups,int id)
    {
        return groups.entrySet().stream().filter(e -> RaidRecommendations.defaultSelected(e.getKey()))
            .flatMap(e -> e.getValue().stream()).anyMatch(i -> i.id==id);
    }
    private boolean contains(Map<String,List<AutoCategories.Item>> groups,int id)
    { return groups.values().stream().flatMap(Collection::stream).anyMatch(i -> i.id==id); }

    @Test public void identicalNamesDoNotConcealBrokenIbanStaff()
    {
        assertEquals("iban's staff",ItemVariantState.familyName(item(1410,"Iban's staff")));
        assertTrue(ItemVariantState.needsActivityReview(item(1410,"Iban's staff")));
        assertEquals("Needs repair/charge — review",ItemVariantState.reviewGroup(item(1410,"Iban's staff")));
        assertEquals("Check charges — review",ItemVariantState.reviewGroup(item(1409,"Iban's staff")));
    }

    @Test public void tridentFullEnhancedAndOrnamentedIdentitiesKeepTheirCombatRole()
    {
        AutoCategories.Item[] bank={item(11905,"Trident of the Seas (full)"),item(11907,"Trident of the Seas"),
            item(22288,"Trident of the Seas (e)"),item(33323,"Trident of the Seas (full) (o)"),
            item(33318,"Trident of the Swamp (e) (o)"),item(11908,"Uncharged trident"),
            item(12900,"Uncharged toxic trident"),item(33320,"Uncharged toxic trident (e) (o)")};
        Map<String,List<AutoCategories.Item>> groups=raid(bank);
        for(int id:new int[]{11905,11907,22288,33323,33318}) { assertTrue("Usable variant "+id,selected(groups,id)); }
        for(int id:new int[]{11908,12900,33320})
        { assertTrue(contains(groups,id)); assertFalse("Depleted variant "+id,selected(groups,id)); }
    }

    @Test public void unchargedUpgradeCannotRemoveChargedLowerWeaponInAnyRaid()
    {
        for(AutoCategories.Preset preset:new AutoCategories.Preset[]{AutoCategories.Preset.CHAMBERS_OF_XERIC,
            AutoCategories.Preset.THEATRE_OF_BLOOD,AutoCategories.Preset.TOMBS_OF_AMASCUT})
        {
            List<AutoCategories.Item> bank=Arrays.asList(item(11905,"Trident of the Seas (full)"),
                item(12900,"Uncharged toxic trident"),item(22481,"Sanguinesti staff (uncharged)"));
            Map<String,List<AutoCategories.Item>> groups=AutoCategories.suggest(preset,bank);
            Map<Integer,String> excluded=new HashMap<>();
            RaidGearFilter.filter(preset,groups,new int[]{99,99,99,99,99,99,99},excluded);
            // Some raid tables do not recommend the lower weapon; only assert preservation where listed.
            if(preset!=AutoCategories.Preset.THEATRE_OF_BLOOD) { assertTrue(selected(groups,11905)); }
            assertFalse(excluded.containsKey(11905));
            assertFalse(selected(groups,12900)); assertFalse(selected(groups,22481));
        }
    }

    @Test public void chargedEnhancedUpgradeCanReplaceSameRoleLowerWeapon()
    {
        Map<String,List<AutoCategories.Item>> groups=raid(item(11905,"Trident of the Seas (full)"),
            item(33318,"Trident of the Swamp (e) (o)"));
        RaidGearFilter.filter(AutoCategories.Preset.CHAMBERS_OF_XERIC,groups,new int[]{99,99,99,99,99,99,99},new HashMap<>());
        assertFalse(contains(groups,11905)); assertTrue(selected(groups,33318));
    }

    @Test public void partialDurabilityIsUsableButBrokenArmourIsNot()
    {
        Map<String,List<AutoCategories.Item>> groups=raid(item(4859,"Ahrim's hood 25"),item(4860,"Ahrim's hood 0"),
            item(29037,"Blue moon chestplate"),item(29058,"Blue moon chestplate (broken)"));
        assertTrue(selected(groups,4859)); assertFalse(selected(groups,4860));
        assertFalse(ItemVariantState.needsActivityReview(item(29037,"Blue moon chestplate")));
        assertTrue(ItemVariantState.needsActivityReview(item(29058,"Blue moon chestplate")));
    }

    @Test public void brokenVoidAndInactiveCrystalDoNotCompleteSets()
    {
        Map<String,List<AutoCategories.Item>> groups=raid(item(13072,"Elite void top"),item(13073,"Elite void robe"),
            item(20475,"Void knight gloves"),item(11665,"Void melee helm"));
        assertFalse(selected(groups,13072));
        groups=raid(item(23971,"Crystal helm"),item(23975,"Crystal body"),item(23981,"Crystal legs"),
            item(25867,"Bow of faerdhinen (c)"));
        assertFalse(selected(groups,23971));
        groups=raid(item(23971,"Crystal helm"),item(23975,"Crystal body"),item(23979,"Crystal legs"),
            item(25867,"Bow of faerdhinen (c)"));
        assertTrue(selected(groups,23971));
    }

    @Test public void reducedFunctionIsNotConfusedWithEntirelyUseless()
    {
        assertFalse(ItemVariantState.needsActivityReview(item(23682,"Crystal pickaxe (inactive)")));
        assertTrue(selected(raid(item(23682,"Crystal pickaxe (inactive)")),23682));
        assertFalse(ItemVariantState.needsActivityReview(item(1704,"Amulet of glory")));
        assertFalse(selected(raid(item(20716,"Tome of fire (empty)")),20716));
        assertFalse(selected(raid(item(22486,"Scythe of vitur (uncharged)")),22486));
        Map<String,List<AutoCategories.Item>> broad=AutoCategories.suggest(AutoCategories.Preset.ALL_CATEGORIES,
            Arrays.asList(item(20716,"Tome of fire (empty)"),item(22486,"Scythe of vitur (uncharged)"),item(5331,"Watering can")));
        for(int id:new int[]{20716,22486,5331}) { assertTrue(contains(broad,id)); }
        assertTrue(broad.keySet().stream().anyMatch(g -> g.contains("fill before watering")));
    }

    @Test public void supplyPathCannotBypassStateCheck()
    {
        Map<String,List<AutoCategories.Item>> groups=raid(item(-77,"Rune pouch (empty)"));
        assertTrue(contains(groups,-77)); assertFalse(selected(groups,-77));
        assertTrue(selected(raid(item(-78,"Saradomin brew(1)")),-78));
    }

    @Test public void depletedToolsRetainBaseUseAndArclightRetainsOnlyItsSpecialRole()
    {
        for(int id:new int[]{13242,13244,21033})
        { assertFalse(ItemVariantState.needsActivityReview(item(id,"uncharged tool"))); }
        AutoCategories.Item arclight=item(30305,"Arclight (inactive)");
        assertTrue(ItemVariantState.needsActivityReview(arclight));
        assertFalse(ItemVariantState.needsActivityReview(arclight,"Special attacks (strategy dependent)"));
        assertTrue(selected(raid(arclight),30305));
    }

    @Test public void permanentChargesAndDepletedQuiverAreDifferentStates()
    {
        Map<String,List<AutoCategories.Item>> groups=raid(item(28955,"Blessed Dizana's quiver"),
            item(28947,"Dizana's quiver (uncharged)"),item(28828,"Blessed Dizana's quiver (broken)"));
        assertTrue(selected(groups,28955));
        assertFalse(selected(groups,28947)); assertFalse(selected(groups,28828));
        assertTrue(ItemVariantState.needsActivityReview(item(27662,"Accursed sceptre (u)")));
    }

    @Test public void depletedTeleportChargesDoNotChangeCombatRoleKeys()
    {
        Map<String,List<AutoCategories.Item>> groups=AutoCategories.suggest(AutoCategories.Preset.TOMBS_OF_AMASCUT,
            Collections.singletonList(item(1704,"Amulet of glory")));
        assertTrue(groups.containsKey("ToA Melee"));
        assertTrue(selected(groups,1704));
    }

    @Test public void recolouredCrystalHasTheSameActiveAndInactiveRules()
    {
        assertEquals("crystal body",ItemVariantState.familyName(item(27769,"Crystal body")));
        assertFalse(ItemVariantState.needsActivityReview(item(27769,"Crystal body")));
        assertTrue(ItemVariantState.needsActivityReview(item(27771,"Crystal body")));
        Map<String,List<AutoCategories.Item>> groups=raid(item(23971,"Crystal helm"),item(27771,"Crystal body"),
            item(23979,"Crystal legs"),item(25867,"Bow of faerdhinen (c)"));
        assertFalse(selected(groups,23971));
    }

    @Test public void nonCombatChargeExceptionsRetainTheirActualFunctions()
    {
        assertFalse(ItemVariantState.needsActivityReview(item(25539,"Celestial ring (uncharged)")));
        assertFalse(ItemVariantState.needsActivityReview(item(8921,"Black mask")));
        assertFalse(ItemVariantState.needsActivityReview(item(19710,"Ring of suffering (i)")));
        assertTrue(ItemVariantState.needsActivityReview(item(29988,"Alchemist's amulet")));
        assertFalse(ItemVariantState.needsActivityReview(item(29990,"Alchemist's amulet")));
        assertTrue(ItemVariantState.needsActivityReview(item(26967,"Circlet of water (uncharged)")));
        assertTrue(ItemVariantState.needsActivityReview(item(25633,"Imcando hammer (broken)")));
    }

    @Test public void emptyCanMeanReadyContainerOrMissingBait()
    {
        assertFalse(ItemVariantState.needsActivityReview(item(583,"Bailing bucket")));
        assertTrue(ItemVariantState.needsActivityReview(item(585,"Bailing bucket")));
        assertTrue(ItemVariantState.needsActivityReview(item(3157,"Karambwan vessel")));
        assertFalse(ItemVariantState.needsActivityReview(item(3159,"Karambwan vessel")));
        assertEquals("Check contents — review",ItemVariantState.reviewGroup(item(22997,"Bottomless compost bucket")));
        assertTrue(ItemVariantState.needsActivityReview(item(4252,"Ectophial")));
        assertFalse(ItemVariantState.needsActivityReview(item(4251,"Ectophial")));
    }

    @Test public void lockedMangledEquipmentCannotUseTheHealthyNameAlias()
    {
        assertTrue(ItemVariantState.needsActivityReview(item(33510,"Ice ancient sceptre (l) (mangled)")));
        assertFalse(ItemVariantState.needsActivityReview(item(28474,"Ice ancient sceptre (l)")));
        assertTrue(ItemTaxonomy.matches(item(28474,"Ice ancient sceptre (l)"),"ancient sceptre"));
        assertFalse(ItemTaxonomy.matches(item(10362,"Amulet of glory (t)"),"amulet of glory"));
    }

    @Test public void rewardCosmeticsRetainBaseRolesWithoutRestoringBrokenPieces()
    {
        Map<String,List<AutoCategories.Item>> groups=raid(item(28688,"Blazing blowpipe"),
            item(28687,"Blazing blowpipe (empty)"),item(30537,"Echo Ahrim's hood 25"),
            item(30543,"Echo Ahrim's hood 0"));
        assertTrue(selected(groups,28688)); assertTrue(selected(groups,30537));
        assertFalse(selected(groups,28687)); assertFalse(selected(groups,30543));
        assertTrue(ItemVariantState.needsActivityReview(item(33025,"Crystal body (inactive)")));
        assertEquals("crystal body",ItemVariantState.familyName(item(33023,"Crystal body")));
        assertEquals("toxic staff of the dead",ItemVariantState.familyName(item(33036,"Toxic staff (Deadman)")));
    }

    @Test public void cosmeticSetWeaponsRecognizeOwnedCosmeticArmour()
    {
        Map<String,List<AutoCategories.Item>> groups=AutoCategories.suggest(AutoCategories.Preset.BROAD_TYPES,
            Arrays.asList(item(30568,"Echo Ahrim's staff"),item(30445,"Echo Ahrim's hood")));
        assertEquals(1,groups.size());
        assertTrue(groups.keySet().iterator().next().startsWith("Ahrim —"));
        groups=AutoCategories.suggest(AutoCategories.Preset.BROAD_TYPES,
            Collections.singletonList(item(30568,"Echo Ahrim's staff")));
        assertEquals("Magic weapons",groups.keySet().iterator().next());
    }

    @Test public void emptyNestBoxesAndReusableContainersAreNotDepletedGear()
    {
        assertFalse(ItemVariantState.needsActivityReview(item(12792,"Nest box (empty)")));
        assertTrue(ItemVariantState.annotate("Nests",item(12792,"Nest box (empty)")).contains("contains empty bird nests"));
        assertFalse(ItemVariantState.needsActivityReview(item(31989,"Boat bottle (empty)")));
        assertFalse(ItemVariantState.needsActivityReview(item(33074,"Facility bottle (empty)")));
        assertFalse(ItemVariantState.needsActivityReview(item(33091,"Bottomless milk bucket (empty)")));
        assertEquals("Check contents — review",ItemVariantState.reviewGroup(item(33089,"Bottomless milk bucket")));
    }

    @Test public void skillingPassivesAndPreparationStatesAreDistinguishedById()
    {
        assertFalse(ItemVariantState.needsActivityReview(item(31243,"Horn of Plenty (empty)")));
        assertFalse(ItemVariantState.needsActivityReview(item(33103,"Cowbell amulet")));
        assertTrue(ItemVariantState.needsActivityReview(item(20275,"Gnomish firelighter")));
        assertTrue(ItemVariantState.needsActivityReview(item(7431,"Fungicide spray 0")));
        assertFalse(ItemVariantState.needsActivityReview(item(7430,"Fungicide spray 1")));
        assertTrue(ItemVariantState.needsActivityReview(item(4700,"Sapphire lantern")));
        assertTrue(ItemVariantState.needsActivityReview(item(4701,"Sapphire lantern")));
        assertFalse(ItemVariantState.needsActivityReview(item(4702,"Sapphire lantern")));
        assertFalse(ItemVariantState.needsActivityReview(item(6279,"Broodoo shield")));
    }

    @Test public void wildernessWeaponModesDoNotCollapseIntoPoweredStaves()
    {
        assertTrue(ItemVariantState.needsActivityReview(item(22547,"Craw's bow (u)")));
        assertFalse(ItemVariantState.needsActivityReview(item(22550,"Craw's bow")));
        assertTrue(ItemVariantState.needsActivityReview(item(27676,"Accursed sceptre (au)")));
        assertFalse(ItemTaxonomy.matches(item(27679,"Accursed sceptre (a)"),"accursed sceptre"));
        assertTrue(ItemVariantState.annotate("Melee",item(27657,"Ursine chainmace (u)")).contains("melee usable"));
    }

    @Test public void damagedTorvaAndBrokenToolsCannotDisplaceReadyAlternatives()
    {
        AutoCategories.Item damaged=item(26378,"Torva platebody (damaged)");
        AutoCategories.Item bandos=item(11832,"Bandos chestplate");
        Map<String,List<AutoCategories.Item>> groups=raid(damaged,bandos);
        Map<Integer,String> excluded=new HashMap<>();
        RaidGearFilter.filter(AutoCategories.Preset.CHAMBERS_OF_XERIC,groups,
            new int[]{99,99,99,99,99,99,99},excluded);
        assertFalse(selected(groups,26378)); assertTrue(selected(groups,11832));
        assertFalse(excluded.containsKey(11832));
        assertTrue(ItemVariantState.needsActivityReview(item(27695,"Dragon pickaxe (broken)")));
        assertFalse(selected(raid(item(27695,"Dragon pickaxe (broken)")),27695));
    }

    @Test public void degradedPouchesRemainUsefulAndHiddenCountersAreNotInvented()
    {
        for(int id:new int[]{5511,5513,5515,26786})
        {
            AutoCategories.Item pouch=item(id,"Essence pouch");
            assertFalse(ItemVariantState.needsActivityReview(pouch));
            assertTrue(ItemVariantState.annotate("Runecraft",pouch).contains("reduced capacity"));
        }
        assertTrue(ItemVariantState.needsActivityReview(item(13353,"Gricoller's can")));
        assertTrue(ItemVariantState.needsActivityReview(item(13660,"Chronicle")));
        // Teleport charges are irrelevant to the book's raid thrall-summoning role.
        assertTrue(selected(raid(item(25818,"Book of the dead")),25818));
        assertFalse(ItemVariantState.needsActivityReview(item(5521,"Binding necklace")));
        assertTrue(ItemVariantState.annotate("Runecraft",item(5521,"Binding necklace")).contains("shared uses"));
    }

    @Test public void activationDoesNotRemoveBountyHunterGearRestrictions()
    {
        AutoCategories.Item active=item(27904,"Vesta's longsword (bh)");
        AutoCategories.Item inactive=item(27906,"Vesta's longsword (bh) (inactive)");
        assertEquals("Activity restrictions — review",ItemVariantState.reviewGroup(active));
        assertEquals("Preparation needed — review",ItemVariantState.reviewGroup(inactive));
        assertTrue(ItemVariantState.needsActivityReview(active));
        assertFalse(ItemTaxonomy.matches(active,"vesta's longsword"));
        assertTrue(ItemVariantState.annotate("Melee",active).contains("restricted combat locations"));
    }

    @Test public void wordsLikeBrokenAndEmptyDoNotEraseFunctionalUses()
    {
        assertFalse(ItemVariantState.needsActivityReview(item(3335,"Broken bark snelm")));
        assertFalse(ItemVariantState.needsActivityReview(item(5350,"Empty plant pot")));
        assertFalse(ItemVariantState.needsActivityReview(item(22660,"Empty bucket pack")));
        assertFalse(ItemVariantState.needsActivityReview(item(19665,"Damaged monkey tail")));
        assertTrue(ItemVariantState.annotate("Crafting",item(19665,"Damaged monkey tail")).contains("cannot string a ballista"));
        assertFalse(ItemVariantState.needsActivityReview(item(27386,"Tumeken's damaged guardian")));
        assertTrue(ItemVariantState.needsActivityReview(item(6662,"Broken fishing rod")));
        assertTrue(ItemVariantState.needsActivityReview(item(22079,"Inert locator orb")));
        assertTrue(ItemVariantState.needsActivityReview(item(7431,"Fungicide spray 0")));
        assertFalse(ItemVariantState.needsActivityReview(item(7432,"Fungicide")));
    }

    @Test public void preparationDailyCountersAndGodBookPassivesStayDistinct()
    {
        assertTrue(ItemVariantState.needsActivityReview(item(4537,"Oil lantern")));
        assertFalse(ItemVariantState.needsActivityReview(item(4539,"Oil lantern")));
        assertTrue(ItemVariantState.annotate("Slayer",item(7053,"Lit bug lantern")).contains("not a general light source"));
        assertFalse(ItemVariantState.needsActivityReview(item(3839,"Damaged book")));
        assertTrue(ItemVariantState.annotate("Prayer",item(3839,"Damaged book")).contains("god protection retained"));
        assertFalse(ItemVariantState.needsActivityReview(item(13136,"Desert amulet 4")));
        assertTrue(ItemVariantState.annotate("Travel",item(13136,"Desert amulet 4")).contains("unlimited"));
        assertTrue(ItemVariantState.annotate("Travel",item(13134,"Desert amulet 2")).contains("daily limit"));
        assertTrue(ItemVariantState.needsActivityReview(item(24735,"Ring of endurance (uncharged)")));
        assertEquals("Activity restrictions — review",ItemVariantState.reviewGroup(item(28543,"Corrupted scythe of vitur")));
    }

    @Test public void allReviewRequiredVariantsStayUncheckedAcrossEveryRaidPreset()
    {
        for(String[] row:ItemTaxonomy.readRows("item-variant-states.tsv",5))
        {
            if(!row[3].equals("true")) { continue; }
            AutoCategories.Item variant=item(Integer.parseInt(row[0]),row[1]);
            for(AutoCategories.Preset preset:new AutoCategories.Preset[]{AutoCategories.Preset.CHAMBERS_OF_XERIC,
                AutoCategories.Preset.THEATRE_OF_BLOOD,AutoCategories.Preset.TOMBS_OF_AMASCUT,AutoCategories.Preset.RAID_MEGA_RARES})
            {
                Map<String,List<AutoCategories.Item>> groups=AutoCategories.suggest(preset,Collections.singletonList(variant));
                for(String group:groups.keySet())
                {
                    if(variant.id==30305 && group.contains("Special attacks")) { continue; }
                    assertFalse(row[0]+" / "+preset+" / "+group,RaidRecommendations.defaultSelected(group));
                }
            }
        }
    }
}
