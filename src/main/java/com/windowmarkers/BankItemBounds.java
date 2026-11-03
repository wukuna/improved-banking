package com.windowmarkers;

import java.awt.Rectangle;
import net.runelite.api.widgets.Widget;

final class BankItemBounds
{
    private BankItemBounds() {}

    static Rectangle of(Widget container,Widget item)
    {

        Rectangle viewport=container.getBounds();
        return new Rectangle(viewport.x+item.getRelativeX()-container.getScrollX(),
            viewport.y+item.getRelativeY()-container.getScrollY(),item.getWidth(),item.getHeight());
    }
}
