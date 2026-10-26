package com.windowmarkers;

import java.util.*;
import java.awt.Color;
import net.runelite.client.config.ConfigItem;
import org.junit.Test;
import static org.junit.Assert.*;

public class SharedGuideRanksTest
{
    private AutoCategories.Item item(int id,String name) { return new AutoCategories.Item(id,name,true,new String[0],0,0,0); }
    private BankSearchIndex bank(List<AutoCategories.Item> items,List<Marker> markers) { return new BankSearchIndex(items,markers,new CustomPresets()); }
    @Test public void bankAndGeShareHeadFilteringColoursAndBrokenStateRules() {
        List<AutoCategories.Item> items=Arrays.asList(item(21018,"Ancestral hat"),item(26241,"Virtus mask"),item(4708,"Ahrim's hood"),item(4860,"Ahrim's hood 0"),item(21021,"Ancestral robe top"));
        BankSearchIndex.Match result=bank(items,Collections.emptyList()).search("toa magic head","");
        assertTrue(result.slots);assertFalse(result.ids.contains(21021));
        assertEquals(1,result.ranks.get(21018).tier);assertEquals(2,result.ranks.get(26241).tier);
        assertEquals(3,result.ranks.get(4708).tier);assertEquals(0,result.ranks.get(4860).tier);
        assertTrue(result.ranks.get(4860).explanation.contains("repair"));
        List<ItemCatalogueIndex.Item> geItems=new ArrayList<>();
        for(AutoCategories.Item i:items) geItems.add(new ItemCatalogueIndex.Item(i.id,i.name,ItemCatalogueIndex.fallbackSlot(i.id,i.name),i.actions));
        ItemCatalogueIndex.Result ge=new ItemCatalogueIndex(geItems).find("toa magic head","");
        Set<Integer> ids=new HashSet<>();ge.items.forEach(i->ids.add(i.id));assertEquals(ids,result.ids);
        ge.ranks.forEach((id,rank)->assertEquals(rank.tier,result.ranks.get(id).tier));
    }
    @Test public void bankAndLegendTyposDoNotMixMiningAndMinigames() {
        BankSearchIndex index=bank(Arrays.asList(item(11920,"Dragon pickaxe"),item(3678,"Flamtaer hammer"),item(590,"Tinderbox")),Collections.emptyList());
        assertEquals(index.find("mining tools"),index.find("minign tools"));
        assertFalse(index.find("minign tools").contains(3678));
        List<BankCatalogue.Choice> choices=Arrays.asList(new BankCatalogue.Choice(BankCatalogue.Kind.CATEGORY,"Mining tools","Mining tools"),new BankCatalogue.Choice(BankCatalogue.Kind.CATEGORY,"Minigame tools","Minigame tools"));
        List<BankCatalogue.Choice> found=BankCatalogue.search(choices,"minign tools",10);
        assertEquals(1,found.size());assertEquals("Mining tools",found.get(0).name);
    }
    @Test public void savedGuidesRetainTheirSourceAndConflictingGuidesStayNeutral() {
        Marker olm=new Marker(0,0,48,36);olm.linkedItemIds=new int[]{22978};olm.guideQuery="cox weapon";olm.guideContext="94147:23";
        Marker tekton=new Marker(0,0,48,36);tekton.linkedItemIds=new int[]{22978};tekton.guideQuery="cox weapon";tekton.guideContext="94147:14";
        BankSearchIndex index=bank(Collections.singletonList(item(22978,"Dragon hunter lance")),Arrays.asList(olm,tekton));
        Marker copy=new com.google.gson.Gson().fromJson(new com.google.gson.Gson().toJson(olm),Marker.class);
        assertEquals(2,index.ranks(Collections.singletonList(copy)).get(22978).tier);
        assertEquals(0,index.ranks(Arrays.asList(copy,tekton)).get(22978).tier);
        copy.clearGuide();assertTrue(index.ranks(Collections.singletonList(copy)).isEmpty());
    }
    @Test public void cosmeticVariantsAreDiscoverableButBrokenOrEmptyOnesAreNeutral() {
        List<AutoCategories.Item> items=Arrays.asList(item(30445,"Echo ahrim's hood"),item(30543,"Echo ahrim's hood 0"),item(28688,"Blazing blowpipe"),item(28687,"Blazing blowpipe (empty)"));
        BankSearchIndex index=bank(items,Collections.emptyList());
        BankSearchIndex.Match magic=index.search("toa magic head","");
        assertTrue(magic.ids.contains(30445));assertEquals(3,magic.ranks.get(30445).tier);assertEquals(0,magic.ranks.get(30543).tier);
        BankSearchIndex.Match ranged=index.search("toa ranged weapon","");
        assertTrue(ranged.ids.containsAll(Arrays.asList(28688,28687)));assertEquals(0,ranged.ranks.get(28687).tier);
        assertTrue(ranged.ranks.get(28688).explanation.contains("Cosmetic variant"));
    }
    @Test public void maxCapeCombatEquivalentsRetainTheirRanksWithoutBecomingSkillingMaxCapes() {
        List<AutoCategories.Item> items=Arrays.asList(item(6570,"Fire cape"),item(13329,"Fire max cape"),item(21295,"Infernal cape"),item(21285,"Infernal max cape"));
        BankSearchIndex.Match result=bank(items,Collections.emptyList()).search("toa melee cape","");
        assertTrue(result.ids.containsAll(Arrays.asList(6570,13329,21295,21285)));
        assertEquals(result.ranks.get(6570).tier,result.ranks.get(13329).tier);
        assertEquals(result.ranks.get(21295).tier,result.ranks.get(21285).tier);
        assertFalse(CombatEquivalence.matches(item(13329,"Fire max cape"),"max cape"));
    }
    @Test public void geRemainsExplicitlyOptInAndBothViewsUseOrdinalColours() throws Exception {
        WindowMarkersConfig config=new WindowMarkersConfig(){};
        assertFalse(config.geCatalogueSearch());
        ConfigItem option=WindowMarkersConfig.class.getMethod("geCatalogueSearch").getAnnotation(ConfigItem.class);
        assertEquals("Enable Grand Exchange search",option.name());assertEquals("geCatalogueSearch",option.keyName());
        assertEquals("1st",TierPresentation.ordinal(1));assertEquals("2nd",TierPresentation.ordinal(2));assertEquals("3rd",TierPresentation.ordinal(3));
        assertEquals("",TierPresentation.ordinal(0));assertEquals("",TierPresentation.ordinal(4));
        assertEquals(new Color(138,43,226),TierPresentation.colour(config,1));
        assertEquals(new Color(0,102,170),TierPresentation.colour(config,2));
        assertEquals(new Color(0,119,68),TierPresentation.colour(config,3));
    }
    @Test public void usableDepletedToolsDoNotInheritMissingChargeEffects() {
        BankSearchIndex index=bank(Arrays.asList(item(21033,"Infernal harpoon (uncharged)"),item(21031,"Infernal harpoon"),
            item(23675,"Crystal axe (inactive)"),item(23682,"Crystal pickaxe (inactive)"),item(13242,"Infernal axe (uncharged)"),
            item(25539,"Celestial ring (uncharged)"),item(28329,"Ring of shadows (uncharged)")),Collections.emptyList());
        assertEquals(0,index.search("tempoross weapon","").ranks.get(21033).tier);
        assertEquals(1,index.search("tempoross weapon","").ranks.get(21031).tier);
        assertEquals(0,index.search("sulliuscep weapon","").ranks.get(23675).tier);
        assertEquals(0,index.search("guardians of the rift ring","").ranks.get(25539).tier);
        assertEquals(1,index.search("cox guardians weapon","").ranks.get(23682).tier);
        assertEquals(1,index.search("wintertodt weapon","").ranks.get(13242).tier);
        assertTrue(index.search("adamant dragon ring","").ranks.get(28329).tier>0);
    }
}
