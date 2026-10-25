package com.windowmarkers;

import com.google.gson.Gson;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class GroupCatalogueTest
{
    @Test public void categorySearchPreservesEveryDocumentedVariantNote() {
        int checked=0;
        for(String[] row:ItemTaxonomy.readRows("item-variant-states.tsv",5)) {
            AutoCategories.Item item=new AutoCategories.Item(Integer.parseInt(row[0]),row[1],true,null,0,0,0);
            if(ItemVariantState.identityNeedsReview(item)) continue;
            for(WikiCatalogue.Entry entry:WikiCatalogue.entries(item)) {
                Map<String,List<AutoCategories.Item>> groups=BankCatalogue.categoryGroups(entry.group,Collections.singletonList(item));
                String expected=ItemVariantState.annotate(entry.group,item);
                assertTrue(row[0]+" / "+entry.group,groups.containsKey(expected));
                assertEquals(1,groups.values().stream().mapToInt(List::size).sum());
                // Lookup emits the annotated secondary role, not necessarily
                // the single primary category chosen by auto-categorization.
                Map<String,List<AutoCategories.Item>> clicked=BankCatalogue.categoryGroups(expected,Collections.singletonList(item));
                assertEquals(row[0]+" / "+expected,Collections.singleton(expected),clicked.keySet());
                assertEquals(item.id,clicked.get(expected).get(0).id);
                checked++;
            }
        }
        assertTrue("Exercise the bundled state catalogue",checked>1000);
    }
    @Test public void categorySearchDoesNotMixChargedAndEmptyToolsOrDuplicateNotes() {
        List<AutoCategories.Item> items=Arrays.asList(
            new AutoCategories.Item(5331,"Watering can",false,null,0,0,0),
            new AutoCategories.Item(5340,"Watering can(8)",false,null,0,0,0));
        String category=WikiCatalogue.entries(items.get(0)).stream().filter(e -> e.group.startsWith("Farming")).findFirst().get().group;
        Map<String,List<AutoCategories.Item>> groups=BankCatalogue.categoryGroups(category,items);
        assertEquals(2,groups.size());
        assertTrue(groups.values().stream().allMatch(list -> list.size()==1));
        String annotated=AutoCategories.suggest(AutoCategories.Preset.ALL_CATEGORIES,items).keySet().iterator().next();
        Map<String,List<AutoCategories.Item>> exact=BankCatalogue.categoryGroups(annotated,items);
        assertEquals(Collections.singleton(annotated),exact.keySet());
    }
    private Marker marker(String label,int... ids) {
        Marker m=new Marker(0,0,48,36); m.label=label; m.linkedItemIds=ids; return m;
    }
    @Test public void fuzzySearchFindsTyposPunctuationAndRaidAliases() {
        assertTrue(SearchText.score("Guthan’s warspear","guthans")<Integer.MAX_VALUE);
        assertTrue(SearchText.score("Herblore — ingredients","herblroe")<Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE,SearchText.score("Magic weapons","woodcutting"));
        List<BankCatalogue.Choice> choices=BankCatalogue.choices(Collections.emptyList(),Collections.emptyList(),new CustomPresets());
        assertTrue(BankCatalogue.search(choices,"cox",150).stream().anyMatch(c -> c.kind==BankCatalogue.Kind.PRESET));
        assertTrue(BankCatalogue.search(choices,"herblroe",150).stream().anyMatch(c -> c.kind==BankCatalogue.Kind.CATEGORY));
        assertTrue(choices.stream().anyMatch(c -> c.kind==BankCatalogue.Kind.WIKI_SETUP));
        for(String query:new String[]{"herb","herbl","herblor","herblroe","ingredients herblroe"})
            assertTrue(query,SearchText.score("Herblore — ingredients",query)<Integer.MAX_VALUE);
        assertTrue(SearchText.score("Ancestral hat","ancsetr")<Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE,SearchText.score("Magic equipment","axe"));
        assertEquals(Integer.MAX_VALUE,SearchText.score("Herblore","x".repeat(300)));
    }
    @Test public void palettePreservesSameNameVariantsAndBankScope() {
        var a=new BankItemSearch.Choice(1409,"Iban's staff"); var b=new BankItemSearch.Choice(1410,"Iban's staff");
        var c=new BankItemSearch.Choice(6570,"Fire cape");
        var all=BankItemSearch.rank(Arrays.asList(a,b,c),"iban staf",null);
        assertEquals(Arrays.asList(a,b),all);
        assertEquals(Collections.singletonList(b),BankItemSearch.rank(Arrays.asList(a,b,c),"iban staf",Collections.singleton(1410)));
        assertEquals(Collections.emptyList(),BankItemSearch.rank(Arrays.asList(a,b,c),"",Collections.emptySet()));
    }
    @Test public void draggingFilteredGroupsKeepsHiddenSlotsAndRejectsStaleOrder() {
        Marker a=marker("A",1),hidden=marker("H",9),b=marker("B",2),c=marker("C",3);
        List<Marker> all=new ArrayList<>(Arrays.asList(a,hidden,b,c));
        assertTrue(GroupEdits.reorder(all,Arrays.asList(a,b,c),Arrays.asList(a,c),3));
        assertEquals(Arrays.asList(b,hidden,a,c),all);
        assertFalse(GroupEdits.reorder(all,Arrays.asList(a,b,c),Collections.singletonList(a),3));
        assertFalse(GroupEdits.reorder(all,Arrays.asList(b,a,c),Collections.singletonList(marker("stale",1)),0));
        assertFalse(GroupEdits.reorder(all,Arrays.asList(b,a,c),Collections.singletonList(c),3));
    }
    @Test public void disconnectedGroupsMergeIdsWithoutCollapsingVariants() {
        assertArrayEquals(new int[]{1409,1410,6570},GroupEdits.union(Arrays.asList(marker("one",1409,1410),marker("two",1410,6570))));
    }
    @Test public void presetRoundTripKeepsAppearanceAndCopiesAreIndependent() {
        Marker group=marker("My Food",385,3144); group.borderArgb=0xff119933; group.labelSize=18;
        CustomPresets.Preset p=new CustomPresets.Preset(); p.name="Supplies"; p.groups.add(group);
        CustomPresets library=new CustomPresets(); library.presets.add(p); Gson gson=new Gson();
        CustomPresets copied=gson.fromJson(gson.toJson(library),CustomPresets.class); copied.normalize();
        assertEquals(group.borderArgb,copied.presets.get(0).groups.get(0).borderArgb);
        copied.presets.get(0).groups.get(0).linkedItemIds[0]=999;
        assertEquals(385,group.linkedItemIds[0]);
        CustomPresets.Preset duplicate=new CustomPresets.Preset(); duplicate.name=" Supplies "; duplicate.groups.add(group);
        copied.presets.add(duplicate); copied.normalize(); assertEquals(1,copied.presets.size());
    }
    @Test public void lookupIncludesSecondaryRolesAndCustomMembershipWithoutInferringReadiness() {
        var item=new AutoCategories.Item(13329,"Fire max cape",true,null,0,0,0);
        var groups=Collections.singletonList(marker("My capes",13329));
        var membership=BankCatalogue.memberships(item,Collections.singletonList(item),groups,new CustomPresets());
        assertTrue(membership.stream().anyMatch(s -> s.contains("Runecraft — pouch protection")));
        assertTrue(membership.contains("Saved group: My capes"));
        assertTrue(membership.stream().noneMatch(s -> s.contains("Crafting Guild teleport")));
        var broken=new AutoCategories.Item(1410,"Iban's staff",true,null,0,0,0);
        var brokenMembership=BankCatalogue.memberships(broken,Collections.singletonList(broken),Collections.emptyList(),new CustomPresets());
        assertTrue(brokenMembership.stream().anyMatch(s -> s.contains("review")));
    }
    @Test public void comparisonSeparatesSharedAndUniqueMemberships() {
        String output=BankCatalogue.compare("A",new HashSet<>(Arrays.asList("food","cooking")),"B",new HashSet<>(Arrays.asList("food","fishing")));
        assertTrue(output.contains("Shared (1)\nfood\n"));
        assertTrue(output.contains("Only A (1)\ncooking\n"));
        assertTrue(output.contains("Only B (1)\nfishing\n"));
    }
    @Test public void focusedLookupRetainsWholeBankSetContext() {
        List<AutoCategories.Item> bank=Arrays.asList(
            new AutoCategories.Item(13072,"Elite void top",true,null,0,0,0),
            new AutoCategories.Item(13073,"Elite void robe",true,null,0,0,0),
            new AutoCategories.Item(8842,"Void knight gloves",true,null,0,0,0),
            new AutoCategories.Item(11664,"Void ranger helm",true,null,0,0,0),
            new AutoCategories.Item(28997,"Dual macuahuitl",true,null,0,0,0),
            new AutoCategories.Item(385,"Shark",false,new String[]{"Eat"},0,0,0));
        for(AutoCategories.Item item:bank) {
            Set<String> expected=new TreeSet<>();
            for(AutoCategories.Preset preset:AutoCategories.Preset.values())
                AutoCategories.suggest(preset,bank).forEach((group,items) -> {
                    if(items.stream().anyMatch(i -> i.id==item.id)) expected.add("Preset: "+preset+" / "+group);
                });
            Set<String> actual=BankCatalogue.memberships(item,bank,Collections.emptyList(),new CustomPresets());
            actual.removeIf(s -> !s.startsWith("Preset:")); assertEquals(item.name,expected,actual);
        }
    }
}
