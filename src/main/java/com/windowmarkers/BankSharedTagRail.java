package com.windowmarkers;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.widgets.JavaScriptCallback;
import net.runelite.api.widgets.Widget;

final class BankSharedTagRail
{
    private final BankActionBar owner;
    private final Map<Widget,Object[]> originalOps=new IdentityHashMap<>(),appliedOps=new IdentityHashMap<>();
    private Widget parent;
    private int offset,count,capacity;
    Rectangle wheelArea;

    BankSharedTagRail(BankActionBar owner) {this.owner=owner;}

    void apply(Widget container,int tagTop)
    {
        if(parent!=container) {restore();parent=container;offset=0;}
        Widget[] children=container.getDynamicChildren();if(children==null)return;
        Widget up=null,down=null,scroll=null;List<Widget[]> tabs=new ArrayList<>();
        for(int i=0;i<children.length;i++) {
            Widget w=children[i];if(w==null)continue;
            List<String> actions=w.getActions()==null?java.util.Collections.emptyList():Arrays.asList(w.getActions());
            if(actions.contains("Scroll up"))up=w;
            else if(actions.contains("Scroll down"))down=w;
            else if(actions.contains("View tag tab")) {
                Widget icon=i+1<children.length?children[i+1]:null;
                if(icon==null || icon.getItemId()<0 || icon.getOriginalX()!=4)icon=null;
                tabs.add(new Widget[]{w,icon});
            } else if(w.getNoScrollThrough() && w.getOriginalX()<=3 && w.getWidth()<=43)scroll=w;
        }
        if(up==null || down==null) {wheelArea=null;return;}
        int divider=tagTop-5;
        int top=41,tabTop=top+up.getHeight()+1;
        capacity=Math.max(0,(divider-down.getHeight()-tabTop)/41);
        count=tabs.size();offset=Math.max(0,Math.min(offset,Math.max(0,count-Math.max(1,capacity))));
        Rectangle area=BankWidgetGeometry.bounds(container);
        wheelArea=new Rectangle(area.x,area.y+top,50,Math.max(0,divider-top));
        owner.move(up,area.x+1,area.y+top);owner.move(down,area.x+1,area.y+divider-down.getHeight());
        replace(up,-1);replace(down,1);
        if(scroll!=null)owner.visibility(scroll,true);
        for(int i=0;i<tabs.size();i++) {
            Widget[] pair=tabs.get(i);boolean shown=i>=offset && i<offset+capacity;
            owner.visibility(pair[0],!shown);if(pair[1]!=null)owner.visibility(pair[1],!shown);
            if(shown) {
                int y=area.y+tabTop+(i-offset)*41;
                owner.move(pair[0],area.x+1,y);if(pair[1]!=null)owner.move(pair[1],area.x+4,y+4);
            }
        }
    }

    void scroll(int delta) {offset=(int)Math.max(0,Math.min(Math.max(0,count-Math.max(1,capacity)),(long)offset+delta));}

    private void replace(Widget button,int delta)
    {
        Object[] callback={(JavaScriptCallback)e -> {if(owner.enabled())scroll(delta);}};
        if(!originalOps.containsKey(button) || !Arrays.equals(button.getOnOpListener(),appliedOps.get(button)))originalOps.put(button,button.getOnOpListener());
        appliedOps.put(button,callback);button.setOnOpListener(callback);
    }

    void beforeFrame()
    {
        originalOps.forEach((w,ops) -> {if(Arrays.equals(w.getOnOpListener(),appliedOps.get(w)))w.setOnOpListener(ops);});
        originalOps.clear();appliedOps.clear();wheelArea=null;
    }
    void restore() {beforeFrame();parent=null;offset=0;count=capacity=0;}
}
