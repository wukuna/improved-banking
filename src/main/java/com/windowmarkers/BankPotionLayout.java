package com.windowmarkers;

import java.util.ArrayList;
import java.util.List;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetType;

final class BankPotionLayout
{
    private Widget parent;
    private Widget[] nativeChildren;
    private final List<Row> rows = new ArrayList<>();
    private final List<Widget> dividers = new ArrayList<>();
    private final List<Extra> extras = new ArrayList<>();
    private int childCount;

    void clear() { parent=null;nativeChildren=null;rows.clear();dividers.clear();extras.clear();childCount=0; }

    void capture(Widget items)
    {
        clear();
        if(items==null || items.isHidden() || items.getWidth()<100)return;
        Widget[] children=items.getDynamicChildren();
        if(children==null)return;
        parent=items;nativeChildren=children.clone();childCount=children.length;
        int half=items.getWidth()/2,lastY=-1;
        for(int i=0;i+4<children.length;i+=5) {
            Widget a=children[i],icon=children[i+1],name=children[i+2],dose=children[i+3],heart=children[i+4];
            if(a==null || icon==null || name==null || dose==null || heart==null
                || a.getType()!=WidgetType.GRAPHIC || a.isSelfHidden() || icon.getItemId()<0
                || name.getType()!=WidgetType.TEXT || dose.getType()!=WidgetType.TEXT
                || heart.getType()!=WidgetType.GRAPHIC || a.getOriginalWidth()<36)continue;
            int column=a.getOriginalX()>0 || lastY==a.getOriginalY()?1:0;
            rows.add(new Row(i,new Widget[]{a,icon,name,dose,heart},column));
            lastY=a.getOriginalY();
        }
        for(Widget child:children)if(child!=null && child.getType()==WidgetType.RECTANGLE
            && child.getOriginalWidth()==1 && child.getOriginalX()==half)dividers.add(child);
    }

    void apply(Widget items)
    {
        if(items==null || items!=parent || items.isHidden() || items.getWidth()<100)return;
        Widget[] children=items.getDynamicChildren();
        if(children==null || children.length<nativeChildren.length)return;
        for(Row row:rows)for(int i=0;i<5;i++)if(children[row.index+i]!=row.parts[i])return;
        if(items.getScrollWidth()!=0 && items.getScrollWidth()!=items.getWidth()) {
            items.setScrollWidth(items.getWidth());
            for(Widget child:children)if(child!=null)child.revalidate();
        }
        if(children.length!=childCount) {
            extras.clear();childCount=children.length;
            for(int i=nativeChildren.length;i+2<children.length;i+=3) {
                Widget back=children[i],fill=children[i+1],text=children[i+2];
                if(back==null || fill==null || text==null || back.getType()!=WidgetType.RECTANGLE
                    || fill.getType()!=WidgetType.RECTANGLE || text.getType()!=WidgetType.TEXT)continue;
                for(Row row:rows) {
                    Widget dose=row.parts[3];
                    if(back.getOriginalX()==dose.getOriginalX() && back.getOriginalY()==dose.getOriginalY()
                        && fill.getOriginalX()==back.getOriginalX() && fill.getOriginalY()==back.getOriginalY()
                        && text.getOriginalX()==back.getOriginalX()+2 && text.getOriginalY()==back.getOriginalY()+1) {
                        extras.add(new Extra(back,dose,0,0,false));extras.add(new Extra(fill,dose,0,0,false));
                        extras.add(new Extra(text,dose,2,1,text.getOriginalWidth()==dose.getOriginalWidth()));
                        break;
                    }
                }
            }
        }
        int half=items.getWidth()/2;
        for(Row row:rows) {
            Widget background=row.parts[0];
            int currentX=background.getOriginalX();
            if(currentX!=row.lastX)row.column=currentX>0?1:0;
            int x=row.column==0?0:(items.getWidth()+1)/2,y=background.getOriginalY();
            for(int i=0;i<5;i++) {
                Widget part=row.parts[i];
                int px=x+(i==4?half-row.heartInset:row.dx[i]);
                int width=i==0 || i==2 || i==3?half:part.getOriginalWidth();
                place(part,px,y+row.dy[i],width);
            }
            row.lastX=x;
        }
        for(Extra extra:extras)place(extra.widget,extra.anchor.getOriginalX()+extra.x,extra.anchor.getOriginalY()+extra.y,
            extra.widthFollows?extra.anchor.getOriginalWidth():extra.widget.getOriginalWidth());
        for(Widget divider:dividers)place(divider,half,divider.getOriginalY(),divider.getOriginalWidth());
    }

    private static void place(Widget widget,int x,int y,int width)
    {
        if(widget.getOriginalX()==x && widget.getOriginalY()==y && widget.getOriginalWidth()==width)return;
        widget.setOriginalX(x);widget.setOriginalY(y);widget.setOriginalWidth(width);widget.revalidate();
    }

    private static final class Row
    {
        final int index,heartInset;
        final Widget[] parts;
        final int[] dx=new int[5],dy=new int[5];
        int column,lastX;
        Row(int index,Widget[] parts,int column) {
            this.index=index;this.parts=parts;this.column=column;lastX=parts[0].getOriginalX();
            for(int i=0;i<5;i++) {dx[i]=parts[i].getOriginalX()-lastX;dy[i]=parts[i].getOriginalY()-parts[0].getOriginalY();}
            heartInset=parts[0].getOriginalWidth()-dx[4];
        }
    }

    private static final class Extra
    {
        final Widget widget,anchor;
        final int x,y;
        final boolean widthFollows;
        Extra(Widget widget,Widget anchor,int x,int y,boolean widthFollows) {
            this.widget=widget;this.anchor=anchor;this.x=x;this.y=y;this.widthFollows=widthFollows;
        }
    }
}
