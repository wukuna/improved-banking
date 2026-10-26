package com.windowmarkers;

import java.util.*;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.banktags.BankTag;
import net.runelite.client.plugins.banktags.BankTagsService;
import net.runelite.client.plugins.banktags.TagManager;

/** Session-only custom tag; never writes to Bank Tags' saved tabs or layouts. */
@Singleton
final class BankSearchView
{
    @Inject net.runelite.api.Client client;
    @Inject BankTagsService bankTags;
    @Inject TagManager tags;
    @Inject ItemManager items;
    private final String tagName="improved banking "+UUID.randomUUID();
    private BankTag tag;
    private String name;
    private boolean slots;
    private SetupLoadout loadout;
    private Map<Integer,ItemCatalogueIndex.Rank> ranks=Collections.emptyMap();
    SetupLoadout loadout() { return isActive()?loadout:null; }
    Map<Integer,ItemCatalogueIndex.Rank> ranks() { return isActive()?ranks:Collections.emptyMap(); }
    private List<Marker> groups=Collections.emptyList();

    void show(String name,List<Marker> groups,boolean slots) { show(name,groups,slots,null,Collections.emptyMap()); }
    void show(String name,List<Marker> groups,boolean slots,SetupLoadout loadout,Map<Integer,ItemCatalogueIndex.Rank> ranks)
    {
        clear();
        this.loadout=loadout; this.ranks=ranks;
        this.name=name; this.groups=new ArrayList<>(groups); this.slots=slots;
        Set<Integer> ids=new HashSet<>();
        for(Marker group:groups) if(group.linked()) for(int id:group.linkedItemIds) ids.add(id);
        if(ids.isEmpty()) { this.groups=Collections.emptyList(); return; }
        tag=id -> ids.contains(items.canonicalize(id));
        tags.registerTag(tagName,tag);
        bankTags.openBankTag(tagName,BankTagsService.OPTION_NO_LAYOUT|BankTagsService.OPTION_HIDE_TAG_NAME);
    }
    boolean isActive() { return tag!=null && tagName.equals(bankTags.getActiveTag()); }
    boolean bySlot() { return slots; }
    String name() { return name; }
    List<Marker> groups() { return isActive()?new ArrayList<>(groups):Collections.emptyList(); }
    void update() {
        if(tag!=null && !isActive()) forget();
        if(isActive()) {
            var title=client.getWidget(net.runelite.api.gameval.InterfaceID.Bankmain.TITLE);
            if(title!=null) title.setText("Bank view: "+name.replace('<',' ').replace('>',' ').substring(0,Math.min(48,name.length())));
        }
    }
    void clear() {
        if(isActive()) {
            bankTags.closeBankTag();
        }
        forget();
    }
    private void forget() { if(tag!=null) tags.unregisterTag(tagName); tag=null; name=null; slots=false;loadout=null;ranks=Collections.emptyMap(); groups=Collections.emptyList(); }
}
