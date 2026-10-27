package com.windowmarkers;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicHTML;
import org.junit.Test;
import static org.junit.Assert.*;

public class PlainTextTest
{
    @Test public void userNamesStayLiteralOnFirstAndLaterDisplay() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            String text = "<html><b>My saved view</b></html>";
            JLabel control = new JLabel(text);
            assertNotNull(control.getClientProperty(BasicHTML.propertyKey));
            JLabel label = new JLabel();
            JButton button = new JButton();
            PlainText.set(label, text);
            PlainText.set(button, text);
            assertNull(label.getClientProperty(BasicHTML.propertyKey));
            assertNull(button.getClientProperty(BasicHTML.propertyKey));
            assertEquals(text, label.getText());
            assertEquals(text, button.getText());
            label.setText("<html><i>Renamed</i></html>");
            button.setText("<html><i>Renamed</i></html>");
            assertNull(label.getClientProperty(BasicHTML.propertyKey));
            assertNull(button.getClientProperty(BasicHTML.propertyKey));
        });
    }
    @Test public void savedNameRendererNeverCreatesHtmlView() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            String name = "<html><b>Profile</b></html>";
            JLabel rendered = (JLabel) PlainText.renderer().getListCellRendererComponent(
                new JList<String>(), name, 0, true, false);
            assertEquals(name, rendered.getText());
            assertNull(rendered.getClientProperty(BasicHTML.propertyKey));
        });
    }
}
