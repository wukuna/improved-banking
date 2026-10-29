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

    static void fitGrid(List<Cell> cells)
    {
        Map<Integer,List<Cell>> rows=new TreeMap<>();
        for(Cell cell:cells) rows.computeIfAbsent(cell.icon.y,y -> new ArrayList<>()).add(cell);
        for(List<Cell> row:rows.values()) {
            row.sort(Comparator.comparingInt(c -> c.icon.x));
            for(int i=0;i<row.size();i++) {
                Cell cell=row.get(i);
                int left=i>0?cell.icon.x-row.get(i-1).icon.x-row.get(i-1).icon.width:-1;
                int right=i+1<row.size()?row.get(i+1).icon.x-cell.icon.x-cell.icon.width:-1;
                // Larger gaps are equipment-diagram spaces, not slot padding.
                if(left<0 || left>24) left=right>=0 && right<=24?right:12;
                if(right<0 || right>24) right=left;
                int before=left-left/2,after=right/2;
                cell.grid.x=cell.icon.x-before;cell.grid.width=cell.icon.width+before+after;
            }
        }
    }

    static Rectangle snap(Rectangle selection, List<Cell> cells)
    {
        if (cells.isEmpty()) { return selection; }
        // Tab separators and setup sections do not share the first row's origin.
        // Snap to the actual cells touched by the drag, not an invented global grid.
        Rectangle snapped=null;
        for(Cell cell:cells) if(selection.intersects(cell.icon))
            snapped=snapped==null?new Rectangle(cell.grid):snapped.union(cell.grid);
        return snapped==null?new Rectangle(selection):snapped;
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
