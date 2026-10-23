package com.windowmarkers;

import java.awt.Color;
import java.util.Arrays;
import javax.swing.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class MarkerLegendTest
{
    @Test public void tabFilteringIncludesOffscreenItemsAndCountsOnlyCurrentMatches()
    {
        Marker magic=new Marker(0,0,40,40); magic.label="Magic"; magic.linkedItemIds=new int[]{1,2,3};
        Marker melee=new Marker(0,0,40,40); melee.linkedItemIds=new int[]{4};
        java.awt.Rectangle bank=new java.awt.Rectangle(0,0,500,400),area=new java.awt.Rectangle(0,0,400,300);
        java.util.List<Marker> markers=Arrays.asList(magic,melee);
        java.util.List<ItemAnchors.Cell> tab=Arrays.asList(new ItemAnchors.Cell(1,new java.awt.Rectangle(0,800,36,32)),new ItemAnchors.Cell(1,new java.awt.Rectangle(40,800,36,32)));
        java.util.List<MarkerLegend.Entry> entries=MarkerLegend.forTab(markers,tab,bank,area,0,Color.BLUE);
        assertEquals(1,entries.size()); assertSame(magic,entries.get(0).marker);
        assertEquals("1 item types in this tab",entries.get(0).detail);
        java.util.List<MarkerLegend.Entry> switched=MarkerLegend.forTab(markers,Arrays.asList(new ItemAnchors.Cell(4,new java.awt.Rectangle(0,0,36,32))),bank,area,0,Color.BLUE);
        assertSame(melee,switched.get(0).marker); assertFalse(MarkerLegend.same(entries,switched));
        assertTrue(MarkerLegend.forTab(markers,tab,null,null,0,Color.BLUE).isEmpty());
        assertTrue(MarkerLegend.forTab(markers,java.util.Collections.emptyList(),bank,area,0,Color.BLUE).isEmpty());
        assertTrue(MarkerLegend.same(entries,MarkerLegend.forTab(markers,tab,bank,area,500,Color.BLUE)));
    }

    @Test public void areaLegendRequiresAnEnclosedItemAndFollowsScroll()
    {
        Marker marker=new Marker(0,800,48,36); marker.scrollWithItems=true;
        java.awt.Rectangle bank=new java.awt.Rectangle(0,0,500,400),area=new java.awt.Rectangle(0,0,400,300);
        assertEquals(1,MarkerLegend.forTab(Arrays.asList(marker),Arrays.asList(new ItemAnchors.Cell(1,new java.awt.Rectangle(6,302,36,32))),bank,area,500,Color.BLUE).size());
        assertTrue(MarkerLegend.forTab(Arrays.asList(marker),Arrays.asList(new ItemAnchors.Cell(1,new java.awt.Rectangle(6,2,36,32))),bank,area,500,Color.BLUE).isEmpty());
    }
    @Test public void pinClearsWhenLayoutEntriesChangeAndCountsDistinctLinks() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            Marker marker=new Marker(0,0,40,40); marker.linkedItemIds=new int[]{1,1,2};
            Marker[] pinned={null};
            MarkerLegend legend=new MarkerLegend(m -> {},m -> pinned[0]=m);
            MarkerLegend.Entry entry=new MarkerLegend.Entry(marker,"Magic",Color.BLUE);
            assertEquals("2 linked item types",entry.detail);
            legend.replace(Arrays.asList(entry));
            JScrollPane scroll=(JScrollPane)legend.getComponent(1);
            JList<?> list=(JList<?>)scroll.getViewport().getView();
            list.setSelectedIndex(0); assertSame(marker,pinned[0]);
            legend.replace(Arrays.asList(new MarkerLegend.Entry(marker,"Magic",Color.BLUE,"1 item types in this tab")));
            assertSame(marker,pinned[0]);
            list.clearSelection(); assertNull(pinned[0]);
            list.setSelectedIndex(0); legend.replace(java.util.Collections.emptyList()); assertNull(pinned[0]);
        });
    }
    @Test public void miniLabelsDoNotRewriteTheFullLegendName()
    {
        Marker marker=new Marker(0,0,40,40); marker.label="Mining — useful equipment";
        assertEquals("Mining",MarkerPresentation.miniTitle(marker.labelText()));
        assertEquals("Mining — useful equipment",marker.labelText());
        assertEquals("Melee",MarkerPresentation.miniTitle("Melee weapons"));
    }

    @Test public void scopePresetAndFuzzyFiltersRetainSelectionAcrossReordering() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            Marker a=new Marker(0,0,48,36), b=new Marker(0,0,48,36);
            a.label="Herblore ingredients"; a.preset="Skilling"; b.label="My food"; b.preset="Custom";
            MarkerLegend.Entry first=new MarkerLegend.Entry(a,a.label,Color.GREEN), second=new MarkerLegend.Entry(b,b.label,Color.BLUE);
            MarkerLegend legend=new MarkerLegend(m -> {},m -> {});
            legend.replace(Arrays.asList(first),Arrays.asList(first,second)); assertEquals(1,legend.rows.size());
            legend.scope.setSelectedIndex(1); assertEquals(2,legend.rows.size());
            legend.list.setSelectedIndex(1);
            legend.replace(Arrays.asList(first),Arrays.asList(second,first)); assertEquals(Arrays.asList(b),legend.selected());
            legend.preset.setSelectedItem("Skilling"); assertEquals(1,legend.rows.size()); assertSame(a,legend.rows.get(0).marker);
            legend.search.setText("herblroe"); legend.filter(); assertEquals(1,legend.rows.size());
            legend.search.setText("ranged"); legend.filter(); assertEquals(0,legend.rows.size());
            legend.close();
        });
    }
}
