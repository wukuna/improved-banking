package com.windowmarkers;

import java.util.ArrayList;
import java.util.List;
import net.runelite.api.widgets.Widget;

/** Geometry owned by one temporary view, retained only until the next native rebuild. */
final class BankViewGeometry
{
    private Widget parent;
    private final List<Position> positions = new ArrayList<>();

    void clear() { parent = null; positions.clear(); }

    void capture(Widget container)
    {
        clear(); parent = container;
        Widget[] children = container.getDynamicChildren();
        if (children == null) return;
        for (Widget child : children)
            if (child != null && !child.isSelfHidden() && child.getItemId() >= 0)
                positions.add(new Position(child));
    }

    void restore(Widget container)
    {
        if (parent != container || container == null) return;
        Widget[] children = container.getDynamicChildren();
        if (children == null) return;
        for (Position p : positions)
        {
            int index = p.widget.getIndex();
            if (index < 0 || index >= children.length || children[index] != p.widget
                || p.widget.getItemId() != p.itemId || p.widget.isSelfHidden()) continue;
            if (p.widget.getOriginalX() != p.x || p.widget.getOriginalY() != p.y)
            {
                p.widget.setOriginalX(p.x); p.widget.setOriginalY(p.y); p.widget.revalidate();
            }
        }
    }

    private static final class Position
    {
        final Widget widget;
        final int itemId, x, y;
        Position(Widget widget)
        {
            this.widget=widget; itemId=widget.getItemId(); x=widget.getOriginalX(); y=widget.getOriginalY();
        }
    }
}
