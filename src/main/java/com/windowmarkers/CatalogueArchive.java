package com.windowmarkers;

import java.util.*;

/** Presentation-only archive: exact searches, compatibility rules and saved groups remain valid. */
final class CatalogueArchive
{
    private static final Set<String> HIDDEN=new HashSet<>();
    static { for(String[] row:ItemTaxonomy.readRows("catalogue-archive.tsv",2)) HIDDEN.add(row[0]); }
    static boolean hidden(String category) {
        if(HIDDEN.contains(category)) return true;
        int suffix=category.indexOf(" — ");
        while(suffix>=0) {
            if(HIDDEN.contains(category.substring(0,suffix))) return true;
            suffix=category.indexOf(" — ",suffix+3);
        }
        return false;
    }
    private CatalogueArchive() { }
}
