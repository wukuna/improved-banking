package com.windowmarkers;

import java.util.concurrent.atomic.AtomicInteger;
import net.runelite.api.widgets.Widget;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class BankViewGeometryTest
{
    @Test public void laterTagLayoutCannotReplaceOwnedViewButRebuildAndDifferentTabsCan() {
        BankViewGeometry geometry=new BankViewGeometry();
        Widget parent=mock(Widget.class),item=mock(Widget.class);
        AtomicInteger x=new AtomicInteger(99),y=new AtomicInteger(22);
        when(item.getOriginalX()).thenAnswer(c -> x.get());when(item.getOriginalY()).thenAnswer(c -> y.get());
        doAnswer(c -> {x.set(c.getArgument(0));return item;}).when(item).setOriginalX(anyInt());
        doAnswer(c -> {y.set(c.getArgument(0));return item;}).when(item).setOriginalY(anyInt());
        when(item.getIndex()).thenReturn(0);when(item.getItemId()).thenReturn(385);
        when(parent.getDynamicChildren()).thenReturn(new Widget[]{item});geometry.capture(parent);
        item.setOriginalX(51);item.setOriginalY(400);geometry.restore(parent);
        assertEquals(99,x.get());assertEquals(22,y.get());
        item.setOriginalY(400);geometry.restore(mock(Widget.class));assertEquals(400,y.get());
        // A reused child with a different item must not inherit the old layout.
        when(item.getItemId()).thenReturn(386);geometry.restore(parent);assertEquals(400,y.get());
        when(item.getItemId()).thenReturn(385);geometry.clear();geometry.restore(parent);assertEquals(400,y.get());
    }
}
