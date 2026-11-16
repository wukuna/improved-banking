package com.windowmarkers;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class BankSearchFrameTest
{
    @Test public void compactAndRailSearchResizeEveryNativeBorderPieceAndRestoreThem() {
        for(boolean rail:new boolean[]{false,true}) {
            BankControlLayoutTest.Fixture f=new BankControlLayoutTest.Fixture();
            when(f.controller.config.bankActionRail()).thenReturn(rail);
            when(f.controller.config.compactBankSearch()).thenReturn(true);
            BankControlsTest.Cell button=f.cells.get(InterfaceID.Bankmain.SEARCH);
            List<Widget> pieces=new ArrayList<>();
            for(int row=0;row<3;row++)for(int col=0;col<3;col++) {
                BankControlsTest.Cell cell=new BankControlsTest.Cell(button.id,button,col==1?6:0,row==1?6:0,
                    col==1?12:6,row==1?12:6,col==2?2:0,row==2?2:0,col==1?1:0,row==1?1:0);
                AtomicReference<Rectangle> measured=new AtomicReference<>(cell.bounds());
                when(cell.widget.getBounds()).thenAnswer(call->new Rectangle(measured.get()));
                doAnswer(call->{measured.set(cell.bounds());return null;}).when(cell.widget).revalidate();
                when(cell.widget.getSpriteId()).thenReturn(1141+row*3+col);
                pieces.add(cell.widget);
            }
            button.children.addAll(pieces);
            if(rail) {
                f.controller.actionBar.plan(f.container.widget);
                f.controller.actionBar.apply(f.container.widget);
            }
            else f.controller.actionBar.applyCompactSearch();
            assertEquals(36,button.bounds().width);
            for(Widget piece:pieces) {
                assertTrue("The resized button must contain its rendered frame",button.bounds().contains(piece.getBounds()));
                verify(piece,never()).setSpriteId(anyInt());
                verify(piece,never()).clearActions();
            }
            assertEquals(button.bounds().x+36,pieces.get(2).getBounds().x+6);
            f.controller.actionBar.restore();
            assertEquals(46,button.bounds().width);
            assertEquals(button.bounds().x+46,pieces.get(2).getBounds().x+6);
            verify(button.widget,never()).clearActions();
            verify(button.widget,never()).setOnOpListener(any(Object[].class));
        }
    }
}
