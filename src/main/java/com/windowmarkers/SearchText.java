package com.windowmarkers;

import java.text.Normalizer;
import java.util.Locale;

/** Shared, bounded typo matching for names, categories and presets. */
final class SearchText
{
    static String normalize(String text)
    {
        return Normalizer.normalize(text==null ? "" : text,Normalizer.Form.NFKD)
            .replaceAll("\\p{M}","").toLowerCase(Locale.ROOT).replace("'","").replace("’","")
            .replaceAll("[^a-z0-9]+"," ").trim();
    }
    static int score(String text,String query)
    { return normalizedScore(normalize(text),normalize(query)); }
    static int normalizedScore(String text,String query)
    {
        if(query.isEmpty() || text.equals(query)) return 0;
        if(query.length()>256) return Integer.MAX_VALUE;
        if(text.startsWith(query)) return 1;
        if(text.contains(query)) return 2;
        int score=3;
        String[] words=text.split(" ");
        for(String needle:query.split(" "))
        {
            if(text.contains(needle)) continue;
            int allowed=needle.length()<4 ? 0 : needle.length()<8 ? 1 : 2;
            boolean match=false;
            for(String word:words) if(matchesWord(word,needle,allowed))
            { match=true; break; }
            if(!match) return Integer.MAX_VALUE;
            score+=2;
        }
        return score;
    }
    private static boolean matchesWord(String word,String needle,int allowed)
    {
        if(allowed==0) return false;
        // Search as the user types: a typo in an unfinished word should not make
        // its suggestions disappear until the final letters have been entered.
        for(int length=Math.max(1,needle.length()-allowed);length<=Math.min(word.length(),needle.length()+allowed);length++)
            if(distance(word.substring(0,length),needle)<=allowed) return true;
        return false;
    }
    private static int distance(String a,String b)
    {
        int[][] d=new int[a.length()+1][b.length()+1];
        for(int i=0;i<=a.length();i++) d[i][0]=i;
        for(int j=0;j<=b.length();j++) d[0][j]=j;
        for(int i=1;i<=a.length();i++) for(int j=1;j<=b.length();j++)
        {
            d[i][j]=Math.min(d[i-1][j]+1,Math.min(d[i][j-1]+1,d[i-1][j-1]+(a.charAt(i-1)==b.charAt(j-1)?0:1)));
            if(i>1 && j>1 && a.charAt(i-1)==b.charAt(j-2) && a.charAt(i-2)==b.charAt(j-1))
                d[i][j]=Math.min(d[i][j],d[i-2][j-2]+1);
        }
        return d[a.length()][b.length()];
    }
}
