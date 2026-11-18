package com.windowmarkers;

import java.util.Arrays;
import net.runelite.api.Client;
import net.runelite.api.EnumComposition;
import net.runelite.api.EnumID;
import net.runelite.api.ScriptID;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class BankPotionStockTest
{
	@Test
	public void readsOnlyReadyStockAndRefreshesOnRelevantChanges()
	{
		BankPotionStock stock = new BankPotionStock();
		stock.client = mock(Client.class);
		Widget items = mock(Widget.class);
		when(stock.client.getWidget(InterfaceID.Bankmain.POTIONSTORE_ITEMS)).thenReturn(items);
		when(items.getVarTransmitTrigger()).thenReturn(new int[] { 12345 });
		EnumComposition finished = mock(EnumComposition.class), unfinished = mock(EnumComposition.class);
		when(stock.client.getEnum(EnumID.POTIONSTORE_POTIONS)).thenReturn(finished);
		when(stock.client.getEnum(EnumID.POTIONSTORE_UNFINISHED_POTIONS)).thenReturn(unfinished);
		when(finished.getIntVals()).thenReturn(new int[] { 100, 101 });
		when(unfinished.getIntVals()).thenReturn(new int[] { 102 });
		for (int id : new int[] { 100, 101, 102 })
		{
			EnumComposition potion = mock(EnumComposition.class);
			when(stock.client.getEnum(id)).thenReturn(potion);
			when(potion.getIntValue(anyInt())).thenAnswer(call -> id * 10 + (int) call.getArgument(0));
		}
		int[] stack = new int[20];
		int[] doses = { 11, 0, 2 };
		when(stock.client.getIntStack()).thenReturn(stack);
		doAnswer(call ->
		{
			int script = call.getArgument(0), potion = call.getArgument(1);
			stack[0] = script == ScriptID.POTIONSTORE_DOSES ? doses[potion - 100] : potion == 102 ? 1 : 4;
			return null;
		}).when(stock.client).runScript(any(Object[].class));
		assertTrue(stock.refresh());
		assertEquals(2, stock.potions().size());
		assertEquals(1004, stock.potions().get(0).itemId);
		assertEquals(2, stock.potions().get(0).quantity);
		assertEquals(Arrays.asList(1001, 1002, 1003, 1004), stock.potions().get(0).variants);
		assertEquals(2, stock.potions().get(1).index);
		assertTrue(stock.potions().get(1).unfinished);
		clearInvocations(stock.client);
		stock.changed(77);
		assertFalse(stock.refresh());
		verify(stock.client, never()).runScript(any(Object[].class));
		doses[0] = 3;
		stock.changed(12345);
		assertTrue(stock.refresh());
		assertEquals(1, stock.potions().size());
		stock.clear();
		assertTrue(stock.potions().isEmpty());
		assertTrue(stock.refresh());
	}

	@Test
	public void missingInterfaceDoesNotExecuteScripts()
	{
		BankPotionStock stock = new BankPotionStock();
		stock.client = mock(Client.class);
		assertFalse(stock.refresh());
		assertTrue(stock.potions().isEmpty());
		verify(stock.client, never()).runScript(any(Object[].class));
	}
}
