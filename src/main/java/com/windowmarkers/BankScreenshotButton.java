package com.windowmarkers;

import java.awt.Rectangle;
import java.util.Arrays;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.widgets.JavaScriptCallback;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetType;
import net.runelite.api.gameval.InterfaceID;

/** Owns only its added screenshot widget; never moves or alters the native close control. */
final class BankScreenshotButton
{
    @Inject Client client;
    private Widget button, parent;

    void update(boolean enabled,Runnable capture)
    {
        Widget frame=client.getWidget(InterfaceID.Bankmain.FRAME);
        Widget close=findClose(frame);
        if(!enabled || frame==null || frame.isHidden() || close==null)
        { if(button!=null) { button.setHidden(true); } return; }
        Widget[] children=frame.getChildren();
        if(parent!=frame || button==null || children==null || !Arrays.asList(children).contains(button))
        {
            clear(); parent=frame;
            button=frame.createChild(-1,WidgetType.GRAPHIC);
            button.setOriginalWidth(20); button.setOriginalHeight(20);
            button.setSpriteId(573);
            button.setAction(0,"Save bank screenshot");
            button.setOnOpListener((JavaScriptCallback)e -> capture.run());
            button.setHasListener(true);
        }
        Rectangle position=position(frame.getBounds(),close.getBounds());
        if(position==null) { button.setHidden(true); return; }
        button.setOriginalX(position.x); button.setOriginalY(position.y);
        button.setHidden(false); button.revalidate();
    }

    static Rectangle position(Rectangle frame,Rectangle close)
    {
        Rectangle result=new Rectangle(close.x-frame.x-24,close.y-frame.y+(close.height-20)/2,20,20);
        return new Rectangle(0,0,frame.width,frame.height).contains(result) ? result : null;
    }

    private static Widget findClose(Widget frame)
    {
        if(frame==null || frame.getChildren()==null) { return null; }
        for(Widget child:frame.getChildren())
        {
            if(child==null || child.isHidden() || child.getActions()==null) { continue; }
            for(String action:child.getActions())
            { if("Close".equalsIgnoreCase(action)) { return child; } }
        }
        return null;
    }

    void clear()
    {
        if(button!=null)
        {
            button.setHidden(true); button.clearActions();
            button.setOnOpListener((Object[])null); button.setHasListener(false);
        }
        button=null; parent=null;
    }
}
