package com.windowmarkers;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import net.runelite.api.*;
import net.runelite.api.widgets.Widget;
import net.runelite.client.game.*;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class SetupItemLayoutTest
{
    @Test public void equipmentAndInventoryUseSeparateHalvesAndAlternativesBelow() {
        var result=SetupLoadoutTest.catalogue().find("toa magic","364045:3","364045:9");
        Widget hat=cell(21018,"Ancestral hat",0,"Wear",51,0),alternative=cell(4708,"Ahrim's hood",0,"Wear",99,0),
            robe=cell(4712,"Ahrim's robetop",4,"Wear",147,0),brew=cell(6685,"Saradomin brew(4)",-1,"Drink",195,0),
            broken=cell(4860,"Ahrim's hood 0",0,"Check",243,0),restore=cell(3024,"Super restore(4)",-1,"Drink",291,0);
        Widget container=mock(Widget.class);when(container.getDynamicChildren()).thenReturn(new Widget[]{hat,alternative,robe,brew,broken,restore});
        List<SetupItemLayout.Heading> headings=new ArrayList<>();
        int[] ends=SetupItemLayout.apply(container,manager,8,48,4,result.loadout,result.ranks,headings);
        assertEquals(99,hat.getOriginalX());assertEquals(22,hat.getOriginalY());
        assertEquals(99,robe.getOriginalX());assertEquals(94,robe.getOriginalY());
        assertTrue(brew.getOriginalX()>=243);assertTrue(restore.getOriginalX()>=243);
        assertTrue(alternative.getOriginalY()>brew.getOriginalY());assertTrue(broken.getOriginalY()>alternative.getOriginalY());
        assertTrue(ends[1]>broken.getOriginalY());assertEquals("Equipment",headings.get(0).text);
        for(Widget cell:new Widget[]{hat,alternative,robe,brew,broken,restore}) { assertTrue(cell.getOriginalX()+36<=423);verify(cell,never()).setItemId(anyInt()); }
    }
    private ItemManager manager=mock(ItemManager.class);
    private Widget cell(int id,String name,int slot,String action,int x,int y) {
        ItemComposition definition=mock(ItemComposition.class);
        when(manager.canonicalize(id)).thenReturn(id); when(manager.getItemComposition(id)).thenReturn(definition);
        when(definition.getName()).thenReturn(name); when(definition.getInventoryActions()).thenReturn(new String[]{action});
        if(slot>=0) {
            ItemStats stats=new ItemStats(true,0,0,ItemEquipmentStats.builder().slot(slot).build());
            when(manager.getItemStats(id)).thenReturn(stats);
        }
        Widget w=mock(Widget.class); AtomicInteger px=new AtomicInteger(x),py=new AtomicInteger(y);
        when(w.getItemId()).thenReturn(id); when(w.getOriginalWidth()).thenReturn(36); when(w.getOriginalHeight()).thenReturn(32);
        when(w.getOriginalX()).thenAnswer(i -> px.get()); when(w.getOriginalY()).thenAnswer(i -> py.get());
        doAnswer(i -> {px.set(i.getArgument(0));return w;}).when(w).setOriginalX(anyInt());
        doAnswer(i -> {py.set(i.getArgument(0));return w;}).when(w).setOriginalY(anyInt()); return w;
    }
    @Test public void equipmentSlotsPrecedeSuppliesAndUnusableVariantsWithoutMutatingBankIdentity() {
        Widget food=cell(385,"Shark",-1,"Eat",51,0),broken=cell(4860,"Ahrim's hood 0",0,"Check",99,0),
            weapon=cell(4151,"Abyssal whip",3,"Wield",147,0),head=cell(21018,"Ancestral hat",0,"Wear",195,0),
            otherHead=cell(4089,"Mystic hat",0,"Wear",243,0),hidden=cell(1050,"Santa hat",0,"Wear",291,0);
        when(hidden.isSelfHidden()).thenReturn(true);
        Widget container=mock(Widget.class); when(container.getDynamicChildren()).thenReturn(new Widget[]{food,broken,weapon,head,otherHead,hidden});
        int[] ends=SetupItemLayout.apply(container,manager,4,48,4);
        assertEquals(36,ends[0]); assertEquals(148,ends[1]);
        assertEquals(4,head.getOriginalY()); assertEquals(4,otherHead.getOriginalY());
        assertEquals(51,head.getOriginalX()); assertEquals(99,otherHead.getOriginalX());
        assertEquals(40,weapon.getOriginalY()); assertEquals(76,food.getOriginalY()); assertEquals(112,broken.getOriginalY());
        verify(hidden,never()).setOriginalY(anyInt());
        for(Widget w:new Widget[]{food,broken,weapon,head,otherHead}) {
            verify(w,never()).setItemId(anyInt());
            verify(w,never()).setHidden(anyBoolean()); verify(w,never()).setOriginalWidth(anyInt());
        }
    }
    @Test public void resizedViewWrapsAlternativesWithinTheirSlotGroup() {
        Widget[] heads=new Widget[5]; for(int i=0;i<5;i++) heads[i]=cell(21018,"Ancestral hat",0,"Wear",51+i*48,0);
        Widget container=mock(Widget.class); when(container.getDynamicChildren()).thenReturn(heads);
        assertEquals(108,SetupItemLayout.apply(container,manager,2,48,0)[1]);
        assertEquals(72,heads[4].getOriginalY());
        // Simulate a native rebuild at wider width before the next arrangement.
        for(int i=0;i<5;i++) { heads[i].setOriginalX(51+i*48); heads[i].setOriginalY(0); }
        assertEquals(36,SetupItemLayout.apply(container,manager,8,48,0)[1]);
        assertEquals(0,heads[4].getOriginalY());
    }
}
