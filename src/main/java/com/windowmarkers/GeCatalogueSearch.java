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
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.VarClientID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetType;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.ProfileChanged;
import net.runelite.client.events.RuneScapeProfileChanged;
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
    @Inject GePlayerItems playerItems;
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
    private boolean detailsDirty;
    private String notice="";
    private WindowMarkersConfig.ActivityItems activityMode=WindowMarkersConfig.ActivityItems.ALL;
    private Map<Integer,String> upgrades=Collections.emptyMap();
    private GeCategorySearch shopping;
    private List<String> suggestions=Collections.emptyList();
    private Widget tips,tipsParent;
    private Boolean ancientRings;

    GeCatalogueSearch() { setPosition(OverlayPosition.DYNAMIC); setLayer(OverlayLayer.ABOVE_WIDGETS); }
    void start()
    {
        active=true; eventBus.register(this); overlays.add(this);
        thread.invokeLater(() -> { if(active) { playerItems.clear();ancientRings=null;if(config.geCatalogueSearch()) playerItems.observeOpenBank();prepare(); } });
    }
    void stop()
    {
        active=false; eventBus.unregister(this); overlays.remove(this);
        thread.invokeLater(() -> { generation++; preparing=false; playerItems.clear();ancientRings=null;clear(); geSearchOpen=false; if(active) prepare(); });
    }
    @Subscribe public void onConfigChanged(ConfigChanged event)
    {
        if(!event.getGroup().equals("windowmarkers") || !event.getKey().startsWith("ge")) return;
        thread.invokeLater(() -> { clear(); failure="";if(!config.geCatalogueSearch()) playerItems.clear();prepare(); });
    }
    @Subscribe public void onProfileChanged(ProfileChanged event)
    { thread.invokeLater(() -> { playerItems.clear();ancientRings=null;clear(); prepare(); }); }
    @Subscribe public void onRuneScapeProfileChanged(RuneScapeProfileChanged event)
    { thread.invokeLater(() -> { playerItems.clear();ancientRings=null;clear(); }); }
    @Subscribe public void onItemContainerChanged(ItemContainerChanged event)
    {
        if(!active || !config.geCatalogueSearch()) return;
        if(event.getContainerId()==InventoryID.BANK) playerItems.bankChanged(event.getItemContainer());
        if(event.getContainerId()==InventoryID.BANK || event.getContainerId()==InventoryID.INV || event.getContainerId()==InventoryID.WORN) detailsDirty=true;
    }
    @Subscribe public void onGrandExchangeOfferChanged(GrandExchangeOfferChanged event)
    { if(active && config.geCatalogueSearch()) { playerItems.offersChanged();detailsDirty=true; } }
    @Subscribe public void onStatChanged(StatChanged event) { detailsDirty=true; }
    @Subscribe public void onVarbitChanged(VarbitChanged event) { ancientRings=null; }
    @Subscribe public void onGameTick(GameTick event)
    {
        if(!active || !config.geCatalogueSearch()) return;
        // Quest.getState uses QUEST_STATUS_GET internally. It must never be called
        // from the GE search/script callbacks while the native script is running.
        if(config.geCatalogueSearch() && config.geMarkLocked() && ancientRings==null && client.getGameState()==GameState.LOGGED_IN) {
            ancientRings=Quest.DESERT_TREASURE_II__THE_FALLEN_EMPIRE.getState(client)==QuestState.FINISHED;detailsDirty=true;
        }
        playerItems.observeOpenBank();
        if(detailsDirty && valid()) decorate();
        detailsDirty=false;
    }
    @Subscribe public void onGameStateChanged(GameStateChanged event)
    {
        if(event.getGameState()==GameState.LOGIN_SCREEN || event.getGameState()==GameState.HOPPING || event.getGameState()==GameState.CONNECTION_LOST) {
            if(playerItems!=null) playerItems.clear();
            ancientRings=null;
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
            protected Void doInBackground() { ItemCatalogueIndex.fallbackSlot(-1,""); GeActivityFilter.prepare();GeRequirements.prepare();return null; }
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
                private GeCategorySearch categories;
                protected ItemCatalogueIndex doInBackground() { categories=new GeCategorySearch(catalogue);return new ItemCatalogueIndex(catalogue); }
                protected void done() {
                    try {
                        ItemCatalogueIndex built=get();
                        thread.invokeLater(() -> { if(current(request)) { index=built;shopping=categories; preparing=false; } });
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
        if(input==null || input.length()>100 || !BankSearchIndex.plainQuery(input)) return;
        GeActivityFilter.Query options=GeActivityFilter.Query.parse(input,config.geActivityFilter());
        ItemCatalogueIndex.Result next=shopping==null || !options.text.equals(input)?null:shopping.find(options.text);
        if(next==null) next=index.find(options.text,config.geGuideContext(),"",client.getRealSkillLevel(Skill.HITPOINTS));
        // Ownership words are GE activity qualifiers, not a general item-name rewrite.
        if(!options.text.equals(input) && (next==null || !next.activity)) next=index.find(input,config.geGuideContext(),"",client.getRealSkillLevel(Skill.HITPOINTS));
        if(next==null) return;
        if(shopping!=null) suggestions=shopping.suggestions(options.text,next.activity);
        activityMode=next.activity?options.mode:WindowMarkersConfig.ActivityItems.ALL;
        if(next.activity && (activityMode!=WindowMarkersConfig.ActivityItems.ALL || config.geHideWorseOwned())) {
            GeActivityFilter.Outcome filtered=GeActivityFilter.apply(next,playerItems.snapshot(),GeRequirements.capture(client,ancientRings),activityMode,config.geHideWorseOwned());
            next=filtered.result;upgrades=filtered.upgrades;notice=filtered.notice;
        }
        query=input; result=next;
        event.consume(); client.setGeSearchResultIndex(0);
        boolean overflow=next.items.size()>ItemCatalogueIndex.LIMIT;
        client.setGeSearchResultCount(overflow ? -1 : next.items.size());
        client.setGeSearchResultIds(overflow ? null : next.ids());
    }
    @Subscribe(priority=-200) public void onScriptPostFired(ScriptPostFired event)
    {
        if(event.getScriptId()!=ScriptID.GE_ITEM_SEARCH) return;
        decorate();
    }
    private void decorate()
    {
        restore();
        if(!valid()) return;
        Widget container=client.getWidget(InterfaceID.Chatbox.MES_LAYER_SCROLLCONTENTS);
        if(container==null || container.isHidden()) return;
        Widget[] children=container.getDynamicChildren();
        if(children==null || children.length%3!=0) return;
        resultContainer=container;
        Map<Integer,ItemCatalogueIndex.Item> byId=new HashMap<>();for(ItemCatalogueIndex.Item item:result.items) byId.put(item.id,item);
        boolean ownership=result.activity && (config.geMarkOwned() || activityMode!=WindowMarkersConfig.ActivityItems.ALL);
        GePlayerItems.Snapshot owned=(ownership || config.geShowPrices())?playerItems.snapshot():GePlayerItems.Snapshot.unknown();
        GeRequirements requirements=config.geMarkLocked()?GeRequirements.capture(client,ancientRings):null;
        // This is the same public result triplet used by RuneLite's GE search
        // highlighter. Verify item identity before touching an associated label.
        for(int i=0;i+2<children.length;i+=3) {
            Widget label=children[i+1];
            if(label==null) continue;
            Widget icon=children[i]!=null && byId.containsKey(children[i].getItemId()) ? children[i] : children[i+2];
            if(icon==null) continue;
            int id=icon.getItemId();
            ItemCatalogueIndex.Item item=byId.get(id);if(item==null) continue;
            ItemCatalogueIndex.Rank rank=result.ranks.getOrDefault(id,new ItemCatalogueIndex.Rank(0,"No ranked recommendation for this result."));
            String text=label.getText();
            if(text==null || text.isBlank()) continue;
            Decoration decoration=new Decoration(label,text,label.getTextColor(),rank);
            String badges="";
            if(config.geTierColours() && rank.tier>=1 && rank.tier<=3) {
                Color colour=TierPresentation.colour(config,rank.tier);
                badges="["+rank.tier+"]";
                decoration.appliedColour=colour.getRGB() & 0xffffff;
            }
            String detail=rank.explanation;
            if(!badges.isEmpty()) detail="["+rank.tier+"] = "+TierPresentation.ordinal(rank.tier)+" guide tier for this setup and role.\n"+detail;
            if(ownership) {
                ItemCatalogueIndex.Item held=owned.owned(item);
                if(held!=null) {
                    badges="[O]"+badges;
                    detail+="\n[O] = Owned"+(held.id==id?"": " equivalent")+": "+held.name+" (last observed bank, inventory or equipment this login).";
                }
                if(!owned.complete) detail+="\nOpen your bank once this login before relying on ownership filters.";
            }
            if(upgrades.containsKey(id)) { badges="[N]"+badges;detail+="\n[N] = Next guide upgrade. "+upgrades.get(id); }
            if(requirements!=null) {
                String locked=requirements.locked(item);
                if(!locked.isEmpty()) { badges="<col=ff7777>[!]</col>"+badges;detail="[!] = Requires: "+locked+".\n"+detail; }
                detail+="\nRequirement checks cover reviewed skills/unlocks only; other quests, charges and set conditions may apply.";
            }
            if(config.geShowPrices()) {
                long price=itemManager.getItemPrice(id);
                String colour=config.gePriceAffordability()?priceColour(price,owned):"c0c0c0";
                badges+=(badges.isEmpty()?"":" ")+"<col="+colour+">"+(price>0?"~"+compactPrice(price):"~?")+"</col>";
                detail="Guide price: "+(price>0?String.format(Locale.ROOT,"%,d gp each",price):"unavailable")+". Cached RuneLite price; not a guaranteed offer.\n"+detail;
                if(config.gePriceAffordability()) detail="Funds: "+String.format(Locale.ROOT,"%,d gp",owned.funds)+
                    (owned.fundsComplete?" observed in inventory/bank coins and platinum.":" confirmed in inventory; bank balance unverified. Open the bank to refresh.")+
                    " One item at guide price; buy limits and account restrictions still apply.\n"+detail;
            }
            if(!notice.isEmpty()) detail=notice+"\n"+detail;
            decoration.appliedText=GeResultLabel.format(label,text,badges);
            decoration.explanation=item.name+"\n"+detail;
            if(!text.equals(decoration.appliedText)) label.setText(decoration.appliedText);
            if(label.getTextColor()!=decoration.appliedColour) label.setTextColor(decoration.appliedColour);
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
        updateTips();
    }
    @Subscribe public void onWidgetClosed(WidgetClosed event)
    {
        if(event.getGroupId()==InterfaceID.GE_OFFERS || event.getGroupId()==InterfaceID.CHATBOX)
        { clear(); geSearchOpen=false; }
    }
    void clear() { restore(); result=null; query=""; resultContainer=null;notice="";upgrades=Collections.emptyMap();activityMode=WindowMarkersConfig.ActivityItems.ALL;suggestions=Collections.emptyList();if(tips!=null) tips.setHidden(true); }
    private void clearTips()
    {
        if(tips!=null) { tips.setHidden(true);tips.clearActions(); }
        tips=null;tipsParent=null;
    }
    private void updateTips()
    {
        if(!valid() || !config.geSearchTips() || suggestions.isEmpty() && notice.isEmpty()) { if(tips!=null) tips.setHidden(true);return; }
        Widget parent=client.getWidget(InterfaceID.Chatbox.MES_LAYER);
        if(parent==null || parent.isHidden() || parent.getWidth()<300 || parent.getHeight()<85) { if(tips!=null) tips.setHidden(true);return; }
        if(parent!=tipsParent || tips==null || parent.getChildren()==null || !Arrays.asList(parent.getChildren()).contains(tips)) {
            clearTips();tipsParent=parent;tips=parent.createChild(-1,WidgetType.TEXT);
            tips.setFontId(494);tips.setOriginalWidth(110);tips.setOriginalHeight(17);
        }
        // Keep the hint in the header, outside the scrolling item results.
        tips.setOriginalX(8);tips.setOriginalY(2);
        Widget prompt=client.getWidget(InterfaceID.Chatbox.MES_TEXT);
        tips.setTextColor(prompt==null?0:prompt.getTextColor());tips.setTextShadowed(prompt!=null && prompt.getTextShadowed());
        tips.setText(notice.isEmpty()?"Search tips [?]":"Search notice [?]");tips.setHidden(false);tips.revalidate();
    }
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
        if(valid() && tips!=null && !tips.isHidden() && tips.getBounds().contains(mouse.getX(),mouse.getY())) {
            String text=notice;
            if(!suggestions.isEmpty()) text+=(text.isEmpty()?"":"\n")+"Narrow your search. Type one of:\n"+String.join("\n",suggestions);
            tooltips.add(new Tooltip(escape(text)));return null;
        }
        if(valid()) for(Decoration d:decorations) {
            Rectangle bounds=d.widget.getBounds();
            if(bounds!=null && resultContainer!=null) bounds=bounds.intersection(resultContainer.getBounds());
            if(!d.widget.isHidden() && bounds!=null && bounds.contains(mouse.getX(),mouse.getY())) {
                tooltips.add(new Tooltip(escape(d.explanation))); break;
            }
        }
        if(valid() && !notice.isEmpty() && result.items.isEmpty()) {
            Widget area=client.getWidget(InterfaceID.Chatbox.MES_LAYER_SCROLLCONTENTS);
            if(area!=null && !area.isHidden() && area.getBounds().contains(mouse.getX(),mouse.getY())) tooltips.add(new Tooltip(escape(notice)));
        }
        if(geSearchOpen && index==null) {
            Widget area=client.getWidget(InterfaceID.Chatbox.MES_LAYER_SCROLLCONTENTS);
            if(area!=null && !area.isHidden() && area.getBounds().contains(mouse.getX(),mouse.getY()))
                tooltips.add(new Tooltip(failure.isEmpty()?"Improved Banking catalogue is loading. Search again in a moment.":failure));
        }
        return null;
    }
    static String priceColour(long price,GePlayerItems.Snapshot owned)
    { return price<=0?"c0c0c0":price<=owned.funds?"55dd88":owned.fundsComplete?"ff7777":"c0c0c0"; }
    static String compactPrice(long price)
    {
        if(price<1000) return Long.toString(price);
        long unit=price>=1_000_000_000_000L?1_000_000_000_000L:price>=1_000_000_000L?1_000_000_000L:price>=1_000_000L?1_000_000L:1000L;
        return String.format(Locale.ROOT,"%.1f",(double)price/unit).replaceFirst("\\.0$","")+(unit==1000?"k":unit==1_000_000?"m":unit==1_000_000_000?"b":"t");
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
        String explanation;
        Decoration(Widget widget,String text,int colour,ItemCatalogueIndex.Rank rank)
        { this.widget=widget; this.text=text; this.colour=colour; this.rank=rank; appliedText=text; appliedColour=colour;explanation=rank.explanation; }
    }
}
