package com.windowmarkers;

import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.widgets.JavaScriptCallback;
import net.runelite.api.widgets.Widget;

/** Visible cells owned by one temporary view, retained only until the next native rebuild. */
final class BankViewGeometry
{
    private Widget parent;
    private final List<Position> positions = new ArrayList<>();
    private BankPresetPositions.Layout layout;
    private Runnable save;
    private List<SetupItemLayout.InventoryNote> notes;
    private int height;
    private long dragRevision;
    void cancelDrag() { dragRevision++; }

    void clear() { cancelDrag(); parent = null; positions.clear(); layout = null; save = null; notes = null; height = 0; }

    void capture(Widget container)
    {
        clear(); parent = container;
        Widget[] children = container.getDynamicChildren();
        if (children == null) return;
        for (Widget child : children)
            if (child != null && !child.isSelfHidden() && child.getItemId() >= 0)
                positions.add(new Position(child));
    }

    int capture(Widget container, BankPresetPositions.Layout layout, Runnable save,
        List<SetupItemLayout.InventoryNote> notes, int height)
    {
        capture(container);
        this.layout = layout; this.save = save; this.notes = notes; this.height = height;
        // A repeated supply has a distinct position. Native bank indexes can change,
        // so identify occurrences in the generated setup, not by bank stack index.
        positions.sort(Comparator.comparingInt((Position p) -> p.y).thenComparingInt(p -> p.x));
        Map<Integer, Integer> occurrences = new HashMap<>();
        for (Position p : positions)
        {
            p.key = p.itemId + ":" + occurrences.merge(p.itemId, 1, Integer::sum);
            for (int i = 0; i < notes.size(); i++)
                if (notes.get(i).x == p.x && notes.get(i).y == p.y) { p.note = i; break; }
        }
        for (Position p : positions)
        {
            int[] saved = layout.cells.get(p.key);
            if (saved != null && saved[0] + 36 <= container.getWidth()) move(p, saved[0], saved[1]);
        }
        // Newly added items must remain visible even if their generated cell is
        // already occupied by a saved move. Keep the saved placement authoritative.
        List<Position> occupied = new ArrayList<>();
        List<Position> ordered = new ArrayList<>(positions);
        ordered.sort(Comparator.comparing(p -> !layout.cells.containsKey(p.key)));
        for (Position p : ordered)
        {
            while (overlaps(p, occupied)) move(p, 51, contentHeight() + 4);
            occupied.add(p);
        }
        restore(container);
        return contentHeight();
    }

    private boolean overlaps(Position p, List<Position> others)
    {
        for (Position q : others) if (p.x < q.x + 36 && p.x + 36 > q.x && p.y < q.y + 32 && p.y + 32 > q.y) return true;
        return false;
    }

    /** Only called for an actual user drag in the currently owned view. */
    boolean drag(Client client, Widget container, java.util.function.BooleanSupplier current)
    {
        Widget dragged = client.getDraggedWidget();
        Position source = find(dragged);
        if (container != parent || source == null || !valid(source, container)) return false;
        // Stop native bank-order mutations and replace the tag-layout callback,
        // which otherwise directly repacks the preset without a bank build event.
        client.setDraggedOnWidget(null);
        long revision = dragRevision;
        dragged.setOnDragCompleteListener((JavaScriptCallback) event -> {
            if (revision != dragRevision || !current.getAsBoolean() || container != parent || find(dragged) != source
                || !valid(source, container)) return;
            drop(source, client.getMouseCanvasPosition());
        });
        return true;
    }

    private void drop(Position source, Point mouse)
    {
        Point origin = parent.getCanvasLocation();
        if (origin == null || mouse == null) return;
        int x = mouse.getX() - origin.getX(), visibleY = mouse.getY() - origin.getY();
        if (x < 0 || x >= parent.getWidth() || visibleY < 0 || visibleY >= parent.getHeight()) return;
        int y = visibleY + parent.getScrollY();
        Position target = null;
        for (Position p : positions)
            if (p != source && valid(p, parent) && x >= p.x && x < p.x + 36 && y >= p.y && y < p.y + 32)
            { target = p; break; }
        int nextX, nextY;
        if (target != null)
        {
            nextX = target.x; nextY = target.y;
            move(target, source.x, source.y); remember(target);
        }
        else
        {
            nextX = Math.max(0, Math.min(parent.getWidth() - 36, 51 + Math.round((x - 69) / 48f) * 48));
            nextY = Math.max(0, Math.min(50000, source.y + Math.round((y - source.y - 16) / 36f) * 36));
            // Do not overlap another cell when dropping in the gap beside it.
            for (Position p : positions) if (p != source && nextX < p.x + 36 && nextX + 36 > p.x
                && nextY < p.y + 32 && nextY + 32 > p.y) return;
        }
        move(source, nextX, nextY); remember(source);
        restore(parent);
        parent.setScrollHeight(Math.max(parent.getHeight(), contentHeight() + 8));
        parent.revalidateScroll();
        if (save != null) save.run();
    }

    private void remember(Position p)
    {
        if (layout != null && p.key != null && (layout.cells.containsKey(p.key) || layout.cells.size() < 4000))
            layout.cells.put(p.key, new int[]{p.x, p.y});
    }

    private void move(Position p, int x, int y)
    {
        p.x = x; p.y = y;
        if (notes != null && p.note >= 0)
            notes.set(p.note, new SetupItemLayout.InventoryNote(notes.get(p.note).text, x, y));
    }

    private int contentHeight()
    {
        int result = height;
        for (Position p : positions) result = Math.max(result, p.y + 36);
        return result;
    }

    private Position find(Widget widget)
    {
        for (Position p : positions) if (p.widget == widget) return p;
        return null;
    }

    boolean occupied(int x, int y)
    {
        for (Position p : positions) if (p.x == x && p.y == y) return true;
        return false;
    }

    List<SetupItemLayout.InventoryNote> visibleNotes(List<SetupItemLayout.InventoryNote> original)
    {
        if (notes != original) return original;
        List<SetupItemLayout.InventoryNote> result = new ArrayList<>();
        for (int i = 0; i < notes.size(); i++)
        {
            SetupItemLayout.InventoryNote note = notes.get(i);
            boolean covered = false;
            for (Position p : positions) if (p.x == note.x && p.y == note.y && p.note != i) { covered = true; break; }
            if (!covered) result.add(note);
        }
        return result;
    }

    private boolean valid(Position p, Widget container)
    {
        if (container == null) return false;
        Widget[] children = container.getDynamicChildren();
        int index = p.widget.getIndex();
        return children != null && index >= 0 && index < children.length && children[index] == p.widget
            && p.widget.getItemId() == p.itemId;
    }

    void restore(Widget container)
    {
        if (parent != container || container == null) return;
        Widget[] children = container.getDynamicChildren();
        if (children == null) return;
        for (Position p : positions)
        {
            int index = p.widget.getIndex();
            if (index < 0 || index >= children.length || children[index] != p.widget
                || p.widget.getItemId() != p.itemId) continue;
            // Another layout can deduplicate repeated supplies and phase gear.
            // Restore only cells that this view showed after native filtering,
            // never previously filtered items or widgets replaced since capture.
            if (p.widget.isSelfHidden() || p.widget.getOriginalX() != p.x || p.widget.getOriginalY() != p.y)
            {
                p.widget.setHidden(false);
                p.widget.setOriginalX(p.x); p.widget.setOriginalY(p.y); p.widget.revalidate();
            }
        }
    }

    private static final class Position
    {
        final Widget widget;
        final int itemId;
        int x, y, note = -1;
        String key;
        Position(Widget widget)
        {
            this.widget=widget; itemId=widget.getItemId(); x=widget.getOriginalX(); y=widget.getOriginalY();
        }
    }
}
