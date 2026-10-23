package com.windowmarkers;

import java.awt.*;
import java.util.List;
import javax.swing.*;
import javax.swing.event.*;

final class CatalogueBrowser
{
    static BankCatalogue.Choice choose(Component parent,List<BankCatalogue.Choice> choices,String initial,java.util.function.Predicate<BankCatalogue.Choice> deletePreset)
    {
        JPanel panel=new JPanel(new BorderLayout(0,8)); JTextField search=new JTextField(initial);
        JComboBox<String> kind=new JComboBox<>(new String[]{"Everything","Categories","Presets","Wiki setups","Custom presets","Saved groups"});
        JPanel top=new JPanel(new BorderLayout(6,0)); top.add(search,BorderLayout.CENTER); top.add(kind,BorderLayout.EAST); panel.add(top,BorderLayout.NORTH);
        DefaultListModel<BankCatalogue.Choice> rows=new DefaultListModel<>(); JList<BankCatalogue.Choice> list=new JList<>(rows);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); list.setVisibleRowCount(14);
        JButton remove=new JButton("Delete saved preset…"); remove.setEnabled(false);
        list.addListSelectionListener(e -> { BankCatalogue.Choice value=list.getSelectedValue(); list.setToolTipText(value==null?null:value.name);
            remove.setEnabled(value!=null && value.kind==BankCatalogue.Kind.CUSTOM_PRESET); });
        panel.add(new JScrollPane(list),BorderLayout.CENTER);
        JPanel footer=new MarkerForm();
        footer.add(new JLabel("<html>Type to search; approximate spellings and CoX/ToB/ToA work.<br>Choose a result to review its items before adding groups.<br>Saved custom presets can be deleted here; existing groups stay.</html>"));
        footer.add(remove); panel.add(footer,BorderLayout.SOUTH);
        Runnable refresh=() -> {
            rows.clear(); int selectedKind=kind.getSelectedIndex();
            List<BankCatalogue.Choice> filtered=new java.util.ArrayList<>();
            for(BankCatalogue.Choice choice:choices) if(selectedKind==0 || choice.kind.ordinal()==selectedKind-1) filtered.add(choice);
            for(BankCatalogue.Choice choice:BankCatalogue.search(filtered,search.getText(),150)) rows.addElement(choice);
        };
        remove.addActionListener(e -> {
            BankCatalogue.Choice selected=list.getSelectedValue();
            if(selected!=null && selected.kind==BankCatalogue.Kind.CUSTOM_PRESET && deletePreset.test(selected)) {
                choices.remove(selected); refresh.run();
            }
        });
        javax.swing.Timer timer=new javax.swing.Timer(150,e -> refresh.run()); timer.setRepeats(false);
        search.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { timer.restart(); } public void removeUpdate(DocumentEvent e) { timer.restart(); } public void changedUpdate(DocumentEvent e) { timer.restart(); }
        });
        kind.addActionListener(e -> refresh.run()); refresh.run();
        try { return MarkerDialogs.confirm(parent,panel,"Categories and presets",760,540)?list.getSelectedValue():null; }
        finally { timer.stop(); }
    }
}
