package com.windowmarkers;

import java.util.*;
import javax.inject.Inject;
import javax.swing.SwingWorker;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.ScriptCallbackEvent;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarClientID;
import net.runelite.api.vars.InputType;
import net.runelite.api.widgets.JavaScriptCallback;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetType;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.bank.BankSearch;
import net.runelite.client.plugins.banktags.BankTag;
import net.runelite.client.plugins.banktags.BankTagsService;

/** Adds only owned search UI and optional matches; native/tag restrictions remain authoritative. */
@javax.inject.Singleton
final class NativeBankSearch
{
    private static final org.slf4j.Logger log=org.slf4j.LoggerFactory.getLogger(NativeBankSearch.class);
    @Inject Client client;
    @Inject ClientThread clientThread;
    @Inject ConfigManager configs;
    @Inject WindowMarkersConfig config;
    @Inject ItemManager itemManager;
    @Inject BankSearch bankSearch;
    @Inject BankTagsService bankTags;
    private Widget parent,checkbox;
    private BankSearchIndex index;
    private String query;
    private Set<Integer> matches=Collections.emptySet();
    private Map<Integer,ItemCatalogueIndex.Rank> ranks=Collections.emptyMap();
    private String guideContext="", inventoryContext="";
    private String tagName;
    private BankTag tag;
    private SetupLoadout loadout;
    private Marker highlight;
    private boolean slots;
    private boolean exactSupply;
    private int hitpoints;
    private long revision;
    private boolean running;
    private volatile long context;

    void start() { running=true; }
    void stop() { boolean restore=searching(); running=false; reset(); if(restore) bankSearch.layoutBank(); }
    void reset() { context++; revision++; query=null; index=null; tagName=null;tag=null;matches=Collections.emptySet(); ranks=Collections.emptyMap(); highlight=null; slots=false; exactSupply=false; loadout=null; clearWidget(); }
    void index(BankSearchIndex next)
    { final long owner=context; clientThread.invokeLater(() -> { if(running && owner==context) { index=next; update(true); } }); }
    boolean searching()
    {
        Widget bank=client.getWidget(InterfaceID.Bankmain.UNIVERSE);
        return running && client.getGameState()==GameState.LOGGED_IN && bank!=null && !bank.isHidden()
            && client.getVarcIntValue(VarClientID.MESLAYERMODE)==InputType.SEARCH.getType();
    }
    void update() { update(false); }
    private void update(boolean indexChanged)
    {
        if(!searching()) {
            if(query!=null) { revision++; query=null; matches=Collections.emptySet(); ranks=Collections.emptyMap(); highlight=null; slots=false; loadout=null; }
            if(checkbox!=null) checkbox.setHidden(true); return;
        }
        updateCheckbox();
        String next=client.getVarcStrValue(VarClientID.MESLAYERINPUT);
        if(!config.improvedBankSearch()) next="";
        String nextContext=config.geGuideContext()==null?"":config.geGuideContext();
        String nextInventory=config.bankInventoryContext()==null?"":config.bankInventoryContext();
        int nextHitpoints=client.getRealSkillLevel(net.runelite.api.Skill.HITPOINTS);
        String nextTagName=bankTags.getActiveTag();BankTag nextTag=bankTags.getActiveBankTag();
        boolean queryChanged=!Objects.equals(query,next) || !Objects.equals(guideContext,nextContext) || !Objects.equals(inventoryContext,nextInventory)
            || hitpoints!=nextHitpoints || !Objects.equals(tagName,nextTagName) || tag!=nextTag;
        if(!queryChanged && !indexChanged) return;
        Set<Integer> previous=new HashSet<>(matches); boolean previousSlots=slots;
        query=next; guideContext=nextContext; inventoryContext=nextInventory; hitpoints=nextHitpoints;exactSupply=false;
        tagName=nextTagName;tag=nextTag;
        if(queryChanged) { matches=Collections.emptySet(); ranks=Collections.emptyMap(); highlight=null; slots=false; loadout=null; }
        final long token=++revision;
        final BankSearchIndex snapshot=index; final String text=next,guide=nextContext,inventory=nextInventory;
        if(snapshot==null || !BankSearchIndex.plainQuery(text)) {
            if(!previous.isEmpty() || previousSlots) bankSearch.layoutBank();
            return;
        }
        new SwingWorker<BankSearchIndex.Match,Void>() {
            protected BankSearchIndex.Match doInBackground() { return snapshot.search(text,guide,inventory,nextHitpoints); }
            protected void done() { try {
                final BankSearchIndex.Match found=get();
                clientThread.invokeLater(() -> {
                    if(!running || token!=revision || !currentContext() || !searching() || !config.improvedBankSearch()
                        || !Objects.equals(guide,config.geGuideContext()==null?"":config.geGuideContext())
                        || !Objects.equals(text,client.getVarcStrValue(VarClientID.MESLAYERINPUT))) return;
                    matches=new HashSet<>(found.ids); slots=found.slots; loadout=found.loadout;exactSupply=SupplySearch.parse(text)!=null || BankCleanup.isQuery(text);
                    // A tab's own membership must not be expanded by category search.
                    if(tag!=null) matches.removeIf(id -> !tag.contains(id));
                    ranks=new HashMap<>(found.ranks); ranks.keySet().retainAll(matches);
                    highlight=null;
                    if(!matches.isEmpty()) {
                        highlight=new Marker(0,0,48,36); highlight.scrollWithItems=true;
                        highlight.guideQuery=text;
                        if(BankCleanup.isQuery(text)) BankCleanup.style(highlight);
                        highlight.linkedItemIds=matches.stream().mapToInt(Integer::intValue).toArray();
                    }
                    if(!matches.equals(previous) || slots!=previousSlots || (queryChanged && (!matches.isEmpty() || loadout!=null && loadout.hasBudgetExamples()))) bankSearch.layoutBank();
                });
            } catch(Exception ex) {
                clientThread.invokeLater(() -> {
                    if(!running || token!=revision) return;
                    matches=Collections.emptySet(); ranks=Collections.emptyMap(); highlight=null; slots=false; loadout=null;
                    if(searching() && (!previous.isEmpty() || previousSlots)) bankSearch.layoutBank();
                    log.warn("Could not refresh Improved Banking search; using ordinary bank search",ex);
                });
            } }
        }.execute();
    }
    void filter(ScriptCallbackEvent event)
    {
        if(!"bankSearchFilter".equals(event.getEventName()) || !searching() || !config.improvedBankSearch() || !currentContext()) return;
        int size=client.getIntStackSize(),strings=client.getObjectStackSize();
        if(size<2 || strings<1) return;
        Object raw=client.getObjectStack()[strings-1];
        if(!(raw instanceof String) || !Objects.equals(query,raw) || !BankSearchIndex.plainQuery(query)) return;
        int id=client.getIntStack()[size-1]; if(id<0) return;
        if(bankTags.getActiveBankTag()!=null && !bankTags.getActiveBankTag().contains(id)) return;
        if(exactSupply) client.getIntStack()[size-2]=matches.contains(itemManager.canonicalize(id))?1:0;
        else if(matches.contains(itemManager.canonicalize(id))) client.getIntStack()[size-2]=1;
    }
    private boolean currentContext() { return Objects.equals(guideContext,config.geGuideContext()==null?"":config.geGuideContext())
        && Objects.equals(inventoryContext,config.bankInventoryContext()==null?"":config.bankInventoryContext())
        && Objects.equals(tagName,bankTags.getActiveTag()) && tag==bankTags.getActiveBankTag(); }
    SetupLoadout loadout() { return groupBySlot()?loadout:null; }
    Marker highlight() { return searching() && config.improvedBankSearch() && currentContext()
        && Objects.equals(query,client.getVarcStrValue(VarClientID.MESLAYERINPUT))?highlight:null; }
    void closeSearch() { if(searching()) bankSearch.reset(true); revision++; query=null; matches=Collections.emptySet(); ranks=Collections.emptyMap(); highlight=null; slots=false; loadout=null; }
    Map<Integer,ItemCatalogueIndex.Rank> ranks() { return highlight()==null?Collections.emptyMap():Collections.unmodifiableMap(ranks); }
    boolean groupBySlot() { return searching() && config.improvedBankSearch() && currentContext() && slots && (!matches.isEmpty() || loadout!=null && loadout.hasBudgetExamples())
        && Objects.equals(query,client.getVarcStrValue(VarClientID.MESLAYERINPUT)); }
    String query() { return query; }
    private boolean matchesCurrentInput() { return searching() && config.improvedBankSearch() && currentContext()
        && Objects.equals(query,client.getVarcStrValue(VarClientID.MESLAYERINPUT)); }
    boolean hasMatches() { return matchesCurrentInput() && !matches.isEmpty(); }
    Set<Integer> matches() { return matchesCurrentInput()?new HashSet<>(matches):Collections.emptySet(); }
    private void updateCheckbox()
    {
        Widget layer=client.getWidget(InterfaceID.Chatbox.MES_LAYER);
        if(layer==null || layer.isHidden() || layer.getWidth()<260 || layer.getHeight()<85) {
            if(checkbox!=null) checkbox.setHidden(true); return;
        }
        if(parent!=layer || checkbox==null || layer.getChildren()==null || !Arrays.asList(layer.getChildren()).contains(checkbox)) {
            clearWidget(); parent=layer; checkbox=layer.createChild(-1,WidgetType.TEXT);
            checkbox.setFontId(494); checkbox.setOriginalWidth(254); checkbox.setOriginalHeight(18);
            checkbox.setTextColor(0xFFFFFF); checkbox.setTextShadowed(true);
            checkbox.setAction(0,"Toggle Improved Banking search");
            checkbox.setOnOpListener((JavaScriptCallback)e -> {
                if(!searching()) return;
                final long owner=context;
                final Widget bank=client.getWidget(InterfaceID.Bankmain.UNIVERSE),clicked=checkbox;
                // layoutBank runs a client script. Never call it from inside a widget script.
                clientThread.invokeLater(() -> {
                    if(owner!=context || !searching() || checkbox!=clicked || parent!=layer
                        || client.getWidget(InterfaceID.Bankmain.UNIVERSE)!=bank
                        || client.getWidget(InterfaceID.Chatbox.MES_LAYER)!=layer) return;
                    configs.setConfiguration("windowmarkers","improvedBankSearch",!config.improvedBankSearch());
                    query=null; revision++; matches=Collections.emptySet(); ranks=Collections.emptyMap();
                    highlight=null; slots=false; loadout=null; exactSupply=false;
                    update(); bankSearch.layoutBank();
                });
            });
            checkbox.setHasListener(true);
        }
        checkbox.setOriginalX((layer.getWidth()-254)/2); checkbox.setOriginalY(layer.getHeight()-22);
        checkbox.setText((config.improvedBankSearch()?"[x] ":"[ ] ")+"Enable Improved Banking search");
        checkbox.setHidden(false); checkbox.revalidate();
    }
    private void clearWidget()
    {
        if(checkbox!=null) { checkbox.setHidden(true); checkbox.clearActions(); checkbox.setOnOpListener((Object[])null); checkbox.setHasListener(false); }
        checkbox=null; parent=null;
    }
}
