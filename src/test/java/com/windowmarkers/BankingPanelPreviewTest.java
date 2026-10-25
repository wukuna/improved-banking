package com.windowmarkers;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import javax.imageio.ImageIO;
import javax.swing.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Offline layout check: no game client, account interaction or input automation. */
public class BankingPanelPreviewTest
{
    @Test public void bankDropdownFitsAndEnterOpensTheSelectedChoice() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            BankCatalogue.Choice choice=new BankCatalogue.Choice(BankCatalogue.Kind.PRESET,"Tombs of Amascut",AutoCategories.Preset.TOMBS_OF_AMASCUT);
            java.util.concurrent.atomic.AtomicReference<BankCatalogue.Choice> opened=new java.util.concurrent.atomic.AtomicReference<>();
            BankCataloguePicker.PickerPanel panel=new BankCataloguePicker.PickerPanel(Arrays.asList(choice),opened::set,() -> {},() -> {},true);
            try {
                panel.addNotify(); panel.setSize(390,350); layout(panel); layout(panel); checkButtons(panel);
                panel.rows.addElement(choice); panel.list.setSelectedIndex(0);
                panel.list.getActionMap().get("open").actionPerformed(new java.awt.event.ActionEvent(panel,0,"open"));
                assertSame(choice,opened.get()); assertTrue(panel.list.getHeight()>60);
                BufferedImage image=new BufferedImage(390,350,BufferedImage.TYPE_INT_ARGB); Graphics2D g=image.createGraphics(); panel.printAll(g); g.dispose();
                File output=new File("build/ui-preview/bank-picker.png"); output.getParentFile().mkdirs(); ImageIO.write(image,"png",output);
                panel.search.setText("new query"); assertTrue(panel.rows.isEmpty());
            } catch(Exception ex) { throw new RuntimeException(ex); }
            finally { panel.stop(); panel.removeNotify(); }
        });
    }
    @Test public void itemLookupActionsFitBelowSelectableMemberships() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                ItemLookupPanel panel=new ItemLookupPanel(Arrays.asList("Fire cape (#6570)","Fire max cape (#13329)"),Arrays.asList(
                    new java.util.TreeSet<>(Arrays.asList("Category: Melee equipment","Preset: Chambers of Xeric / CoX Melee")),
                    new java.util.TreeSet<>(Arrays.asList("Category: Melee equipment","Category: Runecraft — pouch protection"))));
                panel.addNotify(); panel.setSize(740,460); layout(panel); layout(panel); checkButtons(panel);
                assertTrue(panel.list.getHeight()>100);
                BufferedImage image=new BufferedImage(740,460,BufferedImage.TYPE_INT_ARGB); Graphics2D g=image.createGraphics(); panel.printAll(g); g.dispose();
                File output=new File("build/ui-preview/item-lookup.png"); output.getParentFile().mkdirs(); ImageIO.write(image,"png",output); panel.removeNotify();
            } catch(Exception ex) { throw new RuntimeException(ex); }
        });
    }
    @Test public void legendManagementFitsNarrowSidebarAndRendersPreview() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            try {
                MarkerLegend legend=new MarkerLegend(m -> {},m -> {});
                Marker food=new Marker(0,0,48,36); food.label="My Food"; food.linkedItemIds=new int[]{385,3144}; food.preset="Custom";
                Marker magic=new Marker(0,0,48,36); magic.label="Magic weapons"; magic.linkedItemIds=new int[]{11905}; magic.preset="CoX";
                legend.replace(Arrays.asList(new MarkerLegend.Entry(food,food.label,Color.GREEN),new MarkerLegend.Entry(magic,magic.label,Color.CYAN)));
                legend.addNotify(); legend.setSize(205,640); layout(legend); layout(legend);
                assertEquals(2,legend.rows.size());
                assertTrue(legend.list.getCellBounds(0,0).height>10);
                checkButtons(legend);
                BufferedImage image=new BufferedImage(205,640,BufferedImage.TYPE_INT_ARGB); Graphics2D g=image.createGraphics(); legend.printAll(g); g.dispose();
                File output=new File("build/ui-preview/legend.png"); output.getParentFile().mkdirs(); ImageIO.write(image,"png",output);
                legend.close(); legend.removeNotify();
            } catch(Exception ex) { throw new RuntimeException(ex); }
        });
    }
    private void layout(Container c) { c.doLayout(); for(Component child:c.getComponents()) if(child instanceof Container) layout((Container)child); }
    private void checkButtons(Container c) {
        for(Component child:c.getComponents()) {
            if(child instanceof JButton) {
                JButton b=(JButton)child;
                if(!b.getText().isEmpty()) assertTrue(b.getText()+" clipped at "+b.getWidth(),b.getFontMetrics(b.getFont()).stringWidth(b.getText())<=b.getWidth()-b.getInsets().left-b.getInsets().right);
            }
            if(child instanceof Container) checkButtons((Container)child);
        }
    }
}
