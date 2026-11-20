package com.windowmarkers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import net.runelite.client.config.ConfigManager;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class HiddenWikiSetupsTest
{
	@Test
	public void choicesPersistPerProfileAndStaleActionsCannotChangeAnotherProfile()
	{
		HiddenWikiSetups hidden = new HiddenWikiSetups();
		hidden.configs = mock(ConfigManager.class, RETURNS_DEEP_STUBS);
		AtomicLong profile = new AtomicLong(10);
		Map<Long, String> saved = new HashMap<>();
		when(hidden.configs.getProfile().getId()).thenAnswer(i -> profile.get());
		when(hidden.configs.getConfiguration("windowmarkers", HiddenWikiSetups.KEY))
				.thenAnswer(i -> saved.get(profile.get()));
		doAnswer(i ->
		{
			saved.put(profile.get(), i.getArgument(2));
			return null;
		}).when(hidden.configs).setConfiguration(eq("windowmarkers"), eq(HiddenWikiSetups.KEY), any());
		hidden.hide("224363:1", 10);
		assertEquals(Set.of("224363:1"), hidden.read());
		HiddenWikiSetups reopened = new HiddenWikiSetups();
		reopened.configs = hidden.configs;
		assertEquals(hidden.read(), reopened.read());
		profile.set(20);
		assertTrue(reopened.read().isEmpty());
		reopened.hide("224363:1", 10);
		reopened.restore(Set.of("224363:1"), 10);
		assertTrue(reopened.read().isEmpty());
		profile.set(10);
		hidden.hide("569290:3", 10);
		hidden.restore(Set.of("224363:1"), 10);
		assertEquals(Set.of("569290:3"), hidden.read());
		hidden.restore(hidden.read(), 10);
		assertTrue(hidden.read().isEmpty());
	}

	@Test
	public void rowChoicesStayWithinTheirActivityAndRetainExistingSetupChoices()
	{
		HiddenWikiSetups hidden = new HiddenWikiSetups();
		hidden.configs = mock(ConfigManager.class, RETURNS_DEEP_STUBS);
		when(hidden.configs.getProfile().getId()).thenReturn(10L);
		String row = HiddenWikiSetups.rowId("Sarachnis", "00:Head alternatives");
		String other = HiddenWikiSetups.rowId("Hespori", "00:Head alternatives");
		String special = HiddenWikiSetups.rowId("Tombs of Amascut", "11:Kephri, scarabs & swarms");
		assertFalse(special.contains(","));
		assertEquals("Tombs of Amascut / Kephri, scarabs & swarms", HiddenWikiSetups.label(special));
		when(hidden.configs.getConfiguration("windowmarkers", HiddenWikiSetups.KEY))
				.thenReturn("224363:1," + other);
		hidden.hide(row, 10);
		verify(hidden.configs).setConfiguration("windowmarkers", HiddenWikiSetups.KEY,
				String.join(",", new java.util.TreeSet<>(Set.of("224363:1", row, other))));
		when(hidden.configs.getConfiguration("windowmarkers", HiddenWikiSetups.KEY))
				.thenReturn(String.join(",", Set.of("224363:1", row, other)));
		var plan = SetupLoadout.build(List.of(ActivitySetups.byId("224363:1")), Map.of(), Set.of(), "");
		assertEquals(Set.of("224363:1", row), HiddenWikiSetups.forPlan(hidden.read(), plan));
		assertFalse(hidden.layoutIdentity("activity:Sarachnis::true", plan).contains(other));
		hidden.restore(HiddenWikiSetups.forPlan(hidden.read(), plan), 10);
		verify(hidden.configs).setConfiguration("windowmarkers", HiddenWikiSetups.KEY, other);
	}

	@Test
	public void hiddenLayoutsUseSeparatePositionsWithoutChangingOtherActivitiesOrCustomViews()
	{
		HiddenWikiSetups hidden = new HiddenWikiSetups();
		hidden.configs = mock(ConfigManager.class);
		SetupLoadout plan = SetupLoadout.build(List.of(ActivitySetups.byId("224363:1")), Map.of(), Set.of(), "");
		String original = "activity:Sarachnis::true";
		assertEquals(original, hidden.layoutIdentity(original, plan));
		when(hidden.configs.getConfiguration("windowmarkers", HiddenWikiSetups.KEY)).thenReturn("569290:3");
		assertEquals(original, hidden.layoutIdentity(original, plan));
		when(hidden.configs.getConfiguration("windowmarkers", HiddenWikiSetups.KEY)).thenReturn("224363:1,569290:3");
		assertEquals(original + ":hidden:224363:1", hidden.layoutIdentity(original, plan));
		assertEquals("saved:My gear", hidden.layoutIdentity("saved:My gear", plan));
		when(hidden.configs.getConfiguration("windowmarkers", HiddenWikiSetups.KEY)).thenReturn("");
		assertEquals(original, hidden.layoutIdentity(original, plan));
		verify(hidden.configs, never()).setConfiguration(anyString(), anyString(), any());
	}
}
