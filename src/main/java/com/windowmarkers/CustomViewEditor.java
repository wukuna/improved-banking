package com.windowmarkers;

import java.awt.*;
import java.util.*;
import java.util.List;
import javax.swing.*;

/** Edits a detached copy. No saved groups or bank widgets change before Save. */
final class CustomViewEditor extends JPanel
{
    final CustomPresets.Preset draft;
    final JTextField name=new JTextField(),groupName=new JTextField();
    final DefaultListModel<Marker> groups=new DefaultListModel<>();
    final JList<Marker> list=new JList<>(groups);
    private final JCheckBox slots=new JCheckBox("Keep equipment / inventory layout");
    private final JLabel notice=new JLabel("Changes apply only when you save this custom view.");
    private final JPanel items=new JPanel(new BorderLayout(0,6));
    private final BankItemSearch search;
    private final Set<Integer> bank;
    private final Map<Integer,BankItemSearch.Choice> known=new HashMap<>();
    private Marker current;
    private ItemPalette palette;
    private boolean changing;

    CustomViewEditor(CustomPresets.Preset draft,BankItemSearch search,Set<Integer> bank,List<BankItemSearch.Choice> choices)
    {
        super(new BorderLayout(8,8));this.draft=draft;this.search=search;this.bank=new HashSet<>(bank);
        for(BankItemSearch.Choice choice:choices) known.put(choice.id,choice);
        name.setText(draft.name);slots.setSelected(draft.groupBySlot);
        JPanel heading=new MarkerForm();heading.add(new JLabel("Custom view name"));heading.add(name);heading.add(slots);
        heading.add(new JLabel("<html>Select a group, add items, then Save changes.<br>Drag items in the open preset to save their positions. Bank order stays unchanged.</html>"));
        add(heading,BorderLayout.NORTH);
        for(Marker group:draft.groups) groups.addElement(group);
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        list.setCellRenderer((owner,group,index,selected,focus) -> {
            JLabel label=new JLabel();label.putClientProperty("html.disable",Boolean.TRUE);
            label.setText(group.labelText()+" ("+(group.linkedItemIds==null?0:group.linkedItemIds.length)+")");
            label.setOpaque(true);label.setBackground(selected?owner.getSelectionBackground():owner.getBackground());
            label.setForeground(selected?owner.getSelectionForeground():owner.getForeground());
            label.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0,4,0,0,new Color(group.borderArgb==null?0xFFDAA520:group.borderArgb,true)),BorderFactory.createEmptyBorder(6,6,6,4)));
            label.setToolTipText("Group: "+group.labelText());return label;
        });
        JPanel left=new JPanel(new BorderLayout(0,6));left.add(new JScrollPane(list),BorderLayout.CENTER);
        JPanel actions=new MarkerForm();
        button(actions,"Add group",this::addGroup);button(actions,"Remove selected groups",this::removeGroups);
        JPanel order=new JPanel(new GridLayout(1,2,4,0));button(order,"Move up",()->move(-1));button(order,"Move down",()->move(1));actions.add(order);
        left.add(actions,BorderLayout.SOUTH);left.setPreferredSize(new Dimension(220,400));
        JSplitPane split=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,left,items);split.setResizeWeight(0.25);split.setContinuousLayout(true);split.setBorder(null);
        add(split,BorderLayout.CENTER);add(notice,BorderLayout.SOUTH);
        list.addListSelectionListener(e -> { if(!e.getValueIsAdjusting() && !changing) selectGroup(); });
        if(!groups.isEmpty()) list.setSelectedIndex(0);else selectGroup();
    }
    private static void button(JPanel parent,String title,Runnable action)
    { JButton button=new JButton(title);button.addActionListener(e->action.run());parent.add(button); }
    void flush()
    {
        if(current==null || palette==null) return;
        current.label=groupName.getText().trim();current.linkedItemIds=palette.ids();
        for(BankItemSearch.Choice choice:palette.choices()) known.put(choice.id,choice);
        list.repaint();
    }
    private void selectGroup()
    {
        flush();if(palette!=null) palette.close();palette=null;current=null;items.removeAll();
        if(list.getSelectedIndices().length==1) {
            current=list.getSelectedValue();groupName.setText(current.labelText());
            JPanel heading=new MarkerForm();heading.add(new JLabel("Group label"));heading.add(groupName);
            JPanel colours=new JPanel(new GridLayout(1,2,5,0));
            button(colours,"Border colour",()->colour(false));button(colours,"Background",()->colour(true));heading.add(colours);
            items.add(heading,BorderLayout.NORTH);
            List<BankItemSearch.Choice> selected=new ArrayList<>();
            if(current.linkedItemIds!=null) for(int id:current.linkedItemIds) selected.add(known.getOrDefault(id,new BankItemSearch.Choice(id,"Item "+id)));
            palette=new ItemPalette(search,bank,selected,4000);items.add(palette,BorderLayout.CENTER);
        } else items.add(new JLabel("Select one group to edit its items, or several to move/remove."),BorderLayout.NORTH);
        items.revalidate();items.repaint();
    }
    private void colour(boolean background)
    {
        if(current==null) return;
        Integer argb=background?current.backgroundArgb:current.borderArgb;
        Color chosen=MarkerDialogs.chooseColor(this,background?"Group background":"Group border",new Color(argb==null?0xFFDAA520:argb,true));
        if(chosen!=null) {
            if(background) current.backgroundArgb=new Color(chosen.getRed(),chosen.getGreen(),chosen.getBlue(),argb==null?65:new Color(argb,true).getAlpha()).getRGB();
            else current.borderArgb=chosen.getRGB();list.repaint();
        }
    }
    void addGroup()
    {
        flush();if(groups.size()>=250) { notice.setText("A view can contain up to 250 groups.");return; }
        Marker group=new Marker(0,0,48,36);group.label="New group";group.preset="Custom";group.scrollWithItems=true;
        List<Color> used=new ArrayList<>();for(Marker marker:Collections.list(groups.elements())) if(marker.borderArgb!=null) used.add(new Color(marker.borderArgb,true));
        Color colour=MarkerPresentation.nextColor(used);group.borderArgb=colour.getRGB();group.backgroundArgb=new Color(colour.getRed(),colour.getGreen(),colour.getBlue(),65).getRGB();
        groups.addElement(group);list.setSelectedIndex(groups.size()-1);list.ensureIndexIsVisible(groups.size()-1);
    }
    void removeGroups()
    {
        flush();List<Marker> selected=list.getSelectedValuesList();changing=true;
        for(Marker group:selected) groups.removeElement(group);changing=false;
        list.clearSelection();if(!groups.isEmpty()) list.setSelectedIndex(0);else selectGroup();
    }
    void move(int direction)
    {
        flush();List<Marker> all=Collections.list(groups.elements()),selected=list.getSelectedValuesList();
        int[] indices=list.getSelectedIndices();if(indices.length==0) return;
        int drop=direction<0?Math.max(0,indices[0]-1):Math.min(all.size(),indices[indices.length-1]+2);
        if(!GroupEdits.reorder(all,new ArrayList<>(all),selected,drop)) return;
        changing=true;groups.clear();for(Marker group:all) groups.addElement(group);
        for(int i=0;i<groups.size();i++) if(selected.contains(groups.get(i))) list.addSelectionInterval(i,i);
        changing=false;selectGroup();
    }
    boolean valid()
    {
        flush();String title=name.getText().trim();
        if(title.isEmpty() || title.length()>60) { notice.setText("Enter a view name between 1 and 60 characters.");return false; }
        if(groups.isEmpty()) { notice.setText("Add at least one group, or Cancel to keep the saved view.");return false; }
        for(Marker group:Collections.list(groups.elements())) if(group.labelText().isEmpty() || group.label.length()>60 || !group.linked()) {
            notice.setText("Each group needs a 1–60 character label and at least one item. Select the group to finish it.");return false;
        }
        draft.name=title;draft.bankView=true;draft.groupBySlot=slots.isSelected();draft.groups=Collections.list(groups.elements());return true;
    }
    void close() { if(palette!=null) palette.close(); }
    void error(String text) { notice.setText(text); }
}
