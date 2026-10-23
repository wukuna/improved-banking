package com.windowmarkers;

import java.util.*;

/** Separate account/profile-scoped templates; applying copies markers, never moves bank items. */
final class CustomPresets
{
    static final int LIMIT=50;
    List<Preset> presets=new ArrayList<>();
    static final class Preset
    {
        String name;
        List<Marker> groups=new ArrayList<>();
        @Override public String toString() { return name; }
    }
    void normalize()
    {
        if(presets==null) presets=new ArrayList<>();
        Set<String> names=new HashSet<>();
        presets.removeIf(p -> p==null || p.name==null || p.name.trim().isEmpty());
        if(presets.size()>LIMIT) presets=new ArrayList<>(presets.subList(0,LIMIT));
        for(Preset p:presets)
        {
            p.name=p.name.trim().substring(0,Math.min(60,p.name.trim().length()));
            if(p.groups==null) p.groups=new ArrayList<>();
            p.groups.removeIf(m -> m==null || !m.valid() || !m.linked() || m.linkedItemIds.length>4000);
            if(p.groups.size()>250) p.groups=new ArrayList<>(p.groups.subList(0,250));
            for(Marker group:p.groups) group.linkedItemIds=Arrays.stream(group.linkedItemIds).filter(id -> id>=0).distinct().toArray();
            p.groups.removeIf(m -> !m.linked());
        }
        presets.removeIf(p -> !names.add(p.name.toLowerCase(Locale.ROOT)) || p.groups.isEmpty());
    }
}
