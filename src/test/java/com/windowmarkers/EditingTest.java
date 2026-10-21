package com.windowmarkers;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class EditingTest
{
    @Test public void historySupportsUndoRedoBranchingAndBoundedMemory()
    {
        MarkerHistory h=new MarkerHistory(); h.reset("a"); h.record("b"); h.record("c");
        assertEquals("b",h.undo()); assertEquals("a",h.undo()); assertNull(h.undo());
        assertEquals("b",h.redo()); h.record("d"); assertNull(h.redo()); assertEquals("b",h.undo());
        h.reset("0"); for(int i=1;i<=60;i++) { h.record(""+i); }
        for(int i=59;i>=10;i--) { assertEquals(""+i,h.undo()); }
        assertNull(h.undo());
    }
    @Test public void moveAndResizeClampToWindowWithoutLosingMinimumSize()
    {
        Rectangle bounds=new Rectangle(100,100,300,300), original=new Rectangle(150,150,60,70);
        assertEquals(new Rectangle(340,330,60,70),WindowMarkersPlugin.transformRectangle(original,new Point(160,160),new Point(900,900),false,bounds));
        assertEquals(new Rectangle(150,150,250,250),WindowMarkersPlugin.transformRectangle(original,new Point(160,160),new Point(900,900),true,bounds));
        assertEquals(new Rectangle(150,150,16,16),WindowMarkersPlugin.transformRectangle(original,new Point(160,160),new Point(100,100),true,bounds));
    }
    @Test public void labelsCanBePlacedOutsideTheRegion()
    {
        BufferedImage image=new BufferedImage(100,100,2); Graphics2D g=image.createGraphics();
        try
        {
            Marker m=new Marker(0,0,50,50); Rectangle r=new Rectangle(20,20,50,50);
            m.labelPlacement=Marker.LabelPlacement.ABOVE;
            assertTrue(WindowMarkersPlugin.labelBaseline(m,r,g.getFontMetrics())<20);
            m.labelPlacement=Marker.LabelPlacement.BELOW;
            assertTrue(WindowMarkersPlugin.labelBaseline(m,r,g.getFontMetrics())>70);
        } finally { g.dispose(); }
    }
    @Test public void catalogKeepsExistingDefaultKeyAndRepairsUnknownSelection()
    {
        LayoutCatalog catalog=new LayoutCatalog(); assertEquals("base",catalog.storageKey("base"));
        catalog.active="missing"; catalog.normalize(); assertEquals("default",catalog.active);
        String id=UUID.randomUUID().toString(); catalog.names.put(id,"PvM"); catalog.active=id;
        assertEquals("base_layout_"+id,catalog.storageKey("base"));
    }
    private AutoCategories.Item item(int id,String name)
    { return new AutoCategories.Item(id,name,true,null,1,0,0); }
    @Test public void crystalRecommendationRequiresAllPiecesAndVoidIsNotIndependentlyRanked()
    {
        java.util.List<AutoCategories.Item> items=new ArrayList<>(Arrays.asList(item(1,"Bow of faerdhinen"),item(2,"Crystal body"),item(3,"Crystal legs")));
        assertFalse(AutoCategories.suggest(AutoCategories.Preset.CHAMBERS_OF_XERIC,items).containsKey("CoX Crystal bow setup"));
        items.add(item(4,"Crystal helm"));
        assertEquals(4,AutoCategories.suggest(AutoCategories.Preset.CHAMBERS_OF_XERIC,items).get("CoX Crystal bow setup").size());
        assertTrue(AutoCategories.category(item(5,"Elite void top"),true).startsWith("Void —"));
        assertTrue(AutoCategories.category(item(6,"Inquisitor's hauberk"),true).startsWith("Inquisitor —"));
    }
    @Test public void raidsUseDifferentOwnedWeaponChoices()
    {
        java.util.List<AutoCategories.Item> items=Arrays.asList(item(1,"Scythe of vitur"),item(2,"Osmumten's fang"),item(3,"Abyssal whip"));
        assertEquals(2,AutoCategories.suggest(AutoCategories.Preset.TOMBS_OF_AMASCUT,items).get("ToA Melee").get(0).id);
        assertTrue(AutoCategories.suggest(AutoCategories.Preset.THEATRE_OF_BLOOD,items).get("ToB Melee").stream().anyMatch(i -> i.id==1));
    }
}
