package com.windowmarkers;

import java.util.*;

/** Guide annotations explain a choice; only actual prerequisites restrict it. */
final class SetupEquipmentChoice
{
    private SetupEquipmentChoice() { }

    static boolean allowed(ItemCatalogueIndex.Item item,String note,ItemCatalogueIndex.Item weapon,
        Collection<ItemCatalogueIndex.Item> owned)
    {
        String text=note.toLowerCase(Locale.ROOT),name=ItemVariantState.familyName(item.fact);
        String main=weapon==null?"":ItemVariantState.familyName(weapon.fact);
        if(item.slot==13) {
            boolean blessing=name.contains("blessing");
            if(Arrays.asList("toxic blowpipe","blazing blowpipe","bow of faerdhinen","crystal bow","craw's bow","webweaver bow").contains(main))
                return blessing;
            SupplySearch.Query ammo=weapon==null?null:SupplySearch.parse("ammo "+main);
            if(ammo!=null) return blessing?ammo.ids.isEmpty():ammo.ids.contains(item.id);
            // Never infer ammunition compatibility from its guide column alone.
            if(!blessing) return false;
        }
        // An attack-style instruction, risk warning, or explanation of a bonus
        // does not make a usable piece disappear from its equipment slot.
        if(text.isEmpty() || text.startsWith("set to ") || text.startsWith("use stab attack style")
            || text.startsWith("on crush") || text.startsWith("on stab") || text.startsWith("on slash")) return true;
        // Owning Rada's blessing 4 already establishes its diary unlock. This
        // does not waive skill/quest conditions on unrelated guide choices.
        if(item.id==22947 && text.contains("diary")) return true;
        if(text.startsWith("only for phase ")) return true; // already scoped to this source's phase
        if(text.contains("slayer task") || text.contains("slayer assignment") || text.contains("task only") || text.startsWith("only on ")
            || text.contains("after completing") || text.contains("elite kourend") || text.contains("diary")) return false;
        if(text.contains("blood moon") && (text.contains("full") || text.contains("set"))) {
            if(text.contains("without full")) return true; // explicitly ranked standalone weapon
            return (main.equals("dual macuahuitl") || name.equals("dual macuahuitl"))
                && owns(owned,"blood moon helm") && owns(owned,"blood moon chestplate") && owns(owned,"blood moon tassets");
        }
        if(text.contains("void") && (text.contains("full") || text.startsWith("with ") || text.contains("only")))
            return !(text.contains("do not use with emberlight") && (main.equals("emberlight") || main.equals("arclight") || main.startsWith("scythe of vitur")))
                && ownsPrefix(owned,"void ",0) && (owns(owned,"elite void top") || owns(owned,"void knight top"))
                && (owns(owned,"elite void robe") || owns(owned,"void knight robe")) && owns(owned,"void knight gloves");
        if(text.contains("when using scythe") || text.startsWith("with scythe")) return main.startsWith("scythe of vitur");
        if(text.contains("if using a crossbow") || text.contains("if bringing a crossbow")) return main.contains("crossbow");
        if(text.contains("if using a twisted bow") || text.equals("with a twisted bow")) return main.equals("twisted bow");
        if(text.contains("if using a 1-handed weapon") || text.contains("with 1 handed staffs"))
            return true; // the widget layer removes the off-hand for an actual two-handed weapon
        if(text.contains("bow of faerdhinen") && (text.startsWith("with ") || text.startsWith("only ") || text.startsWith("if ")))
            return main.equals("bow of faerdhinen") && (!text.contains("full crystal")
                || owns(owned,"crystal helm") && owns(owned,"crystal body") && owns(owned,"crystal legs"));
        // Unknown conditional rankings stay in alternatives with their guide note.
        return !(text.startsWith("if ") || text.startsWith("when ") || text.startsWith("only ")
            || text.startsWith("with ") || text.startsWith("without ") || text.startsWith("requires ")
            || text.startsWith("best used with ") || text.startsWith("use only "));
    }

    private static boolean owns(Collection<ItemCatalogueIndex.Item> owned,String family) {
        return owned.stream().anyMatch(i->!ItemVariantState.needsActivityReview(i.fact)
            && family.equals(ItemVariantState.familyName(i.fact)));
    }
    private static boolean ownsPrefix(Collection<ItemCatalogueIndex.Item> owned,String prefix,int slot) {
        return owned.stream().anyMatch(i->i.slot==slot && !ItemVariantState.needsActivityReview(i.fact)
            && ItemVariantState.familyName(i.fact).startsWith(prefix));
    }

    static List<Integer> inventoryAlternatives(List<Integer> exact)
    {
        // This is specifically a home teleport, not a combat-cape equivalence,
        // redirected tablet, Wilderness escape, or a promise of POH facilities.
        List<Integer> result=new ArrayList<>(exact);
        if(exact.contains(8013))
            for(int id:new int[]{9789,9790,13280,13342}) if(!result.contains(id)) result.add(id);
        // Unlimited charges preserve the same destinations. Do not reverse this:
        // a finite ring need not cover the trip assumed by an eternal-ring guide.
        if(exact.stream().anyMatch(id->id>=11866 && id<=11873) && !result.contains(21268)) result.add(21268);
        return result;
    }

    static String inventoryAlternativeNote(List<Integer> exact) {
        return exact.contains(8013)
            ? "Owned home-teleport alternative. Construction and ordinary Max capes retain the home teleport; combat Max capes do not. Check your house settings and facilities."
            : "Owned eternal Slayer ring replaces the finite ring's teleports. Destination unlocks still apply; unlimited charges do not unlock quest-gated destinations.";
    }
}
