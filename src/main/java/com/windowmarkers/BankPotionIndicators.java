package com.windowmarkers;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.util.ImageUtil;

final class BankPotionIndicators extends Overlay
{
	private static final BufferedImage CHEST = ImageUtil.resizeImage(
			ImageUtil.loadImageResource(BankPotionIndicators.class, "sidebar-icon.png"), 14, 14);
	private final BankLayoutController owner;

	BankPotionIndicators(BankLayoutController owner)
	{
		this.owner = owner;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.MANUAL);
		drawAfterInterface(InterfaceID.BANKMAIN);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		BankPotionViews views = owner.potionViews;
		if (!owner.active() || views == null || views.externalBank())
			return null;
		Widget items = owner.client.getWidget(InterfaceID.Bankmain.POTIONSTORE_ITEMS);
		Widget viewport = owner.client.getWidget(InterfaceID.Bankmain.POTIONSTORE_CONTAINER);
		if (items == null || items.isHidden() || viewport == null || viewport.isHidden())
			return null;
		Graphics2D g = (Graphics2D) graphics.create();
		try
		{
			g.clip(viewport.getBounds());
			for (BankPotionStock.Potion potion : views.stock.potions())
			{
				if (views.settings.bankTab(potion.enumId) < 0)
					continue;
				int index = potion.index * 5;
				Widget row = items.getChild(index), icon = items.getChild(index + 1), heart = items.getChild(index + 4);
				if (row == null || row.isHidden() || icon == null || !potion.variants.contains(icon.getItemId())
						|| heart == null || heart.isHidden())
					continue;
				Rectangle bounds = heart.getBounds();
				if (bounds.width > 0 && bounds.height > 0)
					g.drawImage(CHEST, bounds.x - CHEST.getWidth() - 2,
							bounds.y + (bounds.height - CHEST.getHeight()) / 2, null);
			}
		}
		finally
		{
			g.dispose();
		}
		return null;
	}
}
