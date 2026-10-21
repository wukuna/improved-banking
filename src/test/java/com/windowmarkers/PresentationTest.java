package com.windowmarkers;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class PresentationTest
{
    private AutoCategories.Item item(int id,String name)
    { return new AutoCategories.Item(id,name,true,null,0,0,0); }
    @Test public void loneSetWeaponsKeepTheirStandaloneRole()
    {
        String[][] pairs={{"Dual macuahuitl","Blood moon chestplate","Melee weapons"},{"Guthan's warspear","Guthan's helm","Melee weapons"},{"Eclipse atlatl","Eclipse moon tassets","Ranged weapons"}};
        for(AutoCategories.Preset preset:new AutoCategories.Preset[]{AutoCategories.Preset.ALL_CATEGORIES,AutoCategories.Preset.COMBAT_GROUPS})
        for(String[] pair:pairs)
        {
            AutoCategories.Item weapon=item(-1,pair[0]);
            assertTrue(AutoCategories.suggest(preset,Collections.singletonList(weapon)).containsKey(pair[2]));
            Map<String,List<AutoCategories.Item>> grouped=AutoCategories.suggest(preset,Arrays.asList(weapon,item(-2,pair[1])));
            assertEquals(2,grouped.get(ItemTaxonomy.equipmentSet(pair[0])).size());
        }
    }
    @Test public void paletteAvoidsExistingColorsAcrossBatches()
    {
        List<Color> used=new ArrayList<>(Arrays.asList(Color.RED,Color.GREEN,Color.BLUE));
        Set<Integer> unique=new HashSet<>(); for(Color c:used) { unique.add(c.getRGB()); }
        for(int n=0;n<100;n++) { Color c=MarkerPresentation.nextColor(used); assertTrue(unique.add(c.getRGB())); used.add(c); }
    }
    @Test public void shortTitlesKeepDetailedReviewNamesSeparate()
    {
        assertEquals("Guthan",MarkerPresentation.title(ItemTaxonomy.equipmentSet("guthan's warspear")));
        assertEquals("Blood Moon",MarkerPresentation.title(ItemTaxonomy.equipmentSet("dual macuahuitl")));
        assertEquals("Potions",MarkerPresentation.title("Potions — general, not activity advice"));
    }
    @Test public void badgesStayWithinBankAndNeverOverlap()
    {
        List<Rectangle> occupied=new ArrayList<>(); Rectangle clip=new Rectangle(0,0,200,100),region=new Rectangle(175,0,25,35);
        Rectangle first=MarkerPresentation.place(region,clip,100,18,Marker.LabelPlacement.ABOVE,occupied);
        Rectangle second=MarkerPresentation.place(region,clip,100,18,Marker.LabelPlacement.ABOVE,occupied);
        assertNotNull(first); assertNotNull(second); assertTrue(clip.contains(first)); assertTrue(clip.contains(second)); assertFalse(first.intersects(second));
        assertNull(MarkerPresentation.place(region,clip,100,18,Marker.LabelPlacement.ABOVE,occupied));
        Graphics2D g=new BufferedImage(200,100,BufferedImage.TYPE_INT_ARGB).createGraphics();
        try { String label=MarkerPresentation.fit("A very long category description",g.getFontMetrics(),80); assertTrue(g.getFontMetrics().stringWidth(label)<=80); }
        finally { g.dispose(); }
    }
    @Test public void occupiedItemSlotsPreventLabelsFromCoveringIcons()
    {
        Rectangle clip=new Rectangle(0,0,200,100),region=new Rectangle(20,30,100,32);
        List<Rectangle> protectedItems=new ArrayList<>(Collections.singletonList(new Rectangle(clip)));
        assertNull(MarkerPresentation.place(region,clip,90,18,Marker.LabelPlacement.ABOVE,protectedItems));
    }
    @Test public void dialogsFitSmallAndOffsetMonitorWorkAreas()
    {
        for(Rectangle screen:new Rectangle[]{new Rectangle(0,0,640,440),new Rectangle(-1920,40,1920,1000)})
        { assertTrue(screen.contains(MarkerDialogs.initialBounds(screen,620,640))); }
    }
}
