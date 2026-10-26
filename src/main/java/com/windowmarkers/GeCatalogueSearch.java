package com.windowmarkers;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.*;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.SwingWorker;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarClientID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.ProfileChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.*;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;

/** Public GE search hook; changes result labels only, never offers or input. */
@Singleton
final class GeCatalogueSearch extends Overlay
{
    private static final org.slf4j.Logger log=org.slf4j.LoggerFactory.getLogger(GeCatalogueSearch.class);
    @Inject Client client;
    @Inject ClientThread thread;
    @Inject ItemManager itemManager;
    @Inject WindowMarkersConfig config;
    @Inject EventBus eventBus;
    @Inject OverlayManager overlays;
    @Inject TooltipManager tooltips;
    private volatile boolean active;
    private long generation;
    private boolean preparing;
    private ItemCatalogueIndex index;
    private String query="";
    private ItemCatalogueIndex.Result result;
    private Widget resultContainer;
    private final List<Decoration> decorations=new ArrayList<>();
    private String failure="";
    private boolean geSearchOpen;

    GeCatalogueSearch() { setPosition(OverlayPosition.DYNAMIC); setLayer(OverlayLayer.ABOVE_WIDGETS); }
    void start()
    {
        active=true; eventBus.register(this); overlays.add(this);
        thread.invokeLater(() -> { if(active) prepare(); });
    }
    void stop()
    {
        active=false; eventBus.unregister(this); overlays.remove(this);
        thread.invokeLater(() -> { generation++; preparing=false; clear(); geSearchOpen=false; if(active) prepare(); });
    }
    @Subscribe public void onConfigChanged(ConfigChanged event)
    {
        if(!event.getGroup().equals("windowmarkers") || !event.getKey().startsWith("ge")) return;
        thread.invokeLater(() -> { clear(); failure=""; prepare(); });
    }
    @Subscribe public void onProfileChanged(ProfileChanged event)
    { thread.invokeLater(() -> { clear(); prepare(); }); }
    @Subscribe public void onGameStateChanged(GameStateChanged event)
    {
        if(event.getGameState()==GameState.LOGIN_SCREEN || event.getGameState()==GameState.HOPPING) {
            generation++; preparing=false; index=null; failure=""; clear(); geSearchOpen=false;
        }
        if(event.getGameState()==GameState.LOGGED_IN) prepare();
    }

    private void prepare()
    {
        if(!active || !config.geCatalogueSearch() || client.getGameState()!=GameState.LOGGED_IN
            || index!=null || preparing || !failure.isEmpty()) return;
        preparing=true; final long request=++generation;
        // Resource parsing and immutable index construction stay off client.
        new SwingWorker<Void,Void>() {
            protected Void doInBackground() { ItemCatalogueIndex.fallbackSlot(-1,""); return null; }
            protected void done() {
                try { get(); thread.invokeLater(() -> collect(request)); }
                catch(Exception ex) { thread.invokeLater(() -> failed(request,ex)); }
            }
        }.execute();
    }
    private boolean current(long request) { return active && request==generation; }
    private void failed(long request,Exception ex)
    { if(current(request)) { preparing=false; failure="Catalogue unavailable; ordinary GE search remains active."; log.warn("Could not prepare Improved Banking GE catalogue",ex); } }
    private void collect(long request)
    {
        if(!current(request)) return;
        List<ItemCatalogueIndex.Item> catalogue=new ArrayList<>();
        final int[] cursor={0};
        thread.invokeLater(() -> {
            if(!current(request)) return true;
            if(!config.geCatalogueSearch()) { preparing=false; return true; }
            if(client.getGameState()!=GameState.LOGGED_IN) return false;
            // Cache definitions are not available before the login screen.
            int count=Math.min(65536,client.getItemCount());
            if(count<=0) return false;
            int end=Math.min(cursor[0]+256,count);
            try {
                while(cursor[0]<end) {
                    int id=cursor[0]++;
                    ItemComposition item=itemManager.getItemComposition(id);
                    if(item==null || !item.isGeTradeable() || item.getNote()!=-1 || item.getPlaceholderTemplateId()!=-1) continue;
                    String name=item.getName();
                    if(name==null || name.isBlank() || name.equalsIgnoreCase("null")) continue;
                    var stats=itemManager.getItemStats(id);
                    var eq=stats==null?null:stats.getEquipment();
                    int slot=eq!=null ? eq.getSlot() : ItemCatalogueIndex.fallbackSlot(id,name);
                    AutoCategories.Item fact=new AutoCategories.Item(id,name,stats!=null && stats.isEquipable(),item.getInventoryActions(),
                        eq==null?0:Math.max(eq.getAstab(),Math.max(eq.getAslash(),eq.getAcrush()))+eq.getStr(),
                        eq==null?0:eq.getArange()+eq.getRstr(),eq==null?0:eq.getAmagic()+(int)eq.getMdmg(),slot);
                    catalogue.add(new ItemCatalogueIndex.Item(fact,name,slot));
                }
            } catch(RuntimeException ex) { failed(request,ex); return true; }
            if(cursor[0]<count) return false;
            new SwingWorker<ItemCatalogueIndex,Void>() {
                protected ItemCatalogueIndex doInBackground() { return new ItemCatalogueIndex(catalogue); }
                protected void done() {
                    try {
                        ItemCatalogueIndex built=get();
                        thread.invokeLater(() -> { if(current(request)) { index=built; preparing=false; } });
                    } catch(Exception ex) { thread.invokeLater(() -> failed(request,ex)); }
                }
            }.execute();
            return true;
        });
    }
    @Subscribe(priority=-50) public void onGrandExchangeSearched(GrandExchangeSearched event)
    {
        clear(); geSearchOpen=true;
        if(!active || !config.geCatalogueSearch() || event.isConsumed()) return;
        prepare();
        if(index==null) return;
        String input=client.getVarcStrValue(VarClientID.MESLAYERINPUT);
        if(input==null) return;
        ItemCatalogueIndex.Result next=index.find(input,config.geGuideContext(),"",client.getRealSkillLevel(Skill.HITPOINTS));
        if(next==null) return;
        query=input; result=next;
        event.consume(); client.setGeSearchResultIndex(0);
        boolean overflow=next.items.size()>ItemCatalogueIndex.LIMIT;
        client.setGeSearchResultCount(overflow ? -1 : next.items.size());
        client.setGeSearchResultIds(overflow ? null : next.ids());
    }
    @Subscribe(priority=-200) public void onScriptPostFired(ScriptPostFired event)
    {
        if(event.getScriptId()!=ScriptID.GE_ITEM_SEARCH) return;
        restore();
        if(!valid() || !config.geTierColours()) return;
        Widget container=client.getWidget(InterfaceID.Chatbox.MES_LAYER_SCROLLCONTENTS);
        if(container==null || container.isHidden()) return;
        Widget[] children=container.getDynamicChildren();
        if(children==null || children.length%3!=0) return;
        resultContainer=container;
        // This is the same public result triplet used by RuneLite's GE search
        // highlighter. Verify item identity before touching an associated label.
        for(int i=0;i+2<children.length;i+=3) {
            Widget label=children[i+1];
            if(label==null) continue;
            Widget icon=children[i]!=null && result.ranks.containsKey(children[i].getItemId()) ? children[i] : children[i+2];
            if(icon==null) continue;
            int id=icon.getItemId();
            ItemCatalogueIndex.Rank rank=result.ranks.get(id);
            if(rank==null) continue;
            String text=label.getText();
            if(text==null || text.isBlank()) continue;
            Decoration decoration=new Decoration(label,text,label.getTextColor(),rank);
            if(rank.tier>=1 && rank.tier<=3) {
                Color colour=TierPresentation.colour(config,rank.tier);
                decoration.appliedText="["+TierPresentation.ordinal(rank.tier)+"] "+text;
                decoration.appliedColour=colour.getRGB() & 0xffffff;
                label.setText(decoration.appliedText); label.setTextColor(decoration.appliedColour);
            }
            decorations.add(decoration);
        }
    }
    private boolean valid()
    { return active && config.geCatalogueSearch() && result!=null && client.getGameState()==GameState.LOGGED_IN
        && query.equals(client.getVarcStrValue(VarClientID.MESLAYERINPUT)); }
    @Subscribe public void onBeforeRender(BeforeRender event)
    {
        if(!active) return;
        prepare();
        if(!valid() || (resultContainer!=null && (resultContainer.isHidden()
            || client.getWidget(InterfaceID.Chatbox.MES_LAYER_SCROLLCONTENTS)!=resultContainer))) clear();
        if(client.getGameState()!=GameState.LOGGED_IN) geSearchOpen=false;
    }
    @Subscribe public void onWidgetClosed(WidgetClosed event)
    {
        if(event.getGroupId()==InterfaceID.GE_OFFERS || event.getGroupId()==InterfaceID.CHATBOX)
        { clear(); geSearchOpen=false; }
    }
    void clear() { restore(); result=null; query=""; resultContainer=null; }
    private void restore()
    {
        for(Decoration d:decorations) {
            if(d.appliedText.equals(d.widget.getText())) d.widget.setText(d.text);
            if(d.appliedColour==d.widget.getTextColor()) d.widget.setTextColor(d.colour);
        }
        decorations.clear();
    }
    @Override public Dimension render(Graphics2D graphics)
    {
        if(!active || !config.geCatalogueSearch()) return null;
        net.runelite.api.Point mouse=client.getMouseCanvasPosition();
        if(valid()) for(Decoration d:decorations) {
            Rectangle bounds=d.widget.getBounds();
            if(bounds!=null && resultContainer!=null) bounds=bounds.intersection(resultContainer.getBounds());
            if(!d.widget.isHidden() && bounds!=null && bounds.contains(mouse.getX(),mouse.getY())) {
                tooltips.add(new Tooltip(escape(d.rank.explanation))); break;
            }
        }
        if(geSearchOpen && index==null) {
            Widget area=client.getWidget(InterfaceID.Chatbox.MES_LAYER_SCROLLCONTENTS);
            if(area!=null && !area.isHidden() && area.getBounds().contains(mouse.getX(),mouse.getY()))
                tooltips.add(new Tooltip(failure.isEmpty()?"Improved Banking catalogue is loading. Search again in a moment.":failure));
        }
        return null;
    }
    private static String escape(String text)
    {
        String s=text.length()>850 ? text.substring(0,847)+"..." : text;
        return s.replace("<","(").replace(">",")").replace("\n","<br>");
    }
    private static final class Decoration
    {
        final Widget widget;
        final String text;
        final int colour;
        final ItemCatalogueIndex.Rank rank;
        String appliedText;
        int appliedColour;
        Decoration(Widget widget,String text,int colour,ItemCatalogueIndex.Rank rank)
        { this.widget=widget; this.text=text; this.colour=colour; this.rank=rank; appliedText=text; appliedColour=colour; }
    }
}
