package com.windowmarkers;

import java.util.*;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.banktags.BankTag;
import net.runelite.client.plugins.banktags.BankTagsService;
import net.runelite.client.plugins.banktags.TagManager;

/** Session-only custom tag; does not create saved tag tabs or layouts. */
@Singleton
final class BankSearchView
{
    @Inject net.runelite.api.Client client;
    @Inject BankTagsService bankTags;
    @Inject TagManager tags;
    @Inject ItemManager items;
    @Inject net.runelite.client.callback.ClientThread clientThread;
    private final String tagName="improved banking "+UUID.randomUUID();
    private BankTag tag;
    private String name;
    private String layoutIdentity;
    String layoutIdentity() { return isActive() ? layoutIdentity : null; }
    private boolean slots;
    private boolean reopen, reopenQueued;
    private SetupLoadout loadout;
    private Marker highlight;
    Marker highlight() { return isActive()?highlight:null; }
    private Map<Integer,ItemCatalogueIndex.Rank> ranks=Collections.emptyMap();
    SetupLoadout loadout() { return isActive()?loadout:null; }
    Map<Integer,ItemCatalogueIndex.Rank> ranks() { return isActive()?ranks:Collections.emptyMap(); }
    private List<Marker> groups=Collections.emptyList();

    void show(String name,List<Marker> groups,boolean slots) { show(name,groups,slots,null,Collections.emptyMap()); }
    void show(String name,List<Marker> groups,boolean slots,SetupLoadout loadout,Map<Integer,ItemCatalogueIndex.Rank> ranks)
    { show(name,groups,slots,loadout,ranks,"saved:"+name); }
    void show(String name,List<Marker> groups,boolean slots,SetupLoadout loadout,Map<Integer,ItemCatalogueIndex.Rank> ranks,String layoutIdentity)
    {
        clear();
        this.loadout=loadout; this.ranks=ranks;
        this.name=name; this.groups=new ArrayList<>(groups); this.slots=slots;
        this.layoutIdentity=layoutIdentity;
        Set<Integer> ids=new HashSet<>();
        for(Marker group:groups) if(group.linked()) for(int id:group.linkedItemIds) ids.add(id);
        if(ids.isEmpty() && (loadout==null || !loadout.hasBudgetExamples())) { this.groups=Collections.emptyList(); return; }
        if(loadout!=null && !ids.isEmpty()) {
            highlight=new Marker(0,0,48,36);highlight.scrollWithItems=true;
            highlight.linkedItemIds=ids.stream().mapToInt(Integer::intValue).toArray();
        }
        // The palette saves exact IDs. Canonicalize both sides so a selected
        // worn/ornament variant is not silently lost; do not expand charge families.
        Set<Integer> canonical=new HashSet<>();for(int id:ids) canonical.add(items.canonicalize(id));
        tag=id -> ids.contains(id) || canonical.contains(items.canonicalize(id));
        tags.registerTag(tagName,tag);
        bankTags.openBankTag(tagName,BankTagsService.OPTION_NO_LAYOUT|BankTagsService.OPTION_HIDE_TAG_NAME);
    }
    boolean isActive() { return tag!=null && tagName.equals(bankTags.getActiveTag()); }
    void filter(net.runelite.api.events.ScriptCallbackEvent event)
    {
        if(!"bankSearchFilter".equals(event.getEventName()) || !isActive()
            || client.getWidget(net.runelite.api.gameval.InterfaceID.Bankmain.ITEMS)==null
            || client.getVarcIntValue(net.runelite.api.gameval.VarClientID.MESLAYERMODE)==net.runelite.api.vars.InputType.SEARCH.getType()) return;
        int size=client.getIntStackSize();
        if(size<2) return;
        int[] stack=client.getIntStack();
        if(stack[size-1]<0) return;
        // A temporary view is an exact selection, even if an earlier search
        // subscriber accepted a different item. Normal tabs/search keep their filters.
        stack[size-2]=tag.contains(stack[size-1])?1:0;
    }
    boolean bySlot() { return slots; }
    String name() { return name; }
    List<Marker> groups() { return isActive()?new ArrayList<>(groups):Collections.emptyList(); }
    /** Capture on the client thread before starting a background view calculation. */
    java.util.function.BooleanSupplier unchangedContext()
    {
        var bank=client.getWidget(net.runelite.api.gameval.InterfaceID.Bankmain.UNIVERSE);
        if(client.getGameState()!=net.runelite.api.GameState.LOGGED_IN || bank==null || bank.isHidden()) return () -> false;
        String activeName=bankTags.getActiveTag();BankTag activeTag=bankTags.getActiveBankTag();
        int nativeTab=client.getVarbitValue(net.runelite.api.gameval.VarbitID.BANK_CURRENTTAB);
        int inputMode=client.getVarcIntValue(net.runelite.api.gameval.VarClientID.MESLAYERMODE);
        String input=client.getVarcStrValue(net.runelite.api.gameval.VarClientID.MESLAYERINPUT);
        return () -> client.getGameState()==net.runelite.api.GameState.LOGGED_IN && bank!=null && !bank.isHidden()
            && client.getWidget(net.runelite.api.gameval.InterfaceID.Bankmain.UNIVERSE)==bank
            && nativeTab==client.getVarbitValue(net.runelite.api.gameval.VarbitID.BANK_CURRENTTAB)
            && Objects.equals(activeName,bankTags.getActiveTag()) && activeTag==bankTags.getActiveBankTag()
            && inputMode==client.getVarcIntValue(net.runelite.api.gameval.VarClientID.MESLAYERMODE)
            && Objects.equals(input,client.getVarcStrValue(net.runelite.api.gameval.VarClientID.MESLAYERINPUT));
    }
    // Bank Tags clears its active tag on unload but remembers its name. Keep the
    // registered contents until the bank reopens or the user explicitly changes views.
    void bankClosed() {
        if(isActive()) reopen=true;
        else if(!reopen) forget();
    }
    private boolean bankOpen() {
        var bank=client.getWidget(net.runelite.api.gameval.InterfaceID.Bankmain.UNIVERSE);
        return bank!=null && !bank.isHidden();
    }
    private boolean canReopen() {
        String active=bankTags.getActiveTag();
        return (active==null || tagName.equals(active))
            && client.getVarbitValue(net.runelite.api.gameval.VarbitID.BANK_CURRENTTAB)==0
            && client.getVarcIntValue(net.runelite.api.gameval.VarClientID.MESLAYERMODE)!=net.runelite.api.vars.InputType.SEARCH.getType();
    }
    void update() {
        if(tag==null || !bankOpen()) return;
        if(reopen) {
            if(!canReopen()) { forget(); return; }
            if(!reopenQueued) {
                reopenQueued=true;
                BankTag expected=tag;
                var unchanged=unchangedContext();
                // Opening a tag rebuilds the bank: defer it out of BeforeRender.
                clientThread.invokeLater(() -> {
                    if(tag!=expected) return;
                    reopenQueued=false;
                    if(!bankOpen()) return;
                    if(!unchanged.getAsBoolean() || !canReopen()) { forget(); return; }
                    reopen=false;
                    bankTags.openBankTag(tagName,BankTagsService.OPTION_NO_LAYOUT|BankTagsService.OPTION_HIDE_TAG_NAME);
                });
            }
            return;
        }
        if(tag!=null && !isActive()) forget();
        if(isActive()) {
            var title=client.getWidget(net.runelite.api.gameval.InterfaceID.Bankmain.TITLE);
            if(title!=null) title.setText("Bank view: "+name.replace('<',' ').replace('>',' ').substring(0,Math.min(48,name.length()))+(loadout!=null && loadout.notEnoughGear()?" - not enough gear in bank":""));
        }
    }
    void clear() {
        if(isActive() || tag!=null && reopen && bankTags.getActiveTag()==null) {
            bankTags.closeBankTag();
        }
        forget();
    }
    private void forget() { if(tag!=null) tags.unregisterTag(tagName); tag=null; name=null; slots=false;reopen=false;reopenQueued=false;loadout=null;highlight=null;ranks=Collections.emptyMap(); groups=Collections.emptyList(); }
}
