package com.windowmarkers;

import java.util.*;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;

/** Account-local review facts, never instructions to drop, destroy, withdraw or sell. */
final class BankCleanup
{
    static boolean isReview(ItemCatalogueIndex.Rank rank) { return rank!=null && rank.role.startsWith("cleanup:"); }
    static java.awt.Color colour(ItemCatalogueIndex.Rank rank)
    { return new java.awt.Color(rank!=null && rank.role.equals("cleanup:niche")?0xA0E1BC6B:0x709AAAAA,true); }
    static void style(Marker marker)
    { marker.borderArgb=0x709AAAAA;marker.backgroundArgb=null;marker.style=WindowMarkersConfig.BorderStyle.PLAIN; }
    static final String JUNK="Junk review",SELL="Sell candidates",ACTIVE="Quest items — active quest",FUTURE="Quest items — future quest",DONE="Quest items — completed uses",UNKNOWN="Quest items — progress unknown";
    private static final List<String[]> RULES=ItemTaxonomy.readRows("bank-cleanup-items.tsv",5);
    private static final Set<Integer> TOOLS=new HashSet<>(Arrays.asList(1265,1267,1269,1271,1273,1275,11920,1349,1351,1353,1355,1357,1359,6739));
    // These ordinary tools also have repeatable Keldagrim task uses.
    private static final Set<Integer> REPEAT_TOOLS=new HashSet<>(Arrays.asList(1267,1349,1353));
    final Map<Integer,ItemCatalogueIndex.Rank> junk=new LinkedHashMap<>(),sell=new LinkedHashMap<>();
    private final Map<String,Map<Integer,ItemCatalogueIndex.Rank>> questGroups=new HashMap<>();
    static final class Progress
    {
        final QuestState witch,tale,giant,fishing;final int cooking,farming,mining,woodcutting;final boolean members;
        Progress(QuestState witch,QuestState tale,QuestState giant,QuestState fishing,int cooking,int farming,int mining,int woodcutting)
        { this(witch,tale,giant,fishing,cooking,farming,mining,woodcutting,false); }
        Progress(QuestState witch,QuestState tale,QuestState giant,QuestState fishing,int cooking,int farming,int mining,int woodcutting,boolean members)
        { this.witch=witch;this.tale=tale;this.giant=giant;this.fishing=fishing;this.cooking=cooking;this.farming=farming;this.mining=mining;this.woodcutting=woodcutting;this.members=members; }
        static Progress unknown() { return new Progress(null,null,null,null,0,0,0,0); }
        /** Called between client events on the client thread; not inside a CS2 callback. */
        static Progress capture(Client client)
        {
            if(client.getGameState()!=GameState.LOGGED_IN) return unknown();
            return new Progress(state(Quest.WITCHS_POTION,client),state(Quest.FORGETTABLE_TALE,client),state(Quest.THE_GIANT_DWARF,client),state(Quest.FISHING_CONTEST,client),
                client.getRealSkillLevel(Skill.COOKING),client.getRealSkillLevel(Skill.FARMING),client.getRealSkillLevel(Skill.MINING),client.getRealSkillLevel(Skill.WOODCUTTING),client.getWorldType().contains(net.runelite.api.WorldType.MEMBERS));
        }
        private static QuestState state(Quest quest,Client client) { try { return quest.getState(client); } catch(RuntimeException unavailable) { return null; } }
        String note(boolean chicken)
        {
            QuestState state=chicken?tale:witch;String name=chicken?"Forgettable Tale...":"Witch's Potion";
            if(state==null) return name+": progress unavailable; keep its quest use in mind.";
            if(state==QuestState.FINISHED) return name+": completed. "+(chicken?"Repeatable Keldagrim task use remains.":"The recorded quest use is finished; collector value may remain.");
            if(state==QuestState.IN_PROGRESS) return name+": in progress. "+(chicken?"One possible random request; not proof that it is your assigned item.":"Needed for this quest unless already handed in.");
            if(!chicken) return name+": not started; no skill or prerequisite quest requirements.";
            List<String> missing=new ArrayList<>();
            if(giant!=QuestState.FINISHED) missing.add(giant==null?"The Giant Dwarf progress unknown":"The Giant Dwarf incomplete");
            if(fishing!=QuestState.FINISHED) missing.add(fishing==null?"Fishing Contest progress unknown":"Fishing Contest incomplete");
            if(cooking<22) missing.add("22 Cooking for completion (boostable)");
            if(farming<17) missing.add("17 Farming (boostable)");
            return name+": not started. "+(missing.isEmpty()?"Reviewed skill/prerequisite checks met; other access conditions may apply.":"Requirements to review: "+String.join(", ",missing)+".");
        }
    }
    BankCleanup(List<AutoCategories.Item> bank,Set<Integer> tradeable,Progress progress)
    {
        Map<Integer,AutoCategories.Item> owned=new HashMap<>();for(AutoCategories.Item item:bank) owned.put(item.id,item);
        for(String[] row:RULES) {
            int id=Integer.parseInt(row[0]);AutoCategories.Item item=owned.get(id);
            // Reviewed rare burnt items can survive in banks despite unavailable-normal-source guards.
            // This narrow review label grants no food, equipment or activity capability.
            if(item==null || !row[1].equalsIgnoreCase(item.name)
                || ItemVariantState.identityNeedsReview(item) && !row[2].equals("rare")) continue;
            boolean niche=!row[2].equals("review");String reason=row[3];
            if(id==2144 || id==2146) {
                reason=id==2144?reason+"\n"+progress.note(true):progress.note(false)+"\n"+reason;
                QuestState state=id==2144?progress.tale:progress.witch;
                String group=state==null?UNKNOWN:state==QuestState.FINISHED?DONE:state==QuestState.IN_PROGRESS?ACTIVE:FUTURE;
                questGroups.computeIfAbsent(group,k->new LinkedHashMap<>()).put(id,new ItemCatalogueIndex.Rank(0,reason,"cleanup:niche"));
                niche=id==2144 || state!=QuestState.FINISHED;
            }
            junk.put(id,new ItemCatalogueIndex.Rank(0,row[1]+"\n"+reason+"\nReview only; nothing is discarded or sold.\n"+row[4],niche?"cleanup:niche":"cleanup:review"));
        }
        List<AutoCategories.Item> tools=new ArrayList<>();
        for(AutoCategories.Item item:bank) if(TOOLS.contains(item.id) && tradeable.contains(item.id)
            && (progress.members || item.id!=11920 && item.id!=6739)) tools.add(item);
        LowerTierFilter.exclusions(tools,progress.mining,progress.woodcutting,0).forEach((id,why)-> {
            if(!REPEAT_TOOLS.contains(id)) sell.put(id,new ItemCatalogueIndex.Rank(0,"Sell candidate for ordinary gathering: "+why+".\nLow-cost spares or personal plans can still justify keeping it. This is not a claim of universal redundancy.","cleanup:sell"));
        });
        // Unmapped quest-related objects remain unknown; completion is not guessed from their name.
        for(AutoCategories.Item item:bank) if(WikiCatalogue.hasFacet(item,"quest") && item.id!=2144 && item.id!=2146)
            questGroups.computeIfAbsent(UNKNOWN,k->new LinkedHashMap<>()).put(item.id,new ItemCatalogueIndex.Rank(0,"Quest-related item. Exact quest/stage uses are not yet mapped here; no completed-quest or disposal claim is made.","cleanup:niche"));
    }
    static String kind(String query)
    {
        String text=SearchText.normalize(query);
        if(text.equals("junk") || text.equals("junk review")) return JUNK;
        if(text.equals("sellable") || text.equals("sell candidates")) return SELL;
        if(text.equals("quest active") || text.equals(SearchText.normalize(ACTIVE))) return ACTIVE;
        if(text.equals("quest future") || text.equals(SearchText.normalize(FUTURE))) return FUTURE;
        if(text.equals("quest completed") || text.equals(SearchText.normalize(DONE))) return DONE;
        if(text.equals("quest unknown") || text.equals(SearchText.normalize(UNKNOWN))) return UNKNOWN;
        return null;
    }
    static boolean isQuery(String query) { return query!=null && kind(query)!=null; }
    BankSearchIndex.Match search(String query)
    {
        String kind=kind(query);if(kind==null) return null;
        Map<Integer,ItemCatalogueIndex.Rank> notes=kind.equals(JUNK)?junk:kind.equals(SELL)?sell:questGroups.getOrDefault(kind,Collections.emptyMap());
        return new BankSearchIndex.Match(new HashSet<>(notes.keySet()),new LinkedHashMap<>(notes),false);
    }
}
