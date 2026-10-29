package com.windowmarkers;

import java.awt.Rectangle;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.*;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.EventBus;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class BankControlsTest
{
    @Test public void settingsColumnsAndRowsStaySeparateAtEveryBankSizeAndRestore() {
        for(int bankWidth:new int[]{488,728,1100}) for(int bankHeight:new int[]{200,330,700}) {
            Client client=mock(Client.class);EventBus bus=new EventBus();
            BankLayoutController controller=new BankLayoutController();controller.client=client;controller.eventBus=bus;
            controller.clientThread=mock(ClientThread.class);controller.config=mock(WindowMarkersConfig.class);
            when(controller.config.bankLayoutEnabled()).thenReturn(true);
            when(controller.config.customWidth()).thenReturn(true);when(controller.config.width()).thenReturn(bankWidth);
            when(controller.config.customHeight()).thenReturn(true);when(controller.config.height()).thenReturn(bankHeight);
            Cell screen=new Cell(-1,null,0,0,1200,900,0,0,0,0);
            Cell root=new Cell(InterfaceID.Bankmain.UNIVERSE,screen,0,0,488,800,0,0,0,0);
            Cell menu=new Cell(InterfaceID.Bankmain.MENU_CONTAINER,root,0,5,10,40,1,2,1,1);
            Cell items=new Cell(InterfaceID.Bankmain.ITEMS,root,0,0,478,600,0,0,0,0);
            when(items.widget.isHidden()).thenReturn(true);
            when(client.getWidget(InterfaceID.Bankmain.INFINITE)).thenReturn(screen.widget);
            when(client.getWidget(InterfaceID.Bankmain.ITEMS_CONTAINER)).thenReturn(mock(Widget.class));
            for(Cell cell:Arrays.asList(root,menu,items)) when(client.getWidget(cell.id)).thenReturn(cell.widget);
            List<Cell> rows=new ArrayList<>();
            // Native init captures the first three rows' Y from a tall parent.
            // The two columns use offsets from opposite parent edges (script 7859).
            for(int i=0;i<10;i++) {
                int y=i<6?(760-28)/2-108+(i/2)*28:23+(i/2)*28;
                Cell row=new Cell(InterfaceID.Bankmain.INCINERATOR_TOGGLE+i,menu,251,y,220,28,i%2==0?2:0,0,0,0);
                when(client.getWidget(row.id)).thenReturn(row.widget);rows.add(row);
            }
            controller.startUp();clearInvocations(controller.clientThread);
            bus.post(new BeforeRender());
            assertEquals(bankWidth,root.bounds().width);assertEquals(Math.max(330,bankHeight),root.bounds().height);
            assertEquals(478,menu.bounds().width);assertEquals(290,menu.bounds().height);
            assertEquals((bankWidth-478)/2,menu.bounds().x-root.bounds().x);assertEquals(35,menu.bounds().y-root.bounds().y);
            for(int i=0;i<rows.size();i++) {
                Rectangle row=rows.get(i).bounds();assertTrue(menu.bounds().contains(row));
                assertTrue("Checkbox rows must stay above native buttons",row.y+row.height<=menu.bounds().y+174);
                for(int j=i+1;j<rows.size();j++) assertFalse("Native checkbox hit boxes must not overlap",row.intersects(rows.get(j).bounds()));
                verify(rows.get(i).widget,never()).clearActions();verify(rows.get(i).widget,never()).setOnOpListener(any(Object[].class));
            }
            // Native size checks and a settings/item transition must keep the same result.
            root.widget.setOriginalWidth(488);bus.post(new ScriptPostFired(ScriptID.BANKMAIN_SIZE_CHECK));
            assertEquals(bankWidth,root.bounds().width);
            for(int i=0;i<12;i++) bus.post(new ClientTick());
            verifyNoInteractions(controller.clientThread);
            when(menu.widget.isHidden()).thenReturn(true);when(items.widget.isHidden()).thenReturn(false);
            bus.post(new BeforeRender());assertEquals(bankHeight,root.bounds().height);
            when(menu.widget.isHidden()).thenReturn(false);when(items.widget.isHidden()).thenReturn(true);
            bus.post(new BeforeRender());assertEquals(Math.max(330,bankHeight),root.bounds().height);
            doAnswer(call -> {((Runnable)call.getArgument(0)).run();return null;}).when(controller.clientThread).invokeLater(any(Runnable.class));
            controller.shutDown();assertArrayEquals(new int[]{0,5,10,40,1,2,1,1},menu.geometry);
            assertEquals((760-28)/2-108,rows.get(0).geometry[1]);
        }
    }

    @Test public void presetButtonUsesNativeArtworkWithoutTouchingOtherPluginsWidgets() {
        BankCataloguePicker picker=new BankCataloguePicker();picker.client=mock(Client.class);
        Cell frame=new Cell(InterfaceID.Bankmain.FRAME,null,100,100,488,400,0,0,0,0);
        Cell close=new Cell(1,frame,461,7,20,20,0,0,0,0),camera=new Cell(2,frame,437,7,20,20,0,0,0,0);
        frame.children.add(close.widget);frame.children.add(camera.widget);when(close.widget.getActions()).thenReturn(new String[]{"Close"});
        when(picker.client.getWidget(frame.id)).thenReturn(frame.widget);
        AtomicInteger opened=new AtomicInteger();picker.update(true,opened::incrementAndGet);picker.update(true,opened::incrementAndGet);
        assertEquals(12,frame.children.size());Widget label=frame.children.get(11);
        assertEquals("View presets",label.getText());assertEquals(495,label.getFontId());
        Rectangle button=label.getBounds();assertEquals(new Rectangle(437,106,96,22),button);
        assertFalse(button.intersects(camera.bounds()));assertFalse(button.intersects(close.bounds()));
        ((JavaScriptCallback)label.getOnOpListener()[0]).run(null);assertEquals(1,opened.get());
        Set<Integer> sprites=new HashSet<>();for(int i=2;i<11;i++) sprites.add(frame.children.get(i).getSpriteId());
        assertEquals(new HashSet<>(Arrays.asList(1141,1142,1143,1144,1145,1146,1147,1148,1149)),sprites);
        frame.widget.setOriginalWidth(728);close.widget.setOriginalX(701);camera.widget.setOriginalX(677);
        picker.update(true,opened::incrementAndGet);assertEquals(677,label.getBounds().x);
        Widget other=mock(Widget.class);frame.children.add(other);
        picker.clear();assertSame(other,frame.children.get(12));assertSame(camera.widget,frame.children.get(1));
        for(int i=2;i<12;i++) assertNull(frame.children.get(i));
        for(int i=0;i<10;i++) {picker.update(true,opened::incrementAndGet);picker.clear();}
        assertEquals("Repeated enable/close must not accumulate hidden controls",13,frame.children.size());
        verify(close.widget,never()).clearActions();verify(camera.widget,never()).setHidden(anyBoolean());verify(other,never()).setHidden(anyBoolean());
    }

    @Test public void headerMakesRoomForButtonAndTracksNativeTitleChangesWithoutLosingText() {
        BankHeaderTitle layout=new BankHeaderTitle();Cell title=new Cell(3,null,0,6,476,24,1,0,0,0);
        FontTypeFace font=mock(FontTypeFace.class);when(title.widget.getFont()).thenReturn(font);
        when(font.getTextWidth(anyString())).thenAnswer(call -> net.runelite.client.util.Text.removeTags(call.getArgument(0)).length()*6);
        String full="Bank view: Alchemical Hydra - not enough gear in bank";title.widget.setText(full);
        layout.update(title.widget,331);assertTrue(title.widget.getText().endsWith("..."));assertTrue(font.getTextWidth(title.widget.getText())<=279);
        layout.update(title.widget,700);assertEquals(full,title.widget.getText());
        title.widget.setText("Bank settings menu");layout.update(title.widget,331);
        layout.clear();assertEquals("Bank settings menu",title.widget.getText());assertArrayEquals(new int[]{0,6,476,24,1,0,0,0},title.geometry);
        layout.update(title.widget,331);title.widget.setText("Native replacement");layout.clear();assertEquals("Native replacement",title.widget.getText());
    }

    /** A small geometry model using RuneLite's absolute/centre/right/minus modes. */
    private static final class Cell {
        final int id;final Widget widget;final Cell parent;final int[] geometry;
        final List<Widget> children=new ArrayList<>();final Map<String,Object> properties=new HashMap<>();
        Cell(int id,Cell parent,int x,int y,int width,int height,int xMode,int yMode,int widthMode,int heightMode) {
            this.id=id;this.parent=parent;geometry=new int[]{x,y,width,height,xMode,yMode,widthMode,heightMode};
            List<String> fields=Arrays.asList("OriginalX","OriginalY","OriginalWidth","OriginalHeight","XPositionMode","YPositionMode","WidthMode","HeightMode");
            widget=mock(Widget.class,call -> {
                String method=call.getMethod().getName();Object[] args=call.getArguments();
                if(method.startsWith("get") && fields.contains(method.substring(3))) return geometry[fields.indexOf(method.substring(3))];
                if(method.startsWith("set") && fields.contains(method.substring(3))) {geometry[fields.indexOf(method.substring(3))]=(int)args[0];return call.getMock();}
                switch(method) {
                    case "getId":return id;
                    case "getWidth":return bounds().width;
                    case "getHeight":return bounds().height;
                    case "getBounds":return bounds();
                    case "getChildren":case "getDynamicChildren":return children.toArray(new Widget[0]);
                    case "getStaticChildren":return new Widget[0];
                    case "setChildren":children.clear();children.addAll(Arrays.asList((Widget[])args[0]));return null;
                    case "setPos":geometry[0]=(int)args[0];geometry[1]=(int)args[1];geometry[4]=(int)args[2];geometry[5]=(int)args[3];return call.getMock();
                    case "setSize":geometry[2]=(int)args[0];geometry[3]=(int)args[1];geometry[6]=(int)args[2];geometry[7]=(int)args[3];return call.getMock();
                    case "createChild":Cell child=new Cell(id,this,0,0,0,0,0,0,0,0);children.add(child.widget);return child.widget;
                }
                if(method.startsWith("set") && args.length==1) {properties.put(method.substring(3),args[0]);return call.getMethod().getReturnType()==Widget.class?call.getMock():null;}
                if(method.equals("setOnOpListener")) {properties.put("OnOpListener",args.clone());return call.getMock();}
                if(method.startsWith("get") && properties.containsKey(method.substring(3))) {
                    Object value=properties.get(method.substring(3));
                    return method.equals("getOnOpListener") && !(value instanceof Object[])?new Object[]{value}:value;
                }
                return org.mockito.Answers.RETURNS_DEFAULTS.answer(call);
            });
        }
        Rectangle bounds() {
            Rectangle p=parent==null?new Rectangle():parent.bounds();int[] g=geometry;
            int w=g[6]==1?p.width-g[2]:g[2],h=g[7]==1?p.height-g[3]:g[3];
            int x=g[4]==1?(p.width-w)/2+g[0]:g[4]==2?p.width-w-g[0]:g[0];
            int y=g[5]==1?(p.height-h)/2+g[1]:g[5]==2?p.height-h-g[1]:g[1];
            return new Rectangle(p.x+x,p.y+y,w,h);
        }
    }
}
