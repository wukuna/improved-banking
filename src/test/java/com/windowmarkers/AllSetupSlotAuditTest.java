package com.windowmarkers;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Exercise every bundled equipment table with its reviewed, ready identities available. */
public class AllSetupSlotAuditTest
{
    @Test public void everyUnconditionalEquipmentSlotSurvivesSelectionAndBrokenStatesStayOut() {
        Map<Integer,ItemCatalogueIndex.Item> known=new HashMap<>();
        for(String[] row:ItemTaxonomy.readRows("ge-item-slots.tsv",3)) {
            int id=Integer.parseInt(row[0]),slot=Integer.parseInt(row[1]);
            known.put(id,GeSearchIndexTest.item(id,row[2],slot));
        }
        Map<String,List<String[]>> options=new LinkedHashMap<>();
        for(String[] row:ItemTaxonomy.readRows("setup-layout-options.tsv",5))
            options.computeIfAbsent(row[0],k -> new ArrayList<>()).add(row);
        int tables=0,checkedSlots=0,readyStates=0;List<String> failures=new ArrayList<>();
        for(var entry:options.entrySet()) {
            ActivitySetups.Setup setup=ActivitySetups.byId(entry.getKey());
            Map<Integer,ItemCatalogueIndex.Item> bank=new HashMap<>();
            for(int id:setup.itemIds) if(known.containsKey(id)) bank.put(id,known.get(id));
            SetupLoadout plan=SetupLoadout.build(Collections.singletonList(setup),bank,bank.keySet(),"");
            if(plan==null || plan.equipmentName.isEmpty()) continue;
            tables++;
            ItemCatalogueIndex.Item weapon=plan.equipment.containsKey(3)
                ?bank.get(plan.equipment.get(3).get(0)):null;
            Set<Integer> expected=new HashSet<>();
            for(String[] row:entry.getValue()) {
                int slot=ItemCatalogueIndex.slot(row[1].replaceAll("[0-9]+$",""));
                if(slot<0 || !row[4].isEmpty()) continue;
                ItemCatalogueIndex.Item item=bank.get(Integer.parseInt(row[2]));
                if(item==null || item.slot!=slot || ItemVariantState.needsActivityReview(item.fact)
                    || ItemVariantState.needsRankReview(item.fact,setup.activity,setup.label)
                    || !SetupItemRequirements.reviewReason(setup.id,item.fact).isEmpty()) continue;
                if(slot==13 && !SetupEquipmentChoice.allowed(item,row[4],weapon,bank.values())) continue;
                expected.add(slot);readyStates++;
            }
            for(int slot:expected) { checkedSlots++;if(!plan.equipment.containsKey(slot)) failures.add(setup.id+" "+setup.label+" slot "+slot); }
            for(List<Integer> ids:plan.equipment.values()) for(int id:ids)
                assertFalse(setup.id+" selected unusable "+id,ItemVariantState.needsActivityReview(bank.get(id).fact));
        }
        System.out.println("Equipment audit: "+tables+" tables; "+checkedSlots+" unconditional slots; "+readyStates+" ready source choices.");
        assertTrue("Missing unconditional slots: "+failures,failures.isEmpty());
        assertTrue("The full equipment catalogue must be exercised",tables>300);
    }
}
