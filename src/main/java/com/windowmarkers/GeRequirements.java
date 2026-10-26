package com.windowmarkers;

import java.util.*;
import net.runelite.api.*;

/** Proven restrictions only. A blank reason is not proof that every quest/unlock is satisfied. */
final class GeRequirements
{
    private static final List<String[]> REQUIREMENTS=ItemTaxonomy.readRows("raid-wear-requirements.tsv",8);
    private static final Skill[] SKILLS={Skill.ATTACK,Skill.STRENGTH,Skill.DEFENCE,Skill.RANGED,Skill.MAGIC,Skill.PRAYER,Skill.HITPOINTS};
    private final int[] levels;
    private final boolean allMaxed;
    private final Boolean ancientRings;
    static void prepare() { /* initialize bundled data on the catalogue worker */ }
    GeRequirements(int[] levels,boolean allMaxed,Boolean ancientRings)
    { this.levels=levels.clone();this.allMaxed=allMaxed;this.ancientRings=ancientRings; }
    static GeRequirements capture(Client client,Boolean ancientRings)
    {
        int[] levels=new int[SKILLS.length];for(int i=0;i<levels.length;i++) levels[i]=client.getRealSkillLevel(SKILLS[i]);
        boolean maxed=Arrays.stream(Skill.values()).filter(s->s!=Skill.OVERALL).allMatch(s->client.getRealSkillLevel(s)>=99);
        return new GeRequirements(levels,maxed,ancientRings);
    }
    boolean reviewed(ItemCatalogueIndex.Item item)
    { return REQUIREMENTS.stream().anyMatch(row->CombatEquivalence.matches(item.fact,row[0])); }
    String locked(ItemCatalogueIndex.Item item)
    {
        List<String> missing=new ArrayList<>();
        if(CombatEquivalence.requiresMaxedSkills(item.fact) && !allMaxed) missing.add("99 in every skill");
        if(CombatEquivalence.matchesAny(item.fact,"ultor ring","magus ring","venator ring","bellator ring") && Boolean.FALSE.equals(ancientRings))
            missing.add("Desert Treasure II completion");
        for(String[] row:REQUIREMENTS) if(CombatEquivalence.matches(item.fact,row[0])) {
            for(int i=0;i<SKILLS.length;i++) if(levels[i]<Integer.parseInt(row[i+1])) missing.add(row[i+1]+" "+SKILLS[i].getName());
            break;
        }
        return String.join(", ",missing);
    }
}
