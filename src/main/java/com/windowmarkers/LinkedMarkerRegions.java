package com.windowmarkers;

import java.awt.Rectangle;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** Shares linked geometry between background, border and hover passes of one frame. */
final class LinkedMarkerRegions
{
    private final Map<Marker, Entry> entries = new IdentityHashMap<>();

    void clear() { entries.clear(); }

    // The caller clears after taking each fresh item-bounds snapshot and on context changes.
    // Returned rectangles are read-only to callers; placement/label passes must copy before editing.
    List<Rectangle> get(Marker marker, List<ItemAnchors.Cell> cells)
    {
        Entry entry = entries.get(marker);
        // Mouse/Swing edits can arrive between render passes. Check membership by value as well.
        if (entry == null || entry.cells != cells || !Arrays.equals(entry.ids, marker.linkedItemIds))
        {
            int[] ids = marker.linkedItemIds == null ? new int[0] : marker.linkedItemIds.clone();
            entry = new Entry(cells, ids, Collections.unmodifiableList(ItemAnchors.regions(ids, cells)));
            entries.put(marker, entry);
        }
        return entry.regions;
    }

    private static final class Entry
    {
        final List<ItemAnchors.Cell> cells;
        final int[] ids;
        final List<Rectangle> regions;
        Entry(List<ItemAnchors.Cell> cells, int[] ids, List<Rectangle> regions)
        { this.cells = cells; this.ids = ids; this.regions = regions; }
    }
}
