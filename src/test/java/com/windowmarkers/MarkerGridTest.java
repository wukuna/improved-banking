package com.windowmarkers;

import java.awt.Rectangle;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class MarkerGridTest
{
    @Test public void drawingUsesTheSelectedSectionsRowOriginInsteadOfTheFirstBankRow() {
        ItemAnchors.Cell first=new ItemAnchors.Cell(1,new Rectangle(51,4,36,32));
        ItemAnchors.Cell later=new ItemAnchors.Cell(2,new Rectangle(51,93,36,32));
        List<ItemAnchors.Cell> cells=Arrays.asList(first,later);
        Rectangle snapped=ItemAnchors.snap(new Rectangle(55,98,24,22),cells);
        assertEquals(later.grid,snapped);assertTrue(snapped.contains(later.icon));
        // Scrolling does not change which row is selected.
        first.icon.translate(0,-60);first.grid.translate(0,-60);
        later.icon.translate(0,-60);later.grid.translate(0,-60);
        assertEquals(later.grid,ItemAnchors.snap(new Rectangle(55,38,24,22),cells));
    }
    @Test public void wideBankSpacingProducesJoinedBordersWithoutCoveringAdjacentItems() {
        List<ItemAnchors.Cell> cells=Arrays.asList(
            new ItemAnchors.Cell(1,new Rectangle(51,4,36,32)),
            new ItemAnchors.Cell(2,new Rectangle(102,4,36,32)),
            new ItemAnchors.Cell(3,new Rectangle(153,4,36,32)));
        ItemAnchors.fitGrid(cells);
        assertEquals(cells.get(0).grid.x+cells.get(0).grid.width,cells.get(1).grid.x);
        List<Rectangle> joined=ItemAnchors.regions(new int[]{1,2},cells);
        assertEquals(1,joined.size());assertFalse(joined.get(0).intersects(cells.get(2).icon));
        assertEquals(joined.get(0),ItemAnchors.snap(new Rectangle(55,9,78,22),cells));
    }
    @Test public void emptySpaceAndEquipmentGapsDoNotInventItemCells() {
        List<ItemAnchors.Cell> cells=Arrays.asList(new ItemAnchors.Cell(1,new Rectangle(51,4,36,32)),
            new ItemAnchors.Cell(2,new Rectangle(291,4,36,32)));
        ItemAnchors.fitGrid(cells);
        assertEquals(48,cells.get(0).grid.width);
        Rectangle empty=new Rectangle(120,50,80,40);
        assertEquals(empty,ItemAnchors.snap(empty,cells));
    }
}
