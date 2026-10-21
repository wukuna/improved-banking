package com.windowmarkers;

import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.*;

final class MarkerLegend extends JPanel
{
    static final class Entry
    {
        final Marker marker;
        final String name, detail;
        final Color color;
        Entry(Marker marker,String name,Color color)
        { this(marker,name,color,marker.linked() ? java.util.Arrays.stream(marker.linkedItemIds).distinct().count()+" linked item types" : "Area marker"); }
        Entry(Marker marker,String name,Color color,String detail)
        {
            this.marker=marker; this.name=name; this.color=color;
            this.detail=detail;
        }
    }
    static List<Entry> forTab(List<Marker> markers,List<ItemAnchors.Cell> cells,Rectangle bank,Rectangle area,int scroll,Color defaultColor)
    {
        List<Entry> entries=new java.util.ArrayList<>();
        if(bank==null || area==null) { return entries; }
        java.util.Set<Integer> tabIds=new java.util.HashSet<>();
        for(ItemAnchors.Cell cell:cells) { tabIds.add(cell.id); }
        for(int i=0;i<markers.size();i++)
        {
            Marker marker=markers.get(i);
            java.util.Set<Integer> matches=new java.util.HashSet<>();
            Rectangle bounds=marker.linked() ? null : marker.onScreen(bank,area,scroll);
            if(marker.linked())
            {
                for(int id:marker.linkedItemIds) { if(tabIds.contains(id)) { matches.add(id); } }
            }
            else for(ItemAnchors.Cell cell:cells)
            { if(bounds!=null && bounds.contains(cell.icon.getCenterX(),cell.icon.getCenterY())) { matches.add(cell.id); } }
            if(matches.isEmpty()) { continue; }
            entries.add(new Entry(marker,marker.labelText().isEmpty() ? "Marker "+(i+1) : marker.labelText(),
                marker.borderArgb==null ? defaultColor : new Color(marker.borderArgb,true),
                matches.size()+" item types in this tab"+(marker.linked() ? "" : " · area")));
        }
        return entries;
    }
    static boolean same(List<Entry> a,List<Entry> b)
    {
        if(a==null || a.size()!=b.size()) { return false; }
        for(int i=0;i<a.size();i++)
        {
            Entry x=a.get(i),y=b.get(i);
            if(x.marker!=y.marker || !x.name.equals(y.name) || !x.detail.equals(y.detail) || !x.color.equals(y.color)) { return false; }
        }
        return true;
    }
    private final DefaultListModel<Entry> rows=new DefaultListModel<>();
    private final JList<Entry> list=new JList<>(rows);
    MarkerLegend(Consumer<Marker> hover,Consumer<Marker> pin)
    {
        super(new BorderLayout(0,8));
        add(new JLabel("<html>Current tab / search results.<br>Hover to highlight. Click to pin.</html>"),BorderLayout.NORTH);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer((view,entry,index,selected,focus) -> {
            JPanel row=new JPanel(new BorderLayout(6,0));
            row.setBorder(BorderFactory.createEmptyBorder(5,4,5,4));
            row.setBackground(selected ? view.getSelectionBackground() : view.getBackground());
            JPanel swatch=new JPanel(); swatch.setBackground(entry.color); swatch.setPreferredSize(new Dimension(8,28));
            JPanel text=new JPanel(new GridLayout(0,1)); text.setOpaque(false);
            JLabel name=new JLabel(entry.name), detail=new JLabel(entry.detail);
            name.setForeground(selected ? view.getSelectionForeground() : view.getForeground());
            detail.setForeground(name.getForeground()); detail.setFont(detail.getFont().deriveFont(10f));
            text.add(name); text.add(detail); row.add(swatch,BorderLayout.WEST); row.add(text,BorderLayout.CENTER);
            return row;
        });
        list.addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseMoved(MouseEvent e)
            {
                int i=list.locationToIndex(e.getPoint());
                Entry entry=i>=0 && list.getCellBounds(i,i).contains(e.getPoint()) ? rows.get(i) : null;
                hover.accept(entry==null ? null : entry.marker);
                list.setToolTipText(entry==null ? null : entry.name+" — "+entry.detail);
            }
        });
        list.addMouseListener(new MouseAdapter() { @Override public void mouseExited(MouseEvent e) { hover.accept(null); } });
        list.addListSelectionListener(e -> { if(!e.getValueIsAdjusting()) { Entry entry=list.getSelectedValue(); pin.accept(entry==null ? null : entry.marker); } });
        add(new JScrollPane(list),BorderLayout.CENTER);
        JButton clear=new JButton("Clear highlight"); clear.addActionListener(e -> { list.clearSelection(); hover.accept(null); pin.accept(null); });
        add(clear,BorderLayout.SOUTH);
    }
    void replace(List<Entry> entries)
    {
        Entry selected=list.getSelectedValue();
        list.setValueIsAdjusting(true);
        rows.clear();
        for(Entry entry:entries) { rows.addElement(entry); }
        if(selected!=null) for(int i=0;i<rows.size();i++)
        { if(rows.get(i).marker==selected.marker) { list.setSelectedIndex(i); break; } }
        list.setValueIsAdjusting(false);
    }
}
