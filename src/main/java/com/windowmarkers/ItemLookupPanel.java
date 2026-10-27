package com.windowmarkers;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import javax.swing.*;

/** Select an exact membership to review and add, or temporarily highlight the inspected items. */
final class ItemLookupPanel extends JPanel
{
    static final class Row
    {
        final String membership, owner;
        Row(String membership,String owner) { this.membership=membership; this.owner=owner; }
        @Override public String toString() { return owner+" · "+membership; }
    }
    final JList<Row> list;
    final JButton send=new JButton("Send filter to Legend");
    final JButton highlight=new JButton("Highlight this item(s)");
    String filter;
    String membership;
    boolean highlightRequested;

    ItemLookupPanel(List<String> names,List<? extends Set<String>> memberships)
    {
        super(new BorderLayout(0,10));
        if(names.isEmpty() || names.size()>2 || names.size()!=memberships.size()) throw new IllegalArgumentException("Choose one or two items");
        JTextArea hint=new JTextArea(String.join("  /  ",names)+"\nSend a classification to review and add its owned items as colored Legend groups. Double-click also sends it.\nMembership describes a use; item state and activity requirements still apply.");
        hint.setLineWrap(true); hint.setWrapStyleWord(true); hint.setEditable(false); hint.setOpaque(false); add(hint,BorderLayout.NORTH);
        DefaultListModel<Row> rows=new DefaultListModel<>();
        Set<String> shared=new TreeSet<>(memberships.get(0));
        if(memberships.size()==2) shared.retainAll(memberships.get(1));
        for(String value:shared) rows.addElement(new Row(value,names.size()==1?"Item":"Both items"));
        if(names.size()==2) for(int i=0;i<2;i++) for(String value:new TreeSet<>(memberships.get(i)))
            if(!shared.contains(value)) rows.addElement(new Row(value,"Only "+names.get(i)));
        list=new JList<>(rows); list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); list.setVisibleRowCount(16);
        list.setCellRenderer((view,row,index,selected,focus) -> {
            JPanel cell=new JPanel(new GridLayout(0,1)); cell.setBorder(BorderFactory.createEmptyBorder(5,6,5,6));
            cell.setBackground(selected?view.getSelectionBackground():view.getBackground());
            for(String text:new String[]{row.membership,row.owner}) {
                JLabel label=new JLabel(text); label.setForeground(selected?view.getSelectionForeground():view.getForeground()); cell.add(label);
            }
            return cell;
        });
        add(new JScrollPane(list),BorderLayout.CENTER);
        JPanel buttons=new JPanel(new GridLayout(0,1,0,5));
        buttons.add(send); buttons.add(highlight);
        buttons.add(new JLabel(rows.isEmpty()?"No recorded memberships. You can still highlight the selected items.":"Highlighting is temporary. Use Legend → Clear highlight to clear it."));
        add(buttons,BorderLayout.SOUTH); send.setEnabled(false);
        list.addListSelectionListener(e -> send.setEnabled(list.getSelectedValue()!=null && !list.getSelectedValue().membership.startsWith("Review: ")));
        send.addActionListener(e -> { Row row=list.getSelectedValue(); if(row!=null) { membership=row.membership; filter=filterText(membership); close(); } });
        highlight.addActionListener(e -> { highlightRequested=true; close(); });
        list.addMouseListener(new MouseAdapter() { @Override public void mouseClicked(MouseEvent e) {
            int i=list.locationToIndex(e.getPoint());
            if(e.getClickCount()==2 && i>=0 && list.getCellBounds(i,i).contains(e.getPoint())) send.doClick();
        }});
        list.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,0),"send");
        list.getActionMap().put("send",new AbstractAction() { public void actionPerformed(ActionEvent e) { send.doClick(); } });
        setPreferredSize(new Dimension(700,460));
    }
    static String filterText(String membership)
    {
        int prefix=membership.indexOf(": ");
        String text=prefix<0?membership:membership.substring(prefix+2);
        if(membership.startsWith("Preset: ")) { int slash=text.indexOf(" / "); if(slash>=0) text=text.substring(slash+3); }
        else if(membership.startsWith("Wiki setup: ")) text=text.replace(" / "," — ").replace(" — state needs review","");
        else if(membership.startsWith("Custom preset: ")) text=text.replace(" / "," ");
        return text;
    }
    private void close() { Window window=SwingUtilities.getWindowAncestor(this); if(window instanceof JDialog) window.dispose(); }
}
