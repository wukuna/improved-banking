package com.windowmarkers;

import java.awt.image.BufferedImage;
import java.util.*;
import java.util.function.*;
import javax.inject.Inject;
import javax.swing.*;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;

/** Incremental client-thread reads; fuzzy ranking runs off the client thread. */
final class BankItemSearch
{
    static final class Choice
    {
        final int id;
        final String name, search;
        BufferedImage image;
        Choice(int id,String name) { this.id=id; this.name=name; search=SearchText.normalize(name); }
        @Override public String toString() { return name+" (#"+id+")"; }
    }
    @Inject Client client;
    @Inject ClientThread thread;
    @Inject ItemManager items;
    private final List<Choice> catalogue=new ArrayList<>();
    private int cursor,size;
    void search(String query,Set<Integer> allowed,BooleanSupplier wanted,Consumer<List<Choice>> done)
    {
        thread.invokeLater(() -> {
            if(!wanted.getAsBoolean()) return true;
            int count=Math.min(client.getItemCount(),100000);
            if(count!=size) { catalogue.clear(); cursor=0; size=count; }
            int end=Math.min(cursor+256,size);
            while(cursor<end)
            {
                int id=cursor++; var item=items.getItemComposition(id); String name=item.getName();
                if(name!=null && !name.isBlank() && !name.equalsIgnoreCase("null") && item.getNote()==-1 && item.getPlaceholderTemplateId()==-1)
                    catalogue.add(new Choice(id,name));
            }
            if(cursor<size) return false;
            List<Choice> snapshot=new ArrayList<>(catalogue);
            new SwingWorker<List<Choice>,Void>() {
                protected List<Choice> doInBackground() { return rank(snapshot,query,allowed); }
                protected void done() {
                    if(!wanted.getAsBoolean()) return;
                    try {
                        List<Choice> matches=get();
                        thread.invokeLater(() -> {
                            if(!wanted.getAsBoolean()) return;
                            for(Choice choice:matches) choice.image=items.getImage(choice.id);
                            SwingUtilities.invokeLater(() -> { if(wanted.getAsBoolean()) done.accept(matches); });
                        });
                    } catch(Exception ex) { done.accept(Collections.emptyList()); }
                }
            }.execute();
            return true;
        });
    }
    static List<Choice> rank(List<Choice> choices,String query,Set<Integer> allowed)
    {
        String needle=SearchText.normalize(query);
        Map<Choice,Integer> scores=new IdentityHashMap<>();
        for(Choice c:choices) if(allowed==null || allowed.contains(c.id))
        { int score=SearchText.normalizedScore(c.search,needle); if(score!=Integer.MAX_VALUE) scores.put(c,score); }
        List<Choice> result=new ArrayList<>(scores.keySet());
        result.sort(Comparator.comparingInt((Choice c)->scores.get(c)).thenComparing(c->c.name).thenComparingInt(c->c.id));
        return new ArrayList<>(result.subList(0,Math.min(80,result.size())));
    }
}
