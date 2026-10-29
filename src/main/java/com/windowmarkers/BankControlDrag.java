package com.windowmarkers;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.*;
import net.runelite.api.KeyCode;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.input.MouseAdapter;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/** Mouse callbacks use only a client-thread snapshot; widget/config work stays on the client thread. */
final class BankControlDrag extends MouseAdapter
{
    private final BankLayoutController owner;
    private volatile Snapshot snapshot;
    private volatile Gesture gesture;
    private boolean captured,suppressClick;
    private volatile boolean cancel;
    private BankControlLayout draft;
    private String appliedDraft;
    final Overlay overlay=new DragOverlay();
    BankControlDrag(BankLayoutController owner) {this.owner=owner;}
    BankControlLayout draft() {return draft;}

    private static final class Snapshot {
        final Widget bank;
        final Rectangle container;
        final Map<BankControlLayout.Control,Rectangle> buttons;
        final String initial,preference;
        final boolean custom;
        Snapshot(Widget bank,Rectangle container,Map<BankControlLayout.Control,Rectangle> buttons,String initial,String preference,boolean custom)
        {this.bank=bank;this.container=container;this.buttons=buttons;this.initial=initial;this.preference=preference;this.custom=custom;}
    }
    private static final class Gesture {
        final Snapshot start;final BankControlLayout.Control control;final Point pressed,point;final boolean released;
        Gesture(Snapshot start,BankControlLayout.Control control,Point pressed,Point point,boolean released)
        {this.start=start;this.control=control;this.pressed=pressed;this.point=point;this.released=released;}
        Gesture at(Point point,boolean released) {return new Gesture(start,control,pressed,point,released);}
    }
    @Override public synchronized MouseEvent mousePressed(MouseEvent event)
    {
        suppressClick=false;
        if(captured) {cancel=true;event.consume();return event;}
        Snapshot current=snapshot;
        if(current==null || !event.isAltDown() || event.isControlDown() || event.isMetaDown() || event.getButton()!=MouseEvent.BUTTON1)return event;
        for(Map.Entry<BankControlLayout.Control,Rectangle> entry:current.buttons.entrySet())if(entry.getValue().contains(event.getPoint())) {
            gesture=new Gesture(current,entry.getKey(),event.getPoint(),event.getPoint(),false);
            cancel=false;captured=true;event.consume();break;
        }
        return event;
    }
    @Override public synchronized MouseEvent mouseDragged(MouseEvent event)
    {
        if(captured) {event.consume();if(gesture!=null)gesture=gesture.at(event.getPoint(),false);}return event;
    }
    @Override public synchronized MouseEvent mouseReleased(MouseEvent event)
    {
        if(captured) {
            event.consume();
            if(event.getButton()==MouseEvent.BUTTON1) {captured=false;suppressClick=true;if(gesture!=null)gesture=gesture.at(event.getPoint(),true);}
        }
        return event;
    }
    @Override public synchronized MouseEvent mouseClicked(MouseEvent event)
    {if(suppressClick){event.consume();suppressClick=false;}return event;}
    @Override public synchronized MouseEvent mouseExited(MouseEvent event)
    {if(captured)cancel=true;return event;}
    void cancel() {cancel=true;snapshot=null;}
    synchronized void resetInput() {clear();captured=false;suppressClick=false;}
    synchronized void clear()
    {
        // Keep consuming the release of an already captured press, even if the bank closes.
        snapshot=null;gesture=null;draft=null;appliedDraft=null;cancel=false;
    }

    /** 0: unchanged; 1: apply preview; 2: restore native bases then apply. */
    synchronized int update()
    {
        Gesture current=gesture;
        if(current==null)return 0;
        Widget bank=owner.client.getWidget(InterfaceID.Bankmain.UNIVERSE);
        Widget container=owner.client.getWidget(InterfaceID.Bankmain.ITEMS_CONTAINER);
        Widget button=owner.client.getWidget(current.control.button);
        if(cancel || bank!=current.start.bank || bank==null || bank.isHidden() || container==null || button==null || button.isHidden()
            || !container.getBounds().equals(current.start.container) || blocked()
            || !owner.config.bankLayoutEnabled() || !owner.config.bankButtonDragging()) {
            boolean changed=draft!=null;clear();return changed?2:0;
        }
        boolean moved=current.pressed.distanceSq(current.point)>=16;
        if(!moved && draft==null) {if(current.released)clear();return 0;}
        boolean first=draft==null;
        BankControlLayout next=BankControlLayout.read(current.start.initial);
        BankControlLayout.Entry selected=next.entries.stream().filter(e -> e.control==current.control).findFirst().get();
        Rectangle bounds=current.start.container;int x=current.point.x-bounds.x,y=current.point.y-bounds.y;
        boolean dock=bounds.width>=BankControlLayout.MIN_DOCK_WIDTH && bounds.height>=BankControlLayout.TOP+BankControlLayout.PITCH+38
            && x>=0 && x<BankControlLayout.DOCK_X+BankControlLayout.DOCK_WIDTH+8 && y>=BankControlLayout.TOP && y<bounds.height-46;
        selected.left=dock;
        if(dock) {
            next.entries.remove(selected);
            List<BankControlLayout.Entry> left=new ArrayList<>();
            for(BankControlLayout.Entry entry:next.entries)if(entry.left && current.start.buttons.containsKey(entry.control))left.add(entry);
            int index=Math.max(0,Math.min(left.size(),(y-BankControlLayout.TOP)/BankControlLayout.PITCH));
            if(index==left.size()){selected.position=1000;next.entries.add(selected);}
            else {BankControlLayout.Entry before=left.get(index);selected.position=before.position;next.entries.add(next.entries.indexOf(before),selected);}
        } else {
            int grab=current.pressed.x-current.start.buttons.get(current.control).x;
            selected.position=Math.max(0,Math.min(1000,(x-grab-2)*1000/Math.max(1,bounds.width-current.control.width-4)));
            next.entries.remove(selected);int index=0;
            while(index<next.entries.size() && (next.entries.get(index).position<selected.position
                || next.entries.get(index).position==selected.position && current.point.x>current.pressed.x))index++;
            next.entries.add(index,selected);
        }
        next.sort();String value=next.write();boolean changed=!value.equals(appliedDraft);draft=next;appliedDraft=value;
        if(current.released) {
            if(!bounds.contains(current.point)) {clear();return 2;}
            // Only this plugin's preferences change. No native operation is invoked.
            owner.configs.setConfiguration("windowmarkers","bankControlLayout",value);
            owner.configs.setConfiguration("windowmarkers","customBankControls",true);
            clear();return 2;
        }
        return first && !current.start.custom?2:changed?1:0;
    }

    void capture()
    {
        Widget bank=owner.client.getWidget(InterfaceID.Bankmain.UNIVERSE),container=owner.client.getWidget(InterfaceID.Bankmain.ITEMS_CONTAINER);
        Widget items=owner.client.getWidget(InterfaceID.Bankmain.ITEMS),bottom=owner.client.getWidget(InterfaceID.Bankmain.BOTTOM);
        if(!owner.config.bankLayoutEnabled() || !owner.config.bankButtonDragging() || bank==null || bank.isHidden() || container==null
            || items==null || items.isHidden() || bottom==null || bottom.isHidden() || blocked() || owner.client.getDraggedWidget()!=null) {
            snapshot=null;return;
        }
        Rectangle area=container.getBounds();Map<BankControlLayout.Control,Rectangle> buttons=new EnumMap<>(BankControlLayout.Control.class);
        boolean custom=owner.config.customBankControls();String preference=custom?owner.config.bankControlLayout():"";
        for(BankControlLayout.Control control:BankControlLayout.Control.values()) {
            Widget widget=owner.client.getWidget(control.button);if(widget==null || widget.isHidden())continue;
            Rectangle r=widget.getBounds();if(!area.contains(r) || r.width<=0 || r.height<=0)continue;
            buttons.put(control,new Rectangle(r));
        }
        Snapshot previous=snapshot;
        if(previous!=null && previous.bank==bank && previous.container.equals(area) && previous.buttons.equals(buttons)
            && previous.custom==custom && Objects.equals(previous.preference,preference))return;
        BankControlLayout initial=BankControlLayout.read(preference);
        for(BankControlLayout.Entry entry:initial.entries) {
            Rectangle r=buttons.get(entry.control);
            if(r!=null && !custom) {
                entry.position=Math.max(0,Math.min(1000,(r.x-area.x-2)*1000/Math.max(1,area.width-r.width-4)));
                entry.left=r.y-area.y<area.height-40;
            }
        }
        initial.sort();snapshot=new Snapshot(bank,new Rectangle(area),buttons,initial.write(),preference,custom);
    }
    private boolean blocked()
    {
        if(owner.client.isMenuOpen())return true;
        for(int id:new int[]{InterfaceID.Bankmain.INCINERATOR_CONFIRM,InterfaceID.Bankmain.BANKTAGS_POPUP_STRINGEXPORT_CONTAINER}) {
            Widget modal=owner.client.getWidget(id);if(modal!=null && !modal.isHidden())return true;
        }
        return false;
    }
    private final class DragOverlay extends Overlay {
        DragOverlay(){setPosition(OverlayPosition.DYNAMIC);setLayer(OverlayLayer.MANUAL);drawAfterInterface(InterfaceID.BANKMAIN);}
        @Override public Dimension render(Graphics2D graphics)
        {
            Snapshot current=snapshot;Gesture moving=gesture;
            if(current==null || moving==null && (!owner.client.isKeyPressed(KeyCode.KC_ALT) || owner.client.isKeyPressed(KeyCode.KC_CONTROL)))return null;
            Rectangle area=current.container;Graphics2D g=(Graphics2D)graphics.create();
            try {
                g.setColor(new Color(100,195,220,110));g.drawRect(area.x+1,area.y+area.height-38,area.width-3,37);
                if(area.width>=BankControlLayout.MIN_DOCK_WIDTH && area.height>=BankControlLayout.TOP+BankControlLayout.PITCH+38)
                    g.drawRect(area.x+BankControlLayout.DOCK_X-1,area.y+BankControlLayout.TOP-1,BankControlLayout.DOCK_WIDTH+1,area.height-BankControlLayout.TOP-39);
                for(Map.Entry<BankControlLayout.Control,Rectangle> entry:current.buttons.entrySet()) {
                    g.setColor(moving!=null && moving.control==entry.getKey()?new Color(255,215,95):new Color(100,195,220,140));g.draw(entry.getValue());
                }
                String tip=moving==null?"Alt-drag bank buttons to arrange":"Release to save position · outside bank to cancel";
                int width=g.getFontMetrics().stringWidth(tip)+12,x=area.x+(area.width-width)/2,y=area.y+area.height-58;
                g.setColor(new Color(20,20,20,220));g.fillRect(x,y,width,18);g.setColor(new Color(255,215,95));g.drawString(tip,x+6,y+13);
            }finally{g.dispose();}
            return null;
        }
    }
}
