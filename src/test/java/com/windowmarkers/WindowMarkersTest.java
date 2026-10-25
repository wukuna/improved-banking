package com.windowmarkers;

import com.google.gson.Gson;
import java.awt.*;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import javax.swing.SwingUtilities;
import net.runelite.api.*;
import net.runelite.api.events.BeforeRender;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ProfileChanged;
import net.runelite.client.events.RuneScapeProfileChanged;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.input.MouseManager;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.overlay.OverlayManager;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class WindowMarkersTest
{
    @Test public void realEventBusAcceptsAllSubscribersAcrossDisableAndReenable() throws Exception
    {
        net.runelite.client.eventbus.EventBus bus=new net.runelite.client.eventbus.EventBus();
        WindowMarkersPlugin p=plugin();
        BankLayoutController layout=new BankLayoutController();
        try {
            bus.register(layout);
            bus.register(p);
            bus.unregister(p);
            bus.unregister(layout);
            p.shutDown();
            p.startUp();
            bus.register(layout);
            bus.register(p);
            net.runelite.api.events.ScriptCallbackEvent event=new net.runelite.api.events.ScriptCallbackEvent();
            event.setEventName("bankSearchFilter");
            bus.post(event);
        } finally {
            bus.unregister(p);
            bus.unregister(layout);
            p.shutDown();
            SwingUtilities.invokeAndWait(() -> {});
        }
    }

    @Test public void itemLookupHighlightIsTemporaryAndClearedByProfileChange() throws Exception
    {
        WindowMarkersPlugin p=plugin();
        var field=WindowMarkersPlugin.class.getDeclaredField("lookupHighlight"); field.setAccessible(true);
        try {
            clearInvocations(p.configs);
            p.highlightLookupItems(new int[]{6570,13329});
            assertArrayEquals(new int[]{6570,13329},((Marker)field.get(p)).linkedItemIds);
            verify(p.configs,never()).setConfiguration(eq("windowmarkers"),anyString(),anyString(),anyString());
            when(p.configs.getProfile().getId()).thenReturn(20L); p.onProfileChanged(new ProfileChanged());
            assertNull(field.get(p));
        } finally { p.shutDown(); }
    }
    @Test public void customPresetsUseAccountAndRuneLiteProfileAndRetainSavedMarkers() throws Exception
    {
        WindowMarkersPlugin p=plugin(); Map<String,String> saved=new HashMap<>();
        when(p.configs.getConfiguration(eq("windowmarkers"),anyString(),anyString()))
            .thenAnswer(call -> saved.get(call.getArgument(1)+"/"+call.getArgument(2)));
        doAnswer(call -> { saved.put(call.getArgument(1)+"/"+call.getArgument(2),call.getArgument(3)); return null; })
            .when(p.configs).setConfiguration(eq("windowmarkers"),anyString(),anyString(),anyString());
        var field=WindowMarkersPlugin.class.getDeclaredField("customPresets"); field.setAccessible(true);
        var save=WindowMarkersPlugin.class.getDeclaredMethod("savePresets"); save.setAccessible(true);
        try {
            CustomPresets library=(CustomPresets)field.get(p); CustomPresets.Preset preset=new CustomPresets.Preset(); preset.name="My Food";
            Marker group=new Marker(0,0,48,36); group.linkedItemIds=new int[]{385,3144}; group.label="Food"; preset.groups.add(group); library.presets.add(preset);
            save.invoke(p); String original=saved.get("account-a/markersV3_profile_10_customPresets"); assertNotNull(original);
            p.createNamedLayout("Raid",false,false);
            assertEquals("My Food",((CustomPresets)field.get(p)).presets.get(0).name);
            when(p.configs.getProfile().getId()).thenReturn(20L); p.onProfileChanged(new ProfileChanged());
            assertTrue(((CustomPresets)field.get(p)).presets.isEmpty());
            when(p.configs.getProfile().getId()).thenReturn(10L); p.onProfileChanged(new ProfileChanged());
            assertEquals(1,((CustomPresets)field.get(p)).presets.size());
            when(p.configs.getRSProfileKey()).thenReturn("account-b"); p.onRuneScapeProfileChanged(new RuneScapeProfileChanged("account-a","account-b"));
            assertTrue(((CustomPresets)field.get(p)).presets.isEmpty());
            assertEquals(original,saved.get("account-a/markersV3_profile_10_customPresets"));
            assertNull(saved.get("account-b/markersV3_profile_10_customPresets"));
        } finally { p.shutDown(); }
    }

    @Test public void filteredReorderSavesAndUndoRestoresFullLayout() throws Exception
    {
        WindowMarkersPlugin p=plugin();
        try {
            String json="[{\"x\":0,\"y\":0,\"width\":48,\"height\":36,\"label\":\"A\"},{\"x\":0,\"y\":0,\"width\":48,\"height\":36,\"label\":\"Hidden\"},{\"x\":0,\"y\":0,\"width\":48,\"height\":36,\"label\":\"B\"}]";
            when(p.configs.getConfiguration("windowmarkers","account-a","markersV3_profile_10")).thenReturn(json);
            p.onProfileChanged(new ProfileChanged());
            var field=WindowMarkersPlugin.class.getDeclaredField("markers"); field.setAccessible(true);
            @SuppressWarnings("unchecked") java.util.List<Marker> markers=(java.util.List<Marker>)field.get(p);
            Marker a=markers.get(0), b=markers.get(2);
            p.reorderGroups(java.util.Arrays.asList(a,b),java.util.Collections.singletonList(b),0);
            assertEquals("B",markers.get(0).label); assertEquals("Hidden",markers.get(1).label);
            p.restoreHistory(false); assertEquals("A",markers.get(0).label); assertEquals("Hidden",markers.get(1).label);
            p.reorderGroups(java.util.Arrays.asList(a,b),java.util.Collections.singletonList(b),0); // old identities after Undo
            assertEquals("A",markers.get(0).label);
        } finally { p.shutDown(); }
    }

    @Test public void namedLayoutsPreserveDefaultAndHistoryDoesNotLeakAcrossAccounts() throws Exception
    {
        WindowMarkersPlugin p=plugin(); WindowMarkersPlugin.MarkerMouse mouse=p.new MarkerMouse();
        Map<String,String> saved=new HashMap<>();
        when(p.configs.getConfiguration(eq("windowmarkers"),anyString(),anyString()))
            .thenAnswer(call -> saved.get(call.getArgument(1)+"/"+call.getArgument(2)));
        doAnswer(call -> { saved.put(call.getArgument(1)+"/"+call.getArgument(2),call.getArgument(3)); return null; })
            .when(p.configs).setConfiguration(eq("windowmarkers"),anyString(),anyString(),anyString());
        String key="account-a/markersV3_profile_10";
        try
        {
            p.arm(); mouse.mousePressed(event(MouseEvent.MOUSE_PRESSED,120,120,1));
            mouse.mouseReleased(event(MouseEvent.MOUSE_RELEASED,180,190,1));
            String original=saved.get(key); assertNotNull(original);
            p.restoreHistory(false); assertEquals("[]",saved.get(key));
            p.restoreHistory(true); assertEquals(original,saved.get(key));
            p.createNamedLayout("PvM",true,false);
            LayoutCatalog catalog=new Gson().fromJson(saved.get(key+"_catalog"),LayoutCatalog.class);
            assertEquals(original,saved.get("account-a/"+catalog.storageKey("markersV3_profile_10")));
            p.createNamedLayout("Skilling",false,false);
            catalog=new Gson().fromJson(saved.get(key+"_catalog"),LayoutCatalog.class);
            assertEquals("[]",saved.get("account-a/"+catalog.storageKey("markersV3_profile_10")));
            assertEquals(original,saved.get(key));
            p.switchNamedLayout(0); p.onBeforeRender(new BeforeRender());
            p.armTransform(0,false);
            assertTrue(mouse.mousePressed(event(MouseEvent.MOUSE_PRESSED,130,130,1)).isConsumed());
            mouse.mouseReleased(event(MouseEvent.MOUSE_RELEASED,160,170,1));
            Marker moved=new Gson().fromJson(saved.get(key),Marker[].class)[0];
            assertEquals(50,moved.x); assertEquals(60,moved.y);
            p.restoreHistory(false); assertEquals(original,saved.get(key));
            p.restoreHistory(true); assertEquals(50,new Gson().fromJson(saved.get(key),Marker[].class)[0].x);
            when(p.configs.getRSProfileKey()).thenReturn("account-b");
            p.onRuneScapeProfileChanged(new RuneScapeProfileChanged("account-a","account-b"));
            p.restoreHistory(false);
            assertNull(saved.get("account-b/markersV3_profile_10"));
        }
        finally { p.shutDown(); }
    }

    @Test public void linkedRegionsFollowReflowWithoutCoveringUnrelatedSlots()
    {
        java.util.List<ItemAnchors.Cell> wide=java.util.Arrays.asList(
            new ItemAnchors.Cell(1,new Rectangle(106,102,36,32)),
            new ItemAnchors.Cell(2,new Rectangle(154,102,36,32)),
            new ItemAnchors.Cell(99,new Rectangle(202,102,36,32)),
            new ItemAnchors.Cell(3,new Rectangle(250,102,36,32)));
        java.util.List<Rectangle> regions=ItemAnchors.regions(new int[]{1,2,3},wide);
        assertEquals(2,regions.size());
        assertTrue(regions.contains(new Rectangle(100,100,96,36)));
        assertFalse(regions.stream().anyMatch(r -> r.contains(220,118)));
        java.util.List<ItemAnchors.Cell> narrow=java.util.Arrays.asList(
            new ItemAnchors.Cell(1,new Rectangle(306,202,36,32)),
            new ItemAnchors.Cell(2,new Rectangle(306,238,36,32)),
            new ItemAnchors.Cell(3,new Rectangle(354,238,36,32)));
        java.util.List<Rectangle> moved=ItemAnchors.regions(new int[]{1,2,3},narrow);
        assertFalse(moved.stream().anyMatch(r -> r.contains(370,210)));
        assertTrue(moved.stream().anyMatch(r -> r.contains(320,210)));
        assertTrue(ItemAnchors.regions(new int[]{42},narrow).isEmpty());
    }

    @Test public void linkingUsesEnclosedVisibleItemCentersAndGridSnapIsOutward()
    {
        java.util.List<ItemAnchors.Cell> cells=java.util.Arrays.asList(
            new ItemAnchors.Cell(1,new Rectangle(106,102,36,32)),
            new ItemAnchors.Cell(2,new Rectangle(154,102,36,32)),
            new ItemAnchors.Cell(3,new Rectangle(106,66,36,32)));
        assertArrayEquals(new int[]{1},ItemAnchors.enclosed(new Rectangle(100,50,48,100),cells,new Rectangle(100,100,200,200)));
        assertEquals(new Rectangle(100,100,96,36),ItemAnchors.snap(new Rectangle(110,105,50,20),cells));
        Marker m=new Marker(0,0,50,50); m.linkedItemIds=new int[]{1,2};
        assertArrayEquals(m.linkedItemIds,new Gson().fromJson(new Gson().toJson(m),Marker.class).linkedItemIds);
    }

    @Test public void linkedBackgroundRepositionsAfterBankResizeAndHidesAbsentItems() throws Exception
    {
        WindowMarkersPlugin p=plugin();
        Widget items=mock(Widget.class), child=mock(Widget.class);
        when(p.client.getWidget(InterfaceID.Bankmain.ITEMS)).thenReturn(items);
        when(items.getBounds()).thenReturn(new Rectangle(110,150,280,200));
        when(items.getDynamicChildren()).thenReturn(new Widget[]{child});
        when(child.getItemId()).thenReturn(123);
        when(child.getBounds()).thenReturn(new Rectangle(126,172,36,32));
        when(p.configs.getConfiguration("windowmarkers","account-a","markersV3_profile_10"))
            .thenReturn("[{\"x\":20,\"y\":20,\"width\":60,\"height\":60,\"scrollWithItems\":true,\"linkedItemIds\":[123],\"backgroundArgb\":-65536}]");
        try
        {
            p.onProfileChanged(new ProfileChanged()); p.onBeforeRender(new BeforeRender());
            WindowMarkersPlugin.BackgroundOverlay bg=p.new BackgroundOverlay();
            BufferedImage canvas=new BufferedImage(500,500,2); Graphics2D g=canvas.createGraphics(); bg.render(g); g.dispose();
            assertEquals(new Color(255,0,0,115).getRGB(),canvas.getRGB(130,180));
            when(items.getBounds()).thenReturn(new Rectangle(110,150,180,200));
            when(child.getBounds()).thenReturn(new Rectangle(174,244,36,32));
            p.onBeforeRender(new BeforeRender());
            canvas=new BufferedImage(500,500,2); g=canvas.createGraphics(); bg.render(g); g.dispose();
            assertEquals(0,canvas.getRGB(130,180)); assertEquals(new Color(255,0,0,115).getRGB(),canvas.getRGB(180,250));
            when(child.isHidden()).thenReturn(true); p.onBeforeRender(new BeforeRender());
            canvas=new BufferedImage(500,500,2); g=canvas.createGraphics(); bg.render(g); g.dispose();
            assertEquals(0,canvas.getRGB(180,250));
        }
        finally { p.shutDown(); }
    }

    @Test public void drawingInScrolledBankSavesContentOffsetAndCancelsOnScroll() throws Exception
    {
        WindowMarkersPlugin p=plugin(); WindowMarkersPlugin.MarkerMouse mouse=p.new MarkerMouse();
        Widget items=mock(Widget.class);
        when(p.client.getWidget(InterfaceID.Bankmain.ITEMS)).thenReturn(items);
        when(items.getBounds()).thenReturn(new Rectangle(110,150,280,200));
        when(items.getScrollY()).thenReturn(200);
        try
        {
            p.onBeforeRender(new BeforeRender()); p.arm();
            mouse.mousePressed(event(MouseEvent.MOUSE_PRESSED,130,170,1));
            mouse.mouseReleased(event(MouseEvent.MOUSE_RELEASED,190,230,1));
            verify(p.configs).setConfiguration("windowmarkers","account-a","markersV3_profile_10",
                "[{\"x\":20,\"y\":220,\"width\":60,\"height\":60,\"scrollWithItems\":true}]");
            clearInvocations(p.configs);
            p.arm(); mouse.mousePressed(event(MouseEvent.MOUSE_PRESSED,130,170,1));
            when(items.getScrollY()).thenReturn(250); p.onBeforeRender(new BeforeRender());
            assertTrue(mouse.mouseReleased(event(MouseEvent.MOUSE_RELEASED,190,230,1)).isConsumed());
            verify(p.configs,never()).setConfiguration(eq("windowmarkers"),anyString(),eq("markersV3_profile_10"),anyString());
        }
        finally { p.shutDown(); }
    }

    @Test public void contentMarkersFollowScrollingAndWindowMovement()
    {
        Rectangle bank=new Rectangle(100,100,400,400), area=new Rectangle(120,160,350,280);
        Marker m=new Marker(30,100,60,60); m.scrollWithItems=true;
        assertEquals(new Rectangle(150,260,60,60),m.onScreen(bank,area,0));
        assertEquals(new Rectangle(150,185,60,60),m.onScreen(bank,area,75));
        assertEquals(new Rectangle(250,285,60,60),m.onScreen(new Rectangle(200,200,400,400),new Rectangle(220,260,350,280),75));
        assertNull(m.onScreen(bank,null,75));
        m.scrollWithItems=false;
        assertEquals(new Rectangle(130,200,60,60),m.onScreen(bank,area,75));
    }

    @Test public void legacyMigrationKeepsTopOfBankPlacementAndHeaderMarkersFixed()
    {
        Rectangle bank=new Rectangle(100,100,400,400), area=new Rectangle(120,160,350,280);
        Marker content=new Marker(30,100,60,60), header=new Marker(20,10,80,20);
        Rectangle original=content.onScreen(bank);
        content.anchorLegacy(bank,area); header.anchorLegacy(bank,area);
        assertEquals(original,content.onScreen(bank,area,0));
        assertTrue(content.scrollWithItems); assertFalse(header.scrollWithItems);
        content.anchorLegacy(bank,area);
        assertEquals(original,content.onScreen(bank,area,0));
        assertEquals(new Rectangle(120,110,80,20),header.onScreen(bank,area,100));
    }

    @Test public void labelPreferencesPersistAndSizeIsBounded()
    {
        Marker m=new Marker(10,10,100,100);
        m.label="Supplies"; m.labelFont="Arial"; m.labelSize=24; m.labelArgb=0xff00ff00;
        Marker saved=new Gson().fromJson(new Gson().toJson(m),Marker.class);
        assertEquals("Arial",saved.labelFont); assertEquals(24,saved.fontSize());
        assertEquals(Integer.valueOf(0xff00ff00),saved.labelArgb);
        assertEquals(24,WindowMarkersPlugin.labelFont(saved).getSize());
        saved.labelSize=1000; assertEquals(48,saved.fontSize());
        saved.labelSize=-1; assertEquals(8,saved.fontSize());
    }

    @Test public void profilesHaveSeparateLayoutsAndOldAccountMarkersRemainCopyable() throws Exception
    {
        WindowMarkersPlugin p=plugin(); WindowMarkersPlugin.MarkerMouse mouse=p.new MarkerMouse();
        Map<String,String> saved=new HashMap<>();
        String legacy="[{\"x\":20,\"y\":20,\"width\":60,\"height\":70,\"label\":\"Herbs\",\"borderArgb\":-65536,\"backgroundArgb\":-16776961}]";
        saved.put("account-a/markersV2",legacy);
        when(p.configs.getConfiguration(eq("windowmarkers"),anyString(),anyString()))
            .thenAnswer(call -> saved.get(call.getArgument(1)+"/"+call.getArgument(2)));
        doAnswer(call -> { saved.put(call.getArgument(1)+"/"+call.getArgument(2),call.getArgument(3)); return null; })
            .when(p.configs).setConfiguration(eq("windowmarkers"),anyString(),anyString(),anyString());
        try
        {
            p.copyAccountMarkers();
            String first=saved.get("account-a/markersV3_profile_10");
            assertNotNull(first); assertEquals(legacy,saved.get("account-a/markersV2"));
            Marker copied=new Gson().fromJson(first,Marker[].class)[0];
            assertEquals("Herbs",copied.label); assertEquals(Integer.valueOf(-65536),copied.borderArgb);
            assertEquals(Integer.valueOf(-16776961),copied.backgroundArgb);
            // An explicit copy must never overwrite a non-empty destination.
            p.copyAccountMarkers(); assertEquals(first,saved.get("account-a/markersV3_profile_10"));
            p.arm(); mouse.mousePressed(event(MouseEvent.MOUSE_PRESSED,120,120,1));
            when(p.configs.getProfile().getId()).thenReturn(20L);
            p.onProfileChanged(new ProfileChanged());
            mouse.mouseReleased(event(MouseEvent.MOUSE_RELEASED,190,190,1));
            assertNull(saved.get("account-a/markersV3_profile_20"));
            p.onBeforeRender(new BeforeRender()); p.copyAccountMarkers();
            assertNotNull(saved.get("account-a/markersV3_profile_20"));
            p.arm(); mouse.mousePressed(event(MouseEvent.MOUSE_PRESSED,150,150,1));
            mouse.mouseReleased(event(MouseEvent.MOUSE_RELEASED,210,210,1));
            assertEquals(2,new Gson().fromJson(saved.get("account-a/markersV3_profile_20"),Marker[].class).length);
            assertEquals(first,saved.get("account-a/markersV3_profile_10"));
            assertEquals(legacy,saved.get("account-a/markersV2"));
            when(p.configs.getProfile().getId()).thenReturn(10L);
            p.onProfileChanged(new ProfileChanged()); p.onBeforeRender(new BeforeRender());
            p.arm(); mouse.mousePressed(event(MouseEvent.MOUSE_PRESSED,150,150,1));
            mouse.mouseReleased(event(MouseEvent.MOUSE_RELEASED,210,210,1));
            assertEquals(2,new Gson().fromJson(saved.get("account-a/markersV3_profile_10"),Marker[].class).length);
            // Renaming changes the display name, not the stable layout key.
            when(p.configs.getProfile().getName()).thenReturn("Renamed wide bank");
            p.onProfileChanged(new ProfileChanged()); p.onBeforeRender(new BeforeRender());
            p.arm(); mouse.mousePressed(event(MouseEvent.MOUSE_PRESSED,170,170,1));
            mouse.mouseReleased(event(MouseEvent.MOUSE_RELEASED,230,230,1));
            assertEquals(3,new Gson().fromJson(saved.get("account-a/markersV3_profile_10"),Marker[].class).length);
        }
        finally { p.shutDown(); }
    }

    @Test public void legacyMarkersKeepTransparentBackgroundAndDefaultBorder()
    {
        Marker marker=new Gson().fromJson("{\"x\":2,\"y\":3,\"width\":40,\"height\":50}",Marker.class);
        assertTrue(marker.valid()); assertNull(marker.backgroundArgb); assertNull(marker.borderArgb);
        assertNull(marker.style); assertEquals("",marker.labelText());
        marker.label="Herbs"; marker.backgroundArgb=0x80336699; marker.borderArgb=0xff123456;
        marker.style=WindowMarkersConfig.BorderStyle.PLAIN;
        Marker restored=new Gson().fromJson(new Gson().toJson(marker),Marker.class);
        assertEquals("Herbs",restored.labelText()); assertEquals(marker.backgroundArgb,restored.backgroundArgb);
        assertEquals(marker.borderArgb,restored.borderArgb); assertEquals(marker.style,restored.style);
    }

    @Test public void themedTintPreservesSpriteTransparencyAndShading()
    {
        BorderRenderer renderer=new BorderRenderer();
        BufferedImage source=new BufferedImage(3,1,BufferedImage.TYPE_INT_ARGB);
        source.setRGB(0,0,0xffffffff); source.setRGB(1,0,0xff808080);
        BufferedImage red=renderer.tint(source,Color.RED);
        assertEquals(0xffff0000,red.getRGB(0,0)); assertEquals(0xff800000,red.getRGB(1,0));
        assertEquals(0,red.getRGB(2,0));
        assertEquals(0xffffffff,source.getRGB(0,0));
        assertEquals(0xff0000ff,renderer.tint(source,Color.BLUE).getRGB(0,0));
    }

    @Test public void backgroundPassClipsToBankAndDoesNotPaintWithoutAccount() throws Exception
    {
        WindowMarkersPlugin p=plugin();
        try
        {
            when(p.configs.getConfiguration("windowmarkers","account-a","markersV3_profile_10"))
                .thenReturn("[{\"x\":280,\"y\":20,\"width\":80,\"height\":80,\"backgroundArgb\":-65536}]");
            p.onProfileChanged(new ProfileChanged()); p.onBeforeRender(new BeforeRender());
            WindowMarkersPlugin.BackgroundOverlay bg=p.new BackgroundOverlay();
            BufferedImage canvas=new BufferedImage(500,500,BufferedImage.TYPE_INT_ARGB);
            Graphics2D g=canvas.createGraphics(); bg.render(g); g.dispose();
            assertEquals(new Color(255,0,0,115).getRGB(),canvas.getRGB(390,130));
            assertEquals(0,canvas.getRGB(410,130));
            // Native item rendering occurs after this background pass; it remains unobscured.
            g=canvas.createGraphics(); g.setColor(Color.BLUE); g.fillRect(385,125,10,10); g.dispose();
            assertEquals(Color.BLUE.getRGB(),canvas.getRGB(390,130));
            when(p.client.getGameState()).thenReturn(GameState.LOGIN_SCREEN);
            p.onBeforeRender(new BeforeRender());
            canvas=new BufferedImage(500,500,BufferedImage.TYPE_INT_ARGB);
            g=canvas.createGraphics(); bg.render(g); g.dispose();
            assertEquals(0,canvas.getRGB(390,130));
        }
        finally { p.shutDown(); }
    }

    @Test public void accountsKeepSeparateLayoutsAndSwitchCancelsDrawing() throws Exception
    {
        WindowMarkersPlugin p=plugin(); WindowMarkersPlugin.MarkerMouse mouse=p.new MarkerMouse();
        Map<String,String> saved=new HashMap<>();
        when(p.configs.getConfiguration(eq("windowmarkers"),anyString(),eq("markersV3_profile_10")))
            .thenAnswer(call -> saved.get(call.getArgument(1)));
        doAnswer(call -> { saved.put(call.getArgument(1),call.getArgument(3)); return null; })
            .when(p.configs).setConfiguration(eq("windowmarkers"),anyString(),eq("markersV3_profile_10"),anyString());
        try
        {
            p.arm(); mouse.mousePressed(event(MouseEvent.MOUSE_PRESSED,120,120,1));
            mouse.mouseReleased(event(MouseEvent.MOUSE_RELEASED,180,190,1));
            String first=saved.get("account-a"); assertNotNull(first);
            p.arm(); mouse.mousePressed(event(MouseEvent.MOUSE_PRESSED,130,130,1));
            when(p.configs.getRSProfileKey()).thenReturn("account-b");
            p.onRuneScapeProfileChanged(new RuneScapeProfileChanged("account-a","account-b"));
            mouse.mouseReleased(event(MouseEvent.MOUSE_RELEASED,190,200,1));
            assertNull(saved.get("account-b"));
            p.onBeforeRender(new BeforeRender()); p.arm();
            mouse.mousePressed(event(MouseEvent.MOUSE_PRESSED,140,140,1));
            mouse.mouseReleased(event(MouseEvent.MOUSE_RELEASED,200,200,1));
            assertEquals(first,saved.get("account-a"));
            assertEquals(1,new Gson().fromJson(saved.get("account-b"),Marker[].class).length);
            when(p.configs.getRSProfileKey()).thenReturn("account-a");
            p.onRuneScapeProfileChanged(new RuneScapeProfileChanged("account-b","account-a"));
            p.onBeforeRender(new BeforeRender()); p.arm();
            mouse.mousePressed(event(MouseEvent.MOUSE_PRESSED,150,150,1));
            mouse.mouseReleased(event(MouseEvent.MOUSE_RELEASED,210,210,1));
            assertEquals(2,new Gson().fromJson(saved.get("account-a"),Marker[].class).length);
            verify(p.configs,never()).setConfiguration(eq("windowmarkers"),eq("markersV1"),anyString());
        }
        finally { p.shutDown(); }
    }

    @Test public void markersFollowWindowAndClampReverseDrag()
    {
        Rectangle bank = new Rectangle(100,100,300,300);
        Marker m = Marker.drag(new Point(200,200), new Point(-50,1000), bank);
        assertEquals(new Rectangle(100,200,100,200), m.onScreen(bank));
        assertEquals(new Rectangle(400,600,100,200), m.onScreen(new Rectangle(400,500,300,300)));
        assertTrue(m.valid());
        assertFalse(new Marker(0,0,1,1).valid());
        assertFalse(new Marker(-1,0,20,20).valid());
    }

    @Test public void borderIsClippedAndCenterStaysTransparent()
    {
        BufferedImage[] parts = new BufferedImage[8];
        for (int i=0;i<8;i++)
        {
            parts[i]=new BufferedImage(5,5,BufferedImage.TYPE_INT_ARGB);
            Graphics2D p=parts[i].createGraphics(); p.setColor(Color.RED); p.fillRect(0,0,5,5); p.dispose();
        }
        BufferedImage target=new BufferedImage(80,80,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=target.createGraphics();
        g.clip(new Rectangle(0,0,40,80));
        BorderRenderer.drawFrame(g,new Rectangle(10,10,50,50),parts); g.dispose();
        assertNotEquals(0,target.getRGB(12,12));
        assertEquals(0,target.getRGB(30,30));
        assertEquals(0,target.getRGB(59,12));
        assertEquals(0,target.getRGB(9,12));
    }

    @Test public void themeSwitchesAndMissingOverridesUseVanilla()
    {
        BorderRenderer renderer=new BorderRenderer();
        renderer.client=mock(Client.class); renderer.sprites=mock(SpriteManager.class);
        Map<Integer,SpritePixels> overrides=new HashMap<>();
        when(renderer.client.getSpriteOverrides()).thenReturn(overrides);
        BufferedImage vanilla=new BufferedImage(2,2,2), themed=new BufferedImage(3,3,2), replacement=new BufferedImage(4,4,2);
        when(renderer.sprites.getSprite(913,0)).thenReturn(vanilla);
        assertSame(vanilla,renderer.image(0,true));
        SpritePixels first=mock(SpritePixels.class), second=mock(SpritePixels.class);
        when(first.toBufferedImage()).thenReturn(themed); when(second.toBufferedImage()).thenReturn(replacement);
        overrides.put(913,first); assertSame(themed,renderer.image(0,true));
        assertSame(vanilla,renderer.image(0,false));
        overrides.put(913,second); assertSame(replacement,renderer.image(0,true));
        overrides.clear(); assertSame(vanilla,renderer.image(0,true));
    }

    @Test public void batchDeletionIsOneUndoStepAndClearOnlyChangesCurrentLayout() throws Exception
    {
        WindowMarkersPlugin p=plugin(); WindowMarkersPlugin.MarkerMouse mouse=p.new MarkerMouse();
        Map<String,String> saved=new HashMap<>();
        when(p.configs.getConfiguration(eq("windowmarkers"),anyString(),anyString()))
            .thenAnswer(call -> saved.get(call.getArgument(1)+"/"+call.getArgument(2)));
        doAnswer(call -> { saved.put(call.getArgument(1)+"/"+call.getArgument(2),call.getArgument(3)); return null; })
            .when(p.configs).setConfiguration(eq("windowmarkers"),anyString(),anyString(),anyString());
        String key="account-a/markersV3_profile_10";
        try
        {
            for(int i=0;i<3;i++)
            {
                p.arm(); mouse.mousePressed(event(MouseEvent.MOUSE_PRESSED,120+i*20,120,1));
                mouse.mouseReleased(event(MouseEvent.MOUSE_RELEASED,180+i*20,190,1));
            }
            String original=saved.get(key);
            p.removeMarkers(new int[]{2,0,2,-1,90},false);
            assertEquals(1,new Gson().fromJson(saved.get(key),Marker[].class).length);
            p.restoreHistory(false); assertEquals(original,saved.get(key));
            p.createNamedLayout("Copy",true,false);
            LayoutCatalog catalog=new Gson().fromJson(saved.get(key+"_catalog"),LayoutCatalog.class);
            String copy="account-a/"+catalog.storageKey("markersV3_profile_10");
            p.removeMarkers(new int[0],true); assertEquals("[]",saved.get(copy));
            assertEquals(original,saved.get(key));
            p.restoreHistory(false); assertEquals(original,saved.get(copy));
            when(p.configs.getRSProfileKey()).thenReturn("account-b");
            p.removeMarkers(new int[0],true); assertEquals(original,saved.get(copy));
        }
        finally { p.shutDown(); }
    }

    private WindowMarkersPlugin plugin() throws Exception
    {
        WindowMarkersPlugin p=new WindowMarkersPlugin();
        p.client=mock(Client.class); p.configs=mock(ConfigManager.class); p.gson=new Gson();
        p.config=new WindowMarkersConfig() {};
        p.mouseManager=mock(MouseManager.class); p.overlayManager=mock(OverlayManager.class);
        p.toolbar=mock(ClientToolbar.class); p.borders=new BorderRenderer();
        p.itemManager=mock(net.runelite.client.game.ItemManager.class);
        when(p.itemManager.canonicalize(anyInt())).thenAnswer(call -> call.getArgument(0));
        Widget bank=mock(Widget.class);
        when(p.client.getGameState()).thenReturn(GameState.LOGGED_IN);
        when(p.client.getWidget(InterfaceID.Bankmain.UNIVERSE)).thenReturn(bank);
        when(bank.getBounds()).thenReturn(new Rectangle(100,100,300,300));
        when(p.configs.getRSProfileKey()).thenReturn("account-a");
        net.runelite.client.config.ConfigProfile profile=mock(net.runelite.client.config.ConfigProfile.class);
        when(profile.getId()).thenReturn(10L); when(p.configs.getProfile()).thenReturn(profile);
        p.startUp(); SwingUtilities.invokeAndWait(() -> {}); p.onBeforeRender(new BeforeRender());
        return p;
    }

    private MouseEvent event(int id,int x,int y,int button)
    {
        return new MouseEvent(new Canvas(),id,0,0,x,y,1,false,button);
    }

    @Test public void explicitDrawingConsumesEntireGestureAndPersistsRelativeCoordinates() throws Exception
    {
        WindowMarkersPlugin p=plugin();
        WindowMarkersPlugin.MarkerMouse mouse=p.new MarkerMouse();
        try
        {
            assertFalse(mouse.mousePressed(event(MouseEvent.MOUSE_PRESSED,120,120,1)).isConsumed());
            p.arm();
            assertFalse(mouse.mousePressed(event(MouseEvent.MOUSE_PRESSED,20,20,1)).isConsumed());
            assertTrue(mouse.mousePressed(event(MouseEvent.MOUSE_PRESSED,120,120,1)).isConsumed());
            assertTrue(mouse.mouseDragged(event(MouseEvent.MOUSE_DRAGGED,180,190,0)).isConsumed());
            assertTrue(mouse.mouseReleased(event(MouseEvent.MOUSE_RELEASED,180,190,1)).isConsumed());
            assertTrue(mouse.mouseClicked(event(MouseEvent.MOUSE_CLICKED,180,190,1)).isConsumed());
            verify(p.configs).setConfiguration("windowmarkers","account-a","markersV3_profile_10","[{\"x\":20,\"y\":20,\"width\":60,\"height\":70,\"scrollWithItems\":false}]");
            assertFalse(mouse.mousePressed(event(MouseEvent.MOUSE_PRESSED,120,120,1)).isConsumed());
        }
        finally { p.shutDown(); }
    }

    @Test public void closingBankDuringDragCancelsWithoutLeakingRelease() throws Exception
    {
        WindowMarkersPlugin p=plugin(); WindowMarkersPlugin.MarkerMouse mouse=p.new MarkerMouse();
        try
        {
            p.arm(); mouse.mousePressed(event(MouseEvent.MOUSE_PRESSED,120,120,1));
            when(p.client.getGameState()).thenReturn(GameState.LOGIN_SCREEN);
            p.onBeforeRender(new BeforeRender());
            assertTrue(mouse.mouseReleased(event(MouseEvent.MOUSE_RELEASED,180,190,1)).isConsumed());
            verify(p.configs,never()).setConfiguration(eq("windowmarkers"),anyString(),eq("markersV3_profile_10"),anyString());
            assertFalse(mouse.mousePressed(event(MouseEvent.MOUSE_PRESSED,120,120,1)).isConsumed());
        }
        finally { p.shutDown(); }
    }

    @Test public void profileChangeCancelsPendingDrag() throws Exception
    {
        WindowMarkersPlugin p=plugin(); WindowMarkersPlugin.MarkerMouse mouse=p.new MarkerMouse();
        try
        {
            p.arm(); mouse.mousePressed(event(MouseEvent.MOUSE_PRESSED,120,120,1));
            p.onProfileChanged(new ProfileChanged());
            mouse.mouseReleased(event(MouseEvent.MOUSE_RELEASED,180,190,1));
            verify(p.configs,never()).setConfiguration(eq("windowmarkers"),anyString(),eq("markersV3_profile_10"),anyString());
        }
        finally { p.shutDown(); }
    }
}



