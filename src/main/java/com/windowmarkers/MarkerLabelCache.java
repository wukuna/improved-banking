package com.windowmarkers;

import java.awt.Rectangle;
import java.util.*;

/** Avoid repeating free-space searches while the bank and its labels are stationary. */
final class MarkerLabelCache
{
    private List<Rectangle> previousItems=Collections.emptyList();
    private final List<Request> previous=new ArrayList<>();
    private int cursor;

    void begin(List<Rectangle> items)
    {
        if(!previousItems.equals(items)) {
            previousItems=copy(items); previous.clear();
        }
        cursor=0;
    }

    Rectangle place(Rectangle region,Rectangle clip,int width,int height,Marker.LabelPlacement preference,List<Rectangle> occupied)
    {
        Request request=new Request(region,clip,width,height,preference);
        if(cursor<previous.size() && previous.get(cursor).same(request)) {
            Rectangle result=previous.get(cursor++).result;
            if(result!=null) occupied.add(new Rectangle(result));
            return result;
        }
        // A changed/removed earlier label changes the free space for every later label.
        while(previous.size()>cursor) previous.remove(previous.size()-1);
        request.result=MarkerPresentation.place(region,clip,width,height,preference,occupied);
        previous.add(request);cursor++;return request.result;
    }

    private static List<Rectangle> copy(List<Rectangle> source)
    { List<Rectangle> result=new ArrayList<>();for(Rectangle r:source) result.add(new Rectangle(r));return result; }

    private static final class Request
    {
        final Rectangle region,clip;
        final int width,height;
        final Marker.LabelPlacement preference;
        Rectangle result;
        Request(Rectangle region,Rectangle clip,int width,int height,Marker.LabelPlacement preference)
        { this.region=new Rectangle(region);this.clip=new Rectangle(clip);this.width=width;this.height=height;this.preference=preference; }
        boolean same(Request other)
        { return region.equals(other.region)&&clip.equals(other.clip)&&width==other.width&&height==other.height&&preference==other.preference; }
    }
}
