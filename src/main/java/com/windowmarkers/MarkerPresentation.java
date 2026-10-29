package com.windowmarkers;

import java.awt.*;
import java.util.List;

final class MarkerPresentation
{
    static String miniTitle(String label)
    {
        String shortTitle=displayTitle(label);
        int separator=shortTitle.indexOf(" — ");
        if(separator>=0) { shortTitle=shortTitle.substring(0,separator); }
        return shortTitle.replace(" weapons","").replace(" equipment","");
    }
    static String displayTitle(String label)
    {
        // Legacy generated descriptions can be shortened without rewriting saved custom labels.
        if(label.contains(" — ") && (label.contains("set") || label.contains("synergy") || label.startsWith("Void —")
            || label.contains("not activity advice") || label.startsWith("Restricted-use supplies"))) { return title(label); }
        return label;
    }
    static String title(String group)
    {
        for(String family:new String[]{"Ahrim","Dharok","Guthan","Karil","Torag","Verac","Blood Moon","Blue Moon","Eclipse Moon","Void","Inquisitor","Crystal","Justiciar","Obsidian"})
        { if(group.startsWith(family+" —")) { return family; } }
        if(group.startsWith("Restricted-use supplies")) { return "Blighted supplies"; }
        if(group.startsWith("Food —")) { return "Food"; }
        if(group.startsWith("Potions —")) { return "Potions"; }
        if(group.startsWith("Other —")) { return "Unclassified"; }
        return group.replaceAll("\\s*\\([^)]*\\)","").replace(" — general, not activity advice","")
            .replace(" — check current quest","").replace(" — check restrictions","");
    }
    static Color nextColor(List<Color> used)
    {
        Color best=null; double bestDistance=-1;
        for(int h=0;h<360;h+=3) for(float s:new float[]{0.5f,0.75f,1f}) for(float b:new float[]{0.65f,0.85f,1f})
        {
            Color c=Color.getHSBColor(h/360f,s,b); double nearest=Double.MAX_VALUE;
            for(Color old:used)
            {
                double r=c.getRed()-old.getRed(),g=c.getGreen()-old.getGreen(),blue=c.getBlue()-old.getBlue();
                nearest=Math.min(nearest,2*r*r+4*g*g+3*blue*blue);
            }
            if(nearest>bestDistance) { bestDistance=nearest; best=c; }
        }
        return best;
    }
    static String fit(String text,FontMetrics metrics,int width)
    {
        if(metrics.stringWidth(text)<=width) { return text; }
        while(!text.isEmpty() && metrics.stringWidth(text+"…")>width) { text=text.substring(0,text.length()-1); }
        return text.isEmpty() ? "" : text+"…";
    }
    static Rectangle place(Rectangle region,Rectangle clip,int width,int height,Marker.LabelPlacement preference,List<Rectangle> occupied)
    {
        int above=region.y-height-2,inside=region.y+2,below=region.y+region.height+2;
        int[] positions=preference==Marker.LabelPlacement.ABOVE ? new int[]{above,inside,below} : preference==Marker.LabelPlacement.BELOW ? new int[]{below,inside,above} : new int[]{inside,above,below};
        int x=Math.max(clip.x,Math.min(region.x+3,clip.x+clip.width-width));
        for(int y:positions)
        {
            Rectangle candidate=new Rectangle(x,y,width,height);
            if(!clip.contains(candidate)) { continue; }
            if(occupied.stream().noneMatch(candidate::intersects)) { occupied.add(candidate); return candidate; }
        }
        // Look beside the group and in nearby empty bands. A dense row must not
        // silently lose its label merely because above/inside/below are occupied.
        java.util.Set<Integer> bands=new java.util.TreeSet<>();
        bands.add(clip.y);bands.add(clip.y+clip.height-height);
        bands.add(Math.max(clip.y,Math.min(region.y,clip.y+clip.height-height)));
        for(Rectangle obstacle:occupied) {
            if(obstacle.y-height-2>=clip.y) bands.add(obstacle.y-height-2);
            if(obstacle.y+obstacle.height+2<=clip.y+clip.height-height) bands.add(obstacle.y+obstacle.height+2);
        }
        java.util.List<Integer> sorted=new java.util.ArrayList<>(bands);
        sorted.sort(java.util.Comparator.comparingInt(y -> Math.abs(y-region.y)));
        for(int y:sorted) {
            if(y<clip.y || y+height>clip.y+clip.height) continue;
            java.util.List<Rectangle> row=new java.util.ArrayList<>();
            for(Rectangle obstacle:occupied) if(obstacle.y<y+height && obstacle.y+obstacle.height>y) row.add(obstacle);
            row.sort(java.util.Comparator.comparingInt(r -> r.x));
            int left=clip.x;Rectangle best=null;int distance=Integer.MAX_VALUE;
            for(int i=0;i<=row.size();i++) {
                int right=i==row.size()?clip.x+clip.width:Math.min(clip.x+clip.width,row.get(i).x-2);
                if(right-left>=width) {
                    int candidateX=Math.max(left,Math.min(x,right-width));
                    int d=Math.abs(candidateX-x);
                    if(d<distance) { best=new Rectangle(candidateX,y,width,height);distance=d; }
                }
                if(i<row.size()) left=Math.max(left,row.get(i).x+row.get(i).width+2);
            }
            if(best!=null) { occupied.add(best); return best; }
        }
        return null;
    }
}
