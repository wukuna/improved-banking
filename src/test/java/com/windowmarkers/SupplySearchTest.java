package com.windowmarkers;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class SupplySearchTest
{
    private static ItemCatalogueIndex.Item item(int id,String name) { return GeSearchIndexTest.item(id,name,-1); }
    private static Set<Integer> ids(ItemCatalogueIndex.Result result) {
        assertNotNull(result);Set<Integer> ids=new HashSet<>();result.items.forEach(i->ids.add(i.id));return ids;
    }
    private static List<ItemCatalogueIndex.Item> foods() { return Arrays.asList(item(385,"Shark"),item(383,"Raw shark"),
        item(3144,"Cooked karambwan"),item(13441,"Anglerfish"),item(391,"Manta ray"),item(7218,"Summer pie"),
        item(7220,"Half a summer pie"),item(1891,"Cake"),item(1895,"Slice of cake"),item(2205,"Worm crunchies"),
        item(32336,"Halibut"),item(29143,"Cooked moonlight antelope"),item(24592,"Blighted anglerfish"),
        item(2428,"Attack potion(4)"),item(1971,"Kebab"),item(29900,"Varlamorian kebab"),item(5982,"Watermelon")); }
    @Test public void cookedRawFastAndComboHaveDistinctMembership() {
        ItemCatalogueIndex index=new ItemCatalogueIndex(foods());
        Set<Integer> food=ids(index.find("food",""));
        assertTrue(food.containsAll(Arrays.asList(385,3144,7218,2205)));
        assertFalse(food.contains(383));assertFalse(food.contains(2428));assertFalse(food.contains(5982));
        assertEquals(food,ids(index.find("cooked foods","")));
        assertEquals(Collections.singleton(383),ids(index.find("raw foods","")));
        Set<Integer> fast=ids(index.find("fast foods","")),combo=ids(index.find("combo food",""));
        assertTrue(fast.containsAll(Arrays.asList(3144,2205,32336,7218,1891)));
        assertFalse(fast.contains(1895));assertFalse(fast.contains(385));
        assertTrue(combo.containsAll(Arrays.asList(3144,2205,32336)));assertFalse(combo.contains(7218));
    }
    @Test public void thresholdUsesOneBiteAndBaseHpInsteadOfPieTotalsOrGearTiers() {
        ItemCatalogueIndex index=new ItemCatalogueIndex(foods());
        Set<Integer> high=ids(index.find("food 20","","",99));
        assertTrue(high.containsAll(Arrays.asList(385,391,13441,32336,29143)));
        assertFalse(high.contains(7218));assertFalse(high.contains(3144));assertFalse(high.contains(383));
        assertFalse(ids(index.find("food 20","","",80)).contains(13441));
        assertTrue(ids(index.find("food 22","","",93)).contains(13441));
        assertFalse(ids(index.find("food 22","","",92)).contains(13441));
        assertFalse(ids(index.find("food 1","" )).isEmpty());
        assertTrue(SearchQuery.parse("food 1").tiers.isEmpty());
        assertEquals(Collections.singleton(1),SearchQuery.parse("toa head 1").tiers);
        assertTrue(ids(index.find("food 999","" )).isEmpty());
        assertFalse(high.contains(1971));assertFalse(high.contains(29900));
    }
    @Test public void delayedRestrictedAndMultiBiteAreExplicit() {
        ItemCatalogueIndex index=new ItemCatalogueIndex(foods());
        assertTrue(ids(index.find("delayed food 20","" )).contains(29143));
        assertFalse(ids(index.find("instant food 20","" )).contains(29143));
        assertTrue(index.find("food 20","").ranks.get(29143).explanation.contains("delayed"));
        assertEquals(Collections.singleton(24592),ids(index.find("wilderness food","")));
        assertFalse(ids(index.find("unrestricted food","")).contains(24592));
        assertTrue(ids(index.find("multi bite food","")).contains(7218));
        assertFalse(ids(index.find("multi bite food","")).contains(7220));
        assertEquals(new HashSet<>(Arrays.asList(13441,24592)),ids(index.find("overhealing food","")));
    }
    @Test public void bankAndGeUseTheSameResultsEvenWhenSavedGroupNamesOverlap() {
        List<AutoCategories.Item> owned=new ArrayList<>();foods().forEach(i->owned.add(i.fact));
        Marker misleading=new Marker(0,0,48,36);misleading.label="food 20";misleading.linkedItemIds=new int[]{383};
        BankSearchIndex bank=new BankSearchIndex(owned,Collections.singletonList(misleading),new CustomPresets(),80);
        ItemCatalogueIndex ge=new ItemCatalogueIndex(foods());
        for(String query:Arrays.asList("food","raw food","food 1","food 20","fast foods","combo food","delayed food","overhealing food")) {
            BankSearchIndex.Match match=bank.search(query,"","");
            assertEquals(query,ids(ge.find(query,"","",80)),match.ids);assertFalse(match.slots);assertNull(match.loadout);
        }
    }
    @Test public void effectFiltersPreserveDosesAndExcludeUnfinishedPotionsAndUnrelatedEffects() {
        ItemCatalogueIndex index=new ItemCatalogueIndex(Arrays.asList(item(2434,"Prayer potion(4)"),item(143,"Prayer potion(1)"),
            item(3024,"Super restore(4)"),item(2430,"Restore potion(4)"),item(99,"Ranarr potion (unf)"),
            item(2446,"Antipoison(4)"),item(12905,"Anti-venom(4)"),item(21978,"Dragon bolts")));
        assertEquals(new HashSet<>(Arrays.asList(2434,143,3024)),ids(index.find("prayer potions","")));
        assertEquals(new HashSet<>(Arrays.asList(3024,2430)),ids(index.find("stat restore potions","")));
        assertEquals(Collections.singleton(12905),ids(index.find("venom cure potions","")));
        assertEquals(new HashSet<>(Arrays.asList(2446,12905)),ids(index.find("poison cure potions","")));
    }
    @Test public void ammoSeparatesWeaponLimitsSpecialBoltsAndPoisonedDarts() {
        ItemCatalogueIndex index=new ItemCatalogueIndex(Arrays.asList(item(11230,"Dragon dart"),item(11234,"Dragon dart(p++)"),
            item(9242,"Ruby bolts (e)"),item(9244,"Dragonstone bolts (e)"),item(21944,"Ruby dragon bolts (e)"),
            item(21905,"Dragon bolts"),item(9140,"Iron bolts"),item(9236,"Opal bolts (e)"),item(8882,"Bone bolts"),
            item(4740,"Bolt rack"),item(11212,"Dragon arrow"),item(892,"Rune arrow")));
        assertEquals(Collections.singleton(11230),ids(index.find("blowpipe ammo","")));
        assertEquals(new HashSet<>(Arrays.asList(9242,9244,9140,9236)),ids(index.find("rcb ammo","")));
        assertTrue(ids(index.find("zcb ammo","")).contains(21944));
        assertFalse(ids(index.find("rcb ammo","")).contains(21944));
        assertEquals(new HashSet<>(Arrays.asList(9140,8882)),ids(index.find("dorgeshuun crossbow ammo","")));
        assertEquals(Collections.singleton(4740),ids(index.find("karils ammo","")));
        assertEquals(Collections.singleton(892),ids(index.find("magic shortbow ammo","")));
        assertEquals(new HashSet<>(Arrays.asList(11212,892)),ids(index.find("tbow ammo","")));
        assertTrue(ids(index.find("bowfa ammo","")).isEmpty());
    }
    @Test public void newGroupsCanBeFoundInLegendAndAppliedToItems() {
        AutoCategories.Item food=item(3144,"Cooked karambwan").fact;
        List<BankCatalogue.Choice> choices=BankCatalogue.choices(Collections.singletonList(food),Collections.emptyList(),new CustomPresets());
        assertFalse(BankCatalogue.search(choices,"combo eating",20).isEmpty());
        Map<String,List<AutoCategories.Item>> groups=BankCatalogue.categoryGroups("Food — combo eating",Collections.singletonList(food));
        assertEquals(3144,groups.get("Food — combo eating").get(0).id);
        assertTrue(BankCatalogue.memberships(food,Collections.singletonList(food),Collections.emptyList(),new CustomPresets()).contains("Category: Food — combo eating"));
    }
}
