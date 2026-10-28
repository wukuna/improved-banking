package com.windowmarkers;

import java.awt.Rectangle;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class LinkedMarkerRegionsTest
{
    private Marker marker(int... ids)
    {
        Marker marker = new Marker(0, 0, 48, 36);
        marker.linkedItemIds = ids;
        return marker;
    }

    private ItemAnchors.Cell cell(int id, int x, int y)
    { return new ItemAnchors.Cell(id, new Rectangle(x + 6, y + 2, 36, 32)); }

    @Test public void backgroundBordersAndHoverShareOneGeometrySnapshot()
    {
        LinkedMarkerRegions cache = new LinkedMarkerRegions();
        Marker marker = marker(1, 2);
        List<ItemAnchors.Cell> cells = Arrays.asList(cell(1, 0, 0), cell(2, 48, 0), cell(3, 144, 0));
        List<Rectangle> regions = cache.get(marker, cells);
        assertEquals(Collections.singletonList(new Rectangle(0, 0, 96, 36)), regions);
        assertSame(regions, cache.get(marker, cells));
        assertSame(regions, cache.get(marker, cells));
    }

    @Test public void scrollingReflowAndTabSwitchUseFreshGeometry()
    {
        LinkedMarkerRegions cache = new LinkedMarkerRegions();
        Marker marker = marker(1, 2);
        cache.get(marker, Arrays.asList(cell(1, 0, 0), cell(2, 48, 0)));
        assertEquals(Collections.singletonList(new Rectangle(0, -36, 48, 72)),
            cache.get(marker, Arrays.asList(cell(1, 0, -36), cell(2, 0, 0))));
        assertEquals(Collections.singletonList(new Rectangle(96, 72, 48, 36)),
            cache.get(marker, Arrays.asList(cell(2, 96, 72), cell(3, 144, 72))));
        assertTrue(cache.get(marker, Collections.singletonList(cell(3, 0, 0))).isEmpty());
    }

    @Test public void membershipEditsBetweenPassesCannotKeepOldHighlights()
    {
        LinkedMarkerRegions cache = new LinkedMarkerRegions();
        Marker marker = marker(1);
        List<ItemAnchors.Cell> cells = Arrays.asList(cell(1, 0, 0), cell(2, 96, 0));
        cache.get(marker, cells);
        marker.linkedItemIds[0] = 2;
        assertEquals(Collections.singletonList(new Rectangle(96, 0, 48, 36)), cache.get(marker, cells));
        marker.linkedItemIds = new int[0];
        assertTrue(cache.get(marker, cells).isEmpty());
    }

    @Test public void newFrameAndClosedContextReleasePreviousBounds()
    {
        LinkedMarkerRegions cache = new LinkedMarkerRegions();
        Marker marker = marker(1);
        List<ItemAnchors.Cell> cells = Collections.singletonList(cell(1, 0, 0));
        cache.get(marker, cells);
        cache.clear();
        cells.get(0).grid.translate(48, 36);
        assertEquals(Collections.singletonList(new Rectangle(48, 36, 48, 36)), cache.get(marker, cells));
        cache.clear();
        assertTrue(cache.get(marker, Collections.emptyList()).isEmpty());
    }
}
