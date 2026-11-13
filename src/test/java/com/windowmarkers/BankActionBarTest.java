package com.windowmarkers;

import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.SwingUtilities;
import net.runelite.api.GameState;
import net.runelite.api.events.BeforeRender;
import net.runelite.api.events.FocusChanged;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.JavaScriptCallback;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.ProfileChanged;
import net.runelite.client.plugins.banktags.BankTagsService;
import net.runelite.client.plugins.banktags.tabs.Layout;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import com.windowmarkers.BankControlLayout.Control;
import com.windowmarkers.BankControlsTest.Cell;

public class BankActionBarTest
{
    @Test public void disablingTheDividerHidesItsBarAndRetainsTheSavedSplit() throws Exception {
        Harness h=new Harness();when(h.c.config.bankActionTagSpace()).thenReturn(true);
        h.saved="v2;split:80;SEARCH:T:0;PLACEHOLDERS:T:50;";h.build();
        int split=h.bar.plan.tagTop;String saved=h.saved;
        Map<String,Rectangle> positions=new LinkedHashMap<>(h.bar.plan.bounds);
        when(h.c.config.bankTagDividerResizable()).thenReturn(false);h.frame();
        Rectangle area=h.f.container.bounds();Point grip=new Point(area.x+25,area.y+split-3);
        List<Object> mocks=new ArrayList<>();mocks.add(h.c.config);mocks.add(h.c.client);
        h.f.cells.values().forEach(cell->mocks.add(cell.widget));clearInvocations(mocks.toArray());
        SwingUtilities.invokeAndWait(()->assertFalse(h.bar.mousePressed(mouse(MouseEvent.MOUSE_PRESSED,grip,false)).isConsumed()));
        verifyNoInteractions(mocks.toArray());
        java.awt.Graphics2D graphics=mock(java.awt.Graphics2D.class);when(graphics.create()).thenReturn(graphics);
        h.bar.overlay.render(graphics);
        verify(graphics,times(2)).drawLine(anyInt(),anyInt(),anyInt(),anyInt());
        verify(graphics,never()).fillRect(anyInt(),anyInt(),anyInt(),anyInt());
        assertEquals(split,h.bar.plan.tagTop);assertEquals(positions,h.bar.plan.bounds);assertEquals(saved,h.saved);
        Point button=h.center(Control.PLACEHOLDERS),end=new Point(button.x,button.y+35);
        h.press(button);h.drag(end);h.frame();h.release(end);h.frame();h.flush();
        assertEquals(80,BankActionLayout.read(h.saved).tagOffset);
        assertEquals(positions.get("PLACEHOLDERS").y+35,h.bar.plan.bounds.get("PLACEHOLDERS").y);
        when(h.c.config.bankTagDividerResizable()).thenReturn(true);h.frame();
        assertTrue(h.bar.mousePressed(mouse(MouseEvent.MOUSE_PRESSED,grip,false)).isConsumed());
    }

    @Test public void turningOffDividerDuringADragCancelsWithoutSavingItsDraft() {
        Harness h=new Harness();when(h.c.config.bankActionTagSpace()).thenReturn(true);h.build();
        int split=h.bar.plan.tagTop;Rectangle area=h.f.container.bounds();
        Point start=new Point(area.x+25,area.y+split-3),end=new Point(start.x,start.y+75);
        h.press(start);h.drag(end);h.frame();assertTrue(h.bar.plan.tagTop>split);
        when(h.c.config.bankTagDividerResizable()).thenReturn(false);
        ConfigChanged change=new ConfigChanged();change.setGroup("windowmarkers");change.setKey("bankTagDividerResizable");
        h.c.onConfigChanged(change);h.flush();h.release(end);h.frame();h.flush();
        assertEquals("",h.saved);assertEquals(split,h.bar.plan.tagTop);
        assertFalse(h.bar.mousePressed(mouse(MouseEvent.MOUSE_PRESSED,start,false)).isConsumed());
    }

    @Test public void rightClickMenusKeepRailDividersVisibleWithoutCapturingInput() {
        Harness h=new Harness();when(h.c.config.bankActionTagSpace()).thenReturn(true);h.build();
        java.awt.Graphics2D graphics=mock(java.awt.Graphics2D.class);when(graphics.create()).thenReturn(graphics);
        when(h.c.client.isMenuOpen()).thenReturn(true);h.frame();h.bar.overlay.render(graphics);
        verify(graphics,atLeast(3)).drawLine(anyInt(),anyInt(),anyInt(),anyInt());
        assertFalse(h.bar.mousePressed(mouse(MouseEvent.MOUSE_PRESSED,h.center(Control.SEARCH),true)).isConsumed());
        Rectangle area=h.f.container.bounds();
        Point split=new Point(area.x+25,area.y+h.bar.plan.tagTop-3);
        assertFalse(h.bar.mousePressed(mouse(MouseEvent.MOUSE_PRESSED,split,false)).isConsumed());
        assertFalse(h.bar.mouseWheelMoved(wheel(new Point(h.f.items.bounds().x+60,h.f.items.bounds().y+20),1)).isConsumed());
        when(h.c.client.isMenuOpen()).thenReturn(false);h.frame();
        assertTrue(h.bar.mousePressed(mouse(MouseEvent.MOUSE_PRESSED,split,false)).isConsumed());
        h.c.shutDown();h.flush();clearInvocations(graphics);h.bar.overlay.render(graphics);verifyNoInteractions(graphics);
    }

    @Test public void splitterDragsWithoutAltAndStopsAtTheOriginalMidpoint() {
        Harness h=new Harness();when(h.c.config.bankActionTagSpace()).thenReturn(true);
        h.saved="v1;SEARCH:T:0;PLACEHOLDERS:T:50;";h.build();
        Rectangle area=h.f.container.bounds();int initial=h.bar.plan.tagTop;
        Point start=new Point(area.x+25,area.y+initial-3),end=new Point(start.x,start.y+90);
        Rectangle search=h.f.cells.get(Control.SEARCH.button).bounds();
        assertTrue(h.bar.mousePressed(mouse(MouseEvent.MOUSE_PRESSED,start,false)).isConsumed());
        h.drag(end);h.frame();assertEquals(initial+90,h.bar.plan.tagTop);
        assertEquals(search.y+90,h.f.cells.get(Control.SEARCH.button).bounds().y);
        assertEquals(0,BankActionLayout.read(h.saved).tagOffset);
        h.release(end);h.frame();h.flush();assertEquals(90,BankActionLayout.read(h.saved).tagOffset);
        for(int i=0;i<5;i++)h.build();assertEquals(initial+90,h.bar.plan.tagTop);
        start=new Point(area.x+25,area.y+h.bar.plan.tagTop-3);end=new Point(start.x,start.y-500);
        h.press(start);h.drag(end);h.frame();h.release(end);h.frame();h.flush();
        assertEquals(initial,h.bar.plan.tagTop);assertEquals(0,BankActionLayout.read(h.saved).tagOffset);
    }

    @Test public void splitterLowerLimitReservesEveryButtonAndPinnedGap() {
        BankActionLayout layout=BankActionLayout.read("v2;split:4000;SEARCH:T:0;PLACEHOLDERS:T:50;");
        Map<String,Dimension> sizes=new LinkedHashMap<>();sizes.put("SEARCH",new Dimension(36,36));
        BankActionLayout.Plan one=layout.plan(718,650,sizes,true);
        sizes.put("PLACEHOLDERS",new Dimension(36,36));
        BankActionLayout.Plan two=layout.plan(718,650,sizes,true);
        assertEquals(one.tagTop-38,two.tagTop);
        assertEquals(650-two.bottomHeight-4,two.bounds.get("PLACEHOLDERS").y+36);
        layout.entries.get("PLACEHOLDERS").x=5;layout.entries.get("PLACEHOLDERS").y=160;
        BankActionLayout.Plan gap=layout.plan(718,650,sizes,true);
        assertTrue(gap.tagTop<two.tagTop);
        assertEquals(gap.tagTop+160,gap.bounds.get("PLACEHOLDERS").y);
        assertEquals(2,gap.bounds.size());
        for(Rectangle r:gap.bounds.values())assertTrue(BankActionLayout.rail('T',718,650,gap.bottomHeight,gap.tagTop).contains(r));
        assertEquals(4000,BankActionLayout.read(layout.write()).tagOffset);
    }

    @Test public void splitterPreferenceSurvivesSmallerBanksAndHiddenTagSpace() {
        BankActionLayout layout=BankActionLayout.read("v2;split:150;SEARCH:T:0;PLACEHOLDERS:T:50;");
        Map<String,Dimension> sizes=new LinkedHashMap<>();
        for(Control control:Control.values())sizes.put(control.name(),new Dimension(control==Control.SEARCH?36:control.width,36));
        BankActionLayout.Plan initial=layout.plan(718,650,sizes,true);String saved=layout.write();
        for(int height:new int[]{340,250,650})for(boolean tags:new boolean[]{false,true}) {
            BankActionLayout.Plan plan=layout.plan(718,height,sizes,tags);
            assertEquals(sizes.size(),plan.bounds.size());
            plan.bounds.forEach((key,r)->{assertTrue(new Rectangle(0,0,718,height).contains(r));
                plan.bounds.forEach((other,b)->{if(!other.equals(key))assertFalse(r.intersects(b));});});
        }
        assertEquals(saved,layout.write());
        assertEquals(initial.bounds,BankActionLayout.read(saved).plan(718,650,sizes,true).bounds);
    }

    @Test public void splitterInputOnlyUsesThePublishedSnapshotAndCancelsAcrossContexts() throws Exception {
        for(boolean profile:new boolean[]{false,true}) {
            Harness h=new Harness();when(h.c.config.bankActionTagSpace()).thenReturn(true);h.build();
            Rectangle area=h.f.container.bounds();Point start=new Point(area.x+25,area.y+h.bar.plan.tagTop-3),end=new Point(start.x,start.y+75);
            List<Object> mocks=new ArrayList<>();mocks.add(h.c.client);mocks.add(h.c.config);
            h.f.cells.values().forEach(cell->mocks.add(cell.widget));clearInvocations(mocks.toArray());
            SwingUtilities.invokeAndWait(()->{assertTrue(h.bar.mousePressed(mouse(MouseEvent.MOUSE_PRESSED,start,false)).isConsumed());h.drag(end);});
            verifyNoInteractions(mocks.toArray());h.frame();
            if(profile)h.c.onProfileChanged(new ProfileChanged());else {FocusChanged e=new FocusChanged();e.setFocused(false);h.c.onFocusChanged(e);}
            h.frame();h.release(end);h.frame();h.flush();assertEquals("",h.saved);
        }
    }

    @Test public void buttonsRemainDraggableBelowAnAdjustedSplitter() {
        Harness h=new Harness();when(h.c.config.bankActionTagSpace()).thenReturn(true);
        h.saved="v2;split:80;SEARCH:T:0;PLACEHOLDERS:T:50;";h.build();
        Rectangle before=h.f.cells.get(Control.PLACEHOLDERS.button).bounds();
        Point start=h.center(Control.PLACEHOLDERS),end=new Point(start.x,start.y+65);
        h.press(start);h.drag(end);h.frame();h.release(end);h.frame();h.flush();
        assertEquals(80,BankActionLayout.read(h.saved).tagOffset);
        assertEquals(before.y+65,h.f.cells.get(Control.PLACEHOLDERS.button).bounds().y);
        for(int i=0;i<5;i++)h.build();assertEquals(before.y+65,h.f.cells.get(Control.PLACEHOLDERS.button).bounds().y);
    }

    @Test public void loweredSplitterExpandsTagViewportAndRestoresNativeArrows() {
        Harness h=new Harness();when(h.c.config.bankActionTagSpace()).thenReturn(true);
        Cell up=h.tag(41,20),down=h.tag(620,20);when(up.widget.getActions()).thenReturn(new String[]{null,"Scroll up"});when(down.widget.getActions()).thenReturn(new String[]{null,"Scroll down"});
        int[] original=down.geometry.clone();List<Cell> tags=new ArrayList<>();
        for(int i=0;i<15;i++){Cell tag=h.tag(62+i*41,40);when(tag.widget.getActions()).thenReturn(new String[]{null,"View tag tab"});tags.add(tag);}
        h.build();long initial=tags.stream().filter(t->!t.widget.isSelfHidden()).count();
        Rectangle area=h.f.container.bounds();Point start=new Point(area.x+25,area.y+h.bar.plan.tagTop-3),end=new Point(start.x,start.y+100);
        h.press(start);h.drag(end);h.frame();h.release(end);h.frame();h.flush();
        assertTrue(tags.stream().filter(t->!t.widget.isSelfHidden()).count()>initial);
        assertEquals(area.y+h.bar.plan.tagTop-5,down.bounds().y+down.bounds().height);
        h.c.shutDown();h.flush();assertArrayEquals(original,down.geometry);
    }

    @Test public void nativeActionsArtworkAndDimensionsSurviveRightRailPlacement() {
        Harness h=new Harness();Map<Widget,int[]> original=new HashMap<>();
        for(Cell c:h.f.cells.values())original.put(c.widget,c.geometry.clone());h.build();
        Rectangle area=h.f.container.bounds();
        for(Control control:Control.values()) {
            Widget w=h.f.cells.get(control.button).widget;Rectangle bounds=w.getBounds();
            assertEquals(control==Control.SEARCH?36:control.width,bounds.width);assertEquals(36,bounds.height);
            if(!control.quantity())assertTrue(bounds.x>=area.x+area.width-50);
            verify(w,never()).setOnOpListener(any(Object[].class));verify(w,never()).setClickMask(anyInt());verify(w,never()).clearActions();
            Widget icon=h.f.cells.get(control.graphic).widget;
            assertEquals(bounds.x+(control==Control.SEARCH?4:control.iconX),icon.getBounds().x);assertEquals(bounds.y+control.iconY,icon.getBounds().y);
        }
        assertTrue(h.f.cells.get(InterfaceID.Bankmain.SCROLLBAR).widget.isSelfHidden());
        for(int i=0;i<10;i++)h.frame();
        h.c.shutDown();h.flush();
        for(Map.Entry<Widget,int[]> e:original.entrySet())assertArrayEquals(e.getValue(),geometry(e.getKey()));
        assertFalse(h.f.cells.get(InterfaceID.Bankmain.SCROLLBAR).widget.isSelfHidden());
    }

    @Test public void buttonsMoveIntoTheLowerHalfWithoutOverlappingTags() {
        Harness h=new Harness();when(h.c.config.bankActionTagSpace()).thenReturn(true);h.build();
        Point start=h.center(Control.SEARCH);Rectangle area=h.f.container.bounds();
        int divider=BankActionLayout.tagTop(area.height,h.bar.plan.bottomHeight);
        Point end=new Point(area.x+20,area.y+divider+12);
        h.press(start);h.drag(end);h.frame();
        Rectangle button=h.f.cells.get(Control.SEARCH.button).bounds();
        assertEquals(area.x+2,button.x);assertTrue(button.y>=area.y+divider);assertTrue(h.saved.isEmpty());
        h.release(end);h.frame();h.flush();
        assertEquals('T',BankActionLayout.read(h.saved).entries.get("SEARCH").dock);
        assertTrue(h.click(end).isConsumed());assertFalse(h.click(end).isConsumed());
    }

    @Test public void draggingWithinTheRailKeepsTheChosenGapAndEveryOtherButtonStill() {
        Harness h=new Harness();h.build();Map<String,Rectangle> before=new LinkedHashMap<>(h.bar.plan.bounds);
        Point start=h.center(Control.SWAP),end=new Point(start.x,start.y+400);
        h.press(start);h.drag(end);h.frame();h.release(end);h.frame();h.flush();
        Rectangle expected=new Rectangle(before.get("SWAP"));expected.translate(0,400);
        assertEquals(expected,h.bar.plan.bounds.get("SWAP"));
        before.forEach((key,r) -> {if(!key.equals("SWAP"))assertEquals(key,r,h.bar.plan.bounds.get(key));});
        assertTrue(h.saved.startsWith("v2;"));
        for(int i=0;i<10;i++)h.build();assertEquals(expected,h.bar.plan.bounds.get("SWAP"));
        before.forEach((key,r) -> {if(!key.equals("SWAP"))assertEquals(key,r,h.bar.plan.bounds.get(key));});
    }

    @Test public void droppingOntoAnOccupiedButtonDoesNotRearrangeTheRack() {
        Harness h=new Harness();h.build();Map<String,Rectangle> before=new LinkedHashMap<>(h.bar.plan.bounds);
        Point start=h.center(Control.SWAP),end=h.center(Control.CONTAINERS);
        h.press(start);h.drag(end);h.frame();h.release(end);h.frame();h.flush();
        assertEquals(before,h.bar.plan.bounds);assertEquals("",h.saved);
    }

    @Test public void bottomPlacementKeepsTheGrabOffsetAndLeavesGaps() {
        Harness h=new Harness();h.build();Point start=h.center(Control.SEARCH);start.x-=10;
        Rectangle before=h.f.cells.get(Control.SEARCH.button).bounds(),area=h.f.container.bounds();
        Point end=new Point(area.x+450,area.y+area.height-18);
        h.press(start);h.drag(end);h.frame();h.release(end);h.frame();h.flush();
        Rectangle after=h.f.cells.get(Control.SEARCH.button).bounds();
        assertEquals(end.x-(start.x-before.x),after.x);assertEquals('B',BankActionLayout.read(h.saved).entries.get("SEARCH").dock);
        Rectangle quantity=h.f.cells.get(Control.ONE.button).bounds();
        for(int i=0;i<5;i++)h.build();assertEquals(after,h.f.cells.get(Control.SEARCH.button).bounds());
        assertEquals(quantity,h.f.cells.get(Control.ONE.button).bounds());
    }

    @Test public void headerButtonsCanMoveAndReturnToTheirNativePosition() {
        Harness h=new Harness();Cell equipment=h.extra(InterfaceID.Bankmain.WORNITEMS_BUTTON,h.f.root,5,5,30,30);
        equipment.widget.setAction(0,"Worn equipment");Rectangle original=equipment.bounds();h.build();
        Rectangle aligned=equipment.bounds();
        assertEquals(h.f.container.bounds().x+h.f.container.bounds().width-25,aligned.x+aligned.width/2);
        assertEquals(h.f.container.bounds().y+(BankActionLayout.TOP-aligned.height)/2,aligned.y);
        Point start=new Point(aligned.x+10,aligned.y+10),end=new Point(h.f.container.bounds().x+h.f.container.bounds().width-20,h.f.container.bounds().y+500);
        h.press(start);h.drag(end);h.frame();h.release(end);h.frame();h.flush();
        assertTrue(equipment.bounds().x>original.x+500);
        start=new Point(equipment.bounds().x+10,equipment.bounds().y+10);end=new Point(h.f.root.bounds().x+300,h.f.root.bounds().y+2);
        h.press(start);h.drag(end);h.frame();h.release(end);h.frame();h.flush();
        assertEquals(aligned,equipment.bounds());verify(equipment.widget,never()).setOnOpListener(any(Object[].class));
        h.c.shutDown();h.flush();assertEquals(original,equipment.bounds());
    }

    @Test public void wheelScrollingRunsOnClientThreadAndStopsAfterProfileChange() throws Exception {
        Harness h=new Harness();h.f.items.widget.setScrollHeight(1800);h.build();
        Rectangle items=h.f.items.bounds();Point inside=new Point(items.x+130,items.y+100);
        clearInvocations(h.c.client,h.f.items.widget);
        SwingUtilities.invokeAndWait(() -> assertTrue(h.bar.mouseWheelMoved(wheel(inside,2)).isConsumed()));
        verifyNoInteractions(h.c.client,h.f.items.widget);h.flush();assertEquals(72,h.f.items.widget.getScrollY());
        h.bar.mouseWheelMoved(wheel(inside,5));h.c.onProfileChanged(new ProfileChanged());h.flush();assertEquals(72,h.f.items.widget.getScrollY());
        assertFalse(h.bar.mouseWheelMoved(wheel(new Point(-10,-10),1)).isConsumed());
    }

    @Test public void equipmentClearsTheHeaderAndFirstRackButtonAtDifferentBankSizes() {
        for(int width:new int[]{478,718,1000})for(int height:new int[]{240,650}) {
            Harness h=new Harness();h.f.root.widget.setOriginalWidth(width+10);h.f.root.widget.setOriginalHeight(height+40);
            h.f.container.widget.setOriginalWidth(width);h.f.container.widget.setOriginalHeight(height);h.f.container.widget.setOriginalY(35);
            Cell equipment=h.extra(InterfaceID.Bankmain.WORNITEMS_BUTTON,h.f.root,5,5,36,36);
            Cell title=h.extra(InterfaceID.Bankmain.TITLE,h.f.root,50,5,width-110,24);
            Cell frame=h.extra(InterfaceID.Bankmain.FRAME,h.f.root,0,0,width+10,height+40);
            Cell close=h.extra(20001,frame,width-20,5,24,24);when(close.widget.getActions()).thenReturn(new String[]{"Close"});frame.children.add(close.widget);
            Rectangle nativeEquipment=equipment.bounds(),nativeClose=close.bounds();h.build();
            for(int i=0;i<5;i++) {
                Rectangle r=equipment.bounds();assertEquals(h.f.container.bounds().x+width-25,r.x+r.width/2);
                assertTrue(r.y>=h.f.container.bounds().y);assertFalse(r.intersects(title.bounds()));assertFalse(r.intersects(close.bounds()));
                for(Rectangle button:h.bar.plan.bounds.values()) {
                    Rectangle screen=new Rectangle(button);screen.translate(h.f.container.bounds().x,h.f.container.bounds().y);
                    assertFalse(r.intersects(screen));
                }
                assertEquals(nativeClose,close.bounds());h.build();
            }
            h.c.shutDown();h.flush();assertEquals(nativeEquipment,equipment.bounds());assertEquals(nativeClose,close.bounds());
        }
    }

    @Test public void awtDraggingReadsSnapshotsAndKeepsNormalClicksNative() throws Exception {
        Harness h=new Harness();h.build();Point start=h.center(Control.INVENTORY);List<Object> mocks=new ArrayList<>();mocks.add(h.c.client);mocks.add(h.c.configs);
        for(Cell c:h.f.cells.values())mocks.add(c.widget);clearInvocations(mocks.toArray());
        SwingUtilities.invokeAndWait(() -> {assertFalse(h.bar.mousePressed(mouse(MouseEvent.MOUSE_PRESSED,start,false)).isConsumed());h.press(start);h.drag(new Point(start.x,start.y+70));h.release(new Point(start.x,start.y+70));});
        verifyNoInteractions(mocks.toArray());
    }

    @Test public void focusAndProfileChangesCancelAnUnfinishedArrangement() {
        for(boolean profile:new boolean[]{false,true}) {
            Harness h=new Harness();h.build();Point start=h.center(Control.SEARCH),end=new Point(h.f.container.bounds().x+200,h.f.container.bounds().y+h.f.container.bounds().height-10);
            h.press(start);h.drag(end);h.frame();
            if(profile)h.c.onProfileChanged(new ProfileChanged());else {FocusChanged e=new FocusChanged();e.setFocused(false);h.c.onFocusChanged(e);}
            h.frame();h.release(end);h.frame();h.flush();assertEquals("",h.saved);
        }
    }

    @Test public void sharedTagsCanScrollToTheLastTagAndRestoreTheirOriginalCallbacks() {
        Harness h=new Harness();when(h.c.config.bankActionTagSpace()).thenReturn(true);
        Cell up=h.tag(41,20),down=h.tag(620,20);when(up.widget.getActions()).thenReturn(new String[]{null,"Scroll up"});when(down.widget.getActions()).thenReturn(new String[]{null,"Scroll down"});
        Object[] nativeUp={(JavaScriptCallback)e -> {}},nativeDown={(JavaScriptCallback)e -> {}};
        up.widget.setOnOpListener(nativeUp);down.widget.setOnOpListener(nativeDown);
        List<Cell> tags=new ArrayList<>();List<int[]> positions=new ArrayList<>();
        for(int i=0;i<20;i++) {
            Cell tag=h.tag(62+i*41,40);when(tag.widget.getActions()).thenReturn(new String[]{null,"View tag tab"});
            tags.add(tag);positions.add(tag.geometry.clone());if(i>=14)tag.widget.setHidden(true);
        }
        h.build();Rectangle area=h.f.container.bounds();int divider=BankActionLayout.tagTop(area.height,h.bar.plan.bottomHeight)-5;
        for(Cell tag:tags)if(!tag.widget.isSelfHidden())assertTrue(tag.bounds().y+tag.bounds().height<=area.y+divider-down.bounds().height);
        assertFalse(tags.get(0).widget.isSelfHidden());assertTrue(tags.get(19).widget.isSelfHidden());
        assertTrue(h.bar.mouseWheelMoved(wheel(new Point(area.x+10,area.y+100),50)).isConsumed());h.flush();h.frame();
        assertFalse(tags.get(19).widget.isSelfHidden());assertTrue(tags.get(0).widget.isSelfHidden());assertEquals(0,h.f.items.widget.getScrollY());
        for(int i=0;i<10;i++) {h.bar.apply(h.f.container.widget);h.frame();}
        h.c.shutDown();h.flush();assertArrayEquals(nativeUp,up.widget.getOnOpListener());assertArrayEquals(nativeDown,down.widget.getOnOpListener());
        for(int i=0;i<20;i++) {assertEquals(i>=14,tags.get(i).widget.isSelfHidden());assertArrayEquals(positions.get(i),tags.get(i).geometry);}
    }

    @Test public void tightBankTagLayoutsKeepTheirOriginalGridAndNormalScrolling() {
        Harness h=new Harness();h.c.bankTagsService=mock(BankTagsService.class);when(h.c.bankTagsService.getActiveLayout()).thenReturn(new Layout("Eight columns"));
        h.f.container.widget.setOriginalWidth(478);h.build();assertFalse(h.bar.enabled());
        assertFalse(h.f.cells.get(InterfaceID.Bankmain.SCROLLBAR).widget.isSelfHidden());
    }

    @Test public void layoutFitsEveryButtonWithoutShrinkingOrOverlap() {
        Map<String,Dimension> sizes=new LinkedHashMap<>();for(Control c:Control.values())sizes.put(c.name(),new Dimension(c.width,36));
        sizes.put("PRESETS",new Dimension(96,22));sizes.put("EQUIPMENT",new Dimension(30,30));
        for(int width:new int[]{478,510,718,1000})for(int height:new int[]{160,240,330,650})for(char dock:new char[]{'B','L','R','T'}) {
            BankActionLayout layout=BankActionLayout.read("");int n=0;for(String key:sizes.keySet())layout.move(key,dock,n++*40);
            BankActionLayout.Plan plan=layout.plan(width,height,sizes,true);assertEquals(sizes.keySet(),plan.bounds.keySet());
            for(Map.Entry<String,Rectangle> e:plan.bounds.entrySet()) {
                Rectangle r=e.getValue();assertEquals(sizes.get(e.getKey()),r.getSize());assertTrue(new Rectangle(0,0,width,height).contains(r));
                for(Map.Entry<String,Rectangle> other:plan.bounds.entrySet())if(!other.getKey().equals(e.getKey()))assertFalse(e.getKey()+" overlaps "+other.getKey(),r.intersects(other.getValue()));
            }
        }
    }

    @Test public void customViewFitsFullColumnsBetweenTheTagStripAndRightRack() {
        for(int width:new int[]{478,624,718,1000}) {
            Harness h=new Harness();h.f.container.widget.setOriginalWidth(width);
            when(h.c.config.bankLayoutEnabled()).thenReturn(false);when(h.c.config.columns()).thenReturn(4);
            List<Cell> cells=new ArrayList<>();
            for(int i=0;i<48;i++) {
                Cell cell=h.extra(20000+i,h.f.items,51+(i%8)*48,(i/8)*36,36,32);
                when(cell.widget.getType()).thenReturn(net.runelite.api.widgets.WidgetType.GRAPHIC);
                when(cell.widget.getItemId()).thenReturn(385);h.f.items.children.add(cell.widget);cells.add(cell);
            }
            h.build();Rectangle area=h.f.container.bounds(),viewport=h.f.items.bounds();
            int row=cells.get(0).bounds().y,right=0,count=0;
            for(Cell cell:cells)if(cell.bounds().y==row) {
                Rectangle r=cell.bounds();assertTrue(viewport.contains(r));assertTrue(r.x>=area.x+56);
                assertEquals(36,r.width);assertEquals(32,r.height);right=Math.max(right,r.x+r.width);count++;
            }
            assertTrue(count>4);
            int gap=area.x+area.width-51-right;
            assertTrue("Right padding: "+gap,gap>=4 && gap<=count+6);
        }
    }

    @Test public void customizationHasOneMainSwitchAndHidesLegacySizeControls() throws Exception {
        for(String name:new String[]{"customWidth","width","customHeight","height","columns","bankWindowControls","customBankControls","bankButtonDragging"})
            assertTrue(WindowMarkersConfig.class.getMethod(name).getAnnotation(net.runelite.client.config.ConfigItem.class).hidden());
        assertEquals("Customize bank view",WindowMarkersConfig.class.getMethod("bankActionRail").getAnnotation(net.runelite.client.config.ConfigItem.class).name());
        for(String name:new String[]{"bankActionRail","bankActionTagSpace","bankTagDividerResizable","bankEquipmentPreview"})
            assertFalse(WindowMarkersConfig.class.getMethod(name).getAnnotation(net.runelite.client.config.ConfigItem.class).hidden());
    }

    @Test public void sectionDividersSpanTheItemViewportAndRestoreWhenDisabled() {
        for(int type:new int[]{net.runelite.api.widgets.WidgetType.LINE,net.runelite.api.widgets.WidgetType.RECTANGLE,net.runelite.api.widgets.WidgetType.GRAPHIC}) {
            Harness h=new Harness();Cell divider=h.extra(20002,h.f.items,0,200,380,2);
            when(divider.widget.getType()).thenReturn(type);when(divider.widget.getItemId()).thenReturn(-1);when(divider.widget.getSpriteId()).thenReturn(897);
            h.f.items.children.add(divider.widget);int[] original=divider.geometry.clone();
            for(int width:new int[]{478,718,1000,478}) {
                h.f.container.widget.setOriginalWidth(width);h.build();
                Rectangle viewport=h.f.items.bounds(),line=divider.bounds();
                assertEquals(viewport.x+51,line.x);assertEquals(viewport.x+viewport.width-6,line.x+line.width);
                assertEquals(2,line.height);assertTrue(line.x+line.width<h.f.container.bounds().x+width-50);
            }
            h.c.shutDown();h.flush();assertArrayEquals(original,divider.geometry);
        }
    }

    @Test public void savedRailPositionsReturnAfterTemporarySpaceConstraints() {
        Map<String,Dimension> sizes=new LinkedHashMap<>();for(Control c:Control.values())sizes.put(c.name(),new Dimension(c.width,36));
        BankActionLayout layout=BankActionLayout.read("v1;SEARCH:T:0;PLACEHOLDERS:T:50;");
        BankActionLayout.Plan initial=layout.plan(718,650,sizes,true);
        layout.pinVisible(718,650,initial.bottomHeight,initial.bounds,true);
        layout.pin("SEARCH",'T',new Rectangle(2,470,46,36),718,650,initial.bottomHeight);
        String saved=layout.write();Map<String,Rectangle> before=layout.plan(718,650,sizes,true).bounds;
        for(boolean tags:new boolean[]{false,true}) {
            BankActionLayout.Plan small=layout.plan(478,240,sizes,tags);
            assertEquals(sizes.size(),small.bounds.size());
            small.bounds.forEach((key,r) -> {
                assertTrue(new Rectangle(0,0,478,240).contains(r));
                small.bounds.forEach((other,b) -> {if(!key.equals(other))assertFalse(r.intersects(b));});
            });
        }
        assertEquals(saved,layout.write());assertEquals(before,BankActionLayout.read(saved).plan(718,650,sizes,true).bounds);
        assertEquals(initial.bounds.get("PLACEHOLDERS"),before.get("PLACEHOLDERS"));
    }

    private static int[] geometry(Widget w) {return new int[]{w.getOriginalX(),w.getOriginalY(),w.getOriginalWidth(),w.getOriginalHeight(),w.getXPositionMode(),w.getYPositionMode(),w.getWidthMode(),w.getHeightMode()};}
    private static MouseEvent mouse(int id,Point p,boolean alt) {return new MouseEvent(new Canvas(),id,0,alt?MouseEvent.ALT_DOWN_MASK:0,p.x,p.y,1,false,MouseEvent.BUTTON1);}
    private static MouseWheelEvent wheel(Point p,int amount) {return new MouseWheelEvent(new Canvas(),MouseEvent.MOUSE_WHEEL,0,0,p.x,p.y,0,false,MouseWheelEvent.WHEEL_UNIT_SCROLL,3,amount);}

    private static final class Harness {
        final BankControlLayoutTest.Fixture f=new BankControlLayoutTest.Fixture();final BankLayoutController c=f.controller;final BankActionBar bar=c.actionBar;
        final List<Runnable> pending=new ArrayList<>();String saved="";int id=10000;
        Harness() {
            for(Cell cell:f.cells.values())prepare(cell);
            when(c.config.bankActionRail()).thenReturn(true);when(c.config.bankButtonDragging()).thenReturn(true);
            when(c.config.bankTagDividerResizable()).thenReturn(true);
            when(c.config.bankActionLayout()).thenAnswer(call -> saved);when(c.client.getGameState()).thenReturn(GameState.LOGGED_IN);
            c.configs=mock(ConfigManager.class,RETURNS_DEEP_STUBS);when(c.configs.getProfile().getId()).thenReturn(10L);
            doAnswer(call -> {pending.add(call.getArgument(0));return null;}).when(c.clientThread).invokeLater(any(Runnable.class));
            doAnswer(call -> {saved=call.getArgument(2);ConfigChanged e=new ConfigChanged();e.setGroup("windowmarkers");e.setKey("bankActionLayout");f.bus.post(e);return null;})
                .when(c.configs).setConfiguration(eq("windowmarkers"),eq("bankActionLayout"),anyString());
        }
        void prepare(Cell cell) {
            when(cell.widget.getParent()).thenReturn(cell.parent==null?null:cell.parent.widget);
            when(cell.widget.isSelfHidden()).thenAnswer(call -> cell.properties.getOrDefault("Hidden",false));
            when(cell.widget.isHidden()).thenAnswer(call -> (boolean)cell.properties.getOrDefault("Hidden",false) || cell.parent!=null && cell.parent.widget.isHidden());
        }
        Cell extra(int id,Cell parent,int x,int y,int w,int height) {Cell cell=f.add(id,parent,x,y,w,height,0,0,0,0);prepare(cell);return cell;}
        Cell tag(int y,int height) {Cell cell=extra(id++,f.container,1,y,39,height);f.container.children.add(cell.widget);return cell;}
        void build() {f.build();frame();}
        void frame() {f.bus.post(new BeforeRender());}
        void flush() {int tries=0;while(!pending.isEmpty() && tries++<20) {List<Runnable> batch=new ArrayList<>(pending);pending.clear();batch.forEach(Runnable::run);}assertTrue(pending.isEmpty());frame();}
        Point center(Control control) {Rectangle r=f.cells.get(control.button).bounds();return new Point(r.x+r.width/2,r.y+r.height/2);}
        void press(Point p) {assertTrue(bar.mousePressed(mouse(MouseEvent.MOUSE_PRESSED,p,true)).isConsumed());}
        void drag(Point p) {bar.mouseDragged(mouse(MouseEvent.MOUSE_DRAGGED,p,true));}
        void release(Point p) {bar.mouseReleased(mouse(MouseEvent.MOUSE_RELEASED,p,true));}
        MouseEvent click(Point p) {return bar.mouseClicked(mouse(MouseEvent.MOUSE_CLICKED,p,true));}
    }
}
