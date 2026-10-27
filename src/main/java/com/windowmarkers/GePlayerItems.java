package com.windowmarkers;

import java.util.*;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.*;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.game.ItemManager;

/** Client-thread snapshots only. Nothing is persisted or shared between logins. */
@Singleton
final class GePlayerItems
{
    @Inject Client client;
    @Inject ItemManager itemManager;
    private long account=-1;
    private Map<Integer,Integer> bank=new LinkedHashMap<>();
    private boolean bankSeen, bankFundsKnown;

    void clear() { account=-1;bank.clear();bankSeen=false;bankFundsKnown=false; }
    private boolean context()
    {
        if(client.getGameState()!=GameState.LOGGED_IN) return false;
        long current=client.getAccountHash();
        if(current!=account) { clear();account=current; }
        return current!=-1 && current!=0;
    }
    void bankChanged(ItemContainer container)
    {
        if(!context() || container==null) return;
        bank=read(container);bankSeen=true;bankFundsKnown=true;
    }
    void offersChanged() { bankFundsKnown=false; }
    void observeOpenBank()
    {
        if(!context() || bankSeen && bankFundsKnown) return;
        Widget window=client.getWidget(InterfaceID.Bankmain.UNIVERSE);
        if(window!=null && !window.isHidden()) bankChanged(client.getItemContainer(InventoryID.BANK));
    }
    private Map<Integer,Integer> read(ItemContainer container)
    {
        Map<Integer,Integer> result=new LinkedHashMap<>();
        if(container!=null) for(Item item:container.getItems()) {
            if(item.getId()<0 || item.getQuantity()<=0 || item.getId()==6512) continue;
            ItemComposition definition=itemManager.getItemComposition(item.getId());
            if(definition==null || definition.getPlaceholderTemplateId()!=-1) continue;
            // Only unwrap banknotes here. Variation families can include depleted states;
            // canonicalize also unwraps placeholders and worn variants, not usability.
            int id=definition.getNote()!=-1?definition.getLinkedNoteId():item.getId();
            if(id>=0) result.merge(id,item.getQuantity(),(a,b)->(int)Math.min(Integer.MAX_VALUE,(long)a+b));
        }
        return result;
    }
    static long funds(Map<Integer,Integer> items)
    { return (long)items.getOrDefault(995,0)+1000L*items.getOrDefault(13204,0); }
    Snapshot snapshot()
    {
        if(!context()) return Snapshot.unknown();
        observeOpenBank();
        ItemContainer inventory=client.getItemContainer(InventoryID.INV);
        ItemContainer wornContainer=client.getItemContainer(InventoryID.WORN);
        Map<Integer,Integer> carried=read(inventory),worn=read(wornContainer);
        Set<Integer> ids=new LinkedHashSet<>(bank.keySet());ids.addAll(carried.keySet());ids.addAll(worn.keySet());
        List<ItemCatalogueIndex.Item> facts=new ArrayList<>();
        for(int id:ids) {
            ItemComposition definition=itemManager.getItemComposition(id);
            if(definition==null || definition.getName()==null) continue;
            var stats=itemManager.getItemStats(id);var equipment=stats==null?null:stats.getEquipment();
            int slot=equipment==null?ItemCatalogueIndex.fallbackSlot(id,definition.getName()):equipment.getSlot();
            facts.add(new ItemCatalogueIndex.Item(id,definition.getName(),slot,definition.getInventoryActions()));
        }
        return new Snapshot(bankSeen && inventory!=null && wornContainer!=null,facts,funds(carried)+(bankFundsKnown?funds(bank):0),bankFundsKnown && inventory!=null);
    }
    static final class Snapshot
    {
        final boolean complete, fundsComplete;
        final List<ItemCatalogueIndex.Item> items;
        final long funds;
        Snapshot(boolean complete,List<ItemCatalogueIndex.Item> items,long funds,boolean fundsComplete)
        { this.complete=complete;this.items=Collections.unmodifiableList(new ArrayList<>(items));this.funds=funds;this.fundsComplete=fundsComplete; }
        static Snapshot unknown() { return new Snapshot(false,Collections.emptyList(),0,false); }
        ItemCatalogueIndex.Item owned(ItemCatalogueIndex.Item candidate)
        {
            for(ItemCatalogueIndex.Item item:items) if(item.id==candidate.id) return item;
            // Equivalent equipment only: readiness and retained benefits are directional.
            if(candidate.slot>=0) for(ItemCatalogueIndex.Item item:items)
                if(item.slot==candidate.slot && ItemVariantState.canRepresentOwnedEquivalent(item.fact,candidate.fact)
                    && ItemTaxonomy.baseName(CombatEquivalence.familyName(item.fact)).equals(ItemTaxonomy.baseName(CombatEquivalence.familyName(candidate.fact)))) return item;
            return null;
        }
    }
}
