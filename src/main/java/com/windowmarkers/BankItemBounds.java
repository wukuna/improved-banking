package com.windowmarkers;

import java.awt.Rectangle;
import net.runelite.api.widgets.Widget;

/** Geometry for direct children of the bank item container, including new preview copies. */
final class BankItemBounds
{
    private BankItemBounds() {}

    static Rectangle of(Widget container,Widget item)
    {
        // Widget.getBounds() uses a cached canvas position. A newly created copy
        // has not been drawn yet during BeforeRender, and moved cells still have
        // the previous layout's position. Relative coordinates are ready after revalidate().
        Rectangle viewport=container.getBounds();
        return new Rectangle(viewport.x+item.getRelativeX()-container.getScrollX(),
            viewport.y+item.getRelativeY()-container.getScrollY(),item.getWidth(),item.getHeight());
    }
}
