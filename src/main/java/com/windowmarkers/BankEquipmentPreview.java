package com.windowmarkers;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Locale;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.KeyCode;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.game.ItemEquipmentStats;
import net.runelite.client.game.ItemStats;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

final class BankEquipmentPreview extends Overlay
{
    private static final int[][] SLOTS={{0,1,0},{1,0,1},{2,1,1},{13,2,1},{3,0,2},{4,1,2},{5,2,2},{7,1,3},{9,0,4},{10,1,4},{12,2,4}};
    private static final String[] LABELS={"Stab","Slash","Crush","Magic","Ranged"};
    private final BankLayoutController owner;

    BankEquipmentPreview(BankLayoutController owner)
    {
        this.owner=owner;setPosition(OverlayPosition.DYNAMIC);setLayer(OverlayLayer.MANUAL);drawAfterInterface(InterfaceID.BANKMAIN);
    }

    @Override public Dimension render(Graphics2D graphics)
    {
        if(!owner.config.bankEquipmentPreview() || owner.itemManager==null || owner.client.isMenuOpen()
            || owner.client.getDraggedWidget()!=null || owner.client.isKeyPressed(KeyCode.KC_ALT))return null;
        Widget bank=owner.client.getWidget(InterfaceID.Bankmain.UNIVERSE),button=owner.client.getWidget(InterfaceID.Bankmain.WORNITEMS_BUTTON);
        Widget items=owner.client.getWidget(InterfaceID.Bankmain.ITEMS);
        if(bank==null || bank.isHidden() || button==null || button.isHidden() || items==null || items.isHidden())return null;
        net.runelite.api.Point mouse=owner.client.getMouseCanvasPosition();
        if(mouse==null || !button.getBounds().contains(mouse.getX(),mouse.getY()))return null;
        ItemContainer equipment=owner.client.getItemContainer(InventoryID.WORN);
        if(equipment==null)return null;
        Rectangle panel=position(button.getBounds(),owner.client.getCanvasWidth(),owner.client.getCanvasHeight());
        if(panel==null)return null;
        Item[] worn=equipment.getItems();double[] stats=new double[15];boolean complete=true;
        for(Item item:worn)if(item!=null && item.getId()>=0 && item.getQuantity()>0) {
            ItemStats data=owner.itemManager.getItemStats(item.getId());
            if(data==null || data.getEquipment()==null) {complete=false;continue;}
            add(stats,data.getEquipment());stats[14]+=data.getWeight();
        }
        Graphics2D g=(Graphics2D)graphics.create();
        try {
            g.setFont(FontManager.getRunescapeSmallFont());
            g.setColor(new Color(25,28,27,248));g.fillRoundRect(panel.x,panel.y,panel.width,panel.height,8,8);
            g.setColor(new Color(151,149,122));g.drawRoundRect(panel.x,panel.y,panel.width-1,panel.height-1,8,8);
            g.setColor(new Color(239,210,139));g.drawString("Worn equipment",panel.x+10,panel.y+18);
            for(int[] slot:SLOTS) {
                int x=panel.x+10+slot[1]*39,y=panel.y+28+slot[2]*36;
                g.setColor(new Color(45,49,46));g.fillRect(x,y,36,32);g.setColor(new Color(80,84,75));g.drawRect(x,y,35,31);
                Item item=slot[0]<worn.length?worn[slot[0]]:null;
                if(item!=null && item.getId()>=0 && item.getQuantity()>0) {
                    BufferedImage image=owner.itemManager.getImage(item.getId(),item.getQuantity(),false);
                    if(image!=null)g.drawImage(image,x,y,null);
                }
            }
            int left=panel.x+139,attack=panel.x+225,defence=panel.x+279;
            g.setColor(new Color(193,192,175));g.drawString("Base gear bonuses",left,panel.y+37);
            g.drawString("Attack",attack,panel.y+55);g.drawString("Defence",defence,panel.y+55);
            for(int i=0;i<5;i++) {
                int y=panel.y+73+i*17;g.setColor(new Color(216,216,204));g.drawString(LABELS[i],left,y);
                right(g,complete?value(stats[i],false):"?",attack+28,y);right(g,complete?value(stats[i+5],false):"?",defence+35,y);
            }
            String[] labels={"Melee strength","Ranged strength","Magic damage","Prayer"};
            for(int i=0;i<4;i++) {
                int y=panel.y+169+i*17;g.drawString(labels[i],left,y);right(g,complete?value(stats[10+i],i==2):"?",panel.x+panel.width-12,y);
            }
            g.setColor(new Color(181,184,167));g.drawString("Gear weight: "+(complete?String.format(Locale.ROOT,"%.1f kg",stats[14]):"?"),panel.x+10,panel.y+222);
            g.setColor(new Color(239,210,139));g.drawString("Click to open worn equipment",panel.x+10,panel.y+panel.height-12);
        } finally {g.dispose();}
        return null;
    }

    static Rectangle position(Rectangle button,int canvasWidth,int canvasHeight)
    {
        int width=326,height=260;
        if(canvasWidth<width || canvasHeight<height)return null;
        int x=button.x+button.width+6,y=button.y;
        if(x+width>canvasWidth)x=button.x-width-6;
        return new Rectangle(Math.max(0,Math.min(canvasWidth-width,x)),Math.max(0,Math.min(canvasHeight-height,y)),width,height);
    }
    static void add(double[] totals,ItemEquipmentStats s)
    {
        double[] values={s.getAstab(),s.getAslash(),s.getAcrush(),s.getAmagic(),s.getArange(),s.getDstab(),s.getDslash(),s.getDcrush(),s.getDmagic(),s.getDrange(),s.getStr(),s.getRstr(),s.getMdmg(),s.getPrayer()};
        for(int i=0;i<values.length;i++)totals[i]+=values[i];
    }
    static String value(double value,boolean percent)
    {
        String number=Math.abs(value-Math.rint(value))<0.001?String.format(Locale.ROOT,"%.0f",value):String.format(Locale.ROOT,"%.1f",value);
        return (value>0?"+":"")+number+(percent?"%":"");
    }
    private static void right(Graphics2D g,String value,int x,int y) {g.drawString(value,x-g.getFontMetrics().stringWidth(value),y);}
}
