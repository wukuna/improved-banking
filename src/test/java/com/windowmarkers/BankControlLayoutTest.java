package com.windowmarkers;

import java.awt.Rectangle;
import java.util.*;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.events.ConfigChanged;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import com.windowmarkers.BankControlLayout.Control;
import com.windowmarkers.BankControlsTest.Cell;

public class BankControlLayoutTest
{
    @Test public void savedPreferencesRoundTripAndRejectUnknownControls() {
        BankControlLayout layout=BankControlLayout.read("v1;SEARCH:1200:L;SWAP:-1:B;SWAP:500:L;NOT_A_WIDGET:80:B;NOTE:bad:L;");
        assertEquals(12,layout.entries.size());assertEquals(Control.SWAP,layout.entries.get(0).control);
        assertEquals(0,layout.entries.get(0).position);assertFalse(layout.entries.get(0).left);
        BankControlLayout.Entry search=layout.entries.stream().filter(e -> e.control==Control.SEARCH).findFirst().get();
        assertEquals(1000,search.position);assertTrue(search.left);
        layout.move(layout.entries.get(11),-1);
        assertEquals(layout.write(),BankControlLayout.read(layout.write()).write());
        assertEquals(BankControlLayout.read("").write(),BankControlLayout.read("future;SEARCH:1:L").write());
    }
    @Test public void everyControlFitsWithoutOverlapAndOverflowReturnsToBottom() {
        Random random=new Random(20261029);
        for(int width:new int[]{478,525,526,718,1100})for(int height:new int[]{160,290,760})for(int iteration=0;iteration<20;iteration++) {
            BankControlLayout layout=BankControlLayout.read("");
            for(BankControlLayout.Entry entry:layout.entries){entry.position=random.nextInt(1001);entry.left=random.nextBoolean();}layout.sort();
            Set<Control> visible=EnumSet.allOf(Control.class);if(iteration%2==0)visible.remove(Control.WORN);
            BankControlLayout.Plan plan=layout.plan(width,height,visible);
            assertEquals(visible,plan.bounds.keySet());
            for(Map.Entry<Control,Rectangle> entry:plan.bounds.entrySet()) {
                Rectangle r=entry.getValue();assertTrue(new Rectangle(0,0,width,height).contains(r));
                assertEquals(entry.getKey().width,r.width);assertEquals(36,r.height);
                if(plan.docked.contains(entry.getKey())) {assertTrue(r.x>=40);assertTrue(r.x+r.width<100);}
                for(Map.Entry<Control,Rectangle> other:plan.bounds.entrySet())if(other.getKey()!=entry.getKey())assertFalse(r.intersects(other.getValue()));
            }
            if(width<526)assertEquals(0,plan.inset);
        }
    }
    @Test public void nativeWidgetsKeepTheirActionsAndSizeAndRestoreAfterDisable() {
        Fixture f=new Fixture();Map<Integer,int[]> original=new HashMap<>();
        for(Cell cell:f.cells.values())original.put(cell.id,cell.geometry.clone());
        f.build();
        assertEquals(49,f.items.geometry[0]);assertEquals(66,f.items.geometry[2]);
        assertEquals(0,f.tags.geometry[0]); // Bank Tags' parent and its dynamic column are untouched.
        Rectangle tagBounds=f.tagColumn.bounds();
        for(Control control:Control.values()) {
            Cell button=f.cells.get(control.button),icon=f.cells.get(control.graphic);
            assertEquals(control.width,button.bounds().width);assertEquals(36,button.bounds().height);
            assertEquals(button.bounds().x+control.iconX,icon.bounds().x);assertEquals(button.bounds().y+control.iconY,icon.bounds().y);
            assertFalse(tagBounds.intersects(button.bounds()));
            verify(button.widget,never()).setHidden(anyBoolean());verify(button.widget,never()).clearActions();
            verify(button.widget,never()).setOnOpListener(any(Object[].class));verify(button.widget,never()).setClickMask(anyInt());
            verify(button.widget,never()).setSize(anyInt(),anyInt(),anyInt(),anyInt());
        }
        assertFalse(f.controller.controls.changed());
        for(int i=0;i<30;i++)f.bus.post(new BeforeRender());
        assertFalse(f.controller.controls.changed());
        verify(f.controller.client,never()).runScript(any(Object[].class));
        // A native button script may reset only one coordinate between frames.
        Cell search=f.cells.get(Control.SEARCH.button);search.widget.setOriginalX(273);f.bus.post(new BeforeRender());
        assertEquals(42,search.geometry[0]);assertFalse(f.controller.controls.changed());
        f.controller.shutDown();
        for(Control control:Control.values())for(int id:new int[]{control.button,control.graphic})assertArrayEquals(original.get(id),f.cells.get(id).geometry);
        assertArrayEquals(original.get(InterfaceID.Bankmain.QUANTITY_LAYER),f.cells.get(InterfaceID.Bankmain.QUANTITY_LAYER).geometry);
        assertArrayEquals(original.get(InterfaceID.Bankmain.BOTTOM),f.cells.get(InterfaceID.Bankmain.BOTTOM).geometry);
        assertArrayEquals(original.get(InterfaceID.Bankmain.ITEMS),f.items.geometry);
    }
    @Test public void hiddenButtonsProfilesResizesAndReopeningDoNotLoseControlsOrPreferences() {
        Fixture f=new Fixture();f.build();String saved=f.controller.config.bankControlLayout();
        Cell hidden=f.cells.get(Control.INVENTORY.button);when(hidden.widget.isHidden()).thenReturn(true);
        f.bus.post(new BeforeRender());assertFalse(f.controller.controls.plan(f.container.widget).bounds.containsKey(Control.INVENTORY));
        verify(hidden.widget,never()).setHidden(false);
        when(hidden.widget.isHidden()).thenReturn(false);f.bus.post(new BeforeRender());
        assertTrue(f.controller.controls.plan(f.container.widget).bounds.containsKey(Control.INVENTORY));
        f.container.widget.setOriginalWidth(478);f.bus.post(new BeforeRender());
        assertEquals("A parent-only resize is detected before rendering",1,f.items.geometry[0]);
        f.build();assertEquals(1,f.items.geometry[0]);assertEquals(18,f.items.geometry[2]);
        assertEquals(36,f.cells.get(InterfaceID.Bankmain.BOTTOM).bounds().height);
        assertEquals(saved,f.controller.config.bankControlLayout());
        f.bus.post(new WidgetClosed(InterfaceID.BANKMAIN,0,true));f.container.widget.setOriginalWidth(718);f.build();
        assertEquals(49,f.items.geometry[0]);
        when(f.controller.config.customBankControls()).thenReturn(false);
        ConfigChanged event=new ConfigChanged();event.setGroup("windowmarkers");event.setKey("customBankControls");f.bus.post(event);
        assertEquals(1,f.items.geometry[0]);assertEquals(36,f.cells.get(InterfaceID.Bankmain.BOTTOM).bounds().height);
        when(f.controller.config.customBankControls()).thenReturn(true);f.build();
        when(f.controller.config.bankControlLayout()).thenReturn("");f.bus.post(new net.runelite.client.events.ProfileChanged());f.build();
        assertEquals(1,f.items.geometry[0]);
    }
    static final class Fixture {
        final BankLayoutController controller=new BankLayoutController();final EventBus bus=new EventBus();
        final Map<Integer,Cell> cells=new HashMap<>();final Cell root,container,items,tags,tagColumn;
        Fixture() {
            controller.client=mock(Client.class);controller.clientThread=mock(ClientThread.class);controller.eventBus=bus;controller.config=mock(WindowMarkersConfig.class);
            when(controller.config.bankLayoutEnabled()).thenReturn(true);when(controller.config.customBankControls()).thenReturn(true);
            when(controller.config.depositButtons()).thenReturn(DepositButtons.BOTTOM);
            BankControlLayout layout=BankControlLayout.read("");for(BankControlLayout.Entry e:layout.entries)e.left=e.control==Control.SEARCH || e.control==Control.INVENTORY || e.control==Control.X;
            when(controller.config.bankControlLayout()).thenReturn(layout.write());
            Cell screen=new Cell(-1,null,0,0,1200,900,0,0,0,0);
            root=add(InterfaceID.Bankmain.UNIVERSE,screen,0,0,728,700,0,0,0,0);
            container=add(InterfaceID.Bankmain.ITEMS_CONTAINER,root,0,5,718,660,0,0,0,0);
            tags=add(InterfaceID.Bankmain.TABS,container,0,0,478,40,1,0,0,0);
            tagColumn=new Cell(1000,container,0,54,40,540,0,0,0,0);container.children.add(tagColumn.widget);
            items=add(InterfaceID.Bankmain.ITEMS,container,1,39,460,81,0,2,0,1);
            add(InterfaceID.Bankmain.SCROLLBAR,container,1,39,16,81,2,2,0,1);
            Cell bottom=add(InterfaceID.Bankmain.BOTTOM,container,0,1,0,36,1,2,1,0);
            Cell quantity=add(InterfaceID.Bankmain.QUANTITY_LAYER,bottom,80,0,188,0,0,0,0,1);
            int[] xs={2,39,2,39,76,113,150,273,323,366,403,440};
            for(Control control:Control.values()) {
                Cell parent=control.quantity()?quantity:bottom;int x=xs[control.ordinal()];
                add(control.button,parent,x,0,control.width,36,0,0,0,0);
                add(control.graphic,parent,x+control.iconX,control.quantity()?7:control.iconY,control.quantity()?25:29,control.quantity()?15:22,0,control.quantity()?2:0,0,control.quantity()?1:0);
            }
            when(controller.client.getWidget(InterfaceID.Bankmain.INFINITE)).thenReturn(screen.widget);
            when(controller.client.getIntStack()).thenReturn(new int[9]);when(controller.client.getIntStackSize()).thenReturn(9);
            doAnswer(call -> {((Runnable)call.getArgument(0)).run();return null;}).when(controller.clientThread).invokeLater(any(Runnable.class));controller.startUp();
        }
        Cell add(int id,Cell parent,int x,int y,int width,int height,int xm,int ym,int wm,int hm) {
            Cell cell=new Cell(id,parent,x,y,width,height,xm,ym,wm,hm);cells.put(id,cell);when(controller.client.getWidget(id)).thenReturn(cell.widget);return cell;
        }
        void build(){bus.post(new ScriptPreFired(ScriptID.BANKMAIN_FINISHBUILDING));}
    }
}
