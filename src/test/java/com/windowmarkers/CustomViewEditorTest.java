package com.windowmarkers;

import com.google.gson.Gson;
import java.util.*;
import javax.swing.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class CustomViewEditorTest
{
    @Test public void addedPaletteItemSurvivesGroupChangesSaveAndReopening() throws Exception {
        CustomPresets.Preset draft=new CustomPresets.Preset();draft.name="Meals";
        Marker group=new Marker(0,0,48,36);group.label="Food";group.linkedItemIds=new int[]{385};draft.groups.add(group);
        SwingUtilities.invokeAndWait(()-> {
            CustomViewEditor editor=new CustomViewEditor(draft,new BankItemSearch(),Collections.emptySet(),Collections.emptyList());
            try {
                java.lang.reflect.Field field=CustomViewEditor.class.getDeclaredField("palette");field.setAccessible(true);
                ItemPalette palette=(ItemPalette)field.get(editor);
                field=ItemPalette.class.getDeclaredField("results");field.setAccessible(true);
                @SuppressWarnings("unchecked") DefaultListModel<BankItemSearch.Choice> results=(DefaultListModel<BankItemSearch.Choice>)field.get(palette);
                results.addElement(new BankItemSearch.Choice(3144,"Cooked karambwan"));
                field=ItemPalette.class.getDeclaredField("tiles");field.setAccessible(true);
                ((JList<?>)field.get(palette)).setSelectedIndex(0);
                clickAdd(palette);
                editor.flush();editor.list.clearSelection();editor.list.setSelectedIndex(0);
                assertTrue(editor.valid());assertArrayEquals(new int[]{385,3144},draft.groups.get(0).linkedItemIds);
                CustomPresets.Preset reopened=new Gson().fromJson(new Gson().toJson(draft),CustomPresets.Preset.class);
                assertArrayEquals(new int[]{385,3144},reopened.groups.get(0).linkedItemIds);
            } catch(ReflectiveOperationException ex) { throw new AssertionError(ex); }
            finally { editor.close(); }
        });
    }
    private static void clickAdd(java.awt.Container container) {
        for(java.awt.Component child:container.getComponents()) {
            if(child instanceof JButton && ((JButton)child).getText().startsWith("Add selected")) ((JButton)child).doClick();
            else if(child instanceof java.awt.Container) clickAdd((java.awt.Container)child);
        }
    }
    @Test public void customGroupLabelsAreLiteralTextNotHtml() throws Exception {
        CustomPresets.Preset draft=new CustomPresets.Preset();draft.name="View";
        Marker group=new Marker(0,0,48,36);group.label="<html><b>My group</b>";group.linkedItemIds=new int[]{385};draft.groups.add(group);
        SwingUtilities.invokeAndWait(()-> {
            CustomViewEditor editor=new CustomViewEditor(draft,new BankItemSearch(),Collections.emptySet(),Collections.emptyList());
            try {
                JLabel cell=(JLabel)editor.list.getCellRenderer().getListCellRendererComponent(editor.list,group,0,false,false);
                assertTrue(cell.getText().startsWith(group.label));
                assertNull(cell.getClientProperty(javax.swing.plaf.basic.BasicHTML.propertyKey));
                assertTrue(cell.getToolTipText().startsWith("Group: "));
            } finally { editor.close(); }
        });
    }
    @Test public void draftChangesDoNotChangeOriginalAndReorderingKeepsMembership() throws Exception {
        CustomPresets.Preset original=new CustomPresets.Preset();original.name="My Food";
        for(int id:new int[]{385,3144}) { Marker m=new Marker(0,0,48,36);m.label="Group "+id;m.linkedItemIds=new int[]{id};original.groups.add(m); }
        Gson gson=new Gson();String before=gson.toJson(original);
        CustomPresets.Preset draft=gson.fromJson(before,CustomPresets.Preset.class);
        SwingUtilities.invokeAndWait(()-> {
            CustomViewEditor editor=new CustomViewEditor(draft,new BankItemSearch(),Collections.emptySet(),Arrays.asList(new BankItemSearch.Choice(385,"Shark"),new BankItemSearch.Choice(3144,"Cooked karambwan")));
            try {
                editor.groupName.setText("Meals");editor.list.setSelectedIndex(1);editor.move(-1);
                assertEquals(3144,editor.groups.get(0).linkedItemIds[0]);
                editor.removeGroups();assertEquals(1,editor.groups.size());
                editor.name.setText("My edited food");assertTrue(editor.valid());
                assertEquals("Meals",draft.groups.get(0).labelText());assertArrayEquals(new int[]{385},draft.groups.get(0).linkedItemIds);
                assertEquals(before,gson.toJson(original));
                editor.addGroup();assertFalse(editor.valid());
            } finally { editor.close(); }
        });
    }
}
