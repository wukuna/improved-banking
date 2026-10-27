package com.windowmarkers;

import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;
import java.awt.Color;
import java.awt.image.BufferedImage;
import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.util.AsyncBufferedImage;
import net.runelite.client.util.ImageCapture;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class BankCaptureTest
{
    private BankCapture capture;
    private Widget item;
    private AsyncBufferedImage image;
    private final List<BooleanSupplier> tasks=new ArrayList<>();
    private final List<Boolean> results=new ArrayList<>();
    private void setup()
    {
        capture=new BankCapture();capture.client=mock(Client.class);capture.items=mock(ItemManager.class);
        capture.clientThread=mock(ClientThread.class);
        doAnswer(call -> { tasks.add(call.getArgument(0));return null; }).when(capture.clientThread).invokeLater(any(BooleanSupplier.class));
        capture.config=new WindowMarkersConfig(){};capture.capture=mock(ImageCapture.class);
        Widget bank=mock(Widget.class),grid=mock(Widget.class);item=mock(Widget.class);
        when(capture.client.getWidget(InterfaceID.Bankmain.UNIVERSE)).thenReturn(bank);
        when(capture.client.getWidget(InterfaceID.Bankmain.ITEMS)).thenReturn(grid);
        when(grid.getWidth()).thenReturn(80);when(grid.getDynamicChildren()).thenReturn(new Widget[]{item});
        when(item.getItemId()).thenReturn(385);when(item.getItemQuantity()).thenReturn(1);
        when(capture.items.getItemComposition(385)).thenReturn(mock(ItemComposition.class));
        image=new AsyncBufferedImage(mock(ClientThread.class),36,32,BufferedImage.TYPE_INT_ARGB);
        when(capture.items.getImage(385,1,true)).thenReturn(image);
    }
    @Test public void coldItemImagesMustFinishBeforeScreenshotIsSaved()
    {
        setup();
        assertTrue(capture.save(Collections.emptyList(),() -> true,results::add));
        assertFalse(tasks.get(0).getAsBoolean());
        verifyNoInteractions(capture.capture);
        // Moving or changing the bank while a sprite loads must not change the captured snapshot.
        when(item.getRelativeX()).thenReturn(50);when(item.getItemId()).thenReturn(999);
        image.setRGB(0,0,Color.RED.getRGB());image.loaded();
        assertTrue(tasks.get(0).getAsBoolean());
        ArgumentCaptor<BufferedImage> saved=ArgumentCaptor.forClass(BufferedImage.class);
        verify(capture.capture).saveScreenshot(saved.capture(),eq("bank"),eq("bank-markers"),eq(true),eq(false));
        assertEquals(Color.RED.getRGB(),saved.getValue().getRGB(0,40));
        assertEquals(Collections.singletonList(true),results);
    }
    @Test public void contextChangesDiscardLateImages()
    {
        setup();AtomicBoolean valid=new AtomicBoolean(true);
        assertTrue(capture.save(Collections.emptyList(),valid::get,results::add));
        valid.set(false);image.loaded();assertTrue(tasks.get(0).getAsBoolean());
        verifyNoInteractions(capture.capture);assertTrue(results.isEmpty());
    }
    @Test public void missingImagesTimeOutAndAllowRetry()
    {
        setup();AtomicLong time=new AtomicLong();capture.clock=time::get;
        assertTrue(capture.save(Collections.emptyList(),() -> true,results::add));
        assertFalse(capture.save(Collections.emptyList(),() -> true,results::add));
        assertFalse(tasks.get(0).getAsBoolean());time.set(java.util.concurrent.TimeUnit.SECONDS.toNanos(10));
        assertTrue(tasks.get(0).getAsBoolean());assertEquals(Collections.singletonList(false),results);
        assertTrue(capture.save(Collections.emptyList(),() -> true,results::add));
        verifyNoInteractions(capture.capture);
    }
    @Test public void cancelledCaptureCannotReplaceNewRequest()
    {
        setup();assertTrue(capture.save(Collections.emptyList(),() -> true,results::add));
        capture.cancel();assertTrue(capture.save(Collections.emptyList(),() -> true,results::add));
        image.loaded();assertTrue(tasks.get(0).getAsBoolean());verifyNoInteractions(capture.capture);
        assertTrue(tasks.get(1).getAsBoolean());
        verify(capture.capture,times(1)).saveScreenshot(any(),eq("bank"),eq("bank-markers"),eq(true),eq(false));
    }
}
