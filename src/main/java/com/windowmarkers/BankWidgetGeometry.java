package com.windowmarkers;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import net.runelite.api.widgets.Widget;

final class BankWidgetGeometry
{
    private BankWidgetGeometry() {}
    public static Point location(Widget widget)
    {
        Point point = new Point();
        Set<Widget> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Widget current = widget; current != null && visited.add(current); current = current.getParent())
        {
            point.translate(current.getRelativeX(), current.getRelativeY());
            Widget parent = current.getParent();
            if (parent != null) point.translate(-parent.getScrollX(), -parent.getScrollY());
        }
        return point;
    }
    public static Rectangle bounds(Widget widget)
    {
        if (widget == null) return new Rectangle();
        Point point = location(widget);
        return new Rectangle(point.x, point.y, widget.getWidth(), widget.getHeight());
    }
}
