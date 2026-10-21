package com.windowmarkers;

import java.awt.Rectangle;
import java.util.*;

/** Pure geometry for matching visible items without changing game widgets. */
final class ItemAnchors
{
    static final class Cell
    {
        final int id;
        final Rectangle icon, grid;
        Cell(int id, Rectangle icon)
        {
            this.id=id; this.icon=new Rectangle(icon);
            grid=new Rectangle(icon);
            // The native bank slot has 12px horizontal and 4px vertical spacing.
            if (grid.width <= 36) { grid.x-=6; grid.width+=12; }
            if (grid.height <= 32) { grid.y-=2; grid.height+=4; }
        }
    }

    static int[] enclosed(Rectangle selection, List<Cell> cells, Rectangle viewport)
    {
        return cells.stream().filter(c -> viewport.contains(c.icon.getCenterX(),c.icon.getCenterY())
            && selection.contains(c.icon.getCenterX(),c.icon.getCenterY())).mapToInt(c -> c.id).distinct().toArray();
    }

    static Rectangle snap(Rectangle selection, List<Cell> cells)
    {
        if (cells.isEmpty()) { return selection; }
        Rectangle grid=cells.get(0).grid;
        int x=grid.x+Math.floorDiv(selection.x-grid.x,grid.width)*grid.width;
        int y=grid.y+Math.floorDiv(selection.y-grid.y,grid.height)*grid.height;
        int right=grid.x+Math.floorDiv(selection.x+selection.width-1-grid.x,grid.width)*grid.width+grid.width;
        int bottom=grid.y+Math.floorDiv(selection.y+selection.height-1-grid.y,grid.height)*grid.height+grid.height;
        return new Rectangle(x,y,right-x,bottom-y);
    }

    static List<Rectangle> regions(int[] ids, List<Cell> cells)
    {
        Set<Integer> wanted=new HashSet<>(); for (int id : ids) { wanted.add(id); }
        List<Rectangle> result=new ArrayList<>();
        for (Cell cell : cells)
        {
            if (wanted.contains(cell.id) && !result.contains(cell.grid)) { result.add(new Rectangle(cell.grid)); }
        }
        result.sort(Comparator.comparingInt((Rectangle r)->r.y).thenComparingInt(r->r.x));
        // Merge only full shared edges: unrelated slots and holes stay unmarked.
        boolean changed;
        do
        {
            changed=false;
            outer: for (int i=0;i<result.size();i++) for (int j=i+1;j<result.size();j++)
            {
                Rectangle a=result.get(i), b=result.get(j);
                boolean horizontal=a.y==b.y && a.height==b.height && (a.x+a.width==b.x || b.x+b.width==a.x);
                boolean vertical=a.x==b.x && a.width==b.width && (a.y+a.height==b.y || b.y+b.height==a.y);
                if (horizontal || vertical)
                { result.set(i,a.union(b)); result.remove(j); changed=true; break outer; }
            }
        } while (changed);
        return result;
    }
}
