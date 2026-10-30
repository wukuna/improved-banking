package com.windowmarkers;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.runelite.client.ui.overlay.components.TooltipComponent;
import net.runelite.client.util.Text;

/** Compact bank hover summaries; full source notes stay in the guide data. */
final class BankItemTooltip extends TooltipComponent
{
    static final int MAX_LINES=6;
    private final String summary;
    private final int width;

    BankItemTooltip(String summary,int width,Color background)
    {
        this.summary=summary;this.width=Math.max(40,Math.min(300,width));setBackgroundColor(background);
    }

    @Override public Dimension render(Graphics2D graphics)
    {
        // Use the tooltip renderer's actual font, including user font/size choices.
        setText(String.join("<br>",wrap(summary,graphics.getFontMetrics(),width)));
        return super.render(graphics);
    }

    static String summary(ItemCatalogueIndex.Rank rank,String note)
    {
        Set<String> lines=new LinkedHashSet<>(),context=new LinkedHashSet<>();
        List<String> details=new ArrayList<>();boolean sourceNotes=false;
        if(rank!=null) {
            String[] raw=rank.explanation.split("\\R");
            String ordinal=TierPresentation.ordinal(rank.tier);
            lines.add(ordinal.isEmpty()?shortNote(raw[0]):"Guide: "+ordinal+" choice for this setup"+(rank.role.isEmpty()?"":" ("+rank.role+")")+".");
            for(int i=1;i<raw.length;i++) {
                String line=raw[i].trim();int split=line.indexOf(": Wiki column ");
                if(split<0)split=line.indexOf(": reduced function;");
                if(split<0)split=line.indexOf(": provisional or ordered choices;");
                if(split>=0) {
                    String source=line.substring(0,split).replace(" — Equipment alternatives","").replace(" - Equipment alternatives","");
                    if(context.size()<2)context.add(source.replace(" / "," · "));sourceNotes=true;
                } else if(!line.isEmpty()) details.add(line);
            }
        }
        // Item-state warnings precede optional context and long guide commentary.
        if(note!=null) {
            int state=note.indexOf("Item state");
            if(state>=0)lines.add(shortNote(note.substring(state)));
        }
        for(String detail:details)if(detail.startsWith("Item state"))lines.add(shortNote(detail));
        for(String detail:details)if(!detail.startsWith("Item state"))lines.add(shortNote(detail));
        if(note!=null&&!note.isBlank())lines.add(shortNote(note));
        lines.addAll(context);
        if(sourceNotes)lines.add("Full notes: Guide choices.");
        lines.remove("");return String.join("\n",lines);
    }

    private static String shortNote(String text)
    {
        String value=text.replaceAll("\\s+"," ").trim()
            .replace("Showing an owned alternative; live prices are not compared.","Owned alternative.");
        if(value.length()<=180)return value;
        int end=value.lastIndexOf(' ',177);return value.substring(0,end>100?end:177)+"...";
    }

    static List<String> wrap(String text,FontMetrics metrics,int width)
    {
        List<String> lines=new ArrayList<>();boolean truncated=false;
        for(String paragraph:text.split("\\R")) {
            String remaining=paragraph.trim().replaceAll("\\s+"," ");
            while(!remaining.isEmpty()) {
                if(lines.size()==MAX_LINES){truncated=true;break;}
                int end=0;
                while(end<remaining.length()) {
                    int next=remaining.offsetByCodePoints(end,1);
                    if(metrics.stringWidth(Text.escapeJagex(remaining.substring(0,next)))>width)break;
                    end=next;
                }
                if(end==0)end=remaining.offsetByCodePoints(0,1);
                if(end<remaining.length()) {int space=remaining.lastIndexOf(' ',end);if(space>0)end=space;}
                lines.add(Text.escapeJagex(remaining.substring(0,end).trim()));remaining=remaining.substring(end).trim();
            }
            if(truncated)break;
        }
        if(truncated&&!lines.isEmpty()) {
            String last=lines.get(lines.size()-1);
            // The escaped line may contain literal tag escapes: shorten the raw
            // last line instead of ever leaving a partial Jagex control tag.
            last=Text.removeTags(last.replace("<lt>","‹").replace("<gt>","›"));
            while(!last.isEmpty()&&metrics.stringWidth(last+"...")>width)last=last.substring(0,last.offsetByCodePoints(last.length(),-1));
            lines.set(lines.size()-1,Text.escapeJagex(last)+"...");
        }
        return lines;
    }
}
