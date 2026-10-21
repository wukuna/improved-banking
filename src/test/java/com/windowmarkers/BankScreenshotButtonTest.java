package com.windowmarkers;

import java.awt.Rectangle;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetType;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class BankScreenshotButtonTest
{
    @Test public void buttonFollowsCloseWithoutMovingItAndCleansUpOnlyItsOwnWidget()
    {
        BankScreenshotButton controller=new BankScreenshotButton(); controller.client=mock(Client.class);
        Widget frame=mock(Widget.class),close=mock(Widget.class),button=mock(Widget.class);
        when(controller.client.getWidget(InterfaceID.Bankmain.FRAME)).thenReturn(frame);
        when(frame.getChildren()).thenReturn(new Widget[]{close,button});
        when(frame.getBounds()).thenReturn(new Rectangle(100,100,500,400));
        when(close.getBounds()).thenReturn(new Rectangle(570,107,20,20));
        when(close.getActions()).thenReturn(new String[]{"Close"});
        when(frame.createChild(-1,WidgetType.GRAPHIC)).thenReturn(button);
        controller.update(true,() -> {}); controller.update(true,() -> {});
        verify(frame,times(1)).createChild(-1,WidgetType.GRAPHIC);
        verify(button,times(2)).setOriginalX(446);
        verify(close,never()).setOriginalX(anyInt()); verify(close,never()).setHidden(anyBoolean());
        when(frame.getBounds()).thenReturn(new Rectangle(100,100,700,400));
        when(close.getBounds()).thenReturn(new Rectangle(770,107,20,20));
        controller.update(true,() -> {}); verify(button).setOriginalX(646);
        controller.update(false,() -> {}); verify(button).setHidden(true);
        controller.clear(); verify(button).clearActions(); verify(button).setHasListener(false);
        verify(close,never()).clearActions();
    }
    @Test public void neverAddsButtonToHiddenBankOrWithoutNativeClose()
    {
        BankScreenshotButton controller=new BankScreenshotButton(); controller.client=mock(Client.class);
        Widget frame=mock(Widget.class);
        when(controller.client.getWidget(InterfaceID.Bankmain.FRAME)).thenReturn(frame);
        controller.update(true,() -> {});
        verify(frame,never()).createChild(anyInt(),anyInt());
        assertNull(BankScreenshotButton.position(new Rectangle(0,0,20,20),new Rectangle(5,0,20,20)));
        Rectangle positioned=BankScreenshotButton.position(new Rectangle(100,100,500,400),new Rectangle(570,107,20,20));
        assertEquals(new Rectangle(446,7,20,20),positioned);
    }
}
