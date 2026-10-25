package com.windowmarkers;

import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.swing.*;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.JavaScriptCallback;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetType;

/** The bank dropdown and sidebar share one catalogue and matching implementation. */
final class BankCataloguePicker
{
    @Inject Client client;
    private Widget parent,button;
    private volatile JPopupMenu popup;
    private volatile long context;

    void update(boolean enabled,Runnable open)
    {
        Widget frame=client.getWidget(InterfaceID.Bankmain.FRAME),close=null;
        if(frame!=null && frame.getChildren()!=null) for(Widget child:frame.getChildren())
            if(child!=null && !child.isHidden() && child.getActions()!=null && Arrays.asList(child.getActions()).contains("Close")) { close=child; break; }
        if(!enabled || frame==null || frame.isHidden() || close==null) { clear(); return; }
        if(parent!=frame || button==null || frame.getChildren()==null || !Arrays.asList(frame.getChildren()).contains(button)) {
            clear(); parent=frame; button=frame.createChild(-1,WidgetType.TEXT);
            button.setOriginalWidth(20); button.setOriginalHeight(20); button.setFontId(494);
            button.setText("v"); button.setTextColor(0x70D8E8); button.setTextShadowed(true);
            button.setAction(0,"Search categories and presets"); button.setHasListener(true);
            button.setOnOpListener((JavaScriptCallback)e -> open.run());
        }
        Rectangle bounds=frame.getBounds(),exit=close.getBounds();
        Rectangle position=new Rectangle(exit.x-bounds.x-48,exit.y-bounds.y+(exit.height-20)/2,20,20);
        if(!new Rectangle(0,0,bounds.width,bounds.height).contains(position)) { clear(); return; }
        button.setOriginalX(position.x); button.setOriginalY(position.y);
        button.setHidden(false); button.revalidate();
    }

    void show(Component canvas,List<BankCatalogue.Choice> choices,boolean canSave,Consumer<BankCatalogue.Choice> choose,Runnable save,Runnable closeView)
    {
        // Called on the client thread; capture widget coordinates here, Swing on EDT.
        if(button==null || button.isHidden() || canvas==null) return;
        Rectangle anchor=button.getBounds();
        long token=++context;
        List<BankCatalogue.Choice> snapshot=new ArrayList<>(choices);
        SwingUtilities.invokeLater(() -> {
            if(token!=context || !canvas.isShowing()) return;
            close();
            PickerPanel panel=new PickerPanel(snapshot,choice -> { close(); choose.accept(choice); },
                () -> { close(); save.run(); },() -> { close(); closeView.run(); },canSave);
            JPopupMenu menu=new JPopupMenu(); popup=menu; menu.setLightWeightPopupEnabled(false);
            menu.setLayout(new BorderLayout()); menu.add(panel,BorderLayout.CENTER);
            menu.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
                public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e) { }
                public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e) { panel.stop(); if(popup==menu) popup=null; }
                public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e) { panel.stop(); }
            });
            int width=Math.min(400,Math.max(240,canvas.getWidth()-16));
            panel.setPreferredSize(new Dimension(width,Math.min(350,Math.max(200,canvas.getHeight()-anchor.y-28))));
            menu.show(canvas,Math.max(0,Math.min(anchor.x-width+20,canvas.getWidth()-width)),anchor.y+20);
            panel.search.requestFocusInWindow();
        });
    }
    void close() { JPopupMenu current=popup; popup=null; if(current!=null) current.setVisible(false); }
    void clear() {
        context++;
        if(button!=null) { button.setHidden(true); button.clearActions(); button.setOnOpListener((Object[])null); button.setHasListener(false); }
        button=null; parent=null;
        JPopupMenu old=popup;
        if(old!=null) SwingUtilities.invokeLater(() -> { old.setVisible(false); if(popup==old) popup=null; });
    }

    static final class PickerPanel extends JPanel
    {
        final JTextField search=new JTextField();
        final DefaultListModel<BankCatalogue.Choice> rows=new DefaultListModel<>();
        final JList<BankCatalogue.Choice> list=new JList<>(rows);
        final javax.swing.Timer timer;
        long revision;
        boolean closed;
        PickerPanel(List<BankCatalogue.Choice> choices,Consumer<BankCatalogue.Choice> choose,Runnable save,Runnable closeView,boolean canSave)
        {
            super(new BorderLayout(6,6)); setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
            search.setToolTipText("Search categories, activities and saved presets. Approximate spelling works.");
            JPanel header=new JPanel(new BorderLayout(0,4)); header.add(new JLabel("Search categories & presets"),BorderLayout.NORTH);
            header.add(search,BorderLayout.CENTER); add(header,BorderLayout.NORTH); add(new JScrollPane(list),BorderLayout.CENTER);
            list.setCellRenderer(new DefaultListCellRenderer() {
                @Override public Component getListCellRendererComponent(JList<?> list,Object value,int index,boolean selected,boolean focus) {
                    JLabel label=(JLabel)super.getListCellRendererComponent(list,value,index,selected,focus);
                    label.setToolTipText(value==null?null:value.toString()); return label;
                }
            });
            list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); list.setVisibleRowCount(7);
            Runnable accept=() -> { if(list.getSelectedValue()!=null) choose.accept(list.getSelectedValue()); };
            JButton open=new JButton("Open selected"),saveButton=new JButton("Save current view…"),back=new JButton("Return to bank");
            saveButton.setEnabled(canSave); open.addActionListener(e -> accept.run());
            saveButton.addActionListener(e -> save.run()); back.addActionListener(e -> closeView.run());
            JPanel footer=new JPanel(new GridLayout(0,1,4,4)); footer.add(open); footer.add(saveButton); footer.add(back); add(footer,BorderLayout.SOUTH);
            list.addMouseListener(new MouseAdapter() { public void mouseClicked(MouseEvent e) {
                int i=list.locationToIndex(e.getPoint()); if(e.getClickCount()==2 && i>=0 && list.getCellBounds(i,i).contains(e.getPoint())) accept.run();
            }});
            search.addActionListener(e -> accept.run());
            list.getInputMap().put(KeyStroke.getKeyStroke("ENTER"),"open");
            list.getActionMap().put("open",new AbstractAction() { public void actionPerformed(ActionEvent e) { accept.run(); } });
            search.getInputMap().put(KeyStroke.getKeyStroke("DOWN"),"results");
            search.getActionMap().put("results",new AbstractAction() { public void actionPerformed(ActionEvent e) { if(!rows.isEmpty()) { list.requestFocusInWindow(); list.setSelectedIndex(0); } } });
            Runnable refresh=() -> {
                long token=++revision; String text=search.getText();
                new SwingWorker<List<BankCatalogue.Choice>,Void>() {
                    protected List<BankCatalogue.Choice> doInBackground() { return BankCatalogue.search(choices,text,80); }
                    protected void done() { if(closed || token!=revision) return; try { rows.clear(); for(var c:get()) rows.addElement(c); if(!rows.isEmpty()) list.setSelectedIndex(0); } catch(Exception ignored) { } }
                }.execute();
            };
            timer=new javax.swing.Timer(150,e -> refresh.run()); timer.setRepeats(false);
            search.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                private void changed() { revision++; rows.clear(); timer.restart(); }
                public void insertUpdate(javax.swing.event.DocumentEvent e) { changed(); }
                public void removeUpdate(javax.swing.event.DocumentEvent e) { changed(); }
                public void changedUpdate(javax.swing.event.DocumentEvent e) { changed(); }
            });
            refresh.run();
        }
        void stop() { closed=true; revision++; timer.stop(); }
    }
}
