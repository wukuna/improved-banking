package com.windowmarkers;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class GeSearchIndexTest
{
    @Test public void explicitlySelectedActivityIsNotBlockedByAnIdenticalItemName() {
        for(boolean ge:new boolean[]{false,true}) {
            ItemCatalogueIndex index=new ItemCatalogueIndex(Arrays.asList(item(6311,"Gout tuber",-1),item(975,"Machete",3)),ge);
            assertNull(index.find("Gout tuber",""));
            assertNotNull(index.find("Gout tuber","291214:1"));
            assertTrue(index.find("Gout tuber","291214:1").activity);
            assertNull(index.find("Gout tuber","41794:1"));
        }
    }

    @Test public void refreshedGuideColumnsAgreeInBankAndGeWithoutPromotingMissingTopTier() {
        List<ItemCatalogueIndex.Item> items=Arrays.asList(item(12817,"Elysian spirit shield",5),item(3839,"Damaged book",5),
            item(22325,"Scythe of vitur",3),item(22486,"Scythe of vitur (uncharged)",3),item(26219,"Osmumten's fang",3),
            item(26374,"Zaryte crossbow",3),item(11785,"Armadyl crossbow",3));
        for(boolean ge:new boolean[]{false,true}) {
            ItemCatalogueIndex index=new ItemCatalogueIndex(items,ge);
            ItemCatalogueIndex.Result z=index.find("zalcano shield","227925:1");
            // Current Zalcano guide promotes Elysian to its first column and books to third.
            assertEquals(1,z.ranks.get(12817).tier);
            assertEquals(3,z.ranks.get(3839).tier);
            ItemCatalogueIndex.Result melee=index.find("graardor weapon","41794:13");
            assertEquals(1,melee.ranks.get(22325).tier);
            assertEquals(0,melee.ranks.get(22486).tier);
            assertEquals(2,melee.ranks.get(26219).tier);
            ItemCatalogueIndex.Result ranged=index.find("graardor weapon","41794:15");
            assertEquals(0,ranged.ranks.get(26374).tier);
            assertEquals(0,ranged.ranks.get(11785).tier);
        }
    }

    static ItemCatalogueIndex.Item item(int id,String name,int slot)
    { return new ItemCatalogueIndex.Item(id,name,slot,new String[0]); }
    static ItemCatalogueIndex index()
    { return new ItemCatalogueIndex(Arrays.asList(item(21018,"Ancestral hat",0),item(26241,"Virtus mask",0),
        item(4708,"Ahrim's hood",0),item(4860,"Ahrim's hood 0",0),item(21021,"Ancestral robe top",4),
        item(11920,"Dragon pickaxe",3),item(385,"Shark",-1),item(12924,"Toxic blowpipe (empty)",3),
        item(11832,"Bandos chestplate",4),item(561,"Nature rune",-1))); }
    private Set<Integer> ids(ItemCatalogueIndex.Result result)
    { assertNotNull(result); Set<Integer> ids=new HashSet<>(); result.items.forEach(i->ids.add(i.id)); return ids; }

    @Test public void unstrungBowsRemainFletchingInputsInBankAndGeNotCombatWeapons()
    {
        int[] unstrungIds={48,50,54,56,58,60,62,64,66,68,70,72};
        String[] names={"Longbow (u)","Shortbow (u)","Oak shortbow (u)","Oak longbow (u)",
            "Willow longbow (u)","Willow shortbow (u)","Maple longbow (u)","Maple shortbow (u)",
            "Yew longbow (u)","Yew shortbow (u)","Magic longbow (u)","Magic shortbow (u)"};
        List<ItemCatalogueIndex.Item> catalogue=new ArrayList<>();
        for(int n=0;n<unstrungIds.length;n++) catalogue.add(item(unstrungIds[n],names[n],-1));
        catalogue.add(item(859,"Magic longbow",3));
        catalogue.add(item(12658,"Iban's staff (u)",3)); // Here (u) means upgraded, not unstrung.
        for(boolean ge:new boolean[]{false,true}) {
            ItemCatalogueIndex index=new ItemCatalogueIndex(catalogue,ge);
            Set<Integer> combat=ids(index.find("general combat",""));
            Set<Integer> fletching=ids(index.find("fletching",""));
            assertTrue(combat.contains(859));assertTrue(combat.contains(12658));
            for(int id:unstrungIds) { assertFalse("Unstrung ID "+id,combat.contains(id));assertTrue(fletching.contains(id)); }
            assertTrue(ids(index.find("vale totems","")).contains(70));
        }
    }

    @Test public void activityAliasesAndSlotsCoverMoreThanToa()
    {
        ItemCatalogueIndex index=index();
        assertTrue(ids(index.find("toa head","")).contains(21018));
        assertFalse(ids(index.find("toa head","")).contains(21021));
        assertEquals(ids(index.find("toa head","")),ids(index.find("tombs head","")));
        assertTrue(ids(index.find("cox head","")).contains(21018));
        assertTrue(ids(index.find("mining tools","")).contains(11920));
        assertTrue(ids(index.find("food","")).contains(385));
        assertFalse(ids(index.find("food","")).contains(21018));
    }
    @Test public void pinnedToaMagicTablePreservesFirstSecondThirdAndConditions()
    {
        ItemCatalogueIndex.Result result=index().find("toa magic head","");
        assertEquals(1,result.ranks.get(21018).tier);
        assertEquals(2,result.ranks.get(26241).tier);
        assertEquals(3,result.ranks.get(4708).tier);
        assertTrue(result.ranks.get(26241).explanation.contains("max hit"));
        assertEquals(0,result.ranks.get(4860).tier);
        assertTrue(result.ranks.get(4860).explanation.toLowerCase().contains("repair"));
    }
    @Test public void categoriesDoNotInheritActivityTiersAndEmptyWeaponsNeedPreparation()
    {
        ItemCatalogueIndex index=index();
        assertEquals(0,index.find("magic equipment","").ranks.get(21018).tier);
        ItemCatalogueIndex.Result result=index.find("toa ranged weapon","");
        assertTrue(ids(result).contains(12924));
        assertEquals(0,result.ranks.get(12924).tier);
    }
    @Test public void ordinaryNamesAndOtherPluginSyntaxPassThrough()
    {
        ItemCatalogueIndex index=index();
        for(String query:new String[]{"Ancestral hat","shark","tag:raid","ge>1000","","xx","nosuchcategoryzz"})
            assertNull(query,index.find(query,""));
    }
    @Test public void slotFallbackRequiresExactCurrentIdentity()
    {
        assertEquals(3,ItemCatalogueIndex.fallbackSlot(12924,"Toxic blowpipe (empty)"));
        assertEquals(-1,ItemCatalogueIndex.fallbackSlot(12924,"Different future item"));
    }
    @Test public void conflictingActivityRanksStayNeutralUntilOneSourceIsChosen()
    {
        ItemCatalogueIndex index=new ItemCatalogueIndex(Collections.singletonList(item(22978,"Dragon hunter lance",3)));
        assertEquals(0,index.find("cox weapon","").ranks.get(22978).tier);
        assertEquals(2,index.find("cox weapon","94147:23").ranks.get(22978).tier);
        // Missing/untradeable higher recommendations do not promote this item.
        assertEquals(5,index.find("cox weapon","94147:14").ranks.get(22978).tier);
    }
    @Test public void fuzzyCategoriesAndActivitySuppliesAreDiscoverable()
    {
        ItemCatalogueIndex index=new ItemCatalogueIndex(Arrays.asList(item(11920,"Dragon pickaxe",3),
            item(6685,"Saradomin brew(4)",-1),item(385,"Shark",-1)));
        assertTrue(ids(index.find("minign tools","")).contains(11920));
        assertTrue(ids(index.find("toa potions","")).contains(6685));
    }
    @Test public void exactFoodQueryDoesNotFuzzilyMatchWoodcutting()
    {
        ItemCatalogueIndex index=new ItemCatalogueIndex(Arrays.asList(item(6739,"Dragon axe",3),item(385,"Shark",-1)));
        assertEquals(Collections.singleton(385),ids(index.find("food","")));
    }
    @Test public void slotWordDoesNotRemoveOrdinaryItemNameMatches()
    {
        ItemCatalogueIndex index=new ItemCatalogueIndex(Collections.singletonList(item(40001,"Ensouled dragon head",-1)));
        assertTrue(ids(index.find("dragon head","")).contains(40001));
    }
    @Test public void everyRankReferencesAnExistingSetupAndExactMember()
    {
        Map<String,ActivitySetups.Setup> setups=new HashMap<>(); ActivitySetups.all().forEach(s->setups.put(s.id,s));
        for(String[] row:ItemTaxonomy.readRows("activity-setup-ranks.tsv",5)) {
            assertNotNull(row[0],setups.get(row[0]));
            assertTrue(row[0]+":"+row[1],setups.get(row[0]).itemIds.contains(Integer.parseInt(row[1])));
            assertTrue(Integer.parseInt(row[3])>=0);
        }
    }
    @Test public void preservesNativeUnsignedIdsAndOverflowInsteadOfTruncating()
    {
        List<ItemCatalogueIndex.Item> items=new ArrayList<>();
        for(int i=0;i<251;i++) items.add(item(40000+i,"Test helm "+i,0));
        ItemCatalogueIndex.Result result=new ItemCatalogueIndex(items).find("head","");
        assertEquals(251,result.items.size());
        assertEquals(40000,Short.toUnsignedInt(result.ids()[0]));
    }
    @Test public void catalogueScaleSearchReportsCostAndKeepsSlotConstraints()
    {
        List<ItemCatalogueIndex.Item> items=new ArrayList<>(); Set<Integer> seen=new HashSet<>();
        for(String[] row:ItemTaxonomy.readRows("ge-item-slots.tsv",3)) if(seen.add(Integer.parseInt(row[0])))
            items.add(item(Integer.parseInt(row[0]),row[2],Integer.parseInt(row[1])));
        long start=System.nanoTime(); ItemCatalogueIndex index=new ItemCatalogueIndex(items); long built=System.nanoTime();
        String[] queries={"toa head","tombs magic head","cox cape","vorkath ranged","mining tools","woodcutting tools","food","magic equipment","zulrah weapon","bandos body"};
        long max=0;
        for(int repeat=0;repeat<3;repeat++) for(String query:queries) {
            long before=System.nanoTime(); ItemCatalogueIndex.Result result=index.find(query,"");
            max=Math.max(max,System.nanoTime()-before);
            if(query.equals("toa head")) assertTrue(result.items.stream().allMatch(i->i.slot==0));
        }
        System.out.println("GE catalogue benchmark: "+items.size()+" equipment identities; build ms="+(built-start)/1e6+"; max query ms="+max/1e6);
    }
}
