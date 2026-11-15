package com.windowmarkers;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class BankActionLayout
{
    static final int TOP=54,RAIL=50,GAP=2;
    private static final char[] DOCKS={'R','L','T','B'};
    static final class Entry {
        final char dock;
        int order,offset=-1;
        Entry(char dock,int order) {this.dock=dock;this.order=order;}
    }
    static final class Plan {
        final Map<String,Rectangle> bounds=new LinkedHashMap<>();
        final Map<String,Character> docks=new LinkedHashMap<>();
        final Map<String,Track> tracks=new LinkedHashMap<>();
        int leftInset,bottomHeight=36,tagTop,tagMaxOffset;
        void place(String key,Track track,Rectangle bounds) {
            this.bounds.put(key,bounds);docks.put(key,track.dock);tracks.put(key,track);
            track.cursor=track.horizontal?bounds.x+bounds.width+GAP:bounds.y+bounds.height+GAP;
            if(track.dock=='L')leftInset=RAIL;
        }
    }
    private static final class Track {
        final char dock;
        final Rectangle bounds;
        final boolean horizontal;
        int cursor;
        Track(char dock,Rectangle bounds) {
            this.dock=dock;this.bounds=bounds;horizontal=dock=='B';cursor=horizontal?bounds.x:bounds.y;
        }
        Rectangle next(Dimension size,Map<String,Rectangle> occupied) {
            int position=cursor;
            while(true) {
                Rectangle candidate=horizontal?
                    new Rectangle(position,bounds.y+(bounds.height-size.height)/2,size.width,size.height):
                    new Rectangle(bounds.x+(bounds.width-size.width)/2,position,size.width,size.height);
                if(!bounds.contains(candidate))return null;
                Rectangle collision=null;
                for(Rectangle other:occupied.values()) {
                    Rectangle spaced=new Rectangle(other);spaced.grow(GAP,GAP);
                    if(candidate.intersects(spaced)) {collision=spaced;break;}
                }
                if(collision==null)return candidate;
                position=horizontal?collision.x+collision.width:collision.y+collision.height;
            }
        }
        double distance(Point point,Rectangle candidate) {
            Rectangle available=new Rectangle(bounds);
            if(horizontal) {available.width-=candidate.x-available.x;available.x=candidate.x;}
            else {available.height-=candidate.y-available.y;available.y=candidate.y;}
            int x=Math.max(available.x,Math.min(point.x,available.x+available.width));
            int y=Math.max(available.y,Math.min(point.y,available.y+available.height));
            return point.distanceSq(x,y);
        }
    }
    final Map<String,Entry> entries=new LinkedHashMap<>();
    int tagOffset;

    static BankActionLayout read(String value)
    {
        BankActionLayout layout=new BankActionLayout();
        int i=0;
        for(BankControlLayout.Control control:BankControlLayout.Control.values())
            layout.entries.put(control.name(),new Entry(control.quantity()?'B':'R',i++*50));
        if(value==null || !(value.startsWith("v1;") || value.startsWith("v2;") || value.startsWith("v3;")) || value.length()>8192)return layout;
        for(String part:value.substring(3).split(";")) {
            String[] fields=part.split(":");
            if(fields.length==2 && fields[0].equals("split")) {
                try {layout.tagOffset=Math.max(0,Math.min(4000,Integer.parseInt(fields[1])));}catch(NumberFormatException ignored) { }
                continue;
            }
            if((fields.length!=3 && fields.length!=5 && !(value.startsWith("v3;") && fields.length==4)) || !fields[0].matches("[A-Z_]{1,32}") || fields[1].length()!=1
                || "BLRTN".indexOf(fields[1].charAt(0))<0)continue;
            try {
                char dock=fields[1].charAt(0);
                int order=Math.max(0,Math.min(1000,Integer.parseInt(fields[2]))),offset=-1;
                if(fields.length==5) {
                    int x=Integer.parseInt(fields[3]),y=Integer.parseInt(fields[4]);
                    if(x<0 || y<0 || x>4000 || y>4000)continue;
                    offset=dock=='B'?x:y;order=offset;
                }
                if(fields.length==4)offset=Math.max(0,Math.min(4000,Integer.parseInt(fields[3])));
                Entry entry=new Entry(dock,order);entry.offset=offset;layout.entries.put(fields[0],entry);
            }
            catch(NumberFormatException ignored) { }
        }
        for(char dock:DOCKS) {
            int order=0;
            for(String key:layout.ordered(dock))layout.entries.get(key).order=order++;
        }
        return layout;
    }

    String write()
    {
        StringBuilder out=new StringBuilder("v3;");
        if(tagOffset>0)out.append("split:").append(tagOffset).append(';');
        entries.forEach((key,e) -> {
            out.append(key).append(':').append(e.dock).append(':').append(e.order);
            if(e.offset>=0)out.append(':').append(e.offset);
            out.append(';');
        });
        return out.toString();
    }

    void move(String key,char dock,int order) {entries.put(key,new Entry(dock,Math.max(0,Math.min(1000,order))));}

    void insert(String key,char dock,Point point,Rectangle rail,Map<String,Rectangle> bounds,Map<String,Character> docks) {
        List<String> keys=ordered(dock);keys.remove(key);
        int index=keys.size();Rectangle moved=bounds.get(key);
        int coordinate=dock=='B'?point.x+(moved==null?0:moved.width/2):point.y+(moved==null?0:moved.height/2);
        for(int i=0;i<keys.size();i++) {
            String other=keys.get(i);Rectangle r=bounds.get(other);
            if(r==null || docks.get(other)==null || docks.get(other)!=dock)continue;
            if(coordinate<=(dock=='B'?r.getCenterX():r.getCenterY())) {
                index=i;break;
            }
        }
        keys.add(index,key);Entry entry=new Entry(dock,0);
        if(dock!='N')entry.offset=Math.max(0,Math.min(4000,dock=='B'?point.x-rail.x:point.y-rail.y));
        entries.put(key,entry);
        for(int i=0;i<keys.size();i++)entries.get(keys.get(i)).order=i;
    }

    private List<String> ordered(char dock) {
        List<String> keys=new ArrayList<>();
        entries.forEach((key,entry) -> {if(entry.dock==dock)keys.add(key);});
        keys.sort(Comparator.comparingInt(key -> entries.get(key).order));
        return keys;
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

    Plan plan(int width,int height,Map<String,Dimension> sizes,boolean tagSpace)
    {
        int rowHeight=Math.max(36,sizes.values().stream().mapToInt(d -> d.height).max().orElse(36));
        Plan plan=new Plan();
        for(int rows=1;rows*(rowHeight+GAP)-GAP<=height-TOP-4;rows++) {
            plan=basePlan(width,height,sizes,tagSpace,rows,rowHeight);
            if(plan.bounds.size()==sizes.keySet().stream().filter(key -> entries.containsKey(key) && entries.get(key).dock!='N').count())break;
        }
        spaceButtons(plan);
        plan.tagTop=tagTop(height,plan.bottomHeight);
        if(!tagSpace)return plan;
        Rectangle rail=rail('T',width,height,plan.bottomHeight);
        int limit=Math.max(0,rail.height);
        for(Map.Entry<String,Rectangle> entry:plan.bounds.entrySet()) {
            if(plan.docks.get(entry.getKey())!='T')continue;
            Rectangle r=entry.getValue();limit=Math.min(limit,rail.y+rail.height-r.y-r.height);
            for(Map.Entry<String,Rectangle> other:plan.bounds.entrySet()) {
                if(plan.docks.get(other.getKey())=='T')continue;
                Rectangle b=other.getValue();
                if(r.x<b.x+b.width && b.x<r.x+r.width && b.y>=r.y+r.height)
                    limit=Math.min(limit,Math.max(0,b.y-r.y-r.height-GAP));
            }
        }
        plan.tagMaxOffset=Math.max(0,limit);
        int offset=Math.min(Math.max(0,tagOffset),plan.tagMaxOffset);
        plan.tagTop+=offset;
        for(Map.Entry<String,Rectangle> entry:plan.bounds.entrySet())if(plan.docks.get(entry.getKey())=='T')entry.getValue().translate(0,offset);
        return plan;
    }

    private void spaceButtons(Plan plan) {
        for(Track track:new java.util.LinkedHashSet<>(plan.tracks.values())) {
            List<String> keys=new ArrayList<>();
            plan.tracks.forEach((key,value)->{if(value==track)keys.add(key);});
            Map<String,Rectangle> spaced=new LinkedHashMap<>();
            int start=track.horizontal?track.bounds.x:track.bounds.y,cursor=start;
            for(String key:keys) {
                Rectangle r=new Rectangle(plan.bounds.get(key));Entry entry=entries.get(key);
                int preferred=entry.dock==track.dock && entry.offset>=0?start+entry.offset:track.horizontal?r.x:r.y;
                int position=Math.max(cursor,preferred);
                if(track.horizontal)r.x=position;else r.y=position;
                spaced.put(key,r);cursor=position+(track.horizontal?r.width:r.height)+GAP;
            }
            int limit=start+(track.horizontal?track.bounds.width:track.bounds.height);
            for(int i=keys.size()-1;i>=0;i--) {
                Rectangle r=spaced.get(keys.get(i));
                int position=Math.min(track.horizontal?r.x:r.y,limit-(track.horizontal?r.width:r.height));
                if(track.horizontal)r.x=position;else r.y=position;
                limit=position-GAP;
            }
            boolean valid=true;
            for(Rectangle r:spaced.values()) {
                if(!track.bounds.contains(r)) {valid=false;break;}
                for(Map.Entry<String,Rectangle> other:plan.bounds.entrySet()) {
                    if(spaced.containsKey(other.getKey()))continue;
                    Rectangle occupied=new Rectangle(other.getValue());occupied.grow(GAP,GAP);
                    if(r.intersects(occupied)) {valid=false;break;}
                }
                if(!valid)break;
            }
            if(valid)plan.bounds.putAll(spaced);
        }
    }

    private Plan basePlan(int width,int height,Map<String,Dimension> sizes,boolean tagSpace,int rows,int rowHeight)
    {
        Plan plan=new Plan();plan.bottomHeight=rows*(rowHeight+GAP)-GAP;
        List<Track> tracks=new ArrayList<>();
        for(char dock:DOCKS)if(dock!='B' && width>=(dock=='L'?536:478) && (dock!='T' || tagSpace))
            tracks.add(new Track(dock,rail(dock,width,height,plan.bottomHeight)));
        Rectangle bottom=rail('B',width,height,plan.bottomHeight);
        for(int i=0;i<rows;i++)tracks.add(new Track('B',new Rectangle(bottom.x,bottom.y+i*(rowHeight+GAP),bottom.width,rowHeight)));
        Map<Character,List<String>> overflow=new LinkedHashMap<>();
        for(char dock:DOCKS) {
            List<String> remaining=new ArrayList<>();overflow.put(dock,remaining);
            boolean full=false;
            for(String key:ordered(dock)) {
                Dimension size=sizes.get(key);if(size==null)continue;
                Track selected=null;Rectangle target=null;
                if(!full)for(Track track:tracks)if(track.dock==dock) {
                    Rectangle candidate=track.next(size,plan.bounds);
                    if(candidate!=null) {selected=track;target=candidate;break;}
                }
                if(selected!=null)plan.place(key,selected,target);
                else {remaining.add(key);if(dock=='B' || size.width<=RAIL-4)full=true;}
            }
        }
        for(Map.Entry<Character,List<String>> group:overflow.entrySet()) {
            Rectangle source=rail(group.getKey(),width,height,plan.bottomHeight);
            Point origin=new Point(source.x+source.width/2,source.y);
            for(Map.Entry<String,Rectangle> placed:plan.bounds.entrySet())if(plan.docks.get(placed.getKey()).equals(group.getKey())) {
                Rectangle r=placed.getValue();origin=new Point(r.x+r.width/2,r.y+r.height/2);
            }
            Track previous=null;
            for(String key:group.getValue()) {
                Dimension size=sizes.get(key);Track selected=null;Rectangle target=null;double distance=Double.MAX_VALUE;
                for(Track track:tracks) {
                    Rectangle candidate=track.next(size,plan.bounds);if(candidate==null)continue;
                    double next=track==previous?-1:track.distance(origin,candidate);
                    if(next<distance) {selected=track;target=candidate;distance=next;}
                }
                if(selected==null)continue;
                plan.place(key,selected,target);previous=selected;
                origin=new Point(target.x+target.width/2,target.y+target.height/2);
            }
        }
        return plan;
    }
}
