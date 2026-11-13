package com.windowmarkers;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Point;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.game.ItemEquipmentStats;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStats;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class BankEquipmentPreviewTest
{
    @Test public void previewUsesTheViewEquipmentButtonAndNeverReplacesItsActions() {
        BankControlLayoutTest.Fixture f=new BankControlLayoutTest.Fixture();BankLayoutController c=f.controller;
        c.itemManager=mock(ItemManager.class);when(c.config.bankEquipmentPreview()).thenReturn(true);
        Widget view=mock(Widget.class);when(view.getBounds()).thenReturn(new Rectangle(10,10,30,30));when(c.client.getWidget(InterfaceID.Bankmain.WORNITEMS_BUTTON)).thenReturn(view);
        when(c.client.getCanvasWidth()).thenReturn(800);when(c.client.getCanvasHeight()).thenReturn(600);
        ItemContainer worn=mock(ItemContainer.class);when(worn.getItems()).thenReturn(new Item[]{new Item(4151,1)});when(c.client.getItemContainer(InventoryID.WORN)).thenReturn(worn);
        when(c.itemManager.getItemStats(4151)).thenReturn(new ItemStats(true,1.5,0,ItemEquipmentStats.builder().str(82).aslash(82).build()));
        Graphics2D graphics=new BufferedImage(800,600,BufferedImage.TYPE_INT_ARGB).createGraphics();
        try {
            when(c.client.getMouseCanvasPosition()).thenReturn(new Point(500,400));c.equipmentPreview.render(graphics);verifyNoInteractions(c.itemManager);
            when(c.client.getMouseCanvasPosition()).thenReturn(new Point(20,20));c.equipmentPreview.render(graphics);verify(c.itemManager).getItemStats(4151);
            verify(view,never()).setOnOpListener(any(Object[].class));verify(view,never()).setAction(anyInt(),anyString());verify(c.client,never()).runScript(any(Object[].class));
            clearInvocations(c.itemManager);when(f.items.widget.isHidden()).thenReturn(true);c.equipmentPreview.render(graphics);verifyNoInteractions(c.itemManager);
        } finally {graphics.dispose();}
    }
    @Test public void bonusTotalsRetainDecimalsAndNegativeValues() {
        double[] stats=new double[15];BankEquipmentPreview.add(stats,ItemEquipmentStats.builder().astab(-5).str(82).mdmg(1.5f).prayer(4).build());
        BankEquipmentPreview.add(stats,ItemEquipmentStats.builder().astab(2).mdmg(2).prayer(3).build());
        assertEquals(-3,stats[0],0);assertEquals(3.5,stats[12],0);assertEquals(7,stats[13],0);
        assertEquals("+3.5%",BankEquipmentPreview.value(stats[12],true));assertEquals("-3",BankEquipmentPreview.value(stats[0],false));
    }
    @Test public void previewStaysOnCanvasAtEveryBankEdge() {
        for(Rectangle button:new Rectangle[]{new Rectangle(0,0,30,30),new Rectangle(730,470,30,30),new Rectangle(340,220,30,30)})
            assertTrue(new Rectangle(0,0,765,504).contains(BankEquipmentPreview.position(button,765,504)));
        assertNull(BankEquipmentPreview.position(new Rectangle(),200,100));
    }
}
