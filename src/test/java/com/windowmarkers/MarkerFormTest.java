package com.windowmarkers;

import javax.swing.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class MarkerFormTest
{
    @Test public void formsWrapAtViewportWidthWithoutStretchingControls() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            MarkerForm form=new MarkerForm();
            JComboBox<String> combo=new JComboBox<>(new String[]{"All classifications"});
            JCheckBox check=new JCheckBox("Choose group");
            JTextArea text=new JTextArea(String.join(", ",java.util.Collections.nCopies(80,"Super restore potion")));
            text.setLineWrap(true); text.setWrapStyleWord(true);
            form.add(combo); form.add(check); form.add(text);
            JScrollPane scroll=new JScrollPane(form); scroll.setSize(600,300); scroll.doLayout();
            scroll.getViewport().doLayout(); form.doLayout();
            int wideHeight=text.getHeight();
            scroll.setSize(260,300); scroll.doLayout(); scroll.getViewport().doLayout(); form.doLayout();
            assertEquals(scroll.getViewport().getWidth(),form.getWidth());
            assertEquals(combo.getPreferredSize().height,combo.getHeight());
            assertEquals(0,combo.getX()); assertEquals(0,check.getX()); assertEquals(0,text.getX());
            assertTrue(text.getHeight()>wideHeight);
            assertTrue(text.getY()>=check.getY()+check.getHeight());
            assertEquals(form.getWidth(),text.getWidth());
        });
    }
}
