package com.windowmarkers;

import com.google.gson.Gson;
import java.util.*;
import javax.swing.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class CustomViewEditorTest
{
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
