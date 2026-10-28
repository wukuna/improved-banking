package com.windowmarkers;

import java.awt.Rectangle;
import net.runelite.api.widgets.Widget;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class BankItemBoundsTest
{
    @Test public void rebuiltCopiesUseTheirCurrentSlotBeforeTheirFirstDraw() {
        Widget bank=mock(Widget.class),item=mock(Widget.class);
        when(bank.getBounds()).thenReturn(new Rectangle(400,100,478,400));
        when(item.getRelativeX()).thenReturn(243);when(item.getRelativeY()).thenReturn(58);
        when(item.getWidth()).thenReturn(36);when(item.getHeight()).thenReturn(32);
        when(item.getBounds()).thenReturn(new Rectangle(0,0,36,32));
        Rectangle expected=new Rectangle(643,158,36,32);
        assertEquals(expected,BankItemBounds.of(bank,item));
        when(item.getBounds()).thenReturn(expected);
        assertEquals(expected,BankItemBounds.of(bank,item));
        verify(item,never()).getBounds();
    }

    @Test public void scrollMoveAndNewViewPositionsApplyWithoutWaitingForACachedDraw() {
        Widget bank=mock(Widget.class),item=mock(Widget.class);
        when(bank.getBounds()).thenReturn(new Rectangle(400,100,478,400));
        when(bank.getScrollX()).thenReturn(5);when(bank.getScrollY()).thenReturn(72);
        when(item.getRelativeX()).thenReturn(51);when(item.getRelativeY()).thenReturn(166);
        when(item.getWidth()).thenReturn(36);when(item.getHeight()).thenReturn(32);
        assertEquals(new Rectangle(446,194,36,32),BankItemBounds.of(bank,item));
        when(bank.getBounds()).thenReturn(new Rectangle(500,180,600,500));
        when(item.getRelativeX()).thenReturn(147);when(item.getRelativeY()).thenReturn(22);
        assertEquals(new Rectangle(642,130,36,32),BankItemBounds.of(bank,item));
    }
}
