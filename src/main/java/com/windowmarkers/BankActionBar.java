package com.windowmarkers;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.runelite.api.KeyCode;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetPositionMode;
import net.runelite.api.widgets.WidgetSizeMode;
import net.runelite.client.input.MouseAdapter;
import net.runelite.client.input.MouseWheelListener;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

final class BankActionBar extends MouseAdapter implements MouseWheelListener
{
    private static final String SPLITTER="split";
    private static final Color RAIL_BORDER=new Color(110,110,100,95);
    private final BankLayoutController owner;
    private final Map<Widget,Geometry> geometry=new IdentityHashMap<>();
    private final Map<Widget,Boolean> hidden=new IdentityHashMap<>();
    private final Map<Widget,Boolean> hiddenApplied=new IdentityHashMap<>();
    private final BankSharedTagRail sharedTags=new BankSharedTagRail(this);
    private volatile Snapshot snapshot;
    private volatile Snapshot displaySnapshot;
    private Gesture gesture;
    private BankActionLayout draft;
    private String lastDraft;
    private boolean captured,suppressClick,cancelled;
    private volatile long context;
    BankActionLayout.Plan plan;
    private String planned;
    final Overlay overlay=new ActionOverlay();

    BankActionBar(BankLayoutController owner) {this.owner=owner;}

    private static final class Geometry {
        final int[] original;int[] applied;
        Geometry(Widget w) {original=read(w);}
        static int[] read(Widget w) {return new int[]{w.getOriginalX(),w.getOriginalY(),w.getOriginalWidth(),w.getOriginalHeight(),w.getXPositionMode(),w.getYPositionMode(),w.getWidthMode(),w.getHeightMode()};}
    }
    static final class Button {
        final Widget button;final List<Widget> parts;
        Button(Widget button,Widget... parts) {this.button=button;this.parts=new ArrayList<>(Arrays.asList(parts));}
        Button(Widget button,List<Widget> parts) {this.button=button;this.parts=new ArrayList<>(parts);}
    }
    private static final class Snapshot {
        final Widget bank,items;final Rectangle area,bankBounds,itemBounds,tagBounds;final Map<String,Rectangle> buttons,placements;
        final String layout,profile;final boolean tags,adjustableDivider;final int bottomHeight,tagTop,tagMaxOffset;final long context;
        Snapshot(Widget bank,Widget items,Rectangle area,Map<String,Rectangle> buttons,BankActionLayout.Plan plan,String layout,String profile,boolean tags,boolean adjustableDivider,long context,Rectangle tagBounds) {
            this.bank=bank;this.items=items;this.area=new Rectangle(area);this.buttons=buttons;this.layout=layout;this.profile=profile;
            this.tags=tags;this.bottomHeight=plan.bottomHeight;tagTop=plan.tagTop;tagMaxOffset=plan.tagMaxOffset;this.context=context;bankBounds=new Rectangle(bank.getBounds());itemBounds=new Rectangle(items.getBounds());
            this.tagBounds=tagBounds==null?null:new Rectangle(tagBounds);
            this.adjustableDivider=adjustableDivider;
            this.placements=new LinkedHashMap<>();plan.bounds.forEach((key,r) -> this.placements.put(key,new Rectangle(r)));
        }
        Rectangle splitter() {return new Rectangle(area.x+1,area.y+tagTop-5,48,5);}
    }
    private static final class Gesture {
        final Snapshot start;final String key;final Point pressed,point;final boolean released;
        Gesture(Snapshot start,String key,Point pressed,Point point,boolean released) {this.start=start;this.key=key;this.pressed=pressed;this.point=point;this.released=released;}
        Gesture at(Point p,boolean released) {return new Gesture(start,key,pressed,p,released);}
    }

    boolean enabled() {
        if(!owner.active() || !owner.config.bankActionRail())return false;
        Widget container=owner.client.getWidget(InterfaceID.Bankmain.ITEMS_CONTAINER);
        return owner.bankTagsService==null || owner.bankTagsService.getActiveLayout()==null
            || container!=null && container.getWidth()>=560;
    }
    boolean tagSpace() {
        Widget container=owner.client.getWidget(InterfaceID.Bankmain.ITEMS_CONTAINER);
        return owner.config.bankActionTagSpace() && container!=null && container.getHeight()>=250;
    }

    Map<String,Button> buttons()
    {
        Map<String,Button> result=new LinkedHashMap<>();
        for(BankControlLayout.Control control:BankControlLayout.Control.values())
            add(result,control.name(),owner.client.getWidget(control.button),owner.client.getWidget(control.graphic));
        int[] ids={InterfaceID.Bankmain.WORNITEMS_BUTTON,InterfaceID.Bankmain.MENU_BUTTON,InterfaceID.Bankmain.BANK_TUT,
            InterfaceID.Bankmain.INCINERATOR_TARGET,InterfaceID.Bankmain.POPUP_BUTTON_OUT,InterfaceID.Bankmain.GIM_STORAGE,InterfaceID.Bankmain.POTIONSTORE_BUTTON};
        String[] names={"EQUIPMENT","SETTINGS","HELP","INCINERATOR","POPUP","GROUP_STORAGE","POTION_STORE"};
        for(int i=0;i<ids.length;i++)add(result,names[i],owner.client.getWidget(ids[i]));
        Widget frame=owner.client.getWidget(InterfaceID.Bankmain.FRAME);
        if(frame!=null && frame.getChildren()!=null)for(Widget child:frame.getChildren())
            if(child!=null && child.getActions()!=null && Arrays.asList(child.getActions()).contains("Close"))add(result,"CLOSE",child);
        if(owner.screenshotButton!=null)add(result,"CAMERA",owner.screenshotButton.widget());
        if(owner.bankPicker!=null && visible(owner.bankPicker.widget()))result.put("PRESETS",new Button(owner.bankPicker.widget(),owner.bankPicker.parts()));
        return result;
    }
    private static void add(Map<String,Button> buttons,String key,Widget widget,Widget... parts) {if(visible(widget))buttons.put(key,new Button(widget,parts));}
    private static boolean visible(Widget w) {return w!=null && !w.isHidden() && w.getWidth()>0 && w.getHeight()>0;}

    private static Dimension size(String key,Widget button) {
        return new Dimension(key.equals("SEARCH")?36:button.getWidth(),button.getHeight());
    }

    BankActionLayout.Plan plan(Widget container)
    {
        Map<String,Dimension> sizes=new LinkedHashMap<>();
        buttons().forEach((key,b) -> sizes.put(key,size(key,b.button)));
        BankActionLayout selected=draft==null?BankActionLayout.read(owner.config.bankActionLayout()):draft;
        plan=selected.plan(container.getWidth(),container.getHeight(),sizes,tagSpace());planned=signature(container,sizes);return plan;
    }
    boolean changed() {
        Widget container=owner.client.getWidget(InterfaceID.Bankmain.ITEMS_CONTAINER);if(container==null)return true;
        Map<String,Dimension> sizes=new LinkedHashMap<>();buttons().forEach((key,b) -> sizes.put(key,size(key,b.button)));
        return !Objects.equals(planned,signature(container,sizes));
    }
    private String signature(Widget container,Map<String,Dimension> sizes) {
        return container.getWidth()+":"+container.getHeight()+":"+tagSpace()+":"+sizes+":"+(draft==null?owner.config.bankActionLayout():draft.write());
    }

    void apply(Widget container)
    {
        if(plan==null)return;
        restoreHidden();
        restoreGeometry();
        hide(owner.client.getWidget(InterfaceID.Bankmain.SCROLLBAR));
        Map<String,Button> buttons=buttons();
        for(int id:new int[]{InterfaceID.Bankmain.BOTTOM,InterfaceID.Bankmain.QUANTITY_LAYER}) {
            Widget w=owner.client.getWidget(id);if(w==null)continue;
            edit(w,0,0,0,0,WidgetPositionMode.ABSOLUTE_LEFT,WidgetPositionMode.ABSOLUTE_TOP,WidgetSizeMode.MINUS,WidgetSizeMode.MINUS);
        }
        for(int id:new int[]{InterfaceID.Bankmain.BOTTOM_LINE4,InterfaceID.Bankmain.BOTTOM_LINE6,InterfaceID.Bankmain.DEPOSIT_LINE})hide(owner.client.getWidget(id));
        if(plan.bounds.containsKey("GROUP_STORAGE") || plan.bounds.containsKey("POTION_STORE")) {
            Widget storage=owner.client.getWidget(InterfaceID.Bankmain.STORAGE_POPUP_TAB);
            if(storage!=null)edit(storage,0,0,0,0,WidgetPositionMode.ABSOLUTE_LEFT,WidgetPositionMode.ABSOLUTE_TOP,WidgetSizeMode.MINUS,WidgetSizeMode.MINUS);
            hide(owner.client.getWidget(InterfaceID.Bankmain.STORAGE_POPUP_TAB_FRAME));
        }
        Rectangle area=container.getBounds();
        Button equipment=buttons.get("EQUIPMENT");
        if(equipment!=null && !plan.bounds.containsKey("EQUIPMENT")) {
            Rectangle bounds=equipment.button.getBounds();
            move(equipment.button,area.x+area.width-BankActionLayout.RAIL/2-bounds.width/2,
                area.y+Math.max(0,(BankActionLayout.TOP-bounds.height)/2));
        }
        for(Map.Entry<String,Rectangle> entry:plan.bounds.entrySet()) {
            Button button=buttons.get(entry.getKey());if(button==null)continue;
            Rectangle target=new Rectangle(entry.getValue());target.translate(area.x,area.y);
            Rectangle original=button.button.getBounds();
            List<Rectangle> partBounds=new ArrayList<>();
            for(Widget part:button.parts)partBounds.add(part==null?null:new Rectangle(part.getBounds()));
            if(entry.getKey().equals("SEARCH"))edit(button.button,button.button.getOriginalX(),button.button.getOriginalY(),36,button.button.getOriginalHeight(),
                button.button.getXPositionMode(),button.button.getYPositionMode(),WidgetSizeMode.ABSOLUTE,button.button.getHeightMode());
            move(button.button,target.x,target.y);
            for(int i=0;i<button.parts.size();i++) {
                Rectangle r=partBounds.get(i);Widget part=button.parts.get(i);
                if(r==null)continue;
                BankControlLayout.Control standard=null;
                try {standard=BankControlLayout.Control.valueOf(entry.getKey());}catch(IllegalArgumentException ignored) { }
                if(standard!=null) {
                    if(standard.quantity())edit(part,part.getOriginalX(),part.getOriginalY(),25,21,part.getXPositionMode(),part.getYPositionMode(),WidgetSizeMode.ABSOLUTE,WidgetSizeMode.ABSOLUTE);
                    move(part,target.x+(standard==BankControlLayout.Control.SEARCH?4:standard.iconX),target.y+standard.iconY);
                } else move(part,target.x+r.x-original.x,target.y+r.y-original.y);
            }
        }
        if(tagSpace())sharedTags.apply(container,plan.tagTop);
    }

    void move(Widget widget,int x,int y)
    {
        Widget parent=widget.getParent();if(parent==null)return;
        Rectangle bounds=parent.getBounds();
        edit(widget,x-bounds.x+parent.getScrollX(),y-bounds.y+parent.getScrollY(),widget.getOriginalWidth(),widget.getOriginalHeight(),
            WidgetPositionMode.ABSOLUTE_LEFT,WidgetPositionMode.ABSOLUTE_TOP,widget.getWidthMode(),widget.getHeightMode());
    }
    private void edit(Widget w,int x,int y,int width,int height,int xm,int ym,int wm,int hm)
    {
        Geometry g=geometry.computeIfAbsent(w,Geometry::new);int[] current=Geometry.read(w);
        for(int i=0;i<current.length;i++)if(g.applied==null || current[i]!=g.applied[i])g.original[i]=current[i];
        w.setPos(x,y,xm,ym);w.setSize(width,height,wm,hm);w.revalidate();g.applied=Geometry.read(w);
    }
    private void hide(Widget w) {visibility(w,true);}
    void visibility(Widget w,boolean value) {
        if(w==null || w.isSelfHidden()==value)return;
        hidden.putIfAbsent(w,w.isSelfHidden());hiddenApplied.put(w,value);w.setHidden(value);
    }
    void restoreHidden() {hidden.forEach((w,value) -> {if(w.isSelfHidden()==hiddenApplied.get(w))w.setHidden(value);});hidden.clear();hiddenApplied.clear();}
    void beforeFrame() {sharedTags.beforeFrame();restoreHidden();restoreGeometry();}
    void restore()
    {
        restoreHidden();
        sharedTags.restore();
        restoreGeometry();plan=null;planned=null;snapshot=null;displaySnapshot=null;
    }
    private void restoreGeometry()
    {
        geometry.forEach((w,g) -> {
            int[] current=Geometry.read(w),r=g.original.clone();
            for(int i=0;i<r.length;i++)if(g.applied==null || current[i]!=g.applied[i])r[i]=current[i];
            w.setPos(r[0],r[1],r[4],r[5]);w.setSize(r[2],r[3],r[6],r[7]);w.revalidate();
        });
        geometry.clear();
    }

    @Override public synchronized MouseEvent mousePressed(MouseEvent event)
    {
        suppressClick=false;Snapshot s=snapshot;
        if(event.isConsumed() || s==null || event.isControlDown() || event.isMetaDown() || event.getButton()!=MouseEvent.BUTTON1)return event;
        if(s.tags && s.adjustableDivider && s.splitter().contains(event.getPoint())) {
            gesture=new Gesture(s,SPLITTER,event.getPoint(),event.getPoint(),false);captured=true;cancelled=false;event.consume();return event;
        }
        if(!event.isAltDown())return event;
        for(Map.Entry<String,Rectangle> entry:s.buttons.entrySet())if(entry.getValue().contains(event.getPoint())) {
            gesture=new Gesture(s,entry.getKey(),event.getPoint(),event.getPoint(),false);captured=true;cancelled=false;event.consume();break;
        }
        return event;
    }
    @Override public synchronized MouseEvent mouseDragged(MouseEvent event) {if(captured) {event.consume();if(gesture!=null)gesture=gesture.at(event.getPoint(),false);}return event;}
    @Override public synchronized MouseEvent mouseReleased(MouseEvent event) {
        if(captured && event.getButton()==MouseEvent.BUTTON1) {event.consume();captured=false;suppressClick=true;if(gesture!=null)gesture=gesture.at(event.getPoint(),true);}return event;
    }
    @Override public synchronized MouseEvent mouseClicked(MouseEvent event) {if(suppressClick && event.getButton()==MouseEvent.BUTTON1){event.consume();suppressClick=false;}return event;}
    @Override public synchronized MouseEvent mouseExited(MouseEvent event) {if(captured)cancel();return event;}
    synchronized void cancel() {cancelled=true;snapshot=null;context++;}
    synchronized void clearInput() {snapshot=null;displaySnapshot=null;gesture=null;draft=null;lastDraft=null;cancelled=false;context++;}
    synchronized void resetInput() {clearInput();captured=false;suppressClick=false;}

    synchronized int update()
    {
        Gesture g=gesture;if(g==null)return 0;
        Widget bank=owner.client.getWidget(InterfaceID.Bankmain.UNIVERSE),container=owner.client.getWidget(InterfaceID.Bankmain.ITEMS_CONTAINER);
        if(cancelled || !available() || bank!=g.start.bank || container==null || !container.getBounds().equals(g.start.area)
            || !Objects.equals(profile(),g.start.profile) || context!=g.start.context
            || (g.key.equals(SPLITTER)?!tagSpace() || !owner.config.bankTagDividerResizable():!buttons().containsKey(g.key))) {
            boolean changed=draft!=null;clearInput();return changed?2:0;
        }
        if(draft==null && g.pressed.distanceSq(g.point)<16) {if(g.released)clearInput();return 0;}
        Rectangle area=g.start.area;int x=g.point.x-area.x,y=g.point.y-area.y;
        BankActionLayout next=BankActionLayout.read(g.start.layout);
        if(g.key.equals(SPLITTER)) {
            int original=g.start.tagTop-BankActionLayout.tagTop(area.height,g.start.bottomHeight);
            next.tagOffset=Math.max(0,Math.min(g.start.tagMaxOffset,original+g.point.y-g.pressed.y));
            return preview(next,g.released);
        }
        char dock=y<0 && g.start.bankBounds.contains(g.point)?'N':y>=area.height-g.start.bottomHeight-6?'B':x>=area.width-54?'R':x<52 && g.start.tags && y>=g.start.tagTop?'T':x>=52 && x<100?'L':0;
        if(!g.start.bankBounds.contains(g.point) || dock==0) {if(g.released){clearInput();return 2;}return 0;}
        next.pinVisible(area.width,area.height,g.start.bottomHeight,g.start.placements,g.start.tags,g.start.tagTop);
        Rectangle target=new Rectangle(g.start.buttons.get(g.key));
        target.translate(g.point.x-g.pressed.x-area.x,g.point.y-g.pressed.y-area.y);
        if(dock!='N') {
            Rectangle rail=BankActionLayout.rail(dock,area.width,area.height,g.start.bottomHeight,g.start.tagTop);
            boolean valid=target.width<=rail.width && target.height<=rail.height && (dock!='L' || area.width>=536);
            target.x=Math.max(rail.x,Math.min(rail.x+rail.width-target.width,target.x));
            target.y=Math.max(rail.y,Math.min(rail.y+rail.height-target.height,target.y));
            for(Map.Entry<String,Rectangle> entry:g.start.placements.entrySet())
                if(!entry.getKey().equals(g.key) && target.intersects(entry.getValue()))valid=false;
            if(!valid) {if(g.released){clearInput();return 2;}return 0;}
        }
        next.pin(g.key,dock,target,area.width,area.height,g.start.bottomHeight,g.start.tagTop);
        return preview(next,g.released);
    }

    private int preview(BankActionLayout next,boolean released) {
        String encoded=next.write();boolean changed=!encoded.equals(lastDraft);draft=next;lastDraft=encoded;
        if(released) {clearInput();owner.configs.setConfiguration("windowmarkers","bankActionLayout",encoded);owner.queueRebuild();return 2;}
        return changed?1:0;
    }

    @Override public MouseWheelEvent mouseWheelMoved(MouseWheelEvent event)
    {
        Snapshot s=snapshot;
        if(event.isConsumed() || s==null)return event;
        boolean tags=s.tagBounds!=null && s.tagBounds.contains(event.getPoint());
        if(!tags && !s.itemBounds.contains(event.getPoint()))return event;
        int delta=(int)Math.round(event.getPreciseWheelRotation()*36);
        if(delta==0)return event;
        event.consume();
        owner.clientThread.invokeLater(() -> {
            if(s.context!=context || !available() || !Objects.equals(s.profile,profile())
                || owner.client.getWidget(InterfaceID.Bankmain.ITEMS)!=s.items || owner.client.getWidget(InterfaceID.Bankmain.UNIVERSE)!=s.bank)return;
            if(tags) {sharedTags.scroll(event.getWheelRotation());return;}
            int max=Math.max(0,s.items.getScrollHeight()-s.items.getHeight());
            s.items.setScrollY((int)Math.max(0,Math.min(max,(long)s.items.getScrollY()+delta)));
        });
        return event;
    }

    void capture()
    {
        if(!available(false) || plan==null) {snapshot=null;displaySnapshot=null;return;}
        Widget bank=owner.client.getWidget(InterfaceID.Bankmain.UNIVERSE),container=owner.client.getWidget(InterfaceID.Bankmain.ITEMS_CONTAINER),items=owner.client.getWidget(InterfaceID.Bankmain.ITEMS);
        Map<String,Rectangle> bounds=new LinkedHashMap<>();buttons().forEach((key,b) -> bounds.put(key,new Rectangle(b.button.getBounds())));
        displaySnapshot=new Snapshot(bank,items,container.getBounds(),bounds,plan,draft==null?owner.config.bankActionLayout():draft.write(),profile(),tagSpace(),owner.config.bankTagDividerResizable(),context,sharedTags.wheelArea);
        snapshot=available()?displaySnapshot:null;
    }
    private String profile() {return owner.configs==null || owner.configs.getProfile()==null?null:owner.configs.getProfile().getId()+"";}
    private boolean available() {return available(true);}
    private boolean available(boolean input)
    {
        if(!enabled() || owner.client.getGameState()!=net.runelite.api.GameState.LOGGED_IN
            || input && (owner.client.isMenuOpen() || owner.client.getDraggedWidget()!=null))return false;
        for(int id:new int[]{InterfaceID.Bankmain.UNIVERSE,InterfaceID.Bankmain.ITEMS_CONTAINER,InterfaceID.Bankmain.ITEMS})if(!visible(owner.client.getWidget(id)))return false;
        for(int id:new int[]{InterfaceID.Bankmain.INCINERATOR_CONFIRM,InterfaceID.Bankmain.BANKTAGS_POPUP_STRINGEXPORT_CONTAINER})if(visible(owner.client.getWidget(id)))return false;
        return true;
    }

    private final class ActionOverlay extends Overlay {
        ActionOverlay() {setPosition(OverlayPosition.DYNAMIC);setLayer(OverlayLayer.MANUAL);drawAfterInterface(InterfaceID.BANKMAIN);}
        @Override public Dimension render(Graphics2D graphics) {
            Snapshot s=displaySnapshot;if(s==null)return null;
            Graphics2D g=(Graphics2D)graphics.create();
            try {
                Rectangle r=s.area;g.setColor(RAIL_BORDER);g.drawLine(r.x+r.width-51,r.y+BankActionLayout.TOP-2,r.x+r.width-51,r.y+r.height-s.bottomHeight-3);
                if(s.tags) {
                    if(s.adjustableDivider) {
                        Rectangle divider=s.splitter();net.runelite.api.Point mouse=owner.client.getMouseCanvasPosition();
                        boolean hover=mouse!=null && divider.contains(mouse.getX(),mouse.getY());
                        g.setColor(hover?new Color(105,190,220):RAIL_BORDER);
                        g.drawLine(divider.x,divider.y,divider.x+divider.width-1,divider.y);
                        for(int i=0;i<3;i++)g.fillRect(divider.x+18+i*5,divider.y+2,2,2);
                    }
                    g.setColor(RAIL_BORDER);g.drawLine(r.x+50,r.y+41,r.x+50,r.y+r.height-s.bottomHeight-3);
                }
                if(owner.client.isKeyPressed(KeyCode.KC_ALT) && !owner.client.isKeyPressed(KeyCode.KC_CONTROL)) {
                    g.setColor(new Color(105,190,220,150));
                    g.drawRect(r.x+1,r.y+r.height-s.bottomHeight-2,r.width-3,s.bottomHeight+1);
                    g.drawRect(r.x+r.width-51,r.y+BankActionLayout.TOP-2,50,Math.max(1,r.height-BankActionLayout.TOP-s.bottomHeight));
                    int leftTop=s.tags?s.tagTop:BankActionLayout.TOP;
                    g.drawRect(r.x+(s.tags?1:41),r.y+leftTop-2,50,Math.max(1,r.height-leftTop-s.bottomHeight));
                    for(Rectangle button:s.buttons.values())g.draw(button);
                }
            } finally {g.dispose();}
            return null;
        }
    }
}
