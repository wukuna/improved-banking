package com.windowmarkers;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.inject.Provides;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.swing.*;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.BeforeRender;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ProfileChanged;
import net.runelite.client.events.RuneScapeProfileChanged;
import net.runelite.client.input.MouseAdapter;
import net.runelite.client.input.MouseManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.FontManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.*;

@PluginDescriptor(name="Improved Banking", description="Beta bank markers, tab-aware legends and Wiki-based categories", enabledByDefault=false, conflicts={"Window Markers","Flexible Bank","Bank Screenshot"})
@net.runelite.client.plugins.PluginDependency(net.runelite.client.plugins.banktags.BankTagsPlugin.class)
public class WindowMarkersPlugin extends Plugin
{
    private static final int MAX_MARKERS=250;
    @Inject Client client;
    @Inject ConfigManager configs;
    @Inject Gson gson;
    @Inject WindowMarkersConfig config;
    @Inject MouseManager mouseManager;
    @Inject OverlayManager overlayManager;
    @Inject ClientToolbar toolbar;
    @Inject BorderRenderer borders;
    @Inject ItemManager itemManager;
    @Inject net.runelite.client.callback.ClientThread clientThread;
    @Inject BankLayoutController bankLayout;
    @Inject BankCapture bankCapture;
    @Inject BankScreenshotButton screenshotButton;
    private List<ItemAnchors.Cell> itemCells = new ArrayList<>();
    private final List<Marker> markers = new ArrayList<>();
    private Rectangle bank;
    private Rectangle itemArea;
    private int itemScroll;
    private boolean drawingItems;
    private Point start;
    private Marker preview;
    private boolean armed, active, suppressClick, captured;
    private String accountKey;
    private String layoutKey;
    private LayoutCatalog catalog=new LayoutCatalog();
    private JComboBox<String> layoutPicker;
    private boolean refreshingLayouts;
    private MarkerHistory history=new MarkerHistory();
    private final java.util.Map<String,MarkerHistory> histories=new java.util.LinkedHashMap<>();
    private Marker transformTarget;
    private boolean resizing;
    private NavigationButton navigation;
    private DefaultListModel<String> rows;
    private JLabel status;
    private MarkerLegend legend;
    private List<MarkerLegend.Entry> legendEntries;
    private volatile long legendRevision;
    private volatile Marker legendHover, legendPinned;
    @Inject net.runelite.client.ui.overlay.tooltip.TooltipManager tooltipManager;
    private final MarkerMouse mouse = new MarkerMouse();
    private final MarkerOverlay overlay = new MarkerOverlay();
    private final BackgroundOverlay backgroundOverlay = new BackgroundOverlay();

    @Provides WindowMarkersConfig provideConfig(ConfigManager manager) { return manager.getConfig(WindowMarkersConfig.class); }

    @Override protected synchronized void startUp()
    {
        active = true;
        if (bankLayout!=null)
        {
            if(configs.getConfiguration("windowmarkers","bankLayoutImported")==null)
            {
                for(String key:new String[]{"customWidth","width","customHeight","height","columns","depositButtons","alignDepositRight"})
                {
                    String value=configs.getConfiguration("flexiblebank",key);
                    if(value!=null && configs.getConfiguration("windowmarkers",key)==null) { configs.setConfiguration("windowmarkers",key,value); }
                }
                configs.setConfiguration("windowmarkers","bankLayoutImported",true);
            }
            bankLayout.startUp();
        }
        load();
        overlayManager.add(overlay);
        overlayManager.add(backgroundOverlay);
        mouseManager.registerMouseListener(mouse);
        SwingUtilities.invokeLater(() -> {
            synchronized (WindowMarkersPlugin.this)
            {
                if (!active) { return; }
                PluginPanel panel = new PluginPanel() {};
                panel.setLayout(new BorderLayout(0, 10));
                JPanel controls = new MarkerForm();
                JPanel toolsPanel = new MarkerForm();
                controls.add(new JLabel("Improved Banking"));
                controls.add(new JLabel("<html>Saved per character + RuneLite profile.<br>Markers in the item area scroll<br>with the bank contents.</html>"));
                status = new JLabel("Open your bank to begin.");
                controls.add(status);
                layoutPicker=new JComboBox<>();
                layoutPicker.addActionListener(e -> { if (!refreshingLayouts) { switchNamedLayout(layoutPicker.getSelectedIndex()); } });
                controls.add(layoutPicker);
                JPanel layoutActions=new JPanel(new GridLayout(1,3,3,0));
                JButton newLayout=new JButton("New"), duplicate=new JButton("Copy"), rename=new JButton("Rename");
                newLayout.addActionListener(e -> nameLayout(false,false)); duplicate.addActionListener(e -> nameLayout(true,false));
                rename.addActionListener(e -> nameLayout(false,true));
                layoutActions.add(newLayout); layoutActions.add(duplicate); layoutActions.add(rename); controls.add(layoutActions);
                JPanel historyActions=new JPanel(new GridLayout(1,2,3,0));
                JButton undo=new JButton("Undo"), redo=new JButton("Redo");
                undo.addActionListener(e -> restoreHistory(false)); redo.addActionListener(e -> restoreHistory(true));
                historyActions.add(undo); historyActions.add(redo); controls.add(historyActions);
                JButton categorize=new JButton("Auto-categorize");
                categorize.addActionListener(e -> autoCategorize()); controls.add(categorize);
                JButton layoutControls=new JButton("Bank size and tabs");
                layoutControls.addActionListener(e -> editBankLayout()); toolsPanel.add(new JLabel("Bank tools")); toolsPanel.add(layoutControls);
                JButton screenshot=new JButton("Save bank screenshot");
                screenshot.addActionListener(e -> clientThread.invokeLater(this::captureBank)); toolsPanel.add(screenshot);
                JButton add = new JButton("Draw marker");
                add.addActionListener(e -> arm());
                controls.add(add);
                JButton cancel = new JButton("Cancel drawing");
                cancel.addActionListener(e -> cancel());
                controls.add(cancel);
                JCheckBox snap = new JCheckBox("Snap areas to item grid",config.snapToGrid());
                snap.addActionListener(e -> configs.setConfiguration("windowmarkers","snapToGrid",snap.isSelected()));
                controls.add(snap);
                JCheckBox link = new JCheckBox("<html>Link new markers<br>to enclosed items</html>",config.linkNewMarkers());
                link.addActionListener(e -> configs.setConfiguration("windowmarkers","linkNewMarkers",link.isSelected()));
                controls.add(link);
                JButton copyAccount = new JButton("Copy earlier account markers");
                copyAccount.addActionListener(e -> copyAccountMarkers());
                toolsPanel.add(new JLabel("Copy older markers")); toolsPanel.add(copyAccount);
                JButton importShared = new JButton("Import shared markers");
                importShared.addActionListener(e -> importShared());
                toolsPanel.add(importShared);
                JComboBox<WindowMarkersConfig.BorderStyle> styles = new JComboBox<>(WindowMarkersConfig.BorderStyle.values());
                styles.setSelectedItem(config.style());
                styles.addActionListener(e -> configs.setConfiguration("windowmarkers", "style", styles.getSelectedItem()));
                toolsPanel.add(new JLabel("Default border style"));
                toolsPanel.add(styles);
                rows = new DefaultListModel<>();
                JList<String> list = new JList<>(rows);
                list.setVisibleRowCount(6);
                list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
                JButton remove = new JButton("Delete selected markers");
                remove.addActionListener(e -> removeMarkers(list.getSelectedIndices(),false));
                JButton edit = new JButton("Edit label and appearance");
                edit.addActionListener(e -> edit(list.getSelectedIndex()));
                JPanel actions = new JPanel(new GridLayout(0,1,0,6));
                JButton move=new JButton("Move selected area"), resize=new JButton("Resize selected area");
                move.addActionListener(e -> armTransform(list.getSelectedIndex(),false));
                resize.addActionListener(e -> armTransform(list.getSelectedIndex(),true));
                actions.add(move); actions.add(resize);
                actions.add(edit); actions.add(remove);
                JButton clear=new JButton("Clear all markers");
                clear.setToolTipText("Clear this layout only. Undo restores all markers.");
                clear.addActionListener(e -> removeMarkers(new int[0],true));
                actions.add(clear);
                list.setToolTipText("Shift + Up/Down selects a range; Ctrl + click selects individual markers.");
                list.addListSelectionListener(e -> {
                    boolean single=list.getSelectedIndices().length==1;
                    edit.setEnabled(single); move.setEnabled(single); resize.setEnabled(single);
                    remove.setEnabled(!list.isSelectionEmpty());
                });
                edit.setEnabled(false); move.setEnabled(false); resize.setEnabled(false); remove.setEnabled(false);
                list.getInputMap().put(KeyStroke.getKeyStroke("DELETE"),"deleteMarkers");
                list.getActionMap().put("deleteMarkers",new AbstractAction() {
                    @Override public void actionPerformed(java.awt.event.ActionEvent e)
                    { removeMarkers(list.getSelectedIndices(),false); }
                });
                JPanel editor=new JPanel(new BorderLayout(0,10));
                editor.add(controls,BorderLayout.NORTH);
                editor.add(new JScrollPane(list),BorderLayout.CENTER);
                editor.add(actions,BorderLayout.SOUTH);
                legend=new MarkerLegend(m -> legendHover=m,m -> legendPinned=m);
                JTabbedPane tabs=new JTabbedPane();
                tabs.addTab("Legend",legend); tabs.addTab("Markers",editor); tabs.addTab("Tools",toolsPanel);
                tabs.addChangeListener(e -> { legendHover=null; });
                panel.add(tabs,BorderLayout.CENTER);
                BufferedImage icon = new BufferedImage(24,24,BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = icon.createGraphics();
                g.setColor(new Color(190,155,85)); g.drawRect(2,2,19,19); g.drawRect(5,5,13,13); g.dispose();
                navigation = NavigationButton.builder().tooltip("Improved Banking").icon(icon).panel(panel).build();
                toolbar.addNavigation(navigation);
                refreshRows();
            }
        });
    }

    @Override protected synchronized void shutDown()
    {
        active = false;
        if(screenshotButton!=null) { screenshotButton.clear(); }
        if(bankLayout!=null) { bankLayout.shutDown(); }
        cancel(); bank = null; captured = false; suppressClick = false;
        mouseManager.unregisterMouseListener(mouse);
        overlayManager.remove(overlay);
        overlayManager.remove(backgroundOverlay);
        borders.clear(); markers.clear();
        NavigationButton old = navigation;
        navigation = null;
        if (old != null) { SwingUtilities.invokeLater(() -> toolbar.removeNavigation(old)); }
    }

    @Subscribe public synchronized void onProfileChanged(ProfileChanged event) { cancel(); bank = null; load(); }
    @Subscribe public synchronized void onRuneScapeProfileChanged(RuneScapeProfileChanged event)
    {
        cancel(); bank = null; load();
    }

    private void load()
    {
        legendHover=null; legendPinned=null;
        markers.clear();
        accountKey = configs.getRSProfileKey();
        catalog=new LayoutCatalog();
        if (accountKey != null && baseLayoutKey() != null)
        {
            try
            {
                String rawCatalog=configs.getConfiguration("windowmarkers",accountKey,baseLayoutKey()+"_catalog");
                LayoutCatalog saved=rawCatalog == null ? null : gson.fromJson(rawCatalog,LayoutCatalog.class);
                if (saved != null) { catalog=saved; catalog.normalize(); }
            }
            catch (JsonParseException ex) { setStatus("Could not read named layouts; using Default."); }
        }
        layoutKey = currentLayoutKey();
        String raw = accountKey == null || layoutKey == null ? null : configs.getConfiguration("windowmarkers", accountKey, layoutKey);
        readMarkers(raw);
        String historyKey=accountKey+"/"+layoutKey;
        if (histories.size()>40) { histories.clear(); }
        history=histories.computeIfAbsent(historyKey,k -> new MarkerHistory());
        String snapshot=gson.toJson(markers);
        if (!history.matches(snapshot)) { history.reset(snapshot); }
        refreshRows();
    }

    private void readMarkers(String raw)
    {
        try
        {
            Marker[] saved = raw == null ? null : gson.fromJson(raw, Marker[].class);
            if (saved != null)
            {
                for (Marker marker : saved)
                {
                    if (marker != null && marker.valid() && markers.size() < MAX_MARKERS) { markers.add(marker); }
                }
            }
        }
        catch (JsonParseException ex) { setStatus("Saved markers could not be read."); }
    }

    private void save()
    {
        commit(true);
    }

    private void commit(boolean record)
    {
        if (!currentLayout()) { load(); return; }
        String snapshot=gson.toJson(markers);
        if (record) { history.record(snapshot); }
        configs.setConfiguration("windowmarkers", accountKey, layoutKey, snapshot);
        refreshRows();
    }

    private String baseLayoutKey()
    {
        return configs.getProfile() == null ? null : "markersV3_profile_" + configs.getProfile().getId();
    }

    private String currentLayoutKey() { String base=baseLayoutKey(); return base == null ? null : catalog.storageKey(base); }

    synchronized void restoreHistory(boolean redo)
    {
        if (!currentLayout()) { return; }
        cancel(); String value=redo ? history.redo() : history.undo();
        if (value == null) { setStatus(redo ? "Nothing to redo." : "Nothing to undo."); return; }
        markers.clear(); readMarkers(value); commit(false);
    }

    synchronized void switchNamedLayout(int index)
    {
        if (!currentLayout() || index<0 || index>=catalog.names.size()) { return; }
        String id=new ArrayList<>(catalog.names.keySet()).get(index);
        if (id.equals(catalog.active)) { return; }
        cancel(); catalog.active=id;
        configs.setConfiguration("windowmarkers",accountKey,baseLayoutKey()+"_catalog",gson.toJson(catalog));
        load();
    }

    private synchronized void captureBank()
    {
        if(!active || bankCapture==null || bank==null) { return; }
        boolean saved=bankCapture.save(new ArrayList<>(markers));
        setStatus(saved ? "Saved to RuneLite screenshots / bank-markers." : "Open a populated bank tab to capture.");
    }

    private void editBankLayout()
    {
        String profile=configs.getProfile().getId()+"";
        JPanel form=new MarkerForm();
        JCheckBox enabled=new JCheckBox("Use built-in bank layout",config.bankLayoutEnabled());
        JCheckBox customWidth=new JCheckBox("Custom width",config.customWidth()),customHeight=new JCheckBox("Custom height",config.customHeight());
        JSpinner width=new JSpinner(new SpinnerNumberModel(Math.max(488,Math.min(4000,config.width())),488,4000,24));
        JSpinner height=new JSpinner(new SpinnerNumberModel(Math.max(200,Math.min(4000,config.height())),200,4000,24));
        JSpinner columns=new JSpinner(new SpinnerNumberModel(Math.max(0,Math.min(60,config.columns())),0,60,1));
        JSpinner gap=new JSpinner(new SpinnerNumberModel(Math.max(0,Math.min(40,config.tabGap())),0,40,2));
        JComboBox<WindowMarkersConfig.TabAlignment> alignment=new JComboBox<>(WindowMarkersConfig.TabAlignment.values());alignment.setSelectedItem(config.tabAlignment());
        JComboBox<DepositButtons> deposits=new JComboBox<>(DepositButtons.values()); deposits.setSelectedItem(config.depositButtons());
        form.add(enabled); form.add(customWidth); form.add(width); form.add(customHeight); form.add(height);
        form.add(new JLabel("Item columns (0 = fit available width)")); form.add(columns);
        form.add(new JLabel("Top tab alignment")); form.add(alignment);
        form.add(new JLabel("Space below tabs (pixels)")); form.add(gap);
        form.add(new JLabel("Deposit buttons")); form.add(deposits);
        form.add(new JLabel("<html>Bank Tag layouts retain their designed item grid.<br>Saved marker layouts remain per account and profile.</html>"));
        if(!MarkerDialogs.confirm(navigation==null ? null : navigation.getPanel(),form,"Bank size and tabs",480,620)) { return; }
        if(!active || !profile.equals(configs.getProfile().getId()+"")) { setStatus("Profile changed. Reopen bank settings."); return; }
        configs.setConfiguration("windowmarkers","bankLayoutEnabled",enabled.isSelected());
        configs.setConfiguration("windowmarkers","customWidth",customWidth.isSelected());
        configs.setConfiguration("windowmarkers","width",width.getValue());
        configs.setConfiguration("windowmarkers","customHeight",customHeight.isSelected());
        configs.setConfiguration("windowmarkers","height",height.getValue());
        configs.setConfiguration("windowmarkers","columns",columns.getValue());
        configs.setConfiguration("windowmarkers","tabGap",gap.getValue());
        configs.setConfiguration("windowmarkers","tabAlignment",alignment.getSelectedItem());
        configs.setConfiguration("windowmarkers","depositButtons",deposits.getSelectedItem());
        setStatus("Bank layout settings saved.");
    }

    private void nameLayout(boolean copy, boolean rename)
    {
        String owner, layout;
        synchronized (this) { if (!currentLayout()) { return; } owner=accountKey; layout=layoutKey; }
        JTextField nameField=new JTextField(24);
        if (!MarkerDialogs.confirm(navigation==null ? null : navigation.getPanel(),nameField,rename ? "Rename current layout" : "Layout name",420,200)) { return; }
        String name=nameField.getText();
        if (name==null || name.trim().isEmpty()) { return; }
        synchronized (this)
        {
            if (!currentLayout() || !owner.equals(accountKey) || !layout.equals(layoutKey)) { return; }
            createNamedLayout(name,copy,rename);
        }
    }

    synchronized void createNamedLayout(String name, boolean copy, boolean rename)
    {
        if (!currentLayout()) { return; }
        name=name.trim(); if (name.isEmpty()) { return; }
        name=name.substring(0,Math.min(40,name.length()));
        if (catalog.names.containsValue(name)) { setStatus("Choose a different layout name."); return; }
        if (!rename && catalog.names.size()>=20) { setStatus("20-layout limit reached."); return; }
        cancel();
        if (rename) { catalog.names.put(catalog.active,name); }
        else
        {
            String id=java.util.UUID.randomUUID().toString();
            catalog.names.put(id,name); catalog.active=id;
            configs.setConfiguration("windowmarkers",accountKey,catalog.storageKey(baseLayoutKey()),copy ? gson.toJson(markers) : "[]");
        }
        configs.setConfiguration("windowmarkers",accountKey,baseLayoutKey()+"_catalog",gson.toJson(catalog));
        load();
    }

    private void autoCategorize()
    {
        JComboBox<AutoCategories.Preset> presets=new JComboBox<>(AutoCategories.Preset.values());
        JCheckBox excludeWorse=new JCheckBox("Exclude worse items (verified raid gear and tools)",false);
        JPanel settings=new MarkerForm();
        settings.add(presets); settings.add(excludeWorse);
        settings.add(new JLabel("<html>Uses your real skill levels and owned replacements.<br>Preserves set, clue, quest and special-use alternatives.<br>Exclusions are listed for review before any markers change.</html>"));
        if (!MarkerDialogs.confirm(navigation==null ? null : navigation.getPanel(),settings,"Auto-categorize",560,280)) { return; }
        AutoCategories.Preset preset=(AutoCategories.Preset)presets.getSelectedItem();
        final boolean filterWorse=excludeWorse.isSelected();
        final String owner, layout;
        synchronized (this)
        {
            if (bank==null || !currentLayout()) { setStatus("Open your bank first."); return; }
            owner=accountKey; layout=layoutKey;
        }
        clientThread.invokeLater(() -> {
            synchronized (WindowMarkersPlugin.this)
            {
                if (!active || bank==null || !owner.equals(accountKey) || !layout.equals(layoutKey) || !currentLayout()) { return; }
                net.runelite.api.ItemContainer container=client.getItemContainer(net.runelite.api.gameval.InventoryID.BANK);
                if (container==null) { return; }
                java.util.Map<Integer,AutoCategories.Item> facts=new java.util.LinkedHashMap<>();
                for (net.runelite.api.Item item : container.getItems())
                {
                    if (item.getId()<0 || item.getQuantity()<=0 || item.getId()==6512) { continue; }
                    int id=itemManager.canonicalize(item.getId());
                    net.runelite.api.ItemComposition definition=itemManager.getItemComposition(id);
                    net.runelite.client.game.ItemStats stats=itemManager.getItemStats(id);
                    net.runelite.client.game.ItemEquipmentStats eq=stats==null ? null : stats.getEquipment();
                    int melee=eq==null ? 0 : Math.max(eq.getAstab(),Math.max(eq.getAslash(),eq.getAcrush()))+eq.getStr();
                    int ranged=eq==null ? 0 : eq.getArange()+eq.getRstr();
                    int magic=eq==null ? 0 : eq.getAmagic()+(int)eq.getMdmg();
                    facts.put(id,new AutoCategories.Item(id,definition.getName(),stats!=null && stats.isEquipable(),definition.getInventoryActions(),melee,ranged,magic));
                }
                List<AutoCategories.Item> owned=new ArrayList<>(facts.values());
                java.util.Map<Integer,String> exclusions=filterWorse ? LowerTierFilter.exclusions(owned,
                    client.getRealSkillLevel(net.runelite.api.Skill.MINING),client.getRealSkillLevel(net.runelite.api.Skill.WOODCUTTING),client.getRealSkillLevel(net.runelite.api.Skill.FISHING)) : java.util.Collections.emptyMap();
                owned.removeIf(i -> exclusions.containsKey(i.id));
                java.util.Map<String,List<AutoCategories.Item>> groups=AutoCategories.suggest(preset,owned);
                if (filterWorse)
                {
                    net.runelite.api.Skill[] skills={net.runelite.api.Skill.ATTACK,net.runelite.api.Skill.STRENGTH,net.runelite.api.Skill.DEFENCE,net.runelite.api.Skill.RANGED,net.runelite.api.Skill.MAGIC,net.runelite.api.Skill.PRAYER,net.runelite.api.Skill.HITPOINTS};
                    int[] levels=new int[skills.length];
                    for(int i=0;i<skills.length;i++) { levels[i]=client.getRealSkillLevel(skills[i]); }
                    boolean ancientRingsUnlocked=net.runelite.api.Quest.DESERT_TREASURE_II__THE_FALLEN_EMPIRE.getState(client)==net.runelite.api.QuestState.FINISHED;
                    RaidGearFilter.filter(preset,groups,levels,ancientRingsUnlocked,exclusions);
                }
                SwingUtilities.invokeLater(() -> reviewCategories(owner,layout,groups,preset,exclusions));
            }
        });
    }

    private void reviewCategories(String owner,String layout,java.util.Map<String,List<AutoCategories.Item>> groups,AutoCategories.Preset preset,java.util.Map<Integer,String> exclusions)
    {
        if (groups.isEmpty()) { setStatus("No owned items matched this preset."); return; }
        JPanel panel=new MarkerForm();
        panel.add(new JLabel("<html>Wiki classifications describe uses, not one combined loadout.<br>Outfit groups include owned pieces; partial ownership does<br>not activate a full-set bonus. Tools are grouped separately.<br>Raid lists depend on role, spellbook, charges and invocations.<br>Unresolved items stay in an unchecked review group.</html>"));
        if (!exclusions.isEmpty())
        {
            JTextArea excluded=new JTextArea("Excluded lower-tier items:\n"+String.join("\n",exclusions.values()));
            excluded.setEditable(false); excluded.setLineWrap(true); excluded.setWrapStyleWord(true); excluded.setColumns(42);
            panel.add(excluded);
        }
        JButton source=new JButton("Open Wiki source");
        source.addActionListener(e -> net.runelite.client.util.LinkBrowser.browse(WikiCatalogue.source(preset)));
        panel.add(source);
        panel.add(new JLabel("<html>Existing linked items are skipped. To correct old groups,<br>check their names below for replacement. Area markers stay.<br>Replacing removes the entire checked marker. Undo restores it.</html>"));
        java.util.Map<JCheckBox,Marker> replacements=new java.util.LinkedHashMap<>();
        synchronized (this)
        {
            if (!currentLayout() || !owner.equals(accountKey) || !layout.equals(layoutKey)) { return; }
            for (Marker marker : markers)
            {
                if (!marker.linked()) { continue; }
                JCheckBox replace=new JCheckBox("Replace: "+marker.labelText(),false);
                replacements.put(replace,marker); panel.add(replace);
            }
        }
        java.util.Map<JCheckBox,List<AutoCategories.Item>> choices=new java.util.LinkedHashMap<>();
        for (java.util.Map.Entry<String,List<AutoCategories.Item>> entry : groups.entrySet())
        {
            JCheckBox choose=new JCheckBox(entry.getKey(),RaidRecommendations.defaultSelected(entry.getKey()));
            choices.put(choose,entry.getValue()); panel.add(choose);
            JTextArea items=new JTextArea(entry.getValue().stream().map(i -> i.name).collect(java.util.stream.Collectors.joining(", ")));
            items.setLineWrap(true); items.setWrapStyleWord(true); items.setEditable(false); items.setColumns(42);
            items.setBackground(panel.getBackground()); panel.add(items);
        }
        
        if (!MarkerDialogs.confirm(navigation==null ? null : navigation.getPanel(),panel,"Review "+preset,620,620)) { return; }
        synchronized (this)
        {
            if (!active || !owner.equals(accountKey) || !layout.equals(layoutKey) || !currentLayout()) { setStatus("Layout changed; run categorization again."); return; }
            boolean anySelected=choices.keySet().stream().anyMatch(JCheckBox::isSelected);
            if (!anySelected) { setStatus("No groups selected; existing markers kept."); return; }
            cancel();
            int replaced=0;
            for (java.util.Map.Entry<JCheckBox,Marker> entry : replacements.entrySet())
            { if (entry.getKey().isSelected() && markers.remove(entry.getValue())) { replaced++; } }
            java.util.Set<Integer> assigned=new java.util.HashSet<>();
            for (Marker m : markers) if (m.linked()) for (int id : m.linkedItemIds) { assigned.add(id); }
            int added=0, limited=0;
            List<Color> usedColors=new ArrayList<>();
            for (Marker existing : markers) { usedColors.add(existing.borderArgb==null ? config.color() : new Color(existing.borderArgb,true)); }
            for (java.util.Map.Entry<JCheckBox,List<AutoCategories.Item>> entry : choices.entrySet())
            {
                if (!entry.getKey().isSelected()) { continue; }
                if (markers.size()>=MAX_MARKERS) { limited++; continue; }
                int[] ids=entry.getValue().stream().mapToInt(i -> i.id).distinct().filter(id -> !assigned.contains(id)).toArray();
                if (ids.length==0) { continue; }
                Marker marker=new Marker(0,0,48,36); marker.scrollWithItems=true; marker.linkedItemIds=ids;
                marker.label=MarkerPresentation.title(entry.getKey().getText()); marker.labelOnce=true;
                marker.labelFont="Arial"; marker.labelSize=12;
                marker.labelPlacement=Marker.LabelPlacement.ABOVE; marker.style=WindowMarkersConfig.BorderStyle.PLAIN;
                Color color=MarkerPresentation.nextColor(usedColors); usedColors.add(color);
                marker.borderArgb=color.getRGB(); marker.backgroundArgb=new Color(color.getRed(),color.getGreen(),color.getBlue(),65).getRGB();
                markers.add(marker); for (int id : ids) { assigned.add(id); } added++;
            }
            if (added>0 || replaced>0) { save(); }
            setStatus(added+" groups added; "+replaced+" replaced."+(limited>0 ? " "+limited+" groups skipped at the "+MAX_MARKERS+"-marker limit." : "")+" Undo restores the previous layout.");
        }
    }

    private boolean currentLayout()
    {
        return accountKey != null && layoutKey != null && accountKey.equals(configs.getRSProfileKey())
            && layoutKey.equals(currentLayoutKey());
    }

    synchronized void copyAccountMarkers()
    {
        if (bank == null || !currentLayout()) { setStatus("Open this account's bank first."); return; }
        if (!markers.isEmpty()) { setStatus("Copy requires an empty profile layout."); return; }
        cancel();
        readMarkers(configs.getConfiguration("windowmarkers", accountKey, "markersV2"));
        if (markers.isEmpty()) { setStatus("No earlier account markers to copy."); return; }
        save(); setStatus("Copied markers into this profile. Original retained.");
    }

    private synchronized void importShared()
    {
        if (bank == null || !currentLayout()) { setStatus("Open this account's bank first."); return; }
        if (!markers.isEmpty()) { setStatus("Import requires an empty account layout."); return; }
        cancel();
        readMarkers(configs.getConfiguration("windowmarkers", "markersV1"));
        if (markers.isEmpty()) { setStatus("No old shared markers to import."); return; }
        save(); setStatus("Copied shared markers to this account.");
    }

    private void refreshRows()
    {
        List<String> layoutNames=new ArrayList<>(catalog.names.values());
        int activeLayout=new ArrayList<>(catalog.names.keySet()).indexOf(catalog.active);
        List<String> labels = new ArrayList<>();
        legendEntries=null;
        long revision=++legendRevision;
        for (int i=0;i<markers.size();i++)
        {
            Marker m = markers.get(i);

            labels.add((m.labelText().isEmpty() ? "Marker " + (i+1) : m.labelText()) + (m.linked() ? " — linked items" : " — " + m.width + " × " + m.height));
        }
        SwingUtilities.invokeLater(() -> {
            if (rows == null) { return; }
            if (layoutPicker != null)
            {
                refreshingLayouts=true; layoutPicker.removeAllItems();
                for (String name : layoutNames) { layoutPicker.addItem(name); }
                layoutPicker.setSelectedIndex(activeLayout); refreshingLayouts=false;
            }
            rows.clear(); for (String label : labels) { rows.addElement(label); }
            if(legend!=null && revision==legendRevision) { legend.replace(java.util.Collections.emptyList()); }
        });
    }

    private void setStatus(String text) { SwingUtilities.invokeLater(() -> { if (status != null) { status.setText("<html>" + text + "</html>"); } }); }

    synchronized void arm()
    {
        cancel();
        if (bank == null || !currentLayout()) { setStatus("Open your bank first."); return; }
        if (markers.size() >= MAX_MARKERS) { setStatus(MAX_MARKERS+"-marker limit reached."); return; }
        armed = true; setStatus("Drag inside bank. Right-click cancels.");
    }

    synchronized void cancel() { armed = false; start = null; preview = null; transformTarget=null; setStatus("Ready. Draw or select a marker."); }

    synchronized void armTransform(int index, boolean resize)
    {
        cancel();
        if (bank==null || !currentLayout() || index<0 || index>=markers.size()) { setStatus("Open the bank and select an area marker."); return; }
        if (markers.get(index).linked()) { setStatus("Unlink this marker before moving its area."); return; }
        transformTarget=markers.get(index); resizing=resize; armed=true;
        setStatus(resize ? "Drag inside the area to set its bottom-right corner." : "Drag inside the area to move it.");
    }

    synchronized void removeMarkers(int[] indices, boolean all)
    {
        if (!active || !currentLayout()) { return; }
        int before=markers.size();
        cancel();
        if (all) { markers.clear(); }
        else
        {
            int[] sorted=java.util.Arrays.stream(indices).distinct().sorted().toArray();
            for(int i=sorted.length-1;i>=0;i--)
            { if(sorted[i]>=0 && sorted[i]<markers.size()) { markers.remove(sorted[i]); } }
        }
        int removed=before-markers.size();
        if(removed>0) { save(); }
        setStatus(removed+" markers deleted from this layout. Undo restores them.");
    }

    private void edit(int index)
    {
        final Marker marker;
        final String owner;
        final String ownerLayout;
        final int[] enclosedIds;
        synchronized (this)
        {
            if (index < 0 || index >= markers.size()) { setStatus("Select a marker first."); return; }
            marker = markers.get(index); owner = accountKey; ownerLayout = layoutKey;
            Rectangle original=bank == null ? null : marker.onScreen(bank,itemArea,itemScroll);
            enclosedIds=original == null || itemArea == null ? new int[0] : ItemAnchors.enclosed(original,itemCells,itemArea);
        }
        JTextField label = new JTextField(marker.labelText());
        JComboBox<Marker.LabelPlacement> placement=new JComboBox<>(Marker.LabelPlacement.values());
        placement.setSelectedItem(marker.labelPlacement==null ? Marker.LabelPlacement.INSIDE : marker.labelPlacement);
        JCheckBox labelOnce=new JCheckBox("Show label once across linked regions",Boolean.TRUE.equals(marker.labelOnce));
        JCheckBox linked = new JCheckBox("Link to enclosed items",marker.linked());
        linked.setToolTipText("Follows selected item identities in every tab. Uncheck to restore the original area.");
        JComboBox<String> font = new JComboBox<>(new String[]{"RuneScape","RuneScape Bold","Arial","Verdana","Serif","Monospaced"});
        font.setSelectedItem(marker.labelFont == null ? "RuneScape" : marker.labelFont);
        JSpinner fontSize = new JSpinner(new SpinnerNumberModel(marker.fontSize(),8,48,1));
        Color[] labelColor={marker.labelArgb == null ? Color.WHITE : new Color(marker.labelArgb,true)};
        JButton labelColorButton = new JButton("Choose label color");
        labelColorButton.setForeground(labelColor[0]);
        labelColorButton.addActionListener(e -> {
            Color chosen=MarkerDialogs.chooseColor(labelColorButton,"Label color",labelColor[0]);
            if (chosen != null) { labelColor[0]=chosen; labelColorButton.setForeground(chosen); }
        });
        JComboBox<WindowMarkersConfig.BorderStyle> style = new JComboBox<>(WindowMarkersConfig.BorderStyle.values());
        style.setSelectedItem(marker.style == null ? config.style() : marker.style);
        JCheckBox customBorder = new JCheckBox("Use custom border color", marker.borderArgb != null);
        JCheckBox fill = new JCheckBox("Show background", marker.backgroundArgb != null);
        Color[] colors = {marker.borderArgb == null ? config.color() : new Color(marker.borderArgb,true),
            marker.backgroundArgb == null ? new Color(60,45,30) : new Color(marker.backgroundArgb,true)};
        JButton border = new JButton("Choose border color"), background = new JButton("Choose background color");
        border.setForeground(colors[0]); background.setForeground(new Color(colors[1].getRGB()));
        border.addActionListener(e -> {
            Color chosen=MarkerDialogs.chooseColor(border,"Border color",colors[0]);
            if (chosen != null) { colors[0]=chosen; customBorder.setSelected(true); border.setForeground(chosen); }
        });
        background.addActionListener(e -> {
            Color chosen=MarkerDialogs.chooseColor(background,"Background color",colors[1]);
            if (chosen != null) { colors[1]=chosen; fill.setSelected(true); background.setForeground(chosen); }
        });
        JSpinner opacity = new JSpinner(new SpinnerNumberModel(marker.backgroundArgb == null ? 50 : colors[1].getAlpha()*100/255,0,100,5));
        JPanel form = new MarkerForm();
        form.add(new JLabel("Label (up to 60 characters)")); form.add(label);
        form.add(placement); form.add(labelOnce);
        form.add(linked);
        form.add(font); form.add(new JLabel("Font size (8–48)")); form.add(fontSize); form.add(labelColorButton);
        form.add(new JLabel("Border style")); form.add(style); form.add(customBorder); form.add(border);
        form.add(fill); form.add(background); form.add(new JLabel("Background opacity (%)")); form.add(opacity);
        
        
        if (!MarkerDialogs.confirm(navigation==null ? null : navigation.getPanel(),form,"Marker appearance",440,640)) { return; }
        synchronized (this)
        {
            if (!active || owner == null || !owner.equals(accountKey) || !java.util.Objects.equals(ownerLayout,layoutKey) || !currentLayout() || !markers.contains(marker))
            { setStatus("Account or profile changed. Reopen the editor."); return; }
            if (linked.isSelected() && !marker.linked() && enclosedIds.length == 0)
            { setStatus("No enclosed items. Open the bank and redraw around items."); return; }
            marker.linkedItemIds=linked.isSelected() ? (marker.linked() ? marker.linkedItemIds : enclosedIds) : null;
            marker.label = label.getText().substring(0,Math.min(60,label.getText().length()));
            marker.labelPlacement=(Marker.LabelPlacement)placement.getSelectedItem(); marker.labelOnce=labelOnce.isSelected();
            marker.labelFont=(String)font.getSelectedItem(); marker.labelSize=((Number)fontSize.getValue()).intValue(); marker.labelArgb=labelColor[0].getRGB();
            marker.style = (WindowMarkersConfig.BorderStyle) style.getSelectedItem();
            marker.borderArgb = customBorder.isSelected() ? colors[0].getRGB() : null;
            int alpha=((Number)opacity.getValue()).intValue()*255/100;
            marker.backgroundArgb = fill.isSelected() ? new Color(colors[1].getRed(),colors[1].getGreen(),colors[1].getBlue(),alpha).getRGB() : null;
            save(); setStatus("Marker appearance saved.");
        }
    }

    @Subscribe public synchronized void onBeforeRender(BeforeRender event)
    {
        if (client.getGameState() != GameState.LOGGED_IN)
        {
            if(screenshotButton!=null) { screenshotButton.clear(); }
            if (bank != null || accountKey != null) { cancel(); bank = null; accountKey = null; layoutKey = null; markers.clear(); refreshRows(); }
            return;
        }
        // A first-time character may not have an RS profile until its first account-scoped write.
        if (configs.getRSProfileKey() == null)
        {
            configs.setRSProfileConfiguration("windowmarkers", "storageVersion", 2);
        }
        if (!java.util.Objects.equals(accountKey, configs.getRSProfileKey()) || !java.util.Objects.equals(layoutKey,currentLayoutKey())) { cancel(); bank = null; load(); }
        Widget widget = client.getGameState() == GameState.LOGGED_IN ? client.getWidget(InterfaceID.Bankmain.UNIVERSE) : null;
        Rectangle next = widget == null || widget.isHidden() ? null : widget.getBounds();
        if (next != null && next.isEmpty()) { next = null; }
        if (bank != null && !bank.equals(next)) { cancel(); }
        bank = next;
        Widget items = bank == null ? null : client.getWidget(InterfaceID.Bankmain.ITEMS);
        Rectangle nextItems = items == null || items.isHidden() ? null : items.getBounds().intersection(bank);
        if (nextItems != null && nextItems.isEmpty()) { nextItems=null; }
        int nextScroll=items == null ? 0 : items.getScrollY();
        if (start != null && (!java.util.Objects.equals(itemArea,nextItems) || itemScroll != nextScroll)) { cancel(); }
        itemArea=nextItems; itemScroll=nextScroll;
        itemCells=new ArrayList<>();
        List<ItemAnchors.Cell> tabCells=new ArrayList<>();
        if (itemArea != null && items.getDynamicChildren() != null)
        {
            for (Widget child : items.getDynamicChildren())
            {
                if (child == null || child.isSelfHidden() || child.getItemId() < 0 || child.getItemId() == 6512
                    || child.getRelativeX()<0 || child.getRelativeY()<0) { continue; }
                Rectangle bounds=child.getBounds();
                if(!bounds.isEmpty()) { tabCells.add(new ItemAnchors.Cell(itemManager.canonicalize(child.getItemId()),bounds)); }
                if(child.isHidden()) { continue; }
                if (bounds.isEmpty() || !bounds.intersects(itemArea)) { continue; }
                itemCells.add(new ItemAnchors.Cell(itemManager.canonicalize(child.getItemId()),bounds));
            }
        }
        if (itemArea != null && currentLayout() && markers.stream().anyMatch(m -> m.scrollWithItems == null))
        {
            String backupKey=layoutKey+"_beforeScroll";
            if (configs.getConfiguration("windowmarkers",accountKey,backupKey) == null)
            { configs.setConfiguration("windowmarkers",accountKey,backupKey,gson.toJson(markers)); }
            for (Marker marker : markers) { marker.anchorLegacy(bank,itemArea); }
            commit(false); history.reset(gson.toJson(markers));
        }
        updateLegend(tabCells);
        if(screenshotButton!=null) { screenshotButton.update(active && bank!=null && config.screenshotButton(),this::captureBank); }
    }

    private void updateLegend(List<ItemAnchors.Cell> tabCells)
    {
        List<MarkerLegend.Entry> entries=MarkerLegend.forTab(markers,tabCells,bank,itemArea,itemScroll,config.color());
        if(MarkerLegend.same(legendEntries,entries)) { return; }
        legendEntries=entries;
        if(entries.stream().noneMatch(e -> e.marker==legendHover)) { legendHover=null; }
        if(entries.stream().noneMatch(e -> e.marker==legendPinned)) { legendPinned=null; }
        long revision=++legendRevision;
        SwingUtilities.invokeLater(() -> {
            if(active && legend!=null && revision==legendRevision) { legend.replace(entries); }
        });
    }

    private Marker drawnMarker(Point end)
    {
        if (transformTarget != null)
        {
            Rectangle area=markerClip(transformTarget);
            if (start==null || area==null) { return null; }
            Rectangle original=transformTarget.onScreen(bank,itemArea,itemScroll);
            Rectangle changed=transformRectangle(original,start,end,resizing,area);
            if (Boolean.TRUE.equals(transformTarget.scrollWithItems) && config.snapToGrid())
            { changed=ItemAnchors.snap(changed,itemCells).intersection(area); }
            Marker result=new Marker(changed.x-area.x,changed.y-area.y,changed.width,changed.height);
            result.scrollWithItems=transformTarget.scrollWithItems;
            if (Boolean.TRUE.equals(result.scrollWithItems)) { result.y+=itemScroll; }
            return result;
        }
        Rectangle area=drawingItems ? itemArea : bank;
        if (start == null || area == null) { return null; }
        Marker marker=Marker.drag(start,end,area);
        if (drawingItems && config.snapToGrid() && marker.width > 0 && marker.height > 0)
        {
            Rectangle snapped=ItemAnchors.snap(marker.onScreen(area),itemCells).intersection(area);
            marker=new Marker(snapped.x-area.x,snapped.y-area.y,snapped.width,snapped.height);
        }
        marker.scrollWithItems=drawingItems;
        if (drawingItems) { marker.y+=itemScroll; }
        return marker;
    }

    static Rectangle transformRectangle(Rectangle original,Point start,Point end,boolean resize,Rectangle area)
    {
        Rectangle r=new Rectangle(original);
        if (resize)
        {
            r.width=Math.max(16,Math.min(area.x+area.width,end.x)-r.x);
            r.height=Math.max(16,Math.min(area.y+area.height,end.y)-r.y);
        }
        else
        {
            r.x=Math.max(area.x,Math.min(area.x+Math.max(0,area.width-r.width),r.x+end.x-start.x));
            r.y=Math.max(area.y,Math.min(area.y+Math.max(0,area.height-r.height),r.y+end.y-start.y));
        }
        return r;
    }

    private Rectangle markerClip(Marker marker)
    {
        return marker.linked() || Boolean.TRUE.equals(marker.scrollWithItems) ? itemArea : bank;
    }

    private List<Rectangle> markerRegions(Marker marker)
    {
        if (marker.linked()) { return ItemAnchors.regions(marker.linkedItemIds,itemCells); }
        Rectangle bounds=marker.onScreen(bank,itemArea,itemScroll);
        return bounds == null ? java.util.Collections.emptyList() : java.util.Collections.singletonList(bounds);
    }

    static Font labelFont(Marker marker)
    {
        Font base;
        String family=marker.labelFont == null ? "RuneScape" : marker.labelFont;
        if (family.equals("RuneScape")) { base=FontManager.getRunescapeFont(); }
        else if (family.equals("RuneScape Bold")) { base=FontManager.getRunescapeBoldFont(); }
        else { base=new Font(family,Font.PLAIN,marker.fontSize()); }
        return base.deriveFont((float)marker.fontSize());
    }

    final class MarkerMouse extends MouseAdapter
    {
        @Override public MouseEvent mousePressed(MouseEvent e)
        {
            synchronized (WindowMarkersPlugin.this)
            {
                suppressClick = false;
                if (!active || !armed || bank == null || !currentLayout()) { return e; }
                if (e.getButton() == MouseEvent.BUTTON3)
                {
                    captured = true; cancel(); e.consume(); return e;
                }
                if (e.getButton() == MouseEvent.BUTTON1 && bank.contains(e.getPoint()))
                {
                    if (transformTarget != null)
                    {
                        captured=true; e.consume();
                        Rectangle target=transformTarget.onScreen(bank,itemArea,itemScroll);
                        if (target != null && target.contains(e.getPoint())) { start=e.getPoint(); }
                        else { setStatus("Start the drag inside the selected area."); }
                        return e;
                    }
                    drawingItems=itemArea != null && itemArea.contains(e.getPoint());
                    start = e.getPoint(); captured = true; e.consume();
                }
            }
            return e;
        }
        @Override public MouseEvent mouseDragged(MouseEvent e)
        {
            synchronized (WindowMarkersPlugin.this)
            {
                if (captured)
                {
                    e.consume();
                    if (start != null && bank != null) { preview = drawnMarker(e.getPoint()); }
                }
            }
            return e;
        }
        @Override public MouseEvent mouseReleased(MouseEvent e)
        {
            synchronized (WindowMarkersPlugin.this)
            {
                if (!captured) { return e; }
                e.consume(); captured = false; suppressClick = true;
                Marker drawn = start == null || bank == null ? null : drawnMarker(e.getPoint());
                Marker target=transformTarget;
                cancel();
                if (drawn != null && drawn.valid())
                {
                    if (target != null)
                    {
                        if (!currentLayout() || !markers.contains(target)) { return e; }
                        target.x=drawn.x; target.y=drawn.y; target.width=drawn.width; target.height=drawn.height;
                        save(); setStatus("Area updated."); return e;
                    }
                    if (config.linkNewMarkers() && itemArea != null)
                    { drawn.linkedItemIds=ItemAnchors.enclosed(drawn.onScreen(bank,itemArea,itemScroll),itemCells,itemArea); }
                    markers.add(drawn); save();
                    setStatus(config.linkNewMarkers() && !drawn.linked() ? "No enclosed items; saved an area marker." : "Marker saved.");
                }
                else if (drawn != null) { setStatus("Draw at least 16 × 16 pixels."); }
            }
            return e;
        }
        @Override public MouseEvent mouseClicked(MouseEvent e)
        {
            synchronized (WindowMarkersPlugin.this) { if (suppressClick) { e.consume(); suppressClick = false; } }
            return e;
        }
    }

    private Marker highlightedMarker()
    {
        if(config.hoverHighlight() && markers.contains(legendHover)) { return legendHover; }
        if(markers.contains(legendPinned)) { return legendPinned; }
        return config.hoverHighlight() ? hoveredBankMarker() : null;
    }

    private Marker hoveredBankMarker()
    {
        net.runelite.api.Point mouse=client.getMouseCanvasPosition();
        if(mouse==null || bank==null || client.isMenuOpen() || armed) { return null; }
        Point point=new Point(mouse.getX(),mouse.getY());
        for(Marker marker:markers)
        {
            Rectangle clip=markerClip(marker);
            if(clip!=null && clip.contains(point))
                for(Rectangle r:markerRegions(marker)) if(r!=null && r.contains(point)) { return marker; }
        }
        return null;
    }

    final class MarkerOverlay extends Overlay
    {
        MarkerOverlay()
        {
            setPosition(OverlayPosition.DYNAMIC);
            setLayer(OverlayLayer.MANUAL);
            drawAfterInterface(InterfaceID.BANKMAIN);
        }
        @Override public Dimension render(Graphics2D graphics)
        {
            synchronized (WindowMarkersPlugin.this)
            {
                if (bank == null) { return null; }
                Graphics2D g = (Graphics2D) graphics.create();
                try
                {
                    g.clip(bank);
                    Marker focused=highlightedMarker();
                    Marker hovered=hoveredBankMarker();
                    if(config.visible() && hovered!=null && tooltipManager!=null)
                    { tooltipManager.add(new net.runelite.client.ui.overlay.tooltip.Tooltip(net.runelite.client.util.Text.escapeJagex(hovered.labelText()))); }
                    if (config.visible())
                    {
                        for (Marker marker : markers)
                        {
                          for (Rectangle r : markerRegions(marker))
                          {
                            Rectangle clip=markerClip(marker);
                            if (r == null || clip == null) { continue; }
                            Shape previousClip=g.getClip(); g.clip(clip);
                            g.setComposite(AlphaComposite.SrcOver.derive(focused!=null && focused!=marker ? 0.18f : 1f));
                            borders.draw(g,r,marker.style == null ? config.style() : marker.style,
                                marker.borderArgb == null ? config.color() : new Color(marker.borderArgb,true),marker.borderArgb != null);
                            if(focused==marker) { BorderRenderer.plain(g,r,Color.WHITE); }
                            g.setComposite(AlphaComposite.SrcOver);
                            g.setClip(previousClip);
                          }
                        }
                    }
                    if (config.visible())
                    {
                        List<Rectangle> occupiedLabels=new ArrayList<>();
                        for (ItemAnchors.Cell cell : itemCells) { occupiedLabels.add(new Rectangle(cell.grid)); }
                        for (Marker marker : markers)
                        {
                            Rectangle clip=markerClip(marker);
                            if (clip==null || marker.labelText().isEmpty() || config.bankLabels()==WindowMarkersConfig.BankLabels.OFF || (focused!=null && focused!=marker)) { continue; }
                            List<Rectangle> regions=new ArrayList<>(markerRegions(marker));
                            regions.removeIf(r -> r==null || !r.intersects(clip));
                            regions.sort(java.util.Comparator.comparingInt((Rectangle r) -> r.width).reversed());
                            for (Rectangle r : regions)
                            {
                                Graphics2D text=(Graphics2D)g.create();
                                boolean drawn=false;
                                try
                                {
                                    text.clip(clip);
                                    boolean mini=config.bankLabels()==WindowMarkersConfig.BankLabels.MINI;
                                    text.setFont(mini ? new Font("Arial",Font.PLAIN,10) : labelFont(marker));
                                    FontMetrics metrics=text.getFontMetrics();
                                    String title=MarkerPresentation.fit(config.bankLabels()==WindowMarkersConfig.BankLabels.MINI ? MarkerPresentation.miniTitle(marker.labelText()) : MarkerPresentation.displayTitle(marker.labelText()),metrics,Math.min(config.bankLabels()==WindowMarkersConfig.BankLabels.MINI ? 90 : 176,clip.width-10));
                                    if (title.isEmpty()) { continue; }
                                    Rectangle badge=MarkerPresentation.place(r,clip,metrics.stringWidth(title)+10,metrics.getHeight()+4,marker.labelPlacement,occupiedLabels);
                                    if (badge==null) { continue; }
                                    text.setColor(new Color(12,12,12,235)); text.fillRoundRect(badge.x,badge.y,badge.width,badge.height,5,5);
                                    text.setColor(marker.borderArgb==null ? config.color() : new Color(marker.borderArgb,true));
                                    text.drawRoundRect(badge.x,badge.y,badge.width-1,badge.height-1,5,5);
                                    text.setColor(marker.labelArgb==null ? Color.WHITE : new Color(marker.labelArgb,true));
                                    text.drawString(title,badge.x+5,badge.y+2+metrics.getAscent()); drawn=true;
                                }
                                finally { text.dispose(); }
                                if (drawn && (config.bankLabels()==WindowMarkersConfig.BankLabels.MINI || Boolean.TRUE.equals(marker.labelOnce))) { break; }
                            }
                        }
                    }
                    if (preview != null && markerClip(preview) != null)
                    { g.clip(markerClip(preview)); BorderRenderer.plain(g, preview.onScreen(bank,itemArea,itemScroll), config.color()); }
                }
                finally { g.dispose(); }
            }
            return null;
        }
    }

    static int labelBaseline(Marker marker,Rectangle r,FontMetrics metrics)
    {
        if (marker.labelPlacement == Marker.LabelPlacement.ABOVE) { return r.y-4-metrics.getDescent(); }
        if (marker.labelPlacement == Marker.LabelPlacement.BELOW) { return r.y+r.height+4+metrics.getAscent(); }
        return r.y+4+metrics.getAscent();
    }

    final class BackgroundOverlay extends Overlay
    {
        BackgroundOverlay()
        {
            setPosition(OverlayPosition.DYNAMIC);
            setLayer(OverlayLayer.MANUAL);
            // The bank frame is rendered before the item-container sibling.
            drawAfterLayer(InterfaceID.Bankmain.FRAME);
        }
        @Override public Dimension render(Graphics2D graphics)
        {
            synchronized (WindowMarkersPlugin.this)
            {
                if (bank == null || !config.visible()) { return null; }
                Graphics2D g=(Graphics2D)graphics.create();
                try
                {
                    g.clip(bank);
                    Marker focused=highlightedMarker();
                    for (Marker marker : markers)
                    {
                        if (marker.backgroundArgb == null) { continue; }
                      for (Rectangle r : markerRegions(marker))
                      {
                        Rectangle clip=markerClip(marker);
                        if (r == null || clip == null) { continue; }
                        Shape previousClip=g.getClip(); g.clip(clip);
                        g.setComposite(AlphaComposite.SrcOver.derive(focused!=null && focused!=marker ? 0.15f : config.quietFills() ? 0.45f : 1f));
                        g.setColor(new Color(marker.backgroundArgb,true)); g.fill(r); g.setClip(previousClip);
                      }
                    }
                }
                finally { g.dispose(); }
            }
            return null;
        }
    }
}


