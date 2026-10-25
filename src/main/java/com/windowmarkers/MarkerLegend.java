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
    interface Actions
    {
        void edit(Marker marker);
        void items(Marker marker);
        void delete(List<Marker> markers);
        void clear(List<Marker> expected);
        void browse(String query);
        default void choose(BankCatalogue.Choice choice) { browse(choice.name); }
        void inspect();
        void reorder(List<Marker> visible,List<Marker> selected,int index);
        void transform(Marker marker,boolean resize);
        default void clearHighlight() { }
    }
    final JTextField search=new JTextField();
    final JComboBox<String> scope=new JComboBox<>(new String[]{"Current bank tab","All saved groups"});
    final JComboBox<String> preset=new JComboBox<>();
    final DefaultListModel<Entry> rows=new DefaultListModel<>();
    final JList<Entry> list=new JList<>(rows);
    private final DefaultListModel<BankCatalogue.Choice> suggestions=new DefaultListModel<>();
    private final JList<BankCatalogue.Choice> suggestionList=new JList<>(suggestions);
    private final JScrollPane suggestionScroll=new JScrollPane(suggestionList);
    private final JButton edit=new JButton("Label / appearance"),remove=new JButton("Delete selected");
    private final JButton move=new JButton("Move area"),resize=new JButton("Resize area"),items=new JButton("Edit group items…");
    private final Consumer<Marker> hover,pin;
    private final Actions actions;
    private List<Entry> tabEntries=java.util.Collections.emptyList(),allEntries=java.util.Collections.emptyList();
    private List<BankCatalogue.Choice> choices=java.util.Collections.emptyList();
    private boolean refreshing;
    private long revision;
    private final javax.swing.Timer timer;
    MarkerLegend(Consumer<Marker> hover,Consumer<Marker> pin) { this(hover,pin,null); }
    MarkerLegend(Consumer<Marker> hover,Consumer<Marker> pin,Actions actions)
    {
        super(new BorderLayout(0,8)); this.hover=hover; this.pin=pin; this.actions=actions;
        JPanel header=new MarkerForm();
        header.add(new JLabel("Search categories and groups")); header.add(search);
        search.setToolTipText("Approximate spelling works. Suggestions search all categories, including groups not added yet.");
        suggestionList.setVisibleRowCount(3); suggestionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        suggestionScroll.setPreferredSize(new Dimension(210,75)); suggestionScroll.setVisible(false); header.add(suggestionScroll);
        suggestionList.addMouseListener(new MouseAdapter() { @Override public void mouseClicked(MouseEvent e) {
            int i=suggestionList.locationToIndex(e.getPoint());
            if(i>=0 && suggestionList.getCellBounds(i,i).contains(e.getPoint()) && actions!=null) actions.choose(suggestions.get(i));
        }});
        suggestionList.getInputMap().put(KeyStroke.getKeyStroke("ENTER"),"browse");
        suggestionList.getActionMap().put("browse",new AbstractAction() { public void actionPerformed(ActionEvent e) {
            if(actions!=null && suggestionList.getSelectedValue()!=null) actions.choose(suggestionList.getSelectedValue());
        }});
        header.add(scope); header.add(preset); preset.addItem("All presets");
        scope.setToolTipText("Current bank tab follows tab changes. All saved groups includes absent items and empty area markers.");
        JButton browse=new JButton("Categories / presets…"),inspect=new JButton("Item lookup / compare…");
        browse.addActionListener(e -> { if(actions!=null) actions.browse(search.getText()); });
        inspect.addActionListener(e -> { if(actions!=null) actions.inspect(); }); header.add(browse); header.add(inspect);
        add(header,BorderLayout.NORTH);
        timer=new javax.swing.Timer(150,e -> filter()); timer.setRepeats(false);
        search.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { timer.restart(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { timer.restart(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { timer.restart(); }
        });
        scope.addActionListener(e -> filter()); preset.addActionListener(e -> { if(!refreshing) filter(); });
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        list.setCellRenderer((view,entry,index,selected,focus) -> {
            JPanel row=new JPanel(new BorderLayout(6,0)); row.setBorder(BorderFactory.createEmptyBorder(5,4,5,4));
            row.setBackground(selected ? view.getSelectionBackground() : view.getBackground());
            JPanel swatch=new JPanel(); swatch.setBackground(entry.color); swatch.setPreferredSize(new Dimension(8,28));
            JPanel text=new JPanel(new GridLayout(0,1)); text.setOpaque(false);
            JLabel name=new JLabel(entry.name),detail=new JLabel(entry.detail);
            name.setForeground(selected ? view.getSelectionForeground() : view.getForeground());
            detail.setForeground(name.getForeground()); detail.setFont(detail.getFont().deriveFont(10f));
            text.add(name); text.add(detail); row.add(swatch,BorderLayout.WEST); row.add(text,BorderLayout.CENTER); return row;
        });
        list.addMouseMotionListener(new MouseMotionAdapter() { @Override public void mouseMoved(MouseEvent e) {
            int i=list.locationToIndex(e.getPoint()); Entry entry=i>=0 && list.getCellBounds(i,i).contains(e.getPoint())?rows.get(i):null;
            hover.accept(entry==null?null:entry.marker); list.setToolTipText(entry==null?null:entry.name+" — "+entry.detail);
        }});
        list.addMouseListener(new MouseAdapter() { @Override public void mouseExited(MouseEvent e) { hover.accept(null); } });
        list.addListSelectionListener(e -> { if(!e.getValueIsAdjusting()) {
            List<Marker> selected=selected(); Marker single=selected.size()==1?selected.get(0):null;
            pin.accept(single); edit.setEnabled(single!=null); remove.setEnabled(!selected.isEmpty());
            items.setEnabled(single!=null && single.linked());
            move.setEnabled(single!=null && !single.linked()); resize.setEnabled(move.isEnabled());
        }});
        list.setVisibleRowCount(5);
        list.setDropMode(DropMode.INSERT); list.setTransferHandler(new GroupTransfer());
        if(!GraphicsEnvironment.isHeadless()) list.setDragEnabled(true);
        add(new JScrollPane(list),BorderLayout.CENTER);
        JPanel footer=new MarkerForm();
        JPanel manage=new JPanel(new GridLayout(0,2,4,4));
        edit.addActionListener(e -> { if(actions!=null && selected().size()==1) actions.edit(selected().get(0)); });
        items.addActionListener(e -> { if(actions!=null && selected().size()==1) actions.items(selected().get(0)); });
        footer.add(items);
        remove.addActionListener(e -> { if(actions!=null) actions.delete(selected()); });
        move.addActionListener(e -> { if(actions!=null && selected().size()==1) actions.transform(selected().get(0),false); });
        resize.addActionListener(e -> { if(actions!=null && selected().size()==1) actions.transform(selected().get(0),true); });
        JButton up=new JButton("Move up"),down=new JButton("Move down"),clearAll=new JButton("Clear all"),clear=new JButton("Unpin");
        clearAll.setToolTipText("Delete every group in this layout. Undo restores them."); clear.setToolTipText("Clear the pinned group highlight.");
        up.addActionListener(e -> shift(-1)); down.addActionListener(e -> shift(1));
        clearAll.addActionListener(e -> {
            List<Marker> expected=new java.util.ArrayList<>(); for(Entry entry:allEntries) expected.add(entry.marker);
            if(actions!=null) actions.clear(expected);
        });
        clear.addActionListener(e -> { list.clearSelection(); hover.accept(null); pin.accept(null); if(actions!=null) actions.clearHighlight(); });
        // Full-width actions remain readable in RuneLite's narrow sidebar.
        footer.add(edit); footer.add(remove);
        manage.add(move); manage.add(resize); manage.add(up); manage.add(down); manage.add(clearAll); manage.add(clear);
        for(Component button:manage.getComponents()) ((JButton)button).setMargin(new Insets(3,2,3,2));
        footer.add(manage); footer.add(new JLabel("<html>Drag to reorder groups. Shift + arrows<br>or Ctrl + click selects several groups.</html>")); add(footer,BorderLayout.SOUTH);
        items.setEnabled(false); edit.setEnabled(false); remove.setEnabled(false); move.setEnabled(false); resize.setEnabled(false);
        list.getInputMap().put(KeyStroke.getKeyStroke("DELETE"),"delete");
        list.getActionMap().put("delete",new AbstractAction() { public void actionPerformed(ActionEvent e) { remove.doClick(); } });
    }
    List<Marker> selected()
    { List<Marker> result=new java.util.ArrayList<>(); for(Entry entry:list.getSelectedValuesList()) result.add(entry.marker); return result; }
    private List<Marker> visible()
    { List<Marker> result=new java.util.ArrayList<>(); for(int i=0;i<rows.size();i++) result.add(rows.get(i).marker); return result; }
    private void shift(int direction)
    {
        int[] selected=list.getSelectedIndices(); if(selected.length==0 || actions==null) return;
        int index=direction<0?Math.max(0,selected[0]-1):Math.min(rows.size(),selected[selected.length-1]+2);
        actions.reorder(visible(),selected(),index);
    }
    void replace(List<Entry> entries) { replace(entries,entries); }
    void replace(List<Entry> tab,List<Entry> all)
    {
        tabEntries=tab; allEntries=all;
        String selected=(String)preset.getSelectedItem(); refreshing=true; preset.removeAllItems(); preset.addItem("All presets");
        java.util.Set<String> names=new java.util.TreeSet<>();
        for(Entry entry:all) names.add(entry.marker.preset==null?"Custom / older groups":entry.marker.preset);
        for(String name:names) preset.addItem(name);
        if(names.contains(selected)) preset.setSelectedItem(selected); refreshing=false; filter();
    }
    void catalogue(List<BankCatalogue.Choice> choices) { this.choices=choices; filter(); }
    void showGroups(List<Marker> groups)
    {
        search.setText(""); preset.setSelectedIndex(0);
        if(scope.getSelectedIndex()==0 && tabEntries.stream().noneMatch(e -> groups.contains(e.marker))) scope.setSelectedIndex(1);
        filter(); list.clearSelection();
        for(int i=0;i<rows.size();i++) if(groups.contains(rows.get(i).marker)) list.addSelectionInterval(i,i);
        if(list.getSelectedIndex()>=0) list.ensureIndexIsVisible(list.getSelectedIndex());
    }
    void resetContext() { search.setText(""); preset.setSelectedIndex(0); list.clearSelection(); }
    void close() { timer.stop(); }
    void filter()
    {
        revision++; List<Marker> selected=selected(); list.setValueIsAdjusting(true); rows.clear();
        List<Entry> base=scope.getSelectedIndex()==0?tabEntries:allEntries; String needle=search.getText();
        String presetName=(String)preset.getSelectedItem();
        for(Entry entry:base)
        {
            String groupPreset=entry.marker.preset==null?"Custom / older groups":entry.marker.preset;
            if(preset.getSelectedIndex()>0 && !groupPreset.equals(presetName)) continue;
            if(SearchText.score(entry.name+" "+entry.marker.category+" "+groupPreset+" "+BankCatalogue.aliases(groupPreset),needle)==Integer.MAX_VALUE) continue;
            rows.addElement(entry);
        }
        for(int i=0;i<rows.size();i++) if(selected.contains(rows.get(i).marker)) list.addSelectionInterval(i,i);
        list.setValueIsAdjusting(false);
        suggestions.clear();
        if(!needle.trim().isEmpty()) for(BankCatalogue.Choice choice:BankCatalogue.search(choices,needle,4)) suggestions.addElement(choice);
        suggestionScroll.setVisible(!suggestions.isEmpty()); revalidate(); hover.accept(null);
    }
    private final class GroupTransfer extends TransferHandler
    {
        private final java.awt.datatransfer.DataFlavor flavor=new java.awt.datatransfer.DataFlavor(
            java.awt.datatransfer.DataFlavor.javaJVMLocalObjectMimeType+";class=java.lang.Object","Legend groups");
        @Override public int getSourceActions(JComponent c) { return MOVE; }
        @Override protected java.awt.datatransfer.Transferable createTransferable(JComponent c)
        {
            Drag drag=new Drag(MarkerLegend.this,revision,visible(),selected());
            return new java.awt.datatransfer.Transferable() {
                public java.awt.datatransfer.DataFlavor[] getTransferDataFlavors() { return new java.awt.datatransfer.DataFlavor[]{flavor}; }
                public boolean isDataFlavorSupported(java.awt.datatransfer.DataFlavor f) { return flavor.equals(f); }
                public Object getTransferData(java.awt.datatransfer.DataFlavor f) throws java.awt.datatransfer.UnsupportedFlavorException {
                    if(!isDataFlavorSupported(f)) throw new java.awt.datatransfer.UnsupportedFlavorException(f); return drag;
                }
            };
        }
        @Override public boolean canImport(TransferSupport support)
        { return actions!=null && support.isDrop() && support.getComponent()==list && support.isDataFlavorSupported(flavor); }
        @Override public boolean importData(TransferSupport support)
        {
            if(!canImport(support)) return false;
            try {
                Drag drag=(Drag)support.getTransferable().getTransferData(flavor);
                if(drag.owner!=MarkerLegend.this || drag.revision!=revision) return false;
                actions.reorder(drag.visible,drag.selected,((JList.DropLocation)support.getDropLocation()).getIndex()); return true;
            } catch(java.awt.datatransfer.UnsupportedFlavorException | java.io.IOException ex) { return false; }
        }
    }
    private static final class Drag
    {
        final MarkerLegend owner; final long revision; final List<Marker> visible,selected;
        Drag(MarkerLegend owner,long revision,List<Marker> visible,List<Marker> selected)
        { this.owner=owner; this.revision=revision; this.visible=visible; this.selected=selected; }
    }
}
