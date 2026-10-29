package com.windowmarkers;

import java.awt.Rectangle;
import java.util.*;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetPositionMode;
import net.runelite.api.widgets.WidgetSizeMode;

/** Moves the existing native widgets. No action forwarding or replacement click targets. */
final class BankControlWidgets
{
    private final BankLayoutController owner;
    private final Map<Widget,int[]> applied=new IdentityHashMap<>();
    private String saved;
    private BankControlLayout layout;
    private int visibility=-1,planWidth,planHeight;
    volatile int previewWidth=478,previewHeight=290;
    BankControlWidgets(BankLayoutController owner) { this.owner=owner; }

    private Set<BankControlLayout.Control> visible()
    {
        Set<BankControlLayout.Control> result=EnumSet.noneOf(BankControlLayout.Control.class);
        for(BankControlLayout.Control control:BankControlLayout.Control.values()) {
            Widget button=owner.client.getWidget(control.button);
            if(button!=null && !button.isHidden())result.add(control);
        }
        return result;
    }
    BankControlLayout.Plan plan(Widget container)
    {
        previewWidth=container.getWidth();previewHeight=container.getHeight();
        planWidth=previewWidth;planHeight=previewHeight;
        if(!Objects.equals(saved,owner.config.bankControlLayout()) || layout==null) {
            saved=owner.config.bankControlLayout();layout=BankControlLayout.read(saved);
        }
        BankControlLayout selected=owner.controlDrag.draft();
        return (selected==null?layout:selected).plan(container.getWidth(),container.getHeight(),visible());
    }
    boolean changed()
    {
        Widget container=owner.client.getWidget(InterfaceID.Bankmain.ITEMS_CONTAINER);
        if(container==null || visibility<0 || container.getWidth()!=planWidth || container.getHeight()!=planHeight)return true;
        if(visibility!=visibilitySignature())return true;
        for(Map.Entry<Widget,int[]> entry:applied.entrySet()) {
            Widget w=entry.getKey();int[] g=entry.getValue();
            if(owner.client.getWidget(w.getId())!=w || w.getOriginalX()!=g[0] || w.getOriginalY()!=g[1]
                || w.getOriginalWidth()!=g[2] || w.getOriginalHeight()!=g[3] || w.getXPositionMode()!=g[4]
                || w.getYPositionMode()!=g[5] || w.getWidthMode()!=g[6] || w.getHeightMode()!=g[7])return true;
        }
        return false;
    }
    void clear() { applied.clear();visibility=-1; }
    void capture()
    {
        for(Map.Entry<Widget,int[]> entry:applied.entrySet())entry.setValue(geometry(entry.getKey()));
        visibility=visibilitySignature();
    }
    private int visibilitySignature()
    {
        int mask=0;
        for(BankControlLayout.Control control:BankControlLayout.Control.values()) {
            Widget button=owner.client.getWidget(control.button);
            if(button!=null && !button.isHidden())mask|=1<<control.ordinal();
        }
        return mask;
    }
    private static int[] geometry(Widget w)
    {
        return new int[]{w.getOriginalX(),w.getOriginalY(),w.getOriginalWidth(),w.getOriginalHeight(),
            w.getXPositionMode(),w.getYPositionMode(),w.getWidthMode(),w.getHeightMode()};
    }
    private void position(Widget w,int x,int y)
    {
        if(w==null)return;owner.setPos(w,x,y,WidgetPositionMode.ABSOLUTE_LEFT,WidgetPositionMode.ABSOLUTE_TOP);applied.put(w,null);
    }
    void apply(Widget container,BankControlLayout.Plan plan)
    {
        Widget bottom=owner.client.getWidget(InterfaceID.Bankmain.BOTTOM);
        if(bottom==null)return;
        int top=plan.docked.isEmpty()?container.getHeight()-37:BankControlLayout.TOP;
        // A transparent native layer spans the left dock and the bottom row;
        // its empty area does not intercept item or Bank Tags clicks.
        owner.setSize(bottom,0,container.getHeight()-1-top,WidgetSizeMode.MINUS,WidgetSizeMode.ABSOLUTE);
        owner.setPos(bottom,0,1,WidgetPositionMode.ABSOLUTE_LEFT,WidgetPositionMode.ABSOLUTE_BOTTOM);applied.put(bottom,null);
        Widget quantity=owner.client.getWidget(InterfaceID.Bankmain.QUANTITY_LAYER);
        if(quantity!=null) {
            position(quantity,0,0);owner.setSize(quantity,0,0,WidgetSizeMode.MINUS,WidgetSizeMode.MINUS);
        }
        for(int id:new int[]{InterfaceID.Bankmain.BOTTOM_LINE4,InterfaceID.Bankmain.BOTTOM_LINE6,InterfaceID.Bankmain.DEPOSIT_LINE})
            position(owner.client.getWidget(id),-1000,0);
        for(BankControlLayout.Control control:BankControlLayout.Control.values()) {
            Rectangle r=plan.bounds.get(control);if(r==null)continue;
            Widget button=owner.client.getWidget(control.button),icon=owner.client.getWidget(control.graphic);
            position(button,r.x,r.y-top);
            position(icon,r.x+control.iconX,r.y-top+control.iconY);
            // Quantity labels use a height relative to their 36px native parent.
            // Freeze that label height while the parent spans both docking areas.
            if(icon!=null && control.quantity())owner.setSize(icon,25,21,WidgetSizeMode.ABSOLUTE,WidgetSizeMode.ABSOLUTE);
        }
    }
}
