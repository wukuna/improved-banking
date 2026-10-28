package com.windowmarkers;

import java.awt.Color;
import java.awt.image.BufferedImage;
import org.junit.Test;
import static org.junit.Assert.*;

public class BorderTintCacheTest
{
    @Test public void filledCacheKeepsReusingColoursAcrossRenderPasses()
    {
        BorderRenderer renderer = new BorderRenderer();
        BufferedImage source = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
        source.setRGB(0, 0, 0xffffffff);
        BufferedImage[] expected = new BufferedImage[400];
        for (int i = 0; i < expected.length; i++) expected[i] = renderer.tint(source, new Color(i));
        for (int frame = 0; frame < 3; frame++)
            for (int i = 0; i < expected.length; i++) assertSame(expected[i], renderer.tint(source, new Color(i)));
        renderer.clear();
        assertNotSame(expected[0], renderer.tint(source, new Color(0)));
    }

    @Test public void recentlyUsedTintSurvivesEntryLimitAndPackReplacementIsFresh()
    {
        BorderRenderer renderer = new BorderRenderer();
        BufferedImage source = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        BufferedImage recent = renderer.tint(source, new Color(0));
        BufferedImage oldest = renderer.tint(source, new Color(1));
        for (int i = 2; i < 2050; i++) {
            renderer.tint(source, new Color(i));
            assertSame(recent, renderer.tint(source, new Color(0)));
        }
        assertNotSame(oldest, renderer.tint(source, new Color(1)));
        BufferedImage replacement = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        replacement.setRGB(0, 0, 0xffffffff);
        BufferedImage changed = renderer.tint(replacement, new Color(0));
        assertNotSame(recent, changed);
        assertEquals(0xff000000, changed.getRGB(0, 0));
    }

    @Test public void largePackSpritesHaveABoundedPixelBudget()
    {
        BorderRenderer renderer = new BorderRenderer();
        BufferedImage source = new BufferedImage(512, 512, BufferedImage.TYPE_INT_ARGB);
        BufferedImage first = renderer.tint(source, new Color(0));
        for (int i = 1; i <= 8; i++) renderer.tint(source, new Color(i));
        assertNotSame(first, renderer.tint(source, new Color(0)));
    }
}
