package com.windowmarkers;

import java.awt.*;
import java.util.List;
import javax.swing.*;
import javax.swing.event.*;

final class CatalogueBrowser
{
    static BankCatalogue.Choice choose(Component parent,List<BankCatalogue.Choice> choices,String initial)
    {
        JPanel panel=new JPanel(new BorderLayout(0,8)); JTextField search=new JTextField(initial);
        JComboBox<String> kind=new JComboBox<>(new String[]{"Everything","Categories","Activities","Presets","Custom presets","Saved groups"});
        BankCatalogue.Kind[] kinds={null,BankCatalogue.Kind.CATEGORY,BankCatalogue.Kind.ACTIVITY,BankCatalogue.Kind.PRESET,BankCatalogue.Kind.CUSTOM_PRESET,BankCatalogue.Kind.SAVED_GROUP};
        JPanel top=new JPanel(new BorderLayout(6,0)); top.add(search,BorderLayout.CENTER); top.add(kind,BorderLayout.EAST); panel.add(top,BorderLayout.NORTH);
        DefaultListModel<BankCatalogue.Choice> rows=new DefaultListModel<>(); JList<BankCatalogue.Choice> list=new JList<>(rows);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); list.setVisibleRowCount(14);
        boolean[] chosen={false};
        Runnable activate=() -> {
            if(list.getSelectedValue()==null) return;
            chosen[0]=true;
            Window window=SwingUtilities.getWindowAncestor(panel); if(window instanceof JDialog) window.dispose();
        };
        list.addMouseListener(new java.awt.event.MouseAdapter() { @Override public void mouseClicked(java.awt.event.MouseEvent e) {
            int index=list.locationToIndex(e.getPoint());
            if(e.getClickCount()==2 && index>=0 && list.getCellBounds(index,index).contains(e.getPoint())) activate.run();
        }});
        list.getInputMap().put(KeyStroke.getKeyStroke("ENTER"),"choose");
        list.getActionMap().put("choose",new AbstractAction() { public void actionPerformed(java.awt.event.ActionEvent e) { activate.run(); } });
        list.addListSelectionListener(e -> { BankCatalogue.Choice value=list.getSelectedValue(); list.setToolTipText(value==null?null:value.name); });
        panel.add(new JScrollPane(list),BorderLayout.CENTER);
        JPanel footer=new MarkerForm();
        footer.add(new JLabel("<html>Type to search; approximate spellings and CoX/ToB/ToA work.<br>Choose a result to continue.<br>Manage saved views in Legend → Saved views.</html>"));
        panel.add(footer,BorderLayout.SOUTH);
        Runnable refresh=() -> {
            rows.clear(); int selectedKind=kind.getSelectedIndex();
            List<BankCatalogue.Choice> filtered=new java.util.ArrayList<>();
            for(BankCatalogue.Choice choice:choices) if(selectedKind==0 || choice.kind==kinds[selectedKind]) filtered.add(choice);
            for(BankCatalogue.Choice choice:BankCatalogue.search(filtered,search.getText(),150)) rows.addElement(choice);
        };
        javax.swing.Timer timer=new javax.swing.Timer(150,e -> refresh.run()); timer.setRepeats(false);
        search.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { timer.restart(); } public void removeUpdate(DocumentEvent e) { timer.restart(); } public void changedUpdate(DocumentEvent e) { timer.restart(); }
        });
        kind.addActionListener(e -> refresh.run()); refresh.run();
        try { boolean accepted=MarkerDialogs.confirm(parent,panel,"Categories and presets",760,540); return accepted || chosen[0]?list.getSelectedValue():null; }
        finally { timer.stop(); }
    }
}
