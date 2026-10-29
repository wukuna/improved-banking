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

@PluginDescriptor(name="Improved Banking", description = "Beta bank organization with Wiki categories, editable gear views, custom groups, junk-use notes and screenshots. Optional GE search and prices. Rapid Wiki updates; feedback welcome.", conflicts={"Window Markers","Flexible Bank","Bank Screenshot"})
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
    @Inject BankItemSearch itemSearch;
    private CustomPresets customPresets=new CustomPresets();
    private JComboBox<CustomPresets.Preset> customPicker;
    private List<BankCatalogue.Choice> catalogueChoices=java.util.Collections.emptyList();
    private long catalogueRevision;
    private final CatalogueWorkQueue catalogueWork = new CatalogueWorkQueue();
    private List<ItemAnchors.Cell> legendCells=new ArrayList<>();
    private Boolean forceLink;
    private Marker appendTarget;
    private String pendingGroupName;
    @Inject net.runelite.client.callback.ClientThread clientThread;
    @Inject BankLayoutController bankLayout;
    @Inject BankCapture bankCapture;
    @Inject BankScreenshotButton screenshotButton;
    @Inject NativeBankSearch nativeSearch;
    @Inject BankSearchView searchView;
    @Inject BankCataloguePicker bankPicker;
    @Inject GeCatalogueSearch geSearch;
    @Inject ReleaseNotesNotice releaseNotes;
    private List<ItemAnchors.Cell> itemCells = new ArrayList<>();
    private final List<Rectangle> previewLabelObstacles=new ArrayList<>();
    private final LinkedMarkerRegions linkedRegions = new LinkedMarkerRegions();
    private final List<Marker> markers = new ArrayList<>();
    private Rectangle bank;
    private Rectangle itemArea;
    private int itemScroll;
    private boolean drawingItems;
    private Point start;
    private Marker preview;
    private boolean armed, active, suppressClick, captured;
    private long lifecycle;
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
    private JLabel geContextLabel, viewLabel;
    private JButton addViewButton, clearViewButton, saveViewButton, reviewItemsButton;
    private String viewSummary="";
    private JTextField viewSearch;
    private long viewRequest;
    @Inject net.runelite.client.plugins.banktags.TagManager bankTagManager;
    @Inject net.runelite.client.plugins.bank.BankSearch nativeBankLayout;
    private String legendTierKey="";
    private MarkerLegend legend;
    private List<MarkerLegend.Entry> legendEntries;
    private volatile long legendRevision;
    private volatile Marker legendHover, legendPinned;
    private volatile Marker lookupHighlight;
    private BankSearchIndex rankingIndex;
    @Inject net.runelite.client.ui.overlay.tooltip.TooltipManager tooltipManager;
    private final MarkerMouse mouse = new MarkerMouse();
    private final MarkerOverlay overlay = new MarkerOverlay();
    private final BackgroundOverlay backgroundOverlay = new BackgroundOverlay();

    @Provides WindowMarkersConfig provideConfig(ConfigManager manager) { return manager.getConfig(WindowMarkersConfig.class); }

    @Override protected synchronized void startUp()
    {
        active = true;
        final long startup = ++lifecycle;
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
        if(geSearch!=null) geSearch.start();
        if(releaseNotes!=null) releaseNotes.start();
        overlayManager.add(overlay);
        overlayManager.add(backgroundOverlay);
        mouseManager.registerMouseListener(mouse);
        SwingUtilities.invokeLater(() -> {
            synchronized (WindowMarkersPlugin.this)
            {
                if (!active || startup != lifecycle) { return; }
                PluginPanel panel = new PluginPanel() {};
                panel.setLayout(new BorderLayout(0, 10));
                JPanel controls = new MarkerForm();
                JPanel toolsPanel = new MarkerForm();
                JPanel viewsPanel = new MarkerForm();
                JPanel guidePanel = new MarkerForm();guidePanel.setVisible(false);
                controls.add(new JLabel("Improved Banking"));
                controls.add(new JLabel("<html>Saved per character and RuneLite profile.</html>"));
                status = new JLabel("Open your bank to begin.");
                status.setPreferredSize(new Dimension(210,48));
                controls.add(status);
                layoutPicker=new JComboBox<>();
                layoutPicker.setRenderer(PlainText.renderer());
                layoutPicker.addActionListener(e -> { if (!refreshingLayouts) { switchNamedLayout(layoutPicker.getSelectedIndex()); } });
                JPanel namedLayouts=new MarkerForm();
                namedLayouts.add(new JLabel("<html>Switch the whole collection of Legend groups.<br>Search views are saved in Legend.</html>"));
                namedLayouts.add(layoutPicker);
                JPanel layoutActions=new JPanel(new GridLayout(1,3,3,0));
                JButton newLayout=new JButton("New"), duplicate=new JButton("Copy"), rename=new JButton("Rename");
                newLayout.addActionListener(e -> nameLayout(false,false)); duplicate.addActionListener(e -> nameLayout(true,false));
                rename.addActionListener(e -> nameLayout(false,true));
                layoutActions.add(newLayout); layoutActions.add(duplicate); layoutActions.add(rename); namedLayouts.add(layoutActions);
                addDisclosure(toolsPanel,"Marker layouts…",namedLayouts);
                JPanel historyActions=new JPanel(new GridLayout(1,2,3,0));
                JButton undo=new JButton("Undo"), redo=new JButton("Redo");
                undo.addActionListener(e -> restoreHistory(false)); redo.addActionListener(e -> restoreHistory(true));
                historyActions.add(undo); historyActions.add(redo); controls.add(historyActions);
                JButton layoutControls=new JButton("Bank size and tabs");
                layoutControls.addActionListener(e -> editBankLayout()); toolsPanel.add(new JLabel("Bank tools")); toolsPanel.add(layoutControls);
                toolsPanel.add(new JLabel("<html>Alt-drag bank buttons to arrange them.</html>"));
                JButton resetButtons=new JButton("Reset bank buttons");
                resetButtons.addActionListener(e -> {
                    configs.setConfiguration("windowmarkers","bankControlLayout","");
                    configs.setConfiguration("windowmarkers","customBankControls",false);
                    setStatus("Bank button arrangement reset.");
                });toolsPanel.add(resetButtons);
                JButton screenshot=new JButton("Save bank screenshot");
                screenshot.addActionListener(e -> clientThread.invokeLater(this::captureBank)); toolsPanel.add(screenshot);
                JPanel customPanel=new MarkerForm();
                customPanel.add(new JLabel("<html>Create named item groups or area markers.<br>Manage existing groups in Legend.</html>"));
                addButton(customPanel,"Create item group…",() -> editGroupItems(null));
                addButton(customPanel,"Draw item group…",() -> armGroup(null));
                addButton(customPanel,"Draw area marker",() -> { synchronized(this) { arm(); forceLink=false; } });
                addButton(customPanel,"Cancel drawing",this::cancel);
                JCheckBox snap=new JCheckBox("Snap areas to item grid",config.snapToGrid());
                snap.addActionListener(e -> configs.setConfiguration("windowmarkers","snapToGrid",snap.isSelected())); customPanel.add(snap);
                viewLabel=new JLabel("All bank items");viewSummary="";viewsPanel.add(viewLabel);
                addViewButton=addButton(viewsPanel,"Add to Legend",this::addViewToLegend);
                clearViewButton=addButton(viewsPanel,"Clear bank filter",this::clearBankView);
                reviewItemsButton=addButton(viewsPanel,"Item uses and reasons…",this::reviewItemUses);
                reviewItemsButton.setVisible(false);
                JPanel savedViews=new MarkerForm();
                saveViewButton=addButton(savedViews,"Save as custom view…",this::saveCurrentBankView);
                addViewButton.setEnabled(false);clearViewButton.setEnabled(false);saveViewButton.setEnabled(false);
                customPicker=new JComboBox<>();customPicker.setRenderer(PlainText.renderer());savedViews.add(customPicker);
                addButton(savedViews,"Open saved view",() -> {
                    CustomPresets.Preset saved=(CustomPresets.Preset)customPicker.getSelectedItem();
                    if(saved!=null) chooseCatalogue(new BankCatalogue.Choice(BankCatalogue.Kind.CUSTOM_PRESET,saved.name,saved),true);
                });
                addButton(savedViews,"Edit saved view…",() -> {
                    CustomPresets.Preset saved=(CustomPresets.Preset)customPicker.getSelectedItem();
                    if(saved!=null) editSavedView(saved);
                });
                addButton(savedViews,"Delete saved view…",() -> {
                    CustomPresets.Preset saved=(CustomPresets.Preset)customPicker.getSelectedItem();
                    if(saved!=null) deleteCustomPreset(saved);
                });
                addButton(toolsPanel,"Add Bank Tags tag…",this::addBankTag);
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
                geContextLabel=new JLabel();
                guidePanel.add(geContextLabel);
                refreshGeContext();
                JButton geGuide=new JButton("Choose bank / GE guide");
                geGuide.addActionListener(e -> {
                    ActivitySetups.Setup setup=ActivitySetupPicker.choose(panel,true);
                    if(setup!=null) {
                        configs.setConfiguration("windowmarkers","geGuideContext",setup.id);
                        refreshGeContext();
                        setStatus("Bank / GE guide selected."); refreshOpenView();
                    }
                });
                guidePanel.add(geGuide);
                JButton clearGeGuide=new JButton("Clear bank / GE guide");
                clearGeGuide.addActionListener(e -> {
                    configs.unsetConfiguration("windowmarkers","geGuideContext");
                    refreshGeContext();
                    setStatus("Using matching guides."); refreshOpenView();
                });
                guidePanel.add(clearGeGuide);
                addButton(guidePanel,"Choose inventory example…",() -> {
                    ActivitySetups.Setup setup=ActivitySetupPicker.chooseInventory(panel);
                    if(setup!=null) { configs.setConfiguration("windowmarkers","bankInventoryContext",setup.id); refreshOpenView(); }
                });
                addButton(guidePanel,"Automatic inventory example",() -> { configs.unsetConfiguration("windowmarkers","bankInventoryContext");refreshOpenView(); });
                addDisclosure(savedViews,"Guide choices…",guidePanel);
                addDisclosure(viewsPanel,"Saved views…",savedViews);
                rows=new DefaultListModel<>();
                legend=new MarkerLegend(m -> legendHover=m,m -> { legendPinned=m; if(m!=null) lookupHighlight=null; },new MarkerLegend.Actions() {
                    public void edit(Marker m) { editMarker(m); }
                    public void items(Marker m) { editGroupItems(m); }
                    public void delete(List<Marker> selected) { synchronized(WindowMarkersPlugin.this) {
                        removeMarkers(selected.stream().mapToInt(markers::indexOf).toArray(),false);
                    }}
                    public void clear(List<Marker> expected) { synchronized(WindowMarkersPlugin.this) {
                        if(markers.equals(expected)) removeMarkers(new int[0],true);
                        else setStatus("Layout changed. Review the groups before clearing.");
                    }}
                    public void browse(String query) {
                        browseCatalogue(query);
                    }
                    public void choose(BankCatalogue.Choice choice) { chooseCatalogue(choice,true); }
                    public void search(String query) { openQueryView(query,config.geGuideContext()); }
                    public void inspect() { inspectItems(); }
                    public void clearHighlight() { lookupHighlight=null; }
                    public void reorder(List<Marker> visible,List<Marker> selected,int index) { reorderGroups(visible,selected,index); }
                    public void transform(Marker m,boolean resize) { synchronized(WindowMarkersPlugin.this) { armTransform(markers.indexOf(m),resize); } }
                    public void drawInto(Marker m) { armGroup(m); }
                    public void merge() { mergeGroups(); }
                    public void saveGroups() { saveCustomPreset(); }
                    public void organize() { autoCategorize(); }
                });
                viewSearch=legend.search;
                legend.viewControls(viewsPanel);
                JTabbedPane tabs=new JTabbedPane();
                tabs.addTab("Legend",legend); tabs.addTab("Custom",customPanel); tabs.addTab("Tools",toolsPanel);
                tabs.addChangeListener(e -> { legendHover=null; });
                panel.add(controls,BorderLayout.NORTH); panel.add(tabs,BorderLayout.CENTER);
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
        if(releaseNotes!=null) releaseNotes.stop();
        active = false; lifecycle++; catalogueRevision++;
        if(geSearch!=null) geSearch.stop();
        lookupHighlight=null;
        resetBankContext(true);
        if(legend!=null) SwingUtilities.invokeLater(legend::close);
        if(bankLayout!=null) { bankLayout.shutDown(); }
        cancel(); bank = null; captured = false; suppressClick = false;
        mouseManager.unregisterMouseListener(mouse);
        overlayManager.remove(overlay);
        overlayManager.remove(backgroundOverlay);
        borders.clear(); markers.clear(); linkedRegions.clear();
        NavigationButton old = navigation;
        navigation = null;
        if (old != null) { SwingUtilities.invokeLater(() -> toolbar.removeNavigation(old)); }
    }

    @Subscribe public synchronized void onProfileChanged(ProfileChanged event) { cancel(); bank = null; load(); }
    @Subscribe public synchronized void onRuneScapeProfileChanged(RuneScapeProfileChanged event)
    {
        cancel(); bank = null; load();
    }

    private void refreshGeContext()
    {
        if(config==null || geContextLabel==null) return;
        String chosen=config.geGuideContext();
        String name=ActivitySetups.all().stream().filter(s->s.id.equals(chosen))
            .map(s->s.activity+" / "+s.label).findFirst().orElse("All matching setups");
        String text="<html>Bank / GE guide: "+name.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;")+"</html>";
        JLabel label=geContextLabel;
        SwingUtilities.invokeLater(() -> { if(geContextLabel==label) label.setText(text); });
    }

    private void load()
    {
        linkedRegions.clear();
        refreshGeContext();
        resetBankContext(false);
        legendHover=null; legendPinned=null; lookupHighlight=null; legendCells.clear(); catalogueRevision++;
        if(legend!=null) SwingUtilities.invokeLater(legend::resetContext);
        customPresets=new CustomPresets();
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
                String templates=configs.getConfiguration("windowmarkers",accountKey,baseLayoutKey()+"_customPresets");
                CustomPresets library=templates==null?null:gson.fromJson(templates,CustomPresets.class);
                if(library!=null) { library.normalize(); customPresets=library; }
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
        final String owner=accountKey,layout=layoutKey;final long started=lifecycle;
        boolean queued=bankCapture.save(new ArrayList<>(markers),() -> {
            synchronized(WindowMarkersPlugin.this) { return started==lifecycle && context(owner,layout); }
        },saved -> {
            synchronized(WindowMarkersPlugin.this) {
                if(started==lifecycle && context(owner,layout))
                    setStatus(saved?"Saved to RuneLite screenshots / bank-markers.":"Screenshot failed: item images could not load. Try again.");
            }
        });
        setStatus(queued ? "Preparing bank screenshot…" : "Open a populated bank tab; wait for any pending screenshot.");
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

    private void autoCategorize() { autoCategorize(AutoCategories.Preset.ALL_CATEGORIES); }

    private void autoCategorize(AutoCategories.Preset initial)
    { autoCategorize(initial,false); }

    private void autoCategorize(AutoCategories.Preset initial,boolean showView)
    {
        JComboBox<AutoCategories.Preset> presets=new JComboBox<>(AutoCategories.Preset.values());
        presets.setSelectedItem(initial);
        JCheckBox excludeWorse=new JCheckBox("Exclude worse items (verified raid gear and tools)",false);
        presets.addActionListener(e -> excludeWorse.setEnabled(presets.getSelectedItem()!=AutoCategories.Preset.WIKI_ACTIVITY_SETUP));
        JPanel settings=new MarkerForm();
        settings.add(presets); settings.add(excludeWorse);
        settings.add(new JLabel("<html>Uses your real skill levels and owned replacements.<br>Preserves set, clue, quest and special-use alternatives.<br>Exclusions are listed for review before any markers change.</html>"));
        if (!MarkerDialogs.confirm(navigation==null ? null : navigation.getPanel(),settings,"Auto-categorize",560,280)) { return; }
        AutoCategories.Preset preset=(AutoCategories.Preset)presets.getSelectedItem();
        if(preset==AutoCategories.Preset.WIKI_ACTIVITY_SETUP) {
            List<BankCatalogue.Choice> activities=ActivityMenu.activities();
            BankCatalogue.Choice choice=CatalogueBrowser.choose(parent(),activities,"");
            if(choice!=null) chooseCatalogue(choice,showView);
            return;
        }
        categorize(preset,null,excludeWorse.isSelected(),showView);
    }

    private void categorize(AutoCategories.Preset preset,ActivitySetups.Setup setup,boolean filterWorse)
    { categorize(preset,setup,filterWorse,false); }

    private void categorize(AutoCategories.Preset preset,ActivitySetups.Setup setup,boolean filterWorse,boolean showView)
    {
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
                List<AutoCategories.Item> owned=bankFacts();
                java.util.Map<Integer,String> exclusions=filterWorse ? LowerTierFilter.exclusions(owned,
                    client.getRealSkillLevel(net.runelite.api.Skill.MINING),client.getRealSkillLevel(net.runelite.api.Skill.WOODCUTTING),client.getRealSkillLevel(net.runelite.api.Skill.FISHING)) : java.util.Collections.emptyMap();
                owned.removeIf(i -> exclusions.containsKey(i.id));
                java.util.Map<String,List<AutoCategories.Item>> groups=setup==null ? AutoCategories.suggest(preset,owned) : setup.suggest(owned);
                if (filterWorse)
                {
                    net.runelite.api.Skill[] skills={net.runelite.api.Skill.ATTACK,net.runelite.api.Skill.STRENGTH,net.runelite.api.Skill.DEFENCE,net.runelite.api.Skill.RANGED,net.runelite.api.Skill.MAGIC,net.runelite.api.Skill.PRAYER,net.runelite.api.Skill.HITPOINTS};
                    int[] levels=new int[skills.length];
                    for(int i=0;i<skills.length;i++) { levels[i]=client.getRealSkillLevel(skills[i]); }
                    boolean ancientRingsUnlocked=net.runelite.api.Quest.DESERT_TREASURE_II__THE_FALLEN_EMPIRE.getState(client)==net.runelite.api.QuestState.FINISHED;
                    boolean allSkillsMaxed=java.util.Arrays.stream(net.runelite.api.Skill.values())
                        .filter(skill -> skill!=net.runelite.api.Skill.OVERALL)
                        .allMatch(skill -> client.getRealSkillLevel(skill)>=99);
                    RaidGearFilter.filter(preset,groups,levels,ancientRingsUnlocked,allSkillsMaxed,exclusions);
                }
                SwingUtilities.invokeLater(() -> reviewCategories(owner,layout,groups,preset,exclusions,setup,false,showView));
            }
        });
    }

    private void reviewCategories(String owner,String layout,java.util.Map<String,List<AutoCategories.Item>> groups,AutoCategories.Preset preset,java.util.Map<Integer,String> exclusions,ActivitySetups.Setup setup)
    { reviewCategories(owner,layout,groups,preset,exclusions,setup,false); }

    private void reviewCategories(String owner,String layout,java.util.Map<String,List<AutoCategories.Item>> groups,AutoCategories.Preset preset,java.util.Map<Integer,String> exclusions,ActivitySetups.Setup setup,boolean explicitCategory)
    { reviewCategories(owner,layout,groups,preset,exclusions,setup,explicitCategory,false); }

    private void reviewCategories(String owner,String layout,java.util.Map<String,List<AutoCategories.Item>> groups,AutoCategories.Preset preset,java.util.Map<Integer,String> exclusions,ActivitySetups.Setup setup,boolean explicitCategory,boolean requestedView)
    {
        if (groups.isEmpty()) { setStatus("No owned items matched this preset."); return; }
        JPanel panel=new MarkerForm();
        panel.add(new JLabel("<html>Wiki classifications describe uses, not one combined loadout.<br>Outfit groups include owned pieces; partial ownership does<br>not activate a full-set bonus. Tools are grouped separately.<br>Raid lists depend on role, spellbook, charges and invocations.<br>Unresolved items stay in an unchecked review group.</html>"));
        if (setup!=null && !setup.notes.isEmpty())
        {
            JTextArea notes=new JTextArea(setup.notes);
            notes.setEditable(false); notes.setLineWrap(true); notes.setWrapStyleWord(true); notes.setColumns(42);
            panel.add(notes);
        }
        if (!exclusions.isEmpty())
        {
            JTextArea excluded=new JTextArea("Excluded lower-tier items:\n"+String.join("\n",exclusions.values()));
            excluded.setEditable(false); excluded.setLineWrap(true); excluded.setWrapStyleWord(true); excluded.setColumns(42);
            panel.add(excluded);
        }
        JButton source=new JButton("Open Wiki source");
        source.addActionListener(e -> net.runelite.client.util.LinkBrowser.browse(setup==null ? WikiCatalogue.source(preset) : setup.source));
        panel.add(source);
        JCheckBox includeGrouped=new JCheckBox("Include items already in other groups",explicitCategory);
        panel.add(includeGrouped);
        boolean slotView=(setup!=null || SetupItemLayout.activityPreset(preset)) && config.setupSearchLayout();
        JCheckBox showView=new JCheckBox("Show selected groups as a temporary bank view",requestedView || slotView);
        panel.add(showView);
        if(slotView) panel.add(new JLabel("<html>View groups equipment by slot, then tools and supplies.<br>Save it from the bank dropdown. Your bank order stays unchanged.</html>"));
        panel.add(new JLabel("<html>Including grouped items keeps their existing groups too.<br>To replace old groups instead, check their names below.<br>Replacing removes the entire checked marker. Undo restores it.</html>"));
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
        
        if (!MarkerDialogs.confirm(navigation==null ? null : navigation.getPanel(),panel,"Review "+(setup==null ? preset : setup.activity+" — "+setup.label),620,620)) { return; }
        synchronized (this)
        {
            if (!active || !owner.equals(accountKey) || !layout.equals(layoutKey) || !currentLayout()) { setStatus("Layout changed; run categorization again."); return; }
            boolean anySelected=choices.keySet().stream().anyMatch(JCheckBox::isSelected);
            if (!anySelected) { setStatus("No groups selected; existing markers kept."); return; }
            cancel();
            int replaced=0;
            for (java.util.Map.Entry<JCheckBox,Marker> entry : replacements.entrySet())
            { if (entry.getKey().isSelected() && markers.remove(entry.getValue())) { replaced++; } }
            java.util.Map<String,List<AutoCategories.Item>> selected=new java.util.LinkedHashMap<>();
            choices.forEach((box,items) -> { if(box.isSelected()) selected.put(box.getText(),items); });
            String presetName=setup==null?preset.toString():setup.activity+" — "+setup.label;
            CategoryGroups.Result result=CategoryGroups.add(markers,selected,presetName,includeGrouped.isSelected(),config.color(),MAX_MARKERS);
            boolean guideChanged=false;
            for(Marker group:result.selected) {
                String previous=group.rankKey();
                if(setup!=null) { group.guideQuery=setup.activity; group.guideContext=setup.id; }
                else if(SetupItemLayout.activityPreset(preset)) { group.guideQuery=preset.toString().split(" — ",2)[0]; group.guideContext=""; }
                guideChanged|=!previous.equals(group.rankKey());
            }
            if(result.added>0 || replaced>0 || guideChanged) save();
            if(showView.isSelected()) {
                // Keep the exact colours/labels the reviewed groups use in Legend.
                // Skipped groups are not silently saved under different colours.
                if(!result.selected.isEmpty()) showBankView(presetName,result.selected,slotView,owner,layout);
            }
            if(!result.selected.isEmpty()) SwingUtilities.invokeLater(() -> {
                synchronized(WindowMarkersPlugin.this) { if(context(owner,layout)) legend.showGroups(result.selected); }
            });
            setStatus(result.added+" groups added; "+replaced+" replaced; "+result.existing+" already present."
                +(result.skipped>0?" "+result.skipped+" skipped because their items are already grouped. Enable Include items already in other groups to keep both uses.":"")
                +(result.limited>0?" "+result.limited+" skipped at the "+MAX_MARKERS+"-marker limit.":"")
                +" Undo restores the previous layout.");
        }
    }

    private static JButton addButton(JPanel panel,String title,Runnable action)
    { JButton button=new JButton(title); button.addActionListener(e -> action.run()); panel.add(button);return button; }

    private static void addDisclosure(JPanel parent,String title,JPanel contents)
    {
        contents.setVisible(false);
        JToggleButton toggle=new JToggleButton(title);
        toggle.addActionListener(e -> { contents.setVisible(toggle.isSelected());parent.revalidate();parent.repaint(); });
        parent.add(toggle);parent.add(contents);
    }

    private Component parent() { return navigation==null?null:navigation.getPanel(); }

    private AutoCategories.Item itemFact(int id)
    {
        var definition=itemManager.getItemComposition(id);
        var stats=itemManager.getItemStats(id); var eq=stats==null?null:stats.getEquipment();
        return new AutoCategories.Item(id,definition.getName(),stats!=null && stats.isEquipable(),definition.getInventoryActions(),
            eq==null?0:Math.max(eq.getAstab(),Math.max(eq.getAslash(),eq.getAcrush()))+eq.getStr(),
            eq==null?0:eq.getArange()+eq.getRstr(),eq==null?0:eq.getAmagic()+(int)eq.getMdmg(),
            eq==null?-2:eq.getSlot());
    }

    private List<AutoCategories.Item> bankFacts()
    {
        java.util.Map<Integer,AutoCategories.Item> facts=new java.util.LinkedHashMap<>();
        var container=client.getItemContainer(net.runelite.api.gameval.InventoryID.BANK);
        if(container!=null) for(var item:container.getItems()) {
            if(item.getId()<0 || item.getQuantity()<=0 || item.getId()==6512) continue;
            int id=itemManager.canonicalize(item.getId()); facts.putIfAbsent(id,itemFact(id));
        }
        return new ArrayList<>(facts.values());
    }

    @Subscribe public synchronized void onItemContainerChanged(net.runelite.api.events.ItemContainerChanged event)
    { if(event.getContainerId()==net.runelite.api.gameval.InventoryID.BANK) reloadCatalogue(); }

    private void reloadCatalogue()
    {
        if(!active || clientThread==null || itemManager==null) return;
        final long revision=++catalogueRevision;
        clientThread.invokeLater(() -> {
            final List<AutoCategories.Item> owned;
            final List<Marker> saved;
            final CustomPresets templates;
            synchronized(this) {
                if(!active || revision!=catalogueRevision) return;
                owned=bankFacts(); saved=new ArrayList<>();
                for(Marker m:markers) saved.add(gson.fromJson(gson.toJson(m),Marker.class));
                templates=gson.fromJson(gson.toJson(customPresets),CustomPresets.class);
            }
            final int baseHitpoints=client.getRealSkillLevel(net.runelite.api.Skill.HITPOINTS);
            final BankCleanup.Progress progress=BankCleanup.Progress.capture(client);
            final java.util.Set<Integer> tradeable=new java.util.HashSet<>();
            for(AutoCategories.Item item:owned) if(itemManager.getItemComposition(item.id).isGeTradeable()) tradeable.add(item.id);
            catalogueWork.submit(new SwingWorker<List<BankCatalogue.Choice>,Void>() {
                private BankSearchIndex searchIndex;
                protected List<BankCatalogue.Choice> doInBackground() {
                    searchIndex=new BankSearchIndex(owned,saved,templates,baseHitpoints,tradeable,progress);
                    return BankCatalogue.choices(owned,saved,templates);
                }
                protected void done() {
                    try { List<BankCatalogue.Choice> choices=get(); synchronized(WindowMarkersPlugin.this) {
                        if(!active || revision!=catalogueRevision) return;
                        // Saved-group choices must refer to current identities, not the worker's snapshots.
                        choices.removeIf(c -> c.kind==BankCatalogue.Kind.SAVED_GROUP || c.kind==BankCatalogue.Kind.CUSTOM_PRESET);
                        for(Marker m:markers) choices.add(new BankCatalogue.Choice(BankCatalogue.Kind.SAVED_GROUP,m.labelText().isEmpty()?"Unnamed group":m.labelText(),m));
                        for(CustomPresets.Preset p:customPresets.presets) choices.add(new BankCatalogue.Choice(BankCatalogue.Kind.CUSTOM_PRESET,p.name,p));
                        catalogueChoices=choices; rankingIndex=searchIndex; if(legend!=null) legend.catalogue(ActivityMenu.choices(choices));
                        if(nativeSearch!=null) nativeSearch.index(searchIndex);
                        clientThread.invokeLater(() -> { synchronized(WindowMarkersPlugin.this) {
                            if(active && revision==catalogueRevision && bank!=null && searchView!=null
                                && searchView.isActive() && BankCleanup.isQuery(searchView.name()))
                                openQueryView(searchView.name(),"");
                        }});
                    }} catch(Exception ex) {
                        synchronized(WindowMarkersPlugin.this) {
                            if(active && revision==catalogueRevision) setStatus("Catalogue search could not be loaded. Reopen the bank to retry.");
                        }
                    } finally { catalogueWork.finished(this); }
                }
            });
        });
    }

    private void browseCatalogue(String query)
    {
        List<BankCatalogue.Choice> choices; String owner,layout;
        synchronized(this) { choices=ActivityMenu.choices(catalogueChoices); owner=accountKey; layout=layoutKey; }
        BankCatalogue.Choice choice=CatalogueBrowser.choose(parent(),choices,query); if(choice==null) return;
        synchronized(this) { if(!context(owner,layout)) return; }
        chooseCatalogue(choice,true);
    }

    private void chooseCatalogue(BankCatalogue.Choice choice)
    { chooseCatalogue(choice,false); }

    private void chooseCatalogue(BankCatalogue.Choice choice,boolean showView)
    {
        final String owner,layout;
        synchronized(this) { if(!currentLayout()) { setStatus("Log in first."); return; } owner=accountKey; layout=layoutKey; }
        if(choice.kind==BankCatalogue.Kind.ACTIVITY) { openQueryView(choice.name,config.geGuideContext(),!showView);return; }
        if(showView) {
            if(choice.kind==BankCatalogue.Kind.CUSTOM_PRESET) { CustomPresets.Preset saved=(CustomPresets.Preset)choice.value; showBankView(saved.name,saved.groups,saved.groupBySlot,owner,layout);return; }
            if(choice.kind==BankCatalogue.Kind.WIKI_SETUP) { ActivitySetups.Setup setup=(ActivitySetups.Setup)choice.value;openQueryView(setup.activity,setup.id);return; }
            if(choice.kind==BankCatalogue.Kind.CATEGORY || choice.kind==BankCatalogue.Kind.PRESET) { openQueryView(choice.viewQuery(),config.geGuideContext());return; }
        }
        switch(choice.kind) {
            case PRESET: autoCategorize((AutoCategories.Preset)choice.value,showView); break;
            case WIKI_SETUP: categorize(AutoCategories.Preset.WIKI_ACTIVITY_SETUP,(ActivitySetups.Setup)choice.value,false,showView); break;
            case CUSTOM_PRESET: applyCustomPreset((CustomPresets.Preset)choice.value,choice.customGroup,showView); break;
            case SAVED_GROUP:
                if(showView) showBankView(choice.name,java.util.Collections.singletonList((Marker)choice.value),false,owner,layout);
                legend.search.setText(""); legend.scope.setSelectedIndex(1); legend.preset.setSelectedIndex(0); legend.filter();
                for(int i=0;i<legend.rows.size();i++) if(legend.rows.get(i).marker==choice.value) {
                    legend.list.setSelectedIndex(i); legend.list.ensureIndexIsVisible(i); break;
                }
                break;
            case CATEGORY:
                clientThread.invokeLater(() -> {
                    List<AutoCategories.Item> owned;
                    synchronized(this) { if(!context(owner,layout) || bank==null) { setStatus("Open your bank first."); return; } owned=bankFacts(); }
                    new SwingWorker<java.util.Map<String,List<AutoCategories.Item>>,Void>() {
                        protected java.util.Map<String,List<AutoCategories.Item>> doInBackground() {
                            return BankCatalogue.categoryGroups(choice,owned);
                        }
                        protected void done() { try { reviewCategories(owner,layout,get(),choice.presetScope==null?AutoCategories.Preset.ALL_CATEGORIES:choice.presetScope,java.util.Collections.emptyMap(),null,true,showView); }
                            catch(Exception ex) { setStatus("Could not load this category."); } }
                    }.execute();
                }); break;
        }
    }

    private boolean context(String owner,String layout)
    { return active && currentLayout() && java.util.Objects.equals(owner,accountKey) && java.util.Objects.equals(layout,layoutKey); }

    synchronized void reorderGroups(List<Marker> visible,List<Marker> selected,int index)
    {
        if(active && currentLayout() && GroupEdits.reorder(markers,visible,selected,index)) {
            cancel(); save(); setStatus("Legend order saved. Bank items stay in place. Undo restores the order.");
        }
    }

    private void editMarker(Marker marker)
    { edit(marker); }

    private void styleGroup(Marker marker,String name)
    {
        List<Color> colors=new ArrayList<>();
        for(Marker m:markers) colors.add(m.borderArgb==null?config.color():new Color(m.borderArgb,true));
        Color color=MarkerPresentation.nextColor(colors);
        marker.label=name; marker.preset="Custom"; marker.category=null; marker.labelOnce=true;
        marker.labelFont="Arial"; marker.labelSize=12; marker.labelPlacement=Marker.LabelPlacement.ABOVE;
        marker.style=WindowMarkersConfig.BorderStyle.PLAIN; marker.borderArgb=color.getRGB();
        marker.backgroundArgb=new Color(color.getRed(),color.getGreen(),color.getBlue(),65).getRGB();
        if(BankCleanup.isQuery(name)) BankCleanup.style(marker);
    }

    private String askName(String title,String initial)
    {
        JTextField field=new JTextField(initial==null?"":initial,30);
        if(!MarkerDialogs.confirm(parent(),field,title,440,190)) return null;
        String name=field.getText().trim(); if(name.isEmpty()) { setStatus("Enter a name for the group or preset."); return null; }
        return name.substring(0,Math.min(60,name.length()));
    }

    private void armGroup(Marker append)
    {
        String owner,layout;
        synchronized(this) { if(!currentLayout() || bank==null) { setStatus("Open your bank first."); return; } owner=accountKey; layout=layoutKey; }
        String name=append==null?askName("Name the item group","My Food"):append.labelText(); if(name==null) return;
        synchronized(this) {
            if(!context(owner,layout) || (append!=null && !markers.contains(append))) return;
            cancel(); if(append==null && markers.size()>=MAX_MARKERS) { setStatus("Marker limit reached."); return; }
            forceLink=true; appendTarget=append; pendingGroupName=name; armed=true;
            setStatus("Draw around items for "+name+". Add another bank selection to include other rows.");
        }
    }

    private void mergeGroups()
    {
        List<Marker> selected=legend.selected(); String owner,layout;
        synchronized(this) {
            if(!currentLayout() || selected.size()<2 || selected.stream().anyMatch(m -> !m.linked() || !markers.contains(m))) {
                setStatus("Select at least two linked groups in Legend to merge."); return;
            }
            owner=accountKey; layout=layoutKey;
        }
        String name=askName("Name the merged group",selected.get(0).labelText()); if(name==null) return;
        synchronized(this) {
            if(!context(owner,layout) || !markers.containsAll(selected)) return;
            Marker merged=gson.fromJson(gson.toJson(selected.get(0)),Marker.class); merged.linkedItemIds=GroupEdits.union(selected);
            if(merged.linkedItemIds.length>4000) { setStatus("A group can contain up to 4,000 item types."); return; }
            merged.label=name; merged.category=null; merged.preset="Custom"; merged.clearGuide();
            int index=selected.stream().mapToInt(markers::indexOf).min().getAsInt();
            cancel(); markers.removeAll(selected); markers.add(index,merged); save(); setStatus("Groups merged. Undo restores the separate groups.");
        }
    }

    /** Palette inputs are captured on the client thread; all Swing work stays on the EDT. */
    private void paletteInputs(int[] initial,java.util.function.BiConsumer<java.util.Set<Integer>,List<BankItemSearch.Choice>> action)
    {
        final String owner,layout;final long generation;
        synchronized(this) { owner=accountKey;layout=layoutKey;generation=lifecycle; }
        clientThread.invokeLater(() -> {
            java.util.Set<Integer> owned=new java.util.LinkedHashSet<>(); for(var fact:bankFacts()) owned.add(fact.id);
            List<BankItemSearch.Choice> selected=new ArrayList<>();
            int count=client.getItemCount();int[] ids=java.util.Arrays.stream(initial).filter(id->id>=0 && id<count).distinct().toArray();
            int[] cursor={0};
            clientThread.invokeLater(() -> {
                synchronized(this) { if(generation!=lifecycle || !context(owner,layout)) return true; }
                int end=Math.min(ids.length,cursor[0]+256);
                while(cursor[0]<end) {
                    int id=ids[cursor[0]++];var definition=itemManager.getItemComposition(id);if(definition==null) continue;
                    var c=new BankItemSearch.Choice(id,definition.getName());c.image=itemManager.getImage(id);selected.add(c);
                }
                if(cursor[0]<ids.length) return false;
                SwingUtilities.invokeLater(() -> { synchronized(WindowMarkersPlugin.this) {
                    if(generation!=lifecycle || !context(owner,layout)) return;
                } action.accept(owned,selected); });
                return true;
            });
        });
    }

    private void editSavedView(CustomPresets.Preset saved)
    {
        final String owner,layout;final CustomPresets.Preset draft;
        synchronized(this) {
            if(!currentLayout() || !customPresets.presets.contains(saved)) return;
            owner=accountKey;layout=layoutKey;draft=gson.fromJson(gson.toJson(saved),CustomPresets.Preset.class);
        }
        editViewDraft(saved,draft,owner,layout);
    }

    private void editViewDraft(CustomPresets.Preset saved,CustomPresets.Preset draft,String owner,String layout)
    {
        paletteInputs(GroupEdits.union(draft.groups),(owned,selected)-> {
            synchronized(this) { if(!context(owner,layout) || saved!=null && !customPresets.presets.contains(saved)) return; }
            CustomViewEditor editor=new CustomViewEditor(draft,itemSearch,owned,selected);
            boolean accepted;
            try { accepted=MarkerDialogs.confirm(parent(),editor,saved==null?"Save as custom view":"Edit custom view",960,760,"Save changes",()-> {
                if(!editor.valid()) return false;
                synchronized(WindowMarkersPlugin.this) {
                    if(!context(owner,layout) || saved!=null && !customPresets.presets.contains(saved)) { editor.error("The account or saved views changed. Cancel and reopen this editor.");return false; }
                    if(saved==null && customPresets.presets.size()>=CustomPresets.LIMIT) { editor.error("50-view limit reached. Remove a saved view first.");return false; }
                    if(customPresets.presets.stream().anyMatch(p->p!=saved && p.name.equalsIgnoreCase(draft.name))) { editor.error("That view name is already used. Choose another name.");return false; }
                    return true;
                }
            }); } finally { editor.close(); }
            if(!accepted) return;
            synchronized(this) {
                if(!context(owner,layout)) return;
                if(saved==null) customPresets.presets.add(draft);
                else {
                    int index=customPresets.presets.indexOf(saved);if(index<0) return;
                    customPresets.presets.set(index,draft);
                }
                savePresets();setStatus("Custom view saved. Existing Legend groups are unchanged.");
            }
            showBankView(draft.name,draft.groups,draft.groupBySlot,owner,layout);
        });
    }

    private void editGroupItems(Marker target)
    {
        final String owner,layout; final int[] initial;
        synchronized(this) {
            if(!currentLayout() || (target!=null && (!markers.contains(target) || !target.linked()))) return;
            owner=accountKey; layout=layoutKey; initial=target==null?new int[0]:target.linkedItemIds.clone();
        }
        paletteInputs(initial,(owned,selected) -> {
            synchronized(this) { if(!context(owner,layout)) return; }
            ItemPalette palette=new ItemPalette(itemSearch,owned,selected,4000);
            JTextField label=new JTextField(target==null?"My group":target.labelText());
            JPanel form=new JPanel(new BorderLayout(0,8));
            JPanel heading=new JPanel(new BorderLayout(6,0)); heading.add(new JLabel("Group name"),BorderLayout.WEST); heading.add(label,BorderLayout.CENTER);
            form.add(heading,BorderLayout.NORTH); form.add(palette,BorderLayout.CENTER);
            boolean accepted; try { accepted=MarkerDialogs.confirm(parent(),form,target==null?"Create item group":"Edit group items",740,680); }
            finally { palette.close(); }
            if(!accepted) return; int[] ids=palette.ids(); String name=label.getText().trim();
            if(ids.length==0 || name.isEmpty()) { setStatus("Choose at least one item and enter a group name."); return; }
            synchronized(this) {
                if(!context(owner,layout) || (target!=null && !markers.contains(target))) return;
                if(target==null && markers.size()>=MAX_MARKERS) { setStatus("Marker limit reached."); return; }
                cancel(); Marker group=target==null?new Marker(0,0,48,36):target;
                if(target==null) { styleGroup(group,name); markers.add(group); }
                group.label=name.substring(0,Math.min(60,name.length())); group.scrollWithItems=true; group.linkedItemIds=ids;
                group.category=null; group.preset="Custom"; group.clearGuide(); save(); setStatus("Item group saved. Manage its appearance in Legend.");
            }
        });
    }

    private void inspectItems()
    {
        final String owner,layout; synchronized(this) { if(!currentLayout()) { setStatus("Log in to look up item categories."); return; } owner=accountKey; layout=layoutKey; }
        paletteInputs(new int[0],(owned,initial) -> {
            synchronized(this) { if(!context(owner,layout)) return; }
            ItemPalette palette=new ItemPalette(itemSearch,owned,initial,2);
            boolean accepted; try { accepted=MarkerDialogs.confirm(parent(),palette,"Choose one item to inspect, or two to compare",740,650); } finally { palette.close(); }
            if(!accepted || palette.ids().length==0) return; int[] ids=palette.ids();
            clientThread.invokeLater(() -> {
                final List<AutoCategories.Item> bankItems,items=new ArrayList<>(); final List<Marker> groups=new ArrayList<>(); final CustomPresets templates;
                synchronized(this) {
                    if(!context(owner,layout)) return;
                    bankItems=bankFacts(); for(int id:ids) items.add(itemFact(id));
                    for(Marker m:markers) groups.add(gson.fromJson(gson.toJson(m),Marker.class));
                    templates=gson.fromJson(gson.toJson(customPresets),CustomPresets.class);
                }
                new SwingWorker<List<java.util.SortedSet<String>>,Void>() {
                    protected List<java.util.SortedSet<String>> doInBackground() {
                        List<java.util.SortedSet<String>> results=new ArrayList<>();
                        for(AutoCategories.Item item:items) results.add(BankCatalogue.memberships(item,bankItems,groups,templates));
                        return results;
                    }
                    protected void done() { try {
                        var memberships=get(); synchronized(WindowMarkersPlugin.this) { if(!context(owner,layout)) return; }
                        List<String> names=new ArrayList<>(); for(AutoCategories.Item item:items) names.add(item.name+" (#"+item.id+")");
                        ItemLookupPanel form=new ItemLookupPanel(names,memberships);
                        MarkerDialogs.confirm(parent(),form,"Item categories",850,640);
                        List<BankCatalogue.Choice> targets=java.util.Collections.emptyList();
                        synchronized(WindowMarkersPlugin.this) {
                            if(!context(owner,layout)) return;
                            if(form.membership!=null) {
                                lookupHighlight=null;
                                targets=BankCatalogue.targets(form.membership,catalogueChoices);
                                if(targets.isEmpty()) { setStatus("That classification is no longer available. Look up the item again."); return; }
                            } else if(form.highlightRequested) {
                                highlightLookupItems(ids);
                                setStatus("Highlighting "+ids.length+" item types. Legend → Clear highlight clears it.");
                            }
                        }
                        if(!targets.isEmpty()) {
                            BankCatalogue.Choice choice=targets.size()==1?targets.get(0):CatalogueBrowser.choose(parent(),targets,"");
                            synchronized(WindowMarkersPlugin.this) { if(!context(owner,layout)) return; }
                            if(choice!=null) chooseCatalogue(choice);
                        }
                    } catch(Exception ex) { setStatus("Item categories could not be loaded."); } }
                }.execute();
            });
        });
    }

    synchronized void highlightLookupItems(int[] ids)
    {
        if(!active || !currentLayout()) return;
        Marker temporary=new Marker(0,0,48,36);
        temporary.linkedItemIds=java.util.Arrays.stream(ids).filter(id -> id>=0).distinct().limit(2).toArray();
        temporary.scrollWithItems=true;
        lookupHighlight=temporary.linkedItemIds.length==0?null:temporary;
        legendHover=null; legendPinned=null;
    }

    private void openBankPicker()
    {
        if(bankPicker==null || bank==null || !currentLayout()) return;
        final String owner=accountKey,layout=layoutKey;
        boolean canSave=searchView!=null && searchView.isActive() || nativeSearch!=null && !nativeSearch.matches().isEmpty();
        bankPicker.show(client.getCanvas(),ActivityMenu.choices(catalogueChoices),canSave,choice -> {
            synchronized(WindowMarkersPlugin.this) { if(!context(owner,layout)) return; }
            chooseCatalogue(choice,true);
        },this::saveCurrentBankView,() -> clientThread.invokeLater(() -> {
            synchronized(WindowMarkersPlugin.this) {
                if(!context(owner,layout)) return;
                viewRequest++;
                if(searchView!=null && searchView.isActive()) searchView.clear();
                else if(nativeSearch!=null) nativeSearch.closeSearch();
            }
        }));
    }

    private void resetBankContext(boolean stopping)
    {
        catalogueWork.clearPending();
        rankingIndex=null;viewRequest++;
        // RuneLite starts/stops plugins on EDT; layout/profile changes may also
        // arrive there. Confine all game-widget and Bank Tags cleanup to client.
        if(clientThread!=null) clientThread.invoke(() -> {
            synchronized(WindowMarkersPlugin.this) {
                if(bankCapture!=null) bankCapture.cancel();
                if(nativeSearch!=null) {
                    if(stopping) nativeSearch.stop();
                    else { nativeSearch.reset(); nativeSearch.start(); }
                }
                if(searchView!=null) searchView.clear();
                if(bankPicker!=null) bankPicker.clear();
                if(stopping && screenshotButton!=null) screenshotButton.clear();
            }
        });
    }

    private synchronized void showBankView(String name,List<Marker> groups,boolean slots,String owner,String layout)
    {
        if(searchView==null) return;
        List<Marker> snapshot=new ArrayList<>();
        for(Marker group:groups) snapshot.add(gson.fromJson(gson.toJson(group),Marker.class));
        BankSearchIndex index=rankingIndex; final long request=++viewRequest;
        clientThread.invokeLater(() -> {
            synchronized(this) { if(request!=viewRequest || !context(owner,layout) || bank==null) return; }
            final java.util.function.BooleanSupplier unchanged=searchView.unchangedContext();
            new SwingWorker<BankSearchIndex.Match,Void>() {
                protected BankSearchIndex.Match doInBackground() {
                    if(index==null || snapshot.isEmpty()) return null;
                    Marker first=snapshot.get(0);
                    return index.search(first.guideQuery==null?name:first.guideQuery,first.guideContext==null?"":first.guideContext,
                        first.inventoryContext==null?"":first.inventoryContext);
                }
                protected void done() { try {
                    BankSearchIndex.Match match=get();
                    clientThread.invokeLater(() -> { synchronized(WindowMarkersPlugin.this) {
                        if(request!=viewRequest || !context(owner,layout) || bank==null || !unchanged.getAsBoolean()) return;
                        nativeSearch.closeSearch();
                        java.util.Set<Integer> allowed=java.util.Arrays.stream(GroupEdits.union(snapshot)).boxed().collect(java.util.stream.Collectors.toSet());
                        searchView.show(name,snapshot,slots,match==null || match.loadout==null?null:match.loadout.retainItems(allowed),match==null?java.util.Collections.emptyMap():match.ranks);
                    }});
                } catch(Exception ex) { setStatus("Could not open the saved view. Try again."); } }
            }.execute();
        });
    }

    private void openQueryView(String text,String guide)
    { openQueryView(text,guide,false); }

    private void openQueryView(String text,String guide,boolean addToLegend)
    {
        final String owner,layout,inventory;final BankSearchIndex index;final long request;
        synchronized(this) {
            if(!currentLayout() || bank==null || rankingIndex==null) { setStatus("Open your bank and wait for the catalogue.");return; }
            owner=accountKey;layout=layoutKey;inventory=config.bankInventoryContext();index=rankingIndex;request=++viewRequest;
        }
        clientThread.invokeLater(() -> {
            synchronized(this) { if(request!=viewRequest || !context(owner,layout) || bank==null) return; }
            final java.util.function.BooleanSupplier unchanged=searchView.unchangedContext();
            new SwingWorker<BankSearchIndex.Match,Void>() {
                protected BankSearchIndex.Match doInBackground() { return index.search(text,guide,inventory); }
                protected void done() { try {
                    BankSearchIndex.Match match=get();
                    clientThread.invokeLater(() -> { synchronized(WindowMarkersPlugin.this) {
                        if(request!=viewRequest || !context(owner,layout) || bank==null || !unchanged.getAsBoolean()) return;
                        if(match.ids.isEmpty() && (match.loadout==null || !match.loadout.hasBudgetExamples())) {
                            nativeSearch.closeSearch();searchView.clear();
                            setStatus("No owned items match that search. Previous filter cleared; showing all bank items.");return;
                        }
                        Marker group=new Marker(0,0,48,36);styleGroup(group,text);group.scrollWithItems=true;
                        group.linkedItemIds=match.ids.stream().sorted().mapToInt(Integer::intValue).toArray();
                        group.guideQuery=text;group.guideContext=guide;group.inventoryContext=inventory;
                        nativeSearch.closeSearch();searchView.show(text,java.util.Collections.singletonList(group),match.slots,match.loadout,match.ranks);
                        setStatus(BankCleanup.isQuery(text)?"Cleanup review: open Item uses and reasons for details. Nothing is discarded or sold.":"View open. Save it to reopen later, or add it to Legend.");
                        if(addToLegend) addViewToLegend();
                    }});
                } catch(Exception ex) { setStatus("Could not open that search. Your saved groups are unchanged."); } }
            }.execute();
        });
    }
    private void clearBankView() {
        final String owner,layout;
        synchronized(this) { viewRequest++;owner=accountKey;layout=layoutKey; }
        clientThread.invokeLater(() -> { synchronized(this) {
            if(!context(owner,layout)) return;
            if(searchView!=null) searchView.clear();if(nativeSearch!=null) nativeSearch.closeSearch();
            SwingUtilities.invokeLater(() -> { synchronized(WindowMarkersPlugin.this) {
                if(context(owner,layout) && legend!=null) legend.search.setText("");
            }});
            setStatus("Showing all bank items.");
        }});
    }
    private void reviewItemUses()
    {
        clientThread.invokeLater(() -> { synchronized(this) {
            if(!currentLayout() || bank==null) { setStatus("Open your bank first.");return; }
            final String owner=accountKey,layout=layoutKey;
            java.util.Map<Integer,ItemCatalogueIndex.Rank> reasons=new java.util.LinkedHashMap<>();
            bankRanks().forEach((id,rank)-> { if(BankCleanup.isReview(rank)) reasons.put(id,rank); });
            if(reasons.isEmpty()) { setStatus("Open Junk review, Sell candidates or a quest review first.");return; }
            paletteInputs(reasons.keySet().stream().mapToInt(Integer::intValue).toArray(),(owned,choices)-> {
                synchronized(WindowMarkersPlugin.this) { if(!context(owner,layout)) return; }
                ItemReviewPanel panel=new ItemReviewPanel(choices,reasons);
                try { MarkerDialogs.show(parent(),panel,"Item uses and cleanup reasons",800,620); }
                finally { panel.close(); }
            });
        }});
    }
    private void refreshOpenView() {
        clientThread.invokeLater(() -> { synchronized(this) {
            if(searchView!=null && searchView.isActive()) {
                List<Marker> groups=searchView.groups();
                String query=!groups.isEmpty() && groups.get(0).guideQuery!=null?groups.get(0).guideQuery:searchView.name();
                SwingUtilities.invokeLater(() -> openQueryView(query,config.geGuideContext()));
            }
        }});
    }
    private void addViewToLegend() {
        final String owner,layout;
        synchronized(this) { if(!currentLayout()) return;owner=accountKey;layout=layoutKey; }
        clientThread.invokeLater(() -> { synchronized(this) {
            if(!context(owner,layout)) return;
            List<Marker> groups=new ArrayList<>(searchView.groups());
            if(groups.isEmpty() && !nativeSearch.matches().isEmpty()) {
                Marker group=new Marker(0,0,48,36);styleGroup(group,nativeSearch.query());group.scrollWithItems=true;
                group.linkedItemIds=nativeSearch.matches().stream().mapToInt(Integer::intValue).toArray();
                group.guideQuery=nativeSearch.query();group.guideContext=config.geGuideContext();group.inventoryContext=config.bankInventoryContext();groups.add(group);
            }
            if(groups.isEmpty()) { setStatus("Open a search or saved view first.");return; }
            int added=0;List<Marker> selected=new ArrayList<>();
            for(Marker group:groups) {
                Marker existing=markers.stream().filter(m -> m.labelText().equals(group.labelText())
                    && java.util.Arrays.equals(java.util.Arrays.stream(m.linkedItemIds==null?new int[0]:m.linkedItemIds).sorted().toArray(),java.util.Arrays.stream(group.linkedItemIds==null?new int[0]:group.linkedItemIds).sorted().toArray())).findFirst().orElse(null);
                if(existing!=null) { selected.add(existing);continue; }
                if(markers.size()>=MAX_MARKERS) continue;
                Marker copy=gson.fromJson(gson.toJson(group),Marker.class);markers.add(copy);selected.add(copy);added++;
            }
            if(added>0) { save();setStatus(added+" groups added to Legend. Select them to edit or remove."); }
            else setStatus(selected.isEmpty()?"Legend is full. Remove groups before adding more.":"Already in Legend. Existing groups selected; colours kept.");
            SwingUtilities.invokeLater(() -> { synchronized(WindowMarkersPlugin.this) {
                if(context(owner,layout) && legend!=null) legend.showGroups(selected);
            }});
        }});
    }
    private void addBankTag() {
        final String owner,layout;
        synchronized(this) { if(!currentLayout() || bank==null) { setStatus("Open your bank first.");return; } owner=accountKey;layout=layoutKey; }
        paletteInputs(new int[0],(owned,selected) -> {
            synchronized(this) { if(!context(owner,layout)) return; }
            ItemPalette palette=new ItemPalette(itemSearch,owned,selected,4000);
            JTextField tag=new JTextField();JCheckBox variations=new JCheckBox("Include Bank Tags item variations",false);
            JPanel form=new JPanel(new BorderLayout(0,8));JPanel heading=new MarkerForm();
            heading.add(new JLabel("Bank Tags tag name"));heading.add(tag);heading.add(variations);
            heading.add(new JLabel("<html>Saves a real Bank Tags tag in this RuneLite profile.<br>Use that tag in Bank Tags and Bank Tag layouts.<br>Existing tags are retained.</html>"));
            form.add(heading,BorderLayout.NORTH);form.add(palette,BorderLayout.CENTER);
            boolean accepted;try { accepted=MarkerDialogs.confirm(parent(),form,"Add tag to item",740,680); } finally { palette.close(); }
            if(!accepted) return;
            String name=net.runelite.client.util.Text.standardize(tag.getText());int[] ids=palette.ids();boolean all=variations.isSelected();
            if(name.isEmpty() || name.length()>60 || name.matches(".*[,<>:].*") || ids.length==0) { setStatus("Choose items and enter one tag name (up to 60 characters; no commas, colons or angle brackets).");return; }
            clientThread.invokeLater(() -> { synchronized(this) {
                if(!context(owner,layout)) return;
                // Bank Tags' public user-data API preserves existing tags and handles variations.
                for(int id:ids) bankTagManager.addTag(id,name,all);
                nativeBankLayout.layoutBank();setStatus("Added Bank Tags tag '"+name+"' to "+ids.length+" item types.");
            }});
        });
    }

    private void saveCurrentBankView()
    {
        final String owner,layout;
        synchronized(this) { if(!currentLayout() || bank==null) { setStatus("Open a bank search or setup first."); return; } owner=accountKey; layout=layoutKey; }
        clientThread.invokeLater(() -> {
            final CustomPresets.Preset template=new CustomPresets.Preset();
            synchronized(WindowMarkersPlugin.this) {
                if(!context(owner,layout) || bank==null) return;
                if(searchView!=null && searchView.isActive()) {
                    template.name=searchView.name(); template.groupBySlot=searchView.bySlot();
                    for(Marker group:searchView.groups()) template.groups.add(gson.fromJson(gson.toJson(group),Marker.class));
                } else if(nativeSearch!=null && !nativeSearch.matches().isEmpty()) {
                    Marker group=new Marker(0,0,48,36);
                    styleGroup(group,nativeSearch.query()); group.scrollWithItems=true;
                    group.linkedItemIds=nativeSearch.matches().stream().sorted().mapToInt(Integer::intValue).toArray();
                    group.guideQuery=nativeSearch.query(); group.guideContext=config.geGuideContext();group.inventoryContext=config.bankInventoryContext();
                    template.groups.add(group); template.name=nativeSearch.query(); template.groupBySlot=nativeSearch.groupBySlot() && config.setupSearchLayout();
                }
                if(template.groups.isEmpty()) { setStatus("No Improved Banking search results to save."); return; }
                template.bankView=true;
            }
            SwingUtilities.invokeLater(() -> {
                synchronized(WindowMarkersPlugin.this) {
                    if(!context(owner,layout)) return;
                    String base=template.name;int copy=1;
                    while(customPresets.presets.stream().anyMatch(p->p.name.equalsIgnoreCase(template.name))) template.name=base+" copy "+copy++;
                }
                editViewDraft(null,template,owner,layout);
            });
        });
    }

    private void saveCustomPreset()
    {
        List<Marker> selected=legend.selected(); final String owner,layout;
        synchronized(this) {
            if(!currentLayout() || selected.isEmpty() || selected.stream().anyMatch(m -> !m.linked() || !markers.contains(m))) {
                setStatus("Select linked item groups in Legend first. Area markers cannot be saved as item presets."); return;
            }
            owner=accountKey; layout=layoutKey;
        }
        final CustomPresets.Preset template=new CustomPresets.Preset();
        synchronized(this) {
            if(!context(owner,layout) || !markers.containsAll(selected)) return;
            template.name="My view";
            for(Marker m:selected) template.groups.add(gson.fromJson(gson.toJson(m),Marker.class));
        }
        editViewDraft(null,template,owner,layout);
    }

    private void savePresets()
    { configs.setConfiguration("windowmarkers",accountKey,baseLayoutKey()+"_customPresets",gson.toJson(customPresets)); refreshRows(); }

    private boolean deleteCustomPreset(CustomPresets.Preset preset)
    {
        if(preset==null) return false;
        String owner,layout; synchronized(this) { owner=accountKey; layout=layoutKey; }
        if(!MarkerDialogs.confirm(parent(),new JLabel("Delete saved preset \""+preset.name+"\"? Existing groups stay."),"Delete preset",540,200)) return false;
        synchronized(this) { if(context(owner,layout) && customPresets.presets.remove(preset)) { savePresets(); setStatus("Saved preset deleted. Existing groups are unchanged."); return true; } }
        return false;
    }

    private void applyCustomPreset(CustomPresets.Preset preset)
    { applyCustomPreset(preset,null); }

    private void applyCustomPreset(CustomPresets.Preset preset,String selectedGroup)
    { applyCustomPreset(preset,selectedGroup,false); }

    private void applyCustomPreset(CustomPresets.Preset preset,String selectedGroup,boolean requestedView)
    {
        if(preset==null) { setStatus("Select a saved custom preset first."); return; }
        String owner,layout; synchronized(this) { if(!currentLayout() || !customPresets.presets.contains(preset)) return; owner=accountKey; layout=layoutKey; }
        JPanel form=new MarkerForm();
        form.add(new JLabel("<html>Add copies of these groups to the current layout.<br>Items not currently owned remain linked for future use.<br>Existing groups and saved preset are retained.</html>"));
        JCheckBox skip=new JCheckBox("Skip items already in a group",selectedGroup==null); form.add(skip);
        JCheckBox showView=new JCheckBox("Open as a temporary bank view",requestedView || preset.bankView); form.add(showView);
        List<JCheckBox> choices=new ArrayList<>();
        for(Marker group:preset.groups) { JCheckBox box=new JCheckBox(group.labelText()+" · "+group.linkedItemIds.length+" item types",selectedGroup==null || selectedGroup.equals(group.labelText())); choices.add(box); form.add(box); }
        if(!MarkerDialogs.confirm(parent(),form,"Apply "+preset.name,600,500)) return;
        synchronized(this) {
            if(!context(owner,layout) || !customPresets.presets.contains(preset)) return;
            List<Marker> viewGroups=new ArrayList<>();
            for(int i=0;i<choices.size();i++) if(choices.get(i).isSelected()) viewGroups.add(preset.groups.get(i));
            java.util.Set<Integer> assigned=new java.util.HashSet<>();
            for(Marker m:markers) if(m.linked()) for(int id:m.linkedItemIds) assigned.add(id);
            int added=0; boolean limited=false; cancel();
            for(int i=0;i<choices.size();i++) if(choices.get(i).isSelected()) {
                if(markers.size()>=MAX_MARKERS) { limited=true; break; }
                Marker copy=gson.fromJson(gson.toJson(preset.groups.get(i)),Marker.class);
                if(skip.isSelected()) copy.linkedItemIds=java.util.Arrays.stream(copy.linkedItemIds).filter(id -> !assigned.contains(id)).toArray();
                if(!copy.linked()) continue;
                copy.preset=preset.name; markers.add(copy); for(int id:copy.linkedItemIds) assigned.add(id); added++;
            }
            if(added>0) save(); setStatus(added+" groups added."+(limited?" Marker limit reached.":"")+" Undo restores the layout.");
            if(showView.isSelected() && !viewGroups.isEmpty()) showBankView(preset.name,viewGroups,preset.groupBySlot,owner,layout);
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
        List<String> names=new ArrayList<>(catalog.names.values());
        int index=new ArrayList<>(catalog.names.keySet()).indexOf(catalog.active);
        List<CustomPresets.Preset> templates=new ArrayList<>(customPresets.presets);
        SwingUtilities.invokeLater(() -> {
            if(layoutPicker!=null) {
                refreshingLayouts=true; layoutPicker.removeAllItems();
                for(String name:names) layoutPicker.addItem(name);
                layoutPicker.setSelectedIndex(index); refreshingLayouts=false;
            }
            if(customPicker!=null) {
                Object selected=customPicker.getSelectedItem(); customPicker.removeAllItems();
                for(CustomPresets.Preset template:templates) customPicker.addItem(template);
                if(templates.contains(selected)) customPicker.setSelectedItem(selected);
            }
        });
        legendEntries=null; updateLegend(legendCells); reloadCatalogue();
    }

    private void setStatus(String text) { SwingUtilities.invokeLater(() -> { if (status != null) { status.setText("<html>" + text.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;") + "</html>"); } }); }

    synchronized void arm()
    {
        cancel();
        if (bank == null || !currentLayout()) { setStatus("Open your bank first."); return; }
        if (markers.size() >= MAX_MARKERS) { setStatus(MAX_MARKERS+"-marker limit reached."); return; }
        armed = true; setStatus("Drag inside bank. Right-click cancels.");
    }

    synchronized void cancel() { armed = false; start = null; preview = null; transformTarget=null; forceLink=null; appendTarget=null; pendingGroupName=null; setStatus("Ready. Draw or select a marker."); }

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

    private void edit(Marker marker)
    {
        final String owner;
        final String ownerLayout;
        final int[] enclosedIds;
        synchronized (this)
        {
            if (!currentLayout() || !markers.contains(marker)) { setStatus("Select a marker first."); return; }
            owner = accountKey; ownerLayout = layoutKey;
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

    @Subscribe(priority=-1) public void onScriptCallbackEvent(net.runelite.api.events.ScriptCallbackEvent event)
    { if(active && nativeSearch!=null) nativeSearch.filter(event); }

    @Subscribe(priority=1) public void onWidgetClosed(net.runelite.api.events.WidgetClosed event)
    {
        // Observe ownership before Bank Tags clears its active tag on unload.
        if(event.getGroupId()==InterfaceID.BANKMAIN && event.isUnload() && searchView!=null) searchView.bankClosed();
    }

    // Snapshot item bounds after BankLayoutController has restored the final frame.
    @Subscribe(priority=-30) public synchronized void onBeforeRender(BeforeRender event)
    {
        if(viewLabel!=null) {
            SetupLoadout plan=searchView!=null && searchView.isActive()?searchView.loadout():nativeSearch==null?null:nativeSearch.loadout();
            String current=searchView!=null && searchView.isActive()?"View: "+searchView.name():nativeSearch!=null && nativeSearch.highlight()!=null?"Search: "+nativeSearch.query():"All bank items";
            if(plan!=null) current+=(plan.equipmentName.isEmpty()?"":"\nEquipment: "+plan.equipmentName)+"\nInventory: "+plan.inventoryName+"\nRepeated supplies use one bank stack. Review guide requirements.";
            if(plan!=null && plan.sectionCount()>1) current+="\n"+plan.sectionCount()+" phase / method loadouts in this activity. Inventories remain separate.";
            if(plan!=null && !plan.equipmentNote.isEmpty()) current+="\n"+plan.equipmentNote;
            boolean hasView=searchView!=null && searchView.isActive();
            boolean hasMatches=hasView || nativeSearch!=null && nativeSearch.hasMatches();
            boolean canClear=hasView || nativeSearch!=null && nativeSearch.query()!=null && !nativeSearch.query().isEmpty();
            boolean review=bank!=null && bankRanks().values().stream().anyMatch(BankCleanup::isReview);
            String key=current+"/"+hasMatches+"/"+canClear+"/"+review;
            if(!key.equals(viewSummary)) { viewSummary=key;String summary=current;JLabel target=viewLabel;
                SwingUtilities.invokeLater(() -> { if(target==viewLabel) {
                    target.setText("<html>"+summary.split("\n",2)[0].replace("&","&amp;").replace("<","&lt;").replace(">","&gt;")+"</html>");target.setToolTipText(summary);
                    if(addViewButton!=null) addViewButton.setEnabled(hasMatches);
                    if(saveViewButton!=null) saveViewButton.setEnabled(hasMatches);
                    if(clearViewButton!=null) clearViewButton.setEnabled(canClear);
                    if(reviewItemsButton!=null) reviewItemsButton.setVisible(review);
                } });
            }
        }
        if(nativeSearch!=null) nativeSearch.update();
        if(searchView!=null) searchView.update();
        if (client.getGameState() != GameState.LOGGED_IN)
        {
            linkedRegions.clear();
            catalogueWork.clearPending();
            if(screenshotButton!=null) { screenshotButton.clear(); }
            if(bankPicker!=null) bankPicker.clear();
            if(searchView!=null) searchView.clear();
            if (bank != null || accountKey != null) {
                cancel(); bank=null; itemArea=null; accountKey=null; layoutKey=null; markers.clear(); legendCells.clear();
                customPresets=new CustomPresets(); lookupHighlight=null; catalogueRevision++;
                if(nativeSearch!=null) nativeSearch.reset();
                if(legend!=null) SwingUtilities.invokeLater(legend::resetContext);
                refreshRows();
            }
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
        // Quest progress is read between events on each bank opening, never from a render/script callback.
        if(bank==null && next!=null) reloadCatalogue();
        bank = next;
        Widget items = bank == null ? null : client.getWidget(InterfaceID.Bankmain.ITEMS);
        Rectangle nextItems = items == null || items.isHidden() ? null : items.getBounds().intersection(bank);
        if (nextItems != null && nextItems.isEmpty()) { nextItems=null; }
        int nextScroll=items == null ? 0 : items.getScrollY();
        if (start != null && (!java.util.Objects.equals(itemArea,nextItems) || itemScroll != nextScroll)) { cancel(); }
        itemArea=nextItems; itemScroll=nextScroll;
        itemCells=new ArrayList<>();
        previewLabelObstacles.clear();
        linkedRegions.clear();
        List<ItemAnchors.Cell> tabCells=new ArrayList<>();
        Widget[] itemChildren=itemArea==null?null:items.getDynamicChildren();
        if (itemChildren != null)
        {
            for (Widget child : itemChildren)
            {
                if (child == null || child.isSelfHidden() || child.getItemId() < 0 || child.getItemId() == 6512
                    || child.getRelativeX()<0 || child.getRelativeY()<0) { continue; }
                Rectangle bounds=BankItemBounds.of(items,child);
                if(bounds.isEmpty()) continue;
                if(bankLayout!=null && bankLayout.inventoryPreview(child)) {
                    if(!child.isHidden() && bounds.intersects(itemArea)) previewLabelObstacles.add(bounds);
                    continue;
                }
                ItemAnchors.Cell cell=new ItemAnchors.Cell(itemManager.canonicalize(child.getItemId()),bounds);
                tabCells.add(cell);
                if(child.isHidden()) { continue; }
                if (!bounds.intersects(itemArea)) { continue; }
                itemCells.add(cell);
            }
        }
        ItemAnchors.fitGrid(tabCells);
        if (itemArea != null && currentLayout() && markers.stream().anyMatch(m -> m.scrollWithItems == null))
        {
            String backupKey=layoutKey+"_beforeScroll";
            if (configs.getConfiguration("windowmarkers",accountKey,backupKey) == null)
            { configs.setConfiguration("windowmarkers",accountKey,backupKey,gson.toJson(markers)); }
            for (Marker marker : markers) { marker.anchorLegacy(bank,itemArea); }
            commit(false); history.reset(gson.toJson(markers));
        }
        legendCells=tabCells; updateLegend(tabCells);
        if(screenshotButton!=null) { screenshotButton.update(active && bank!=null && config.screenshotButton(),this::captureBank); }
        if(bankPicker!=null) bankPicker.update(active && bank!=null && config.bankCatalogueButton(),this::openBankPicker);
    }

    private void updateLegend(List<ItemAnchors.Cell> tabCells)
    {
        List<MarkerLegend.Entry> entries=MarkerLegend.forTab(markers,tabCells,bank,itemArea,itemScroll,config.color());
        String tierKey=config.geTierColours()?TierPresentation.key(config):"";
        if(MarkerLegend.same(legendEntries,entries) && tierKey.equals(legendTierKey)) { return; }
        legendTierKey=tierKey;
        legendEntries=entries;
        if(entries.stream().noneMatch(e -> e.marker==legendHover)) { legendHover=null; }
        if(entries.stream().noneMatch(e -> e.marker==legendPinned)) { legendPinned=null; }
        List<MarkerLegend.Entry> all=new ArrayList<>();
        for(int i=0;i<markers.size();i++) {
            Marker m=markers.get(i);
            all.add(new MarkerLegend.Entry(m,m.labelText().isEmpty()?"Marker "+(i+1):m.labelText(),m.borderArgb==null?config.color():new Color(m.borderArgb,true)));
        }
        long revision=++legendRevision;
        SwingUtilities.invokeLater(() -> {
            if(active && legend!=null && revision==legendRevision) { legend.replace(entries,all); legend.tiers(tierKey); }
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
        if (marker.linked()) { return linkedRegions.get(marker,itemCells); }
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
                Marker target=transformTarget,append=appendTarget; Boolean linking=forceLink; String groupName=pendingGroupName;
                cancel();
                if (drawn != null && drawn.valid() && active && currentLayout())
                {
                    if (target != null)
                    {
                        if (!currentLayout() || !markers.contains(target)) { return e; }
                        target.x=drawn.x; target.y=drawn.y; target.width=drawn.width; target.height=drawn.height;
                        save(); setStatus("Area updated."); return e;
                    }
                    boolean linked=linking==null?config.linkNewMarkers():linking;
                    if (linked && itemArea != null && Boolean.TRUE.equals(drawn.scrollWithItems))
                    { drawn.linkedItemIds=ItemAnchors.enclosed(drawn.onScreen(bank,itemArea,itemScroll),itemCells,itemArea); }
                    if(Boolean.TRUE.equals(linking) && !drawn.linked()) { setStatus("No enclosed items. Draw around item icons to create a group."); return e; }
                    if(append!=null) {
                        if(markers.contains(append)) {
                            int[] joined=GroupEdits.union(java.util.Arrays.asList(append,drawn));
                            if(joined.length>4000) { setStatus("A group can contain up to 4,000 item types."); return e; }
                            append.linkedItemIds=joined; append.category=null; append.preset="Custom"; append.clearGuide();
                            save(); setStatus("Items added to "+append.labelText()+". Undo restores the group.");
                        }
                        return e;
                    }
                    if(markers.size()>=MAX_MARKERS) return e;
                    if(groupName!=null) { styleGroup(drawn,groupName); }
                    markers.add(drawn); save();
                    setStatus(linked && !drawn.linked() ? "No enclosed items; saved an area marker." : "Marker saved.");
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
        if(lookupHighlight!=null) { return lookupHighlight; }
        if(nativeSearch!=null && nativeSearch.highlight()!=null) { return nativeSearch.highlight(); }
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

    private List<Marker> renderedMarkers() {
        if(searchView==null || !searchView.isActive()) return markers;
        List<Marker> rendered=new ArrayList<>(markers);rendered.addAll(searchView.groups());return rendered;
    }

    final class MarkerOverlay extends Overlay
    {
        private final MarkerLabelCache labelPositions=new MarkerLabelCache();
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
                    if(config.bankTooltips() && config.visible() && hovered!=null && tooltipManager!=null)
                    { tooltipManager.add(new net.runelite.client.ui.overlay.tooltip.Tooltip(net.runelite.client.util.Text.escapeJagex(hovered.labelText()))); }
                    if (config.visible())
                    {
                        for (Marker marker : renderedMarkers())
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
                        occupiedLabels.addAll(previewLabelObstacles);
                        if(itemArea!=null && bankLayout!=null) for(SetupItemLayout.Heading h:bankLayout.searchHeadings())
                            occupiedLabels.add(h.emptySlot?new Rectangle(itemArea.x+h.x,itemArea.y+h.y-itemScroll,36,32)
                                :new Rectangle(itemArea.x+h.x,itemArea.y+h.y-itemScroll-12,170,16));
                        labelPositions.begin(occupiedLabels);
                        for (Marker marker : renderedMarkers())
                        {
                            Rectangle clip=markerClip(marker);
                            if (clip==null || marker.labelText().isEmpty() || config.bankLabels()==WindowMarkersConfig.BankLabels.OFF) { continue; }
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
                                    boolean custom=marker.category==null;
                                    String title=MarkerPresentation.fit(custom?marker.labelText():mini?MarkerPresentation.miniTitle(marker.labelText()):MarkerPresentation.displayTitle(marker.labelText()),metrics,Math.min(mini?90:176,clip.width-10));
                                    if (title.isEmpty()) { continue; }
                                    Rectangle badge=labelPositions.place(r,clip,metrics.stringWidth(title)+10,metrics.getHeight()+4,marker.labelPlacement,occupiedLabels);
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
                    if(itemArea!=null && bankLayout!=null) {
                        Graphics2D headings=(Graphics2D)g.create();
                        try { headings.clip(itemArea);headings.setFont(new Font("Arial",Font.PLAIN,11));headings.setColor(new Color(230,205,145));
                            Font headingFont=headings.getFont(),slotFont=headingFont.deriveFont(9f);
                            Color headingColor=headings.getColor(),slotColor=new Color(155,155,155,130);
                            for(SetupItemLayout.Heading heading:bankLayout.searchHeadings()) {
                                int x=itemArea.x+heading.x,y=itemArea.y+heading.y-itemScroll;
                                if(heading.emptySlot) {
                                    headings.setColor(slotColor);headings.drawRoundRect(x,y,35,31,4,4);
                                    headings.setFont(slotFont);
                                    headings.drawString(heading.text,x+(36-headings.getFontMetrics().stringWidth(heading.text))/2,y+19);
                                } else {
                                    headings.setColor(headingColor);headings.setFont(headingFont);
                                    headings.drawString(MarkerPresentation.fit(heading.text,headings.getFontMetrics(),Math.min(380,itemArea.width-heading.x-6)),x,y);
                                }
                            }
                            net.runelite.api.Point mouse=client.getMouseCanvasPosition();
                            if(config.bankTooltips() && mouse!=null && !client.isMenuOpen() && itemArea.contains(mouse.getX(),mouse.getY()) && tooltipManager!=null)
                                for(SetupItemLayout.InventoryNote note:bankLayout.inventoryNotes())
                                    if(new Rectangle(itemArea.x+note.x,itemArea.y+note.y-itemScroll,36,32).contains(mouse.getX(),mouse.getY()))
                                        tooltipManager.add(new net.runelite.client.ui.overlay.tooltip.Tooltip(net.runelite.client.util.Text.escapeJagex(note.text)));
                        } finally { headings.dispose(); }
                    }
                    Marker itemHighlight=lookupHighlight!=null?lookupHighlight:nativeSearch==null?null:nativeSearch.highlight();
                    if(itemHighlight==null && searchView!=null) itemHighlight=searchView.highlight();
                    if(itemHighlight!=null && itemArea!=null && !BankCleanup.isQuery(itemHighlight.guideQuery))
                    {
                        Graphics2D highlight=(Graphics2D)g.create();
                        try {
                            highlight.clip(itemArea); highlight.setStroke(new BasicStroke(2));
                            for(Rectangle r:markerRegions(itemHighlight)) if(r!=null) BorderRenderer.plain(highlight,r,Color.YELLOW);
                        } finally { highlight.dispose(); }
                    }
                    if(config.visible() && itemArea!=null) {
                        java.util.Map<Integer,ItemCatalogueIndex.Rank> tiers=bankRanks();
                        Graphics2D ranked=(Graphics2D)g.create();
                        try {
                            ranked.clip(itemArea);
                            net.runelite.api.Point mouse=client.getMouseCanvasPosition();
                            for(ItemAnchors.Cell cell:itemCells) {
                                ItemCatalogueIndex.Rank rank=tiers.get(cell.id);
                                if(rank==null || !cell.grid.intersects(itemArea)) continue;
                                if(BankCleanup.isReview(rank)) {
                                    ranked.setStroke(new BasicStroke(1));
                                    BorderRenderer.plain(ranked,cell.grid,BankCleanup.colour(rank));
                                } else {
                                    if(!config.geTierColours()) continue;
                                    TierPresentation.draw(ranked,cell.grid,rank.tier,config);
                                }
                                if(config.bankTooltips() && mouse!=null && !client.isMenuOpen() && itemArea.contains(mouse.getX(),mouse.getY()) && cell.grid.contains(mouse.getX(),mouse.getY()) && tooltipManager!=null) {
                                    String explanation=rank.explanation.length()>850?rank.explanation.substring(0,847)+"...":rank.explanation;
                                    tooltipManager.add(new net.runelite.client.ui.overlay.tooltip.Tooltip(net.runelite.client.util.Text.escapeJagex(explanation).replace("\n","<br>")));
                                }
                            }
                        } finally { ranked.dispose(); }
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

    private java.util.Map<Integer,ItemCatalogueIndex.Rank> bankRanks()
    {
        if(nativeSearch!=null && nativeSearch.searching()) return nativeSearch.ranks();
        if(rankingIndex==null) return java.util.Collections.emptyMap();
        if(searchView!=null && searchView.isActive()) return searchView.ranks().isEmpty() || searchView.ranks().values().stream().anyMatch(BankCleanup::isReview)
            ?rankingIndex.ranks(searchView.groups()):searchView.ranks();
        Marker selected=highlightedMarker();
        return rankingIndex.ranks(selected==null?markers:java.util.Collections.singletonList(selected));
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
                    for (Marker marker : renderedMarkers())
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


