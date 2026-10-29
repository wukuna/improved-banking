package com.windowmarkers;

import java.util.*;

/** Presentation-only curation. Original setup IDs, classifications and saved memberships remain intact. */
final class ActivityMenu
{
    private static final Map<String,String> NAMES=new LinkedHashMap<>();
    static {
        NAMES.put("Abyss","Abyss runecrafting");
        NAMES.put("Chambers of Xeric/Challenge Mode","Chambers of Xeric — Challenge Mode");
        NAMES.put("Chaos Temple (church)","Chaos Altar prayer training");
        NAMES.put("Chest (Rogues' Castle)","Rogues' Castle chest thieving");
        NAMES.put("Ecumenical key","Ecumenical key farming");
        NAMES.put("Gout tuber","Gout tuber gathering");
        NAMES.put("Krystilia","Wilderness Slayer");
        NAMES.put("Raw anglerfish","Anglerfish fishing");
        NAMES.put("Shades of Mort'ton (minigame)","Shades of Mort'ton");
        NAMES.put("Slayer task/Dark beast","Dark beasts — Slayer");
        NAMES.put("Slayer task/Drakes","Drakes — Slayer");
        NAMES.put("Sulliuscep","Sulliuscep woodcutting");
        NAMES.put("TzHaar Fight Cave","Fight Caves");
    }
    static String canonical(String activity) {
        if("Barbarian Assault/Advanced strategies".equals(activity)) return "Barbarian Assault";
        if("Slayer task/Fossil Island wyverns".equals(activity)) return "Fossil Island Wyverns";
        return activity;
    }
    static String name(String activity) { return NAMES.getOrDefault(activity,activity); }
    static String query(String text) {
        String normalized=SearchText.normalize(text);
        for(Map.Entry<String,String> entry:NAMES.entrySet())
            if(SearchText.normalize(entry.getValue()).equals(normalized)) return entry.getKey();
        return text;
    }
    static List<ActivitySetups.Setup> setups(String activity) {
        List<ActivitySetups.Setup> result=new ArrayList<>();
        if(activity!=null) for(ActivitySetups.Setup setup:ActivitySetups.all())
            if(canonical(setup.activity).equals(canonical(activity))) result.add(setup);
        return result;
    }
    static List<BankCatalogue.Choice> choices(List<BankCatalogue.Choice> all) {
        List<BankCatalogue.Choice> result=new ArrayList<>();
        Set<String> seen=new HashSet<>();
        for(BankCatalogue.Choice choice:all) {
            if(choice.kind==BankCatalogue.Kind.ACTIVITY) {
                String activity=canonical((String)choice.value);
                if(!activity.equals("Hespori (Echo)") && seen.add(activity))
                    result.add(new BankCatalogue.Choice(BankCatalogue.Kind.ACTIVITY,name(activity),activity));
            } else if(choice.kind==BankCatalogue.Kind.CUSTOM_PRESET || choice.kind==BankCatalogue.Kind.SAVED_GROUP) result.add(choice);
        }
        return result;
    }
    static List<BankCatalogue.Choice> activities() {
        List<BankCatalogue.Choice> raw=new ArrayList<>();
        for(String activity:ActivitySetups.activities("")) raw.add(new BankCatalogue.Choice(BankCatalogue.Kind.ACTIVITY,activity,activity));
        return choices(raw);
    }
}
