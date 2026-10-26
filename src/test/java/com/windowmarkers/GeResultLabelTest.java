package com.windowmarkers;

import net.runelite.api.FontTypeFace;
import net.runelite.api.widgets.Widget;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class GeResultLabelTest
{
    static FontTypeFace font()
    {
        FontTypeFace font=mock(FontTypeFace.class);
        when(font.getBaseline()).thenReturn(12);
        when(font.getTextWidth(anyString())).thenAnswer(call -> {
            String text=((String)call.getArgument(0)).replaceAll("<[^>]*>","");
            int width=0;for(char c:text.toCharArray()) width+=c=='i'||c=='l'?3:c=='M'||c=='W'?8:6;
            return width;
        });
        return font;
    }
    @Test public void longNamesAndAllBadgeCombinationsStayInsideTwoNativeLines()
    {
        FontTypeFace font=font();Widget label=mock(Widget.class);
        when(label.getFont()).thenReturn(font);when(label.getHeight()).thenReturn(32);
        for(int width:new int[]{75,100,120,160}) {
            when(label.getWidth()).thenReturn(width);
            for(String name:new String[]{"Amulet of rancour","<u>Ancestral</u> robe bottom","Lightbearer","Masori chaps (f)"})
            for(String badges:new String[]{"[1]","[O][2]","<col=ff7777>[!]</col>[N][O][3]"}) {
                String result=GeResultLabel.format(label,name,badges+" <col=55dd88>~123.4m</col>");
                String[] lines=result.split("<br>",-1);assertEquals(2,lines.length);
                for(String line:lines) assertTrue(line,font.getTextWidth(line)<=width);
            }
        }
    }
    @Test public void truncatedMarkupIsClosedBeforeTheDetailsLine()
    {
        String text=GeResultLabel.fit("<u>Ancestral robe bottom</u>",font(),70);
        assertTrue(text.startsWith("<u>"));assertTrue(text.endsWith("...</col></u></str>"));
        assertFalse(text.contains("<br>"));
    }
    @Test public void shortRowsAndUnavailableFontsDoNotForceExtraLines()
    {
        Widget label=mock(Widget.class);when(label.getWidth()).thenReturn(120);when(label.getHeight()).thenReturn(16);
        FontTypeFace font=font();when(label.getFont()).thenReturn(font);
        assertEquals("Shark",GeResultLabel.format(label,"Shark","[O] ~900"));
        when(label.getFont()).thenReturn(null);
        assertEquals("Shark",GeResultLabel.format(label,"Shark","[O] ~900"));
    }
}
