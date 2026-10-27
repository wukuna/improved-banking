package com.windowmarkers;

import java.util.*;
import net.runelite.api.QuestState;
import org.junit.Test;
import static org.junit.Assert.*;

public class BankCleanupTest
{
    private static AutoCategories.Item item(int id,String name) { return new AutoCategories.Item(id,name,false,null,0,0,0); }
    private static BankCleanup.Progress progress(QuestState witch,QuestState tale,int mining)
    { return new BankCleanup.Progress(witch,tale,QuestState.FINISHED,QuestState.FINISHED,22,17,mining,99,true); }
    private static List<AutoCategories.Item> burnt() { return Arrays.asList(item(2144,"Burnt chicken"),item(2146,"Burnt meat"),item(387,"Burnt shark")); }

    @Test public void finishedQuestDoesNotEraseRepeatableChickenUseOrRemoveItFromJunk()
    {
        BankCleanup review=new BankCleanup(burnt(),Collections.emptySet(),progress(QuestState.FINISHED,QuestState.FINISHED,99));
        assertEquals(new HashSet<>(Arrays.asList(2144,2146,387)),review.search("junk").ids);
        assertEquals("cleanup:niche",review.junk.get(2144).role);
        assertTrue(review.junk.get(2144).explanation.contains("Repeatable Keldagrim task use remains"));
        assertEquals("cleanup:review",review.junk.get(2146).role);
        assertTrue(review.junk.get(2146).explanation.contains("recorded quest use is finished"));
        assertEquals(new HashSet<>(Arrays.asList(2144,2146)),review.search("quest completed").ids);
        assertFalse(review.junk.get(2144).explanation.contains("useless"));
    }
    @Test public void activeFutureAndUnknownProgressAreKeptSeparate()
    {
        BankCleanup review=new BankCleanup(burnt(),Collections.emptySet(),progress(QuestState.IN_PROGRESS,QuestState.NOT_STARTED,99));
        assertEquals(Collections.singleton(2146),review.search("quest active").ids);
        assertEquals(Collections.singleton(2144),review.search("quest future").ids);
        assertTrue(review.junk.get(2146).explanation.contains("unless already handed in"));
        BankCleanup unknown=new BankCleanup(burnt(),Collections.emptySet(),BankCleanup.Progress.unknown());
        assertEquals(new HashSet<>(Arrays.asList(2144,2146)),unknown.search("quest unknown").ids);
        assertTrue(unknown.search("quest completed").ids.isEmpty());
        assertEquals("cleanup:niche",unknown.junk.get(2146).role);
        BankCleanup.Progress unmet=new BankCleanup.Progress(null,QuestState.NOT_STARTED,QuestState.NOT_STARTED,QuestState.FINISHED,1,1,1,1);
        assertTrue(unmet.note(true).contains("boostable"));
        assertTrue(unmet.note(true).contains("Giant Dwarf incomplete"));
        assertFalse(unmet.note(true).contains("ineligible"));
    }
    @Test public void nicheContainersRareItemsAndUnknownUsesStayInJunkWithReasons()
    {
        BankCleanup review=new BankCleanup(Arrays.asList(item(2005,"Burnt stew"),item(1867,"Burnt pitta bread"),item(3383,"Burnt eel"),item(999999,"Burnt shark"),item(387,"Raw shark")),Collections.emptySet(),BankCleanup.Progress.unknown());
        assertEquals(new HashSet<>(Arrays.asList(2005,1867,3383)),review.search("junk").ids);
        assertTrue(review.junk.get(2005).explanation.contains("recover bowl"));
        assertTrue(review.junk.get(1867).explanation.contains("Rare/collector"));
        assertTrue(review.junk.get(3383).explanation.contains("not been fully established"));
        for(ItemCatalogueIndex.Rank rank:review.junk.values()) assertEquals("cleanup:niche",rank.role);
    }
    @Test public void sellCandidatesRequireKnownUsableOwnedUpgradeAndDoNotRankCombatGear()
    {
        List<AutoCategories.Item> bank=Arrays.asList(item(1271,"Adamant pickaxe"),item(1273,"Mithril pickaxe"),item(1275,"Rune pickaxe"),item(11920,"Dragon pickaxe"),item(1267,"Iron pickaxe"),item(1349,"Iron axe"),item(1353,"Steel axe"),item(6739,"Dragon axe"),item(28997,"Dual macuahuitl"),item(22325,"Scythe of vitur"),item(23677,"Crystal pickaxe (inactive)"));
        Set<Integer> tradeable=new HashSet<>();for(AutoCategories.Item item:bank) tradeable.add(item.id);
        BankCleanup low=new BankCleanup(bank,tradeable,progress(null,null,21));
        assertFalse(low.sell.containsKey(1271));
        BankCleanup high=new BankCleanup(bank,tradeable,progress(null,null,61));
        assertTrue(high.sell.toString(),high.sell.containsKey(1271));
        for(int id:new int[]{11920,1267,1273,1275,1349,1353,28997,22325,23677}) assertFalse(high.sell.containsKey(id));
        assertTrue(new BankCleanup(bank,Collections.emptySet(),progress(null,null,99)).sell.isEmpty());
        assertTrue(new BankCleanup(Arrays.asList(item(1271,"Adamant pickaxe"),item(11920,"Dragon pickaxe")),tradeable,
            new BankCleanup.Progress(null,null,null,null,1,1,99,99,false)).sell.isEmpty());
    }
    @Test public void newPreviewAndSavedGroupsKeepAccountSpecificCleanupReasons()
    {
        BankSearchIndex complete=new BankSearchIndex(burnt(),Collections.emptyList(),new CustomPresets(),99,Collections.emptySet(),progress(QuestState.FINISHED,QuestState.FINISHED,99));
        Marker marker=new Marker(0,0,48,36);marker.guideQuery="junk";marker.linkedItemIds=new int[]{2144,2146};
        assertEquals(2,complete.ranks(Collections.singletonList(marker)).size());
        assertEquals("cleanup:review",complete.ranks(Collections.singletonList(marker)).get(2146).role);
        BankSearchIndex unknown=new BankSearchIndex(burnt(),Collections.singletonList(marker),new CustomPresets());
        assertEquals("cleanup:niche",unknown.ranks(Collections.singletonList(marker)).get(2146).role);
        BankCleanup.style(marker);assertNull(marker.backgroundArgb);assertTrue(new java.awt.Color(marker.borderArgb,true).getAlpha()<128);
    }
}
