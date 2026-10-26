package com.windowmarkers;

import java.awt.Component;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/** Search activities first, then inspect one source setup and its conditions. */
final class ActivitySetupPicker
{
    static ActivitySetups.Setup choose(Component parent)
    { return choose(parent,false); }
    static ActivitySetups.Setup choose(Component parent,boolean ge)
    {
        JTextField search=new JTextField(32);
        JComboBox<String> activities=new JComboBox<>();
        JComboBox<ActivitySetups.Setup> setups=new JComboBox<>();
        activities.setMaximumRowCount(12); setups.setMaximumRowCount(12);
        JTextArea notes=new JTextArea(7,48);
        notes.setEditable(false); notes.setLineWrap(true); notes.setWrapStyleWord(true);
        JPanel panel=new MarkerForm();
        panel.add(new JLabel("Search activity")); panel.add(search);
        panel.add(activities); panel.add(new JLabel("Choose a setup")); panel.add(setups);
        panel.add(new JLabel(ge
            ? "<html>Bank and GE tiers follow one equipment table and its conditions.<br>Selecting it narrows searches for that activity.<br>Example tables provide matches without tier colours.</html>"
            : "<html>One Wiki table or example at a time.<br>Review its conditions; quantities and a complete loadout are not assumed.<br>These setups keep all alternatives and start unchecked.</html>"));
        panel.add(notes);
        JButton source=new JButton("Open this Wiki setup source");
        source.addActionListener(e -> {
            ActivitySetups.Setup setup=(ActivitySetups.Setup)setups.getSelectedItem();
            if (setup!=null) { net.runelite.client.util.LinkBrowser.browse(setup.source); }
        });
        panel.add(source);
        setups.addActionListener(e -> {
            ActivitySetups.Setup setup=(ActivitySetups.Setup)setups.getSelectedItem();
            notes.setText(setup==null ? "No matching setup." : setup.notes.isEmpty()
                ? "Check the linked guide for skill, quest and encounter requirements." : setup.notes);
            notes.setCaretPosition(0); source.setEnabled(setup!=null);
        });
        activities.addActionListener(e -> {
            setups.removeAllItems();
            for (ActivitySetups.Setup setup:ActivitySetups.forActivity((String)activities.getSelectedItem()))
            { setups.addItem(setup); }
        });
        Runnable refresh=() -> {
            Object previous=activities.getSelectedItem();
            activities.removeAllItems();
            for (String activity:ActivitySetups.activities(search.getText())) { activities.addItem(activity); }
            if (previous!=null && ActivitySetups.activities(search.getText()).contains(previous))
            { activities.setSelectedItem(previous); }
        };
        search.getDocument().addDocumentListener(new DocumentListener()
        {
            public void insertUpdate(DocumentEvent e) { refresh.run(); }
            public void removeUpdate(DocumentEvent e) { refresh.run(); }
            public void changedUpdate(DocumentEvent e) { refresh.run(); }
        });
        refresh.run();
        if (!MarkerDialogs.confirm(parent,panel,ge?"Choose bank / GE guide":"Choose a Wiki activity setup",680,520)) { return null; }
        return (ActivitySetups.Setup)setups.getSelectedItem();
    }
}
