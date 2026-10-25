package com.windowmarkers;

import java.awt.Color;
import java.util.*;

/** Apply reviewed classifications without discarding other uses or resetting existing styles. */
final class CategoryGroups
{
    static final class Result
    {
        final List<Marker> selected=new ArrayList<>();
        int added, existing, skipped, limited;
    }
    static Result add(List<Marker> markers,Map<String,List<AutoCategories.Item>> groups,String preset,
        boolean includeGrouped,Color defaultColor,int limit)
    {
        Result result=new Result();
        Set<Integer> assigned=new HashSet<>(); List<Color> colors=new ArrayList<>();
        for(Marker marker:markers) {
            if(marker.linked()) for(int id:marker.linkedItemIds) assigned.add(id);
            colors.add(marker.borderArgb==null?defaultColor:new Color(marker.borderArgb,true));
        }
        for(Map.Entry<String,List<AutoCategories.Item>> entry:groups.entrySet()) {
            int[] requested=entry.getValue().stream().mapToInt(i -> i.id).distinct().sorted().toArray();
            Marker existing=markers.stream().filter(m -> m.linked() && entry.getKey().equals(m.category)
                && preset.equals(m.preset) && Arrays.equals(requested,Arrays.stream(m.linkedItemIds).distinct().sorted().toArray())).findFirst().orElse(null);
            if(existing!=null) { result.existing++; result.selected.add(existing); continue; }
            int[] ids=Arrays.stream(requested).filter(id -> includeGrouped || !assigned.contains(id)).toArray();
            if(ids.length==0) { result.skipped++; continue; }
            if(markers.size()>=limit) { result.limited++; continue; }
            Marker marker=new Marker(0,0,48,36); marker.scrollWithItems=true; marker.linkedItemIds=ids;
            marker.label=MarkerPresentation.title(entry.getKey()); marker.labelOnce=true;
            marker.category=entry.getKey(); marker.preset=preset; marker.labelFont="Arial"; marker.labelSize=12;
            marker.labelPlacement=Marker.LabelPlacement.ABOVE; marker.style=WindowMarkersConfig.BorderStyle.PLAIN;
            Color color=MarkerPresentation.nextColor(colors); colors.add(color);
            marker.borderArgb=color.getRGB(); marker.backgroundArgb=new Color(color.getRed(),color.getGreen(),color.getBlue(),65).getRGB();
            markers.add(marker); result.selected.add(marker); result.added++;
            for(int id:ids) assigned.add(id);
        }
        return result;
    }
}
