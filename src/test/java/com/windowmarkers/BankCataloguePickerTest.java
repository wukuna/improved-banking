package com.windowmarkers;

import java.awt.Canvas;
import java.awt.Rectangle;
import java.util.Collections;
import javax.swing.SwingUtilities;
import net.runelite.api.widgets.Widget;
import org.junit.Test;
import static org.mockito.Mockito.*;

public class BankCataloguePickerTest
{
    @Test public void closingBankCancelsPopupAlreadyQueuedForSwing() throws Exception {
        BankCataloguePicker picker=new BankCataloguePicker(); Widget button=mock(Widget.class);
        when(button.getBounds()).thenReturn(new Rectangle(400,20,20,20));
        var field=BankCataloguePicker.class.getDeclaredField("button"); field.setAccessible(true); field.set(picker,button);
        Canvas canvas=mock(Canvas.class);
        SwingUtilities.invokeAndWait(() -> {
            picker.show(canvas,Collections.emptyList(),false,c -> {},() -> {},() -> {});
            picker.clear();
        });
        SwingUtilities.invokeAndWait(() -> {});
        verify(canvas,never()).isShowing(); verify(button).setHidden(true);
        verify(button).clearActions(); verify(button).setHasListener(false);
    }
}
