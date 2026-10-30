package com.windowmarkers;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.List;
import javax.imageio.ImageIO;
import net.runelite.client.ui.FontManager;
import org.junit.Test;
import static org.junit.Assert.*;

public class BankItemTooltipTest
{
    private static ItemCatalogueIndex.Rank halberd()
    {
        ItemCatalogueIndex index=new ItemCatalogueIndex(Collections.singletonList(
            new ItemCatalogueIndex.Item(29796,"Noxious halberd",3,new String[]{"Wield"})),false);
        return index.find("araxxor","529623:1").ranks.get(29796);
    }

    @Test public void actualAraxxorHalberdKeepsRankAndSetupWithoutCopyingTheStrategyParagraph()
    {
        ItemCatalogueIndex.Rank rank=halberd();assertNotNull(rank);
        assertTrue(rank.explanation.contains("Ctrl+click"));String original=rank.explanation;
        String compact=BankItemTooltip.summary(rank,null);
        assertTrue(compact.contains("1st"));assertTrue(compact.contains("Araxxor"));
        assertFalse(compact.contains("Ctrl+click"));assertFalse(compact.contains("Mirrorback"));
        assertTrue(compact.contains("Guide choices"));assertTrue(compact.length()<220);
        assertEquals(original,rank.explanation);assertEquals(1,rank.tier);
    }

    @Test public void stateWarningsPrecedeSourceContextAndRepeatedSlotNotesAreDeduplicated()
    {
        String warning="Item state: uncharged; charge before use.";
        ItemCatalogueIndex.Rank rank=new ItemCatalogueIndex.Rank(0,"Needs charging; rank withheld.\n"+warning+"\nExample / Magic / weapon: reduced function; charge-dependent guide rank withheld. Long strategy note");
        String summary=BankItemTooltip.summary(rank,warning);
        assertTrue(summary.startsWith("Needs charging"));assertTrue(summary.contains("uncharged"));
        assertEquals(summary.indexOf(warning),summary.lastIndexOf(warning));
        assertTrue(summary.indexOf("uncharged")<summary.indexOf("Example"));
        String missing=BankItemTooltip.summary(null,"Guide example: no ready bank item for this slot. Check ownership, charges and requirements.");
        assertTrue(missing.contains("no ready bank item"));assertTrue(missing.contains("charges"));
    }

    @Test public void wrappingBoundsLongWordsAndEscapesMarkupForDifferentTooltipFonts()
    {
        BufferedImage image=new BufferedImage(600,600,BufferedImage.TYPE_INT_ARGB);Graphics2D g=image.createGraphics();
        String text="<img=999999><col=ff0000>"+"extraordinary".repeat(60)+"\n"+"More notes. ".repeat(100);
        for(Font font:new Font[]{FontManager.getRunescapeFont(),new Font(Font.SANS_SERIF,Font.BOLD,24)}) {
            g.setFont(font);List<String> lines=BankItemTooltip.wrap(text,g.getFontMetrics(),150);
            assertEquals(BankItemTooltip.MAX_LINES,lines.size());
            for(String line:lines){assertTrue(line,g.getFontMetrics().stringWidth(line)<=150);assertFalse(line.contains("<img="));assertFalse(line.contains("<col="));}
            assertTrue(lines.get(lines.size()-1).endsWith("..."));
            BankItemTooltip tip=new BankItemTooltip(text,150,new Color(20,30,25,220));
            Dimension dimension=tip.render(g);assertTrue(dimension.width<=158);assertTrue(dimension.height<=g.getFontMetrics().getHeight()*6+8);
        }
        g.dispose();
    }

    @Test public void renderCompactRealHalberdAndInventoryExamples() throws Exception
    {
        BufferedImage image=new BufferedImage(680,250,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();
        g.setColor(new Color(35,36,31));g.fillRect(0,0,680,250);g.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,13));g.setColor(Color.WHITE);
        g.drawString("Actual tooltip renderer - Araxxor halberd",15,25);g.drawString("Missing inventory item",350,25);
        g.setFont(FontManager.getRunescapeFont());Color background=new Color(0,35,35,230);
        BankItemTooltip halberd=new BankItemTooltip(BankItemTooltip.summary(halberd(),null),300,background);halberd.setPreferredLocation(new Point(15,45));
        Dimension size=halberd.render(g);assertTrue(size.width<=308);
        BankItemTooltip missing=new BankItemTooltip(BankItemTooltip.summary(null,"Guide example: no ready bank item for this slot. Check ownership, charges and requirements."),280,background);missing.setPreferredLocation(new Point(350,45));missing.render(g);
        g.dispose();ImageIO.write(image,"png",new java.io.File("build/bank-tooltip-preview.png"));
    }
}
