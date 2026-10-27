package com.windowmarkers;

import java.util.*;
import javax.swing.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class ItemReviewPanelTest
{
    @Test public void findBySpecificUseAndReadFullReasonWithoutChangingItems() throws Exception
    {
        Map<Integer,ItemCatalogueIndex.Rank> reasons=new LinkedHashMap<>();
        reasons.put(2144,new ItemCatalogueIndex.Rank(0,"Burnt chicken\nRepeatable use: Keldagrim tasks\nQuest completed; repeatable use remains.","cleanup:niche"));
        reasons.put(2146,new ItemCatalogueIndex.Rank(0,"Burnt meat\nWitch's Potion: completed","cleanup:review"));
        SwingUtilities.invokeAndWait(()-> {
            ItemReviewPanel panel=new ItemReviewPanel(Arrays.asList(new BankItemSearch.Choice(2144,"Burnt chicken"),new BankItemSearch.Choice(2146,"Burnt meat")),reasons);
            try {
                assertEquals(2,panel.rows.size());panel.search.setText("keldagrim");
                assertEquals(1,panel.rows.size());assertEquals(2144,panel.list.getSelectedValue().id);
                assertTrue(panel.detail.getText().contains("repeatable use remains"));assertFalse(panel.detail.isEditable());
                panel.search.setText("not present");assertEquals(0,panel.rows.size());
                assertFalse(panel.detail.getText().contains("Burnt chicken"));assertEquals(2,reasons.size());
            } finally { panel.close(); }
        });
    }
}
