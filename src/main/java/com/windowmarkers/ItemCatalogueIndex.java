package com.windowmarkers;

import java.util.*;
import java.util.stream.Collectors;

/** Shared immutable categories and guide rankings. Callers supply GE-tradeable or owned identities. */
final class ItemCatalogueIndex
{
    static final int LIMIT=250;
    static final class Item
    {
        final int id, slot;
        final String name, search;
        final AutoCategories.Item fact;
        Item(int id,String name,int slot,String[] actions)
        { this(new AutoCategories.Item(id,name,slot>=0,actions,0,0,0,slot),name,slot); }
        Item(AutoCategories.Item fact,String name,int slot)
        { id=fact.id; this.name=name; search=SearchText.normalize(name); this.slot=slot; this.fact=fact; }
    }
    static final class Rank
    {
        final int tier;
        final String explanation,role;
        Rank(int tier,String explanation) { this(tier,explanation,""); }
        Rank(int tier,String explanation,String role) { this.tier=tier; this.explanation=explanation; this.role=role; }
    }
    static final class Result
    {
        final List<Item> items;
        final Map<Integer,Rank> ranks;
        final boolean activity;
        final SetupLoadout loadout;
        final List<ActivitySetups.Setup> setups;
        Result(List<Item> items,Map<Integer,Rank> ranks,boolean activity) { this(items,ranks,activity,null); }
        Result(List<Item> items,Map<Integer,Rank> ranks,boolean activity,SetupLoadout loadout) { this(items,ranks,activity,loadout,Collections.emptyList()); }
        Result(List<Item> items,Map<Integer,Rank> ranks,boolean activity,SetupLoadout loadout,List<ActivitySetups.Setup> setups)
        { this.items=items; this.ranks=ranks; this.activity=activity; this.loadout=loadout; this.setups=Collections.unmodifiableList(new ArrayList<>(setups)); }
        short[] ids() { short[] ids=new short[items.size()]; for(int i=0;i<ids.length;i++) ids[i]=(short)items.get(i).id; return ids; }
    }
    private static final class Field
    {
        final String slot, note;
        final int tier;
        Field(String[] row) { slot=row[2]; tier=Integer.parseInt(row[3]); note=row[4]; }
    }
    private static final Map<String,Map<Integer,List<Field>>> RANKS=new HashMap<>();
    private static final Map<Integer,String[]> SLOTS=new HashMap<>();
    private static final List<String[]> PURCHASE_VARIANTS=ItemTaxonomy.readRows("ge-purchase-variants.tsv",5);
    private static final Map<Integer,String[]> FUNCTIONAL_SLOTS=new HashMap<>();
    private static final Map<Integer,String[]> VARIANT_NOTES=new HashMap<>();
    static
    {
        for(String[] row:ItemTaxonomy.readRows("activity-setup-ranks.tsv",5))
            RANKS.computeIfAbsent(row[0],k->new HashMap<>()).computeIfAbsent(Integer.parseInt(row[1]),k->new ArrayList<>()).add(new Field(row));
        for(String[] row:ItemTaxonomy.readRows("ge-item-slots.tsv",3)) SLOTS.put(Integer.parseInt(row[0]),row);
        for(String[] row:ItemTaxonomy.readRows("ge-functional-slots.tsv",5)) FUNCTIONAL_SLOTS.put(Integer.parseInt(row[0]),row);
        for(String[] row:ItemTaxonomy.readRows("catalogue-variant-notes.tsv",4)) VARIANT_NOTES.put(Integer.parseInt(row[0]),row);
    }
    static int fallbackSlot(int id,String name)
    { String[] row=SLOTS.get(id); return row!=null && row[2].equalsIgnoreCase(name) ? Integer.parseInt(row[1]) : -1; }

    private final Map<Integer,Item> items=new HashMap<>();
    private final Map<String,Set<Integer>> categories=new LinkedHashMap<>();
    private final Map<AutoCategories.Preset,Set<Integer>> presets=new EnumMap<>(AutoCategories.Preset.class);
    private final Map<String,Map<String,Set<Integer>>> activityCategories=new LinkedHashMap<>();
    private final List<ActivitySetups.Setup> setups=ActivitySetups.all();
    private final Map<String,String> setupText=new HashMap<>();
    private final Map<String,Set<Integer>> setupItems=new HashMap<>();
    private final Map<String,Map<Integer,String>> purchaseNotes=new HashMap<>();
    private final Set<String> exactNames=new HashSet<>();
    private final Set<String> vocabulary=new HashSet<>();
    private final boolean ge;

    ItemCatalogueIndex(List<Item> catalogue)
    { this(catalogue,true); }
    ItemCatalogueIndex(List<Item> catalogue,boolean ge)
    {
        this.ge=ge;
        Map<Integer,List<String>> itemGroups=new HashMap<>();
        for(Item item:catalogue)
        {
            if(item.id<0 || item.id>65535) continue;
            items.put(item.id,item); exactNames.add(normalize(item.name));
            if(!ItemVariantState.identityNeedsReview(item.fact))
                for(WikiCatalogue.Entry entry:WikiCatalogue.entries(item.fact)) {
                    add(entry.group,item.id);
                    itemGroups.computeIfAbsent(item.id,k->new ArrayList<>()).add(normalize(entry.group));
                    // Index the factual facet, not a mixed preset title. A
                    // food query must not include tools merely because they
                    // boost a skill and a preset also includes boost items.
                    add(entry.facet,item.id);
                    if(entry.facet.equals("outfit")) { add("skilling outfits",item.id); }
                    if(entry.facet.equals("combat")) add("general combat",item.id);
                    if(entry.facet.equals("skilling") && entry.group.contains(" — ")
                        && "Tools".equals(ItemTaxonomy.weaponRole(ItemVariantState.familyName(item.fact))))
                        add(entry.group.substring(0,entry.group.indexOf(" — "))+" tools",item.id);
                }
        }
        // Preset subcategories are the same labels exposed in the bank legend.
        // Index each factual group, not mixed preset titles that would turn a
        // food query into every potion or resource in a broad preset.
        List<AutoCategories.Item> facts=new ArrayList<>();
        items.values().forEach(item->facts.add(item.fact));
        for(AutoCategories.Preset preset:AutoCategories.Preset.values()) {
            Set<Integer> membership=new HashSet<>();presets.put(preset,membership);
            AutoCategories.suggest(preset,facts).forEach((group,members)-> {
                for(AutoCategories.Item item:members) { add(group,item.id); if(!group.startsWith("Other")) membership.add(item.id); }
            });
        }
        Map<String,Set<Integer>> byActivity=new HashMap<>();
        for(ActivitySetups.Setup setup:setups) {
            setupText.put(setup.id,normalize(setup.activity+" "+setup.label));
            Set<Integer> ids=new HashSet<>(setup.itemIds);
            for(String[] rule:PURCHASE_VARIANTS) {
                int reference=Integer.parseInt(rule[0]),target=Integer.parseInt(rule[1]);
                Item item=items.get(target);
                if(setup.itemIds.contains(reference) && item!=null && item.name.equalsIgnoreCase(rule[2]) && ids.add(target))
                    purchaseNotes.computeIfAbsent(setup.id,k->new HashMap<>()).put(target,rule[3]);
            }
            setupItems.put(setup.id,ids);
            byActivity.computeIfAbsent(normalize(setup.activity),k->new HashSet<>()).addAll(ids);
        }
        byActivity.forEach((activity,ids)-> {
            Map<String,Set<Integer>> groups=new HashMap<>();
            for(int id:ids) for(String group:itemGroups.getOrDefault(id,Collections.emptyList()))
                groups.computeIfAbsent(activity+" "+group,k->new HashSet<>()).add(id);
            activityCategories.put(activity,groups);
        });
        setupText.values().forEach(this::indexWords);
        categories.keySet().forEach(this::indexWords);
        activityCategories.values().forEach(groups->groups.keySet().forEach(this::indexWords));
    }
    private void indexWords(String text) { Collections.addAll(vocabulary,text.split(" +")); }
    private void add(String text,int id)
    { categories.computeIfAbsent(normalize(text),k->new HashSet<>()).add(id); }
    Map<String,Set<Integer>> categoryDocuments() { return Collections.unmodifiableMap(categories); }
    Set<Integer> presetIds(AutoCategories.Preset preset) { return Collections.unmodifiableSet(presets.get(preset)); }
    static String normalize(String query)
    {
        String s=" "+SearchText.normalize(query)+" ";
        String[][] aliases={{"toa","tombs of amascut"},{"cox","chambers of xeric"},{"tob","theatre of blood"},
            {"kbd","king black dragon"},{"kq","kalphite queen"},
            {"dks","dagannoth kings"},{"corp","corporeal beast"},{"mage","magic"},{"range","ranged"},{"armor","armour"}};
        for(String[] alias:aliases) s=s.replace(" "+alias[0]+" "," "+alias[1]+" ");
        return s.trim();
    }
    static int slot(String token)
    {
        switch(token) {
            case "head": case "helm": case "helmet": case "hat": case "hood": case "headgear": case "helms": case "helmets": case "hats": return 0;
            case "cape": case "back": case "capes": return 1;
            case "neck": case "amulet": case "necklace": case "amulets": case "necklaces": case "pendant": return 2;
            case "weapon": case "weapons": return 3;
            case "body": case "chest": case "torso": case "top": case "tops": case "platebody": case "robetop": return 4;
            case "shield": case "shields": case "offhand": case "defender": return 5;
            case "legs": case "leg": case "bottom": case "bottoms": case "platelegs": case "plateskirt": case "chaps": case "tassets": case "skirt": return 7;
            case "hands": case "hand": case "gloves": case "glove": case "gauntlets": case "bracers": return 9;
            case "feet": case "foot": case "boots": case "boot": case "shoes": return 10;
            case "ring": case "rings": return 12;
            case "ammo": case "ammunition": case "arrows": case "bolts": return 13;
            default:return -1;
        }
    }
    private static boolean matches(String text,String query,boolean fuzzy)
    {
        boolean exact=true;
        for(String word:query.split(" +")) if(!text.contains(word)) { exact=false; break; }
        return exact || fuzzy && SearchText.normalizedScore(text,query)!=Integer.MAX_VALUE;
    }
    private static boolean mentionsActivity(String query,String activity,boolean fuzzy)
    {
        for(String word:normalize(activity).split(" +")) {
            if(word.length()<3 || word.equals("the") || word.equals("and")) continue;
            for(String term:query.split(" +"))
                if(term.length()>=3 && matches(word,term,fuzzy)) return true;
        }
        return false;
    }
    private boolean exactCategoryMatch(String terms)
    {
        if(setupText.values().stream().anyMatch(t->matches(t,terms,false))
            || categories.keySet().stream().anyMatch(t->matches(t,terms,false))) return true;
        for(Map.Entry<String,Map<String,Set<Integer>>> activity:activityCategories.entrySet())
            if(mentionsActivity(terms,activity.getKey(),false)
                && activity.getValue().keySet().stream().anyMatch(t->matches(t,terms,false))) return true;
        return false;
    }
    private String correctWholeWords(String terms)
    { return SearchText.correctWholeWords(vocabulary,terms); }

    /** Null means leave native search (and other search plugins) entirely alone. */
    Result find(String raw,String context)
    { return find(raw,context,""); }
    Result find(String raw,String context,String inventoryContext)
    { return find(raw,context,inventoryContext,99); }
    Result find(String raw,String context,String inventoryContext,int hitpoints)
    {
        if(!BankSearchIndex.plainQuery(raw) || raw.length()>100) return null;
        SupplySearch.Query supply=SupplySearch.parse(raw);
        if(supply!=null) return SupplySearch.find(supply,items.values(),hitpoints);
        String query=normalize(raw);
        ActivitySetups.Setup chosen=setups.stream().filter(s->s.id.equals(context)).findFirst().orElse(null);
        // Explicit guide selection disambiguates activities that are also item
        // names (for example Gout tuber or a boss pet). Ordinary item searches
        // still pass through when no matching guide has been selected.
        boolean selectedActivity=chosen!=null && mentionsActivity(query,chosen.activity,false);
        if(query.length()<3 || exactNames.contains(query) && !categories.containsKey(query) && !selectedActivity) return null;
        SearchQuery qualifiers=SearchQuery.parse(raw);
        query=normalize(qualifiers.text);
        int wantedSlot=-1;
        List<String> words=new ArrayList<>();
        for(String word:query.split(" +")) {
            int candidate=slot(word);
            if(candidate>=0) { if(wantedSlot>=0 && wantedSlot!=candidate) return null; wantedSlot=candidate; }
            else words.add(word);
        }
        String terms=String.join(" ",words);
        // Prefer exact words/prefixes globally. A valid "food" query must not
        // acquire Woodcutting gear merely because food is one edit from wood.
        boolean fuzzy=!terms.isEmpty() && !exactCategoryMatch(terms);
        if(fuzzy) {
            // A complete-word correction outranks a loose prefix match:
            // "minign tools" means mining, not tools for a minigame.
            String corrected=correctWholeWords(terms);
            if(!corrected.equals(terms) && exactCategoryMatch(corrected)) { terms=corrected; fuzzy=false; }
        }
        Set<Integer> found=new HashSet<>();
        List<ActivitySetups.Setup> matched=new ArrayList<>();
        boolean useContext=chosen!=null && mentionsActivity(terms,chosen.activity,fuzzy);
        boolean recognized=terms.isEmpty() && wantedSlot>=0;
        if(recognized) found.addAll(items.keySet());
        if(!terms.isEmpty()) {
            for(ActivitySetups.Setup setup:setups) if(matches(setupText.get(setup.id),terms,fuzzy)) {
                recognized=true;
                // A selected guide only narrows that activity; other activities
                // and ordinary categories remain searchable.
                if(useContext && family(chosen.activity).equals(family(setup.activity)) && chosen!=setup) continue;
                if(mentionsActivity(terms,setup.activity,fuzzy)) matched.add(setup);
                found.addAll(setupItems.get(setup.id));
            }
            for(Map.Entry<String,Set<Integer>> category:categories.entrySet()) if(matches(category.getKey(),terms,fuzzy)) {
                recognized=true; found.addAll(category.getValue());
            }
            for(Map.Entry<String,Map<String,Set<Integer>>> activity:activityCategories.entrySet())
                if(mentionsActivity(terms,activity.getKey(),fuzzy))
                    for(Map.Entry<String,Set<Integer>> group:activity.getValue().entrySet()) if(matches(group.getKey(),terms,fuzzy)) {
                        recognized=true; found.addAll(group.getValue());
                    }
        }
        if(!recognized) return null;
        if(useContext) found.retainAll(setupItems.get(chosen.id));
        // Preserve ordinary name matches alongside semantic additions. Purchase
        // variants above are explicit, source-backed links, not family guesses.
        String ordinary=SearchText.normalize(raw);
        for(Item item:items.values()) if(item.search.contains(ordinary)) found.add(item.id);
        final int filterSlot=wantedSlot;
        List<Item> result=found.stream().map(items::get).filter(Objects::nonNull)
            .filter(i->filterSlot<0 || fitsSlot(i,filterSlot) || i.search.contains(ordinary))
            .sorted(Comparator.comparing((Item i)->i.name,String.CASE_INSENSITIVE_ORDER).thenComparingInt(i->i.id)).collect(Collectors.toList());
        Map<Integer,Rank> ranks=new HashMap<>();
        if(!ge || result.size()<=LIMIT || !matched.isEmpty() || !qualifiers.tiers.isEmpty()) for(Item item:result) ranks.put(item.id,rank(item,matched,wantedSlot));
        if(!qualifiers.tiers.isEmpty()) {
            result.removeIf(item->!qualifiers.accepts(ranks.get(item.id)));
            Set<Integer> retained=result.stream().map(item->item.id).collect(Collectors.toSet());
            ranks.keySet().retainAll(retained);
        }
        SetupLoadout loadout=!ge && wantedSlot<0 && qualifiers.tiers.isEmpty()
            ? SetupLoadout.build(matched,items,found,inventoryContext):null;
        if(loadout!=null) {
            Set<Integer> included=result.stream().map(i->i.id).collect(Collectors.toSet());
            for(int id:loadout.inventory.values()) if(included.add(id)) result.add(items.get(id));
        }
        return new Result(result,ranks,!matched.isEmpty(),loadout,matched);
    }
    Map<Integer,Rank> guideRanks(String query,String context)
    { return guideRanks(query,context,99); }
    Map<Integer,Rank> guideRanks(String query,String context,int hitpoints)
    {
        Result result=find(query,context,"",hitpoints);
        return result==null?Collections.emptyMap():result.ranks;
    }
    private static String family(String activity) { return normalize(activity.split("/",2)[0]); }
    private static String[] functionalSlot(Item item)
    { String[] row=FUNCTIONAL_SLOTS.get(item.id); return row!=null && row[1].equalsIgnoreCase(item.name)?row:null; }
    private static boolean fitsSlot(Item item,int slot)
    { String[] row=functionalSlot(item); return item.slot==slot || row!=null && Integer.parseInt(row[2])==slot; }
    private Rank rank(Item item,List<ActivitySetups.Setup> matched,int wantedSlot)
    {
        Set<String> notes=new LinkedHashSet<>();
        String[] variant=VARIANT_NOTES.get(item.id);
        if(variant!=null && variant[1].equalsIgnoreCase(item.name)) notes.add(variant[2]);
        for(ActivitySetups.Setup setup:matched) {
            String note=purchaseNotes.getOrDefault(setup.id,Collections.emptyMap()).get(item.id);
            if(note!=null) notes.add(note);
            String requirement=SetupItemRequirements.reviewReason(setup.id,item.fact);
            if(!requirement.isEmpty()) notes.add(setup.activity+" / "+setup.label+": "+requirement);
        }
        String[] functional=functionalSlot(item);
        if(functional!=null) notes.add(functional[3]);
        String state=ItemVariantState.annotate("Item state",item.fact);
        if(!state.equals("Item state")) notes.add(state);
        String extra=notes.isEmpty()?"":"\n"+String.join("\n",notes);
        if(ItemVariantState.needsActivityReview(item.fact))
            return new Rank(0,ItemVariantState.annotate(ItemVariantState.reviewGroup(item.fact),item.fact)+". Purchase state is not a ready-to-use loadout."+extra);
        Set<Integer> tiers=new HashSet<>(); Set<String> roles=new HashSet<>(); List<String> sources=new ArrayList<>();
        for(ActivitySetups.Setup setup:matched) {
            List<Field> fields=RANKS.getOrDefault(setup.id,Collections.emptyMap()).get(item.id);
            if(fields==null) continue;
            for(Field field:fields) {
                int sourceSlot=field.slot.equals("special") || field.slot.equals("2h") ? 3 : slot(field.slot);
                // Wiki's ammo/spell row can contain ingredients or spell links.
                // Those remain searchable, but are not ranked equipment.
                if(item.slot<0 || !fitsSlot(item,sourceSlot) || wantedSlot>=0 && sourceSlot!=wantedSlot) continue;
                boolean reduced=ItemVariantState.needsRankReview(item.fact,setup.activity,setup.label)
                    || !SetupItemRequirements.reviewReason(setup.id,item.fact).isEmpty();
                int value=reduced ? 0 : field.tier;
                tiers.add(value); roles.add(field.slot.equals("special") ? "special attack"
                    :field.slot.equals("ammo")?"ammunition":field.slot.equals("shield")?"shield":"equipment");
                if(sources.size()<3) sources.add(setup.activity+" / "+setup.label+" / "+field.slot+": "+
                    (reduced ? "reduced function; charge-dependent guide rank withheld" : value==0 ? "provisional or ordered choices; review guide" : "Wiki column "+value)+
                    (field.note.isEmpty()?"":". "+field.note));
            }
        }
        int tier=tiers.size()==1 && roles.size()==1 ? tiers.iterator().next() : 0;
        String reason=tiers.isEmpty() ? "Category or example match; no ranked recommendation for this search." :
            tier==0 ? "Ranking differs by setup/role, or needs review. Refine the search or use Legend / Saved views / Guide choices." :
            "Wiki tier "+tier+" for the matching setup(s), not a universal ranking. Requirements and set bonuses still apply.";
        return new Rank(tier,reason+extra+(sources.isEmpty()?"":"\n"+String.join("\n",sources)),roles.size()==1?roles.iterator().next():"");
    }
}
