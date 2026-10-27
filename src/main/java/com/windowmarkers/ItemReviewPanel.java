package com.windowmarkers;

import java.awt.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.runelite.client.util.AsyncBufferedImage;

/** Read-only reasons, separate from the explicit custom-view editor. */
final class ItemReviewPanel extends JPanel
{
    final JTextField search=new JTextField();
    final DefaultListModel<BankItemSearch.Choice> rows=new DefaultListModel<>();
    final JList<BankItemSearch.Choice> list=new JList<BankItemSearch.Choice>(rows) {
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
    };
    final JTextArea detail=new JTextArea();
    private final List<BankItemSearch.Choice> choices;
    private final Map<Integer,ItemCatalogueIndex.Rank> reasons;
    private volatile boolean closed;
    ItemReviewPanel(List<BankItemSearch.Choice> choices,Map<Integer,ItemCatalogueIndex.Rank> reasons)
    {
        super(new BorderLayout(0,8));this.choices=new ArrayList<>(choices);this.reasons=new HashMap<>(reasons);
        this.choices.sort(Comparator.comparing(c->c.name));
        JPanel heading=new MarkerForm();
        heading.add(new JLabel("<html>Junk is a review list, not a claim that an item has no use.<br>Amber: a niche or unverified use. Grey: ordinary cleanup review.<br>Sell candidates currently cover reviewed ordinary gathering tools only.</html>"));
        heading.add(new JLabel("Find an item or use"));heading.add(search);add(heading,BorderLayout.NORTH);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);list.setFixedCellHeight(52);
        list.setCellRenderer((owner,choice,index,selected,focus)-> {
            JPanel row=new JPanel(new BorderLayout(8,0));row.setOpaque(true);
            row.setBackground(selected?owner.getSelectionBackground():owner.getBackground());
            row.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0,3,0,0,BankCleanup.colour(reasons.get(choice.id))),BorderFactory.createEmptyBorder(4,5,4,4)));
            JLabel icon=new JLabel(choice.image==null?null:new ImageIcon(choice.image));icon.setPreferredSize(new Dimension(36,32));row.add(icon,BorderLayout.WEST);
            JPanel text=new JPanel(new GridLayout(2,1));text.setOpaque(false);
            JLabel name=new JLabel(choice.name),reason=new JLabel(shortReason(choice));
            name.setForeground(selected?owner.getSelectionForeground():owner.getForeground());reason.setForeground(name.getForeground());
            text.add(name);text.add(reason);row.add(text,BorderLayout.CENTER);row.setToolTipText(shortReason(choice));return row;
        });
        detail.setEditable(false);detail.setLineWrap(true);detail.setWrapStyleWord(true);detail.setRows(7);
        detail.setFont(UIManager.getFont("Label.font"));detail.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
        JScrollPane itemScroll=new JScrollPane(list),detailScroll=new JScrollPane(detail);
        itemScroll.setMinimumSize(new Dimension(160,100));detailScroll.setMinimumSize(new Dimension(160,80));
        JSplitPane split=new JSplitPane(JSplitPane.VERTICAL_SPLIT,itemScroll,detailScroll);split.setResizeWeight(0.65);split.setBorder(null);
        add(split,BorderLayout.CENTER);
        list.addListSelectionListener(e->{ if(!e.getValueIsAdjusting()) {
            BankItemSearch.Choice choice=list.getSelectedValue();
            detail.setText(choice==null?"Select an item to read its specific uses and evidence.":choice.name+" (#"+choice.id+")\n\n"+reasons.get(choice.id).explanation);
            detail.setCaretPosition(0);
        }});
        search.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filter(); }
            public void removeUpdate(DocumentEvent e) { filter(); }
            public void changedUpdate(DocumentEvent e) { filter(); }
        });
        for(BankItemSearch.Choice choice:choices) if(choice.image instanceof AsyncBufferedImage)
            ((AsyncBufferedImage)choice.image).onLoaded(()->SwingUtilities.invokeLater(()->{ if(!closed) list.repaint(); }));
        filter();
    }
    private String shortReason(BankItemSearch.Choice choice)
    {
        String[] lines=reasons.get(choice.id).explanation.split("\n");
        return lines.length>1 && lines[0].equalsIgnoreCase(choice.name)?lines[1]:lines[0];
    }
    private void filter()
    {
        String query=SearchText.normalize(search.getText());rows.clear();
        for(BankItemSearch.Choice choice:choices) if(reasons.containsKey(choice.id)
            && SearchText.normalize(choice.name+" "+reasons.get(choice.id).explanation).contains(query)) rows.addElement(choice);
        if(!rows.isEmpty()) list.setSelectedIndex(0);
    }
    void close() { closed=true; }
}
