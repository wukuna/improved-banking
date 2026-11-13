package com.windowmarkers;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class BankActionLayout
{
    static final int TOP=54,RAIL=50,GAP=2;
    static final class Entry {
        char dock;int order;
        int x=-1,y=-1;
        Entry(char dock,int order) {this.dock=dock;this.order=order;}
        boolean pinned() {return x>=0 && y>=0;}
    }
    static final class Plan {
        final Map<String,Rectangle> bounds=new LinkedHashMap<>();
        int leftInset,bottomHeight=36,tagTop,tagMaxOffset;
    }
    final Map<String,Entry> entries=new LinkedHashMap<>();
    int tagOffset;

    static BankActionLayout read(String value)
    {
        BankActionLayout layout=new BankActionLayout();
        int i=0;
        for(BankControlLayout.Control control:BankControlLayout.Control.values())
            layout.entries.put(control.name(),new Entry(control.quantity()?'B':'R',i++*50));
        if(value==null || !(value.startsWith("v1;") || value.startsWith("v2;")) || value.length()>8192)return layout;
        for(String part:value.substring(3).split(";")) {
            String[] fields=part.split(":");
            if(fields.length==2 && fields[0].equals("split")) {
                try {layout.tagOffset=Math.max(0,Math.min(4000,Integer.parseInt(fields[1])));}catch(NumberFormatException ignored) { }
                continue;
            }
            if((fields.length!=3 && fields.length!=5) || !fields[0].matches("[A-Z_]{1,32}") || fields[1].length()!=1
                || "BLRTN".indexOf(fields[1].charAt(0))<0)continue;
            try {
                Entry entry=new Entry(fields[1].charAt(0),Math.max(0,Math.min(1000,Integer.parseInt(fields[2]))));
                if(fields.length==5) {
                    entry.x=Integer.parseInt(fields[3]);entry.y=Integer.parseInt(fields[4]);
                    if(entry.x<0 || entry.y<0 || entry.x>4000 || entry.y>4000)continue;
                }
                layout.entries.put(fields[0],entry);
            }
            catch(NumberFormatException ignored) { }
        }
        return layout;
    }

    String write()
    {
        StringBuilder out=new StringBuilder("v2;");
        if(tagOffset>0)out.append("split:").append(tagOffset).append(';');
        entries.forEach((key,e) -> {
            out.append(key).append(':').append(e.dock).append(':').append(e.order);
            if(e.pinned())out.append(':').append(e.x).append(':').append(e.y);
            out.append(';');
        });
        return out.toString();
    }

    void move(String key,char dock,int order) {entries.put(key,new Entry(dock,Math.max(0,Math.min(1000,order))));}
    void pin(String key,char dock,Rectangle bounds,int width,int height,int bottomHeight) {
        pin(key,dock,bounds,width,height,bottomHeight,tagTop(height,bottomHeight));
    }
    void pin(String key,char dock,Rectangle bounds,int width,int height,int bottomHeight,int tagTop) {
        Entry previous=entries.get(key),entry=new Entry(dock,previous==null?1000:previous.order);
        if(dock!='N') {
            Rectangle rail=rail(dock,width,height,bottomHeight,tagTop);
            entry.x=bounds.x-rail.x;
            entry.y=dock=='B'?height-1-bounds.y-bounds.height:bounds.y-rail.y;
        }
        entries.put(key,entry);
    }
    static int tagTop(int height,int bottomHeight) {return TOP+Math.max(0,height-bottomHeight-TOP)/2+4;}

    static Rectangle rail(char dock,int width,int height,int bottomHeight) {
        return rail(dock,width,height,bottomHeight,tagTop(height,bottomHeight));
    }
    static Rectangle rail(char dock,int width,int height,int bottomHeight,int tagTop) {
        if(dock=='B')return new Rectangle(2,height-bottomHeight-1,width-4,bottomHeight);
        int top=dock=='T'?tagTop:TOP;
        return new Rectangle(dock=='L'?42:dock=='T'?2:width-RAIL+2,top,RAIL-4,Math.max(0,height-bottomHeight-4-top));
    }

    void pinVisible(int width,int height,int bottomHeight,Map<String,Rectangle> bounds,boolean tagSpace) {
        pinVisible(width,height,bottomHeight,bounds,tagSpace,tagTop(height,bottomHeight));
    }
    void pinVisible(int width,int height,int bottomHeight,Map<String,Rectangle> bounds,boolean tagSpace,int tagTop) {
        bounds.forEach((key,r) -> {
            Entry entry=entries.get(key);
            if(entry==null || entry.dock=='N' || entry.pinned())return;
            char dock=entry.dock;
            if(dock=='T' && !tagSpace || !rail(dock,width,height,bottomHeight,tagTop).contains(r))dock='B';
            pin(key,dock,r,width,height,bottomHeight,tagTop);
        });
    }

    Plan plan(int width,int height,Map<String,Dimension> sizes,boolean tagSpace)
    {
        Plan plan=basePlan(width,height,sizes,tagSpace);
        plan.tagTop=tagTop(height,plan.bottomHeight);
        if(!tagSpace)return plan;
        Rectangle rail=rail('T',width,height,plan.bottomHeight);
        List<String> tags=new ArrayList<>();
        plan.bounds.forEach((key,r) -> {if(entries.get(key).dock=='T' && rail.contains(r))tags.add(key);});
        int limit=Math.max(0,rail.height);
        for(String key:tags) {
            Rectangle r=plan.bounds.get(key);
            limit=Math.min(limit,rail.y+rail.height-r.y-r.height);
            for(Map.Entry<String,Rectangle> other:plan.bounds.entrySet()) {
                if(tags.contains(other.getKey()))continue;
                Rectangle b=other.getValue();
                if(r.x<b.x+b.width && b.x<r.x+r.width && b.y>=r.y+r.height)
                    limit=Math.min(limit,Math.max(0,b.y-r.y-r.height-GAP));
            }
        }
        plan.tagMaxOffset=Math.max(0,limit);
        int offset=Math.min(Math.max(0,tagOffset),plan.tagMaxOffset);
        plan.tagTop+=offset;
        for(String key:tags)plan.bounds.get(key).translate(0,offset);
        return plan;
    }

    private Plan basePlan(int width,int height,Map<String,Dimension> sizes,boolean tagSpace)
    {
        List<String> keys=new ArrayList<>(sizes.keySet());
        keys.removeIf(key -> !entries.containsKey(key) || entries.get(key).dock=='N');
        keys.sort(Comparator.<String>comparingInt(key -> entries.get(key).pinned()?0:1).thenComparingInt(key -> entries.get(key).order));
        int bottomHeight=36;
        for(String key:keys) {
            Entry e=entries.get(key);
            if(e.dock=='B' && e.pinned())bottomHeight=Math.max(bottomHeight,Math.min(height-TOP-4,e.y+sizes.get(key).height));
        }
        Plan plan=new Plan();
        for(int pass=0;pass<=keys.size()+1;pass++) {
            plan=new Plan();List<String> bottom=new ArrayList<>();
            for(String key:keys) {
                Entry e=entries.get(key);Dimension d=sizes.get(key);
                char dock=e.dock=='T' && !tagSpace?'B':e.dock;
                Rectangle r=null;
                if(dock!='B' && d.width<=RAIL-4 && width>=(dock=='L'?536:478)) {
                    Rectangle rail=rail(dock,width,height,bottomHeight);
                    r=e.pinned()?new Rectangle(rail.x+e.x,rail.y+e.y,d.width,d.height):
                        free(rail,d,plan.bounds,false);
                    if(r!=null && (!rail.contains(r) || overlaps(r,plan.bounds)))r=null;
                }
                if(r==null)bottom.add(key);
                else {plan.bounds.put(key,r);if(dock=='L')plan.leftInset=RAIL;}
            }
            boolean fits=true;
            for(String key:bottom) {
                Entry e=entries.get(key);Dimension d=sizes.get(key);Rectangle rail=rail('B',width,height,bottomHeight);
                Rectangle r=e.dock=='B' && e.pinned()?new Rectangle(rail.x+e.x,height-1-e.y-d.height,d.width,d.height):null;
                if(r==null || !rail.contains(r) || overlaps(r,plan.bounds))r=free(rail,d,plan.bounds,true);
                if(r==null) {fits=false;break;}
                plan.bounds.put(key,r);
            }
            plan.bottomHeight=bottomHeight;
            if(fits)break;
            bottomHeight+=sizes.values().stream().mapToInt(d -> d.height).max().orElse(36)+GAP;
        }
        return plan;
    }

    private static Rectangle free(Rectangle rail,Dimension size,Map<String,Rectangle> occupied,boolean horizontal) {
        for(int y=rail.y;y+size.height<=rail.y+rail.height;y++) {
            int x=horizontal?rail.x:rail.x+(rail.width-size.width)/2;
            while(x+size.width<=rail.x+rail.width) {
                Rectangle candidate=new Rectangle(x,y,size.width,size.height),collision=null;
                for(Rectangle r:occupied.values()) {
                    Rectangle spaced=new Rectangle(r);spaced.grow(GAP,GAP);
                    if(candidate.intersects(spaced)) {collision=spaced;break;}
                }
                if(collision==null)return candidate;
                if(!horizontal)break;
                x=collision.x+collision.width;
            }
        }
        return null;
    }

    private static boolean overlaps(Rectangle bounds,Map<String,Rectangle> occupied) {
        for(Rectangle r:occupied.values())if(r.intersects(bounds))return true;
        return false;
    }
}
