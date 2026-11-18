package com.windowmarkers;

import net.runelite.api.gameval.InterfaceID;

enum BankControl
{
	SWAP("Swap / insert", "Swap", InterfaceID.Bankmain.SWAP_INSERT, InterfaceID.Bankmain.SWAP_INSERT_GRAPHIC, 36, 3, 7),
	NOTE("Item / note", "Note", InterfaceID.Bankmain.NOTE, InterfaceID.Bankmain.NOTE_GRAPHIC, 36, 3, 7),
	ONE("Quantity: 1", "1", InterfaceID.Bankmain.QUANTITY1, InterfaceID.Bankmain.QUANTITY1_TEXT, 36, 6, 8),
	FIVE("Quantity: 5", "5", InterfaceID.Bankmain.QUANTITY5, InterfaceID.Bankmain.QUANTITY5_TEXT, 36, 6, 8),
	TEN("Quantity: 10", "10", InterfaceID.Bankmain.QUANTITY10, InterfaceID.Bankmain.QUANTITY10_TEXT, 36, 6, 8),
	X("Quantity: X", "X", InterfaceID.Bankmain.QUANTITYX, InterfaceID.Bankmain.QUANTITYX_TEXT, 36, 6, 8),
	ALL("Quantity: All", "All", InterfaceID.Bankmain.QUANTITYALL, InterfaceID.Bankmain.QUANTITYALL_TEXT, 36, 6, 8),
	SEARCH("Search", "Find", InterfaceID.Bankmain.SEARCH, InterfaceID.Bankmain.SEARCH_GRAPHIC, 46, 9, 7),
	PLACEHOLDERS("Placeholders", "Lock", InterfaceID.Bankmain.PLACEHOLDER, InterfaceID.Bankmain.PLACEHOLDER_GRAPHIC, 36,
			3, 7),
	CONTAINERS("Empty containers", "Empty", InterfaceID.Bankmain.DEPOSITCONTAINERS,
			InterfaceID.Bankmain.DEPOSITCONTAINERS_GRAPHIC, 36, 3, 7),
	INVENTORY("Deposit inventory", "Inv", InterfaceID.Bankmain.DEPOSITINV, InterfaceID.Bankmain.DEPOSITINV_GRAPHIC, 36,
			3, 7),
	WORN("Deposit worn items", "Worn", InterfaceID.Bankmain.DEPOSITWORN, InterfaceID.Bankmain.DEPOSITWORN_GRAPHIC, 36,
			3, 7);

	final String label, shortLabel;
	final int button, graphic, width, iconX, iconY;

	BankControl(String label, String shortLabel, int button, int graphic, int width, int iconX, int iconY)
	{
		this.label = label;
		this.shortLabel = shortLabel;
		this.button = button;
		this.graphic = graphic;
		this.width = width;
		this.iconX = iconX;
		this.iconY = iconY;
	}

	boolean quantity()
	{
		return ordinal() >= ONE.ordinal() && ordinal() <= ALL.ordinal();
	}

	@Override
	public String toString()
	{
		return label;
	}
}
