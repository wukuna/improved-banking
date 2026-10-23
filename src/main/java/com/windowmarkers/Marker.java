package com.windowmarkers;

import java.awt.Point;
import java.awt.Rectangle;

final class Marker
{
    int x, y, width, height;
    String label;
    String category, preset;
    Integer borderArgb;
    Integer backgroundArgb;
    WindowMarkersConfig.BorderStyle style;
    Boolean scrollWithItems;
    Integer labelArgb;
    Integer labelSize;
    String labelFont;
    enum LabelPlacement
    {
        INSIDE("Inside"), ABOVE("Above"), BELOW("Below");
        private final String label;
        LabelPlacement(String label) { this.label=label; }
        @Override public String toString() { return label; }
    }
    LabelPlacement labelPlacement;
    Boolean labelOnce;
    int[] linkedItemIds;
    boolean linked() { return linkedItemIds != null && linkedItemIds.length > 0; }

    int fontSize() { return labelSize == null ? 16 : Math.max(8,Math.min(48,labelSize)); }

    Rectangle onScreen(Rectangle bank, Rectangle items, int scrollY)
    {
        if (Boolean.TRUE.equals(scrollWithItems))
        {
            return items == null ? null : new Rectangle(items.x+x,items.y+y-scrollY,width,height);
        }
        return onScreen(bank);
    }

    void anchorLegacy(Rectangle bank, Rectangle items)
    {
        if (scrollWithItems != null) { return; }
        Rectangle old=onScreen(bank);
        scrollWithItems=items.contains(old.getLocation());
        if (scrollWithItems) { x=old.x-items.x; y=old.y-items.y; }
    }

    String labelText() { return label == null ? "" : label.replaceAll("[\\p{Cntrl}]", " ").substring(0, Math.min(60, label.length())); }

    Marker(int x, int y, int width, int height)
    {
        this.x = x; this.y = y; this.width = width; this.height = height;
    }

    boolean valid()
    {
        return x >= 0 && y >= 0 && width >= 16 && height >= 16
            && x <= 10000 && y <= 10000 && width <= 10000 && height <= 10000;
    }

    Rectangle onScreen(Rectangle bank)
    {
        return new Rectangle(bank.x + x, bank.y + y, width, height);
    }

    static Marker drag(Point start, Point end, Rectangle bank)
    {
        int ex = Math.max(bank.x, Math.min(bank.x + bank.width, end.x));
        int ey = Math.max(bank.y, Math.min(bank.y + bank.height, end.y));
        return new Marker(Math.min(start.x, ex) - bank.x, Math.min(start.y, ey) - bank.y,
            Math.abs(start.x - ex), Math.abs(start.y - ey));
    }
}
