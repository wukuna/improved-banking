package com.windowmarkers;

import java.util.Arrays;
import org.junit.Test;
import net.runelite.client.config.ConfigManager;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class BankPotionItemOrderTest
{
	private BankPotionSettings saved(String value)
	{
		BankPotionSettings settings = new BankPotionSettings();
		settings.configs = mock(ConfigManager.class);
		when(settings.configs.getRSProfileConfiguration("windowmarkers", "storedPotion.bankItems.0")).thenReturn(value);
		return settings;
	}

	@Test
	public void addedAndWithdrawnItemsDoNotUndoLocalInsertion()
	{
		BankPotionSettings settings = saved("10,20,30|30,10,20");
		assertEquals(Arrays.asList(30, 10, 20, 40), BankPotionItemOrder.read(settings, 0, Arrays.asList(10, 20, 30, 40)));
		assertEquals(Arrays.asList(30, 10, 40, 20), BankPotionItemOrder.read(settings, 0, Arrays.asList(10, 40, 20, 30)));
		assertEquals(Arrays.asList(30, 20), BankPotionItemOrder.read(settings, 0, Arrays.asList(20, 30)));
		assertEquals(Arrays.asList(30, 10, 20), BankPotionItemOrder.read(settings, 0, Arrays.asList(10, 20, 30)));
		verify(settings.configs, never()).setRSProfileConfiguration(anyString(), anyString(), any());
	}

	@Test
	public void externalNativeReorderingAndInvalidSettingsUseTheCurrentNativeOrder()
	{
		for (String value : new String[] { "10,20,30|30,10,20", "broken", "10,20|10,10", "-1|20", "|x" })
		{
			BankPotionSettings settings = saved(value);
			assertEquals(Arrays.asList(30, 20, 10), BankPotionItemOrder.read(settings, 0, Arrays.asList(30, 20, 10)));
			verify(settings.configs, never()).setRSProfileConfiguration(anyString(), anyString(), any());
		}
	}
}
