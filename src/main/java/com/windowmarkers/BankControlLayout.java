package com.windowmarkers;

import java.awt.Rectangle;
import java.util.*;
import net.runelite.api.gameval.InterfaceID;

/** Saved preferences contain named controls, never arbitrary widget IDs. */
final class BankControlLayout
{
    static final int DOCK_WIDTH=48, DOCK_X=42, TOP=54, PITCH=38, MIN_DOCK_WIDTH=526;
    enum Control
    {
        SWAP("Swap / insert", "Swap", InterfaceID.Bankmain.SWAP_INSERT, InterfaceID.Bankmain.SWAP_INSERT_GRAPHIC, 36, 3, 7),
        NOTE("Item / note", "Note", InterfaceID.Bankmain.NOTE, InterfaceID.Bankmain.NOTE_GRAPHIC, 36, 3, 7),
        ONE("Quantity: 1", "1", InterfaceID.Bankmain.QUANTITY1, InterfaceID.Bankmain.QUANTITY1_TEXT, 36, 6, 8),
        FIVE("Quantity: 5", "5", InterfaceID.Bankmain.QUANTITY5, InterfaceID.Bankmain.QUANTITY5_TEXT, 36, 6, 8),
        TEN("Quantity: 10", "10", InterfaceID.Bankmain.QUANTITY10, InterfaceID.Bankmain.QUANTITY10_TEXT, 36, 6, 8),
        X("Quantity: X", "X", InterfaceID.Bankmain.QUANTITYX, InterfaceID.Bankmain.QUANTITYX_TEXT, 36, 6, 8),
        ALL("Quantity: All", "All", InterfaceID.Bankmain.QUANTITYALL, InterfaceID.Bankmain.QUANTITYALL_TEXT, 36, 6, 8),
        SEARCH("Search", "Find", InterfaceID.Bankmain.SEARCH, InterfaceID.Bankmain.SEARCH_GRAPHIC, 46, 9, 7),
        PLACEHOLDERS("Placeholders", "Lock", InterfaceID.Bankmain.PLACEHOLDER, InterfaceID.Bankmain.PLACEHOLDER_GRAPHIC, 36, 3, 7),
        CONTAINERS("Empty containers", "Empty", InterfaceID.Bankmain.DEPOSITCONTAINERS, InterfaceID.Bankmain.DEPOSITCONTAINERS_GRAPHIC, 36, 3, 7),
        INVENTORY("Deposit inventory", "Inv", InterfaceID.Bankmain.DEPOSITINV, InterfaceID.Bankmain.DEPOSITINV_GRAPHIC, 36, 3, 7),
        WORN("Deposit worn items", "Worn", InterfaceID.Bankmain.DEPOSITWORN, InterfaceID.Bankmain.DEPOSITWORN_GRAPHIC, 36, 3, 7);

        final String label, shortLabel;
        final int button, graphic, width, iconX, iconY;
        Control(String label,String shortLabel,int button,int graphic,int width,int iconX,int iconY)
        { this.label=label;this.shortLabel=shortLabel;this.button=button;this.graphic=graphic;this.width=width;this.iconX=iconX;this.iconY=iconY; }
        boolean quantity() { return ordinal()>=ONE.ordinal() && ordinal()<=ALL.ordinal(); }
        @Override public String toString() { return label; }
    }
    static final class Entry
    {
        final Control control;
        int position;
        boolean left;
        Entry(Control control,int position,boolean left) { this.control=control;this.position=position;this.left=left; }
        @Override public String toString() { return control.label+(left?"  — left dock":""); }
    }
    static final class Plan
    {
        final Map<Control,Rectangle> bounds=new EnumMap<>(Control.class);
        final Set<Control> docked=EnumSet.noneOf(Control.class);
        int inset;
    }
    final List<Entry> entries=new ArrayList<>();

    static BankControlLayout read(String value)
    {
        BankControlLayout layout=new BankControlLayout();Set<Control> seen=EnumSet.noneOf(Control.class);
        if(value!=null && value.startsWith("v1;") && value.length()<2048)
            for(String part:value.substring(3).split(";")) {
                String[] fields=part.split(":");if(fields.length!=3)continue;
                try {
                    Control control=Control.valueOf(fields[0]);int position=Integer.parseInt(fields[1]);
                    if((!fields[2].equals("L") && !fields[2].equals("B")) || !seen.add(control))continue;
                    layout.entries.add(new Entry(control,Math.max(0,Math.min(1000,position)),fields[2].equals("L")));
                } catch(IllegalArgumentException ignored) { /* Unknown or malformed preference: retain defaults. */ }
            }
        for(Control control:Control.values()) if(!seen.contains(control))
            layout.entries.add(new Entry(control,control.ordinal()*1000/(Control.values().length-1),false));
        layout.sort();return layout;
    }
    void sort() { entries.sort(Comparator.comparingInt(e -> e.position)); }
    String write()
    {
        StringBuilder out=new StringBuilder("v1;");
        for(Entry entry:entries)out.append(entry.control.name()).append(':').append(entry.position).append(':').append(entry.left?'L':'B').append(';');
        return out.toString();
    }
    void move(Entry entry,int direction)
    {
        int index=entries.indexOf(entry),other=index+direction;if(other<0 || other>=entries.size())return;
        Entry neighbour=entries.get(other);int position=entry.position;entry.position=neighbour.position;neighbour.position=position;
        Collections.swap(entries,index,other);
    }
    Plan plan(int width,int height,Set<Control> visible)
    {
        Plan plan=new Plan();List<Entry> row=new ArrayList<>();
        int capacity=width>=MIN_DOCK_WIDTH?Math.max(0,(height-38-TOP)/PITCH):0;
        for(Entry entry:entries) {
            if(!visible.contains(entry.control))continue;
            if(entry.left && plan.docked.size()<capacity) {
                plan.bounds.put(entry.control,new Rectangle(DOCK_X,TOP+plan.docked.size()*PITCH,entry.control.width,36));
                plan.docked.add(entry.control);
            } else row.add(entry);
        }
        plan.inset=plan.docked.isEmpty()?0:DOCK_WIDTH;
        int remaining=row.stream().mapToInt(e -> e.control.width+2).sum(),cursor=2;
        for(Entry entry:row) {
            int size=entry.control.width;remaining-=size+2;
            int desired=2+(int)((long)entry.position*Math.max(0,width-4-size)/1000);
            int x=Math.max(cursor,Math.min(desired,width-2-size-remaining));
            plan.bounds.put(entry.control,new Rectangle(x,height-37,size,36));cursor=x+size+2;
        }
        return plan;
    }
}
