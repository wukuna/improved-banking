package com.windowmarkers;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.LongSupplier;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.game.ItemManager;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.util.AsyncBufferedImage;
import net.runelite.client.util.ImageCapture;

/** Captures the current tab's full laid-out content without clicking or scrolling the game. */
final class BankCapture
{
    private static final org.slf4j.Logger log=org.slf4j.LoggerFactory.getLogger(BankCapture.class);
    @Inject Client client;
    @Inject ClientThread clientThread;
    @Inject ItemManager items;
    @Inject ImageCapture capture;
    @Inject BorderRenderer borders;
    @Inject WindowMarkersConfig config;
    LongSupplier clock=System::nanoTime;
    private long revision;
    private boolean pending;

    /** Called on the client thread when the plugin/account/profile context changes. */
    void cancel() { revision++; pending=false; }

    /** True means queued, not saved. Never blocks the client thread waiting for sprites. */
    boolean save(List<Marker> markers,BooleanSupplier valid,Consumer<Boolean> completed)
    {
        if(pending || !valid.getAsBoolean()) { return false; }
        Widget bank=client.getWidget(InterfaceID.Bankmain.UNIVERSE),grid=client.getWidget(InterfaceID.Bankmain.ITEMS);
        if(bank==null || grid==null || bank.isHidden() || grid.isHidden() || grid.getDynamicChildren()==null) { return false; }
        List<Icon> icons=new ArrayList<>(); List<ItemAnchors.Cell> cells=new ArrayList<>();
        int width=grid.getWidth(),height=32;
        for(Widget child:grid.getDynamicChildren())
        {
            if(child==null || child.isSelfHidden() || child.getItemId()<0 || child.getItemId()==6512) { continue; }
            int x=child.getRelativeX(),y=child.getRelativeY();
            if(x<0 || y<0) { continue; }
            icons.add(new Icon(child.getItemId(),child.getItemQuantity(),x,y));
            cells.add(new ItemAnchors.Cell(items.canonicalize(child.getItemId()),new Rectangle(x,y,36,32)));
            height=Math.max(height,y+36); width=Math.max(width,x+42);
        }
        if(icons.isEmpty()) { return false; }
        List<Group> visible=new ArrayList<>(); List<List<Rectangle>> regions=new ArrayList<>();
        for(Marker marker:markers)
        {
            List<Rectangle> bounds=marker.linked() ? ItemAnchors.regions(marker.linkedItemIds,cells)
                : Boolean.TRUE.equals(marker.scrollWithItems) ? java.util.Collections.singletonList(new Rectangle(marker.x,marker.y,marker.width,marker.height)) : java.util.Collections.emptyList();
            if(!bounds.isEmpty() && config.visible()) {
                // Freeze presentation too: the user can edit markers while images are loading.
                visible.add(new Group(marker,config)); regions.add(bounds);
            }
        }
        int contentHeight=height+8,totalHeight=contentHeight+60+visible.size()*22;
        if(!reasonableSize(width,totalHeight)) { return false; }
        AtomicInteger remaining=new AtomicInteger(icons.size());
        for(Icon icon:icons) {
            icon.image=items.getImage(icon.id,icon.quantity,true);
            if(icon.image==null) { return false; }
            var definition=items.getItemComposition(icon.id);
            icon.placeholder=definition!=null && definition.getPlaceholderTemplateId()>0;
            // The listener retains only a counter, not the capture or its bank snapshot.
            icon.image.onLoaded(remaining::decrementAndGet);
        }
        pending=true;final long request=++revision,started=clock.getAsLong();final int captureWidth=width;
        clientThread.invokeLater((BooleanSupplier)() -> {
            if(request!=revision) { return true; }
            if(!valid.getAsBoolean()) { pending=false;return true; }
            if(remaining.get()>0) {
                if(clock.getAsLong()-started<TimeUnit.SECONDS.toNanos(10)) { return false; }
                pending=false;completed.accept(false);return true;
            }
            pending=false;
            try {
                render(captureWidth,totalHeight,contentHeight,icons,visible,regions);
                completed.accept(true);
            } catch(RuntimeException ex) {
                log.warn("Could not capture bank screenshot",ex);completed.accept(false);
            }
            return true;
        });
        return true;
    }

    private void render(int width,int totalHeight,int contentHeight,List<Icon> icons,List<Group> visible,List<List<Rectangle>> regions)
    {
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
                    Group marker=visible.get(i);
                    if(marker.backgroundArgb==null) { continue; }
                    content.setColor(new Color(marker.backgroundArgb,true));
                    for(Rectangle r:regions.get(i)) { content.fillRect(r.x,r.y,r.width,r.height); }
                }
                for(Icon cell:icons)
                {
                    BufferedImage icon=cell.placeholder?net.runelite.client.util.ImageUtil.alphaOffset(cell.image,0.5f):cell.image;
                    content.drawImage(icon,cell.x,cell.y,null);
                }
                for(int i=0;i<visible.size();i++)
                {
                    Group marker=visible.get(i);
                    for(Rectangle r:regions.get(i)) { borders.draw(content,r,marker.style,marker.borderColor,marker.tint); }
                }
            }
            finally { content.dispose(); }
            // A separate legend retains every label without covering any item icon.
            g.setFont(new Font("Arial",Font.PLAIN,12));
            int y=contentHeight+55;
            for(Group marker:visible)
            {
                g.setColor(marker.borderColor); g.fillRect(12,y-10,12,12);
                g.setColor(Color.WHITE); g.drawString(MarkerPresentation.fit(marker.label,g.getFontMetrics(),width-44),32,y);y+=22;
            }
        }
        finally { g.dispose(); }
        capture.saveScreenshot(image,"bank","bank-markers",true,false);
    }
    private static final class Icon
    {
        final int id,quantity,x,y;
        AsyncBufferedImage image;
        boolean placeholder;
        Icon(int id,int quantity,int x,int y) { this.id=id;this.quantity=quantity;this.x=x;this.y=y; }
    }
    private static final class Group
    {
        final String label;
        final Integer backgroundArgb;
        final WindowMarkersConfig.BorderStyle style;
        final Color borderColor;
        final boolean tint;
        Group(Marker marker,WindowMarkersConfig config) {
            label=marker.labelText();backgroundArgb=marker.backgroundArgb;
            style=marker.style==null?config.style():marker.style;tint=marker.borderArgb!=null;
            borderColor=tint?new Color(marker.borderArgb,true):config.color();
        }
    }
    static boolean reasonableSize(int width,int height) { return width>0 && width<=4096 && height>0 && height<=30000 && (long)width*height<=32000000; }
}
