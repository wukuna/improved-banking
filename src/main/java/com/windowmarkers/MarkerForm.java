package com.windowmarkers;

import java.awt.*;
import javax.swing.*;
import javax.swing.plaf.basic.BasicHTML;
import javax.swing.text.View;

/** A width-constrained column: natural control heights and wrapping text. */
final class MarkerForm extends JPanel implements Scrollable
{
    MarkerForm() { super(null); }

    private int height(Component child, int width)
    {
        if (child instanceof JTextArea)
        {
            child.setSize(width, Short.MAX_VALUE);
        }
        if (child instanceof JLabel)
        {
            View html=(View)((JLabel)child).getClientProperty(BasicHTML.propertyKey);
            if (html!=null)
            {
                html.setSize(width,0);
                return (int)Math.ceil(html.getPreferredSpan(View.Y_AXIS));
            }
        }
        return child.getPreferredSize().height;
    }

    @Override public Dimension getPreferredSize()
    {
        Insets in=getInsets();
        int width=getWidth()>0 ? getWidth() : 420;
        int y=in.top;
        for(Component child:getComponents()) if(child.isVisible())
        { y+=height(child,Math.max(1,width-in.left-in.right))+4; }
        return new Dimension(width,y+in.bottom);
    }

    @Override public void doLayout()
    {
        Insets in=getInsets(); int y=in.top, width=Math.max(1,getWidth()-in.left-in.right);
        for(Component child:getComponents()) if(child.isVisible())
        {
            int h=height(child,width); child.setBounds(in.left,y,width,h); y+=h+4;
        }
    }
    @Override public Dimension getMinimumSize() { return new Dimension(0,0); }
    @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
    @Override public boolean getScrollableTracksViewportWidth() { return true; }
    @Override public boolean getScrollableTracksViewportHeight() { return false; }
    @Override public int getScrollableUnitIncrement(Rectangle r,int axis,int direction) { return 20; }
    @Override public int getScrollableBlockIncrement(Rectangle r,int axis,int direction) { return Math.max(20,r.height-20); }
}
