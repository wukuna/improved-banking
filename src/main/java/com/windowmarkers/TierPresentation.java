package com.windowmarkers;

import java.awt.*;

/** The same guide tiers, colours and labels in the bank and GE. */
final class TierPresentation
{
    static String ordinal(int tier) { return tier==1?"1st":tier==2?"2nd":tier==3?"3rd":""; }
    static String key(WindowMarkersConfig config) {
        StringBuilder text=new StringBuilder("<html>Guide tiers: ");
        for(int i=1;i<=3;i++) {
            if(i>1) text.append(" · ");
            text.append("<font color='#").append(String.format("%06x",colour(config,i).getRGB()&0xffffff)).append("'>").append(ordinal(i)).append("</font>");
        }
        return text.append("</html>").toString();
    }
    static Color colour(WindowMarkersConfig config,int tier) {
        return tier==1?config.geFirstColour():tier==2?config.geSecondColour():config.geThirdColour();
    }
    static ItemCatalogueIndex.Rank merge(ItemCatalogueIndex.Rank a,ItemCatalogueIndex.Rank b) {
        if(a.tier==b.tier && a.role.equals(b.role)) return a;
        return new ItemCatalogueIndex.Rank(0,"Different selected guide roles or requirements; select a single setup to see its tier.");
    }
    static void draw(Graphics2D g,Rectangle grid,int tier,WindowMarkersConfig config) {
        if(tier<1 || tier>3) return;
        Color colour=colour(config,tier);
        g.setStroke(new BasicStroke(2)); BorderRenderer.plain(g,grid,colour);
        drawLabel(g,grid,tier,config);
    }
    static void drawLabel(Graphics2D g,Rectangle grid,int tier,WindowMarkersConfig config) {
        if(tier<1 || tier>3) return;
        Color colour=colour(config,tier);
        g.setFont(new Font("Arial",Font.BOLD,9));
        String text=ordinal(tier); FontMetrics fm=g.getFontMetrics();
        int width=fm.stringWidth(text)+4,height=fm.getHeight();
        int x=grid.x+grid.width-width,y=grid.y+grid.height-height;
        g.setColor(new Color(15,15,15,220));g.fillRoundRect(x,y,width,height,3,3);
        g.setColor(colour);g.drawRoundRect(x,y,width-1,height-1,3,3);
        g.setColor(Color.WHITE);g.drawString(text,x+2,y+fm.getAscent());
    }
}
