package com.windowmarkers;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.runelite.api.GameState;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetPositionMode;
import net.runelite.api.widgets.WidgetSizeMode;
import net.runelite.client.input.MouseAdapter;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

final class BankWindowDrag extends MouseAdapter
{
    private final BankLayoutController owner;
    private volatile Snapshot snapshot;
    private Gesture gesture;
    private Rectangle draft;
    private volatile Point anchorPoint;
    Rectangle anchorBounds() {
        Widget bank=owner.client.getWidget(InterfaceID.Bankmain.UNIVERSE);
        return bank==null?new Rectangle():new Rectangle(bank.getBounds());
    }
    boolean anchorAvailable() {
        Widget bank=owner.client.getWidget(InterfaceID.Bankmain.UNIVERSE);
        return owner.layoutEnabled() && owner.config.bankActionRail() && owner.client.isResized()
            && owner.client.getGameState()==GameState.LOGGED_IN && bank!=null && !bank.isHidden();
    }
    Rectangle anchorLimits() {Widget modal=owner.modal();return modal==null || modal.getParent()==null?null:new Rectangle(modal.getParent().getBounds());}
    void anchorMove(Point point) { anchorPoint=new Point(point); }
    void anchorRelease(boolean keep) {
        if(keep && anchorPoint!=null) {
            Widget modal=owner.modal();Widget area=modal==null?null:modal.getParent();
            if(area!=null) {
                Rectangle bounds=anchorBounds(),available=area.getBounds();bounds.translate(-available.x,-available.y);
                owner.configs.setConfiguration("windowmarkers","bankWindowPosition",new BankWindowPlacement(bounds,available.width,available.height).write());
            }
        }
        boolean restore=anchorPoint!=null && !keep;anchorPoint=null;
        if(restore)owner.releaseAnchorFrame();
    }
    private boolean captured,suppressClick,cancelled;
    final Overlay overlay=new GripOverlay();

    BankWindowDrag(BankLayoutController owner) {this.owner=owner;}

    private static final class Snapshot
    {
        final Widget bank,modal;
        final Rectangle area,bounds,title,grip;
        final List<Rectangle> excluded;
        final String profile;

        Snapshot(Widget bank,Widget modal,Rectangle area,Rectangle bounds,Rectangle title,List<Rectangle> excluded,String profile)
        {
            this.bank=bank;this.modal=modal;this.area=new Rectangle(area);this.bounds=new Rectangle(bounds);
            this.title=title;this.excluded=excluded;this.profile=profile;
            grip=new Rectangle(bounds.x+bounds.width-12,bounds.y+bounds.height-12,12,12);
        }
    }

    private static final class Gesture
    {
        final Snapshot start;
        final Point pressed,point;
        final boolean resize,released;

        Gesture(Snapshot start,Point pressed,Point point,boolean resize,boolean released)
        {this.start=start;this.pressed=pressed;this.point=point;this.resize=resize;this.released=released;}

        Gesture at(Point point,boolean released) {return new Gesture(start,pressed,point,resize,released);}
    }

    @Override public synchronized MouseEvent mousePressed(MouseEvent event)
    {
        suppressClick=false;
        if(event.isConsumed() || event.getButton()!=MouseEvent.BUTTON1 || event.isAltDown()
            || event.isControlDown() || event.isMetaDown() || event.isShiftDown())return event;
        Snapshot current=snapshot;
        if(current==null)return event;
        Point point=event.getPoint();
        for(Rectangle excluded:current.excluded)if(excluded.contains(point))return event;
        boolean resize=current.grip.contains(point);
        if(!resize && (anchorPoint!=null || !current.title.contains(point)))return event;
        gesture=new Gesture(current,point,point,resize,false);
        cancelled=false;captured=true;event.consume();return event;
    }

    @Override public synchronized MouseEvent mouseDragged(MouseEvent event)
    {
        if(captured) {event.consume();if(gesture!=null)gesture=gesture.at(event.getPoint(),false);}
        return event;
    }

    @Override public synchronized MouseEvent mouseReleased(MouseEvent event)
    {
        if(captured && event.getButton()==MouseEvent.BUTTON1) {
            event.consume();captured=false;suppressClick=true;
            if(gesture!=null)gesture=gesture.at(event.getPoint(),true);
        }
        return event;
    }

    @Override public synchronized MouseEvent mouseClicked(MouseEvent event)
    {
        if(suppressClick && event.getButton()==MouseEvent.BUTTON1) {event.consume();suppressClick=false;}
        return event;
    }

    @Override public synchronized MouseEvent mouseExited(MouseEvent event) {if(captured)cancel();return event;}

    synchronized void cancel() {cancelled=true;snapshot=null;}
    synchronized void resetInput() {clear();captured=false;suppressClick=false;}
    synchronized void clear() {snapshot=null;gesture=null;draft=null;cancelled=false;}

    synchronized int update()
    {
        Gesture current=gesture;
        if(current==null)return 0;
        Widget bank=owner.client.getWidget(InterfaceID.Bankmain.UNIVERSE),modal=owner.modal();
        Widget area=modal==null?null:modal.getParent();
        if(cancelled || !available(bank) || modal!=current.start.modal || bank!=current.start.bank || area==null
            || !current.start.area.equals(area.getBounds()) || !Objects.equals(current.start.profile,profile())) {
            boolean changed=draft!=null;clear();return changed?2:0;
        }
        int dx=current.point.x-current.pressed.x,dy=current.point.y-current.pressed.y;
        if(draft==null && (long)dx*dx+(long)dy*dy<16) {if(current.released)clear();return 0;}
        Rectangle bounds=current.start.bounds,available=current.start.area;
        Rectangle next=new Rectangle(bounds.x-available.x,bounds.y-available.y,bounds.width,bounds.height);
        if(current.resize) {
            next.width=BankWindowPlacement.limitSize(bounds.width+dx,BankWindowPlacement.MIN_WIDTH,available.width-next.x);
            next.height=BankWindowPlacement.limitSize(bounds.height+dy,BankWindowPlacement.MIN_HEIGHT,available.height-next.y);
        } else {
            next.x=Math.max(0,Math.min(available.width-next.width,next.x+dx));
            next.y=Math.max(0,Math.min(available.height-next.height,next.y+dy));
        }
        boolean changed=!next.equals(draft);draft=next;
        if(current.released) {
            String placement=new BankWindowPlacement(next,available.width,available.height).write();
            clear();
            if(current.resize) {
                owner.configs.setConfiguration("windowmarkers","width",next.width);
                owner.configs.setConfiguration("windowmarkers","height",next.height);
                owner.configs.setConfiguration("windowmarkers","customWidth",true);
                owner.configs.setConfiguration("windowmarkers","customHeight",true);
            }
            owner.configs.setConfiguration("windowmarkers","bankWindowPosition",placement);
            owner.queueRebuild();return 2;
        }
        return changed?1:0;
    }

    synchronized boolean apply(Widget bank,Widget modal,boolean settings)
    {
        if(!owner.config.bankActionRail() || !owner.client.isResized() || modal==null || modal.getParent()==null)return false;
        BankWindowPlacement saved=BankWindowPlacement.read(owner.config.bankWindowPosition());
        if(draft==null && saved==null && anchorPoint==null)return false;
        Rectangle area=modal.getParent().getBounds();
        if(area.width<BankWindowPlacement.MIN_WIDTH || area.height<BankWindowPlacement.MIN_HEIGHT)return false;
        int[] base=owner.baseSize(bank);
        if(base[2]!=WidgetSizeMode.ABSOLUTE || base[3]!=WidgetSizeMode.ABSOLUTE)return false;
        int width=owner.config.customWidth()?owner.config.width():saved==null?base[0]:saved.bounds.width;
        int height=owner.config.customHeight()?owner.config.height():saved==null?base[1]:saved.bounds.height;
        Rectangle target=draft!=null?new Rectangle(draft):saved!=null?saved.fit(area.width,area.height,width,height)
            :new Rectangle(0,0,BankWindowPlacement.limitSize(width,BankWindowPlacement.MIN_WIDTH,area.width),BankWindowPlacement.limitSize(height,BankWindowPlacement.MIN_HEIGHT,area.height));
        Point anchored=anchorPoint;
        if(anchored!=null) {target.x=Math.max(0,Math.min(area.width-target.width,anchored.x-area.x));target.y=Math.max(0,Math.min(area.height-target.height,anchored.y-area.y));}
        if(settings) {target.height=Math.min(area.height,Math.max(330,target.height));target.y=Math.min(target.y,area.height-target.height);}
        owner.setPos(modal,target.x,target.y,WidgetPositionMode.ABSOLUTE_LEFT,WidgetPositionMode.ABSOLUTE_TOP);
        owner.setSize(modal,target.width,Math.min(area.height-target.y,target.height+BankLayoutController.MODAL_HEIGHT_MARGIN),WidgetSizeMode.ABSOLUTE,WidgetSizeMode.ABSOLUTE);
        modal.revalidateScroll();
        Widget parent=owner.client.getWidget(InterfaceID.Bankmain.INFINITE);
        if(parent==null)return false;
        Rectangle parentBounds=parent.getBounds();
        owner.setSize(bank,target.width,target.height,WidgetSizeMode.ABSOLUTE,WidgetSizeMode.ABSOLUTE);
        owner.setPos(bank,area.x+target.x-parentBounds.x,area.y+target.y-parentBounds.y,
            WidgetPositionMode.ABSOLUTE_LEFT,WidgetPositionMode.ABSOLUTE_TOP);
        return true;
    }

    void capture()
    {
        Widget bank=owner.client.getWidget(InterfaceID.Bankmain.UNIVERSE),modal=owner.modal();
        Widget area=modal==null?null:modal.getParent();
        if(!available(bank) || area==null || area.getWidth()<BankWindowPlacement.MIN_WIDTH
            || area.getHeight()<BankWindowPlacement.MIN_HEIGHT) {snapshot=null;return;}
        Rectangle bounds=bank.getBounds(),space=area.getBounds();
        if(!space.contains(bounds)) {snapshot=null;return;}
        Widget title=owner.client.getWidget(InterfaceID.Bankmain.TITLE);
        Rectangle titleBounds=title==null || title.isHidden()?new Rectangle():bounds.intersection(title.getBounds());
        List<Rectangle> excluded=new ArrayList<>();
        collectButtons(owner.client.getWidget(InterfaceID.Bankmain.FRAME),excluded,0);
        for(BankControlLayout.Control control:BankControlLayout.Control.values()) {
            Widget button=owner.client.getWidget(control.button);
            if(button!=null && !button.isHidden())excluded.add(new Rectangle(button.getBounds()));
        }
        snapshot=new Snapshot(bank,modal,space,bounds,titleBounds,excluded,profile());
    }

    private boolean available(Widget bank)
    {
        if(!owner.layoutEnabled() || !owner.config.bankActionRail() || !owner.client.isResized()
            || owner.client.getGameState()!=GameState.LOGGED_IN || bank==null || bank.isHidden()
            || owner.client.isMenuOpen() || owner.client.getDraggedWidget()!=null)return false;
        Widget items=owner.client.getWidget(InterfaceID.Bankmain.ITEMS);
        if(items==null || items.isHidden())return false;
        for(int id:new int[]{InterfaceID.Bankmain.INCINERATOR_CONFIRM,InterfaceID.Bankmain.BANKTAGS_POPUP_STRINGEXPORT_CONTAINER}) {
            Widget popup=owner.client.getWidget(id);if(popup!=null && !popup.isHidden())return false;
        }
        return true;
    }

    private String profile() {return owner.configs==null || owner.configs.getProfile()==null?null:owner.configs.getProfile().getId()+"";}

    private static void collectButtons(Widget widget,List<Rectangle> excluded,int depth)
    {
        if(widget==null || widget.isHidden() || depth>4)return;
        if(widget.getActions()!=null || widget.getOnOpListener()!=null)excluded.add(new Rectangle(widget.getBounds()));
        Widget[][] children={widget.getStaticChildren(),widget.getDynamicChildren()};
        for(Widget[] group:children)if(group!=null)for(Widget child:group)collectButtons(child,excluded,depth+1);
    }

    private final class GripOverlay extends Overlay
    {
        GripOverlay() {setPosition(OverlayPosition.DYNAMIC);setLayer(OverlayLayer.MANUAL);drawAfterInterface(InterfaceID.BANKMAIN);}

        @Override public Dimension render(Graphics2D graphics)
        {
            Snapshot current=snapshot;
            if(current==null)return null;
            Graphics2D g=(Graphics2D)graphics.create();
            try {
                g.setColor(new Color(210,190,130));
                Rectangle r=current.grip;
                for(int n=3;n<=9;n+=3)g.drawLine(r.x+11-n,r.y+11,r.x+11,r.y+11-n);
            } finally {g.dispose();}
            return null;
        }
    }
}
