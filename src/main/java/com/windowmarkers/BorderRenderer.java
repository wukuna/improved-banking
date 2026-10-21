package com.windowmarkers;

import java.awt.*;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.SpritePixels;
import net.runelite.client.game.SpriteManager;

final class BorderRenderer
{
    // Vanilla equipment-button frame: four corners, left/top/right/bottom edges.
    static final int[] IDS = {913, 914, 915, 916, 917, 918, 919, 920};
    @Inject Client client;
    @Inject SpriteManager sprites;
    private final SpritePixels[] previous = new SpritePixels[8];
    private final BufferedImage[] themed = new BufferedImage[8];
    private final java.util.Map<BufferedImage, java.util.Map<Integer, BufferedImage>> tinted = new java.util.IdentityHashMap<>();
    private int tintCount;

    BufferedImage image(int index, boolean theme)
    {
        SpritePixels override = theme ? client.getSpriteOverrides().get(IDS[index]) : null;
        if (override == null) { return sprites.getSprite(IDS[index], 0); }
        if (previous[index] != override)
        {
            previous[index] = override;
            themed[index] = override.toBufferedImage();
        }
        return themed[index];
    }

    void clear()
    {
        java.util.Arrays.fill(previous, null);
        java.util.Arrays.fill(themed, null);
        tinted.clear(); tintCount = 0;
    }

    void draw(Graphics2D g, Rectangle r, WindowMarkersConfig.BorderStyle style, Color color, boolean tint)
    {
        BufferedImage[] parts = new BufferedImage[8];
        if (style != WindowMarkersConfig.BorderStyle.PLAIN)
        {
            for (int i=0; i<8; i++)
            {
                parts[i] = image(i, style == WindowMarkersConfig.BorderStyle.ACTIVE_THEME);
                if (parts[i] == null) { plain(g, r, color); return; }
                if (tint) { parts[i] = tint(parts[i], color); }
            }
            drawFrame(g, r, parts);
        }
        else { plain(g, r, color); }
    }

    BufferedImage tint(BufferedImage source, Color color)
    {
        if (tintCount >= 256) { tinted.clear(); tintCount = 0; }
        java.util.Map<Integer,BufferedImage> colors = tinted.computeIfAbsent(source, key -> new java.util.HashMap<>());
        BufferedImage result = colors.get(color.getRGB());
        if (result != null) { return result; }
        result = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y=0;y<source.getHeight();y++) for (int x=0;x<source.getWidth();x++)
        {
            int pixel=source.getRGB(x,y);
            int light=Math.max((pixel >> 16)&255, Math.max((pixel >> 8)&255, pixel&255));
            int alpha=(pixel >>> 24)*color.getAlpha()/255;
            result.setRGB(x,y,(alpha<<24) | ((color.getRed()*light/255)<<16) | ((color.getGreen()*light/255)<<8) | (color.getBlue()*light/255));
        }
        colors.put(color.getRGB(),result); tintCount++;
        return result;
    }

    static void plain(Graphics2D g, Rectangle r, Color color)
    {
        g.setColor(color);
        g.drawRect(r.x, r.y, r.width - 1, r.height - 1);
    }

    static void drawFrame(Graphics2D graphics, Rectangle r, BufferedImage[] p)
    {
        Graphics2D g = (Graphics2D) graphics.create();
        try
        {
            g.clip(r);
            int left = Math.min(r.width / 2, Math.max(p[0].getWidth(), p[2].getWidth()));
            int right = Math.min(r.width / 2, Math.max(p[1].getWidth(), p[3].getWidth()));
            int top = Math.min(r.height / 2, Math.max(p[0].getHeight(), p[1].getHeight()));
            int bottom = Math.min(r.height / 2, Math.max(p[2].getHeight(), p[3].getHeight()));
            tile(g, p[4], new Rectangle(r.x, r.y+top, Math.min(left,p[4].getWidth()), r.height-top-bottom));
            tile(g, p[5], new Rectangle(r.x+left, r.y, r.width-left-right, Math.min(top,p[5].getHeight())));
            tile(g, p[6], new Rectangle(r.x+r.width-Math.min(right,p[6].getWidth()), r.y+top, Math.min(right,p[6].getWidth()), r.height-top-bottom));
            tile(g, p[7], new Rectangle(r.x+left, r.y+r.height-Math.min(bottom,p[7].getHeight()), r.width-left-right, Math.min(bottom,p[7].getHeight())));
            g.drawImage(p[0], r.x, r.y, left, top, null);
            g.drawImage(p[1], r.x+r.width-right, r.y, right, top, null);
            g.drawImage(p[2], r.x, r.y+r.height-bottom, left, bottom, null);
            g.drawImage(p[3], r.x+r.width-right, r.y+r.height-bottom, right, bottom, null);
        }
        finally { g.dispose(); }
    }

    private static void tile(Graphics2D g, BufferedImage image, Rectangle bounds)
    {
        if (bounds.isEmpty()) { return; }
        g.setPaint(new TexturePaint(image, new Rectangle(bounds.x, bounds.y, image.getWidth(), image.getHeight())));
        g.fill(bounds);
    }
}
