package com.windowmarkers;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.banktags.*;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class BankViewCopiesRegressionTest
{
    @Test public void presetKeepsEverySearchLayoutCellAfterTagLayoutDeduplication() {
        for(String activity:Arrays.asList("Alchemical Hydra","Callisto","Abyssal Sire")) {
            BankLayoutController c=new BankLayoutController();c.client=mock(Client.class);c.clientThread=mock(ClientThread.class);
            c.config=mock(WindowMarkersConfig.class);c.eventBus=mock(EventBus.class);c.itemManager=mock(ItemManager.class);
            when(c.config.bankLayoutEnabled()).thenReturn(true);when(c.config.setupSearchLayout()).thenReturn(true);
            when(c.itemManager.canonicalize(anyInt())).thenAnswer(a->a.getArgument(0));c.startUp();
            c.searchView=new BankSearchView();c.searchView.client=c.client;c.searchView.items=c.itemManager;
            c.searchView.bankTags=mock(BankTagsService.class);c.searchView.tags=mock(TagManager.class);
            doAnswer(a->{when(c.searchView.bankTags.getActiveTag()).thenReturn(a.getArgument(0));return null;})
                .when(c.searchView.bankTags).openBankTag(anyString(),anyInt());
            Widget parent=mock(Widget.class);List<Widget> children=new ArrayList<>();
            when(c.client.getWidget(InterfaceID.Bankmain.ITEMS)).thenReturn(parent);when(parent.getWidth()).thenReturn(478);
            when(parent.getDynamicChildren()).thenAnswer(a->children.toArray(new Widget[0]));
            when(parent.getChildren()).thenAnswer(a->children.toArray(new Widget[0]));
            when(parent.createChild(anyInt(),anyInt())).thenAnswer(a->{Widget copy=cell(children.size(),-1);children.add(copy);return copy;});
            Map<Integer,ItemCatalogueIndex.Item> bank=new LinkedHashMap<>();
            for(Object[] fact:new Object[][]{{31097,"Avernic treads (max)",10},{22947,"Rada's blessing 4",13},
                {11865,"Slayer helmet (i)",0},{6685,"Saradomin brew(4)",-1},{3024,"Super restore(4)",-1},
                {24589,"Blighted manta ray",-1},{24595,"Blighted karambwan",-1},{24598,"Blighted super restore(4)",-1}}) {
                int id=(int)fact[0];bank.put(id,GeSearchIndexTest.item(id,(String)fact[1],(int)fact[2]));
                var definition=mock(ItemComposition.class);when(definition.getName()).thenReturn((String)fact[1]);
                when(c.itemManager.getItemComposition(id)).thenReturn(definition);
            }
            SetupLoadout plan=SetupLoadout.build(ActivitySetups.forActivity(activity),bank,bank.keySet(),"");
            assertNotNull(plan);assertTrue(plan.sectionCount()>1);
            for(int id:plan.itemIds()) children.add(cell(children.size(),id));
            Widget excluded=cell(children.size(),2347);excluded.setHidden(true);children.add(excluded);
            Marker selected=new Marker(0,0,48,36);selected.linkedItemIds=plan.itemIds().stream().mapToInt(Integer::intValue).toArray();
            c.searchView.show(activity,Collections.singletonList(selected),true,plan,Collections.emptyMap());
            int[] stack=new int[9];when(c.client.getIntStack()).thenReturn(stack);when(c.client.getIntStackSize()).thenReturn(9);
            c.onScriptPreFired(new ScriptPreFired(ScriptID.BANKMAIN_FINISHBUILDING));
            List<Widget> expected=new ArrayList<>();Map<Widget,int[]> positions=new IdentityHashMap<>();
            for(Widget w:children) if(!w.isSelfHidden()) {expected.add(w);positions.put(w,new int[]{w.getOriginalX(),w.getOriginalY()});}
            assertTrue("Repeated items/previews must exist: "+activity,expected.size()>plan.itemIds().size()+5);
            EventBus bus=new EventBus();bus.register(c);
            // Bank Tag Layouts retains one widget per item ID and hides the rest.
            bus.register(ScriptPostFired.class,e->{Set<Integer> seen=new HashSet<>();for(Widget w:expected) {
                if(!seen.add(w.getItemId())) w.setHidden(true);
                else {w.setOriginalX(350);w.setOriginalY(900);}
            }},-1);
            bus.post(new ScriptPostFired(ScriptID.BANKMAIN_BUILD));
            for(Widget w:expected) {
                assertFalse(activity+" lost item display "+w.getItemId(),w.isSelfHidden());
                assertEquals(positions.get(w)[0],w.getOriginalX());assertEquals(positions.get(w)[1],w.getOriginalY());
            }
            assertTrue("Unrelated filtered item must stay hidden",excluded.isSelfHidden());
            c.searchView.clear();expected.get(0).setHidden(true);
            c.onScriptPostFired(new ScriptPostFired(ScriptID.BANKMAIN_BUILD));
            assertTrue("Switching tabs releases ownership",expected.get(0).isSelfHidden());
            verify(c.client,never()).runScript(any(Object[].class));
        }
    }
    private Widget cell(int index,int itemId) {
        Widget w=mock(Widget.class);AtomicInteger id=new AtomicInteger(itemId),x=new AtomicInteger(),y=new AtomicInteger();
        AtomicBoolean hidden=new AtomicBoolean();when(w.getIndex()).thenReturn(index);
        when(w.getItemId()).thenAnswer(a->id.get());doAnswer(a->{id.set(a.getArgument(0));return w;}).when(w).setItemId(anyInt());
        when(w.isSelfHidden()).thenAnswer(a->hidden.get());doAnswer(a->{hidden.set(a.getArgument(0));return w;}).when(w).setHidden(anyBoolean());
        when(w.getOriginalWidth()).thenReturn(36);when(w.getOriginalHeight()).thenReturn(32);
        when(w.getOriginalX()).thenAnswer(a->x.get());when(w.getOriginalY()).thenAnswer(a->y.get());
        doAnswer(a->{x.set(a.getArgument(0));return w;}).when(w).setOriginalX(anyInt());
        doAnswer(a->{y.set(a.getArgument(0));return w;}).when(w).setOriginalY(anyInt());return w;
    }
}
