package com.windowmarkers;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import net.runelite.api.Client;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.banktags.BankTagsService;
import net.runelite.client.plugins.banktags.TagManager;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class PresetVisibilityTest
{
    @Test public void outlinesBackgroundsAndRankLabelsCanBeHiddenIndependently() throws Exception
    {
        WindowMarkersPlugin p=new WindowMarkersPlugin();p.client=mock(Client.class);
        p.config=mock(WindowMarkersConfig.class,CALLS_REAL_METHODS);p.borders=new BorderRenderer();
        when(p.config.bankLabels()).thenReturn(WindowMarkersConfig.BankLabels.OFF);
        when(p.config.style()).thenReturn(WindowMarkersConfig.BorderStyle.PLAIN);
        p.searchView=new BankSearchView();p.searchView.bankTags=mock(BankTagsService.class);p.searchView.tags=mock(TagManager.class);p.searchView.items=mock(ItemManager.class);
        when(p.searchView.items.canonicalize(anyInt())).thenAnswer(c->c.getArgument(0));
        doAnswer(c->{when(p.searchView.bankTags.getActiveTag()).thenReturn(c.getArgument(0));return null;}).when(p.searchView.bankTags).openBankTag(anyString(),anyInt());
        Marker marker=new Marker(0,0,48,36);marker.linkedItemIds=new int[]{1};marker.scrollWithItems=true;marker.backgroundArgb=Color.RED.getRGB();
        p.searchView.show("Hydra",Collections.singletonList(marker),true,null,Collections.singletonMap(1,new ItemCatalogueIndex.Rank(1,"First choice")));
        field(p,"bank",new Rectangle(0,0,140,110));field(p,"itemArea",new Rectangle(0,0,140,110));
        field(p,"itemCells",Collections.singletonList(new ItemAnchors.Cell(1,new Rectangle(26,22,36,32))));
        field(p,"rankingIndex",new BankSearchIndex(Collections.emptyList(),Collections.emptyList(),new CustomPresets()));
        BufferedImage normal=render(p);assertNotEquals(0,normal.getRGB(40,30));
        when(p.config.hidePresetItemOutlines()).thenReturn(true);
        BufferedImage labels=render(p);assertEquals(0,labels.getRGB(40,30));assertTrue(pixels(labels)>0);
        when(p.config.hidePresetItemLabels()).thenReturn(true);assertEquals(0,pixels(render(p)));
        marker.label="Hydra";when(p.config.bankLabels()).thenReturn(WindowMarkersConfig.BankLabels.MINI);
        assertEquals("Preset name tags start hidden",0,pixels(render(p)));
        when(p.config.hidePresetNameTags()).thenReturn(false);assertTrue(pixels(render(p))>0);
        when(p.config.hidePresetNameTags()).thenReturn(true);
        p.bankLayout=new BankLayoutController();p.bankLayout.config=p.config;p.bankLayout.searchView=p.searchView;
        var headings=BankLayoutController.class.getDeclaredField("searchHeadings");headings.setAccessible(true);
        @SuppressWarnings("unchecked") java.util.List<SetupItemLayout.Heading> list=(java.util.List<SetupItemLayout.Heading>)headings.get(p.bankLayout);
        list.add(new SetupItemLayout.Heading("Equipment",1,80));
        assertTrue("Section headings remain independently visible",pixels(render(p))>0);
        when(p.config.hidePresetSectionHeadings()).thenReturn(true);assertEquals(0,pixels(render(p)));
        when(p.config.hidePresetItemOutlines()).thenReturn(false);assertNotEquals(0,render(p).getRGB(40,30));
        p.searchView.clear();assertFalse(p.hidePresetOutlines());assertFalse(p.hidePresetLabels());
    }
    private static void field(Object o,String name,Object value)throws Exception{var f=o.getClass().getDeclaredField(name);f.setAccessible(true);f.set(o,value);}
    private static BufferedImage render(WindowMarkersPlugin p){BufferedImage image=new BufferedImage(140,110,BufferedImage.TYPE_INT_ARGB);Graphics2D g=image.createGraphics();p.new BackgroundOverlay().render(g);p.new MarkerOverlay().render(g);g.dispose();return image;}
    private static int pixels(BufferedImage image){int count=0;for(int pixel:image.getRGB(0,0,image.getWidth(),image.getHeight(),null,0,image.getWidth()))if((pixel>>>24)>0)count++;return count;}
}
