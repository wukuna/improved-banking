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
    @Inject net.runelite.client.config.ConfigManager configs;
    private final String tagName="improved banking "+UUID.randomUUID();
    private BankTag tag;
    private String name;
    private boolean slots;
    private List<Marker> groups=Collections.emptyList();

    void show(String name,List<Marker> groups,boolean slots)
    {
        clear();
        this.name=name; this.groups=new ArrayList<>(groups); this.slots=slots;
        Set<Integer> ids=new HashSet<>();
        for(Marker group:groups) if(group.linked()) for(int id:group.linkedItemIds) ids.add(id);
        if(ids.isEmpty()) { this.groups=Collections.emptyList(); return; }
        tag=id -> ids.contains(items.canonicalize(id));
        tags.registerTag(tagName,tag);
        String remembered=configs.getConfiguration("banktags","tab");
        bankTags.openBankTag(tagName,BankTagsService.OPTION_NO_LAYOUT|BankTagsService.OPTION_HIDE_TAG_NAME);
        // The public API also remembers its active name. Do not leave an
        // ephemeral UUID in another plugin's persistent last-tab preference.
        if(tagName.equals(configs.getConfiguration("banktags","tab"))) remember(remembered);
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
            String remembered=configs.getConfiguration("banktags","tab");
            bankTags.closeBankTag();
            if("".equals(configs.getConfiguration("banktags","tab"))) remember(remembered);
        }
        forget();
    }
    private void remember(String name) {
        if(name==null) configs.unsetConfiguration("banktags","tab");
        else configs.setConfiguration("banktags","tab",name);
    }
    private void forget() { if(tag!=null) tags.unregisterTag(tagName); tag=null; name=null; slots=false; groups=Collections.emptyList(); }
}
