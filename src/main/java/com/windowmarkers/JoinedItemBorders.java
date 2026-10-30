package com.windowmarkers;

import java.awt.*;
import java.awt.geom.Area;
import java.util.*;
import java.util.List;

/** Union outlines preserve holes and L-shaped groups without drawing internal seams. */
final class JoinedItemBorders
{
    private final Map<List<Rectangle>,Shape> shapes=new LinkedHashMap<List<Rectangle>,Shape>(16,0.75f,true) {
        @Override protected boolean removeEldestEntry(Map.Entry<List<Rectangle>,Shape> entry) { return size()>32; }
    };
    void clear() { shapes.clear(); }
    Shape outline(List<Rectangle> regions) {
        Shape shape=shapes.get(regions);
        if(shape!=null) return shape;
        List<Rectangle> key=new ArrayList<>();Area area=new Area();
        for(Rectangle region:regions) if(region!=null && !region.isEmpty()) {
            Rectangle copy=new Rectangle(region);key.add(copy);area.add(new Area(copy));
        }
        shapes.put(key,area);return area;
    }
    void draw(Graphics2D g,List<Rectangle> regions,Color colour) {
        g.setColor(colour);g.draw(outline(regions));
    }
    static Map<Color,List<Rectangle>> groups(List<ItemAnchors.Cell> cells,int[] highlighted,
        Map<Integer,ItemCatalogueIndex.Rank> ranks,WindowMarkersConfig config)
    {
        Set<Integer> owned=new HashSet<>();for(int id:highlighted) owned.add(id);
        Map<Color,List<Rectangle>> result=new LinkedHashMap<>();
        for(ItemAnchors.Cell cell:cells) {
            ItemCatalogueIndex.Rank rank=ranks.get(cell.id);
            if(rank!=null && BankCleanup.isReview(rank)) continue;
            Color colour=rank!=null && config.geTierColours() && rank.tier>=1 && rank.tier<=3
                ?TierPresentation.colour(config,rank.tier):owned.contains(cell.id)?Color.YELLOW:null;
            if(colour!=null) result.computeIfAbsent(colour,key->new ArrayList<>()).add(cell.grid);
        }
        return result;
    }
}
