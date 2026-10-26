package com.windowmarkers;

import java.util.*;

/** Optional guide-tier qualifiers; never rewrites the player's native input. */
final class SearchQuery
{
    final String text;
    final Set<Integer> tiers;
    private SearchQuery(String text,Set<Integer> tiers) { this.text=text;this.tiers=Collections.unmodifiableSet(tiers); }
    static SearchQuery parse(String raw) {
        List<String> words=new ArrayList<>(); Set<Integer> tiers=new HashSet<>();
        for(String word:SearchText.normalize(raw).split(" +")) {
            switch(word) {
                case "1": case "1st": case "first": case "bis": tiers.add(1);break;
                case "2": case "2nd": case "second": tiers.add(2);break;
                case "3": case "3rd": case "third": tiers.add(3);break;
                default:words.add(word);
            }
        }
        return new SearchQuery(String.join(" ",words),tiers);
    }
    boolean accepts(ItemCatalogueIndex.Rank rank) { return tiers.isEmpty() || rank!=null && tiers.contains(rank.tier); }
}
