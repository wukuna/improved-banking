package com.windowmarkers;

import java.util.LinkedHashMap;
import java.util.Map;

final class LayoutCatalog
{
    String active="default";
    Map<String,String> names=new LinkedHashMap<>();
    LayoutCatalog() { names.put("default","Default"); }
    void normalize()
    {
        if (names==null) { names=new LinkedHashMap<>(); }
        names.entrySet().removeIf(e -> e.getKey()==null || !e.getKey().matches("default|[a-f0-9-]{36}") || e.getValue()==null);
        names.putIfAbsent("default","Default");
        if (!names.containsKey(active)) { active="default"; }
    }
    String storageKey(String base) { return "default".equals(active) ? base : base+"_layout_"+active; }
}
