package com.windowmarkers;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.event.*;

/** Searchable real-item palette; selection is by exact ID, including same-name variants. */
final class ItemPalette extends JPanel
{
    private final JTextField query=new JTextField();
    private final JCheckBox bankOnly=new JCheckBox("Only items in this bank",true);
    private final DefaultListModel<BankItemSearch.Choice> results=new DefaultListModel<>(), selected=new DefaultListModel<>();
    private final JList<BankItemSearch.Choice> tiles=new JList<>(results), picked=new JList<>(selected);
    private final JLabel hint=new JLabel("Search by name; select pictures to add items.");
    private final javax.swing.Timer debounce;
    private volatile int revision;
    private volatile boolean closed;
    private final int limit;
    ItemPalette(BankItemSearch search,Set<Integer> bank,List<BankItemSearch.Choice> initial,int limit)
    {
        super(new BorderLayout(8,8)); this.limit=limit;
        JPanel header=new JPanel(new BorderLayout(0,4));
        query.setToolTipText("Item name, including approximate spelling. Same-name variants retain separate IDs.");
        header.add(query,BorderLayout.NORTH); header.add(bankOnly,BorderLayout.SOUTH); add(header,BorderLayout.NORTH);
        for(BankItemSearch.Choice choice:initial) selected.addElement(choice);
        tiles.setLayoutOrientation(JList.HORIZONTAL_WRAP); tiles.setVisibleRowCount(-1);
        tiles.setFixedCellWidth(125); tiles.setFixedCellHeight(68);
        tiles.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        tiles.setCellRenderer((list,choice,index,isSelected,focus) -> {
            JLabel label=new JLabel(choice.name,choice.image==null?null:new ImageIcon(choice.image),SwingConstants.CENTER);
            label.setVerticalTextPosition(SwingConstants.BOTTOM); label.setHorizontalTextPosition(SwingConstants.CENTER);
            label.setOpaque(true); label.setBackground(isSelected?list.getSelectionBackground():list.getBackground());
            label.setForeground(isSelected?list.getSelectionForeground():list.getForeground());
            label.setBorder(BorderFactory.createEmptyBorder(4,3,4,3)); label.setToolTipText(choice.toString()); return label;
        });
        JScrollPane options=new JScrollPane(tiles); options.setPreferredSize(new Dimension(520,210));
        JPanel middle=new JPanel(new BorderLayout(0,5)); middle.add(options,BorderLayout.CENTER);
        JButton add=new JButton("Add selected items ↓"); add.addActionListener(e -> addSelected()); middle.add(add,BorderLayout.SOUTH);
        tiles.addMouseListener(new MouseAdapter() { @Override public void mouseClicked(MouseEvent e) { if(e.getClickCount()==2) addSelected(); } });
        tiles.getInputMap().put(KeyStroke.getKeyStroke("ENTER"),"add");
        tiles.getActionMap().put("add",new AbstractAction() { public void actionPerformed(ActionEvent e) { addSelected(); } });
        add(middle,BorderLayout.CENTER);
        picked.setVisibleRowCount(3); picked.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        JPanel footer=new JPanel(new BorderLayout(0,4)); footer.add(new JScrollPane(picked),BorderLayout.CENTER);
        JButton remove=new JButton("Remove selected items"); remove.addActionListener(e -> {
            for(BankItemSearch.Choice choice:picked.getSelectedValuesList()) selected.removeElement(choice); count();
        });
        JPanel bottom=new JPanel(new GridLayout(0,1,0,4)); bottom.add(remove); bottom.add(hint); footer.add(bottom,BorderLayout.SOUTH); add(footer,BorderLayout.SOUTH);
        debounce=new javax.swing.Timer(180,e -> {
            int version=revision; String text=query.getText();
            if(!bankOnly.isSelected() && text.trim().length()<2) { hint.setText("Type at least two letters to search all items."); return; }
            hint.setText("Searching…");
            search.search(text,bankOnly.isSelected()?bank:null,()->!closed && revision==version,matches -> {
                results.clear();
                for(BankItemSearch.Choice choice:matches) {
                    results.addElement(choice);
                    if(choice.image instanceof net.runelite.client.util.AsyncBufferedImage)
                        ((net.runelite.client.util.AsyncBufferedImage)choice.image).onLoaded(() -> SwingUtilities.invokeLater(() -> { if(!closed) tiles.repaint(); }));
                }
                hint.setText(matches.size()+" matches shown · "+selected.size()+" selected"+(matches.size()==80?" · refine search":""));
            });
        }); debounce.setRepeats(false);
        query.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { refresh(); } public void removeUpdate(DocumentEvent e) { refresh(); } public void changedUpdate(DocumentEvent e) { refresh(); }
        });
        bankOnly.addActionListener(e -> refresh()); debounce.start();
    }
    private void refresh() { revision++; results.clear(); debounce.restart(); }
    private void count() { hint.setText(selected.size()+" selected"+(limit==2?" (up to two for comparison)":"")); }
    private void addSelected()
    {
        for(BankItemSearch.Choice choice:tiles.getSelectedValuesList())
        {
            boolean exists=false; for(int i=0;i<selected.size();i++) if(selected.get(i).id==choice.id) exists=true;
            if(!exists && selected.size()<limit) selected.addElement(choice);
        }
        count();
    }
    int[] ids() { return Collections.list(selected.elements()).stream().mapToInt(c -> c.id).toArray(); }
    void close() { closed=true; revision++; debounce.stop(); }
}
