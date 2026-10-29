package com.windowmarkers;

import java.awt.Canvas;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import net.runelite.api.events.*;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.ProfileChanged;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import com.windowmarkers.BankControlLayout.Control;

public class BankControlDragTest
{
    private final Canvas canvas=new Canvas();
    @Test public void altDragReordersNativeButtonsAndSavesOnlyOnRelease() {
        Harness h=new Harness(false);Point start=h.center(Control.NOTE);Rectangle area=h.f.container.bounds();
        clearInvocations(h.f.controller.client,h.f.controller.configs);
        MouseEvent press=mouse(MouseEvent.MOUSE_PRESSED,start,true);h.drag.mousePressed(press);assertTrue(press.isConsumed());
        Point end=new Point(area.x+area.width-5,start.y);
        MouseEvent move=mouse(MouseEvent.MOUSE_DRAGGED,end,true);h.drag.mouseDragged(move);assertTrue(move.isConsumed());
        verifyNoInteractions(h.f.controller.client,h.f.controller.configs); // No client access on the AWT callback.
        h.frame();assertNotNull(h.drag.draft());verifyNoInteractions(h.f.controller.configs);
        int x=h.f.cells.get(Control.NOTE.button).bounds().x;assertTrue(x>area.x+area.width-50);
        MouseEvent release=mouse(MouseEvent.MOUSE_RELEASED,end,false);h.drag.mouseReleased(release);assertTrue(release.isConsumed());h.frame();
        assertTrue(h.custom.get());BankControlLayout saved=BankControlLayout.read(h.saved.get());
        assertEquals(Control.NOTE,saved.entries.get(saved.entries.size()-1).control);
        MouseEvent click=mouse(MouseEvent.MOUSE_CLICKED,end,false);h.drag.mouseClicked(click);assertTrue(click.isConsumed());
        verify(h.f.controller.client,never()).runScript(any(Object[].class));
    }
    @Test public void altDragDocksAndUndocksWithNativeActionsUntouched() {
        Harness h=new Harness(false);Rectangle area=h.f.container.bounds();Point start=h.center(Control.ONE);
        h.drag.mousePressed(mouse(MouseEvent.MOUSE_PRESSED,start,true));
        Point dock=new Point(area.x+55,area.y+65);h.drag.mouseDragged(mouse(MouseEvent.MOUSE_DRAGGED,dock,true));h.frame();
        assertEquals(49,h.f.items.geometry[0]);assertEquals(area.x+42,h.f.cells.get(Control.ONE.button).bounds().x);
        h.drag.mouseReleased(mouse(MouseEvent.MOUSE_RELEASED,dock,true));h.frame();
        assertTrue(h.entry(Control.ONE).left);
        start=h.center(Control.ONE);Point bottom=new Point(area.x+200,area.y+area.height-20);
        h.drag.mousePressed(mouse(MouseEvent.MOUSE_PRESSED,start,true));h.drag.mouseDragged(mouse(MouseEvent.MOUSE_DRAGGED,bottom,true));h.frame();
        h.drag.mouseReleased(mouse(MouseEvent.MOUSE_RELEASED,bottom,true));h.frame();
        assertFalse(h.entry(Control.ONE).left);assertEquals(1,h.f.items.geometry[0]);
        verify(h.f.cells.get(Control.ONE.button).widget,never()).setOnOpListener(any(Object[].class));
        verify(h.f.cells.get(Control.ONE.button).widget,never()).setClickMask(anyInt());
    }
    @Test public void ordinaryClicksAltClicksAndControlAltRemainSafe() {
        Harness h=new Harness(false);Point start=h.center(Control.SEARCH);
        MouseEvent regular=mouse(MouseEvent.MOUSE_PRESSED,start,false);h.drag.mousePressed(regular);assertFalse(regular.isConsumed());
        MouseEvent overlayMove=new MouseEvent(canvas,MouseEvent.MOUSE_PRESSED,0,InputEvent.ALT_DOWN_MASK|InputEvent.CTRL_DOWN_MASK,start.x,start.y,1,false,MouseEvent.BUTTON1);
        h.drag.mousePressed(overlayMove);assertFalse(overlayMove.isConsumed());
        h.drag.mousePressed(mouse(MouseEvent.MOUSE_PRESSED,start,true));h.drag.mouseReleased(mouse(MouseEvent.MOUSE_RELEASED,start,true));h.frame();
        verifyNoInteractions(h.f.controller.configs);assertFalse(h.custom.get());
        when(h.f.controller.client.isMenuOpen()).thenReturn(true);h.frame();
        MouseEvent menu=mouse(MouseEvent.MOUSE_PRESSED,start,true);h.drag.mousePressed(menu);assertFalse(menu.isConsumed());
    }
    @Test public void canceledDragsRestoreAndCannotSaveAcrossBankCloseProfileOrFocusChanges() {
        for(int cancel=0;cancel<5;cancel++) {
            Harness h=new Harness(false);Rectangle area=h.f.container.bounds();Point start=h.center(Control.X),dock=new Point(area.x+55,area.y+65);
            h.drag.mousePressed(mouse(MouseEvent.MOUSE_PRESSED,start,true));h.drag.mouseDragged(mouse(MouseEvent.MOUSE_DRAGGED,dock,true));h.frame();
            assertNotNull(h.drag.draft());
            if(cancel==0)h.f.bus.post(new WidgetClosed(InterfaceID.BANKMAIN,0,false));
            else if(cancel==1)h.f.bus.post(new ProfileChanged());
            else if(cancel==2) {FocusChanged event=new FocusChanged();event.setFocused(false);h.f.bus.post(event);}
            else if(cancel==3)h.f.container.widget.setOriginalWidth(478);
            else dock=new Point(area.x+area.width+30,area.y+100);
            if(cancel<4)h.frame();
            h.drag.mouseReleased(mouse(MouseEvent.MOUSE_RELEASED,dock,true));h.frame();
            verifyNoInteractions(h.f.controller.configs);assertNull(h.drag.draft());assertFalse(h.custom.get());
            assertEquals(1,h.f.items.geometry[0]);
            assertEquals(113,h.f.cells.get(Control.X.button).geometry[0]);
        }
    }
    @Test public void disablingAndReenablingDuringADragRestoresAndDoesNotSwallowTheNextClick() {
        Harness h=new Harness(false);Point start=h.center(Control.NOTE),end=new Point(start.x+100,start.y);
        h.drag.mousePressed(mouse(MouseEvent.MOUSE_PRESSED,start,true));h.drag.mouseDragged(mouse(MouseEvent.MOUSE_DRAGGED,end,true));h.frame();
        h.f.controller.shutDown();h.frame();h.f.controller.startUp();h.frame();
        MouseEvent normal=mouse(MouseEvent.MOUSE_PRESSED,h.center(Control.NOTE),false);h.drag.mousePressed(normal);
        assertFalse(normal.isConsumed());assertFalse(h.custom.get());verifyNoInteractions(h.f.controller.configs);
    }
    private MouseEvent mouse(int id,Point point,boolean alt)
    {return new MouseEvent(canvas,id,0,alt?InputEvent.ALT_DOWN_MASK:0,point.x,point.y,1,false,id==MouseEvent.MOUSE_DRAGGED?MouseEvent.NOBUTTON:MouseEvent.BUTTON1);}
    private static final class Harness {
        final BankControlLayoutTest.Fixture f=new BankControlLayoutTest.Fixture();final BankControlDrag drag=f.controller.controlDrag;
        final AtomicBoolean custom=new AtomicBoolean();final AtomicReference<String> saved=new AtomicReference<>("");final List<Runnable> pending=new ArrayList<>();
        Harness(boolean enabled) {
            custom.set(enabled);when(f.controller.config.customBankControls()).thenAnswer(call -> custom.get());
            when(f.controller.config.bankControlLayout()).thenAnswer(call -> saved.get());when(f.controller.config.bankButtonDragging()).thenReturn(true);
            f.controller.configs=mock(ConfigManager.class);
            doAnswer(call -> {pending.add(call.getArgument(0));return null;}).when(f.controller.clientThread).invokeLater(any(Runnable.class));
            doAnswer(call -> {saved.set(call.getArgument(2));changed("bankControlLayout");return null;}).when(f.controller.configs).setConfiguration(eq("windowmarkers"),eq("bankControlLayout"),any(String.class));
            doAnswer(call -> {custom.set(call.getArgument(2));changed("customBankControls");return null;}).when(f.controller.configs).setConfiguration(eq("windowmarkers"),eq("customBankControls"),eq(true));
            f.build();frame();
        }
        void changed(String key) {ConfigChanged event=new ConfigChanged();event.setGroup("windowmarkers");event.setKey(key);f.bus.post(event);}
        void frame(){f.bus.post(new BeforeRender());List<Runnable> tasks=new ArrayList<>(pending);pending.clear();tasks.forEach(Runnable::run);f.build();f.bus.post(new BeforeRender());}
        Point center(Control control) {Rectangle r=f.cells.get(control.button).bounds();return new Point(r.x+r.width/2,r.y+r.height/2);}
        BankControlLayout.Entry entry(Control control) {return BankControlLayout.read(saved.get()).entries.stream().filter(e -> e.control==control).findFirst().get();}
    }
}
