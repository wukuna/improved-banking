package com.windowmarkers;

import java.util.*;

/** Identity-based edits keep filtered-out groups and saved item variants intact. */
final class GroupEdits
{
    static int[] union(Collection<Marker> groups)
    { return groups.stream().filter(Marker::linked).flatMapToInt(m -> Arrays.stream(m.linkedItemIds)).distinct().toArray(); }

    static boolean reorder(List<Marker> all,List<Marker> visible,List<Marker> moving,int dropIndex)
    {
        if(visible.isEmpty() || moving.isEmpty() || dropIndex<0 || dropIndex>visible.size()
            || !all.containsAll(visible) || !visible.containsAll(moving)
            || new HashSet<>(visible).size()!=visible.size()) return false;
        List<Marker> actual=new ArrayList<>();
        for(Marker marker:all) if(visible.contains(marker)) actual.add(marker);
        if(!actual.equals(visible)) return false;
        List<Marker> ordered=new ArrayList<>(visible);
        List<Marker> picked=new ArrayList<>(); int before=0;
        for(int i=0;i<visible.size();i++) if(moving.contains(visible.get(i)))
        { picked.add(visible.get(i)); if(i<dropIndex) before++; }
        ordered.removeAll(picked); ordered.addAll(dropIndex-before,picked);
        if(ordered.equals(visible)) return false;
        int index=0;
        for(int i=0;i<all.size();i++) if(visible.contains(all.get(i))) all.set(i,ordered.get(index++));
        return true;
    }
}
