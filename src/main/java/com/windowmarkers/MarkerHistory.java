package com.windowmarkers;

import java.util.ArrayDeque;
import java.util.Deque;

final class MarkerHistory
{
    private String current;
    private final Deque<String> undo=new ArrayDeque<>(), redo=new ArrayDeque<>();
    void reset(String value) { current=value; undo.clear(); redo.clear(); }
    void record(String value)
    {
        if (value.equals(current)) { return; }
        if (current != null) { undo.addLast(current); if (undo.size()>50) { undo.removeFirst(); } }
        current=value; redo.clear();
    }
    String undo()
    {
        if (undo.isEmpty()) { return null; }
        redo.addLast(current); current=undo.removeLast(); return current;
    }
    String redo()
    {
        if (redo.isEmpty()) { return null; }
        undo.addLast(current); current=redo.removeLast(); return current;
    }
    boolean matches(String value) { return value.equals(current); }
}
