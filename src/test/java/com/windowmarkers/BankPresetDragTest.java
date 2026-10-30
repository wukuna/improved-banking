package com.windowmarkers;

import com.google.gson.Gson;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.ScriptEvent;
import net.runelite.api.widgets.JavaScriptCallback;
import net.runelite.api.widgets.Widget;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class BankPresetDragTest
{
    private static Widget cell(int index,int id,int x,int y)
    {
        Widget cell=mock(Widget.class);
        AtomicInteger px=new AtomicInteger(x),py=new AtomicInteger(y);
        when(cell.getIndex()).thenReturn(index);when(cell.getItemId()).thenReturn(id);
        when(cell.getOriginalX()).thenAnswer(i->px.get());when(cell.getOriginalY()).thenAnswer(i->py.get());
        doAnswer(i->{px.set(i.getArgument(0));return cell;}).when(cell).setOriginalX(anyInt());
        doAnswer(i->{py.set(i.getArgument(0));return cell;}).when(cell).setOriginalY(anyInt());
        return cell;
    }
    private static final class Fixture
    {
        final Widget parent=mock(Widget.class),a=cell(0,385,51,40),b=cell(1,385,99,40),c=cell(2,3144,147,40);
        final Client client=mock(Client.class);
        final BankViewGeometry geometry=new BankViewGeometry();
        final BankPresetPositions.Layout saved=new BankPresetPositions.Layout();
        final List<SetupItemLayout.InventoryNote> notes=new ArrayList<>(Arrays.asList(
            new SetupItemLayout.InventoryNote("First slot",51,40),new SetupItemLayout.InventoryNote("Second slot",99,40)));
        final AtomicInteger writes=new AtomicInteger();
        final AtomicReference<JavaScriptCallback> callback=new AtomicReference<>();
        Fixture() {
            when(parent.getDynamicChildren()).thenReturn(new Widget[]{a,b,c});
            when(parent.getWidth()).thenReturn(478);when(parent.getHeight()).thenReturn(300);
            when(parent.getCanvasLocation()).thenReturn(new Point(100,200));
            when(client.getDraggedWidget()).thenReturn(a);
            doAnswer(i->{callback.set(i.getArgument(0));return null;}).when(a).setOnDragCompleteListener(any(JavaScriptCallback.class));
            geometry.capture(parent,saved,writes::incrementAndGet,notes,80);
        }
        void drop(int x,int y) { when(client.getMouseCanvasPosition()).thenReturn(new Point(100+x,200+y));callback.get().run(mock(ScriptEvent.class)); }
    }

    @Test public void dropOverridesTagCallbackSwapsOnlyChosenOccurrenceAndSurvivesRebuild() {
        Fixture f=new Fixture();AtomicInteger foreign=new AtomicInteger();
        f.a.setOnDragCompleteListener((JavaScriptCallback)e->foreign.incrementAndGet());
        assertTrue(f.geometry.drag(f.client,f.parent,()->true));
        verify(f.client).setDraggedOnWidget(null);
        f.drop(110,50);
        assertEquals(0,foreign.get());assertEquals(1,f.writes.get());
        assertEquals(99,f.a.getOriginalX());assertEquals(51,f.b.getOriginalX());assertEquals(147,f.c.getOriginalX());
        assertEquals(99,f.notes.get(0).x);assertEquals(51,f.notes.get(1).x);
        assertArrayEquals(new int[]{99,40},f.saved.cells.get("385:1"));
        BankPresetPositions.Layout reloaded=new Gson().fromJson(new Gson().toJson(f.saved),BankPresetPositions.Layout.class);
        // A rebuilt bank may reorder real stack indexes; occurrence identity is visual.
        Widget first=cell(1,385,51,40),second=cell(0,385,99,40);
        when(f.parent.getDynamicChildren()).thenReturn(new Widget[]{second,first});
        f.geometry.capture(f.parent,reloaded,()->{},new ArrayList<>(),80);
        assertEquals(99,first.getOriginalX());assertEquals(51,second.getOriginalX());
        verify(f.client,never()).runScript(any(Object[].class));
    }

    @Test public void emptyCellDropUsesScrolledCoordinatesAndDoesNotCollapseOtherItems() {
        Fixture f=new Fixture();when(f.parent.getScrollY()).thenReturn(36);
        f.geometry.drag(f.client,f.parent,()->true);f.drop(69,100);
        assertEquals(51,f.a.getOriginalX());assertEquals(112,f.a.getOriginalY());
        assertEquals(99,f.b.getOriginalX());assertEquals(40,f.b.getOriginalY());
        f.b.setOriginalY(800);f.geometry.restore(f.parent);assertEquals(40,f.b.getOriginalY());
    }

    @Test public void cancelOutsideBankAndStaleCallbacksNeverSaveOrMove() {
        Fixture f=new Fixture();f.geometry.drag(f.client,f.parent,()->true);f.drop(800,50);
        assertEquals(0,f.writes.get());assertEquals(51,f.a.getOriginalX());
        f.geometry.drag(f.client,f.parent,()->false);f.drop(110,50);assertEquals(0,f.writes.get());
        f.geometry.drag(f.client,f.parent,()->true);f.geometry.cancelDrag();f.drop(110,50);assertEquals(0,f.writes.get());
        f.geometry.drag(f.client,f.parent,()->true);f.geometry.clear();f.drop(110,50);assertEquals(0,f.writes.get());
        assertFalse(f.geometry.drag(f.client,f.parent,()->true));
    }

    @Test public void addingItemCannotHideItUnderAnExistingSavedPosition() {
        Fixture f=new Fixture();f.saved.cells.put("385:1",new int[]{147,40});
        f.geometry.capture(f.parent,f.saved,()->{},f.notes,80);
        assertEquals(147,f.a.getOriginalX());
        assertTrue(f.c.getOriginalY()>=80);assertEquals(51,f.c.getOriginalX());
        assertEquals(147,f.notes.get(0).x);assertEquals(99,f.notes.get(1).x);
    }
}
