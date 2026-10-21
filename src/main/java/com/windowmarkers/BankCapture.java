package com.windowmarkers;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.game.ItemManager;
import net.runelite.client.util.ImageCapture;

/** Captures the current tab's full laid-out content without clicking or scrolling the game. */
final class BankCapture
{
    @Inject Client client;
    @Inject ItemManager items;
    @Inject ImageCapture capture;
    @Inject BorderRenderer borders;
    @Inject WindowMarkersConfig config;

    boolean save(List<Marker> markers)
    {
        Widget bank=client.getWidget(InterfaceID.Bankmain.UNIVERSE),grid=client.getWidget(InterfaceID.Bankmain.ITEMS);
        if(bank==null || grid==null || bank.isHidden() || grid.isHidden() || grid.getDynamicChildren()==null) { return false; }
        List<Widget> widgets=new ArrayList<>(); List<ItemAnchors.Cell> cells=new ArrayList<>();
        int width=grid.getWidth(),height=32;
        for(Widget child:grid.getDynamicChildren())
        {
            if(child==null || child.isSelfHidden() || child.getItemId()<0 || child.getItemId()==6512) { continue; }
            int x=child.getRelativeX(),y=child.getRelativeY();
            if(x<0 || y<0) { continue; }
            widgets.add(child); cells.add(new ItemAnchors.Cell(items.canonicalize(child.getItemId()),new Rectangle(x,y,36,32)));
            height=Math.max(height,y+36); width=Math.max(width,x+42);
        }
        if(widgets.isEmpty()) { return false; }
        List<Marker> visible=new ArrayList<>(); List<List<Rectangle>> regions=new ArrayList<>();
        for(Marker marker:markers)
        {
            List<Rectangle> bounds=marker.linked() ? ItemAnchors.regions(marker.linkedItemIds,cells)
                : Boolean.TRUE.equals(marker.scrollWithItems) ? java.util.Collections.singletonList(new Rectangle(marker.x,marker.y,marker.width,marker.height)) : java.util.Collections.emptyList();
            if(!bounds.isEmpty() && config.visible()) { visible.add(marker); regions.add(bounds); }
        }
        int contentHeight=height+8,totalHeight=contentHeight+60+visible.size()*22;
        if(!reasonableSize(width,totalHeight)) { return false; }
        BufferedImage image=new BufferedImage(width,totalHeight,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=image.createGraphics();
        try
        {
            g.setColor(new Color(32,29,24)); g.fillRect(0,0,width,totalHeight);
            g.setFont(new Font("Arial",Font.BOLD,16));g.setColor(new Color(230,202,130));g.drawString("Bank — current tab / filter",12,24);
            Graphics2D content=(Graphics2D)g.create(0,40,width,contentHeight);
            try
            {
                for(int i=0;i<visible.size();i++)
                {
                    Marker marker=visible.get(i);
                    if(marker.backgroundArgb==null) { continue; }
                    content.setColor(new Color(marker.backgroundArgb,true));
                    for(Rectangle r:regions.get(i)) { content.fillRect(r.x,r.y,r.width,r.height); }
                }
                for(Widget child:widgets)
                {
                    BufferedImage icon=items.getImage(child.getItemId(),child.getItemQuantity(),true);
                    if(items.getItemComposition(child.getItemId()).getPlaceholderTemplateId()>0) { icon=net.runelite.client.util.ImageUtil.alphaOffset(icon,0.5f); }
                    content.drawImage(icon,child.getRelativeX(),child.getRelativeY(),null);
                }
                for(int i=0;i<visible.size();i++)
                {
                    Marker marker=visible.get(i);
                    for(Rectangle r:regions.get(i)) { borders.draw(content,r,marker.style==null ? config.style() : marker.style,marker.borderArgb==null ? config.color() : new Color(marker.borderArgb,true),marker.borderArgb!=null); }
                }
            }
            finally { content.dispose(); }
            // A separate legend retains every label without covering any item icon.
            g.setFont(new Font("Arial",Font.PLAIN,12));
            int y=contentHeight+55;
            for(Marker marker:visible)
            {
                g.setColor(marker.borderArgb==null ? config.color() : new Color(marker.borderArgb,true)); g.fillRect(12,y-10,12,12);
                g.setColor(Color.WHITE); g.drawString(MarkerPresentation.fit(marker.labelText(),g.getFontMetrics(),width-44),32,y);y+=22;
            }
        }
        finally { g.dispose(); }
        capture.saveScreenshot(image,"bank","bank-markers",true,false);
        return true;
    }
    static boolean reasonableSize(int width,int height) { return width>0 && width<=4096 && height>0 && height<=30000 && (long)width*height<=32000000; }
}
