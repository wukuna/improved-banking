package com.windowmarkers;

import java.util.*;
import java.util.stream.Collectors;
import org.junit.Test;
import static org.junit.Assert.*;

public class ActivityMenuTest
{
    @Test public void explicitActivityNamesWorkWhenTheirNamesakeItemIsOwned() {
        for(boolean ge:new boolean[]{false,true}) {
            ItemCatalogueIndex index=new ItemCatalogueIndex(Arrays.asList(
                GeSearchIndexTest.item(6311,"Gout tuber",-1),GeSearchIndexTest.item(975,"Machete",3)),ge);
            assertNull(index.find("Gout tuber",""));
            ItemCatalogueIndex.Result gathering=index.find("Gout tuber gathering","");
            assertNotNull(gathering); assertTrue(gathering.activity);
            assertEquals(ids(ActivitySetups.forActivity("Gout tuber")),ids(gathering.setups));
        }
    }
    @Test public void activityBrowsingRetainsPersonalViewsAndExactArchivedMemberships() {
        Marker group=new Marker(0,0,48,36); group.label="Hespori (Echo)"; group.linkedItemIds=new int[]{385};
        CustomPresets saved=new CustomPresets(); CustomPresets.Preset preset=new CustomPresets.Preset();
        preset.name="Farming tools and supplies"; preset.groups.add(group); saved.presets.add(preset);
        List<BankCatalogue.Choice> all=BankCatalogue.choices(Collections.emptyList(),Collections.singletonList(group),saved);
        List<BankCatalogue.Choice> menu=ActivityMenu.choices(all);
        assertEquals(102,menu.stream().filter(c->c.kind==BankCatalogue.Kind.ACTIVITY).count());
        assertEquals(104,menu.size());
        assertTrue(menu.stream().anyMatch(c->c.value==preset)); assertTrue(menu.stream().anyMatch(c->c.value==group));
        assertTrue(all.stream().anyMatch(c->c.kind==BankCatalogue.Kind.CATEGORY));
        for(String original:Arrays.asList("Hespori (Echo)","Slayer task/Fossil Island wyverns","Barbarian Assault/Advanced strategies")) {
            assertFalse(menu.stream().anyMatch(c->c.kind==BankCatalogue.Kind.ACTIVITY && c.name.equals(original)));
            assertEquals(original,BankCatalogue.targets("Activity: "+original,all).get(0).viewQuery());
            assertFalse(ActivitySetups.forActivity(original).isEmpty());
        }
        assertEquals("Farming tools and supplies",BankCatalogue.targets("Category: Farming tools and supplies",all).get(0).name);
        assertArrayEquals(new int[]{385},group.linkedItemIds);
    }
    @Test public void everyVisibleActivityResolvesAndRenamedQueriesRetainItsSources() {
        ItemCatalogueIndex index=new ItemCatalogueIndex(Collections.emptyList(),false);
        int renamed=0;
        for(BankCatalogue.Choice choice:ActivityMenu.activities()) {
            ItemCatalogueIndex.Result result=index.find(choice.viewQuery(),"");
            assertNotNull(choice.name,result); assertTrue(choice.name,result.activity);
            assertFalse(choice.name,result.setups.isEmpty());
            if(!choice.name.equals(choice.viewQuery())) {
                renamed++;
                ItemCatalogueIndex.Result alias=index.find(choice.name,"");
                assertNotNull(choice.name,alias);
                assertEquals(choice.name,ids(result.setups),ids(alias.setups));
                assertTrue(BankCatalogue.search(ActivityMenu.activities(),choice.viewQuery(),150).stream().anyMatch(c->c.name.equals(choice.name)));
            }
        }
        assertEquals(13,renamed);
        ItemCatalogueIndex.Result challenge=index.find("Chambers of Xeric — Challenge Mode","");
        assertTrue(challenge.setups.stream().allMatch(s->s.activity.equals("Chambers of Xeric/Challenge Mode")));
        assertTrue(index.find("Abyss runecrafting","").setups.stream().allMatch(s->s.activity.equals("Abyss")));
    }
    @Test public void mergedActivitiesRetainMethodsAndSelectedGuideConditions() {
        ItemCatalogueIndex index=new ItemCatalogueIndex(Collections.emptyList(),false);
        for(String activity:Arrays.asList("Barbarian Assault","Fossil Island Wyverns")) {
            List<ActivitySetups.Setup> methods=ActivityMenu.setups(activity);
            assertEquals(2,methods.stream().map(s->s.activity).distinct().count());
            assertEquals(ids(methods),ids(index.find(activity,"").setups));
            for(ActivitySetups.Setup method:methods) {
                assertSame(method,ActivitySetups.byId(method.id));
                assertEquals(Collections.singleton(method.id),ids(index.find(activity,method.id).setups));
            }
        }
        List<BankCatalogue.Choice> found=BankCatalogue.search(ActivityMenu.activities(),"barbarian assault advanced",10);
        assertEquals("Barbarian Assault",found.get(0).name);
        ItemCatalogueIndex.Result wyverns=index.find("Fossil Island Wyverns","");
        assertNotNull(wyverns.loadout);
        assertTrue(wyverns.loadout.sections().stream().anyMatch(p->p.sectionName.startsWith("Slayer task /")));
    }
    private static Set<String> ids(List<ActivitySetups.Setup> setups) {
        return setups.stream().map(s->s.id).collect(Collectors.toSet());
    }
}
