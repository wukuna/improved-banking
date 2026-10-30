package com.windowmarkers;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class JoinedItemBordersTest
{
    private ItemAnchors.Cell cell(int id,int x,int y) { return new ItemAnchors.Cell(id,new Rectangle(x+6,y+2,36,32)); }
    @Test public void lShapeHasNoInternalSeamsAndLeavesTheMissingSlotEmpty() {
        JoinedItemBorders renderer=new JoinedItemBorders();
        List<Rectangle> cells=Arrays.asList(new Rectangle(10,10,48,36),new Rectangle(58,10,48,36),new Rectangle(10,46,48,36));
        BufferedImage image=new BufferedImage(130,100,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=image.createGraphics();g.setStroke(new BasicStroke(2));renderer.draw(g,cells,Color.YELLOW);g.dispose();
        assertEquals(0,image.getRGB(58,25));assertEquals(0,image.getRGB(25,46));
        assertEquals(0,image.getRGB(80,65));
        assertNotEquals(0,image.getRGB(10,25));assertNotEquals(0,image.getRGB(58,65));
        assertFalse(renderer.outline(cells).contains(80,65));
    }
    @Test public void holesAndSeparatedSectionsStayOpen() {
        List<Rectangle> cells=new ArrayList<>();
        for(int y=0;y<3;y++) for(int x=0;x<3;x++) if(x!=1 || y!=1) cells.add(new Rectangle(x*48,y*36,48,36));
        cells.add(new Rectangle(0,200,48,36));
        Shape outline=new JoinedItemBorders().outline(cells);
        assertFalse(outline.contains(72,54));assertFalse(outline.contains(20,160));assertTrue(outline.contains(20,220));
    }
    @Test public void rankColoursReplaceYellowAndAdjacentSameColoursShareOneGroup() {
        WindowMarkersConfig config=mock(WindowMarkersConfig.class);when(config.geTierColours()).thenReturn(true);
        when(config.geFirstColour()).thenReturn(Color.MAGENTA);when(config.geSecondColour()).thenReturn(Color.CYAN);
        Map<Integer,ItemCatalogueIndex.Rank> ranks=new HashMap<>();
        ranks.put(1,new ItemCatalogueIndex.Rank(1,"First"));ranks.put(2,new ItemCatalogueIndex.Rank(1,"First"));
        ranks.put(3,new ItemCatalogueIndex.Rank(2,"Second"));
        List<ItemAnchors.Cell> cells=Arrays.asList(cell(1,0,0),cell(2,48,0),cell(3,96,0),cell(4,144,0));
        var groups=JoinedItemBorders.groups(cells,new int[]{1,2,3,4},ranks,config);
        assertEquals(2,groups.get(Color.MAGENTA).size());assertEquals(Collections.singletonList(cells.get(3).grid),groups.get(Color.YELLOW));
        assertEquals(Collections.singletonList(cells.get(2).grid),groups.get(Color.CYAN));
        when(config.geTierColours()).thenReturn(false);
        assertEquals(4,JoinedItemBorders.groups(cells,new int[]{1,2,3,4},ranks,config).get(Color.YELLOW).size());
    }
    @Test public void cachedGeometryTracksMovementAndIsBounded() {
        JoinedItemBorders renderer=new JoinedItemBorders();List<Rectangle> cells=Collections.singletonList(new Rectangle(0,0,48,36));
        Shape first=renderer.outline(cells);assertSame(first,renderer.outline(cells));
        cells.get(0).translate(0,36);assertNotSame(first,renderer.outline(cells));assertTrue(first.contains(20,20));
        for(int i=0;i<40;i++) renderer.outline(Collections.singletonList(new Rectangle(i,100,48,36)));
        assertNotSame(first,renderer.outline(Collections.singletonList(new Rectangle(0,0,48,36))));
        renderer.clear();assertNotSame(first,renderer.outline(Collections.singletonList(new Rectangle(0,0,48,36))));
    }
}
