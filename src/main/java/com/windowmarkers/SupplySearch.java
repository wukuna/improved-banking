package com.windowmarkers;

import java.util.*;

/** Source-backed supply filters. Names alone never establish healing or ammunition compatibility. */
final class SupplySearch
{
    private static final Map<Integer,Food> FOODS=new HashMap<>();
    private static final Map<String,Set<Integer>> GROUPS=new LinkedHashMap<>();
    private static final Map<String,String> ALIASES=new HashMap<>();
    static {
        for(String[] row:ItemTaxonomy.readRows("food-search-facts.tsv",12)) FOODS.put(Integer.parseInt(row[0]),new Food(row));
        for(String[] row:ItemTaxonomy.readRows("supply-search-groups.tsv",3))
            GROUPS.computeIfAbsent(SearchText.normalize(row[2]),k->new HashSet<>()).add(Integer.parseInt(row[0].substring(2)));
        alias("blowpipe","toxic blowpipe","blazing blowpipe","bp");
        alias("twisted bow","tbow");alias("rune crossbow","rcb");alias("armadyl crossbow","acb");
        alias("zaryte crossbow","zcb");alias("dragon hunter crossbow","dhcb");
        alias("karil crossbow","karils crossbow","karils");
        alias("hunters crossbow","hunter crossbow");alias("hunters sunlight crossbow","sunlight crossbow","hunter sunlight crossbow");
        alias("comp ogre bow","composite ogre bow");alias("cannon","dwarf multicannon","multicannon");
        alias("eclipse atlatl","atlatl");alias("ballista","heavy ballista","light ballista");
        for(String bow:Arrays.asList("oak","willow","maple","yew","magic")) alias(bow+" bow",bow+" shortbow",bow+" longbow");
        alias("magic bow","magic shortbow i","magic shortbow","magic longbow");
        alias("prayer restore","prayer restoration","restore prayer");alias("stat restore","stat restoration","restore stats");
        alias("run energy","energy","run restoration");alias("aggression","aggro","goading");
        alias("poison cure","antipoison");alias("venom cure","antivenom","anti venom");
        alias("antifire","dragonfire","dragonfire protection");alias("special attack","spec","special restore");
    }
    private static void alias(String canonical,String... aliases) { for(String alias:aliases) ALIASES.put(alias,canonical); }
    static boolean foodQuery(String raw) {
        String text=" "+SearchText.normalize(raw)+" ";
        return text.contains(" food ") || text.contains(" foods ");
    }
    static final class Query {
        final Set<String> flags=new HashSet<>();
        final Set<Integer> ids=new HashSet<>();
        boolean food,raw; int minimum=-1;
        String explanation="";
    }
    static Query parse(String raw) {
        String text=SearchText.normalize(raw);
        if(foodQuery(raw)) {
            Query q=new Query();q.food=true;
            for(String word:text.split(" +")) {
                if(word.matches("[0-9]{1,3}")) {
                    if(q.minimum>=0) return null;
                    q.minimum=Integer.parseInt(word);continue;
                }
                switch(word) {
                    case "food":case "foods":case "cooked":case "ready":case "to":case "eat":case "eating":case "healing":case "hp":case "bite":case "bites":case "only":case "cooking":case "inputs":case "area":break;
                    case "raw":case "uncooked":q.raw=true;break;
                    case "fast":case "combo":case "instant":case "delayed":case "multi":case "unrestricted":q.flags.add(word);break;
                    case "overheal":case "overhealing":q.flags.add("overheal");break;
                    case "wilderness":case "blighted":case "restricted":q.flags.add("restricted");break;
                    default:return null;
                }
            }
            if(q.raw) q.ids.addAll(GROUPS.getOrDefault("raw food cooking inputs",Collections.emptySet()));
            return q;
        }
        String kind=text.matches(".*\\b(?:potion|potions)\\b.*")?"potions":text.matches(".*\\b(?:ammo|ammunition)\\b.*")?"ammo":"";
        if(kind.isEmpty()) return null;
        String terms=text.replaceAll("\\b(?:potion|potions|ammo|ammunition|for)\\b"," ").replaceAll(" +"," ").trim();
        terms=ALIASES.getOrDefault(terms,terms);
        if(kind.equals("potions") && Arrays.asList("attack","strength","defence","defense","magic","ranged","agility","fishing","hunter").contains(terms))
            terms=(terms.equals("defense")?"defence":terms)+" boost";
        Query q=new Query();String key=kind+" "+terms;
        // A weapon that uses charges must not fuzzily acquire ammo from a similarly named bow.
        if(kind.equals("ammo") && Arrays.asList("bowfa","bow of faerdhinen","crystal bow","craws bow","webweaver bow").contains(terms)) {
            q.explanation="This weapon uses charges, not interchangeable ammunition. Check its charging requirements.";return q;
        }
        if(kind.equals("potions") && terms.equals("prayer")) {
            q.ids.addAll(GROUPS.getOrDefault("potions prayer restore",Collections.emptySet()));
            q.ids.addAll(GROUPS.getOrDefault("potions prayer regeneration",Collections.emptySet()));
        } else if(GROUPS.containsKey(key)) q.ids.addAll(GROUPS.get(key));
        else return null;
        q.explanation=kind.equals("ammo")?"Compatible ammunition for "+terms+". Weapon, Slayer and area requirements still apply.":"Potion effect: "+terms+". Strength, duration and side effects vary; this is not an activity recommendation.";
        return q;
    }
    static ItemCatalogueIndex.Result find(Query query,Collection<ItemCatalogueIndex.Item> items,int hitpoints) {
        List<ItemCatalogueIndex.Item> selected=new ArrayList<>();Map<Integer,ItemCatalogueIndex.Rank> notes=new HashMap<>();
        int hp=Math.max(1,Math.min(99,hitpoints));
        for(ItemCatalogueIndex.Item item:items) {
            String note=query.explanation;
            if(query.food && !query.raw) {
                Food food=FOODS.get(item.id);
                if(food==null || !food.name.equalsIgnoreCase(item.name) || !food.accepts(query,hp)) continue;
                note=food.explanation(hp);
            } else {
                if(!query.ids.contains(item.id)) continue;
                if(item.name.toLowerCase(Locale.ROOT).startsWith("blighted ")) note+=" Blighted: area restrictions apply.";
                if(query.raw) {
                    // Uncooked ingredients have no ready-to-eat healing value.
                    if(query.minimum>=0 || !query.flags.isEmpty()) continue;
                    note="Raw cooking input; prepare it before eating.";
                }
            }
            selected.add(item);notes.put(item.id,new ItemCatalogueIndex.Rank(0,note));
        }
        selected.sort(Comparator.comparing((ItemCatalogueIndex.Item item)->item.name,String.CASE_INSENSITIVE_ORDER).thenComparingInt(item->item.id));
        return new ItemCatalogueIndex.Result(selected,notes,false);
    }
    private static final class Food {
        final String name,formula;final int instant,delayed;final boolean multi,fast,combo,overheal,restricted;
        Food(String[] row) {
            name=row[1];instant=Integer.parseInt(row[2]);delayed=Integer.parseInt(row[3]);formula=row[4];
            multi=row[5].equals("1");fast=row[6].equals("1");combo=row[7].equals("1");overheal=row[8].equals("1");restricted=row[9].equals("1");
        }
        int immediate(int hp) {
            switch(formula) {
                case "anglerfish":return hp/10+2*(hp/25)+5*(hp/93)+2;
                case "strawberry":return 1+hp*6/100;
                case "watermelon":return 1+hp/20;
                case "sweetcorn":return 1+hp/10;
                default:return instant;
            }
        }
        boolean accepts(Query q,int hp) {
            if(q.flags.contains("fast")&&!fast || q.flags.contains("combo")&&!combo || q.flags.contains("multi")&&!multi
                || q.flags.contains("overheal")&&!overheal || q.flags.contains("delayed")&&delayed==0
                || q.flags.contains("instant")&&delayed>0 || q.flags.contains("restricted")&&!restricted
                || q.flags.contains("unrestricted")&&restricted) return false;
            if(q.minimum<0)return true;
            if(formula.equals("unknown") || formula.equals("random")) return false;
            return immediate(hp)+delayed>=q.minimum;
        }
        String explanation(int hp) {
            String heal=formula.equals("unknown")?"Healing varies; check the item's requirements":formula.equals("random")?"Unreliable healing; excluded from healing thresholds":
                immediate(hp)+" HP immediately"+(delayed>0?" + "+delayed+" delayed HP (pending hunter-food healing can be replaced)":"")+" per bite";
            if(!Arrays.asList("fixed","range","random","unknown").contains(formula))heal+=" at your base "+hp+" Hitpoints";
            if(formula.equals("range"))heal+=" (minimum; the actual amount varies)";
            return heal+"."+(multi?" Multiple bites per item; thresholds use one bite.":"")+(combo?" Combo-eating food.":"")
                +(fast?" Non-standard attack/eating delay.":"")+(overheal?" Can overheal; PvP restrictions apply.":"")
                +(restricted?" Blighted: Wilderness/PvP-world restrictions apply.":"");
        }
    }
}
