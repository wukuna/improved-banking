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
import net.runelite.api.SpriteID;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.JavaScriptCallback;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetType;

/** The bank dropdown and sidebar share one catalogue and matching implementation. */
final class BankCataloguePicker
{
    @Inject Client client;
    private static final int WIDTH=96,HEIGHT=22,EDGE=6;
    private static final int[] SPRITES={SpriteID.BUTTON_CORNER_TOP_LEFT,SpriteID.BUTTON_EDGE_TOP,SpriteID.BUTTON_CORNER_TOP_RIGHT,
        SpriteID.BUTTON_EDGE_LEFT,SpriteID.BUTTON_MIDDLE,SpriteID.BUTTON_EDGE_RIGHT,
        SpriteID.BUTTON_CORNER_BOTTOM_LEFT,SpriteID.BUTTON_EDGE_BOTTOM,SpriteID.BUTTON_CORNER_BOTTOM_RIGHT};
    private Widget parent,button;
    private final BankHeaderTitle title=new BankHeaderTitle();
    private final List<Widget> background=new ArrayList<>();
    private volatile JPopupMenu popup;
    private volatile long context;

    void update(boolean enabled,Runnable open)
    {
        Widget frame=client.getWidget(InterfaceID.Bankmain.FRAME),close=null;
        if(frame!=null && frame.getChildren()!=null) for(Widget child:frame.getChildren())
            if(child!=null && !child.isHidden() && child.getActions()!=null && Arrays.asList(child.getActions()).contains("Close")) { close=child; break; }
        if(!enabled || frame==null || frame.isHidden() || close==null) { clear(); return; }
        List<Widget> children=Arrays.asList(frame.getChildren());
        if(parent!=frame || button==null || !children.contains(button) || !children.containsAll(background)) {
            clear(); parent=frame;
            // The native bank buttons use these nine tiled sprite pieces. Keep
            // sprite IDs so resource packs supply their normal button artwork.
            for(int sprite:SPRITES) {
                Widget part=frame.createChild(-1,WidgetType.GRAPHIC);
                part.setSpriteId(sprite);part.setSpriteTiling(true);background.add(part);
            }
            button=frame.createChild(-1,WidgetType.TEXT);
            button.setOriginalWidth(WIDTH); button.setOriginalHeight(HEIGHT); button.setFontId(495);
            button.setXTextAlignment(1);button.setYTextAlignment(1);
            button.setText("View presets"); button.setTextColor(0xFF981F); button.setTextShadowed(true);
            button.setAction(0,"View presets"); button.setHasListener(true);
            button.setOnMouseOverListener((JavaScriptCallback)e -> { if(button!=null) button.setTextColor(0xFFFFFF); });
            button.setOnMouseLeaveListener((JavaScriptCallback)e -> { if(button!=null) button.setTextColor(0xFF981F); });
            button.setOnOpListener((JavaScriptCallback)e -> open.run());
        }
        Rectangle position=position(frame.getBounds(),close.getBounds());
        if(position==null) { clear(); return; }
        for(int i=0;i<background.size();i++) {
            int column=i%3,row=i/3;
            Widget part=background.get(i);
            part.setOriginalX(position.x+(column==0?0:column==1?EDGE:WIDTH-EDGE));
            part.setOriginalY(position.y+(row==0?0:row==1?EDGE:HEIGHT-EDGE));
            part.setOriginalWidth(column==1?WIDTH-2*EDGE:EDGE);
            part.setOriginalHeight(row==1?HEIGHT-2*EDGE:EDGE);
            part.setHidden(false);part.revalidate();
        }
        button.setOriginalX(position.x); button.setOriginalY(position.y);
        button.setHidden(false); button.revalidate();
        title.update(client.getWidget(InterfaceID.Bankmain.TITLE),position.x-6);
    }

    static Rectangle position(Rectangle frame,Rectangle close) {
        // Leave the existing screenshot button and native close control intact.
        Rectangle result=new Rectangle(close.x-frame.x-28-WIDTH,close.y-frame.y+(close.height-HEIGHT)/2,WIDTH,HEIGHT);
        return new Rectangle(0,0,frame.width,frame.height).contains(result)?result:null;
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
            menu.show(canvas,Math.max(0,Math.min(anchor.x+anchor.width-width,canvas.getWidth()-width)),anchor.y+anchor.height);
            panel.search.requestFocusInWindow();
        });
    }
    void close() { JPopupMenu current=popup; popup=null; if(current!=null) current.setVisible(false); }
    void clear() {
        context++;
        title.clear();
        if(button!=null) { button.setHidden(true); button.clearActions(); button.setOnOpListener((Object[])null);
            button.setOnMouseOverListener((Object[])null);button.setOnMouseLeaveListener((Object[])null);button.setHasListener(false); }
        for(Widget part:background) part.setHidden(true);
        if(parent!=null && parent.getChildren()!=null) {
            Widget[] remaining=parent.getChildren().clone();int length=remaining.length;
            for(int i=0;i<length;i++) {
                Widget child=remaining[i];
                if(child==button || background.stream().anyMatch(part -> part==child)) remaining[i]=null;
            }
            while(length>0 && remaining[length-1]==null) length--;
            parent.setChildren(Arrays.copyOf(remaining,length));
        }
        background.clear();
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
            search.setToolTipText("Search activities and saved presets. Approximate spelling works.");
            JPanel header=new JPanel(new BorderLayout(0,4)); header.add(new JLabel("Search activities & saved views"),BorderLayout.NORTH);
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
                    protected List<BankCatalogue.Choice> doInBackground() { return BankCatalogue.search(choices,text,choices.size()); }
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
