package com.windowmarkers;

import java.util.Objects;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetPositionMode;
import net.runelite.api.widgets.WidgetSizeMode;
import net.runelite.client.util.Text;

/** Reserve header space for the picker without covering the native title. */
final class BankHeaderTitle
{
    private Widget widget;
    private int x,width,xMode,widthMode,appliedWidth;
    private String originalText,appliedText;
    private static final int LEFT=52;

    void update(Widget current,int right) {
        if(current!=widget) { clear();widget=current; }
        if(widget==null) return;
        if(appliedText==null || widget.getOriginalX()!=LEFT) x=widget.getOriginalX();
        if(appliedText==null || widget.getXPositionMode()!=WidgetPositionMode.ABSOLUTE_LEFT) xMode=widget.getXPositionMode();
        if(appliedText==null || widget.getOriginalWidth()!=appliedWidth) width=widget.getOriginalWidth();
        if(appliedText==null || widget.getWidthMode()!=WidgetSizeMode.ABSOLUTE) widthMode=widget.getWidthMode();
        if(!Objects.equals(widget.getText(),appliedText)) originalText=widget.getText();
        if(originalText==null) originalText="";
        appliedWidth=Math.max(1,right-LEFT);
        appliedText=originalText;
        if(widget.getFont()!=null && widget.getFont().getTextWidth(originalText)>appliedWidth) {
            String plain=Text.removeTags(originalText);
            int end=plain.length();
            while(end>0 && widget.getFont().getTextWidth(plain.substring(0,end)+"...")>appliedWidth) end--;
            appliedText=plain.substring(0,end)+"...";
        }
        widget.setOriginalX(LEFT);widget.setXPositionMode(WidgetPositionMode.ABSOLUTE_LEFT);
        widget.setOriginalWidth(appliedWidth);widget.setWidthMode(WidgetSizeMode.ABSOLUTE);
        widget.setText(appliedText);widget.revalidate();
    }

    void clear() {
        if(widget!=null && appliedText!=null) {
            if(widget.getOriginalX()==LEFT) widget.setOriginalX(x);
            if(widget.getXPositionMode()==WidgetPositionMode.ABSOLUTE_LEFT) widget.setXPositionMode(xMode);
            if(widget.getOriginalWidth()==appliedWidth) widget.setOriginalWidth(width);
            if(widget.getWidthMode()==WidgetSizeMode.ABSOLUTE) widget.setWidthMode(widthMode);
            if(Objects.equals(widget.getText(),appliedText)) widget.setText(originalText);
            widget.revalidate();
        }
        widget=null;originalText=null;appliedText=null;
    }
}
