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
    private String guideContext="";
    private Marker highlight;
    private boolean slots;
    private long revision;
    private boolean running;
    private volatile long context;

    void start() { running=true; }
    void stop() { boolean restore=searching(); running=false; reset(); if(restore) bankSearch.layoutBank(); }
    void reset() { context++; revision++; query=null; index=null; matches=Collections.emptySet(); ranks=Collections.emptyMap(); highlight=null; slots=false; clearWidget(); }
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
            if(query!=null) { revision++; query=null; matches=Collections.emptySet(); ranks=Collections.emptyMap(); highlight=null; slots=false; }
            if(checkbox!=null) checkbox.setHidden(true); return;
        }
        updateCheckbox();
        String next=client.getVarcStrValue(VarClientID.MESLAYERINPUT);
        if(!config.improvedBankSearch()) next="";
        String nextContext=config.geGuideContext()==null?"":config.geGuideContext();
        boolean queryChanged=!Objects.equals(query,next) || !Objects.equals(guideContext,nextContext);
        if(!queryChanged && !indexChanged) return;
        Set<Integer> previous=new HashSet<>(matches); boolean previousSlots=slots;
        query=next; guideContext=nextContext;
        if(queryChanged) { matches=Collections.emptySet(); ranks=Collections.emptyMap(); highlight=null; slots=false; }
        final long token=++revision;
        final BankSearchIndex snapshot=index; final String text=next,guide=nextContext;
        if(snapshot==null || !BankSearchIndex.plainQuery(text)) {
            if(!previous.isEmpty() || previousSlots) bankSearch.layoutBank();
            return;
        }
        new SwingWorker<BankSearchIndex.Match,Void>() {
            protected BankSearchIndex.Match doInBackground() { return snapshot.search(text,guide); }
            protected void done() { try {
                final BankSearchIndex.Match found=get();
                clientThread.invokeLater(() -> {
                    if(!running || token!=revision || !searching() || !config.improvedBankSearch()
                        || !Objects.equals(guide,config.geGuideContext()==null?"":config.geGuideContext())
                        || !Objects.equals(text,client.getVarcStrValue(VarClientID.MESLAYERINPUT))) return;
                    matches=new HashSet<>(found.ids); slots=found.slots;
                    // A tab's own membership must not be expanded by category search.
                    if(bankTags.getActiveBankTag()!=null) matches.removeIf(id -> !bankTags.getActiveBankTag().contains(id));
                    ranks=new HashMap<>(found.ranks); ranks.keySet().retainAll(matches);
                    highlight=null;
                    if(!matches.isEmpty()) {
                        highlight=new Marker(0,0,48,36); highlight.scrollWithItems=true;
                        highlight.linkedItemIds=matches.stream().mapToInt(Integer::intValue).toArray();
                    }
                    if(!matches.equals(previous) || slots!=previousSlots || (queryChanged && !matches.isEmpty())) bankSearch.layoutBank();
                });
            } catch(Exception ex) {
                clientThread.invokeLater(() -> {
                    if(!running || token!=revision) return;
                    matches=Collections.emptySet(); ranks=Collections.emptyMap(); highlight=null; slots=false;
                    if(searching() && (!previous.isEmpty() || previousSlots)) bankSearch.layoutBank();
                    log.warn("Could not refresh Improved Banking search; using ordinary bank search",ex);
                });
            } }
        }.execute();
    }
    void filter(ScriptCallbackEvent event)
    {
        if(!"bankSearchFilter".equals(event.getEventName()) || !searching() || !config.improvedBankSearch() || !currentGuide()) return;
        int size=client.getIntStackSize(),strings=client.getObjectStackSize();
        if(size<2 || strings<1) return;
        Object raw=client.getObjectStack()[strings-1];
        if(!(raw instanceof String) || !Objects.equals(query,raw) || !BankSearchIndex.plainQuery(query)) return;
        int id=client.getIntStack()[size-1]; if(id<0) return;
        if(bankTags.getActiveBankTag()!=null && !bankTags.getActiveBankTag().contains(id)) return;
        if(matches.contains(itemManager.canonicalize(id))) client.getIntStack()[size-2]=1;
    }
    private boolean currentGuide() { return Objects.equals(guideContext,config.geGuideContext()==null?"":config.geGuideContext()); }
    Marker highlight() { return searching() && config.improvedBankSearch() && currentGuide()
        && Objects.equals(query,client.getVarcStrValue(VarClientID.MESLAYERINPUT))?highlight:null; }
    void closeSearch() { if(searching()) bankSearch.reset(true); revision++; query=null; matches=Collections.emptySet(); ranks=Collections.emptyMap(); highlight=null; slots=false; }
    Map<Integer,ItemCatalogueIndex.Rank> ranks() { return highlight()==null?Collections.emptyMap():Collections.unmodifiableMap(ranks); }
    boolean groupBySlot() { return searching() && config.improvedBankSearch() && currentGuide() && slots && !matches.isEmpty()
        && Objects.equals(query,client.getVarcStrValue(VarClientID.MESLAYERINPUT)); }
    String query() { return query; }
    Set<Integer> matches() { return searching() && config.improvedBankSearch() && currentGuide()
        && Objects.equals(query,client.getVarcStrValue(VarClientID.MESLAYERINPUT))?new HashSet<>(matches):Collections.emptySet(); }
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
                configs.setConfiguration("windowmarkers","improvedBankSearch",!config.improvedBankSearch());
                query=null; revision++; matches=Collections.emptySet(); ranks=Collections.emptyMap(); highlight=null; update(); bankSearch.layoutBank();
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
