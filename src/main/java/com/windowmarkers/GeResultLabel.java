package com.windowmarkers;

import net.runelite.api.FontTypeFace;
import net.runelite.api.widgets.Widget;

/** Fits annotations inside the native GE result row without moving its click target. */
final class GeResultLabel
{
    static String format(Widget label, String name, String details)
    {
        if (details.isEmpty()) return name;
        FontTypeFace font = label.getFont();
        int width = label.getWidth();
        if (font == null || width <= 0) return name;
        int lineHeight = label.getLineHeight() > 0 ? label.getLineHeight() : font.getBaseline();
        // Unusual layouts may only have room for the name. Details remain in the tooltip.
        if (lineHeight <= 0 || label.getHeight() < lineHeight * 2) return fit(name, font, width);
        return fit(name, font, width) + "<br>" + fit(details, font, width);
    }

    static String fit(String text, FontTypeFace font, int width)
    {
        text = text.replace("<br>", " ");
        if (font.getTextWidth(text) <= width) return text;
        String ending = "...</col></u></str>";
        if (font.getTextWidth(ending) > width) return "";
        StringBuilder prefix = new StringBuilder();
        // Preserve the native search highlighting; never cut through a markup tag.
        for (int i = 0; i < text.length();) {
            int end = text.charAt(i) == '<' ? text.indexOf('>', i) + 1 : i + 1;
            if (end <= i) end = i + 1;
            String token = text.substring(i, end);
            if (font.getTextWidth(prefix.toString() + token + ending) > width) break;
            prefix.append(token);
            i = end;
        }
        return prefix + ending;
    }
}
