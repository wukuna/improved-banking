package com.windowmarkers;

import java.awt.Component;
import java.awt.Dimension;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BooleanSupplier;
import javax.inject.Inject;
import javax.swing.*;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;

final class HiddenWikiSetups
{
	static final String KEY = "hiddenWikiSetups";
	@Inject
	ConfigManager configs;
	@Inject
	ClientThread clientThread;

	Set<String> read()
	{
		String saved = configs.getConfiguration("windowmarkers", KEY);
		Set<String> result = new TreeSet<>();
		if (saved != null)
			for (String id : saved.split(","))
				if (!id.isEmpty())
					result.add(id);
		return result;
	}

	long profile()
	{
		return configs.getProfile() == null ? -1 : configs.getProfile().getId();
	}

	void hide(String id, long profile)
	{
		if (profile < 0 || profile != profile() || ActivitySetups.byId(id) == null && !id.startsWith("row:"))
			return;
		Set<String> hidden = read();
		if (hidden.add(id))
			configs.setConfiguration("windowmarkers", KEY, String.join(",", hidden));
	}

	void restore(Collection<String> ids, long profile)
	{
		if (profile < 0 || profile != profile())
			return;
		Set<String> hidden = read();
		if (hidden.removeAll(ids))
			configs.setConfiguration("windowmarkers", KEY, String.join(",", hidden));
	}

	String layoutIdentity(String identity, SetupLoadout plan)
	{
		if (identity == null || !identity.startsWith("activity:") || plan == null)
			return identity;
		Set<String> relevant = forPlan(read(), plan);
		return relevant.isEmpty() ? identity : identity + ":hidden:" + String.join(",", relevant);
	}

	static String rowId(String activity, String group)
	{
		return "row:" + URLEncoder.encode(activity, StandardCharsets.UTF_8) + ":"
				+ URLEncoder.encode(group, StandardCharsets.UTF_8);
	}

	static Set<String> forPlan(Set<String> hidden, SetupLoadout plan)
	{
		Set<String> relevant = new TreeSet<>();
		if (hidden == null || plan == null)
			return relevant;
		for (SetupLoadout section : plan.sections())
			if (hidden.contains(section.equipmentSource))
				relevant.add(section.equipmentSource);
		for (String id : hidden)
			if (id.startsWith(rowId(plan.activity, "")))
				relevant.add(id);
		return relevant;
	}

	static String label(String id)
	{
		if (id.startsWith("row:"))
		{
			String[] parts = id.split(":", 3);
			if (parts.length == 3)
				try
				{
					String group = URLDecoder.decode(parts[2], StandardCharsets.UTF_8);
					return URLDecoder.decode(parts[1], StandardCharsets.UTF_8) + " / " + group.substring(3);
				}
				catch (IllegalArgumentException | IndexOutOfBoundsException ignored)
				{
					return id;
				}
		}
		ActivitySetups.Setup setup = ActivitySetups.byId(id);
		return setup == null ? id : setup.activity + " / " + setup.label;
	}

	void showRestoreDialog(Component parent, Collection<String> hidden, BooleanSupplier valid)
	{
		long profile = profile();
		List<String> ids = new ArrayList<>(hidden);
		ids.sort(Comparator.comparing(HiddenWikiSetups::label));
		if (ids.isEmpty())
			return;
		SwingUtilities.invokeLater(() ->
		{
			if (profile != profile())
				return;
			JList<String> choices = new JList<>(ids.stream().map(HiddenWikiSetups::label).toArray(String[]::new));
			choices.setCellRenderer(PlainText.renderer());
			choices.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
			JScrollPane scroll = new JScrollPane(choices);
			scroll.setPreferredSize(new Dimension(360, 220));
			int result = JOptionPane.showOptionDialog(parent, scroll, "Hidden sections",
					JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null,
					new Object[] { "Restore selected", "Restore all", "Cancel" }, "Restore selected");
			if (result != 0 && result != 1)
				return;
			List<String> restore = new ArrayList<>();
			if (result == 1)
				restore.addAll(ids);
			else
				for (int index : choices.getSelectedIndices())
					restore.add(ids.get(index));
			clientThread.invokeLater(() ->
			{
				if (valid.getAsBoolean())
					restore(restore, profile);
			});
		});
	}
}
