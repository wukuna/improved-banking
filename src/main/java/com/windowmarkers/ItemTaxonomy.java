package com.windowmarkers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Factual identities, not an ordering of interchangeable gear. Sources accompany bundled data. */
final class ItemTaxonomy
{
    private static final Map<String,String> WEAPONS=new HashMap<>();
    private static final Map<String,String> SETS=new HashMap<>();
    static
    {
        for (String[] row : readRows("weapon-roles.tsv",2)) { WEAPONS.put(row[0],row[1]); }
        set("Ahrim — magic set (staff required)","ahrim's hood","ahrim's robetop","ahrim's robeskirt","ahrim's staff");
        set("Dharok — melee set (greataxe required)","dharok's helm","dharok's platebody","dharok's platelegs","dharok's greataxe");
        set("Guthan — melee set (warspear required)","guthan's helm","guthan's platebody","guthan's chainskirt","guthan's warspear");
        set("Karil — ranged set (crossbow required)","karil's coif","karil's leathertop","karil's leatherskirt","karil's crossbow");
        set("Torag — melee set (hammers required)","torag's helm","torag's platebody","torag's platelegs","torag's hammers");
        set("Verac — melee set (flail required)","verac's helm","verac's brassard","verac's plateskirt","verac's flail");
        set("Blood Moon — set + dual macuahuitl","blood moon helm","blood moon chestplate","blood moon tassets","dual macuahuitl");
        set("Blue Moon — magic/melee set + spear","blue moon helm","blue moon chestplate","blue moon tassets","blue moon spear");
        set("Eclipse Moon — ranged set + atlatl","eclipse moon helm","eclipse moon chestplate","eclipse moon tassets","eclipse atlatl");
        set("Void — helm, top, robe and gloves","void melee helm","void ranger helm","void mage helm","void knight top","void knight robe","void knight gloves","elite void top","elite void robe");
        set("Inquisitor — crush armour and mace synergy","inquisitor's great helm","inquisitor's hauberk","inquisitor's plateskirt","inquisitor's mace");
        set("Crystal — bow armour synergy","crystal helm","crystal body","crystal legs","crystal bow","bow of faerdhinen");
        set("Justiciar — armour set (no required weapon)","justiciar faceguard","justiciar chestguard","justiciar legguards");
        set("Obsidian — melee armour/weapon synergy","obsidian helmet","obsidian platebody","obsidian platelegs","toktz-xil-ak","toktz-xil-ek","tzhaar-ket-om","tzhaar-ket-em","berserker necklace");
    }
    private static void set(String label,String... names) { for (String n : names) { SETS.put(n,label); } }
    static List<String[]> readRows(String file,int columns)
    {
        List<String[]> result=new ArrayList<>();
        InputStream stream=ItemTaxonomy.class.getResourceAsStream(file);
        if (stream==null) { throw new IllegalStateException("Missing taxonomy resource: "+file); }
        try (BufferedReader reader=new BufferedReader(new InputStreamReader(stream,StandardCharsets.UTF_8)))
        {
            String line;
            while ((line=reader.readLine())!=null)
            {
                if (line.isEmpty() || line.startsWith("#")) { continue; }
                String[] row=line.split("\t",-1);
                if (row.length!=columns) { throw new IllegalStateException("Invalid taxonomy row in "+file); }
                result.add(row);
            }
        }
        catch (IOException e) { throw new IllegalStateException("Cannot read taxonomy: "+file,e); }
        return result;
    }
    static String baseName(String name)
    {
        String n=name.toLowerCase(Locale.ROOT).replace('\u2019','\'').trim();
        // Keep functional (i), (f), poisoned and (a) variants distinct.
        n=n.replaceFirst("\\s*\\([1-4]\\)$","");
        n=n.replaceFirst("\\s+(100|75|50|25|0)$","");
        n=n.replaceFirst("\\s*\\((?:uncharged|empty|inactive|broken|or|l)\\)$","");
        if (n.startsWith("holy sanguinesti staff") || n.startsWith("holy scythe of vitur") || n.startsWith("holy ghrazi rapier")) { n=n.substring(5); }
        if (n.startsWith("sanguine scythe of vitur")) { n=n.substring(9); }
        if (n.startsWith("twisted ancestral ")) { n=n.substring(8); }
        if (n.startsWith("bow of faerdhinen") || n.startsWith("blade of saeldor"))
        { n=n.replaceFirst("\\s*\\(c\\)(?:\\s*\\([a-z]+\\))?$",""); }
        return n.trim();
    }
    static boolean needsRepair(String name) { return name.endsWith(" 0") || name.contains("(broken)"); }
    static boolean needsCharge(String name) { return name.contains("(uncharged)") || name.contains("(empty)") || name.contains("(inactive)"); }
    static String equipmentSet(String n) { return SETS.get(baseName(n)); }
    static String weaponRole(String n)
    {
        String role=WEAPONS.get(n);
        return role!=null ? role : WEAPONS.get(baseName(n));
    }
    static boolean matchesAny(String name,String... expected)
    { for (String n : expected) { if (matches(name,n)) { return true; } } return false; }
    static boolean matches(String name,String expected)
    {
        String n=baseName(name), e=baseName(expected);
        if (n.equals(e)) { return true; }
        switch (e)
        {
            case "god capes": case "imbued god cape": return n.matches("imbued (saradomin|guthix|zamorak) cape");
            case "blessed body": return n.matches("(saradomin|guthix|zamorak|armadyl|bandos|ancient) d'hide body");
            case "blessed chaps": return n.matches("(saradomin|guthix|zamorak|armadyl|bandos|ancient) chaps");
            case "god blessing": return matchesAny(n,"holy blessing","unholy blessing","peaceful blessing","honourable blessing","war blessing","ancient blessing");
            case "ardougne cloak": return n.matches("ardougne cloak [1-4]");
            case "ancient sceptre": return n.matches("(?:blood|ice|smoke|shadow) ancient sceptre");
            case "mystic hat": case "mystic robe top": case "mystic robe bottom":
                return n.matches(java.util.regex.Pattern.quote(e)+" \\((?:light|dark|dusk)\\)");
            default: return false;
        }
    }
}
