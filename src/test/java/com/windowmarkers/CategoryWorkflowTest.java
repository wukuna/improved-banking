package com.windowmarkers;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.List;
import javax.swing.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class CategoryWorkflowTest
{
    private AutoCategories.Item item(int id,String name) { return new AutoCategories.Item(id,name,false,null,0,0,0); }

    @Test public void lookupClickAddsColoredSecondaryGroupEvenWhenItemIsAlreadyGrouped() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            AutoCategories.Item empty=item(5331,"Watering can");
            String category=WikiCatalogue.entries(empty).stream().filter(e -> e.group.startsWith("Farming")).findFirst().get().group;
            String membership="Category: "+ItemVariantState.annotate(category,empty);
            ItemLookupPanel lookup=new ItemLookupPanel(Collections.singletonList(empty.name),Collections.singletonList(Collections.singleton(membership)));
            lookup.list.setSelectedIndex(0); lookup.send.doClick();
            assertEquals(membership,lookup.membership);
            List<BankCatalogue.Choice> targets=BankCatalogue.targets(lookup.membership,Collections.emptyList());
            assertEquals(1,targets.size());
            Map<String,List<AutoCategories.Item>> groups=BankCatalogue.categoryGroups(targets.get(0),Collections.singletonList(empty));
            Marker first=new Marker(0,0,48,36); first.label="My tools"; first.linkedItemIds=new int[]{5331}; first.borderArgb=Color.CYAN.getRGB();
            List<Marker> markers=new ArrayList<>(Collections.singletonList(first));
            CategoryGroups.Result added=CategoryGroups.add(markers,groups,"All classifications",true,Color.YELLOW,250);
            assertEquals(1,added.added); assertEquals(2,markers.size());
            Marker fresh=added.selected.get(0);
            assertArrayEquals(new int[]{5331},fresh.linkedItemIds);
            assertNotEquals(first.borderArgb,fresh.borderArgb);
            assertEquals(fresh.borderArgb & 0xffffff,fresh.backgroundArgb & 0xffffff);
            assertEquals(65,new Color(fresh.backgroundArgb,true).getAlpha());
            assertTrue(fresh.category.contains("fill before watering"));
            MarkerLegend legend=new MarkerLegend(m -> {},m -> {});
            List<MarkerLegend.Entry> rows=Arrays.asList(new MarkerLegend.Entry(first,first.label,Color.CYAN),
                new MarkerLegend.Entry(fresh,fresh.label,new Color(fresh.borderArgb,true)));
            legend.replace(rows); legend.search.setText("unrelated search"); legend.showGroups(added.selected);
            assertEquals(Collections.singletonList(fresh),legend.selected());
            assertEquals(fresh.borderArgb.intValue(),legend.list.getSelectedValue().color.getRGB());
            int color=fresh.borderArgb;
            CategoryGroups.Result repeated=CategoryGroups.add(markers,groups,"All classifications",true,Color.YELLOW,250);
            assertEquals(0,repeated.added); assertEquals(1,repeated.existing); assertSame(fresh,repeated.selected.get(0));
            assertEquals(color,fresh.borderArgb.intValue()); legend.close();
        });
    }
    @Test public void bulkDefaultKeepsExistingGroupsAndReportsSkippedMemberships() {
        Marker existing=new Marker(0,0,48,36); existing.linkedItemIds=new int[]{385}; existing.borderArgb=Color.BLUE.getRGB();
        List<Marker> markers=new ArrayList<>(Collections.singletonList(existing));
        CategoryGroups.Result result=CategoryGroups.add(markers,Collections.singletonMap("Food",Collections.singletonList(item(385,"Shark"))),"Broad",false,Color.RED,250);
        assertEquals(0,result.added); assertEquals(1,result.skipped); assertEquals(1,markers.size());
    }
    @Test public void presetMembershipKeepsActivityScopeAndReviewRowsCannotBeAdded() throws Exception {
        String membership="Preset: "+AutoCategories.Preset.THEATRE_OF_BLOOD+" / ToB Magic equipment";
        var target=BankCatalogue.targets(membership,Collections.emptyList()).get(0);
        assertEquals(AutoCategories.Preset.THEATRE_OF_BLOOD,target.presetScope);
        assertEquals("ToB Magic equipment",target.name);
        assertTrue(BankCatalogue.targets("Review: Unknown identity",Collections.emptyList()).isEmpty());
        SwingUtilities.invokeAndWait(() -> {
            ItemLookupPanel lookup=new ItemLookupPanel(Collections.singletonList("Unknown"),Collections.singletonList(Collections.singleton("Review: Unknown identity")));
            lookup.list.setSelectedIndex(0); assertFalse(lookup.send.isEnabled()); assertTrue(lookup.highlight.isEnabled());
        });
    }
    @Test public void lookupResolvesExactSavedCustomAndWikiTargetsWithoutFuzzyGuessing() {
        Marker marker=new Marker(0,0,48,36); marker.label="My food / potions"; marker.linkedItemIds=new int[]{385};
        CustomPresets.Preset preset=new CustomPresets.Preset(); preset.name="Raids"; preset.groups.add(marker);
        CustomPresets custom=new CustomPresets(); custom.presets.add(preset);
        List<BankCatalogue.Choice> all=BankCatalogue.choices(Collections.emptyList(),Collections.singletonList(marker),custom);
        assertSame(marker,BankCatalogue.targets("Saved group: "+marker.label,all).get(0).value);
        var target=BankCatalogue.targets("Custom preset: Raids / "+marker.label,all).get(0);
        assertSame(preset,target.value); assertEquals(marker.label,target.customGroup);
        ActivitySetups.Setup setup=ActivitySetups.all().iterator().next();
        assertTrue(BankCatalogue.targets("Wiki setup: "+setup.activity+" / "+setup.label+" — state needs review",all).stream().anyMatch(c -> c.kind==BankCatalogue.Kind.ACTIVITY && c.name.equals(setup.activity)));
        assertTrue(BankCatalogue.targets("Saved group: My fod / potions",all).isEmpty());
    }
    @Test public void legendSuggestionEnterPassesTheActualChoiceRatherThanSearchingItsNameAgain() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            BankCatalogue.Choice choice=new BankCatalogue.Choice(BankCatalogue.Kind.CATEGORY,"My exact category","My exact category");
            BankCatalogue.Choice[] chosen={null};
            MarkerLegend legend=new MarkerLegend(m -> {},m -> {},new MarkerLegend.Actions() {
                public void edit(Marker m) {} public void items(Marker m) {} public void delete(List<Marker> m) {}
                public void clear(List<Marker> m) {} public void browse(String q) { fail("Must not reopen a fuzzy search"); }
                public void choose(BankCatalogue.Choice c) { chosen[0]=c; }
                public void inspect() {} public void reorder(List<Marker> a,List<Marker> b,int i) {} public void transform(Marker m,boolean r) {}
            });
            legend.catalogue(Collections.singletonList(choice)); legend.search.setText("exact"); legend.filter();
            JList<?> suggestions=findSuggestions(legend); assertNotNull(suggestions); suggestions.setSelectedIndex(0);
            suggestions.getActionMap().get("browse").actionPerformed(new ActionEvent(suggestions,0,"browse"));
            assertSame(choice,chosen[0]); legend.close();
        });
    }
    private JList<?> findSuggestions(Container c) {
        for(Component child:c.getComponents()) {
            if(child instanceof JList && ((JList<?>)child).getModel().getSize()>0
                && ((JList<?>)child).getModel().getElementAt(0) instanceof BankCatalogue.Choice) return (JList<?>)child;
            if(child instanceof Container) { JList<?> found=findSuggestions((Container)child); if(found!=null) return found; }
        }
        return null;
    }
}
