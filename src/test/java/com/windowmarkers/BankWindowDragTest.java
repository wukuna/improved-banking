package com.windowmarkers;

import java.awt.Canvas;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ScriptID;
import net.runelite.api.events.BeforeRender;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.FocusChanged;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.ProfileChanged;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import com.windowmarkers.BankControlsTest.Cell;

public class BankWindowDragTest
{
    @Test public void movingUsesOnlyTheTitleAndSavesOnRelease() {
        Fixture f=new Fixture();Rectangle initial=f.bank.bounds();
        Point title=f.titlePoint();assertTrue(f.press(title).isConsumed());
        f.drag(new Point(title.x+80,title.y+65));f.render();
        assertEquals(initial.x+80,f.bank.bounds().x);assertEquals(initial.y+65,f.bank.bounds().y);
        assertEquals(f.bank.bounds().width,f.modal.bounds().width);assertEquals(f.bank.bounds().height+3,f.modal.bounds().height);
        assertEquals(initial.width,f.bank.bounds().width);assertTrue(f.settings.isEmpty());
        assertTrue(f.release(new Point(title.x+80,title.y+65)).isConsumed());f.render();f.flush();
        assertEquals(initial.x+80,f.bank.bounds().x);assertEquals(initial.y+65,f.bank.bounds().y);
        assertTrue(f.settings.containsKey("bankWindowPosition"));assertFalse(f.settings.containsKey("customWidth"));
        assertTrue(f.click(title).isConsumed());assertFalse(f.click(title).isConsumed());
        f.controller.onWidgetClosed(new WidgetClosed(InterfaceID.BANKMAIN,0,true));
        f.build();f.render();assertEquals(initial.x+80,f.bank.bounds().x);
    }

    @Test public void cornerResizeReflowsNativeItemsAndPreservesTheirActionsAndDimensions() {
        Fixture f=new Fixture();Cell item=f.add(InterfaceID.Bankmain.ITEMS+100,f.items,51,0,36,32,0,0,0,0);
        when(item.widget.getType()).thenReturn(net.runelite.api.widgets.WidgetType.GRAPHIC);
        when(item.widget.getItemId()).thenReturn(385);f.items.children.add(item.widget);
        Rectangle before=f.bank.bounds();Point corner=f.corner();
        assertTrue(f.press(corner).isConsumed());f.drag(new Point(corner.x+200,corner.y+90));f.render();
        assertEquals(before.width+200,f.bank.bounds().width);assertEquals(before.height+90,f.bank.bounds().height);
        verify(f.controller.client,never()).runScript(any(Object[].class));
        f.controller.onClientTick(new ClientTick());f.flush();
        verify(f.controller.client).runScript(any(Object[].class));
        assertEquals(36,item.bounds().width);assertEquals(32,item.bounds().height);
        verify(item.widget,never()).clearActions();verify(item.widget,never()).setOnOpListener(any(Object[].class));
        f.release(new Point(corner.x+200,corner.y+90));f.render();f.flush();
        assertEquals(before.width+200,f.settings.get("width"));assertEquals(before.height+90,f.settings.get("height"));
        assertEquals(true,f.settings.get("customWidth"));assertEquals(true,f.settings.get("customHeight"));
    }

    @Test public void draggingClampsToTheGameAreaAndResizingKeepsMinimumDimensions() {
        Fixture f=new Fixture();Point title=f.titlePoint();f.press(title);f.drag(new Point(-500,-500));f.render();
        assertEquals(f.area.bounds().x,f.bank.bounds().x);assertEquals(f.area.bounds().y,f.bank.bounds().y);
        f.release(new Point(-500,-500));f.render();f.flush();
        Point corner=f.corner();f.press(corner);f.drag(new Point(0,0));f.render();
        assertEquals(488,f.bank.bounds().width);assertEquals(200,f.bank.bounds().height);
        f.drag(new Point(10000,10000));f.render();assertEquals(f.area.bounds(),f.bank.bounds());
    }

    @Test public void closeAndOtherHeaderButtonsKeepTheirNormalClicks() {
        Fixture f=new Fixture();
        Cell close=f.add(1,f.frame,8,8,20,20,2,0,0,0);when(close.widget.getActions()).thenReturn(new String[]{"Close"});
        f.frame.children.add(close.widget);
        Cell extra=f.add(2,f.frame,180,8,80,20,0,0,0,0);when(extra.widget.getOnOpListener()).thenReturn(new Object[]{1});
        f.frame.children.add(extra.widget);f.render();
        for(Point p:new Point[]{new Point(close.bounds().x+2,close.bounds().y+2),new Point(extra.bounds().x+2,extra.bounds().y+2),new Point(f.items.bounds().x+50,f.items.bounds().y+50)})
            assertFalse(f.press(p).isConsumed());
        assertFalse(f.controller.windowDrag.mousePressed(event(MouseEvent.MOUSE_PRESSED,f.titlePoint(),MouseEvent.ALT_DOWN_MASK)).isConsumed());
    }

    @Test public void awtMouseCallbacksNeverReadClientOrWidgetState() throws Exception {
        Fixture f=new Fixture();Point title=f.titlePoint();List<Object> mocks=new ArrayList<>();mocks.add(f.controller.client);mocks.add(f.controller.configs);
        for(Cell cell:f.cells.values())mocks.add(cell.widget);clearInvocations(mocks.toArray());
        SwingUtilities.invokeAndWait(() -> {f.press(title);f.drag(new Point(title.x+40,title.y+30));f.release(new Point(title.x+40,title.y+30));f.click(title);});
        verifyNoInteractions(mocks.toArray());
    }

    @Test public void focusLossCancelsTheDraftAndStillConsumesItsRelease() {
        Fixture f=new Fixture();Rectangle initial=f.bank.bounds();Point title=f.titlePoint();
        f.press(title);f.drag(new Point(title.x+70,title.y+50));f.render();
        FocusChanged focus=new FocusChanged();focus.setFocused(false);f.controller.onFocusChanged(focus);f.render();
        assertEquals(initial,f.bank.bounds());assertTrue(f.release(title).isConsumed());f.render();assertTrue(f.settings.isEmpty());
    }

    @Test public void profileSwitchCannotSaveAnOldGestureOrRunAnOldRebuild() {
        Fixture f=new Fixture();Point title=f.titlePoint();
        f.press(title);f.drag(new Point(title.x+70,title.y+50));f.render();f.release(title);
        f.controller.queueRebuild();when(f.controller.configs.getProfile().getId()).thenReturn(20L);
        f.controller.onProfileChanged(new ProfileChanged());f.render();
        List<Runnable> pending=new ArrayList<>(f.pending);f.pending.clear();
        clearInvocations(f.controller.client);pending.get(0).run();verify(f.controller.client,never()).runScript(any(Object[].class));
        for(int i=1;i<pending.size();i++)pending.get(i).run();assertTrue(f.settings.isEmpty());
    }

    @Test public void resizingTheClientDoesNotOverwriteTheSavedPosition() {
        Fixture f=new Fixture();Point title=f.titlePoint();f.press(title);f.release(new Point(title.x+180,title.y+160));f.render();f.flush();
        Rectangle large=f.bank.bounds();String saved=(String)f.settings.get("bankWindowPosition");
        f.area.widget.setOriginalWidth(600);f.area.widget.setOriginalHeight(450);f.render();
        assertTrue(f.area.bounds().contains(f.bank.bounds()));assertEquals(saved,f.settings.get("bankWindowPosition"));
        f.area.widget.setOriginalWidth(1000);f.area.widget.setOriginalHeight(800);f.render();
        assertEquals(large,f.bank.bounds());assertEquals(saved,f.settings.get("bankWindowPosition"));
    }

    @Test public void nativeSizeChecksCannotShrinkAStoredWindowDuringAltOrMovement() {
        Fixture f=new Fixture();Point title=f.titlePoint();
        f.press(title);f.release(new Point(title.x+80,title.y+65));f.render();f.flush();
        Rectangle saved=f.bank.bounds();String placement=(String)f.settings.get("bankWindowPosition");
        when(f.controller.client.isKeyPressed(net.runelite.api.KeyCode.KC_ALT)).thenReturn(true);
        for(int i=0;i<30;i++) {
            f.nativeSizeCheck();f.bus.post(new BeforeRender());
            assertEquals(saved,f.bank.bounds());assertEquals(placement,f.settings.get("bankWindowPosition"));
        }
        when(f.controller.client.isKeyPressed(net.runelite.api.KeyCode.KC_ALT)).thenReturn(false);
        for(int i=0;i<6;i++) {
            title=f.titlePoint();f.press(title);Point end=new Point(title.x-5,title.y-5);
            f.drag(end);f.render();f.nativeSizeCheck();f.release(end);f.render();f.flush();f.nativeSizeCheck();
            assertEquals(saved.getSize(),f.bank.bounds().getSize());
        }
        assertFalse(f.settings.containsKey("customHeight"));
    }

    @Test public void storedDimensionsSurviveNativeResetAndSmallScreenClamping() {
        Fixture f=new Fixture();Point title=f.titlePoint();f.press(title);f.release(new Point(title.x+30,title.y+30));f.render();f.flush();
        Rectangle saved=f.bank.bounds();String placement=(String)f.settings.get("bankWindowPosition");
        f.area.widget.setOriginalHeight(350);f.render();
        for(int i=0;i<10;i++)f.nativeSizeCheck();
        assertTrue(f.area.bounds().contains(f.bank.bounds()));assertEquals(placement,f.settings.get("bankWindowPosition"));
        f.area.widget.setOriginalHeight(800);f.nativeSizeCheck();f.render();assertEquals(saved,f.bank.bounds());
        f.settings.put("customHeight",true);f.settings.put("height",850);f.area.widget.setOriginalHeight(1000);f.render();
        for(int i=0;i<10;i++)f.nativeSizeCheck();
        assertEquals(850,f.bank.bounds().height);
    }

    @Test public void fixedModeAndDisableRestoreTheNativeModalAndWindow() {
        Fixture f=new Fixture();int[] nativeModal=f.modal.geometry.clone(),nativeBank=f.bank.geometry.clone();
        Point title=f.titlePoint();f.press(title);f.release(new Point(title.x+80,title.y+65));f.render();f.flush();
        when(f.controller.client.isResized()).thenReturn(false);f.render();
        assertArrayEquals(nativeModal,f.modal.geometry);assertArrayEquals(nativeBank,f.bank.geometry);assertFalse(f.press(f.titlePoint()).isConsumed());
        when(f.controller.client.isResized()).thenReturn(true);f.render();
        f.controller.shutDown();f.flush();assertArrayEquals(nativeModal,f.modal.geometry);assertArrayEquals(nativeBank,f.bank.geometry);
    }

    @Test public void replacementModalIsNotRestoredUsingAnOldWidgetsGeometry() {
        Fixture f=new Fixture();Point title=f.titlePoint();f.press(title);f.release(new Point(title.x+50,title.y+50));f.render();f.flush();
        Cell replacement=new Cell(f.modal.id,f.area,18,22,600,550,0,0,0,0);
        when(f.controller.client.getWidget(f.modal.id)).thenReturn(replacement.widget);
        f.controller.shutDown();f.flush();assertArrayEquals(new int[]{18,22,600,550,0,0,0,0},replacement.geometry);
    }

    @Test public void hiddenItemsOpenMenusAndNativeDragsReleaseTheHitAreas() {
        Fixture f=new Fixture();when(f.items.widget.isHidden()).thenReturn(true);f.render();assertFalse(f.press(f.titlePoint()).isConsumed());
        when(f.items.widget.isHidden()).thenReturn(false);when(f.controller.client.isMenuOpen()).thenReturn(true);f.render();assertFalse(f.press(f.titlePoint()).isConsumed());
        when(f.controller.client.isMenuOpen()).thenReturn(false);when(f.controller.client.getDraggedWidget()).thenReturn(mock(Widget.class));f.render();assertFalse(f.press(f.titlePoint()).isConsumed());
        when(f.controller.client.getDraggedWidget()).thenReturn(null);when(f.controller.client.getGameState()).thenReturn(GameState.LOGIN_SCREEN);f.render();assertFalse(f.press(f.titlePoint()).isConsumed());
    }

    @Test public void placementRejectsCorruptValuesAndFitsWithoutSavingClampedCoordinates() {
        for(String value:new String[]{null,"","v2:1:2:488:300:1000:800","v1:-1:0:488:300:1000:800","v1:900:0:488:300:1000:800","v1:0:0:0:0:0:0","v1:0:0:488:300:2147483647:800"})
            assertNull(BankWindowPlacement.read(value));
        BankWindowPlacement placement=new BankWindowPlacement(new Rectangle(512,400,488,400),1000,800);
        assertEquals(new Rectangle(112,50,488,400),placement.fit(600,450,488,400));
        assertEquals(new Rectangle(0,0,320,180),placement.fit(320,180,488,400));
        assertEquals(placement.write(),BankWindowPlacement.read(placement.write()).write());
        assertEquals(placement.bounds,placement.fit(1000,800,488,400));
    }

    private static MouseEvent event(int type,Point p,int modifiers) {return new MouseEvent(new Canvas(),type,1,modifiers,p.x,p.y,1,false,MouseEvent.BUTTON1);}

    private static final class Fixture
    {
        final BankLayoutController controller=new BankLayoutController();
        final EventBus bus=new EventBus();
        final Map<Integer,Cell> cells=new HashMap<>();
        final Map<String,Object> settings=new HashMap<>();
        final List<Runnable> pending=new ArrayList<>();
        final Cell area,modal,infinite,bank,items,title,frame;

        Fixture() {
            controller.client=mock(Client.class);controller.clientThread=mock(ClientThread.class);controller.eventBus=bus;
            controller.configs=mock(ConfigManager.class,RETURNS_DEEP_STUBS);when(controller.configs.getProfile().getId()).thenReturn(10L);
            controller.config=mock(WindowMarkersConfig.class);
            when(controller.config.bankLayoutEnabled()).thenReturn(true);when(controller.config.bankActionRail()).thenReturn(true);
            when(controller.config.bankWindowPosition()).thenAnswer(c -> settings.getOrDefault("bankWindowPosition",""));
            when(controller.config.customWidth()).thenAnswer(c -> settings.getOrDefault("customWidth",false));
            when(controller.config.customHeight()).thenAnswer(c -> settings.getOrDefault("customHeight",false));
            when(controller.config.width()).thenAnswer(c -> settings.getOrDefault("width",488));
            when(controller.config.height()).thenAnswer(c -> settings.getOrDefault("height",400));
            when(controller.client.isResized()).thenReturn(true);when(controller.client.getGameState()).thenReturn(GameState.LOGGED_IN);
            doAnswer(c -> {pending.add(c.getArgument(0));return null;}).when(controller.clientThread).invokeLater(any(Runnable.class));
            org.mockito.stubbing.Answer<Object> save=c -> {
                String key=c.getArgument(1);settings.put(key,c.getArgument(2));
                ConfigChanged changed=new ConfigChanged();changed.setGroup("windowmarkers");changed.setKey(key);bus.post(changed);return null;
            };
            doAnswer(save).when(controller.configs).setConfiguration(eq("windowmarkers"),anyString(),anyString());
            doAnswer(save).when(controller.configs).setConfiguration(eq("windowmarkers"),anyString(),any(Object.class));
            area=add(100,null,20,25,1000,800,0,0,0,0);
            modal=add(101,area,0,0,512,500,1,1,0,0);
            infinite=add(InterfaceID.Bankmain.INFINITE,modal,0,0,0,0,0,0,1,1);
            bank=add(InterfaceID.Bankmain.UNIVERSE,infinite,0,0,488,400,1,1,0,0);
            frame=add(InterfaceID.Bankmain.FRAME,bank,0,0,0,0,0,0,1,1);
            title=add(InterfaceID.Bankmain.TITLE,frame,50,6,100,24,0,0,1,0);
            Cell container=add(InterfaceID.Bankmain.ITEMS_CONTAINER,bank,5,40,10,45,0,0,1,1);
            items=add(InterfaceID.Bankmain.ITEMS,container,1,39,18,81,0,0,1,1);
            when(items.widget.getOnInvTransmitListener()).thenReturn(new Object[]{ScriptID.BANKMAIN_BUILD});
            when(controller.client.getIntStack()).thenReturn(new int[9]);when(controller.client.getIntStackSize()).thenReturn(9);
            doAnswer(c -> {build();return null;}).when(controller.client).runScript(any(Object[].class));
            controller.startUp();flush();build();render();clearInvocations(controller.client);
        }

        Cell add(int id,Cell parent,int x,int y,int width,int height,int xm,int ym,int wm,int hm) {
            Cell cell=new Cell(id,parent,x,y,width,height,xm,ym,wm,hm);cells.put(id,cell);
            when(controller.client.getWidget(id)).thenReturn(cell.widget);when(cell.widget.getParent()).thenReturn(parent==null?null:parent.widget);return cell;
        }
        void build() {bus.post(new ScriptPreFired(ScriptID.BANKMAIN_FINISHBUILDING));}
        void nativeSizeCheck() {
            bank.widget.setSize(488,Math.min(800,infinite.bounds().height-3),0,0);
            bank.widget.setPos(0,(infinite.bounds().height-bank.bounds().height)/2,1,2);
            bus.post(new net.runelite.api.events.ScriptPostFired(ScriptID.BANKMAIN_SIZE_CHECK));
        }
        void render() {controller.onBeforeRender(new BeforeRender());}
        void flush() {int guard=0;while(!pending.isEmpty() && guard++<30) {List<Runnable> batch=new ArrayList<>(pending);pending.clear();batch.forEach(Runnable::run);}assertTrue(pending.isEmpty());}
        Point titlePoint() {return new Point(title.bounds().x+10,title.bounds().y+10);}
        Point corner() {Rectangle r=bank.bounds();return new Point(r.x+r.width-2,r.y+r.height-2);}
        MouseEvent press(Point p) {return controller.windowDrag.mousePressed(event(MouseEvent.MOUSE_PRESSED,p,0));}
        void drag(Point p) {controller.windowDrag.mouseDragged(event(MouseEvent.MOUSE_DRAGGED,p,MouseEvent.BUTTON1_DOWN_MASK));}
        MouseEvent release(Point p) {return controller.windowDrag.mouseReleased(event(MouseEvent.MOUSE_RELEASED,p,0));}
        MouseEvent click(Point p) {return controller.windowDrag.mouseClicked(event(MouseEvent.MOUSE_CLICKED,p,0));}
    }
}
