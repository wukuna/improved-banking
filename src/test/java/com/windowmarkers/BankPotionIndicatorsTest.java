package com.windowmarkers;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class BankPotionIndicatorsTest
{
	@Test
	public void indicatorFollowsTheCurrentHeartAndBankAssignmentWithinTheViewport()
	{
		BankPotionViewsTest.Fixture f = new BankPotionViewsTest.Fixture();
		BankLayoutController owner = new BankControlLayoutTest.Fixture().controller;
		owner.client = f.views.client;
		owner.potionViews = f.views;
		Widget viewport = mock(Widget.class), row = mock(Widget.class), icon = mock(Widget.class),
				heart = mock(Widget.class);
		when(owner.client.getWidget(InterfaceID.Bankmain.POTIONSTORE_CONTAINER)).thenReturn(viewport);
		when(viewport.getBounds()).thenReturn(new Rectangle(0, 0, 100, 100));
		when(f.store.widget.getChild(15)).thenReturn(row);
		when(f.store.widget.getChild(16)).thenReturn(icon);
		when(f.store.widget.getChild(19)).thenReturn(heart);
		when(icon.getItemId()).thenReturn(104);
		when(heart.getBounds()).thenReturn(new Rectangle(70, 40, 13, 12));
		BankPotionIndicators overlay = new BankPotionIndicators(owner);
		assertEquals(0, pixels(overlay, new Rectangle(0, 0, 100, 100)));
		when(f.views.configs.getRSProfileConfiguration("windowmarkers", "storedPotion.1000.tab")).thenReturn("0");
		assertTrue(pixels(overlay, new Rectangle(54, 39, 14, 14)) > 0);
		assertEquals(0, pixels(overlay, new Rectangle(70, 40, 13, 12)));
		when(heart.getBounds()).thenReturn(new Rectangle(40, 70, 13, 12));
		assertEquals(0, pixels(overlay, new Rectangle(54, 39, 14, 14)));
		assertTrue(pixels(overlay, new Rectangle(24, 69, 14, 14)) > 0);
		when(viewport.getBounds()).thenReturn(new Rectangle(0, 0, 100, 60));
		assertEquals(0, pixels(overlay, new Rectangle(0, 0, 100, 100)));
		when(viewport.getBounds()).thenReturn(new Rectangle(0, 0, 100, 100));
		when(f.views.configs.getRSProfileConfiguration("windowmarkers", "storedPotion.1000.tab")).thenReturn(null);
		assertEquals(0, pixels(overlay, new Rectangle(0, 0, 100, 100)));
	}

	private int pixels(BankPotionIndicators overlay, Rectangle area)
	{
		BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		try
		{
			overlay.render(graphics);
		}
		finally
		{
			graphics.dispose();
		}
		int count = 0;
		for (int y = area.y; y < area.y + area.height; y++)
			for (int x = area.x; x < area.x + area.width; x++)
				if ((image.getRGB(x, y) >>> 24) != 0)
					count++;
		return count;
	}
}
